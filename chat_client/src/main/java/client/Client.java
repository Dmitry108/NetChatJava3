package client;

public interface Client {
    void register(String login, String nickname, String password);
    void updateNickname(String login, String nickname);
    String getNickname();
    String getLogin();
}