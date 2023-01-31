package uk.ac.soton.comp1206.ui;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Leaderboard extends ScoresList {
    private ListProperty<Pair<String, String>> lives = new SimpleListProperty<>();
    private static final Logger logger = LogManager.getLogger(Leaderboard.class);

    public Leaderboard() {
        super();
    }

    /**
     * returns UI lives
     */
    public ListProperty<Pair<String, String>> getLives() { return lives; }

    /**
     * reveals the leaderboard
     */
    public void displayLeaderboard() {
        logger.info("revealing leaderboard");


        for (Pair<String, Integer> score : scores) {
            var text = new Text(score.getKey() + ":" + score.getValue());
            text.getStyleClass().add("leaderboard");

            for (Pair<String, String> life : lives) {
                if (score.getKey().contains(life.getKey())) {
                    text.getStyleClass().add("deadscore");
                }
            }

            userScores.getChildren().add(text);
        }
    }

    /**
     * clears leaderboards children
     */
    public void clear() {
        userScores.getChildren().clear();
    }

}
