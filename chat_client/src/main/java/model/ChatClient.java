package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import packet.Packet;
import packet.PacketType;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatClient implements Observed {

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);
    private static final Gson GSON = new Gson();
    private static final int ATTEMPTS_TO_CONNECT = 5;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private List<Observer> observers;
    private String serverHost;
    private int serverPort;
    private Socket socket;
    private Thread readerThread;
    private BufferedReader reader;
    private PrintWriter writer;
    private List<String> usersList;
    private String nickname;
    private boolean isConnected;

    public ChatClient() {
        observers = new ArrayList<>();
        usersList = new ArrayList<>();
    }

    public void launchChatClient(String serverName, int serverPort, String nickname) {
        this.serverHost = serverName;
        this.serverPort = serverPort;

        if (connect()) {
            login(nickname);
        } else {
            String systemMessage = "Не удалось подключиться к серверу. Проверьте имя и порт сервера.";
            notifyNewConnectionRejected(systemMessage);
        }
    }

    private boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
            return true;
        } catch (IOException e) {
            log.error("Failed connect to server.", e);
        }
        return false;
    }

    private void login(String nickname) {
        Packet request = new Packet();
        request.setType(PacketType.LOGIN);
        request.setDate(dateFormat.format(new Date()));
        request.setNickname(nickname);
        String line = GSON.toJson(request);
        writer.println(line);
        writer.flush();

        String receivedNickname = null;
        try {
            receivedNickname = reader.readLine();
        } catch (IOException e) {
            log.error("Failed server login response.", e);
        }

        Packet response = GSON.fromJson(receivedNickname, Packet.class);
        if (response.getType() != PacketType.LOGIN_REJECTED) {
            this.nickname = nickname;
            isConnected = true;
            usersList = response.getUsers();

            notifyStatusUpdated(true);
            notifyMessageReceived(String.format("Вы присоединились к чату под именем %s.", nickname));
            notifyUsersUpdated(response);
            startMessageReader();
        } else {
            String systemMessage;
            if (isConnected) {
                isConnected = false;
                systemMessage = "Соединение с сервером потеряно. При переподключении кто-то занял ваше имя. " +
                        "Выберите другое имя и попробуйте подключиться снова.";
            } else {
                systemMessage = "Пользователь с данным именем уже зарегистрирован. Выберите другое имя.";
            }
            notifyNewConnectionRejected(systemMessage);
        }
    }

    private void startMessageReader() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    readPacketData(line);
                }
            } catch (IOException e) {
                disconnectSocket();
                if (isConnected) {
                    log.error("Failed to listen to server.", e);
                    sendDisconnectMessage();
                    reconnect();
                }
            }
        });
        readerThread.start();
    }

    private void readPacketData(String line) {
        Packet packet = GSON.fromJson(line, Packet.class);
        String date = packet.getDate();
        String user = packet.getNickname();
        String message = packet.getMessage();
        String rxMessage = null;
        switch (packet.getType()) {
            case LOGIN:
                rxMessage = String.format("%s %s присоединился к чату.", date, user);
                notifyUsersUpdated(packet);
                break;
            case LOGOUT:
                if (user.equals(nickname)) {
                    break;
                }
                rxMessage = String.format("%s %s отключился.", date, user);
                notifyUsersUpdated(packet);
                break;
            case CHAT:
                rxMessage = String.format("%s [%s]: %s", date, user, message);
                break;
            default:
        }
        notifyMessageReceived(rxMessage);
    }

    private void reconnect() {
        String systemMessage = "Соединение с сервером потеряно. Попытка переподключиться...";
        notifyMessageReceived(systemMessage);
        int attemptsCounter = 0;
        while (attemptsCounter < ATTEMPTS_TO_CONNECT) {
            log.info("Trying to reconnect...");
            if (connect()) {
                login(nickname);
                break;
            }

            try {
                Thread.sleep(1000);
                attemptsCounter++;
                if (attemptsCounter == ATTEMPTS_TO_CONNECT) {
                    systemMessage = "Попытки восстановить соединение с сервером исчерпаны.";
                    notifyMessageReceived(systemMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Failed to sleep thread.", e);
            }
        }
    }

    public void disconnectCommand() {
        sendDisconnectMessage();
        disconnectSocket();
    }

    private void sendDisconnectMessage() {
        isConnected = false;
        notifyMessageReceived("Вы отключились.");
        notifyStatusUpdated(false);
    }

    private void disconnectSocket() {
        Packet packet = new Packet();
        packet.setType(PacketType.LOGOUT);
        packet.setNickname(nickname);
        sendPacket(packet);
        try {
            socket.close();
        } catch (IOException e) {
            log.error("Failed to disconnect.", e);
            return;
        }
        readerThread.interrupt();
    }

    public void sendUserMessage(String value) {
        Packet packet = new Packet();
        packet.setType(PacketType.CHAT);
        packet.setMessage(value);
        sendPacket(packet);
    }
    private void sendPacket(Packet packet) {
        packet.setDate(dateFormat.format(new Date()));
        packet.setNickname(nickname);
        String line = GSON.toJson(packet);
        writer.println(line);
        writer.flush();
    }

    private void notifyNewConnectionRejected(String systemMessage) {
        for (Observer observer : observers) {
            observer.newConnectionRejected(systemMessage);
        }
    }

    private void notifyStatusUpdated(boolean value) {
        for (Observer observer : observers) {
            observer.statusUpdated(value);
        }
    }

    private void notifyMessageReceived(String value) {
        for (Observer observer : observers) {
            observer.messageReceived(value);
        }
    }

    private void notifyUsersUpdated(Packet packet) {
        usersList = packet.getUsers();
        for (Observer observer : observers) {
            observer.usersUpdated(usersList);
        }
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
}