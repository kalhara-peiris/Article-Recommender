package org.example.javaoop;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class categoryController extends CatergorizedArticles{


    @FXML
    protected Button readMore1;
    @FXML
    protected Button readMore2;
    @FXML
    protected Button readMore3;
    @FXML
    protected VBox articlesContainer;
    @FXML
    protected Label articleTitle1;
    @FXML
    protected Label articleTitle2;
    @FXML
    protected Label articleTitle3;
    @FXML
    protected Button skipButton;
    @FXML
    protected TextField searchField;

    protected List<Article> currentArticles;
    protected int currentIndex = 0;
    protected String currentUsername;

    protected static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    protected static final String DB_USER = "root";
    protected static final String DB_PASSWORD = "";

    // Abstract method that concrete classes must implement to specify their category
    protected abstract String getCategoryId();

    // Abstract method for category-specific CSS
    protected abstract String getCategoryCssFile();

    public categoryController() {
        super(Runtime.getRuntime().availableProcessors() * 2);
        currentArticles = new ArrayList<>();
    }

    @FXML
    public void initialize() {
        loadCategoryArticles();
        displayCurrentArticles();

        skipButton.setOnAction(event -> handleSkipButton());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchArticles(newValue);
        });
    }

    protected void loadCategoryArticles() {
        currentArticles.clear();
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM articlecategory WHERE categoryID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, getCategoryId());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Article article = new Article(
                            rs.getString("ArticleID"),
                            rs.getString("title"),
                            rs.getString("url")
                    );
                    currentArticles.add(article);
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Error loading articles: " + e.getMessage());
        }
    }

    protected void displayCurrentArticles() {
        clearArticleTitles();

        if (currentIndex >= currentArticles.size()) {
            currentIndex = 0;
        }

        for (int i = 0; i < 3; i++) {
            int articleIndex = currentIndex + i;
            if (articleIndex < currentArticles.size()) {
                Article article = currentArticles.get(articleIndex);
                setArticleTitle(i + 1, article.getTitle());
                setupReadMoreButton(i + 1, article);
            }
        }
    }

    protected void clearArticleTitles() {
        articleTitle1.setText("");
        articleTitle2.setText("");
        articleTitle3.setText("");
    }

    protected void setArticleTitle(int position, String title) {
        switch (position) {
            case 1: articleTitle1.setText(title); break;
            case 2: articleTitle2.setText(title); break;
            case 3: articleTitle3.setText(title); break;
        }
    }

    protected void setupReadMoreButton(int position, Article article) {
        Button readMoreButton = findReadMoreButton(position);
        if (readMoreButton != null) {
            readMoreButton.setOnAction(event -> showArticleContent(article));
        }
    }

    protected Button findReadMoreButton(int position) {
        switch (position) {
            case 1: return readMore1;
            case 2: return readMore2;
            case 3: return readMore3;
            default: return null;
        }
    }

    protected void showArticleContent(Article article) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());

            ArticleContainer controller = loader.getController();
            controller.setArticleData(
                    article.getId(),
                    article.getTitle(),
                    article.getUrl(),
                    getCategoryName(),
                    currentUsername
            );

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showError("Error", "Could not load articlessss detail view: " + e.getMessage());
        }
    }

    protected String getCategoryName() {
        switch (getCategoryId()) {
            case "C01": return "Technology";
            case "C02": return "Health";
            case "C03": return "Sports";
            case "C04": return "AI";
            case "C06": return "Science";
            default: return "Unknown";
        }
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    @FXML
    protected void handleSkipButton() {
        currentIndex += 3;
        displayCurrentArticles();
    }

    protected void searchArticles(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            loadCategoryArticles();
        } else {
            currentArticles.clear();
            try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT * FROM ArticleCategory WHERE categoryID = ? AND title LIKE ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, getCategoryId());
                    stmt.setString(2, "%" + searchTerm + "%");
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        Article article = new Article(
                                rs.getString("ArticleID"),
                                rs.getString("title"),
                                rs.getString("url")
                        );
                        currentArticles.add(article);
                    }
                }
                currentIndex = 0;
                displayCurrentArticles();
            } catch (SQLException e) {
                showError("Search Error", "Error searching articles: " + e.getMessage());
            }
        }
    }

    protected void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Navigation methods that are common to all categories
    @FXML
    public void recommended(ActionEvent event) throws IOException {
        navigateToView(event, "recommend.fxml", "articleStyle.css");
    }

    @FXML
    public void technology(ActionEvent event) throws IOException {
        navigateToView(event, "technology.fxml", "technology.css");
    }

    @FXML
    public void health(ActionEvent event) throws IOException {
        navigateToView(event, "health.fxml", "technology.css");
    }

    @FXML
    public void sport(ActionEvent event) throws IOException {
        navigateToView(event, "sport.fxml", "technology.css");
    }

    @FXML
    public void AI(ActionEvent event) throws IOException {
        navigateToView(event, "AI.fxml", "technology.css");
    }

    @FXML
    public void science(ActionEvent event) throws IOException {
        navigateToView(event, "science.fxml", "technology.css");
    }

    // Helper method for navigation
    protected void navigateToView(ActionEvent event, String fxmlFile, String cssFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // Inner class to hold article data
    protected static class Article {
        private final String id;
        private final String title;
        private final String url;

        public Article(String id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }
}

