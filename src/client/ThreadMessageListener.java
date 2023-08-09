package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;

public class ThreadMessageListener extends Thread {
    private _ClientSocket clientSocket;
    private ClientGUI clientGUI;

    public ThreadMessageListener(_ClientSocket clientSocket, ClientGUI clientGUI) {
        this.clientSocket = clientSocket;
        this.clientGUI = clientGUI;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getSocket().getInputStream()));
            String message;
            while (!Thread.currentThread().isInterrupted()) {
                // Process the incoming message (update UI, handle requests, etc.)
                message = reader.readLine();
                handleIncomingMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Message listener thread stopped");
        }
    }

    private void handleIncomingMessage(String message) {
        try {
            // Parse the incoming JSON message
            Map<String, String> resultMap = _JsonUtil.jsonToMap(message);

            // Extract the "type" field from the JSON message
            String messageType = resultMap.get("type").toString();

            // Handle the message based on its type using a switch-case statement
            switch (messageType) {
                case "usernames":
                    // get connected users
                    //Platform.runLater(() -> {
                        clientGUI.setUserList(_getUserList(resultMap.get("data").toString()));
                    //});
                    break;

                case "request":
                    // Handle request message
                    String usernameOpponent = resultMap.get("opponent").toString();
                    Platform.runLater(() -> {
                        clientGUI.showGameRequestDialog(usernameOpponent);
                    });
                    break;

                case "response":
                    // openChatRoom();
                    String res = resultMap.get("data").toString();
                    if (res.equals("no")){
                        Platform.runLater(() -> {
                            clientGUI.option2();
                        });
                    }else{
                         Platform.runLater(() -> {
                            clientGUI.playGame();
                        });
                    }
                    break;

                case "notification":
                    // Handle notification message
                    // handleNotificationMessage(jsonMessage);
                    break;

                case "chat":
                    // Handle chat message
                    String data = resultMap.get("data").toString();
                    Platform.runLater(() -> {
                        clientGUI.appendToChatArea(data);
                    });
                    break;

                // Add more cases for other message types as needed

                default:
                    // Handle unknown or unsupported message types
                    System.out.println("Received unknown message type: " + messageType);
            }
        } catch (Exception e) {
            System.out.println("Error in handleIncomingMessage");
            System.out.println(e.getMessage());
            
        }
    }

    private List<String> _getUserList(String usernames) {
		List<String> userList = new ArrayList<>();
		if (!usernames.equals("[null]")) {

			// Remove brackets
			usernames = usernames.substring(1, usernames.length() - 1);

			// Split by ", " to get a list of usernames
			userList = new ArrayList<>(Arrays.asList(usernames.split(", ")));

		} 
		return userList;
	}
}
