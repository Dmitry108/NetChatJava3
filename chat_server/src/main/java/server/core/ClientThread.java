package server.core;

import common.ChatProtocol;
import network.SocketThread;
import network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {
    private String nickname;
    private boolean isAuth;
    private boolean isReconnection;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public String getNickname() {
        return nickname;
    }

    public boolean getIsAuth() {
        return isAuth;
    }

    public void messageFormatError(String message) {
        sendMessage(ChatProtocol.getMessageFormatError(message));
        close();
    }

    public boolean isReconnection() {
        return isReconnection;
    }

    public void reconnect() {
        isReconnection = true;
        close();
    }
}