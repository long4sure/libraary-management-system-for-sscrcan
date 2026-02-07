package com.school.lms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.time.LocalDate;

public class IssueBook extends JFrame {
    private JTable table;
    private JTextField bookIdField, borrowerField, dueDateField, searchField;
    private JPanel mainCard;
    private JLabel logoLabel;
    private Image originalLogoImage;

    public IssueBook() {
        setTitle("📚 Issue Book");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));

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
        JLabel header = new JLabel("Issue Book", SwingConstants.CENTER);
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

        // Search
        searchField = new JTextField();
        JButton searchBtn = createButton("Search");
        JPanel searchPanel = new JPanel(new BorderLayout(10,10));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("🔎 Search (Title/Author/Editor/Call No.): "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        mainCard.add(searchPanel, BorderLayout.NORTH);

        // Table
        table = new JTable(new DefaultTableModel(
                new Object[]{"Book ID", "Call No.", "Accession No.", "Title", "Author/Editor", "Available"}, 0));
        configureTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(180,0,0),2,true));
        mainCard.add(scrollPane, BorderLayout.CENTER);

        // Form
        bookIdField = createField(false);
        borrowerField = createField(true);
        dueDateField = createField(true);
        dueDateField.setText(LocalDate.now().plusDays(7).toString());

        JPanel form = new JPanel(new GridLayout(1,6,10,10));
        form.setOpaque(false);
        form.add(new JLabel("Book ID:")); form.add(bookIdField);
        form.add(new JLabel("Borrower Name:")); form.add(borrowerField);
        form.add(new JLabel("Due Date (YYYY-MM-DD):")); form.add(dueDateField);

        // Buttons
        JButton issueBtn = createButton("Issue");
        JButton refreshBtn = createButton("Refresh");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
        btnPanel.setOpaque(false);
        btnPanel.add(refreshBtn); btnPanel.add(issueBtn);

        JPanel southPanel = new JPanel(new BorderLayout(10,10));
        southPanel.setOpaque(false);
        southPanel.add(form, BorderLayout.CENTER);
        southPanel.add(btnPanel, BorderLayout.SOUTH);
        mainCard.add(southPanel, BorderLayout.SOUTH);

        // Table selection
        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) bookIdField.setText(String.valueOf(table.getValueAt(r, 0)));
        });

        // Actions
        searchBtn.addActionListener(e -> loadAvailableBooks(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> loadAvailableBooks(null));
        issueBtn.addActionListener(e -> issue());

        loadAvailableBooks(null);

        // Window resize listener
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateLogoSize();
                updateTableAndForm();
            }
        });
    }

    private void loadAvailableBooks(String keyword) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);
        try (Connection c = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, call_no, accession_no, title, author, available_copies " +
                         "FROM books WHERE available_copies > 0";
            if(keyword!=null && !keyword.isEmpty())
                sql += " AND (title LIKE ? OR author LIKE ? OR call_no LIKE ? OR accession_no LIKE ?)";
            sql += " ORDER BY id DESC";

            PreparedStatement ps = c.prepareStatement(sql);
            if(keyword!=null && !keyword.isEmpty()) {
                String like="%"+keyword+"%";
                ps.setString(1,like); ps.setString(2,like); ps.setString(3,like); ps.setString(4,like);
            }
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                m.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("call_no"),
                        rs.getString("accession_no"), rs.getString("title"),
                        rs.getString("author"), rs.getInt("available_copies")
                });
            }
        } catch (Exception ex) { showErr(ex); }
    }

    private void issue() {
        try (Connection c = DatabaseConnection.getConnection()) {
            int bookId = Integer.parseInt(bookIdField.getText().trim());
            String borrower = borrowerField.getText().trim();
            LocalDate due = LocalDate.parse(dueDateField.getText().trim());

            PreparedStatement chk = c.prepareStatement("SELECT available_copies FROM books WHERE id=?");
            chk.setInt(1, bookId);
            ResultSet rs = chk.executeQuery();
            if (!rs.next()) { JOptionPane.showMessageDialog(this,"Book not found"); return; }
            if(rs.getInt(1) <= 0) { JOptionPane.showMessageDialog(this,"No copies available"); return; }

            PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO issued_books (book_id, borrower_name, issue_date, due_date) VALUES (?, ?, CURDATE(), ?)");
            ins.setInt(1, bookId); ins.setString(2, borrower);
            ins.setDate(3, java.sql.Date.valueOf(due));
            ins.executeUpdate();

            PreparedStatement upd = c.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE id=?");
            upd.setInt(1, bookId); upd.executeUpdate();

            JOptionPane.showMessageDialog(this,"✅ Book Issued Successfully!");
            loadAvailableBooks(null);
            borrowerField.setText("");
        } catch (Exception ex) { showErr(ex); }
    }

    // ===== Helpers =====
    private JTextField createField(boolean editable){
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setEnabled(editable);
        f.setBorder(new LineBorder(new Color(180,0,0)));
        return f;
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

        int[] widths = {60,120,140,400,250,80};
        for(int i=0;i<widths.length;i++){
            TableColumn col = t.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setMinWidth(widths[i]);
            col.setMaxWidth(widths[i]);
        }
    }

    private void showErr(Exception ex){
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,"⚠️ "+ex.getMessage());
    }

    // RoundedPanel
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

    // === Responsive resizing logic ===
    private void updateLogoSize() {
        if (logoLabel != null && originalLogoImage != null) {
            int h = Math.min(Math.max(60, getHeight() / 8), 250);
            logoLabel.setIcon(new ImageIcon(getScaledImage(originalLogoImage, h)));
        }
    }

    private void updateTableAndForm() {
        int w = getWidth();
        int h = getHeight();

        // Table row height
        table.setRowHeight(Math.max(25, h / 25));

        // Adjust table columns proportionally
        int[] colPercents = {5,15,15,35,25,5};
        int totalWidth = table.getParent().getWidth();
        TableColumnModel colModel = table.getColumnModel();
        for (int i=0;i<colPercents.length;i++){
            int colWidth = totalWidth * colPercents[i] / 100;
            TableColumn col = colModel.getColumn(i);
            col.setPreferredWidth(colWidth);
        }

        // Adjust form field font size
        int fontSize = Math.max(12, w / 100);
        for (Component c : mainCard.getComponents()) {
            if (c instanceof JScrollPane) continue;
            if (c instanceof JPanel panel) {
                for (Component inner : panel.getComponents()) {
                    if (inner instanceof JTextField tf) tf.setFont(new Font("Segoe UI", Font.PLAIN, fontSize));
                }
            }
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
        SwingUtilities.invokeLater(() -> new IssueBook().setVisible(true));
    }
}
