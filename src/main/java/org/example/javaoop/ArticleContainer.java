
package org.example.javaoop;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.io.IOException;

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

    public void initialize() {
        setupButtonHandlers();
    }

    public void setArticleData(String articleId, String title, String url, String category, String username) {
        this.articleId = articleId;
        this.articleUrl = url;
        this.currentUsername = username;

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
        backButton.setOnAction(event -> handleBack());
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
                javafx.application.Platform.runLater(() -> {
                    contentArea.setText(content);
                });

            } catch (IOException e) {
                showError("Error", "Could not load article content: " + e.getMessage());
            }
        }).start();
    }

    private void handleLike() {
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

    private void recordInteraction(String interactionType) {
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String interactionId = java.util.UUID.randomUUID().toString();
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
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
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

    private void handleBack() {
        try {
            // Load the technology view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("technology-view.fxml"));
            Scene scene = new Scene(loader.load());

            // Get the TechnologyController and initialize it if needed
            TechnologyController controller = loader.getController();
            controller.setCurrentUsername(currentUsername);

            // Get the current stage and set the new scene
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showError("Navigation Error", "Could not return to technology view: " + e.getMessage());
        }
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private void showError(String title, String content) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showSuccess(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}