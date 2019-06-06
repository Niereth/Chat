package model;

import java.util.List;

public interface Observer {

    void statusUpdated(boolean value);

    void messageReceived(String value);

    void usersUpdated(List<String> usersList);

    void newConnectionRejected(String systemMessage);
}
