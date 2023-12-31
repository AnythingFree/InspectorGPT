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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SceneBuilder {

    private ClientGUI clientGUI;

    public SceneBuilder(ClientGUI clientGUI, Stage primaryStage) {
        this.clientGUI = clientGUI;
    }

    public Scene getMainScene() {
        BorderPane layout = new BorderPane();

        // Create an HBox for the buttons
        HBox buttonBox = new HBox(10); // 10 is the spacing between buttons

        // Create buttons for the options
        Button option1Button = new Button("General Channel");
        option1Button.setOnAction(e -> this.clientGUI.handleOption1());

        Button option2Button = new Button("Game Zone");
        option2Button.setOnAction(e -> this.clientGUI.handleOption2());

        Button option3Button = new Button("Watch & Chat");
        option3Button.setOnAction(e -> this.clientGUI.handleOption3());

        // Add buttons to the buttonBox
        buttonBox.getChildren().addAll(option1Button, option2Button, option3Button);
        layout.setTop(buttonBox);

        this.clientGUI.leaderboardTable = getLeaderboardTable();
        layout.setCenter(this.clientGUI.leaderboardTable);

        Scene scene = new Scene(layout, 400, 300);
        return scene;
    }

    private TableView<_LeaderboardEntry> getLeaderboardTable() {
        // Create a TableView and define columns
        TableView<_LeaderboardEntry> leaderboardTable = new TableView<>();
        TableColumn<_LeaderboardEntry, String> nameColumn = new TableColumn<>("Name");
        TableColumn<_LeaderboardEntry, Integer> scoreColumn = new TableColumn<>("Score");

        // Bind columns to data model properties
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().getName());
        scoreColumn.setCellValueFactory(cellData -> cellData.getValue().getScore().asObject());

        // Add columns to the table
        leaderboardTable.getColumns().addAll(nameColumn, scoreColumn);

        return leaderboardTable;
    }

    public Scene getChatScene() {
        BorderPane layout = new BorderPane();

        // create the chatArea
        this.clientGUI.chatArea = new TextArea();
        this.clientGUI.chatArea.setEditable(false);
        layout.setCenter(this.clientGUI.chatArea);

        // create inputField, sendButton
        this.clientGUI.inputField = new TextField();
        Button sendButton = new Button("Send");
        sendButton.setId("SendButton");
        sendButton.setOnAction(e -> this.clientGUI.sendMessage());

        // Set an event listener for the Enter key press event on the TextField
        this.clientGUI.inputField.setOnAction(event -> {
            sendButton.fire();
        });

        // pack it all up
        HBox inputBox = new HBox(this.clientGUI.inputField, sendButton);
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

    // uradi za opciju 3!!
    public Scene getOption3Scene(List<String> channelList) {
        BorderPane channelListLayout = new BorderPane();

        // Create a ListView to display the channel list
        ListView<String> channelListView = new ListView<>();
        channelListView.getItems().addAll(channelList);
        channelListLayout.setCenter(channelListView);

        // backButton
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> this.clientGUI.goBackToMainWindow());

        // watch Button
        Button watchButton = new Button("Watch");
        watchButton.setOnAction(e -> {
            String selectedChannel = channelListView.getSelectionModel().getSelectedItem();
            if (selectedChannel != null && !selectedChannel.equals("")) {
                this.clientGUI.watchChatScene(selectedChannel);
            }
        });

        // Create a VBox to hold both the back button and the input box
        HBox bottomContainer = new HBox(watchButton, backButton);
        bottomContainer.setSpacing(10); // Set spacing between components

        // put them on bottom
        channelListLayout.setBottom(bottomContainer);
        // kada se klikne na kanal treba da se otvori chat da klijent moze da gleda

        Scene scene = new Scene(channelListLayout, 300, 200);
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

    public Scene getGameScene() {
        Scene scene = getChatScene();

        BorderPane root = (BorderPane) scene.getRoot();
        Button timerButton = new Button("Timer");
        timerButton.setId("TimerButton");

        timerButton.setOnAction(e -> {
            this.clientGUI.switchPlayer();
        });

        this.clientGUI.player1Time = new Label(" Player1: 10:00");
        this.clientGUI.player2Time = new Label(" Player2: 10:00");
        HBox hbox = new HBox(timerButton, this.clientGUI.player1Time, this.clientGUI.player2Time);
        hbox.setSpacing(10);
        root.setTop(hbox);

        scene.setRoot(root);
        return scene;
    }

    public BorderPane getViewersChatLayout() {
        BorderPane layout = new BorderPane();

        // create the chatArea
        this.clientGUI.chatArea = new TextArea();
        this.clientGUI.chatArea.setEditable(false);
        layout.setRight(this.clientGUI.chatArea);
        this.clientGUI.chatArea.setText("2.\n");
        this.clientGUI.chatArea.setPrefWidth(400);

        // create the chatArea2
        this.clientGUI.chatArea2 = new TextArea();
        this.clientGUI.chatArea2.setEditable(false);
        layout.setLeft(this.clientGUI.chatArea2);
        this.clientGUI.chatArea2.setText("1.\n");
        this.clientGUI.chatArea2.setPrefWidth(400);

        // create inputField
        this.clientGUI.inputField = new TextField();

        // sendButton
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> this.clientGUI.sendMessage2());

        // Set an event listener for the Enter key press event on the TextField
        this.clientGUI.inputField.setOnAction(event -> {
            sendButton.fire();
        });

        // backButton
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            this.clientGUI.goBackToMainWindow();
        });
        // Create a VBox to hold both the back button and the input box
        HBox bottomContainer = new HBox(this.clientGUI.inputField, sendButton, backButton);
        bottomContainer.setSpacing(10); // Set spacing between components

        // put them on bottom
        layout.setBottom(bottomContainer);

        // Scene scene = new Scene(layout, 400, 300);
        return layout;
    }

    public Scene getWatchScene() {
        HBox root = new HBox();
        root.getChildren().addAll(getViewersChatLayout());

        Scene scene = new Scene(root, 800, 300);
        return scene;

    }

}
