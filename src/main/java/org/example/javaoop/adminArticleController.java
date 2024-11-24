package org.example.javaoop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class adminArticleController implements Initializable {

    // Database configuration
    protected static final String dbUrl = "jdbc:mysql://localhost:3306/javacw";
    protected static final String dbUser = "root";
    protected static final String dbPassword = "";

    // FXML injected elements
    @FXML private Button dashBoardBtn;
    @FXML private Button articlesBtn;
    @FXML private Button usersBtn;
    @FXML private Button logoutBtn;

    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;

    @FXML private Label totalArticles;
    @FXML private Label publishedArticles;
    @FXML private Label pendingArticles;

    @FXML private TextField searchField;
    @FXML private VBox articleList;
    @FXML private Pagination articlePagination;

    private static final int ITEMS_PER_PAGE = 10;
    private ObservableList<Article> articles;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articles = FXCollections.observableArrayList();
        loadArticles();
        setupSearchListener();
        setupPagination();
        setupButtons();
    }

    private void loadArticles() {
        clearArticleList();
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String query = "SELECT articleId, title, url FROM article";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Article article = new Article(
                                rs.getString("articleId"),
                                rs.getString("title"),
                                rs.getString("url")
                        );
                        articles.add(article);
                    }
                }
            }
            // Update total articles count
            totalArticles.setText(String.valueOf(articles.size()));
            showPage(0);
        } catch (SQLException e) {
            showError("Database Error", "Could not load articles: " + e.getMessage());
        }
    }
    private void deleteArticle(String articleId) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // First try to delete from articlecategory table
                String deleteCategories = "DELETE FROM articlecategory WHERE articleId = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteCategories)) {
                    pstmt.setString(1, articleId);
                    pstmt.executeUpdate(); // We don't check affected rows as article might not have categories
                }
                String deleteCategories1 = "DELETE FROM userinteraction WHERE articleId = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteCategories1)) {
                    pstmt.setString(1, articleId);
                    pstmt.executeUpdate(); // We don't check affected rows as article might not have categories
                }

                // Then delete from article table
                String deleteArticle = "DELETE FROM article WHERE articleId = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteArticle)) {
                    pstmt.setString(1, articleId);
                    int affected = pstmt.executeUpdate();

                    if (affected > 0) {
                        // If article deletion was successful, commit transaction
                        conn.commit();
                        // Update UI
                        articles.removeIf(article -> article.getArticleId().equals(articleId));
                        setupPagination();
                        showPage(articlePagination.getCurrentPageIndex());
                        totalArticles.setText(String.valueOf(articles.size()));
                    } else {
                        // If no article was deleted, rollback
                        conn.rollback();
                    }
                }
            } catch (SQLException e) {
                // If any error occurs, rollback the transaction
                conn.rollback();
                throw e; // Re-throw to be caught by outer catch block
            }
        } catch (SQLException e) {
            showError("Delete Error", "Could not delete article: " + e.getMessage());
        }
    }



    private void addArticleToView(Article article) {
        VBox articleItem = new VBox();
        articleItem.getStyleClass().add("article-item");

        Label titleLabel = new Label(article.getTitle());
        titleLabel.getStyleClass().add("article-title");

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(e -> deleteArticle(article.getArticleId()));

        javafx.scene.layout.HBox itemContent = new javafx.scene.layout.HBox(10);
        itemContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox titleBox = new VBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.getChildren().add(titleLabel);
        deleteButton.setMinWidth(60); // Set minimum width
        deleteButton.setPrefWidth(70); // Set preferred width
        deleteButton.setMaxHeight(30);

        itemContent.getChildren().addAll(titleBox, deleteButton);
        articleItem.getChildren().add(itemContent);

        articleItem.setPadding(new javafx.geometry.Insets(15));

        articleList.getChildren().add(articleItem);
    }

    private void setupPagination() {
        int pageCount = (int) Math.ceil(articles.size() / (double) ITEMS_PER_PAGE);
        pageCount = Math.max(1, pageCount); // Ensure at least 1 page
        articlePagination.setPageCount(pageCount);
        articlePagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) ->
                showPage(newIndex.intValue()));
    }

    private void showPage(int pageIndex) {
        clearArticleList();
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, articles.size());

        for (int i = fromIndex; i < toIndex; i++) {
            addArticleToView(articles.get(i));
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterArticles(newValue);
        });
    }

    private void filterArticles(String searchText) {
        clearArticleList();
        ObservableList<Article> filteredList = articles.filtered(article ->
                article.getTitle().toLowerCase().contains(searchText.toLowerCase())
        );

        for (Article article : filteredList) {
            addArticleToView(article);
        }
    }

    private void setupButtons() {
        dashBoardBtn.setOnAction(e -> handleDashboard());
        usersBtn.setOnAction(e -> handleUsers());
        logoutBtn.setOnAction(e -> handleLogout());
    }

    private void handleDashboard() {
        // Add dashboard navigation logic
    }

    private void handleUsers() {
        // Add users view navigation logic
    }

    private void handleLogout() {
        // Add logout logic
    }

    private void clearArticleList() {
        articleList.getChildren().clear();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Article class
    private static class Article {
        private final String articleId;
        private final String title;
        private final String url;

        public Article(String articleId, String title, String url) {
            this.articleId = articleId;
            this.title = title;
            this.url = url;
        }

        public String getArticleId() {
            return articleId;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}