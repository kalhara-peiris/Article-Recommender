package org.example.javaoop;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignIn {
    @FXML
    private TextField signInUsername;
    @FXML
    private PasswordField signInPassword;
    @FXML
    private Button logIn;

    private User user;
    private final DBHandler dbHandler = new DBHandler();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @FXML
    public void handleSignIn(ActionEvent event) {
        String username = signInUsername.getText().trim();
        String password = signInPassword.getText();

        if (!validateInput(username, password)) {
            return;
        }

        if (isAdminLogin(username, password)) {
            handleAdminLogin();
            return;
        }

        handleUserLogin(username, password);
    }

    private boolean validateInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all fields.");
            return false;
        }
        return true;
    }

    private boolean isAdminLogin(String username, String password) {
        return username.equals("admin") && password.equals("12345");
    }

    private void handleAdminLogin() {
        Platform.runLater(() -> {
            Stage stage = (Stage) logIn.getScene().getWindow();
            user.setCurrentUsername("admin");
            Navigator.navigateToAdmin(stage);
        });
    }

    private void handleUserLogin(String username, String password) {
        CompletableFuture.runAsync(() -> {
            try {
                if (dbHandler.validateUser(username, password)) {
                    Platform.runLater(() -> {
                        user.setCurrentUsername(username);
                        Stage stage = (Stage) logIn.getScene().getWindow();
                        Navigator.navigateToRecommendGUI(stage, username);
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.")
                    );
                }
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to connect to database: " + e.getMessage())
                );
            }
        }, executorService);
    }

    @FXML
    public void loadSignUp(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("signUp.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("signUp.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void cleanup() {
        executorService.shutdown();
    }
}