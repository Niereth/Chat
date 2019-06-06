import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import packet.Packet;
import packet.PacketType;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectionHandler extends Thread {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static final Gson GSON = new Gson();
    private static final Object LOCK = new Object();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final Server server;
    private final Socket clientSocket;
    private PrintWriter writer;
    private String id;

    ConnectionHandler(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            log.error("Failed to read client {} data", clientSocket.getRemoteSocketAddress(), e);
            handleIncorrectLogout();
        } finally {
            closeClientSocket();
            server.getConnectionsMap().remove(id);
        }
    }

    private void handleClientSocket() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer = new PrintWriter(clientSocket.getOutputStream());
        String line;
        while ((line = reader.readLine()) != null) {
            Packet packet = GSON.fromJson(line, Packet.class);
            switch (packet.getType()) {
                case LOGIN:
                    handleLogin(packet);
                    break;
                case LOGOUT:
                    handleLogout(packet);
                    break;
                case CHAT:
                    sendPacketToEachConnection(packet);
                    break;
                default:
            }
        }
    }

    private void handleLogin(Packet packet) {
        synchronized (LOCK) {
            if (server.getConnectionsMap().containsKey(packet.getNickname())) {
                Packet packetOut = new Packet();
                packetOut.setType(PacketType.LOGIN_REJECTED);
                sendPacket(packetOut);
            } else {
                id = packet.getNickname();
                server.getConnectionsMap().put(packet.getNickname(), this);
                List<String> usersList = new ArrayList<>(server.getConnectionsMap().keySet());
                packet.setUsers(usersList);
                sendPacketToEachConnection(packet);
            }
        }
    }

    private void handleIncorrectLogout() {
        Packet packet = new Packet();
        packet.setDate(dateFormat.format(new Date()));
        packet.setNickname(id);
        packet.setType(PacketType.LOGOUT);
        handleLogout(packet);
    }

    private void handleLogout(Packet packet) {
        server.getConnectionsMap().remove(packet.getNickname());
        List<String> usersList = new ArrayList<>(server.getConnectionsMap().keySet());
        packet.setUsers(usersList);
        sendPacketToEachConnection(packet);
    }

    private void sendPacketToEachConnection(Packet packet) {
        for (ConnectionHandler connection : server.getConnectionsMap().values()) {
            connection.sendPacket(packet);
        }
    }

    private void sendPacket(Packet packet) {
        String line = GSON.toJson(packet);
        writer.println(line);
        writer.flush();
    }

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            log.error("Failed to close client socket.", e);
        }
    }
}