package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import util._JsonUtil;

public class ThreadMessageListener extends Thread {
    private ClientGUI clientGUI;
    private BufferedReader reader;

    public ThreadMessageListener(_ClientSocket clientSocket, ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
        try {
            this.reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getSocket().getInputStream()));
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String message;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Process the incoming message (update UI, handle requests, etc.)
                message = reader.readLine();
                handleIncomingMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Error reading message from server: " + e.getMessage());
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
                case "system":
                    String data = resultMap.get("data").toString();

                    switch (data) {
                        case "inputFiledOFF":
                            Platform.runLater(() -> {
                                clientGUI.disableInputField();
                            });
                            break;

                        case "inputFiledON":
                            Platform.runLater(() -> {
                                clientGUI.enableInputField();
                            });
                            break;

                        case "TimerButton":
                            String state = resultMap.get("state").toString();
                            if (state.equals("ON"))
                                Platform.runLater(() -> {
                                    clientGUI.enableTimerButton();
                                });
                            else
                                Platform.runLater(() -> {
                                    clientGUI.disableTimerButton();
                                });
                            break;

                        case "notification":
                            Platform.runLater(() -> {
                                clientGUI.showNotification(resultMap.get("message"));
                            });
                            break;
                        case "TimeLabel":
                            Platform.runLater(() -> {
                                clientGUI.updateTimeLabels(resultMap.get("player1Time"), resultMap.get("player2Time"));
                            });
                            break;
                        case "refreshTable":
                            String result = resultMap.get("result").toString();
                            Platform.runLater(() -> {
                                clientGUI.refreshTable(result);
                            });
                            break;

                        case "gameFinished":
                            String timeLeft = resultMap.get("timeLeft").toString();
                            Platform.runLater(() -> {
                                clientGUI.disableInputField();
                                clientGUI.disableTimerButton();
                                clientGUI.stopClock(timeLeft);
                            });
                            break;
                    }
                    break;
                // usernames treba isto biti system message
                case "usernames":
                    System.out.println(resultMap.get("data"));
                    clientGUI.setUserList(_getListFromString(resultMap.get("data")));
                    break;

                case "request":
                    // Handle request message
                    String usernameOpponent = resultMap.get("opponent");
                    Platform.runLater(() -> {
                        clientGUI.showGameRequestDialog(usernameOpponent);
                    });
                    break;

                case "response":
                    // openChatRoom();
                    String res = resultMap.get("data").toString();
                    if (res.equals("no")) {
                        Platform.runLater(() -> {
                            clientGUI.goBackToOption2();
                        });
                    } else {
                        Platform.runLater(() -> {
                            clientGUI.playGame();
                        });
                    }
                    break;

                case "chat":
                    // Handle chat message
                    Platform.runLater(() -> {
                        clientGUI.appendToChatArea(resultMap.get("data"));
                    });
                    break;

                case "scene":
                    // Handle scene message
                    String scene = resultMap.get("scene").toString();

                    if (scene.equals("option2"))
                        Platform.runLater(() -> {
                            clientGUI.getScene2();
                        });
                    else if (scene.equals("option1")) {
                        Platform.runLater(() -> {
                            clientGUI.getScene1();
                        });
                    } else if (scene.equals("option3")) {
                        Platform.runLater(() -> {
                            clientGUI.getScene3();
                        });
                    }

                    break;
                case "channels":
                    String channelsString = resultMap.get("data");
                    List<String> channels = _getListFromString(channelsString);

                    Platform.runLater(() -> {
                        clientGUI.getChannels(channels);
                    });

                    break;

                // Add more cases for other message types as needed

                default:
                    // Handle unknown or unsupported message types
                    System.out.println("Received unknown message type: " + messageType);
            }
        } catch (Exception e) {
            System.out.println("Error in handleIncomingMessage [client]");
            System.out.println(e.getMessage());

        }
    }

    private List<String> _getListFromString(String string) {
        List<String> userList = new ArrayList<>();
        if (!string.equals("[null]")) {

            // Remove brackets
            string = string.substring(1, string.length() - 1);

            // Split by ", " to get a list of usernames
            userList = new ArrayList<>(Arrays.asList(string.split(", ")));

        }
        return userList;
    }
}
