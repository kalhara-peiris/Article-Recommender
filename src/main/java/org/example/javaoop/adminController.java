package org.example.javaoop;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

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
    private ProgressBar progressBar;

    @FXML
    private Label statusText;

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
        articleCollectors = new Articles();
        // Initialize article count on startup
        updateArticleCount();
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
}