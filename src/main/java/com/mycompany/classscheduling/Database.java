package com.mycompany.classscheduling;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:sqlite:school_scheduling.db";

    public static Connection connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(URL);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Connection Error: " + e.getMessage());
            return null;
        }
    }

    public static void initialize() {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL);";
        String subjectsTable = "CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, professor TEXT, startTime INTEGER, endTime INTEGER, dayOfWeek TEXT, department TEXT, username TEXT);";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(subjectsTable);
            
            try {
                stmt.execute("ALTER TABLE subjects ADD COLUMN dayOfWeek TEXT;");
            } catch (SQLException e) {
                // Column already exists
            }

            stmt.execute("INSERT OR IGNORE INTO users VALUES ('admin', '1234')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void handleLogin(String username, String password, JFrame currentFrame) {
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(currentFrame, "Please fill all fields");
            return;
        }
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                new homePage(username).setVisible(true);
                currentFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(currentFrame, "Invalid credentials");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(currentFrame, "Login Error: " + e.getMessage());
        }
    }

    public static void handleRegister(String username, String password, JFrame currentFrame) {
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(currentFrame, "Please fill all fields");
            return;
        }
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(currentFrame, "Registration Successful!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(currentFrame, "Error: User might already exist.");
        }
    }

    public static void clearLoginForm(JTextField userField, JPasswordField passField) {
        userField.setText("");
        passField.setText("");
    }

    public static void heapSort(List<SubjectData> list) {
        int n = list.size();
        for (int i = n / 2 - 1; i >= 0; i--) heapify(list, n, i);
        for (int i = n - 1; i > 0; i--) {
            SubjectData temp = list.get(0);
            list.set(0, list.get(i));
            list.set(i, temp);
            heapify(list, i, 0);
        }
    }

    private static void heapify(List<SubjectData> list, int n, int i) {
        int largest = i;
        int l = 2 * i + 1;
        int r = 2 * i + 2;
        if (l < n && list.get(l).start > list.get(largest).start) largest = l;
        if (r < n && list.get(r).start > list.get(largest).start) largest = r;
        if (largest != i) {
            SubjectData swap = list.get(i);
            list.set(i, list.get(largest));
            list.set(largest, swap);
            heapify(list, n, largest);
        }
    }

    public static void loadScheduleToTable(JTable table, String username, JFrame currentFrame) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        List<SubjectData> subjects = new ArrayList<>();

        String sql = "SELECT * FROM subjects WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                subjects.add(new SubjectData(
                    rs.getString("name"),
                    rs.getString("professor"),
                    rs.getInt("startTime"),
                    rs.getInt("endTime"),
                    rs.getString("department"),
                    rs.getString("dayOfWeek") 
                ));
            }
            heapSort(subjects);
            for (SubjectData s : subjects) {
                model.addRow(new Object[]{
                    s.name, 
                    s.prof, 
                    s.day, 
                    formatTime(s.start), 
                    formatTime(s.end), 
                    s.department
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(currentFrame, "Error: " + e.getMessage());
        }
    }

    public static void setupTimeSpinners(JSpinner s1, JSpinner s2) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0);
        s1.setModel(new SpinnerDateModel(cal.getTime(), null, null, Calendar.MINUTE));
        s1.setEditor(new JSpinner.DateEditor(s1, "HH:mm"));

        cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 20);
        s2.setModel(new SpinnerDateModel(cal.getTime(), null, null, Calendar.MINUTE));
        s2.setEditor(new JSpinner.DateEditor(s2, "HH:mm"));
    }

    public static int getSpinnerTimeAsInt(JSpinner spinner) {
        Object value = spinner.getValue();
        if (value instanceof java.util.Date) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime((java.util.Date) value);
            return (cal.get(java.util.Calendar.HOUR_OF_DAY) * 100) + cal.get(java.util.Calendar.MINUTE);
        } 
        if (value instanceof Integer) {
            return ((Integer) value) * 100;
        }
        return 0;
    }

    public static String formatTime(int time) {
        int h = time / 100;
        int m = time % 100;
        String period = h >= 12 ? "PM" : "AM";
        int displayH = (h > 12) ? h - 12 : (h == 0 ? 12 : h);
        return String.format("%d:%02d %s", displayH, m, period);
    }

    public static void setupDepartmentDropdown(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        String[] depts = {"Computer Engineering", "Computer Science", "Information Technology", "Hospitality Management", "Tourism"};
        for (String d : depts) comboBox.addItem(d);
    }

    public static void setupDayDropdown(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (String d : days) comboBox.addItem(d);
    }

    public static void clearAddSubjectForm(JTextField f1, JTextField f2, JSpinner s1, JSpinner s2) {
        f1.setText(""); f2.setText(""); setupTimeSpinners(s1, s2);
    }

    public static void handleAddSubject(String name, String prof, int start, int end, String day, String dept, String user, JFrame frame) {
        if (name.isEmpty() || prof.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "All fields required!");
            return;
        }
        
        String checkSql = "SELECT COUNT(*) FROM subjects WHERE username = ? AND dayOfWeek = ? AND startTime < ? AND endTime > ?";
        try (Connection conn = connect(); PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
            checkPstmt.setString(1, user);
            checkPstmt.setString(2, day);
            checkPstmt.setInt(3, end);
            checkPstmt.setInt(4, start);
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(frame, "Conflict: A class is already scheduled on " + day + " at this time!");
                return;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String sql = "INSERT INTO subjects(name, professor, startTime, endTime, dayOfWeek, department, username) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name); 
            pstmt.setString(2, prof);
            pstmt.setInt(3, start); 
            pstmt.setInt(4, end);
            pstmt.setString(5, day); 
            pstmt.setString(6, dept); 
            pstmt.setString(7, user);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Subject Added!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }
    
    public static void goToSchedulePage(String user, JFrame frame) {
        new scheduleTable(user).setVisible(true);
        frame.dispose();
    }

    public static void logout(JFrame frame) {
        new loginPage().setVisible(true);
        frame.dispose();
    }

    public static void handleRemoveSubject(String name, String prof, String day, String dept, String user, JFrame frame) {
        String sql = "DELETE FROM subjects WHERE name = ? AND professor = ? AND dayOfWeek = ? AND department = ? AND username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, prof);
            pstmt.setString(3, day);
            pstmt.setString(4, dept);
            pstmt.setString(5, user);
            int deletedRows = pstmt.executeUpdate();
            if (deletedRows > 0) {
                JOptionPane.showMessageDialog(frame, "Subject removed successfully!");
            } else {
                JOptionPane.showMessageDialog(frame, "Error: Subject not found.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Database Error: " + e.getMessage());
        }
    }
}