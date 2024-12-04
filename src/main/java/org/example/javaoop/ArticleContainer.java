package org.example.javaoop;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.io.IOException;
import java.util.concurrent.*;

public class ArticleContainer {
    @FXML
    private Label articleTitle;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label contentArea;
    @FXML
    private Button likeButton;
    @FXML
    private Button dislikeButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;

    private String articleId;
    private String articleUrl;
    private String currentUsername;
    private Stage primaryStage;
    private User currentUser;


    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Thread pool for handling concurrent operations
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // Queue for handling database operations
    private final BlockingQueue<Runnable> dbOperationQueue = new LinkedBlockingQueue<>();

    // Thread for processing database operations
    private final Thread dbOperationProcessor;

    public ArticleContainer() {
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
                // Get current username from User class instead of HelloApplication
                String currentUsername = currentUser.getCurrentUsername();
                if (currentUsername != null) {
                    currentUser = new User(currentUsername);
                    setupButtonHandlers();
                } else {
                    showError("Error", "No user is currently logged in. Please log in again.");
                }
            });
        }, executorService);
    }


    private void handleLike() {
        if (currentUser == null || currentUser.getCurrentUsername()==null) {
            showError("Error", "No user is currently logged in. Please log in again.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                currentUser.likeArticle(articleId);
                Platform.runLater(() -> {
                    likeButton.setDisable(true);
                    dislikeButton.setDisable(true);
                    showSuccess("Thank you for your feedback!");
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Database Error", "Could not record like: " + e.getMessage())
                );
            }
        }, executorService);
    }

    private void handleDislike() {
        CompletableFuture.runAsync(() -> {
            try {
                currentUser.disLikeArticle(articleId);
                Platform.runLater(() -> {
                    likeButton.setDisable(true);
                    dislikeButton.setDisable(true);
                    showSuccess("Thank you for your feedback!");
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Database Error", "Could not record dislike: " + e.getMessage())
                );
            }
        }, executorService);
    }

    private void handleSave() {
        CompletableFuture.runAsync(() -> {
            try {
                currentUser.saveArticle(articleId);
                Platform.runLater(() -> {
                    saveButton.setDisable(true);
                    showSuccess("Article saved successfully!");
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Database Error", "Could not save article: " + e.getMessage())
                );
            }
        }, executorService);
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        this.currentUser = new User(username);
    }

    public void setArticleData(String articleId, String title, String url, String category, String username) {
        CompletableFuture.runAsync(() -> {
            if (username == null || username.trim().isEmpty()) {
                Platform.runLater(() -> showError("Error", "No user is currently logged in. Please log in again."));
                return;
            }
            currentUser = new User(username);
            this.articleId = articleId;
            this.articleUrl = url;

            Platform.runLater(() -> {
                articleTitle.setText(title);
                categoryLabel.setText("Category: " + category);

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                dateLabel.setText("Published: " + now.format(formatter));
            });

            loadArticleContent();
        }, executorService);
    }

    private void setupButtonHandlers() {
        likeButton.setOnAction(event -> handleLike());
        dislikeButton.setOnAction(event -> handleDislike());
        saveButton.setOnAction(event -> handleSave());
    }

    private void loadArticleContent() {
        CompletableFuture.runAsync(() -> {
            try {
                Document doc = Jsoup.connect(articleUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                String content = doc.select("article, .article-content, .story-content, .main-content, .post-content").text();
                Platform.runLater(() -> contentArea.setText(content));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Error", "Could not load article content: " + e.getMessage()));
            }
        }, executorService);
    }

    public void recommended(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("recommend.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleStyle.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Cleanup method to properly shutdown threads when the controller is no longer needed
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