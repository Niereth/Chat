import controller.Controller;
import model.ChatClient;
import view.ChatWindow;

import javax.swing.*;

public class ChatClientLauncher {

    public static void main(String[] args) {
        ChatClient model = new ChatClient();
        Controller controller = new Controller(model);
        SwingUtilities.invokeLater(() -> {
            ChatWindow view = new ChatWindow(controller);
            model.addObserver(view);
        });
    }
}