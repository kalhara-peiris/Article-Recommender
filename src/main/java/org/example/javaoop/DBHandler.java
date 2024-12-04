package org.example.javaoop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class DBHandler {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private Connection connection;

    public DBHandler() {
        initializeConnection();
    }
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    private void initializeConnection() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    // User Management Methods
    public ObservableList<User> getAllUsers() throws SQLException {
        ObservableList<User> users = FXCollections.observableArrayList();
        String query = "SELECT username, password FROM user";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getString("username"),
                        rs.getString("password")
                ));
            }
        }
        return users;
    }

    public boolean deleteUser(String username) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // Delete user preferences
            String deletePreference = "DELETE FROM userpreference WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deletePreference)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }

            // Delete user interactions
            String deleteInteractions = "DELETE FROM userinteraction WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteInteractions)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }

            // Delete user
            String deleteUser = "DELETE FROM user WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteUser)) {
                pstmt.setString(1, username);
                int affected = pstmt.executeUpdate();

                if (affected > 0) {
                    connection.commit();
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public ObservableList<User> searchUsers(String searchText) throws SQLException {
        ObservableList<User> users = FXCollections.observableArrayList();
        String query = "SELECT username, password FROM user WHERE username LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "%" + searchText + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                            rs.getString("username"),
                            rs.getString("password")
                    ));
                }
            }
        }
        return users;
    }

    // Article Management Methods
    public ObservableList<Article> getAllArticles() throws SQLException {
        ObservableList<Article> articles = FXCollections.observableArrayList();
        String query = "SELECT articleId, title, url FROM article";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                articles.add(new Article(
                        rs.getString("articleId"),
                        rs.getString("title"),
                        rs.getString("url")
                ));
            }
        }
        return articles;
    }

    public boolean deleteArticle(String articleId) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // Delete article categories
            String deleteCategories = "DELETE FROM articlecategory WHERE articleId = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteCategories)) {
                pstmt.setString(1, articleId);
                pstmt.executeUpdate();
            }

            // Delete user interactions with the article
            String deleteInteractions = "DELETE FROM userinteraction WHERE articleId = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteInteractions)) {
                pstmt.setString(1, articleId);
                pstmt.executeUpdate();
            }

            // Delete article
            String deleteArticle = "DELETE FROM article WHERE articleId = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteArticle)) {
                pstmt.setString(1, articleId);
                int affected = pstmt.executeUpdate();

                if (affected > 0) {
                    connection.commit();
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public ObservableList<Article> searchArticles(String searchText) throws SQLException {
        ObservableList<Article> articles = FXCollections.observableArrayList();
        String query = "SELECT articleId, title, url FROM article WHERE title LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "%" + searchText + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    articles.add(new Article(
                            rs.getString("articleId"),
                            rs.getString("title"),
                            rs.getString("url")
                    ));
                }
            }
        }
        return articles;
    }

    // Count Methods
    public int getTotalArticleCount() throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM Article";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    public int getTotalUserCount() throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM user";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    public int getUncategorizedArticleCount() throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM Articletest a " +
                "WHERE NOT EXISTS (SELECT 1 FROM ArticleCategorytest ac WHERE ac.ArticleID = a.ArticleID)";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    // User Interaction Methods
    public void recordInteraction(String username, String articleId, String interactionType) throws SQLException {
        String query = "INSERT INTO userinteraction (interactionID, username, ArticleID, interactionType) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, username);
            pstmt.setString(3, articleId);
            pstmt.setString(4, interactionType);
            pstmt.executeUpdate();
        }
    }

    public boolean hasReadArticle(String username, String articleId) throws SQLException {
        String query = "SELECT ArticleID FROM userinteraction WHERE username = ? AND ArticleID = ? AND interactionType = 'READ'";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, articleId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public void updatePreferenceScore(String username, String categoryId, int scoreChange) throws SQLException {
        String query = "INSERT INTO userpreference (username, categoryID, preferenceScore) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE preferenceScore = preferenceScore + ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, categoryId);
            pstmt.setInt(3, scoreChange);
            pstmt.setInt(4, scoreChange);
            pstmt.executeUpdate();
        }
    }

    // Category Management Methods
    public String getCategoryForArticle(String articleId) throws SQLException {
        String query = "SELECT categoryID FROM ArticleCategory WHERE ArticleID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, articleId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("categoryID") : null;
        }
    }

    public Map<String, Double> getUserPreferences(String username) throws SQLException {
        Map<String, Double> preferences = new HashMap<>();
        String query = "SELECT categoryID, preferenceScore FROM userpreference WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                preferences.put(
                        rs.getString("categoryID"),
                        rs.getDouble("preferenceScore")
                );
            }
        }
        return preferences;
    }

    public List<Article> getUnreadArticles(String username) throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT a.ArticleID, a.title, a.url, a.categoryID " +
                "FROM ArticleCategory a " +
                "WHERE a.ArticleID NOT IN " +
                "(SELECT ArticleID FROM userinteraction WHERE username = ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(new Article(
                        rs.getString("ArticleID"),
                        rs.getString("title"),
                        rs.getString("url"),
                        rs.getString("categoryID")
                ));
            }
        }
        return articles;
    }

    public List<Article> getRandomArticles(int limit) throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT ArticleID, title, url, categoryID FROM ArticleCategory ORDER BY RAND() LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(new Article(
                        rs.getString("ArticleID"),
                        rs.getString("title"),
                        rs.getString("url"),
                        rs.getString("categoryID")
                ));
            }
        }
        return articles;
    }

    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean isUsernameTaken(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean insertUser(String username,String password){
        String insertQuery = "INSERT INTO user (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean validateUser(String username, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM user WHERE username = ? AND password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);

                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            }
        }
    }

}