package org.example.javaoop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
public abstract class categoryController extends CatergorizedArticles {
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
    protected User currentUser;

    protected static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    protected static final String DB_USER = "root";
    protected static final String DB_PASSWORD = "";

    // Thread management
    protected final ExecutorService executorService;
    private final BlockingQueue<Runnable> dbOperationQueue;
    private final Thread dbOperationProcessor;

    public abstract String getCategoryId();

    public abstract String getCategoryCssFile();

    public categoryController() {
        super(Runtime.getRuntime().availableProcessors() * 2);
        currentArticles = new CopyOnWriteArrayList<>();
        executorService = Executors.newFixedThreadPool(3);
        dbOperationQueue = new LinkedBlockingQueue<>();
        dbOperationProcessor = new Thread(this::processDbOperationQueue);
        dbOperationProcessor.setDaemon(true);
        dbOperationProcessor.start();
    }

    private void processDbOperationQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Runnable operation = dbOperationQueue.take();
                operation.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @FXML
    public void initialize() {
        CompletableFuture.runAsync(() -> {
            Platform.runLater(() -> {
                try {
                    initializeUser();
                    setupEventHandlers();
                    loadCategoryArticles();
                } catch (Exception e) {
                    showError("Error", "Failed to initialize: " + e.getMessage());
                }
            });
        }, executorService);
    }

    private void initializeUser() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        if (stage != null && stage.getUserData() instanceof HelloApplication) {
            HelloApplication app = (HelloApplication) stage.getUserData();
            this.currentUsername = app.getCurrentUsername();
            this.currentUser = new User(currentUsername);
        }
    }

    private void setupEventHandlers() {
        skipButton.setOnAction(event -> handleSkipButton());
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                searchArticles(newValue));
    }

    protected void loadCategoryArticles() {
        CompletableFuture.runAsync(() -> {
            currentArticles.clear();
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT * FROM articlecategory WHERE categoryID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, getCategoryId());
                    ResultSet rs = stmt.executeQuery();

                    List<Article> newArticles = new ArrayList<>();
                    while (rs.next()) {
                        Article article = new Article(
                                rs.getString("ArticleID"),
                                rs.getString("title"),
                                rs.getString("url")
                        );
                        newArticles.add(article);
                    }

                    Platform.runLater(() -> {
                        currentArticles.addAll(newArticles);
                        displayCurrentArticles();
                    });
                }
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Database Error", "Error loading articles: " + e.getMessage()));
            }
        }, executorService);
    }

    protected void searchArticles(String searchTerm) {
        CompletableFuture.runAsync(() -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                loadCategoryArticles();
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT * FROM ArticleCategory WHERE categoryID = ? AND title LIKE ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, getCategoryId());
                    stmt.setString(2, "%" + searchTerm + "%");
                    ResultSet rs = stmt.executeQuery();

                    List<Article> searchResults = new ArrayList<>();
                    while (rs.next()) {
                        Article article = new Article(
                                rs.getString("ArticleID"),
                                rs.getString("title"),
                                rs.getString("url")
                        );
                        searchResults.add(article);
                    }

                    Platform.runLater(() -> {
                        currentArticles.clear();
                        currentArticles.addAll(searchResults);
                        currentIndex = 0;
                        displayCurrentArticles();
                    });
                }
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Search Error", "Error searching articles: " + e.getMessage()));
            }
        }, executorService);
    }
    @FXML
    protected void handleSkipButton() {
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    int articleIndex = currentIndex + i;
                    if (articleIndex < currentArticles.size()) {
                        Article article = currentArticles.get(articleIndex);
                        currentUser.skipArticle(article.getId()); // Pass the ID instead of the Article object
                    }
                }
                currentIndex += 3;
                Platform.runLater(this::displayCurrentArticles);
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Error", "Failed to record skip: " + e.getMessage()));
            }
        }, executorService);
    }



    protected void displayCurrentArticles() {
        Platform.runLater(() -> {
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
        });
    }

    protected void clearArticleTitles() {
        articleTitle1.setText("");
        articleTitle2.setText("");
        articleTitle3.setText("");
    }

    protected void setArticleTitle(int position, String title) {
        switch (position) {
            case 1:
                articleTitle1.setText(title);
                break;
            case 2:
                articleTitle2.setText(title);
                break;
            case 3:
                articleTitle3.setText(title);
                break;
        }
    }

    protected void setupReadMoreButton(int position, Article article) {
        Button readMoreButton = findReadMoreButton(position);
        if (readMoreButton != null) {
            readMoreButton.setOnAction(event ->
                    CompletableFuture.runAsync(() ->
                            showArticleContent(article), executorService)
            );
        }
    }

    protected Button findReadMoreButton(int position) {
        switch (position) {
            case 1:
                return readMore1;
            case 2:
                return readMore2;
            case 3:
                return readMore3;
            default:
                return null;
        }
    }

    protected void showArticleContent(Article article) {
        CompletableFuture.runAsync(() -> {
            try {
                currentUser.readArticle(article.getId());
                Platform.runLater(() -> {
                    try {
                        loadArticleView(article);
                    } catch (IOException e) {
                        showError("Error", "Could not load article view: " + e.getMessage());
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Error", "Failed to record read: " + e.getMessage()));
            }
        }, executorService);
    }

    protected String getCategoryId(String articleId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT categoryID FROM ArticleCategory WHERE ArticleID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, articleId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("categoryID");
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not get category ID: " + e.getMessage());
        }
        return null;
    }

    private void loadArticleView(Article article) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
        Parent root = loader.load();
        ArticleContainer controller = loader.getController();
        controller.setCurrentUsername(currentUsername);
        controller.setArticleData(article.getId(), article.getTitle(),
                article.getUrl(), getCategoryName(), currentUsername);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    protected void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    protected String getCategoryName() {
        switch (getCategoryId()) {
            case "C01":
                return "Technology";
            case "C02":
                return "Health";
            case "C03":
                return "Sports";
            case "C04":
                return "AI";
            case "C06":
                return "Science";
            default:
                return "Unknown";
        }
    }

    // Navigation methods
    @FXML
    public void recommended(ActionEvent event) throws IOException {
        navigateToView(event, "recommend.fxml", "articleStyle.css");
    }

    @FXML
    public void userProfile(ActionEvent event) throws IOException {
        navigateToView(event, "userProfile.fxml", "userProfile.css");
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

    protected void navigateToView(ActionEvent event, String fxmlFile, String cssFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        this.currentUser = new User(username);
    }

    public void cleanup() {
        executorService.shutdown();
        dbOperationProcessor.interrupt();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    protected static class Article {
        private final String id;
        private final String title;
        private final String url;

        public Article(String id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}