package org.example.javaoop;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUp {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox termsCheckbox;

    @FXML
    private Button signUpButton;

    private DBHandler dbHandler = new DBHandler();

    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);


    @FXML
    public void handleSignUp(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!validateInput(username, password)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                User newUser = new User(username, password);
                if (registerUser(newUser)) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
                        Stage stage = (Stage) signUpButton.getScene().getWindow();
                        Navigator.navigateToLogin(stage);
                    });
                }
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to create account")
                );
            }
        }, executorService);
    }
    private boolean registerUser(User user) throws SQLException {
        // Check if username exists
        if (dbHandler.isUsernameTaken(user.getUsername())) {
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Error", "Username already exists")
            );
            return false;
        }

        // Insert new user
        return dbHandler.insertUser(user.getUsername(), user.getPassword());
    }


    private boolean validateInput(String username, String password) {
        if(username.equals("admin") && password.equals("12345")){
            showAlert(Alert.AlertType.ERROR,"Error","Please sign in as admin");
            return false;
        }
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all fields.");
            return false;
        }
        if (password.length() < 8) {
            showAlert(Alert.AlertType.ERROR, "Error", "Password must be at least 8 characters long.");
            return false;
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            showAlert(Alert.AlertType.ERROR, "Error", "Password must contain both letters and numbers.");
            return false;
        }
        if (!termsCheckbox.isSelected()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please accept the terms and conditions.");
            return false;
        }
        return true;
    }


    @FXML
    public void loadSignIn(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("signIn.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("signIn.css").toExternalForm());

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