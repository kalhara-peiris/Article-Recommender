package org.example.javaoop;

import java.sql.SQLException;
import java.util.ArrayList;

public class User {
    private String username;
    private DBHandler dbManager;
    private Article article;
    private ArrayList<Article> articles = new ArrayList<>();

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

    public void readArticle(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "READ");
        String categoryId = dbManager.getCategoryForArticle(articleId);
        if (categoryId != null) {
            dbManager.updatePreferenceScore(username, categoryId, 3);
        }
    }

    public void skipArticle(String articleId) throws SQLException {
        if (!dbManager.hasReadArticle(username, articleId)) {
            dbManager.recordInteraction(username, articleId, "SKIP");
            String categoryId = dbManager.getCategoryForArticle(articleId);
            if (categoryId != null) {
                dbManager.updatePreferenceScore(username, categoryId, -1);
            }
        }
    }
    public void likeArticle(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "LIKE");
        String categoryId = dbManager.getCategoryForArticle(articleId);
        if (categoryId != null) {
            dbManager.updatePreferenceScore(username, categoryId, 5);
        }
    }

    public void disLikeArticle(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "DISLIKE");
        String categoryId = dbManager.getCategoryForArticle(articleId);
        if (categoryId != null) {
            dbManager.updatePreferenceScore(username, categoryId, -3);
        }
    }

    public void saveArticle(String articleId) throws SQLException {
        dbManager.recordInteraction(username, articleId, "SAVE");
    }

    public void cleanup() {
        dbManager.cleanup();
    }
}