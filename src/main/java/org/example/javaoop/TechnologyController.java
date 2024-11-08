package org.example.javaoop;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class TechnologyController extends CatergorizedArticles {
    @FXML
    public Button readMore1;
    @FXML
    public Button readMore2;
    @FXML
    public Button readMore3;
    @FXML
    private VBox articlesContainer;

    @FXML
    private Label articleTitle1;

    @FXML
    private Label articleTitle2;

    @FXML
    private Label articleTitle3;

    @FXML
    private Button skipButton;

    @FXML
    private TextField searchField;

    private List<Article> currentArticles;
    private int currentIndex = 0;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public TechnologyController() {
        super(Runtime.getRuntime().availableProcessors() * 2);
        currentArticles = new ArrayList<>();
    }

    @FXML
    public void initialize() {
        loadTechnologyArticles();
        displayCurrentArticles();

        skipButton.setOnAction(event -> handleSkipButton());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchArticles(newValue);
        });
    }

    private void loadTechnologyArticles() {
        currentArticles.clear();
        try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM ArticleCategory WHERE categoryID = 'C01'"; // C01 is Technology
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Article article = new Article(
                            rs.getString("ArticleID"),
                            rs.getString("title"),
                            rs.getString("url")
                    );
                    currentArticles.add(article);
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Error loading technology articles: " + e.getMessage());
        }
    }

    private void displayCurrentArticles() {
        clearArticleTitles();

        if (currentIndex >= currentArticles.size()) {
            currentIndex = 0; // Reset to beginning if we've reached the end
        }

        for (int i = 0; i < 3; i++) {
            int articleIndex = currentIndex + i;
            if (articleIndex < currentArticles.size()) {
                Article article = currentArticles.get(articleIndex);
                setArticleTitle(i + 1, article.getTitle());

                // Set up read more button for this article
                setupReadMoreButton(i + 1, article);
            }
        }
    }

    private void clearArticleTitles() {
        articleTitle1.setText("");
        articleTitle2.setText("");
        articleTitle3.setText("");
    }

    private void setArticleTitle(int position, String title) {
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

    private void setupReadMoreButton(int position, Article article) {
        Button readMoreButton = findReadMoreButton(position);
        if (readMoreButton != null) {
            readMoreButton.setOnAction(event -> showArticleContent(article));
        }
    }

    private Button findReadMoreButton(int position) {
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
    private void showArticleContent(Article article) {
        try {
            // Load the article detail view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());
            // Get the controller and initialize it with article data
            ArticleContainer controller = loader.getController();
            controller.setArticleData(
                    article.getId(),
                    article.getTitle(),
                    article.getUrl(),
                    "Technology",
                    currentUsername
            );

            // Get the current stage and set the new scene
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showError("Error", "Could not load article detail view: " + e.getMessage());
        }
    }

    private String currentUsername;
    public void setCurrentUsername(String username){
        this.currentUsername=username;
    }
    private void showArticleContents(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            String content = doc.select("article, .article-content, .story-content").text();

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Article Content");
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();

        } catch (IOException e) {
            showError("Error", "Could not load article content: " + e.getMessage());
        }
    }

    @FXML
    private void handleSkipButton() {
        currentIndex += 3;
        displayCurrentArticles();
    }

    private void searchArticles(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            loadTechnologyArticles(); // Reset to show all articles
        } else {
            currentArticles.clear();
            try (Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT * FROM ArticleCategory WHERE categoryID = 'C01' AND title LIKE ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, "%" + searchTerm + "%");
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        Article article = new Article(
                                rs.getString("ArticleID"),
                                rs.getString("title"),
                                rs.getString("url")
                        );
                        currentArticles.add(article);
                    }
                }
                currentIndex = 0;
                displayCurrentArticles();
            } catch (SQLException e) {
                showError("Search Error", "Error searching articles: " + e.getMessage());
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner class to hold article data
    private static class Article {
        private final String id;
        private final String title;
        private final String url;

        public Article(String id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
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
    public void technology(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("technology.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void health(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("health.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void sport(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("sport.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void AI(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AI.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void science(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("science.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // Navigation methods for category buttons

}