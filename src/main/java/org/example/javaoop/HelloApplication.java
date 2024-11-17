package org.example.javaoop;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private String currentUsername;

    // Add setter and getter for username
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("signUp.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(getClass().getResource("signUp.css").toExternalForm());


        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
    public void recommended(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("recommend.fxml"));
        Parent root = fxmlLoader.load();
        RecommendController controller = fxmlLoader.getController();
        if (controller != null) {
            controller.setCurrentUsername(currentUsername);
        }

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setUserData(this);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleStyle.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void technology(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("technology.fxml"));
        Parent root = fxmlLoader.load();
        Object controller = fxmlLoader.getController();
        if (controller instanceof categoryController) {
            ((categoryController) controller).setCurrentUsername(currentUsername);
        }
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void health(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("health.fxml"));
        Parent root = fxmlLoader.load();
        Object controller = fxmlLoader.getController();
        if (controller instanceof categoryController) {
            ((categoryController) controller).setCurrentUsername(currentUsername);
        }
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void sport(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("sport.fxml"));
        Parent root = fxmlLoader.load();
        Object controller = fxmlLoader.getController();
        if (controller instanceof categoryController) {
            ((categoryController) controller).setCurrentUsername(currentUsername);
        }
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void AI(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AI.fxml"));
        Parent root = fxmlLoader.load();
        Object controller = fxmlLoader.getController();
        if (controller instanceof categoryController) {
            ((categoryController) controller).setCurrentUsername(currentUsername);
        }
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void science(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("science.fxml"));
        Parent root = fxmlLoader.load();
        // Get the controller and set username
        Object controller = fxmlLoader.getController();
        if (controller instanceof categoryController) {
            ((categoryController) controller).setCurrentUsername(currentUsername);
        }
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("technology.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    public void ArticleView(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("articleContainer.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("articleContainer.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }




    public static void main(String[] args) {
        launch();
    }
}