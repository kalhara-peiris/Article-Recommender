package org.example.javaoop;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class adminController {
    @FXML
    private Button startCollectionButton;

    @FXML
    private Button startCategorizationButton;

    private CatergorizedArticles categorizer;
    private boolean isCategorizationRunning = false;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusText;

    @FXML
    private Label userCount;

    @FXML
    private Label progressText;

    @FXML
    private Label collectionRateLabel;

    @FXML
    private Label articleCountLabel;

    @FXML
    private Label currentStatusLabel;

    @FXML
    private Label lastUpdatedLabel;

    private Articles articleCollectors;
    private int totalTargetArticles = 50;

    @FXML
    public void initialize() {
        updateArticleCount();
        updateUserCount();
        articleCollectors = new Articles();
        articleCollectors = new Articles();
        categorizer = new CatergorizedArticles(Runtime.getRuntime().availableProcessors() * 2);
        // Initialize article count on startup
        updateArticleCount();
    }

    public void loadUser(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminViewArticle.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("adminArticleView.css").toExternalForm());
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    private void updateArticleCount() {
        try (Connection conn = DriverManager.getConnection(Articles.dbUrl, Articles.dbUser, Articles.dbPassword)) {
            String countQuery = "SELECT COUNT(*) AS total FROM Article";
            try (PreparedStatement pstmt = conn.prepareStatement(countQuery);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    int totalArticles = rs.getInt("total");
                    articleCountLabel.setText(String.valueOf(totalArticles));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            articleCountLabel.setText("Error");
        }
    }
    private void updateUserCount() {
        try (Connection conn = DriverManager.getConnection(Articles.dbUrl, Articles.dbUser, Articles.dbPassword)) {
            String countQuery = "SELECT COUNT(*) AS total FROM user";
            try (PreparedStatement pstmt = conn.prepareStatement(countQuery);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    int totalUser = rs.getInt("total");
                    userCount.setText(String.valueOf(totalUser));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            articleCountLabel.setText("Error");
        }
    }

    @FXML
    private void handleStartCollection() {
        // Disable the button during collection
        startCollectionButton.setDisable(true);

        // Reset progress indicators
        progressBar.setProgress(0);
        statusText.setText("Collecting articles...");
        currentStatusLabel.setText("Collection in Progress");
        progressText.setText("0 articles collected out of target " + totalTargetArticles);
        collectionRateLabel.setText("Average collection rate: 0 articles/minute");

        // Create a background task for article collection
        Task<Void> collectionTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                long startTime = System.currentTimeMillis();

                // Custom implementation to track progress
                articleCollectors.collectArticles(progress -> updateCollectionProgress(progress));

                return null;
            }


            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    updateUIAfterCollection();
                    startCollectionButton.setDisable(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusText.setText("Collection Failed");
                    currentStatusLabel.setText("Error during collection");
                    startCollectionButton.setDisable(false);
                });
            }
        };

        // Run the task in a background thread
        new Thread(collectionTask).start();
    }
    private void updateCollectionProgress(double progress) {
        Platform.runLater(() -> {
            // Update progress bar
            progressBar.setProgress(progress);

            // Update progress text
            int collectedArticles = (int) (progress * totalTargetArticles);
            progressText.setText(String.format("%d articles collected out of target %d",
                    collectedArticles, totalTargetArticles));

            // Update status text
            statusText.setText("Collecting articles...");
            currentStatusLabel.setText(String.format("Collection in Progress (%d of %d articles)",
                    collectedArticles, totalTargetArticles));

            // Update collection rate (simple estimation)
            collectionRateLabel.setText(String.format("Average collection rate: %d articles/minute",
                    Math.max(1, (int)(collectedArticles * 2))));
        });
    }


    private void updateUIAfterCollection() {
        // Update article count from database
        updateArticleCount();

        // Update last updated time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lastUpdatedLabel.setText("Last Updated: " + now.format(formatter));

        // Update status
        statusText.setText("Collection Complete");
        currentStatusLabel.setText("Finished collecting articles");
        progressBar.setProgress(1.0);
        progressText.setText(String.format("%d articles collected", totalTargetArticles));
    }
    @FXML
    private void handleStartCategorization() {
        if (isCategorizationRunning) {
            return;
        }

        // Disable both buttons during categorization
        startCategorizationButton.setDisable(true);
        startCollectionButton.setDisable(true);
        isCategorizationRunning = true;

        // Reset progress indicators
        progressBar.setProgress(0);
        statusText.setText("Categorizing articles...");
        currentStatusLabel.setText("Categorization in Progress");
        progressText.setText("Preparing for categorization...");
        collectionRateLabel.setText("Processing articles...");

        Task<Void> categorizationTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Get count of uncategorized articles
                int totalUncategorized = getUncategorizedArticleCount();

                if (totalUncategorized == 0) {
                    Platform.runLater(() -> {
                        statusText.setText("No articles to categorize");
                        currentStatusLabel.setText("All articles are already categorized");
                        progressBar.setProgress(1.0);
                    });
                    return null;
                }
                final int targetArticles = Math.min(50, totalUncategorized);
                final int[] processedCount = {0};
                categorizer.setCategoryProgressCallback((processed, total) -> {
                    processedCount[0] = processed;
                    double progress = (double) processed / totalUncategorized;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                        progressText.setText(String.format("%d articles categorized out of %d", processed, totalUncategorized));
                        collectionRateLabel.setText(String.format("Processing articles... %d%%", (int)(progress * 100)));
                    });
                });

                // Start categorization
                categorizer.processCategorization();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    updateUIAfterCategorization();
                    isCategorizationRunning = false;
                    startCategorizationButton.setDisable(false);
                    startCollectionButton.setDisable(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusText.setText("Categorization Failed");
                    currentStatusLabel.setText("Error during categorization");
                    isCategorizationRunning = false;
                    startCategorizationButton.setDisable(false);
                    startCollectionButton.setDisable(false);
                });
            }
        };

        new Thread(categorizationTask).start();
    }

    private int getUncategorizedArticleCount() {
        try (Connection conn = DriverManager.getConnection(Articles.dbUrl, Articles.dbUser, Articles.dbPassword)) {
            String countQuery = "SELECT COUNT(*) AS total FROM Articletest a " +
                    "WHERE NOT EXISTS (SELECT 1 FROM ArticleCategorytest ac WHERE ac.ArticleID = a.ArticleID)";
            try (PreparedStatement pstmt = conn.prepareStatement(countQuery);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    private void updateUIAfterCategorization() {
        // Update last updated time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lastUpdatedLabel.setText("Last Updated: " + now.format(formatter));

        // Update status
        statusText.setText("Categorization Complete");
        currentStatusLabel.setText("Finished categorizing articles");
        progressBar.setProgress(1.0);

        // Update article count
        updateArticleCount();
    }

}