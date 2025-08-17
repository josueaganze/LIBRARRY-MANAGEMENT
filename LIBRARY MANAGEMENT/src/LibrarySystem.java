import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibrarySystem extends JFrame {

    private DatabaseManager dbManager = new DatabaseManager();
    private JTable bookTable;
    private DefaultTableModel tableModel;

    private JTextField idField, titleField, authorField;

    public LibrarySystem() {
        setTitle("Library Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();
        refreshTable();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        // Control Panel
        JPanel controlPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        
        controlPanel.add(new JLabel("ID (for Update/Delete):"));
        idField = new JTextField();
        controlPanel.add(idField);
        
        controlPanel.add(new JLabel("Title:"));
        titleField = new JTextField();
        controlPanel.add(titleField);

        controlPanel.add(new JLabel("Author:"));
        authorField = new JTextField();
        controlPanel.add(authorField);

        JButton createBtn = new JButton("Add Book");
        createBtn.addActionListener(e -> createBook());
        controlPanel.add(createBtn);

        JButton updateBtn = new JButton("Update Book");
        updateBtn.addActionListener(e -> updateBook());
        controlPanel.add(updateBtn);

        JButton deleteBtn = new JButton("Delete Book");
        deleteBtn.addActionListener(e -> deleteBook());
        controlPanel.add(deleteBtn);
        
        JButton borrowBtn = new JButton("Borrow Book");
        borrowBtn.addActionListener(e -> updateBookStatus(false));
        controlPanel.add(borrowBtn);
        
        JButton returnBtn = new JButton("Return Book");
        returnBtn.addActionListener(e -> updateBookStatus(true));
        controlPanel.add(returnBtn);

        add(controlPanel, BorderLayout.WEST);

        // Book Display Table
        String[] columnNames = {"ID", "Title", "Author", "Available"};
        tableModel = new DefaultTableModel(columnNames, 0);
        bookTable = new JTable(tableModel);
        
        JScrollPane scrollPane = new JScrollPane(bookTable);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void createBook() {
        try {
            String title = titleField.getText();
            String author = authorField.getText();
            if (title.isEmpty() || author.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and Author cannot be empty.");
                return;
            }
            Book newBook = new Book(0, title, author, true);
            dbManager.addBook(newBook);
            refreshTable();
            clearFields();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding book: " + ex.getMessage());
        }
    }

    private void updateBook() {
        try {
            int selectedRow = bookTable.getSelectedRow();
            if (selectedRow < 0) {
                 JOptionPane.showMessageDialog(this, "Please select a row to update.");
                 return;
            }
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String title = titleField.getText();
            String author = authorField.getText();
            
            Book updatedBook = new Book(id, title.isEmpty() ? (String) tableModel.getValueAt(selectedRow, 1) : title,
                                         author.isEmpty() ? (String) tableModel.getValueAt(selectedRow, 2) : author,
                                         (boolean) tableModel.getValueAt(selectedRow, 3));
            
            dbManager.updateBook(updatedBook);
            refreshTable();
            clearFields();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid ID. Please enter a number.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating book: " + ex.getMessage());
        }
    }

    private void deleteBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                dbManager.deleteBook(id);
                refreshTable();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a book to delete.");
        }
    }

    private void updateBookStatus(boolean isAvailable) {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                boolean currentStatus = (boolean) tableModel.getValueAt(selectedRow, 3);
                
                if (currentStatus != isAvailable) {
                    dbManager.updateBookStatus(id, isAvailable);
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Book status updated successfully.");
                } else {
                    String statusText = isAvailable ? "available" : "borrowed";
                    JOptionPane.showMessageDialog(this, "Book is already " + statusText + ".");
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error updating book status: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a book to update its status.");
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0); // Clear table
        try {
            List<Book> books = dbManager.getAllBooks();
            for (Book book : books) {
                Object[] row = new Object[]{book.getId(), book.getTitle(), book.getAuthor(), book.isAvailable()};
                tableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
        }
    }

    private void clearFields() {
        idField.setText("");
        titleField.setText("");
        authorField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySystem().setVisible(true));
    }
    
    //--------------------------------------------------------------------------------------
    // Nested Classes for Data Model and Database Management
    //--------------------------------------------------------------------------------------
    
    private static class Book {
        private int id;
        private String title;
        private String author;
        private boolean isAvailable;

        public Book(int id, String title, String author, boolean isAvailable) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.isAvailable = isAvailable;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }
    }

    private static class DatabaseManager {
        private static final String URL = "jdbc:mysql://localhost:3306/joshualib";
        private static final String USER = "root";
        private static final String PASSWORD = "jo2000";

        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        public void addBook(Book book) throws SQLException {
            String sql = "INSERT INTO books (title, author, is_available) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, book.getTitle());
                pstmt.setString(2, book.getAuthor());
                pstmt.setBoolean(3, book.isAvailable());
                pstmt.executeUpdate();
            }
        }

        public List<Book> getAllBooks() throws SQLException {
            List<Book> books = new ArrayList<>();
            String sql = "SELECT * FROM books";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("is_available")
                    ));
                }
            }
            return books;
        }

        public void updateBook(Book book) throws SQLException {
            String sql = "UPDATE books SET title = ?, author = ?, is_available = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, book.getTitle());
                pstmt.setString(2, book.getAuthor());
                pstmt.setBoolean(3, book.isAvailable());
                pstmt.setInt(4, book.getId());
                pstmt.executeUpdate();
            }
        }

        public void deleteBook(int id) throws SQLException {
            String sql = "DELETE FROM books WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        }

        public void updateBookStatus(int id, boolean isAvailable) throws SQLException {
            String sql = "UPDATE books SET is_available = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, isAvailable);
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }
        }
    }
}