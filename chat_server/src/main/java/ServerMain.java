public class ServerMain {

    private static final int SERVER_PORT
            = Integer.parseInt(new PropertiesLoader().getPropertyValue("SERVER_PORT"));

    public static void main(String[] args) {
        Server server = new Server(SERVER_PORT);
        server.startServer();
    }
}