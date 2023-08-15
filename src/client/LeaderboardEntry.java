package client;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class LeaderboardEntry {

    private final SimpleStringProperty name;
    private final SimpleIntegerProperty score;

    public LeaderboardEntry(String name, int score) {
        this.name = new SimpleStringProperty(name);
        this.score = new SimpleIntegerProperty(score);
    }

    public SimpleStringProperty getName() {
        return name;
    }

    public SimpleIntegerProperty getScore() {
        return score;
    }

}
