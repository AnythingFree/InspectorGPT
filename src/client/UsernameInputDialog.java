package client;

import javafx.application.Application;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class UsernameInputDialog extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Username Input Dialog");

        // Create a TextInputDialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Username Input");
        dialog.setHeaderText(null); // No header text
        dialog.setContentText("Please enter your username:");

        // Show the dialog and wait for the response
        dialog.showAndWait().ifPresent(username -> {
            // You can use the entered username here
            System.out.println("Entered username: " + username);
        });

        // Close the application
        primaryStage.close();
    }
}

