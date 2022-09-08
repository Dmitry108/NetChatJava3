package server.core;

import common.ChatProtocol;
import network.ServerSocketThread;
import network.ServerSocketThreadListener;
import network.SocketThread;
import network.SocketThreadListener;
import server.utils.ServerLogger;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
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
        ClientsDBProvider.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server thread stopped");
        ClientsDBProvider.disconnect();
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
        ServerLogger.warning(throwable.getMessage());
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
            String message = client.getNickname() + " disconnected";
            ServerLogger.finer(message);
            sendToAllAuthorizes(ChatProtocol.getMessageBroadcast("Server", message));
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
            case ChatProtocol.UPDATE_NICKNAME_REQUEST -> updateNickname(clientThread, strArray[1], strArray[2]);
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

    private void updateNickname(ClientThread client, String login, String nickname) {
        try {
            boolean isNicknameExist = ClientsDBProvider.checkNicknameExists(nickname);
            if (isNicknameExist) {
                client.updateNicknameDeny("This nickname is already used");
            } else if (ClientsDBProvider.updateNickname(login, nickname)) {
                client.updateNicknameAccess(nickname);
                client.setNickname(nickname);
                sendToAllAuthorizes(ChatProtocol.getUserList(getUsers()));
            } else {
                client.updateNicknameDeny("Error on updating");
            }
        } catch (SQLException e) {
            ServerLogger.warning(e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNotAuthMessage(ClientThread clientThread, String message) {
        String[] strArray = message.split(ChatProtocol.DELIMITER);
        switch (strArray[0]) {
            case ChatProtocol.AUTH_REQUEST -> authorize(clientThread, message, strArray);
            case ChatProtocol.REGISTER_REQUEST -> register(clientThread, message, strArray);
        }
    }

    private void authorize(ClientThread clientThread, String message, String[] strArray) {
        if (strArray.length != 3) {
            clientThread.messageFormatError(message);
            return;
        }
        String login = strArray[1];
        String password = strArray[2];
        String nickname = ClientsDBProvider.getNicknameByLoginAndPassword(login, password);
        if (nickname == null) {
            putLog("Invalid login attempt " + login);
            clientThread.authFail();
            return;
        } else {
            ClientThread oldClient = findClientByNickname(nickname);
            clientThread.authAccept(nickname);
            if (oldClient == null) {
                String msg = nickname + " connected";
                sendToAllAuthorizes(ChatProtocol.getMessageBroadcast("Server", msg));
            } else {
                oldClient.reconnect();
                clients.remove(oldClient);
            }
        }
        sendToAllAuthorizes(ChatProtocol.getUserList(getUsers()));
    }

    private void register(ClientThread clientThread, String message, String[] strArray) {
        if (strArray.length != 4) {
            clientThread.messageFormatError(message);
            return;
        }
        String login = strArray[1];
        String nickname = strArray[2];
        String password = strArray[3];
        try {
            boolean isLoginExist = ClientsDBProvider.checkLoginExists(login);
            boolean isNicknameExist = ClientsDBProvider.checkNicknameExists(nickname);
            if (isLoginExist) clientThread.registerFail("This login is already used");
            if (isNicknameExist) clientThread.registerFail("This nickname is already used");
            if (isLoginExist || isNicknameExist) return;
        } catch (SQLException e) {
            ServerLogger.warning(e.getMessage());
            e.printStackTrace();
        }
        if (ClientsDBProvider.register(login, nickname, password)) {
            clientThread.registerAccess();
        } else {
            clientThread.registerFail("Error on registration");
        }
    }

    @Override
    public synchronized void onSocketThreadException(SocketThread thread, Throwable throwable) {
        ServerLogger.warning(throwable.getMessage());
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

    private synchronized ClientThread findClientByNickname(String nickname) {
        ClientThread client;
        for (SocketThread socketThread : clients) {
            client = (ClientThread) socketThread;
            if (!client.getIsAuth()) continue;
            if (client.getNickname().equals(nickname)) {
                return client;
            }
        }
        return null;
    }
}