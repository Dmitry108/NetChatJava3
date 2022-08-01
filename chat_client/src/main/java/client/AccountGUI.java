package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AccountGUI extends JFrame implements ActionListener {

    private static AccountGUI instance;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private static final String TITLE = "Account";

    private Client client;

    private final JPanel panel = new JPanel(new GridLayout(4, 1));
    private final JLabel loginLabel = new JLabel("Login");
    private final JTextField loginTextField = new JTextField();
    private final JLabel nicknameLabel = new JLabel("Nickname");
    private final JTextField nickTextField = new JTextField();
    private final JLabel errorLabel = new JLabel();
    private final JButton updateButton = new JButton("Update");

    private AccountGUI() {
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);

        panel.add(loginLabel);
        panel.add(loginTextField);
        panel.add(nicknameLabel);
        panel.add(nickTextField);
        add(errorLabel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(updateButton, BorderLayout.SOUTH);

        loginTextField.setEnabled(false);
        updateButton.addActionListener(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    public static AccountGUI getInstance() {
        if (instance == null) {
            instance = new AccountGUI();
        }
        return instance;
    }

    public AccountGUI setClient(Client client) {
        this.client = client;
        loginTextField.setText(client.getLogin());
        nickTextField.setText(client.getNickname());
        return this;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source.equals(updateButton)) {
            updateData();
        } else {
            throw new IllegalStateException("Unexpected event");
        }
    }

    private void updateData() {
        errorLabel.setText("");
        String nickname = nickTextField.getText();
        if (nickname.equals("")) {
            errorLabel.setText("Nickname is not filled");
            nickTextField.requestFocus();
            return;
        }
        if (!client.getNickname().equals(nickname)) {
            client.updateNickname(client.getLogin(), nickname);
            setVisible(false);
        } else {
            errorLabel.setText("Nickname is not changed");
            nickTextField.requestFocus();
        }
    }
}