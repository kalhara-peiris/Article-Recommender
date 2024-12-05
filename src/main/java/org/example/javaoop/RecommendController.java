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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
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
    DBHandler DB;

    private boolean isInitialized = false;

    // Thread pool for handling concurrent operations
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // Queue for handling interaction records
    private final BlockingQueue<Runnable> interactionQueue = new LinkedBlockingQueue<>();

    // Thread for processing interactions
    private final Thread interactionProcessor;

    public RecommendController() {
        interactionProcessor = new Thread(this::processInteractionQueue);
        interactionProcessor.setDaemon(true);
        interactionProcessor.start();
        try {
            DB = new DBHandler();
        } catch (Exception e) {
            System.err.println("Error initializing DBHandler: " + e.getMessage());
        }
    }

    private void processInteractionQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Runnable interaction = interactionQueue.take();
                interaction.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    @FXML
    @Override
    public void initialize() {
        if (isInitialized) {
            System.out.println("Already initialized ");
            return;
        }
        isInitialized = true;

        System.out.println("RecommendController initializing...");

        // Get username from User class
        String username = User.getCurrentUsername();
        System.out.println("Current username: " + username);

        if (username != null) {
            this.currentUser = new User(username);
            setupEventHandlers();
            loadCategoryArticles();
            displayCurrentArticles();
        } else {
            System.err.println("No user is currently logged in");
            Platform.runLater(() -> showError("Error", "No user is currently logged in. Please log in again."));
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
    public String getCategoryId() {
        return null; // This controller handles multiple categories
    }

    @Override
    public String getCategoryCssFile() {
        return "articleStyle.css";
    }

    @Override
    protected void loadCategoryArticles() {
        CompletableFuture.runAsync(() -> {
            try {
                if (currentUser == null || currentUser.getUsername() == null) {
                    Platform.runLater(() -> showError("Error", "No user is currently logged in"));
                    return;
                }

                boolean hasInteractions = dbHandler.hasUserInteractions(currentUser.getUsername());
                List<Article> articles;

                if (hasInteractions) {
                    Map<String, Double> preferences = dbHandler.getUserPreferences(currentUser.getUsername());
                    articles = dbHandler.getPersonalizedArticles(currentUser.getUsername(), preferences);
                } else {
                    articles = dbHandler.getRandomArticles(9);
                }

                Platform.runLater(() -> {
                    currentArticles.clear();
                    currentArticles.addAll(articles);
                    displayCurrentArticles();
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Database Error", "Error loading articles: " + e.getMessage()));
            }
        }, executorService);
    }
    @Override
    protected void showArticleContent(Article article) {
        CompletableFuture.runAsync(() -> {
            try {
                if (currentUser == null) {
                    Platform.runLater(() -> showError("Error", "No user is currently logged in"));
                    return;
                }

                currentUser.readArticle(article.getArticleId());

                Platform.runLater(() -> {
                    try {
                        loadArticleView(article);
                    } catch (IOException e) {
                        showError("Error", "Could not load article detail view: " + e.getMessage());
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Database Error", "Could not record read interaction: " + e.getMessage())
                );
            }
        }, executorService);
    }

    @Override
    protected void handleSkipButton() {
        CompletableFuture.runAsync(() -> {
            try {
                if (currentUser == null) {
                    Platform.runLater(() -> showError("Error", "No user is currently logged in"));
                    return;
                }

                // Process skip for each article in the current batch
                for (int i = 0; i < 3; i++) {
                    int articleIndex = currentIndex + i;
                    if (articleIndex < currentArticles.size()) {
                        Article article = currentArticles.get(articleIndex);
                        currentUser.skipArticle(article.getArticleId());
                    }
                }

                currentIndex += 3;
                Platform.runLater(this::displayCurrentArticles);

            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Error", "Failed to process skip: " + e.getMessage())
                );
            }
        }, executorService);
    }

    private void loadArticleView(Article article) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());

        ArticleContainer controller = loader.getController();
        controller.setArticleData(
                article.getArticleId(),
                article.getTitle(),
                article.getUrl(),
                getCategoryNameArticle(article),
                currentUser.getUsername()
        );

        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }


    protected void loadCategoryArticle() {
        System.out.println("Loading articles for user: " + currentUser.getUsername());
        if (currentUser.getUsername() == null || currentUser.getUsername().trim().isEmpty()) {
            System.out.println("Username is null or empty, skipping article loading");
            return;
        }

        CompletableFuture.supplyAsync(this::hasUserInteractions, executorService)
                .thenAcceptAsync(hasInteractions -> {
                    currentArticles.clear();
                    if (hasInteractions) {
                        System.out.println("Loading personalized articles");
                        loadPersonalizedArticles();
                    } else {
                        System.out.println("Loading random articles");
                        loadRandomArticles();
                    }
                }, executorService)
                .exceptionally(throwable -> {
                    Platform.runLater(() -> showError("Error", "Failed to load articles: " + throwable.getMessage()));
                    return null;
                });
    }

    private boolean hasUserInteractions() {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT COUNT(*) FROM userinteraction WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, currentUser.getUsername());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            Platform.runLater(() -> showError("Database Error", "Error checking user interactions: " + e.getMessage()));
        }
        return false;
    }

    private void loadRandomArticles() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT a.ArticleID, a.title, a.url " +
                        "FROM ArticleCategory a " +
                        "ORDER BY RAND() LIMIT 9";

                List<Article> randomArticles = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        Article article = new Article(
                                rs.getString("ArticleID"),
                                rs.getString("title"),
                                rs.getString("url")
                        );
                        randomArticles.add(article);
                    }
                }

                Platform.runLater(() -> {
                    currentArticles.addAll(randomArticles);
                    displayCurrentArticles();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Database Error", "Error loading random articles: " + e.getMessage()));
            }
        }, executorService);
    }

    private void loadPersonalizedArticles() {
        CompletableFuture.supplyAsync(() -> getUserCategoryPreferences(), executorService)
                .thenAcceptAsync(categoryScores -> {
                    try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        System.out.println("Loading personalized articles for user: " + currentUser.getUsername());

                        String query = "SELECT a.ArticleID, a.title, a.url, a.categoryID " +
                                "FROM ArticleCategory a " +
                                "WHERE a.ArticleID NOT IN " +
                                "(SELECT ArticleID FROM userinteraction WHERE username = ?)";

                        List<ArticleScore> scoredArticles = new ArrayList<>();

                        try (PreparedStatement stmt = conn.prepareStatement(query)) {
                            stmt.setString(1, currentUser.getUsername());
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

                        // Sort articles by score and select top 9
                        List<Article> recommendedArticles = scoredArticles.stream()
                                .sorted((a1, a2) -> Double.compare(a2.score, a1.score))
                                .limit(9)
                                .map(as -> as.article)
                                .collect(Collectors.toList());

                        Platform.runLater(() -> {
                            currentArticles.addAll(recommendedArticles);
                            displayCurrentArticles();
                        });
                    } catch (SQLException e) {
                        Platform.runLater(() ->
                                showError("Database Error", "Error loading personalized articles: " + e.getMessage())
                        );
                    }
                }, executorService)
                .exceptionally(throwable -> {
                    Platform.runLater(() ->
                            showError("Error", "Failed to load personalized articles: " + throwable.getMessage())
                    );
                    return null;
                });
    }

    private Map<String, Double> getUserCategoryPreferences() {
        Map<String, Double> preferences = new HashMap<>();
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT categoryID, preferenceScore FROM userpreference WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, currentUser.getUsername());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    preferences.put(
                            rs.getString("categoryID"),
                            rs.getDouble("preferenceScore")
                    );
                }
            }
        } catch (SQLException e) {
            Platform.runLater(() ->
                    showError("Database Error", "Error loading user preferences: " + e.getMessage())
            );
        }
        return preferences;
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

    @Override
    protected void displayCurrentArticles() {
        Platform.runLater(() -> {
            System.out.println("Displaying articles. Current count: " + currentArticles.size());
            if (currentIndex >= currentArticles.size()) {
                currentIndex = 0;
            }

            for (int i = 0; i < 3; i++) {
                int articleIndex = currentIndex + i;
                if (articleIndex < currentArticles.size()) {
                    Article article = currentArticles.get(articleIndex);
                    setArticleTitle(i + 1, article.getTitle());
                    setArticleCategory(i + 1, article.getArticleId());
                    setupReadMoreButton(i + 1, article);
                    System.out.println("Set article " + (i+1) + ": " + article.getTitle());
                }
            }
        });
    }

    protected void setArticleCategory(int position, String id) {
        if (position == 1) {
            switch (getCategoryId(id)) {
                case "C01": category1.setText("Technology"); break;
                case "C02": category1.setText("Health"); break;
                case "C03": category1.setText("Sport"); break;
                case "C04": category1.setText("AI"); break;
                default: category1.setText("Science"); break;
            }
        } else if (position == 2) {
            switch (getCategoryId(id)) {
                case "C01": category2.setText("Technology"); break;
                case "C02": category2.setText("Health"); break;
                case "C03": category2.setText("Sport"); break;
                case "C04": category2.setText("AI"); break;
                default: category2.setText("Science"); break;
            }
        } else if (position == 3) {
            switch (getCategoryId(id)) {
                case "C01": category3.setText("Technology"); break;
                case "C02": category3.setText("Health"); break;
                case "C03": category3.setText("Sport"); break;
                case "C04": category3.setText("AI"); break;
                default: category3.setText("Science"); break;
            }
        }
    }



    private void recordInteraction(String articleId, String interactionType) {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO userinteraction (interactionID, username, ArticleID, interactionType) " +
                    "VALUES (?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, currentUser.getUsername());
                stmt.setString(3, articleId);
                stmt.setString(4, interactionType);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            Platform.runLater(() ->
                    showError("Database Error", "Error recording interaction: " + e.getMessage())
            );
        }
    }


    @Override
    public void setCurrentUsername(String username) {
        System.out.println("Setting username in RecommendController: " + username);
        if (username != null && !username.trim().isEmpty()) {
            User.setCurrentUsername(username);
            this.currentUser = new User(username);

            CompletableFuture.runAsync(() -> {
                System.out.println("Loading articles for user: " + username);
                loadCategoryArticles();
                Platform.runLater(this::displayCurrentArticles);
            }, executorService);
        } else {
            this.currentUser = null;
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

    public String getCategoryNameArticle(Article article) {
        switch (getCategoryId(article.getArticleId())) {
            case "C01": return "Technology";
            case "C02": return "Health";
            case "C03": return "Sports";
            case "C04": return "AI";
            case "C06": return "Science";
            default: return "Unknown";
        }
    }

    // Cleanup method to properly shutdown threads when the controller is no longer needed
    public void cleanup() {
        executorService.shutdown();
        interactionProcessor.interrupt();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
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