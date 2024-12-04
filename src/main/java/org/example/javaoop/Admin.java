package org.example.javaoop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.SQLException;
import java.util.function.Consumer;

public class Admin extends User {
    private final DBHandler dbHandler;
    private final Article articleCollector;
    private final CatergorizedArticles categorizer;
    private final int totalTargetArticles;

    // Progress callback interfaces
    public interface CollectionProgressCallback {
        void onProgress(double progress);
    }

    public interface CategorizationProgressCallback {
        void onProgress(int processed, int total);
    }

    public Admin() {
        super("admin","12345");
        this.dbHandler = new DBHandler();
        this.articleCollector = new Article();
        this.categorizer = new CatergorizedArticles(Runtime.getRuntime().availableProcessors() * 2);
        this.totalTargetArticles = 50;
    }

    // Original methods
    public int getTotalTargetArticles() {
        return totalTargetArticles;
    }

    public void collectArticles(CollectionProgressCallback progressCallback) throws Exception {
        articleCollector.collectArticles(progress -> progressCallback.onProgress(progress));
    }

    public void categorizeArticles(Consumer<Integer> totalUncategorizedCallback,
                                   CategorizationProgressCallback progressCallback) throws SQLException {
        int totalUncategorized = dbHandler.getUncategorizedArticleCount();
        totalUncategorizedCallback.accept(totalUncategorized);

        if (totalUncategorized > 0) {
            categorizer.setCategoryProgressCallback((processed, total) ->
                    progressCallback.onProgress(processed, total));
            categorizer.processCategorization();
        }
    }

    public int getTotalArticleCount() throws SQLException {
        return dbHandler.getTotalArticleCount();
    }

    public int getTotalUserCount() throws SQLException {
        return dbHandler.getTotalUserCount();
    }

    // New methods for user management
    public ObservableList<User> getUsers() throws SQLException {
        return dbHandler.getAllUsers();
    }

    public boolean deleteUser(String username) throws SQLException {
        return dbHandler.deleteUser(username);
    }

    // New methods for article management
    public ObservableList<Article> getArticles() throws SQLException {
        return dbHandler.getAllArticles();
    }

    public boolean deleteArticle(String articleId) throws SQLException {
        return dbHandler.deleteArticle(articleId);
    }

    public ObservableList<Article> searchArticles(String searchText) throws SQLException {
        return dbHandler.searchArticles(searchText);
    }

    public ObservableList<User> searchUsers(String searchText) throws SQLException {
        return dbHandler.searchUsers(searchText);
    }
}