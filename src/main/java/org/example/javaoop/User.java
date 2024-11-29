package org.example.javaoop;

import java.sql.SQLException;

public class User {
    private String username;
    private DBHandler dbManager;

    private String password;


    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public User(String username) {
        this.username = username;
        this.dbManager = new DBHandler();
    }

    public void recordReadInteraction(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "READ");
        String categoryId = dbManager.getCategoryForArticle(articleId);
        if (categoryId != null) {
            dbManager.updatePreferenceScore(username, categoryId, 3);
        }
    }

    public void recordSkipInteraction(String articleId) throws SQLException {
        if (!dbManager.hasReadArticle(username, articleId)) {
            dbManager.recordInteraction(username, articleId, "SKIP");
            String categoryId = dbManager.getCategoryForArticle(articleId);
            if (categoryId != null) {
                dbManager.updatePreferenceScore(username, categoryId, -1);
            }
        }
    }
    public void recordLikeInteraction(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "LIKE");
        String categoryId = dbManager.getCategoryForArticle(articleId);
        if (categoryId != null) {
            dbManager.updatePreferenceScore(username, categoryId, 5);
        }
    }

    public void recordDislikeInteraction(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "DISLIKE");
        String categoryId = dbManager.getCategoryForArticle(articleId);
        if (categoryId != null) {
            dbManager.updatePreferenceScore(username, categoryId, -3);
        }
    }

    public void recordSaveInteraction(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "SAVE");
    }

    public void cleanup() {
        dbManager.cleanup();
    }
}