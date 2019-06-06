import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final int serverPort;
    private ServerSocket socket;
    private boolean isRunning = false;
    private ConcurrentMap<String, ConnectionHandler> connections;

    Server(int serverPort) {
        this.serverPort = serverPort;
        connections = new ConcurrentHashMap<>();
    }

    void startServer() {
        try {
            socket = new ServerSocket(serverPort);
            log.info("Server socket initialized on port {}", serverPort);
        } catch (IOException e) {
            log.error("Failed initialize server socket on port {}", serverPort);
            return;
        }

        new Thread(() -> {
            while (isRunning) {
                try {
                    log.info("About to accept client connection...");
                    Socket clientSocket = socket.accept();
                    log.info("Accepted connection from {}", clientSocket);

                    ConnectionHandler connection = new ConnectionHandler(this, clientSocket);
                    connection.start();
                } catch (IOException e) {
                    log.error("Failed to connect to client on socket {}", socket);
                }
            }

        }).start();

        isRunning = true;
    }

    public void stopServer() {
        isRunning = false;
        try {
            socket.close();
        } catch (IOException e) {
            log.error("Failed to stop server.", e);
        }
    }

    ConcurrentMap<String, ConnectionHandler> getConnectionsMap() {
        return connections;
    }
}