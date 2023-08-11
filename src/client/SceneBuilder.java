package client;

import java.util.List;
import java.util.Optional;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SceneBuilder {

    private ClientGUI clientGUI;

    public SceneBuilder(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    public Scene getMainScene() {
        BorderPane layout = new BorderPane();

        // Create an HBox for the buttons
        HBox buttonBox = new HBox(10); // 10 is the spacing between buttons

        // Create buttons for the options
        Button option1Button = new Button("Option 1");
        option1Button.setOnAction(e -> this.clientGUI.handleOption1());

        Button option2Button = new Button("Option 2");
        option2Button.setOnAction(e -> this.clientGUI.handleOption2());

        Button option3Button = new Button("Option 3");
        option3Button.setOnAction(e -> this.clientGUI.handleOption3());

        // Add buttons to the buttonBox
        buttonBox.getChildren().addAll(option1Button, option2Button, option3Button);
        layout.setTop(buttonBox);

        Scene scene = new Scene(layout, 400, 300);
        return scene;
    }

    public Scene getChatScene(TextArea chatArea, TextField inputField) {
        BorderPane layout = new BorderPane();

        // create the chatArea
        chatArea = new TextArea();
        chatArea.setEditable(false);
        layout.setCenter(chatArea);

        // create inputField, sendButton
        inputField = new TextField();
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> this.clientGUI.sendMessage());
        HBox inputBox = new HBox(inputField, sendButton);
        // backButton
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> this.clientGUI.goBackToMainWindow());
        // Create a VBox to hold both the back button and the input box
        HBox bottomContainer = new HBox(inputBox, backButton);
        bottomContainer.setSpacing(10); // Set spacing between components

        // put them on bottom
        layout.setBottom(bottomContainer);

        Scene scene = new Scene(layout, 400, 300);
        return scene;
    }

    public Scene getOption2Scene(List<String> userList) {
        BorderPane userListLayout = new BorderPane();

        // Create a ListView to display the user list
        ListView<String> userListView = new ListView<>();
        userListView.getItems().addAll(userList);

        // Create a button to send a request
        Button sendRequestButton = new Button("Send Request");
        sendRequestButton.setOnAction(e -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                this.clientGUI.sendRequest(selectedUser);

                // Disable the button and show a waiting message
                sendRequestButton.setDisable(true);
                userListView.setDisable(true);

                // Display a waiting message
                Label waitingLabel = new Label("Waiting for " + selectedUser + " to respond...");
                userListLayout.setCenter(waitingLabel);
            }
        });

        userListLayout.setCenter(userListView);
        // backButton
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> this.clientGUI.goBackToMainWindow());
        // Create a VBox to hold both the back button and the input box
        HBox bottomContainer = new HBox(sendRequestButton, backButton);
        bottomContainer.setSpacing(10); // Set spacing between components

        // put them on bottom
        userListLayout.setBottom(bottomContainer);

        Scene scene = new Scene(userListLayout, 300, 200);
        return scene;

    }

    public boolean showGameRequestDialog(String usernameOpponent, String name) {
        // Create and configure the dialog
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Game Request: " + name);
        alert.setHeaderText("Incoming Game Request");
        alert.setContentText("You have an incoming game request from " + usernameOpponent + ". Do you want to accept?");

        // Add buttons to the dialog
        ButtonType acceptButton = new ButtonType("Accept");
        ButtonType declineButton = new ButtonType("Decline");
        alert.getButtonTypes().setAll(acceptButton, declineButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == acceptButton)
            return true;
        else
            return false;
    }

}
