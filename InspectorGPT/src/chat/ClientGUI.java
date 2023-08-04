package chat;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ClientGUI extends Application {

    private String name = "";
    private TextArea chatArea;
    private TextField inputField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Client");

        BorderPane layout = new BorderPane();

        chatArea = new TextArea();
        chatArea.setEditable(false);
        layout.setCenter(chatArea);

        inputField = new TextField();
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        HBox inputBox = new HBox(inputField, sendButton);
        layout.setBottom(inputBox);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

       

        primaryStage.show();
        
        connectToServer();
    }

    private void connectToServer() {
        try (Socket socket = new Socket("localhost", ChatServer.SERVER_TEST_PORT)) {
          
        	System.out.println("Connected to the chat server @ " + ChatServer.SERVER_TEST_PORT);

        	// get connected users
            BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String usernames = read.readLine();
            

            // print connected users
            //System.out.println("\rConnected users: " +  usernames);
            appendToChatArea("\rConnected users: " +  usernames);

            // make connected users list
            List<String> userList = getUserList(usernames);

            // choose username
            setName();
            while (userList.contains(this.name) || this.name.equals("")) {
                //System.out.println("Username is taken. Please choose a different username: ");
            	appendToChatArea("Username is taken. Please choose a different username: ");
                setName();
             }
            
            // ovo sve iznad radimo ovde u klijentu jer this.name treba da se prosledi u tredove
            

            // Dispatch threads
            // getWt(socket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private List<String> getUserList(String usernames) {
        List<String> userList = new ArrayList<>();
        if (!usernames.equals("[null]")) {

            // Remove brackets
             usernames = usernames.substring(1, usernames.length() - 1);

            // Split by ", " to get a list of usernames
            userList = new ArrayList<>(Arrays.asList(usernames.split(", ")));

        } else {
            userList = new ArrayList<>();
        }
        return userList;
    }
    
    private void setName() throws IOException {
        //System.out.print("Enter your username: ");
        appendToChatArea("Enter your username: ");
        this.name = inputField.getText();
        //Scanner sc = new Scanner(System.in);
        //this.name = sc.nextLine();
        // sc.close();
        // We cannot close sc, since we will use it later
    }
/*
    private void getWt(Socket socket) {
        Thread rt = new ClientReadThread(name, socket, this);
        Thread wt = new ClientWriteThread(name, socket, inputField);
        rt.start();
        wt.start();
    }
*/
    
    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
        	
        	// send to server
        	
        	
        	// write to chat
        	appendToChatArea(message + "\n");

        	
            inputField.clear();
        }
    }

	public void appendToChatArea(String message) {
		chatArea.appendText(message);
	}
}

