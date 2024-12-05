package org.example.javaoop;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class Navigator {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/javacw";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String FXML_PATH = "/org/example/javaoop/";
    private static final String CSS_PATH = "/org/example/javaoop/";
    // Navigate to recommend page for regular users
    public static void navigateToRecommendGUI(Stage stage, String username) {
        loadScene(stage, "recommend.fxml", "articleStyle.css");
    }
    public static void navigateToSportGUI(Stage stage, String username) {
        loadScene(stage, "sport.fxml", "technology.css");
    }
    public static void navigateToHealthGUI(Stage stage, String username) {
        loadScene(stage, "health.fxml", "technology.css");
    }
    public static void navigateToScienceGUI(Stage stage, String username) {
        loadScene(stage, "science.fxml", "technology.css");
    }
    public static void navigateToAIGUI(Stage stage, String username) {
        loadScene(stage, "AI.fxml", "technology.css");
    }
    public static void navigateToTechnologyGUI(Stage stage, String username) {
        loadScene(stage, "technology.fxml", "technology.css");
    }

    public static void navigateToUserProfileGUI(Stage stage, String username) {
        loadScene(stage, "userProfile.fxml", "userProfile.css");
    }
    public static void navigateToSignUpGUI(Stage stage) {
        loadScene(stage, "signUp.fxml", "SignUp.css");
    }

    // Navigate to admin dashboard
    public static void navigateToAdmin(Stage stage) {
        loadScene(stage, "adminInterface.fxml", "adminUI.css");
    }
    public static void navigateToViewArticleGUI(Stage stage) {
        loadScene(stage, "adminArticleView.fxml", "adminArticleView.css");
    }
    public static void navigateToViewUsersGUI(Stage stage) {
        loadScene(stage, "adminUserView.fxml", "adminUserView.css");
    }

    // Navigate back to login page (for logout)
    public static void navigateToLogin(Stage stage) {
        loadScene(stage, "signIn.fxml", "SignIn.css");
    }



    // Core method to load FXML scenes with CSS
    private static void loadScene(Stage stage, String fxmlFile, String cssFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Navigator.class.getResource(FXML_PATH + fxmlFile)
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Add CSS to the scene
            String cssPath = Navigator.class.getResource(CSS_PATH + cssFile).toExternalForm();
            scene.getStylesheets().add(cssPath);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load page: " + fxmlFile);
        }
    }

    // Method to load multiple CSS files if needed
    private static void loadScene(Stage stage, String fxmlFile, String... cssFiles) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Navigator.class.getResource(FXML_PATH + fxmlFile)
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Add multiple CSS files to the scene
            for (String cssFile : cssFiles) {
                String cssPath = Navigator.class.getResource(CSS_PATH + cssFile).toExternalForm();
                scene.getStylesheets().add(cssPath);
            }

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation Error", "Could not load page: " + fxmlFile);
        }
    }

    private static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
