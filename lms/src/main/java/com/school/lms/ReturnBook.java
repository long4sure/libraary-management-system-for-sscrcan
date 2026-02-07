package com.school.lms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;

public class ReturnBook extends JFrame {
    private JTable table;
    private JTextField searchField;
    private JLabel logoLabel;
    private Image originalLogoImage;
    private RoundedPanel mainCard;

    public ReturnBook() {
        setTitle(" Return Book");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900,600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // ESC closes window
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Window icon
        setIconImage(new ImageIcon(getClass().getResource("/com/school/lms/sscrcanlogo.png")).getImage());

        // Background gradient
        JPanel background = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(110, 0, 0),
                        0, getHeight(), new Color(210, 160, 0)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        add(background);

        // Header
        JLabel header = new JLabel("📖 Return Book", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 28));
        header.setForeground(Color.WHITE);
        header.setBorder(new EmptyBorder(10,0,10,0));

        try {
            originalLogoImage = new ImageIcon(getClass().getResource("/com/school/lms/sscrcanlogolong.png")).getImage();
            logoLabel = new JLabel(new ImageIcon(getScaledImage(originalLogoImage, 90)));
        } catch (Exception e) {
            logoLabel = new JLabel("Logo Missing");
        }
        logoLabel.setBorder(new EmptyBorder(5,10,5,10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(header, BorderLayout.CENTER);
        headerPanel.add(logoLabel, BorderLayout.EAST);
        background.add(headerPanel, BorderLayout.NORTH);

        // Main card
        mainCard = new RoundedPanel(25, new Color(255,255,255,240));
        mainCard.setLayout(new BorderLayout(15,15));
        mainCard.setBorder(new EmptyBorder(20,20,20,20));
        background.add(mainCard, BorderLayout.CENTER);

        // Top search panel
        JPanel top = new JPanel(new BorderLayout(8,8));
        top.setOpaque(false);
        searchField = new JTextField();
        JButton searchBtn = createButton("Search Borrower/Title/Call No.");
        JButton refreshBtn = createButton("Refresh");
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
        topRight.setOpaque(false);
        topRight.add(refreshBtn);
        topRight.add(searchBtn);
        top.add(searchField, BorderLayout.CENTER);
        top.add(topRight, BorderLayout.EAST);
        mainCard.add(top, BorderLayout.NORTH);

        // Table
        table = new JTable();
        table.setModel(new DefaultTableModel(
                new Object[]{"Txn ID","Book ID","Call No.","Accession No.","Title","Author/Editor","Borrower","Issue Date","Due Date"}, 0));
        configureTable(table);
        mainCard.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom return button
        JButton returnBtn = createButton("Mark as Returned");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        south.setOpaque(false);
        south.add(returnBtn);
        mainCard.add(south, BorderLayout.SOUTH);

        // Action listeners
        refreshBtn.addActionListener(e -> loadOutstanding(null));
        searchBtn.addActionListener(e -> loadOutstanding(searchField.getText().trim()));
        returnBtn.addActionListener(e -> markReturned());

        loadOutstanding(null);

        // Responsive resizing
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateLogoSize();
                updateTableSize();
            }
        });
    }

    private void loadOutstanding(String q) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);
        String sql =
            "SELECT ib.id AS txn_id, b.id AS book_id, b.call_no, b.accession_no, b.title, b.author, " +
            "ib.borrower_name, ib.issue_date, ib.due_date " +
            "FROM issued_books ib JOIN books b ON ib.book_id=b.id " +
            "WHERE ib.return_date IS NULL ";
        if (q != null && !q.isEmpty()) {
            sql += "AND (ib.borrower_name LIKE ? OR b.title LIKE ? OR b.call_no LIKE ? OR b.accession_no LIKE ?) ";
        }
        sql += "ORDER BY ib.issue_date DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (q != null && !q.isEmpty()) {
                String like = "%" + q + "%";
                ps.setString(1, like); ps.setString(2, like);
                ps.setString(3, like); ps.setString(4, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                m.addRow(new Object[]{
                        rs.getInt("txn_id"),
                        rs.getInt("book_id"),
                        rs.getString("call_no"),
                        rs.getString("accession_no"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("borrower_name"),
                        rs.getDate("issue_date"),
                        rs.getDate("due_date")
                });
            }
        } catch (Exception ex) { showErr(ex); }
    }

    private void markReturned() {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a row first"); return; }
        int txnId = (int) table.getValueAt(r, 0);
        int bookId = (int) table.getValueAt(r, 1);

        if (JOptionPane.showConfirmDialog(this, "Confirm return of this book?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try (Connection c = DatabaseConnection.getConnection()) {
            PreparedStatement upTxn = c.prepareStatement("UPDATE issued_books SET return_date = CURDATE() WHERE id=?");
            upTxn.setInt(1, txnId);
            upTxn.executeUpdate();

            PreparedStatement upBook = c.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE id=?");
            upBook.setInt(1, bookId);
            upBook.executeUpdate();

            JOptionPane.showMessageDialog(this, "Book successfully marked as returned!");
            loadOutstanding(null);
        } catch (Exception ex) { showErr(ex); }
    }

    private JButton createButton(String text){
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD,14));
        b.setBackground(new Color(255,204,0));
        b.setForeground(new Color(60,0,0));
        b.setBorder(new LineBorder(new Color(110,0,0),2,true));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e){ b.setBackground(new Color(255,230,100)); }
            public void mouseExited(java.awt.event.MouseEvent e){ b.setBackground(new Color(255,204,0)); }
        });
        return b;
    }

    private void configureTable(JTable t){
        t.setFont(new Font("Segoe UI",Font.PLAIN,14));
        t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));
        t.setRowHeight(35);
        t.setSelectionBackground(new Color(255,230,140));
        t.setGridColor(new Color(220,220,220));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    private void updateLogoSize() {
        if (logoLabel != null && originalLogoImage != null) {
            int h = Math.min(Math.max(60, getHeight() / 8), 200);
            logoLabel.setIcon(new ImageIcon(getScaledImage(originalLogoImage, h)));
        }
    }

    private void updateTableSize() {
        int w = mainCard.getWidth();
        int[] colPercents = {5,5,10,10,25,20,10,7,8}; // relative widths
        table.setRowHeight(Math.max(25, getHeight()/25));
        TableColumnModel colModel = table.getColumnModel();
        for(int i=0;i<colModel.getColumnCount();i++){
            if(i<colPercents.length) colModel.getColumn(i).setPreferredWidth(w*colPercents[i]/100);
        }
    }

    private void showErr(Exception ex){
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,"⚠️ "+ex.getMessage());
    }

    static class RoundedPanel extends JPanel{
        private int radius; private Color bg;
        RoundedPanel(int r, Color c){ radius=r; bg=c; setOpaque(false);}
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);
            super.paintComponent(g);
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

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new ReturnBook().setVisible(true));
    }
}
