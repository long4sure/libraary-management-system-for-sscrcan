package com.school.lms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;

public class SearchBooks extends JFrame {
    private JTextField searchField;
    private JTable resultsTable;
    private JLabel logoLabel;
    private Image originalLogoImage;
    private RoundedPanel mainCard;

    public SearchBooks() {
        setTitle(" Search Books");
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

        // Header with logo
        JLabel header = new JLabel("🔎 Search Books", SwingConstants.CENTER);
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

        // Search panel
        JPanel topPanel = new JPanel(new BorderLayout(10,10));
        topPanel.setOpaque(false);
        searchField = new JTextField();
        JButton searchBtn = createButton("Search");
        JButton refreshBtn = createButton("Show All");

        JPanel searchInner = new JPanel(new BorderLayout(5,5));
        searchInner.setOpaque(false);
        searchInner.add(new JLabel("Search (Title / Author / Editor / Call No.): "), BorderLayout.WEST);
        searchInner.add(searchField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
        btnPanel.setOpaque(false);
        btnPanel.add(refreshBtn);
        btnPanel.add(searchBtn);
        searchInner.add(btnPanel, BorderLayout.EAST);

        topPanel.add(searchInner, BorderLayout.CENTER);
        mainCard.add(topPanel, BorderLayout.NORTH);

        // Table
        resultsTable = new JTable(new DefaultTableModel(
                new String[]{"Book ID", "Call No.", "Accession No.", "Title", "Author/Editor", "Available Copies"}, 0
        ));
        configureTable(resultsTable);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(new LineBorder(new Color(180,0,0),2,true));
        mainCard.add(scrollPane, BorderLayout.CENTER);

        // Event listeners
        searchBtn.addActionListener(e -> searchBooks(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> loadAllBooks());
        searchField.addActionListener(e -> searchBooks(searchField.getText().trim()));

        // Load all books at startup
        loadAllBooks();

        // Responsive resizing
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateLogoSize();
                updateTableSize();
            }
        });
    }

    private void loadAllBooks(){
        DefaultTableModel model=(DefaultTableModel)resultsTable.getModel();
        model.setRowCount(0);
        try(Connection conn=DatabaseConnection.getConnection()){
            String sql="SELECT id, call_no, accession_no, title, author, available_copies FROM books ORDER BY title ASC";
            PreparedStatement stmt=conn.prepareStatement(sql);
            ResultSet rs=stmt.executeQuery();
            while(rs.next()){
                model.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("call_no"), rs.getString("accession_no"),
                        rs.getString("title"), rs.getString("author"), rs.getInt("available_copies")
                });
            }
        } catch(Exception e){ showError(e); }
    }

    private void searchBooks(String keyword){
        DefaultTableModel model=(DefaultTableModel)resultsTable.getModel();
        model.setRowCount(0);
        if(keyword.isEmpty()){ loadAllBooks(); return; }

        try(Connection conn=DatabaseConnection.getConnection()){
            String sql="SELECT id, call_no, accession_no, title, author, available_copies FROM books " +
                    "WHERE title LIKE ? OR author LIKE ? OR call_no LIKE ? OR accession_no LIKE ? ORDER BY title ASC";
            PreparedStatement stmt=conn.prepareStatement(sql);
            String like="%"+keyword+"%";
            for(int i=1;i<=4;i++) stmt.setString(i, like);
            ResultSet rs=stmt.executeQuery();
            while(rs.next()){
                model.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("call_no"), rs.getString("accession_no"),
                        rs.getString("title"), rs.getString("author"), rs.getInt("available_copies")
                });
            }
        } catch(Exception e){ showError(e); }
    }

    private void showError(Exception e){
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JButton createButton(String text){
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setFocusPainted(false);
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
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        t.setRowHeight(35);
        t.setGridColor(new Color(220,220,220));
        t.setSelectionBackground(new Color(255,230,140));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void updateLogoSize() {
        if (logoLabel != null && originalLogoImage != null) {
            int h = Math.min(Math.max(60, getHeight() / 8), 200);
            logoLabel.setIcon(new ImageIcon(getScaledImage(originalLogoImage, h)));
        }
    }

    private void updateTableSize() {
        int w = mainCard.getWidth();
        int[] colPercents = {5,10,12,40,25,8};
        resultsTable.setRowHeight(Math.max(25, getHeight()/30));
        TableColumnModel colModel = resultsTable.getColumnModel();
        for(int i=0;i<colModel.getColumnCount();i++){
            if(i<colPercents.length) colModel.getColumn(i).setPreferredWidth(w*colPercents[i]/100);
        }
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
        SwingUtilities.invokeLater(() -> new SearchBooks().setVisible(true));
    }
}
