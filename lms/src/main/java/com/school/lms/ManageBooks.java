package com.school.lms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;

public class ManageBooks extends JFrame {
    private JTable table;
    private JTextField idField, callNoField, accessionNoField, titleField, authorField, totalField, availableField, searchField;
    private JPanel mainCard;
    private JLabel logoLabel;
    private Image originalLogoImage;

    public ManageBooks() {
        setTitle("📚 Manage Books");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout());

        // Window icon & ESC close
        setIconImage(new ImageIcon(getClass().getResource("/com/school/lms/sscrcanlogo.png")).getImage());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

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

        // Header with scaled logo
        JLabel headerLabel = new JLabel(" Manage Books", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(new EmptyBorder(10,0,5,0));

        try {
            originalLogoImage = new ImageIcon(getClass().getResource("/com/school/lms/sscrcanlogolong.png")).getImage();
            logoLabel = new JLabel(new ImageIcon(getScaledImage(originalLogoImage, 100)));
        } catch (Exception e) {
            logoLabel = new JLabel("Logo Missing");
        }
        logoLabel.setBorder(new EmptyBorder(5,10,5,10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        headerPanel.add(logoLabel, BorderLayout.EAST);
        background.add(headerPanel, BorderLayout.NORTH);

        // Main card
        mainCard = new RoundedPanel(25, new Color(255,255,255,240));
        mainCard.setLayout(new BorderLayout(15,15));
        mainCard.setBorder(new EmptyBorder(20,20,20,20));
        background.add(mainCard, BorderLayout.CENTER);

        // Search bar
        searchField = new JTextField();
        JButton searchBtn = createButton("Search");
        searchBtn.setPreferredSize(new Dimension(120,40));

        JPanel searchPanel = new JPanel(new BorderLayout(10,10));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("🔍 Search (Title / Author / Call No / Accession No): "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        mainCard.add(searchPanel, BorderLayout.NORTH);

        // Table setup
        table = new JTable(new DefaultTableModel(
                new Object[]{"ID","Call No.","Accession No.","Title","Author/Editor","Total","Available"}, 0));
        configureTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(180,0,0),2,true));
        mainCard.add(scrollPane, BorderLayout.CENTER);

        // Form section
        idField = createField(false);
        callNoField = createField(true);
        accessionNoField = createField(true);
        titleField = createField(true);
        authorField = createField(true);
        totalField = createField(true);
        availableField = createField(true);

        JPanel formPanel = new JPanel(new GridLayout(4,6,10,10));
        formPanel.setOpaque(false);
        formPanel.add(new JLabel("ID:")); formPanel.add(idField);
        formPanel.add(new JLabel("Call No:")); formPanel.add(callNoField);
        formPanel.add(new JLabel("Accession No:")); formPanel.add(accessionNoField);
        formPanel.add(new JLabel("Title:")); formPanel.add(titleField);
        formPanel.add(new JLabel("Author/Editor:")); formPanel.add(authorField);
        formPanel.add(new JLabel("Total Copies:")); formPanel.add(totalField);
        formPanel.add(new JLabel("Available Copies:")); formPanel.add(availableField);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
        buttonPanel.setOpaque(false);
        JButton addBtn = createButton("Add"), updateBtn = createButton("Update"),
                deleteBtn = createButton("Delete"), clearBtn = createButton("Clear"),
                refreshBtn = createButton("Refresh"), closeBtn = createButton("Close");
        buttonPanel.add(addBtn); buttonPanel.add(updateBtn); buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn); buttonPanel.add(refreshBtn); buttonPanel.add(closeBtn);

        JPanel southPanel = new JPanel(new BorderLayout(10,10));
        southPanel.setOpaque(false);
        southPanel.add(formPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainCard.add(southPanel, BorderLayout.SOUTH);

        // Actions
        addBtn.addActionListener(e -> addBook());
        updateBtn.addActionListener(e -> updateBook());
        deleteBtn.addActionListener(e -> deleteBook());
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> loadBooks(null));
        closeBtn.addActionListener(e -> dispose());
        searchBtn.addActionListener(e -> loadBooks(searchField.getText().trim()));
        searchField.addActionListener(e -> loadBooks(searchField.getText().trim()));
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        loadBooks(null);

        // Window resize listener
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateLogoSize();
                updateTableAndForm();
            }
        });
    }

    // ===== Helpers =====
    private JTextField createField(boolean enabled) {
        JTextField t = new JTextField();
        t.setEnabled(enabled);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setBorder(new LineBorder(new Color(180,0,0)));
        return t;
    }

    private JButton createButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD,14));
        b.setBackground(new Color(255,204,0));
        b.setForeground(new Color(60,0,0));
        b.setBorder(new LineBorder(new Color(110,0,0),2,true));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(new Color(255,230,100)); }
            public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(new Color(255,204,0)); }
        });
        return b;
    }

    private void configureTable(JTable t) {
        t.setFont(new Font("Segoe UI",Font.PLAIN,14));
        t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));
        t.setRowHeight(35);
        t.setGridColor(new Color(220,220,220));
        t.setSelectionBackground(new Color(255,230,140));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = {60,120,140,450,280,80,90};
        for(int i=0;i<widths.length;i++){
            TableColumn col = t.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setMinWidth(widths[i]);
            col.setMaxWidth(widths[i]);
        }

        t.getColumnModel().getColumn(3).setCellRenderer(new MultiLineCellRenderer());
        t.getColumnModel().getColumn(4).setCellRenderer(new MultiLineCellRenderer());

        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFocus, int row, int col){
                Component c = super.getTableCellRendererComponent(tbl,val,isSel,hasFocus,row,col);
                if(!isSel) c.setBackground(row%2==0?new Color(255,255,245):new Color(245,245,230));
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, defaultRenderer);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    // ===== Data Logic =====
    private void loadBooks(String keyword){
        DefaultTableModel m=(DefaultTableModel)table.getModel();
        m.setRowCount(0);
        String sql="SELECT * FROM books" + ((keyword!=null&&!keyword.isEmpty())?" WHERE title LIKE ? OR author LIKE ? OR call_no LIKE ? OR accession_no LIKE ?":"") + " ORDER BY title ASC";
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement(sql)){
            if(keyword!=null&&!keyword.isEmpty()){
                String like="%"+keyword+"%";
                ps.setString(1,like); ps.setString(2,like); ps.setString(3,like); ps.setString(4,like);
            }
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                m.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("call_no"), rs.getString("accession_no"),
                        rs.getString("title"), rs.getString("author"),
                        rs.getInt("total_copies"), rs.getInt("available_copies")
                });
            }
        } catch(Exception ex){ showErr(ex); }
    }

    private void fillFormFromSelection(){
        int r=table.getSelectedRow();
        if(r<0) return;
        idField.setText(String.valueOf(table.getValueAt(r,0)));
        callNoField.setText(String.valueOf(table.getValueAt(r,1)));
        accessionNoField.setText(String.valueOf(table.getValueAt(r,2)));
        titleField.setText(String.valueOf(table.getValueAt(r,3)));
        authorField.setText(String.valueOf(table.getValueAt(r,4)));
        totalField.setText(String.valueOf(table.getValueAt(r,5)));
        availableField.setText(String.valueOf(table.getValueAt(r,6)));
    }

    private void addBook() { upsertBook(false); }
    private void updateBook() { if(!idField.getText().isEmpty()) upsertBook(true); }
    private void upsertBook(boolean update){
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement(
                    update?"UPDATE books SET call_no=?,accession_no=?,title=?,author=?,total_copies=?,available_copies=? WHERE id=?"
                           :"INSERT INTO books (call_no,accession_no,title,author,total_copies,available_copies) VALUES (?,?,?,?,?,?)")){
            int total=parseInt(totalField.getText()), avail=parseInt(availableField.getText());
            if(avail>total){ JOptionPane.showMessageDialog(this,"❗ Available cannot exceed Total."); return; }

            ps.setString(1,callNoField.getText().trim());
            ps.setString(2,accessionNoField.getText().trim());
            ps.setString(3,titleField.getText().trim());
            ps.setString(4,authorField.getText().trim());
            ps.setInt(5,total); ps.setInt(6,avail);
            if(update) ps.setInt(7,parseInt(idField.getText()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, update?"✅ Book Updated Successfully!":"✅ Book Added Successfully!");
            clearForm(); loadBooks(null);
        } catch(Exception ex){ showErr(ex); }
    }

    private void deleteBook(){
        int r=table.getSelectedRow();
        if(r<0) return;
        int id=(int)table.getValueAt(r,0);
        if(JOptionPane.showConfirmDialog(this,"Delete book ID "+id+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement("DELETE FROM books WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"🗑️ Deleted Successfully!");
            clearForm(); loadBooks(null);
        } catch(Exception ex){ showErr(ex); }
    }

    private void clearForm(){
        idField.setText(""); callNoField.setText(""); accessionNoField.setText("");
        titleField.setText(""); authorField.setText(""); totalField.setText(""); availableField.setText("");
        table.clearSelection();
    }

    private int parseInt(String s){ try{return Integer.parseInt(s.trim().isEmpty()?"0":s.trim());}catch(Exception e){return 0;} }
    private void showErr(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(this,"⚠️ "+ex.getMessage()); }

    // ===== RoundedPanel =====
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

    // ===== Multi-line Cell Renderer =====
    static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer{
        MultiLineCellRenderer(){ setLineWrap(true); setWrapStyleWord(true); setOpaque(true); setFont(new Font("Segoe UI",Font.PLAIN,14)); }
        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
            setText(value==null?"":value.toString());
            setSize(table.getColumnModel().getColumn(column).getWidth(),getPreferredSize().height);
            int newHeight=getPreferredSize().height+10;
            if(table.getRowHeight(row)!=newHeight) table.setRowHeight(row,newHeight);
            setBackground(isSelected?table.getSelectionBackground():(row%2==0?new Color(255,255,245):new Color(245,245,230)));
            return this;
        }
    }

    // ===== Responsive resizing logic =====
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
        int[] colPercents = {5,15,15,35,25,5,5};
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

    public static void main(String[] args){ SwingUtilities.invokeLater(() -> new ManageBooks().setVisible(true)); }
}
