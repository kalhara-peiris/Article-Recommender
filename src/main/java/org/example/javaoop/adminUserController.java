package org.example.javaoop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class adminUserController implements Initializable {
    // FXML injected UI components
    @FXML private Button dashboardBtn;
    @FXML private Button articlesBtn;
    @FXML private Button usersBtn;
    @FXML private Button logoutBtn;
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Label totalUsers;
    @FXML private TextField searchField;
    @FXML private VBox userList;
    @FXML private Pagination userPagination;

    // Constants and instance variables
    private static final int ITEMS_PER_PAGE = 10;
    private ObservableList<User> users;
    private Admin admin;

    // Initialize controller
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        admin = new Admin();
        users = FXCollections.observableArrayList();
        loadUsers();
        setupSearchListener();
        setupPagination();
        setupButtons();
    }

    // Load users from database
    private void loadUsers() {
        clearUserList();
        try {
            users = admin.getUsers();
            totalUsers.setText(String.valueOf(users.size()));
            showPage(0);
        } catch (SQLException e) {
            showError("Database Error", "Could not load users: " + e.getMessage());
        }
    }

    // Delete user from database and update UI
    private void deleteUser(String username) {
        try {
            if (admin.deleteUser(username)) {
                users.removeIf(user -> user.getUsername().equals(username));
                setupPagination();
                showPage(userPagination.getCurrentPageIndex());
                totalUsers.setText(String.valueOf(users.size()));
            }
        } catch (SQLException e) {
            showError("Delete Error", "Could not delete user: " + e.getMessage());
        }
    }

    // Add user item to the UI list
    private void addUserToView(User user) {
        VBox userItem = new VBox();
        userItem.getStyleClass().add("user-item");

        HBox itemContent = new HBox(20);
        itemContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox userInfo = new VBox(5);
        Label usernameLabel = new Label(user.getUsername());
        usernameLabel.getStyleClass().add("username-label");

        String maskedPassword = "*".repeat(8);
        Label passwordLabel = new Label(maskedPassword);
        passwordLabel.getStyleClass().add("password-label");

        userInfo.getChildren().addAll(usernameLabel, passwordLabel);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(e -> deleteUser(user.getUsername()));

        itemContent.getChildren().addAll(userInfo, deleteButton);
        itemContent.setPadding(new javafx.geometry.Insets(10));

        userItem.getChildren().add(itemContent);
        userList.getChildren().add(userItem);
    }

    // Setup pagination controls
    private void setupPagination() {
        int pageCount = (int) Math.ceil(users.size() / (double) ITEMS_PER_PAGE);
        pageCount = Math.max(1, pageCount);
        userPagination.setPageCount(pageCount);
        userPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) ->
                showPage(newIndex.intValue()));
    }

    // Display users for current page
    private void showPage(int pageIndex) {
        clearUserList();
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, users.size());

        for (int i = fromIndex; i < toIndex; i++) {
            addUserToView(users.get(i));
        }
    }

    // Setup search functionality
    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });
    }

    // Filter users based on search text
    private void filterUsers(String searchText) {
        try {
            ObservableList<User> filteredUsers = admin.searchUsers(searchText);
            clearUserList();
            for (User user : filteredUsers) {
                addUserToView(user);
            }
        } catch (SQLException e) {
            showError("Search Error", "Could not search users: " + e.getMessage());
        }
    }

    // Setup navigation button handlers
    private void setupButtons() {
        dashboardBtn.setOnAction(e -> handleDashboard());
        articlesBtn.setOnAction(e -> handleArticles());
        logoutBtn.setOnAction(e -> handleLogout());
    }

    // Navigate to dashboard
    private void handleDashboard() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminInterface.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("adminUI.css").toExternalForm());
            Stage stage = (Stage)(dashboardBtn.getScene().getWindow());
            stage.setTitle("Admin Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load dashboard: " + e.getMessage());
        }
    }

    // Navigate to articles view
    private void handleArticles() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminViewArticle.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("adminArticleView.css").toExternalForm());
            Stage stage = (Stage)(dashboardBtn.getScene().getWindow());
            stage.setTitle("Article Management");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load articles view: " + e.getMessage());
        }
    }

    // Handle logout action
    private void handleLogout() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("signUp.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("signUp.css").toExternalForm());
            Stage stage = (Stage)(dashboardBtn.getScene().getWindow());
            stage.setTitle("Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load login page: " + e.getMessage());
        }
    }

    // Clear the user list view
    private void clearUserList() {
        userList.getChildren().clear();
    }

    // Display error dialog
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
