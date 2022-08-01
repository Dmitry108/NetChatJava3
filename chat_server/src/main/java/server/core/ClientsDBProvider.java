package server.core;

import java.sql.*;

public class ClientsDBProvider {
    private static Connection connection;
    private static PreparedStatement statement;

    private static final String REGISTER_QUERY = "INSERT INTO users (login, nickname, password) VALUES (?, ?, ?);";
    private static final String AUTH_QUERY = "SELECT nickname FROM users WHERE login = ? AND password = ?;";
    private static final String LOGIN_EXISTS = "SELECT login FROM users WHERE login = ?;";
    private static final String NICKNAME_EXISTS = "SELECT nickname FROM users WHERE nickname = ?;";
    private static final String UPDATE_NICKNAME = "UPDATE users SET nickname = ? WHERE login = ?;";

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
            if (request.next()) {
                return request.getString("nickname");
            }
        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return null;
    }

    public static boolean register(String login, String nickname, String password) {
        try {
            statement = connection.prepareStatement(REGISTER_QUERY);
            statement.setString(1, login);
            statement.setString(2, nickname);
            statement.setString(3, password);
            return statement.executeUpdate() != 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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

    public static boolean updateNickname(String login, String nickname) {
        try {
            statement = connection.prepareStatement(UPDATE_NICKNAME);
            statement.setString(1, nickname);
            statement.setString(2, login);
            return statement.executeUpdate() != 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}