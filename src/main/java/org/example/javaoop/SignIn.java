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

    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @FXML
    public void handleSignIn(ActionEvent event) {
        String username = signInUsername.getText().trim();
        String password = signInPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all fields.");
            return;
        }
        if (username.equals("admin") && password.equals("12345")) {
            Platform.runLater(() -> {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("adminInterface.fxml"));
                    Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
                    scene.getStylesheets().add(getClass().getResource("adminUI.css").toExternalForm());

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    HelloApplication app = (HelloApplication) stage.getUserData();
                    app.setCurrentUsername(username);

                    stage.setScene(scene);
                    stage.setTitle("Admin Dashboard");
                    stage.show();
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load admin page: " + e.getMessage());
                }
            });
            return;
        }

        CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT * FROM user WHERE username = ? AND password = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);

                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        Platform.runLater(() -> {
                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("recommend.fxml"));
                                Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
                                scene.getStylesheets().add(getClass().getResource("articleStyle.css").toExternalForm());

                                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                HelloApplication app = (HelloApplication) stage.getUserData();
                                app.setCurrentUsername(username);

                                stage.setScene(scene);
                                stage.show();
                            } catch (IOException e) {
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to load main page: " + e.getMessage());
                            }
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.")
                        );
                    }
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