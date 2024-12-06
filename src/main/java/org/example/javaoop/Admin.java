package org.example.javaoop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Consumer;

// Admin class handles article collection, categorization and user management operations
public class Admin extends User {
    // Core components for database, article and categorization operations
    private final DBHandler dbHandler;
    private final Article articleCollector;
    private final CatergorizedArticles categorizer;
    private final int totalTargetArticles;
    private final ArrayList<Article> articles = new ArrayList<Article>();

    // Callback interface for article collection progress
    public interface CollectionProgressCallback {
        void onProgress(double progress);
    }

    // Callback interface for categorization progress
    public interface CategorizationProgressCallback {
        void onProgress(int processed, int total);
    }

    // Initialize admin with default credentials and components
    public Admin() {
        super("admin","12345");
        this.dbHandler = new DBHandler();
        this.articleCollector = new Article();
        this.categorizer = new CatergorizedArticles(Runtime.getRuntime().availableProcessors() * 2);
        this.totalTargetArticles = 10;
    }

    // Returns target number of articles
    public int getTotalTargetArticles() {
        return totalTargetArticles;
    }

    // Starts article collection process with progress tracking
    public void collectArticles(CollectionProgressCallback progressCallback) throws Exception {
        articleCollector.collectArticles(progress -> progressCallback.onProgress(progress));
    }

    // Manages article categorization process with progress tracking
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

    // Returns total number of articles in database
    public int getTotalArticleCount() throws SQLException {
        return dbHandler.getTotalArticleCount();
    }

    // Returns total number of users in database
    public int getTotalUserCount() throws SQLException {
        return dbHandler.getTotalUserCount();
    }

    // Retrieves list of all users
    public ObservableList<User> getUsers() throws SQLException {
        return dbHandler.getAllUsers();
    }

    // Deletes specified user from system
    public boolean deleteUser(String username) throws SQLException {
        return dbHandler.deleteUser(username);
    }

    // Retrieves list of all articles
    public ObservableList<Article> getArticles() throws SQLException {
        return dbHandler.getAllArticles();
    }

    // Deletes specified article from system
    public boolean deleteArticle(String articleId) throws SQLException {
        return dbHandler.deleteArticle(articleId);
    }

    // Searches articles based on search text
    public ObservableList<Article> searchArticles(String searchText) throws SQLException {
        return dbHandler.searchArticles(searchText);
    }

    // Searches users based on search text
    public ObservableList<User> searchUsers(String searchText) throws SQLException {
        return dbHandler.searchUsers(searchText);
    }
}
