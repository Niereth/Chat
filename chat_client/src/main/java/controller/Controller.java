package controller;

import model.ChatClient;

public class Controller {

    private ChatClient model;

    public Controller(ChatClient m) {
        model = m;
    }

    public void connectToServer(String ipAddress, int port, String nickname) {
        model.launchChatClient(ipAddress, port, nickname);
    }

    public void sendUserMessage(String value) {
        model.sendUserMessage(value);
    }

    public void disconnect() {
        model.disconnectCommand();
    }
}