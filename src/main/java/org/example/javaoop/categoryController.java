package org.example.javaoop;

import javafx.application.Platform;
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
import java.util.UUID;

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

    @FXML
    protected Label category1;
    @FXML
    protected Label category2;
    @FXML
    protected Label category3;


    protected List<Article> currentArticles;
    protected int currentIndex = 0;
    protected String currentUsername;

    protected static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    protected static final String DB_USER = "root";
    protected static final String DB_PASSWORD = "";

    // Abstract method that concrete classes must implement to specify their category
    public abstract String getCategoryId();

    // Abstract method for category-specific CSS
    public abstract String getCategoryCssFile();

    public categoryController() {
        super(Runtime.getRuntime().availableProcessors() * 2);
        currentArticles = new ArrayList<>();
    }
    @FXML
    public void initialize() {
        Platform.runLater(() -> {  // Add Platform.runLater to ensure scene is ready
            // Try to get username from stage userData
            try {
                Stage stage = (Stage) searchField.getScene().getWindow();
                if (stage != null && stage.getUserData() instanceof HelloApplication) {
                    HelloApplication app = (HelloApplication) stage.getUserData();
                    this.currentUsername = app.getCurrentUsername();
                    System.out.println("Initialized with username: " + currentUsername); // Debug line
                }
            } catch (Exception e) {
                System.err.println("Error getting username in initialize: " + e.getMessage());
                e.printStackTrace();
            }

            // Continue with other initialization
            loadCategoryArticles();
            displayCurrentArticles();

            skipButton.setOnAction(event -> handleSkipButton());
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                searchArticles(newValue);
            });
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
        System.out.println(title);
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
    protected void recordSkipInteraction() {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // First, get all READ interactions for current articles in view
            String readCheckQuery = "SELECT ArticleID FROM userinteraction WHERE username = ? AND ArticleID = ? AND interactionType = 'READ'";
            String skipInsertQuery = "INSERT INTO userinteraction (interactionID, username, ArticleID, interactionType) VALUES (?, ?, ?, ?)";

            // Check and record skip for each article currently displayed
            for (int i = 0; i < 3; i++) {
                int articleIndex = currentIndex + i;
                if (articleIndex < currentArticles.size()) {
                    Article article = currentArticles.get(articleIndex);

                    // Check if this article was read
                    boolean wasRead = false;
                    try (PreparedStatement checkStmt = conn.prepareStatement(readCheckQuery)) {
                        checkStmt.setString(1, currentUsername);
                        checkStmt.setString(2, article.getId());
                        ResultSet rs = checkStmt.executeQuery();
                        wasRead = rs.next(); // If there's a result, article was read
                    }

                    // If article wasn't read, record it as skipped
                    if (!wasRead) {
                        try (PreparedStatement skipStmt = conn.prepareStatement(skipInsertQuery)) {
                            skipStmt.setString(1, UUID.randomUUID().toString());
                            skipStmt.setString(2, currentUsername);
                            skipStmt.setString(3, article.getId());
                            skipStmt.setString(4, "SKIP");
                            skipStmt.executeUpdate();
                        }
                        updateUserPreference(article.getId(),-1);
                    }
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not record skip interaction: " + e.getMessage());
        }
    }
    protected void showArticleContent(Article article) {
        try {
            // Record READ interaction
            try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "INSERT INTO userinteraction (interactionID, username, ArticleID, interactionType) " +
                        "VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, UUID.randomUUID().toString());
                    pstmt.setString(2, currentUsername);
                    pstmt.setString(3, article.getId());
                    pstmt.setString(4, "READ");
                    pstmt.executeUpdate();
                }
                updateUserPreference(article.getId(), 3);
            }catch (SQLException e){
                showError("DataBase Error","Could not record read interaction"+e.getMessage());
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
            Parent root = loader.load();

            // Get the current stage and its HelloApplication instance
            Stage currentStage = (Stage) searchField.getScene().getWindow();
            HelloApplication app = null;
            if (currentStage.getUserData() instanceof HelloApplication) {
                app = (HelloApplication) currentStage.getUserData();
            }

            ArticleContainer controller = loader.getController();

            // Set the username from multiple possible sources
            String username = currentUsername;
            if (username == null && app != null) {
                username = app.getCurrentUsername();
            }

            // Set up the controller
            controller.setCurrentUsername(username);
            controller.setArticleData(
                    article.getId(),
                    article.getTitle(),
                    article.getUrl(),
                    getCategoryName(),
                    username
            );

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());

            // Pass the HelloApplication instance to the new scene
            Stage stage = (Stage) searchField.getScene().getWindow();
            if (app != null) {
                stage.setUserData(app);
            }
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showError("Error", "Could not load article detail view: " + e.getMessage());
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
        recordSkipInteraction();
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
    protected void updateUserPreference(String articleId, int scoreChange) {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Get the category ID for this article
            String categoryId = getCategoryId(articleId);
            if (categoryId != null) {
                String query = "INSERT INTO userpreference (username, categoryID, preferenceScore) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE preferenceScore = preferenceScore + ?";

                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, currentUsername);
                    pstmt.setString(2, categoryId);
                    pstmt.setInt(3, scoreChange);
                    pstmt.setInt(4, scoreChange);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not update preference: " + e.getMessage());
        }
    }
    protected String getCategoryId(String articleId) {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT categoryID FROM ArticleCategory WHERE ArticleID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, articleId);
                var rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("categoryID");
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not get category ID: " + e.getMessage());
        }
        return null;
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
    public void userProfile(ActionEvent event) throws IOException{
        navigateToView(event,"userProfile.fxml","userProfile.css");
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

