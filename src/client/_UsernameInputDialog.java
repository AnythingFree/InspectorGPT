package client;

import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;

public class _UsernameInputDialog extends TextInputDialog {

    private String name;

    public String getUsername(List<String> userList) {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Username Input");
        usernameDialog.setHeaderText("Usernames in use: " + userList);
        usernameDialog.setContentText("Please enter your username:");

        final boolean[] validUsername = { false };
        while (!validUsername[0]) {
            usernameDialog.showAndWait().ifPresent(username -> {
                if (username.isEmpty()) {
                    usernameDialog.setHeaderText("Username cannot be empty.");
                } else if (userList.contains(username)) {
                    usernameDialog.setHeaderText("Username is taken. Please choose a different username.");
                } else {
                    this.name = username;
                    validUsername[0] = true;
                }
            });

            if (usernameDialog.getResult() == null) {
                Platform.exit();
                System.exit(0);
            }
        }

        return this.name;
    }
}

