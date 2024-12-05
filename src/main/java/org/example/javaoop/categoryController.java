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
    protected DBHandler dbHandler;

    public categoryController() {
        super(Runtime.getRuntime().availableProcessors() * 2);
        currentArticles = new CopyOnWriteArrayList<>();
        executorService = Executors.newFixedThreadPool(3);
        dbOperationQueue = new LinkedBlockingQueue<>();
        dbOperationProcessor = new Thread(this::processDbOperationQueue);
        dbOperationProcessor.setDaemon(true);
        dbOperationProcessor.start();
        try {
            dbHandler = new DBHandler();
        } catch (Exception e) {
            showError("Database Error", "Failed to initialize database connection");
        }
    }

    protected void loadCategoryArticles() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Article> articles = dbHandler.getCategoryArticles(getCategoryId());
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

    protected void searchArticles(String searchTerm) {
        CompletableFuture.runAsync(() -> {
            try {
                if (searchTerm == null || searchTerm.trim().isEmpty()) {
                    loadCategoryArticles();
                    return;
                }
                List<Article> articles = dbHandler.searchCategoryArticles(getCategoryId(), searchTerm);
                Platform.runLater(() -> {
                    currentArticles.clear();
                    currentArticles.addAll(articles);
                    currentIndex = 0;
                    displayCurrentArticles();
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Search Error", "Error searching articles: " + e.getMessage()));
            }
        }, executorService);
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
        String username = User.getCurrentUsername();
        if(username !=null && !username.trim().isEmpty()){
            this.currentUser=new User(username);
        }
    }

    private void setupEventHandlers() {
        skipButton.setOnAction(event -> handleSkipButton());
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                searchArticles(newValue));
    }


    @FXML
    protected void handleSkipButton() {
        CompletableFuture.runAsync(() -> {
            try {
                if (currentUser == null) {
                    Platform.runLater(() -> showError("Error", "No user is currently logged in"));
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    int articleIndex = currentIndex + i;
                    if (articleIndex < currentArticles.size()) {
                        Article article = currentArticles.get(articleIndex);
                        currentUser.skipArticle(article.getArticleId()); // Pass the ID instead of the Article object
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
                if(currentUser==null){
                    Platform.runLater(()-> showError("Error","No user is currently logged in"));
                    return;
                }
                currentUser.readArticle(article.getArticleId());
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

        // Use currentUser's username
        String username = currentUser.getUsername();
        controller.setArticleData(
                article.getArticleId(),
                article.getTitle(),
                article.getUrl(),
                getCategoryName(),
                username
        );

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
    private Stage getStageFromEvent(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }


    @FXML
    public void recommended(ActionEvent event) {
        Navigator.navigateToRecommendGUI(getStageFromEvent(event), User.getCurrentUsername());
    }

    @FXML
    public void userProfile(ActionEvent event) {
        Navigator.navigateToUserProfileGUI(getStageFromEvent(event), User.getCurrentUsername());
    }
    @FXML
    public void technology(ActionEvent event) {
        Navigator.navigateToTechnologyGUI(getStageFromEvent(event), User.getCurrentUsername());
    }
    @FXML
    public void health(ActionEvent event) {
        Navigator.navigateToHealthGUI(getStageFromEvent(event), User.getCurrentUsername());
    }
    @FXML
    public void sport(ActionEvent event) {
        Navigator.navigateToSportGUI(getStageFromEvent(event), User.getCurrentUsername());
    }
    @FXML
    public void AI(ActionEvent event) {
        Navigator.navigateToAIGUI(getStageFromEvent(event), User.getCurrentUsername());
    }
    @FXML
    public void science(ActionEvent event) {
        Navigator.navigateToScienceGUI(getStageFromEvent(event), User.getCurrentUsername());
    }


    public void setCurrentUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            User.setCurrentUsername(username); // Update static username
            this.currentUser = new User(username);
        } else {
            this.currentUser = null;
        }
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
}