package org.example.javaoop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class HelloApplication extends Application {
    // Using AtomicReference for thread-safe username access
    private final AtomicReference<String> currentUsername = new AtomicReference<>();

    public void setCurrentUsername(String username) {
        currentUsername.set(username);
    }

    public String getCurrentUsername() {
        return currentUsername.get();
    }

    @Override
    public void start(Stage stage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("signUp.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            scene.getStylesheets().add(getClass().getResource("signUp.css").toExternalForm());

            // Set the HelloApplication instance in the stage's user data
            stage.setUserData(this);
            stage.setTitle("Hello!");
            stage.setScene(scene);
            stage.show();

            // Add stage close handler for cleanup
            stage.setOnCloseRequest(event -> {
                cleanup();
                Platform.exit();
            });
        } catch (IOException e) {
            System.err.println("Failed to start application: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void stop() {
        cleanup();
    }

    private void cleanup() {
        // This will be called when the application is shutting down
        // Clean up any resources, shut down thread pools, etc.
        try {
            // Get all open windows/stages
            Platform.runLater(() -> {
                for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                    if (window instanceof Stage) {
                        // Get the controller and call cleanup if it exists
                        Scene scene = ((Stage) window).getScene();
                        if (scene != null && scene.getRoot() != null) {
                            Object controller = scene.getUserData();
                            if (controller instanceof AutoCloseable) {
                                try {
                                    ((AutoCloseable) controller).close();
                                } catch (Exception e) {
                                    System.err.println("Error during cleanup: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error during application cleanup: " + e.getMessage());
        }
    }

    // Helper method for navigation that ensures thread safety
    public void recommended(ActionEvent event) throws IOException {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("recommend.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
                scene.getStylesheets().add(getClass().getResource("articleStyle.css").toExternalForm());

                // Get the controller and set the username
                RecommendController controller = fxmlLoader.getController();
                controller.setCurrentUsername(getCurrentUsername());

                Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                stage.setUserData(this);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                System.err.println("Navigation error: " + e.getMessage());
            }
        });
    }
}