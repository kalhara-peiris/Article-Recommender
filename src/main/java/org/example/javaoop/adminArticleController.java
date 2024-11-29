package org.example.javaoop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class adminArticleController implements Initializable {
    @FXML private Button dashBoardBtn;
    @FXML private Button articlesBtn;
    @FXML private Button usersBtn;
    @FXML private Button logoutBtn;
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Label totalArticles;
    @FXML private Label publishedArticles;
    @FXML private Label pendingArticles;
    @FXML private TextField searchField;
    @FXML private VBox articleList;
    @FXML private Pagination articlePagination;

    private static final int ITEMS_PER_PAGE = 10;
    private ObservableList<Article> articles;
    private Admin admin;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        admin = new Admin();
        articles = FXCollections.observableArrayList();
        loadArticles();
        setupSearchListener();
        setupPagination();
        setupButtons();
    }

    private void loadArticles() {
        clearArticleList();
        try {
            articles = admin.getArticles();
            totalArticles.setText(String.valueOf(articles.size()));
            showPage(0);
        } catch (SQLException e) {
            showError("Database Error", "Could not load articles: " + e.getMessage());
        }
    }

    private void deleteArticle(String articleId) {
        try {
            if (admin.deleteArticle(articleId)) {
                articles.removeIf(article -> article.getArticleId().equals(articleId));
                setupPagination();
                showPage(articlePagination.getCurrentPageIndex());
                totalArticles.setText(String.valueOf(articles.size()));
            }
        } catch (SQLException e) {
            showError("Delete Error", "Could not delete article: " + e.getMessage());
        }
    }

    private void addArticleToView(Article article) {
        VBox articleItem = new VBox();
        articleItem.getStyleClass().add("article-item");

        Label titleLabel = new Label(article.getTitle());
        titleLabel.getStyleClass().add("article-title");

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(e -> deleteArticle(article.getArticleId()));

        HBox itemContent = new HBox(10);
        itemContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox titleBox = new VBox();
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.getChildren().add(titleLabel);
        deleteButton.setMinWidth(60);
        deleteButton.setPrefWidth(70);
        deleteButton.setMaxHeight(30);

        itemContent.getChildren().addAll(titleBox, deleteButton);
        articleItem.getChildren().add(itemContent);

        articleItem.setPadding(new javafx.geometry.Insets(15));
        articleList.getChildren().add(articleItem);
    }

    private void setupPagination() {
        int pageCount = (int) Math.ceil(articles.size() / (double) ITEMS_PER_PAGE);
        pageCount = Math.max(1, pageCount);
        articlePagination.setPageCount(pageCount);
        articlePagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) ->
                showPage(newIndex.intValue()));
    }

    private void showPage(int pageIndex) {
        clearArticleList();
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, articles.size());

        for (int i = fromIndex; i < toIndex; i++) {
            addArticleToView(articles.get(i));
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterArticles(newValue);
        });
    }

    private void filterArticles(String searchText) {
        try {
            ObservableList<Article> filteredArticles = admin.searchArticles(searchText);
            clearArticleList();
            for (Article article : filteredArticles) {
                addArticleToView(article);
            }
        } catch (SQLException e) {
            showError("Search Error", "Could not search articles: " + e.getMessage());
        }
    }

    private void setupButtons() {
        dashBoardBtn.setOnAction(e -> handleDashboard());
        usersBtn.setOnAction(e -> handleUsers());
        logoutBtn.setOnAction(e -> handleLogout());
    }

    private void handleDashboard() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminInterface.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("adminUI.css").toExternalForm());
            Stage stage = (Stage)(dashBoardBtn.getScene().getWindow());
            stage.setTitle("Admin Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load dashboard: " + e.getMessage());
        }
    }

    private void handleUsers() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminUserView.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("adminUserView.css").toExternalForm());
            Stage stage = (Stage)(dashBoardBtn.getScene().getWindow());
            stage.setTitle("User Management");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load user management view: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("signUp.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("signUp.css").toExternalForm());
            Stage stage = (Stage)(dashBoardBtn.getScene().getWindow());
            stage.setTitle("Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load login page: " + e.getMessage());
        }
    }

    private void clearArticleList() {
        articleList.getChildren().clear();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}