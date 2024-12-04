package org.example.javaoop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.control.ScrollPane;

public class userProfileController {
    @FXML private Label username;
    @FXML private Label memberSince;
    @FXML private Circle profileCircle;
    @FXML private Label readCount;
    @FXML private Label likeCount;
    @FXML private Label saveCount;
    @FXML private Label dislikeCount;
    @FXML private Label skipCount;
    @FXML private TabPane tabPane;
    @FXML private Button backButton;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private String currentUsername;



    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // Get username from User class instead of stage userData
            String username = User.getCurrentUsername();
            if (username != null) {
                currentUsername = username;
                loadUserProfile();
                loadUserStats();
                setupTabHandlers();
            } else {
                showError("Error", "No user is currently logged in");
            }
        });

        backButton.setOnAction(event -> {
            try {
                recommended(event);
            } catch (IOException e) {
                showError("Navigation Error", "Could not navigate back: " + e.getMessage());
            }
        });
    }



    private void setupTabHandlers() {
        // Add listener for tab selection changes
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadArticlesForTab(newTab);
            }
        });

        // Load content for the initially selected tab
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            loadArticlesForTab(selectedTab);
        }
    }


    private void loadUserProfile() {
        String username = getCurrentUsername();
        if (username != null) {
            this.username.setText(username);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT MIN(interactionTime) as firstInteraction FROM userinteraction WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        Timestamp firstInteraction = rs.getTimestamp("firstInteraction");
                        if (firstInteraction != null) {
                            LocalDateTime dateTime = firstInteraction.toLocalDateTime();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                            memberSince.setText("Member since: " + dateTime.format(formatter));
                        } else {
                            memberSince.setText("Member since: New Member");
                        }
                    }
                }
            } catch (SQLException e) {
                showError("Database Error", "Error loading user profile: " + e.getMessage());
            }
        }
    }

    private void loadUserStats() {
        String username = getCurrentUsername();
        if(username==null) return;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT interactionType, COUNT(*) as count FROM userinteraction " +
                    "WHERE username = ? GROUP BY interactionType";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                int reads = 0, likes = 0, saves = 0, dislikes = 0, skips = 0;

                while (rs.next()) {
                    String type = rs.getString("interactionType");
                    int count = rs.getInt("count");

                    switch (type) {
                        case "READ": reads = count; break;
                        case "LIKE": likes = count; break;
                        case "SAVE": saves = count; break;
                        case "DISLIKE": dislikes = count; break;
                        case "SKIP": skips = count; break;
                    }
                }

                readCount.setText("Read: " + reads + " articles");
                likeCount.setText("Liked: " + likes + " articles");
                saveCount.setText("Saved: " + saves + " articles");
                dislikeCount.setText("Disliked: " + dislikes + " articles");
                skipCount.setText("Skipped: " + skips);
            }
        } catch (SQLException e) {
            showError("Database Error", "Error loading user statistics: " + e.getMessage());
        }
    }

    private void loadArticlesForTab(Tab tab) {
        String username = getCurrentUsername();
        String interactionType = getInteractionTypeForTab(tab.getText());
        System.out.println("Loading articles for interaction type: " + interactionType); // Debug log

        VBox articlesList = new VBox(10);
        articlesList.getStyleClass().add("articles-list");
        articlesList.setStyle("-fx-padding: 20;"); // Add padding to the VBox

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT ac.title, ac.categoryID, ui.interactionTime " +
                    "FROM userinteraction ui " +
                    "JOIN ArticleCategory ac ON ui.ArticleID = ac.ArticleID " +
                    "WHERE ui.username = ? AND ui.interactionType = ? " +
                    "ORDER BY ui.interactionTime DESC";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, interactionType);
                System.out.println("Executing query with username: " + currentUsername +
                        " and type: " + interactionType); // Debug log
                ResultSet rs = stmt.executeQuery();

                boolean hasArticles = false;
                while (rs.next()) {
                    hasArticles = true;
                    VBox articleItem = new VBox(5);
                    articleItem.getStyleClass().add("article-item");
                    articleItem.setStyle("-fx-background-color: white; -fx-padding: 15; " +
                            "-fx-border-color: #e0e0e0; -fx-border-radius: 4;");

                    Label titleLabel = new Label(rs.getString("title"));
                    titleLabel.getStyleClass().add("article-title");
                    titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                    String category = getCategoryName(rs.getString("categoryID"));
                    LocalDateTime interactionTime = rs.getTimestamp("interactionTime").toLocalDateTime();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                    Label metaLabel = new Label(String.format("Category: %s | %s on: %s",
                            category, tab.getText(), interactionTime.format(formatter)));
                    metaLabel.getStyleClass().add("article-meta");
                    metaLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

                    articleItem.getChildren().addAll(titleLabel, metaLabel);
                    articlesList.getChildren().add(articleItem);
                }

                if (!hasArticles) {
                    Label noArticlesLabel = new Label("No " + tab.getText().toLowerCase() + " articles yet");
                    noArticlesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666666; -fx-padding: 20;");
                    articlesList.getChildren().add(noArticlesLabel);
                }
            }

            ScrollPane scrollPane = new ScrollPane(articlesList);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            tab.setContent(scrollPane);

        } catch (SQLException e) {
            showError("Database Error", "Error loading articles: " + e.getMessage());
            System.out.println("SQL Error: " + e.getMessage()); // Debug log
        }
    }

    private String getInteractionTypeForTab(String tabText) {
        switch (tabText.toUpperCase()) {
            case "LIKED": return "LIKE";
            case "READ": return "READ";
            case "SAVED": return "SAVE";
            case "DISLIKED": return "DISLIKE";
            case "SKIPPED": return "SKIP";
            default: return tabText.toUpperCase();
        }
    }

    private String getCategoryName(String categoryId) {
        switch (categoryId) {
            case "C01": return "Technology";
            case "C02": return "Health";
            case "C03": return "Sports";
            case "C04": return "AI";
            case "C06": return "Science";
            default: return "Unknown";
        }
    }

    @FXML
    public void recommendeds(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("recommend.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleStyle.css").toExternalForm());

        if (stage.getUserData() instanceof HelloApplication) {
            HelloApplication app = (HelloApplication) stage.getUserData();
            stage.setUserData(app);
        }

        stage.setScene(scene);
        stage.show();
    }
    @FXML
    public void recommended(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("recommend.fxml"));
        Parent root = loader.load();

        // Get the controller and set the current username
        RecommendController controller = loader.getController();
        String username = User.getCurrentUsername();
        if (username != null) {
            controller.setCurrentUsername(username);
        }

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleStyle.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void setCurrentUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            User.setCurrentUsername(username); // Update the static username
            this.currentUsername = username;
            if (this.username != null) {
                this.username.setText(username);
                loadUserProfile();
                loadUserStats();
                setupTabHandlers();
            }
        }
    }

    // Helper method to get current username
    private String getCurrentUsername() {
        return currentUsername != null ? currentUsername : User.getCurrentUsername();
    }
}