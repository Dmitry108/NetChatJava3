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

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean getIsAuth() {
        return isAuth;
    }

    public void authAccept(String nickname) {
        this.nickname = nickname;
        this.isAuth = true;
        sendMessage(ChatProtocol.getAuthAccept(nickname));
    }

    public void authFail() {
        sendMessage(ChatProtocol.getAuthDenied());
        close();
    }

    public void registerResponse(String responseCode) {
        if (responseCode.equals(ChatProtocol.ACCESS)) {
            sendMessage(ChatProtocol.getRegisterAccess());
        } else {
            sendMessage(ChatProtocol.getRegisterDeny(responseCode));
        }
    }

    public void updateNicknameAccess(String nickname) {
        this.nickname = nickname;
        sendMessage(ChatProtocol.getUpdateNicknameAccess(nickname));
    }

    public void updateNicknameDeny(String message) {
        sendMessage(ChatProtocol.getUpdateNicknameDeny(message));
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