package com.example.csit228capstone.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.example.csit228capstone.util.SupabaseConnectionManager;

public class LigtasApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SupabaseConnectionManager.getInstance();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/login/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1440, 900);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

        primaryStage.setTitle("LIGTAS-Brgy — Barangay Command Center");
        primaryStage.setScene(scene);
        primaryStage.setFullScreenExitHint("");


        try {
            Image icon = new Image(
                    getClass().getResourceAsStream("/images/ligtas-brgy-logo.png")
            );
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("App icon not found: " + e.getMessage());
        }

         primaryStage.setMinWidth(1200);
         primaryStage.setMinHeight(800);
          primaryStage.setFullScreen(true);
          primaryStage.show();
    }

    @Override
    public void stop() {
        SupabaseConnectionManager.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}