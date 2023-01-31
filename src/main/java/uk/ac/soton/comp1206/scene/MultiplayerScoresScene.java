package uk.ac.soton.comp1206.scene;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import uk.ac.soton.comp1206.ui.Leaderboard;
import uk.ac.soton.comp1206.ui.ScoresList;

import java.util.Timer;
import java.util.TimerTask;

public class MultiplayerScoresScene extends ScoresScene{
    private static final Logger logger = LogManager.getLogger(MultiplayerScoresScene.class);
    private ObservableList<Pair<String, Integer>> observableGameScores = FXCollections.observableArrayList();
    private ListProperty<Pair<String, Integer>> gameScores= new SimpleListProperty<>(observableGameScores);
    private Leaderboard leaderboard;

    public MultiplayerScoresScene(GameWindow gameWindow, Game game, Leaderboard leaderboard) {
        super(gameWindow, game);
        this.leaderboard = leaderboard;
    }

    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var menuPane = new StackPane();
        menuPane.setMaxWidth(gameWindow.getWidth());
        menuPane.setMaxHeight(gameWindow.getHeight());
        menuPane.getStyleClass().add("menu-background");
        root.getChildren().add(menuPane);

        mainPane = new BorderPane();
        menuPane.getChildren().add(mainPane);

        //online + local scores title
        var hbox = new HBox();
        var localText = new Text("GAME SCORES");
        var onlineText = new Text("ONLINE SCORES");
        hbox.setSpacing(400);
        localText.getStyleClass().add("scoreTitle");
        onlineText.getStyleClass().add("scoreTitle");
        hbox.getChildren().addAll(localText, onlineText);
        mainPane.setTop(hbox);

        communicator.addListener(this::receiveCommunication);
        loadOnlineScores();

        displayGameScores();
    }

    public void displayGameScores() {
        //sets UI component on the left
        leaderboard.setAlignment(Pos.CENTER_LEFT);
        mainPane.setLeft(leaderboard);

        var timer = new Timer();
        var onlineTimerTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> displayOnlineScores());
                timer.cancel();
            }
        };
        timer.schedule(onlineTimerTask, 3000);
    }
}
