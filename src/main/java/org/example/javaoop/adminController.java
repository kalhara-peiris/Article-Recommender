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
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class adminController {
    private CatergorizedArticles categorizer;
    @FXML
    private Button startCollectionButton;
    @FXML
    private Button startCategorizationButton;
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

    private Admin admin;
    private boolean isCategorizationRunning = false;

    @FXML
    public void initialize() {
        admin = new Admin();
        categorizer = new CatergorizedArticles(Runtime.getRuntime().availableProcessors() * 2);
        updateArticleCount();
        updateUserCount();
    }

    private void updateArticleCount() {
        try {
            int totalArticles = admin.getTotalArticleCount();
            articleCountLabel.setText(String.valueOf(totalArticles));
        } catch (SQLException e) {
            e.printStackTrace();
            articleCountLabel.setText("Error");
        }
    }

    private void updateUserCount() {
        try {
            int totalUsers = admin.getTotalUserCount();
            userCount.setText(String.valueOf(totalUsers));
        } catch (SQLException e) {
            e.printStackTrace();
            userCount.setText("Error");
        }
    }

    @FXML
    public void loadArticle(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminViewArticle.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("adminArticleView.css").toExternalForm());
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void loadUser(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminUserView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("adminUserView.css").toExternalForm());
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void logOut(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("signUp.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("signUp.css").toExternalForm());
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleStartCollection() {
        startCollectionButton.setDisable(true);
        initializeCollectionUI();

        Task<Void> collectionTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                admin.collectArticles(new Admin.CollectionProgressCallback() {
                    @Override
                    public void onProgress(double progress) {
                        Platform.runLater(() -> updateCollectionProgress(progress));
                    }
                });
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

        new Thread(collectionTask).start();
    }


    @FXML
    private void handleStartCategorization() {
        if (isCategorizationRunning) return;

        startCategorizationButton.setDisable(true);
        startCollectionButton.setDisable(true);
        isCategorizationRunning = true;
        initializeCategorizationUI();

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

                // Set up progress callback
                categorizer.setCategoryProgressCallback((processed, total) -> {
                    double progress = (double) processed / total;
                    Platform.runLater(() -> updateCategorizationProgress(processed, total, progress));
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

    private void initializeCollectionUI() {
        progressBar.setProgress(0);
        statusText.setText("Collecting articles...");
        currentStatusLabel.setText("Collection in Progress");
        progressText.setText("0 articles collected out of target " + admin.getTotalTargetArticles());
        collectionRateLabel.setText("Average collection rate: 0 articles/minute");
    }

    private void initializeCategorizationUI() {
        progressBar.setProgress(0);
        statusText.setText("Categorizing articles...");
        currentStatusLabel.setText("Categorization in Progress");
        progressText.setText("Preparing for categorization...");
        collectionRateLabel.setText("Processing articles...");
    }

    private void updateCollectionProgress(double progress) {
        progressBar.setProgress(progress);
        int collectedArticles = (int) (progress * admin.getTotalTargetArticles());
        progressText.setText(String.format("%d articles collected out of target %d",
                collectedArticles, admin.getTotalTargetArticles()));
        statusText.setText("Collecting articles...");
        currentStatusLabel.setText(String.format("Collection in Progress (%d of %d articles)",
                collectedArticles, admin.getTotalTargetArticles()));
        collectionRateLabel.setText(String.format("Average collection rate: %d articles/minute",
                Math.max(1, (int)(collectedArticles * 2))));
    }

    private void updateCategorizationProgress(int processed, int total, double progress) {
        progressBar.setProgress(progress);
        progressText.setText(String.format("%d articles categorized out of %d", processed, total));
        collectionRateLabel.setText(String.format("Processing articles... %d%%", (int)(progress * 100)));
    }

    private void updateUIAfterCollection() {
        updateArticleCount();
        LocalDateTime now = LocalDateTime.now();
        lastUpdatedLabel.setText("Last Updated: " +
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        statusText.setText("Collection Complete");
        currentStatusLabel.setText("Finished collecting articles");
        progressBar.setProgress(1.0);
        progressText.setText(String.format("%d articles collected", admin.getTotalTargetArticles()));
    }

    private void updateUIAfterCategorization() {
        LocalDateTime now = LocalDateTime.now();
        lastUpdatedLabel.setText("Last Updated: " +
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        statusText.setText("Categorization Complete");
        currentStatusLabel.setText("Finished categorizing articles");
        progressBar.setProgress(1.0);
        updateArticleCount();
    }

    private int getUncategorizedArticleCount() {
        try (Connection conn = DriverManager.getConnection(Article.dbUrl, Article.dbUser, Article.dbPassword)) {
            String countQuery = "SELECT COUNT(*) AS total FROM Article a " +
                    "WHERE NOT EXISTS (SELECT 1 FROM ArticleCategory ac WHERE ac.ArticleID = a.ArticleID)";
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

}