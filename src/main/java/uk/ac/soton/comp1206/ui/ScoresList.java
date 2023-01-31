package uk.ac.soton.comp1206.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.scene.ScoresScene;

import java.util.Timer;
import java.util.TimerTask;

public class ScoresList extends VBox {
    protected ListProperty<Pair<String, Integer>> scores = new SimpleListProperty<>();
    protected VBox userScores;
    private static final Logger logger = LogManager.getLogger(ScoresList.class);
    private Timer timer;

    /**
     * Creates a score list
     */
    public ScoresList() {
        logger.info("Creating scores list");

        setPrefWidth(200);
        setPadding(new Insets(50, 75, 0, 75));

        userScores = new VBox();
        userScores.setSpacing(30);
        userScores.setAlignment(Pos.TOP_LEFT);
        getChildren().add(userScores);
    }

    /**
     * animates the scores
     */
    public void reveal() {
        logger.info("revealing scores" + scores.get(0).toString());

        timer = new Timer();
        var timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!scores.isEmpty()) {
                    Platform.runLater(() -> displayScore());
                    reveal();
                } else {
                    timer.cancel();
                }
            }
        };
        //display a score every 1/4 second
        timer.schedule(timerTask, 250);
    }

    /**
     * return the UI scores
     * @return
     */
    public ListProperty<Pair<String, Integer>> getScores() { return scores; }

    /**
     * formats the score
     */
    public void displayScore() {
        logger.info("Revealing a score");

        var text = new Text(scores.get(0).toString());
        text.getStyleClass().add("channelItem");
        userScores.getChildren().add(text);
        scores.remove(0);
    }

    /**
     * sets the title text
     */
    public void setTopText(String titleText) {
        var text = new Text(titleText);
        text.getStyleClass().add("score");
        getChildren().add(text);
    }
}
