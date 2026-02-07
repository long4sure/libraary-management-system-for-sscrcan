package com.school.lms;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
  
public class Login extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel logoLabel, messageLabel;
    private Image originalLogoImage;
    private JButton loginBtn;

    public Login() {
        setupWindow();
        JPanel bgPanel = createBackgroundPanel();
        add(bgPanel);
        bgPanel.add(createLogoPanel(), BorderLayout.NORTH);
        bgPanel.add(createLoginPanel(), BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                updateLogoSize();
            }
        });
    }

    /** Window setup */
    private void setupWindow() {
        setTitle("San Sebastian Library System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout());
        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/school/lms/sscrcanlogo.png")));
        } catch (Exception ignored) {}
    }

    /** Gradient background panel */
    private JPanel createBackgroundPanel() {
        return new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                var g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, new Color(110, 0, 0), 0, getHeight(), new Color(210, 160, 0)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    /** Logo section */
    private JPanel createLogoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        try {
            originalLogoImage = new ImageIcon(getClass().getResource("/com/school/lms/sscrcanlogolong.png")).getImage();
            logoLabel = new JLabel(new ImageIcon(getScaledImage(originalLogoImage, 150)));
            panel.add(logoLabel);
        } catch (Exception e) {
            panel.add(new JLabel("Logo Missing"));
        }
        return panel;
    }

    /** Login form */
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        RoundedPanel card = new RoundedPanel(30, new Color(255, 255, 255, 230));
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(400, 320));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 170, 0), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);

        JCheckBox showPass = new JCheckBox("Show Password");
        showPass.setOpaque(false);
        showPass.addActionListener(e -> passwordField.setEchoChar(showPass.isSelected() ? (char)0 : '•'));

        loginBtn = new JButton("Login");
        JButton searchBtn = new JButton("Check Book");

        messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setForeground(Color.RED);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        addField(card, gbc, 0, "Username:", usernameField);
        addField(card, gbc, 1, "Password:", passwordField);

        gbc.gridx = 1;
        gbc.gridy = 2;
        card.add(showPass, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(loginBtn);
        btnPanel.add(searchBtn);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        card.add(btnPanel, gbc);

        gbc.gridy = 4;
        card.add(messageLabel, gbc);

        loginBtn.addActionListener(e -> authenticate());
        searchBtn.addActionListener(e -> new SearchBooks().setVisible(true));

        getRootPane().setDefaultButton(loginBtn);
        panel.add(card);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(lbl, gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void updateLogoSize() {
        if (logoLabel != null && originalLogoImage != null) {
            int h = Math.max(120, getHeight() / 7);
            h = Math.min(h, 307);
            logoLabel.setIcon(new ImageIcon(getScaledImage(originalLogoImage, h)));
        }
    }

    private Image getScaledImage(Image src, int height) {
        int w = src.getWidth(null) * height / src.getHeight(null);
        BufferedImage resized = new BufferedImage(w, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(src, 0, 0, w, height, null);
        g2.dispose();
        return resized;
    }

    private void authenticate() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        loginBtn.setEnabled(false);
        messageLabel.setText("Authenticating...");

        SwingUtilities.invokeLater(() -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT username, role FROM users WHERE username=? AND password=?")) {
                stmt.setString(1, user);
                stmt.setString(2, pass);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    dispose();
                    new Dashboard(user, rs.getString("role")).setVisible(true);
                } else {
                    messageLabel.setText("Invalid username or password.");
                }
            } catch (Exception ex) {
                messageLabel.setText("Database error: " + ex.getMessage());
            } finally {
                loginBtn.setEnabled(true);
            }
        });
    }

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;
        RoundedPanel(int r, Color c) { radius = r; bgColor = c; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}

