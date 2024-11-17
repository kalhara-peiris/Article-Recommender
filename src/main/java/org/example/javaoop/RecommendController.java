package org.example.javaoop;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendController extends categoryController {
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

    private boolean isInitialized=false;
    @FXML
    @Override
    public void initialize() {
        if (isInitialized){
            System.out.println("Already initialized ");
            return;
        }
        isInitialized = true;

        System.out.println("RecommendController initializing...");
        System.out.println("Current username: " + currentUsername);

        if (currentUsername == null) {
            Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) searchField.getScene().getWindow();
                    if (stage != null && stage.getUserData() instanceof HelloApplication) {
                        HelloApplication app = (HelloApplication) stage.getUserData();
                        currentUsername = app.getCurrentUsername();
                        System.out.println("Username retrieved from HelloApplication: " + currentUsername);
                        loadCategoryArticles();
                        displayCurrentArticles();
                    }
                } catch (Exception e) {
                    System.err.println("Error in initialize: " + e.getMessage());
                }
            });
        }

        setupEventHandlers();
    }

    private void setupEventHandlers() {
        skipButton.setOnAction(event -> handleSkipButton());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchArticles(newValue);
        });
    }




    @Override
    protected String getCategoryId() {
        return null; // This controller handles multiple categories
    }

    @Override
    protected String getCategoryCssFile() {
        return "articleStyle.css";
    }

    @Override
    protected void loadCategoryArticles() {

        System.out.println("Loading articles for user: " + currentUsername);
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            System.out.println("Username is null or empty, skipping article loading");
            return;
        }

        currentArticles.clear();
        if (hasUserInteractions()) {
            System.out.println("Loading personalized articles");
            loadPersonalizedArticles();
        } else {
            System.out.println("Loading random articles");
            loadRandomArticles();
        }
    }




    private boolean hasUserInteractions() {
        if(currentUsername==null) return false;
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT COUNT(*) FROM userinteraction WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, currentUsername);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Error checking user interactions: " + e.getMessage());
        }
        return false;
    }

    private void loadRandomArticles() {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT a.ArticleID, a.title, a.url " +
                    "FROM ArticleCategory a " +
                    "ORDER BY RAND() LIMIT 9";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
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
            showError("Database Error", "Error loading random articles: " + e.getMessage());
        }
    }

    private void loadPersonalizedArticles() {
        Map<String, Double> categoryScores = getUserCategoryPreferences();
        List<ArticleScore> scoredArticles = new ArrayList<>();

        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Loading personalized articles for user : "+currentUsername);
            // Get articles excluding those the user has already interacted with
            String query = "SELECT a.ArticleID, a.title, a.url, a.categoryID " +
                    "FROM ArticleCategory a " +
                    "WHERE a.ArticleID NOT IN " +
                    "(SELECT ArticleID FROM userinteraction WHERE username = ?)";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, currentUsername);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String categoryId = rs.getString("categoryID");
                    double categoryScore = categoryScores.getOrDefault(categoryId, 0.0);

                    ArticleScore articleScore = new ArticleScore(
                            new Article(
                                    rs.getString("ArticleID"),
                                    rs.getString("title"),
                                    rs.getString("url")
                            ),
                            categoryScore
                    );
                    scoredArticles.add(articleScore);
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Error loading personalized articles: " + e.getMessage());
        }

        // Sort articles by score and select top 9
        scoredArticles.sort((a1, a2) -> Double.compare(a2.score, a1.score));
        currentArticles = scoredArticles.stream()
                .limit(9)
                .map(as -> as.article)
                .collect(Collectors.toList());
    }
    @Override
    protected void showArticleContent(Article article) {
        try {
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
                super.updateUserPreference(article.getId(), 3);
            }catch (SQLException e){
                showError("DataBase Error","Could not record read interaction"+e.getMessage());
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());

            ArticleContainer controller = loader.getController();
            controller.setCurrentUsername(currentUsername); // Set username before setting article data
            controller.setArticleData(
                    article.getId(),
                    article.getTitle(),
                    article.getUrl(),
                    getCategoryNameArticle(article),
                    currentUsername
            );

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showError("Error", "Could not load article detail view: " + e.getMessage());
        }
    }
    public String getCategoryNameArticle(Article article){
        switch (getCategoryId(article.getId())) {
            case "C01": return "Technology";
            case "C02": return "Health";
            case "C03": return "Sports";
            case "C04": return "AI";
            case "C06": return "Science";
            default: return "Unknown";
        }
    }

    private Map<String, Double> getUserCategoryPreferences() {
        Map<String, Double> preferences = new HashMap<>();

        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT categoryID, preferenceScore FROM userpreference WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, currentUsername);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    preferences.put(
                            rs.getString("categoryID"),
                            rs.getDouble("preferenceScore")
                    );
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Error loading user preferences: " + e.getMessage());
        }

        return preferences;
    }

    @Override
    protected void handleSkipButton() {
        // Record skip interaction for recommendations
        currentArticles.stream()
                .skip(currentIndex)
                .limit(3)
                .forEach(article -> recordInteraction(article.getId(), "SKIP"));

        // Move to next set of articles
        super.handleSkipButton();
    }

    private void recordInteraction(String articleId, String interactionType) {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO userinteraction (interactionID, username, ArticleID, interactionType) " +
                    "VALUES (?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, currentUsername);
                stmt.setString(3, articleId);
                stmt.setString(4, interactionType);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            showError("Database Error", "Error recording interaction: " + e.getMessage());
        }
    }
    public void ArticleView(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    @Override
    public void setCurrentUsername(String username) {
        System.out.println("Setting username in RecommendController: " + username);
        this.currentUsername = username;

        if (username != null && !username.trim().isEmpty()) {
            Platform.runLater(() -> {
                System.out.println("Loading articles for user"+username);
                loadCategoryArticles();
                displayCurrentArticles();
            });
        }
    }
    @Override
    protected void displayCurrentArticles() {
        System.out.println("Displaying articles. Current count: " + currentArticles.size());
        super.displayCurrentArticles();
        if (currentIndex >= currentArticles.size()) {
            currentIndex = 0;
        }

        for (int i = 0; i < 3; i++) {
            int articleIndex = currentIndex + i;
            if (articleIndex < currentArticles.size()) {
                Article article = currentArticles.get(articleIndex);
                setArticleTitle(i + 1, article.getTitle());
                setArticleCategory(i+1,article.getId());
                setupReadMoreButton(i + 1, article);
                System.out.println("Set article " + (i+1) + ": " + article.getTitle());

            }
        }
    }
    protected void setArticleCategory(int position,String id){
        if(position==1){
            if(getCategoryId(id).equals("C01")){
                category1.setText("Technology");
            }else if(getCategoryId(id).equals("C02")){
                category1.setText("Health");
            }else if(getCategoryId(id).equals("C03")){
                category1.setText("Sport");
            }else if(getCategoryId(id).equals("C04")){
                category1.setText("AI");
            }else{
                category1.setText("Science");
            }
        }else  if(position==2){
            if(getCategoryId(id).equals("C01")){
                category2.setText("Technology");
            }else if(getCategoryId(id).equals("C02")){
                category2.setText("Health");
            }else if(getCategoryId(id).equals("C03")){
                category2.setText("Sport");
            }else if(getCategoryId(id).equals("C04")){
                category2.setText("AI");
            }else{
                category2.setText("Science");
            }
        }else  if(position==3){
            if(getCategoryId(id).equals("C01")){
                category3.setText("Technology");
            }else if(getCategoryId(id).equals("C02")){
                category3.setText("Health");
            }else if(getCategoryId(id).equals("C03")){
                category3.setText("Sport");
            }else if(getCategoryId(id).equals("C04")){
                category3.setText("AI");
            }else{
                category3.setText("Science");
            }
        }

    }


    // Helper class for scoring articles
    private static class ArticleScore {
        final Article article;
        final double score;

        ArticleScore(Article article, double score) {
            this.article = article;
            this.score = score;
        }
    }
}