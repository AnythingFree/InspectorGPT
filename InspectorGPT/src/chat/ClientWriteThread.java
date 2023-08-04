package chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javafx.scene.control.TextField;


final class ClientWriteThread extends Thread {
    private final String username;
    private PrintWriter toServer;
    
    private TextField inputField; // Reference to the input field in the GUI



    ClientWriteThread(String username, Socket socket, TextField inputField) {
        this.username = username;
        this.inputField = inputField;
        
        try {
            this.toServer = new PrintWriter(socket.getOutputStream(), true);
            this.toServer.flush();
        } catch (IOException ex) {
            System.out.println("Error getting output stream from user : " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    @Override
    public void run() {
        // First send username, server expects that info
        this.toServer.println(this.username);

        // Then send input to server line by line, until `bye`
        try (Scanner sc = new Scanner(System.in)) {
            String text;
            
            do {
            	
                System.out.printf("\rw[%s]: ", this.username);
                
                text = inputField.getText(); // Get text from the GUI input field

                if (!text.isEmpty()) {
                    this.toServer.println(text);
                }
                    
                // Clear the input field
                inputField.clear();
                
            } while (!text.equals("bye"));
        }
    }
}
