package ui;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    private JLabel statusLabel;

    public interface LoginListener {
        void onLogin(String username, String host);
    }

    public LoginPanel(LoginListener listener) {
        setLayout(new GridBagLayout());
        setBackground(new Color(240, 242, 250));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("🎨 MultiUserPaint", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.BLACK);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        add(title, g);

        JLabel sub = new JLabel("Çok Kullanıcılı Çizim Uygulaması", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(Color.BLACK);
        g.gridy = 1;
        add(sub, g);

        g.gridwidth = 1;

        JLabel userLabel = new JLabel("Kullanıcı Adı:");
        userLabel.setForeground(Color.BLACK);
        g.gridy = 2; g.gridx = 0; add(userLabel, g);
        JTextField userField = new JTextField(16);
        g.gridx = 1; add(userField, g);

        JButton btn = new JButton("Bağlan →");
        btn.setBackground(new Color(60, 120, 200));
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        add(btn, g);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLACK);
        g.gridy = 4; add(statusLabel, g);

        Runnable doLogin = () -> {
            String user = userField.getText().trim();
            if (user.isEmpty()) { setStatusMessage("Kullanıcı adı boş olamaz!"); return; }
            listener.onLogin(user, "localhost");
        };
        btn.addActionListener(e -> doLogin.run());
        userField.addActionListener(e -> doLogin.run());
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(message == null ? " " : message);
    }
}