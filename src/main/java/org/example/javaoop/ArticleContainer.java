
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.io.IOException;
import java.util.UUID;

public class ArticleContainer  {
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

    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (currentUsername == null) {
                try {
                    Stage stage = (Stage) likeButton.getScene().getWindow();
                    if (stage != null && stage.getUserData() instanceof HelloApplication) {
                        HelloApplication app = (HelloApplication) stage.getUserData();
                        currentUsername = app.getCurrentUsername();
                    }
                } catch (Exception e) {
                    System.err.println("Error in initialize: " + e.getMessage());
                }
            }
            setupButtonHandlers();
        });
    }



    public void setArticleData(String articleId, String title, String url, String category, String username) {
        setCurrentUsername(username);
        if (username == null || username.trim().isEmpty()) {
            showError("Error", "No user is currently logged in. Please log in again.");
            return;
        }
        this.articleId = articleId;
        this.articleUrl = url;


        articleTitle.setText(title);
        categoryLabel.setText("Category: " + category);

        // Set current date
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        dateLabel.setText("Published: " + now.format(formatter));

        // Load article content
        loadArticleContent();
    }

    private void setupButtonHandlers() {

        likeButton.setOnAction(event -> handleLike());
        dislikeButton.setOnAction(event -> handleDislike());
        saveButton.setOnAction(event -> handleSave());
    }

    private void loadArticleContent() {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(articleUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                String content = doc.select("article, .article-content, .story-content, .main-content, .post-content").text();

                // Update UI on JavaFX Application Thread
                Platform.runLater(() -> {
                    contentArea.setText(content);
                });

            } catch (IOException e) {
                showError("Error", "Could notss load article content: " + e.getMessage());
            }
        }).start();
    }

    private void handleLike() {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            showError("Error", "No user is currently logged in. Please log in again.");
            return;
        }
        recordInteraction("LIKE");
        updateUserPreference(5); // Increase preference score by 5
        likeButton.setDisable(true);
        dislikeButton.setDisable(true);
        showSuccess("Thank you for your feedback!");
    }

    private void handleDislike() {
        recordInteraction("DISLIKE");
        updateUserPreference(-3); // Decrease preference score by 3
        likeButton.setDisable(true);
        dislikeButton.setDisable(true);
        showSuccess("Thank you for your feedback!");
    }

    private void handleSave() {
        recordInteraction("SAVE");
        saveButton.setDisable(true);
        showSuccess("Article saved successfully!");
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

    private void recordInteraction(String interactionType) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String interactionId = UUID.randomUUID().toString();
            String query = "INSERT INTO userinteraction (interactionID, username, ArticleID, interactionType) " +
                    "VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, interactionId);
                pstmt.setString(2, currentUsername);
                pstmt.setString(3, articleId);
                pstmt.setString(4, interactionType);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not record interaction: " + e.getMessage());
        }
    }

    private void updateUserPreference(int scoreChange) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Get the category ID for this article
            String categoryId = getCategoryId();
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

    private String getCategoryId() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
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



    public void setCurrentUsername(String username) {
        this.currentUsername = username;
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
}