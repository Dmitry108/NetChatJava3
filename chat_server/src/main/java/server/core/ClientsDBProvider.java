package server.core;

import common.ChatProtocol;

import java.sql.*;

public class ClientsDBProvider {
    private static Connection connection;
    private static PreparedStatement statement;

    private static final String REGISTER_QUERY = "INSERT INTO users (login, nickname, password) VALUES (?, ?, ?);";
    private static final String AUTH_QUERY = "SELECT nickname FROM users WHERE login = ? AND password = ?;";
    private static final String LOGIN_EXISTS = "SELECT login FROM users WHERE login = ?;";
    private static final String NICKNAME_EXISTS = "SELECT nickname FROM users WHERE nickname = ?;";

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat_server/src/main/resources/clients.db");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }

    synchronized static String getNicknameByLoginAndPassword(String login, String password) {
        try {
            statement = connection.prepareStatement(AUTH_QUERY);
            statement.setString(1, login);
            statement.setString(2, password);
            ResultSet request = statement.executeQuery();
//            System.out.println(request.toString());
            if (request.next()) {
                return request.getString("nickname");
            }
        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return null;
    }

    public static String register(String login, String nickname, String password) {
        try {
            boolean isLoginExist = checkLoginExists(login);
            boolean isNicknameExist = checkNicknameExists(nickname);
            if (isLoginExist && isNicknameExist) return ChatProtocol.LOGIN_NICKNAME_EXISTS;
            if (isLoginExist) return ChatProtocol.LOGIN_EXISTS;
            if (isNicknameExist) return ChatProtocol.NICKNAME_EXISTS;
            statement = connection.prepareStatement(REGISTER_QUERY);
            statement.setString(1, login);
            statement.setString(2, nickname);
            statement.setString(3, password);
            if (statement.executeUpdate() != 0) return ChatProtocol.ACCESS;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ChatProtocol.ERROR;
    }

    public static boolean checkLoginExists(String login) throws SQLException {
        statement = connection.prepareStatement(LOGIN_EXISTS);
        statement.setString(1, login);
        ResultSet request = statement.executeQuery();
        return request.next();
    }

    public static boolean checkNicknameExists(String nickname) throws SQLException {
        statement = connection.prepareStatement(NICKNAME_EXISTS);
        statement.setString(1, nickname);
        ResultSet request = statement.executeQuery();
        return request.next();
    }
}