package com.school.lms;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import javax.swing.Timer;

public class Dashboard extends JFrame {
    private JLabel availableBooksLabel, borrowedBooksLabel, overdueBooksLabel, updatedLabel;
    private JLabel logoLabel, welcomeLabel;
    private Image originalLogoImage;
    private Connection conn;
    private JPanel centerPanel;

    public Dashboard(String username, String role) {
        setupWindow();

        JPanel background = createBackgroundPanel();
        add(background);

        background.add(createTopPanel(username, role), BorderLayout.NORTH);
        background.add(createCenterPanel(), BorderLayout.CENTER);
        background.add(createNavPanel(role), BorderLayout.SOUTH);

        connectDB();
        updateDashboard();
        Timer t = new Timer(10000, e -> updateDashboard());
        t.start();

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateLogoSize();
                adjustLayout();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(Dashboard.this,
                        "Are you sure you want to exit?", "Exit",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) System.exit(0);
            }
        });
    }

    private void setupWindow() {
        setTitle("San Sebastian Library System - Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/school/lms/sscrcanlogo.png")));
        } catch (Exception ignored) {}
        setMinimumSize(new Dimension(900, 600));
    }

    private JPanel createBackgroundPanel() {
        return new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(110, 0, 0), 0, getHeight(), new Color(210, 160, 0)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    private JPanel createTopPanel(String username, String role) {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(15, 30, 15, 30));

        try {
            originalLogoImage = new ImageIcon(getClass().getResource("/com/school/lms/sscrcanlogolong.png")).getImage();
            logoLabel = new JLabel(new ImageIcon(scaleImage(originalLogoImage, 100)));
            top.add(logoLabel, BorderLayout.WEST);
        } catch (Exception e) {
            top.add(new JLabel("Logo Missing"), BorderLayout.WEST);
        }

        welcomeLabel = new JLabel("Welcome, " + username + " (" + role + ")", SwingConstants.RIGHT);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLabel.setForeground(Color.WHITE);
        top.add(welcomeLabel, BorderLayout.EAST);
        return top;
    }

    private JPanel createCenterPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        centerPanel = new JPanel(new GridLayout(1, 3, 40, 40));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(60, 160, 20, 160));

        availableBooksLabel = addSummaryCard(" Available Books", new Color(255, 230, 120));
        borrowedBooksLabel = addSummaryCard(" Borrowed Books", new Color(255, 210, 80));
        overdueBooksLabel = addSummaryCard(" Overdue Books", new Color(255, 180, 60));

        updatedLabel = new JLabel("Updated just now", SwingConstants.CENTER);
        updatedLabel.setForeground(Color.WHITE);
        updatedLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));

        wrapper.add(centerPanel, BorderLayout.CENTER);
        wrapper.add(updatedLabel, BorderLayout.SOUTH);
        return wrapper;
    }

    private JLabel addSummaryCard(String title, Color bg) {
        GradientPanel card = new GradientPanel(bg, new Color(255, 255, 255, 150));
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(300, 160));
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(new Color(100, 0, 0));

        JLabel valueLbl = new JLabel("0", SwingConstants.CENTER);
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 46));
        valueLbl.setForeground(new Color(80, 0, 0));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);

        addHoverEffect(card);
        centerPanel.add(card);
        return valueLbl;
    }

    private void addHoverEffect(JPanel card) {
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new LineBorder(Color.YELLOW, 2, true));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                card.setBorder(new EmptyBorder(25, 25, 25, 25));
                card.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private JPanel createNavPanel(String role) {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        nav.setBackground(new Color(180, 0, 0));
        nav.setBorder(new MatteBorder(3, 0, 3, 0, new Color(255, 204, 0)));

        String[] items = {"Manage Books", "Issue Book", "Return Book", "Search Books", "Reports", "Logout"};
        for (String name : items) {
            if (role.equalsIgnoreCase("staff") && name.equals("Manage Books")) continue;
            nav.add(createNavButton(name));
        }
        return nav;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(255, 204, 0));
        btn.setForeground(new Color(60, 0, 0));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(150, 45));
        btn.setBorder(new LineBorder(new Color(110, 0, 0), 2, true));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(255, 230, 80)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(255, 204, 0)); }
        });

        btn.addActionListener(e -> handleNavAction(text));
        return btn;
    }

    private void handleNavAction(String text) {
        switch (text) {
            case "Manage Books" -> new ManageBooks().setVisible(true);
            case "Issue Book" -> new IssueBook().setVisible(true);
            case "Return Book" -> new ReturnBook().setVisible(true);
            case "Search Books" -> new SearchBooks().setVisible(true);
            case "Reports" -> new Reports().setVisible(true);
            case "Logout" -> logout();
        }
    }

    private void logout() {
        if (JOptionPane.showConfirmDialog(this, "Logout now?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
            new Login().setVisible(true);
        }
    }

    private void connectDB() {
        try { conn = DriverManager.getConnection("jdbc:mysql://localhost/library_db", "root", ""); }
        catch (SQLException e) { JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage()); }
    }

    private void updateDashboard() {
        if (conn == null) return;
        try (Statement s = conn.createStatement()) {
            availableBooksLabel.setText(queryValue(s, "SELECT SUM(available_copies) FROM books"));
            borrowedBooksLabel.setText(queryValue(s, "SELECT COUNT(*) FROM issued_books WHERE return_date IS NULL"));
            overdueBooksLabel.setText(queryValue(s, "SELECT COUNT(*) FROM issued_books WHERE return_date IS NULL AND due_date < CURDATE()"));
            updatedLabel.setText("Updated: " + new java.util.Date().toString());
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    private String queryValue(Statement s, String q) throws SQLException {
        try (ResultSet rs = s.executeQuery(q)) {
            return rs.next() && rs.getString(1) != null ? rs.getString(1) : "0";
        }
    }

    // === Responsive adjustments ===
    private void adjustLayout() {
        int w = getWidth();
        int cols = (w > 1300) ? 3 : (w > 900) ? 2 : 1;
        centerPanel.setLayout(new GridLayout(1, cols, 40, 40));

        int pad = (w > 1300) ? 160 : (w > 900) ? 100 : 40;
        centerPanel.setBorder(new EmptyBorder(60, pad, 20, pad));

        for (Component c : centerPanel.getComponents()) {
            if (c instanceof JPanel panel) {
                Dimension size = new Dimension(Math.max(250, w / 5), Math.max(140, getHeight() / 6));
                panel.setPreferredSize(size);
                for (Component inner : panel.getComponents()) {
                    if (inner instanceof JLabel label && label.getText().matches("\\d+")) {
                        label.setFont(new Font("Segoe UI", Font.BOLD, Math.max(36, w / 30)));
                    }
                }
            }
        }

        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void updateLogoSize() {
        if (logoLabel != null && originalLogoImage != null) {
            int h = Math.min(Math.max(80, Math.min(getHeight() / 8, getWidth() / 6)), 250);
            logoLabel.setIcon(new ImageIcon(scaleImage(originalLogoImage, h)));
        }
    }

    private Image scaleImage(Image img, int height) {
        int w = img.getWidth(null) * height / img.getHeight(null);
        BufferedImage resized = new BufferedImage(w, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(img, 0, 0, w, height, null);
        g2.dispose();
        return resized;
    }

    // Inner gradient panel for cards
    static class GradientPanel extends JPanel {
        private final Color c1, c2;
        GradientPanel(Color c1, Color c2) { this.c1 = c1; this.c2 = c2; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard("admin", "admin").setVisible(true));
    }
}
