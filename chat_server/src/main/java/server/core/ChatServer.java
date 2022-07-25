package server.core;

import common.ChatProtocol;
import network.ServerSocketThread;
import network.ServerSocketThreadListener;
import network.SocketThread;
import network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {
    private final int SERVER_SOCKET_TIMEOUT = 2000;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss ");
    private ChatServerListener listener;
    private ServerSocketThread server;
    private Vector<SocketThread> clients = new Vector<>();

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            putLog("Server already started");
        } else {
            server = new ServerSocketThread(this, "Chat server", port, SERVER_SOCKET_TIMEOUT);
        }
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            putLog("Server is not running");
        } else {
            server.interrupt();
        }
    }

    private void putLog(String message) {
        listener.onChatServerMessage(String.format("%s %s: %s", DATE_FORMAT.format(System.currentTimeMillis()),
                Thread.currentThread().getName(), message));
    }

    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server thread started");
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server thread stopped");
        clients.forEach(SocketThread::close);
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
    }

    @Override
    public void onServerSoTimeout(ServerSocketThread thread, ServerSocket server) {

    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket client) {
        putLog("Client connected");
        String name = "SocketThread " + client.getInetAddress() + ": " + client.getPort();
        new ClientThread(this, name, client);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public synchronized void onSockedStart(SocketThread thread, Socket socket) {
        putLog("Client connected");
    }

    @Override
    public synchronized void onSockedStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(client);
        //специальное служебное сообщение
        if (client.getIsAuth() && !client.isReconnection()) {
            sendToAllAuthorizes(ChatProtocol.getMessageBroadcast("Server", client.getNickname() + " disconnected"));
        }
        sendToAllAuthorizes(ChatProtocol.getUserList(getUsers()));
    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        putLog("Client is ready");
        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String message) {
        ClientThread client = (ClientThread) thread;
        if (client.getIsAuth()) {
            handleAuthMessage(client, message);
        } else {
            handleNotAuthMessage(client, message);
        }
    }

    public void handleAuthMessage(ClientThread clientThread, String message) {
        System.out.println(message);
        String[] strArray = message.split(ChatProtocol.DELIMITER, 3);
        switch (strArray[0]) {
            case ChatProtocol.USER_BROADCAST -> sendToAllAuthorizes(
                    ChatProtocol.getMessageBroadcast(clientThread.getNickname(), strArray[1]));
            case ChatProtocol.USER_PRIVATE -> sendPrivate(
                    ChatProtocol.getMessagePrivate(clientThread.getNickname(), strArray[1]), strArray[2]);
            default -> clientThread.messageFormatError(message);
        }
    }

    private void sendToAllAuthorizes(String message) {
        clients.forEach(client -> {
            if (((ClientThread) client).getIsAuth()) {
                client.sendMessage(message);
            }
        });
    }

    private void sendPrivate(String message, String nicknames) {
        List<String> strNick = Arrays.asList(nicknames.split(ChatProtocol.DELIMITER));
        clients.forEach(client -> {
            ClientThread clientThread = (ClientThread) client;
            if (clientThread.getIsAuth() && strNick.contains(clientThread.getNickname())) {
                client.sendMessage(message);
            }
        });
    }

    private void handleNotAuthMessage(ClientThread clientThread, String message) {

    }

    @Override
    public synchronized void onSocketThreadException(SocketThread thread, Throwable throwable) {
        throwable.printStackTrace();
    }

    private String getUsers() {
        final StringBuilder sb = new StringBuilder();
        clients.forEach(thread -> {
            ClientThread client = (ClientThread) thread;
            if (client.getIsAuth()) {
                sb.append(client.getNickname()).append(ChatProtocol.DELIMITER);
            }
        });
        return sb.toString();
    }
}