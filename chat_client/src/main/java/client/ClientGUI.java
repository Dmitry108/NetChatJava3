package client;

import common.ChatProtocol;
import network.SocketThread;
import network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class ClientGUI extends JFrame implements Client, ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss ");
    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;
    public static final String TITLE = "Chat";

    private final JPanel panelTop = new JPanel(new GridLayout(2, 4));
    private final JTextField ipAddressTextField = new JTextField("127.0.0.1");
    private final JTextField portTextField = new JTextField("222");
    private final JButton connectButton = new JButton("Connect");
    private final JCheckBox onTopCheckBox = new JCheckBox("Always on top");
    private final JTextField loginTextField = new JTextField("aglar");
    private final JPasswordField passwordField = new JPasswordField("123");
    private final JButton loginButton = new JButton("Login");
    private final JButton registerButton = new JButton("Register");
    private final JTextArea logTextArea = new JTextArea();
    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton logoutButton = new JButton("Logout");
    private final JButton accountButton = new JButton("Account");
    private final JTextField messageTextField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JList<String> usersList = new JList<>();

    private boolean shownIoErrors = false;
    private SocketThread socketThread;
    private MessagesLog messagesLog;
    private String nickname;

    public ClientGUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);

        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        JScrollPane userListScrollPane = new JScrollPane(usersList);
        userListScrollPane.setPreferredSize(new Dimension(100, 0));

        panelTop.add(ipAddressTextField);
        panelTop.add(portTextField);
        panelTop.add(connectButton);
        panelTop.add(onTopCheckBox);
        panelTop.add(loginTextField);
        panelTop.add(passwordField);
        panelTop.add(loginButton);
        panelTop.add(registerButton);
        JPanel panel = new JPanel(new GridLayout(1,2));
        panel.add(accountButton);
        panel.add(logoutButton);
//        panelBottom.add(logoutButton, BorderLayout.WEST);
        panelBottom.add(panel, BorderLayout.WEST);
        panelBottom.add(messageTextField, BorderLayout.CENTER);
        panelBottom.add(sendButton, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
        add(panelBottom, BorderLayout.SOUTH);
        add(userListScrollPane, BorderLayout.EAST);

        onTopCheckBox.addActionListener(this);
        connectButton.addActionListener(this);
        sendButton.addActionListener(this);
        messageTextField.addActionListener(this);
        loginButton.addActionListener(this);
        registerButton.addActionListener(this);
        accountButton.addActionListener(this);
        logoutButton.addActionListener(this);

        setUIReady(false);
        setUIConnection(false);
        setVisible(true);
        Thread.setDefaultUncaughtExceptionHandler(this);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public String getLogin() {
        return loginTextField.getText();
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source.equals(onTopCheckBox)) {
            setAlwaysOnTop(onTopCheckBox.isSelected());
        } else if (source.equals(connectButton)) {
            connect();
        } else if (source.equals(loginButton)) {
            authorize(socketThread);
        } else if (source.equals(registerButton)) {
            openRegistrationForm();
        } else if (source.equals(sendButton) || source.equals(messageTextField)) {
            sendMessage();
        } else if (source.equals(accountButton)) {
            openAccountForm();
        } else if (source.equals(logoutButton)) {
            disconnect();
        } else {
            throw new IllegalStateException("Unexpected event");
        }
    }

    private void connect() {
        try {
            Socket socket = new Socket(ipAddressTextField.getText(), Integer.parseInt(portTextField.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    private void disconnect() {
        socketThread.close();
    }

    private void openRegistrationForm() {
        RegistrationGUI.getInstance().setClient(this).setVisible(true);
    }

    private void authorize(SocketThread thread) {
        String login = loginTextField.getText();
        String password = new String(passwordField.getPassword());
        thread.sendMessage(ChatProtocol.getAuthRequest(login, password));
    }

    private void sendMessage() {
        String text = messageTextField.getText();
        if (text.equals("")) return;
        messageTextField.setText("");
        messageTextField.requestFocus();
        if (usersList.isSelectionEmpty()) {
            socketThread.sendMessage(ChatProtocol.getClientBroadcast(text));
        } else {
            List<String> nicknames = usersList.getSelectedValuesList();
            StringBuilder sb = new StringBuilder();
            nicknames.forEach(user -> sb.append(user).append(ChatProtocol.DELIMITER));
            socketThread.sendMessage(ChatProtocol.getClientPrivate(text, sb.toString()));
            usersList.clearSelection();
        }
    }

    private void setUIReady(boolean isReady) {
        loginButton.setEnabled(isReady);
        registerButton.setEnabled(isReady);
    }

    private void setUIConnection(boolean flag) {
        panelTop.setVisible(!flag);
        panelBottom.setVisible(flag);
        setTitle(TITLE + (nickname != null ? " logged in as: " + nickname : ""));
        if (!flag) usersList.setListData(new String[0]);
    }

    private void putLog(String message) {
        if (message.equals("")) return;
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }

    private void openAccountForm() {
        AccountGUI.getInstance().setClient(this).setVisible(true);
    }

    public void showException(Thread thread, Throwable throwable) {
        String message = throwable.getStackTrace().length == 0 ? "Empty stack trace" :
                String.format("Exception in thread %s %s: %s%n%s", thread.getName(),
                        throwable.getClass().getCanonicalName(), throwable.getMessage(),
                        throwable.getStackTrace()[0]);
        JOptionPane.showMessageDialog(null, message, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onSockedStart(SocketThread thread, Socket socket) {
        putLog("Start");
    }

    @Override
    public void onSockedStop(SocketThread thread) {
        nickname = null;
        setUIConnection(false);
        setUIReady(false);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        setUIReady(true);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String message) {
        handleMessage(message);
    }

    private void handleMessage(String message) {
        String[] strArray = message.split(ChatProtocol.DELIMITER);
        String messageType = strArray[0];
        switch (messageType) {
            case ChatProtocol.MESSAGE_FORMAT_ERROR -> {
                putLog(message);
                socketThread.close();
            }
            case ChatProtocol.AUTH_ACCEPT -> {
                this.nickname = strArray[1];
                messagesLog = new MessagesLog(loginTextField.getText());
                setUIConnection(true);
                putLog(messagesLog.read(100).toString());
            }
            case ChatProtocol.AUTH_DENY -> putLog(message);
            case ChatProtocol.REGISTER_ACCESS -> putLog("Registration access");
            case ChatProtocol.USER_LIST -> {
                String users = message.substring(ChatProtocol.USER_LIST.length() + ChatProtocol.DELIMITER.length());
                String[] usersArray = users.split(ChatProtocol.DELIMITER);
                Arrays.sort(usersArray);
                usersList.setListData(usersArray);
            }
            case ChatProtocol.MESSAGE_BROADCAST -> onGetMessage(strArray, "%s: %s: %s");
            case ChatProtocol.MESSAGE_PRIVATE -> onGetMessage(strArray, "%s: %s private: %s");
            case ChatProtocol.UPDATE_NICKNAME_ACCESS -> {
                this.nickname = strArray[1];
                putLog("Nickname was changed");
            }
            case ChatProtocol.REGISTER_DENY, ChatProtocol.UPDATE_NICKNAME_DENY -> putLog(strArray[1]);
            default -> throw new RuntimeException("Unknown message type: " + messageType);
        }
    }

    private void onGetMessage(String[] strArray, String s) {
        String textLog = String.format(s, DATE_FORMAT.format(Long.parseLong(strArray[1])), strArray[2], strArray[3]);
        putLog(textLog);
        System.out.print(textLog);
        messagesLog.write(textLog);
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Throwable throwable) {
        showException(thread, throwable);
    }

    @Override
    public void register(String login, String nickname, String password) {
        socketThread.sendMessage(ChatProtocol.getRegisterRequest(login, nickname, password));
    }

    @Override
    public void updateNickname(String login, String nickname) {
        socketThread.sendMessage(ChatProtocol.getUpdateNicknameRequest(login, nickname));
    }
}