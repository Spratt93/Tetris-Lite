package uk.ac.soton.comp1206.scene;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import uk.ac.soton.comp1206.ui.ScoresList;

import java.io.*;
import java.util.*;

public class ScoresScene extends BaseScene {
    private static final Logger logger = LogManager.getLogger(ScoresScene.class);

    //contains local scores
    private ObservableList<Pair<String, Integer>> observableLocalScores = FXCollections.observableArrayList();
    private ListProperty<Pair<String, Integer>> localScores= new SimpleListProperty<>(observableLocalScores);

    //contains online scores
    protected ObservableList<Pair<String, Integer>> observableRemoteScores = FXCollections.observableArrayList();
    protected ListProperty<Pair<String, Integer>> remoteScores = new SimpleListProperty<>(observableRemoteScores);

    protected Communicator communicator = new Communicator("ws://discord.ecs.soton.ac.uk:9700");

    //game end state
    protected Game game;

    //file that's read from and written to
    private File scoresFile = new File("./scores.txt");

    protected BorderPane mainPane;

    public ScoresScene(GameWindow gameWindow, Game game) {
        super(gameWindow);
        this.game = game;
    }

    @Override
    public void initialise() {
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
        var localText = new Text("LOCAL SCORES");
        var onlineText = new Text("ONLINE SCORES");
        hbox.setSpacing(400);
        localText.getStyleClass().add("scoreTitle");
        onlineText.getStyleClass().add("scoreTitle");
        hbox.getChildren().addAll(localText, onlineText);
        mainPane.setTop(hbox);


        communicator.addListener(this::receiveCommunication);
        loadOnlineScores();

        displayLocalScores();
    }

    /**
     * load the scores from a file to the scores list and orders the list
     */
    public void loadScores() throws IOException {
        if (scoresFile.exists() && scoresFile.canRead()) {
            logger.info("Loading scores");

            var input = new BufferedReader(new FileReader(scoresFile));
            var line = input.readLine();

            //adds score from file to list
            while (line != null) {
                var split = line.split(":");
                Pair<String, Integer> score = new Pair<>(split[0], Integer.valueOf(split[1]));
                localScores.add(score);
                logger.info("Loading score " + score.toString());
                line = input.readLine();
            }
            input.close();
            localScores.sort(Comparator.comparingInt(o -> o.getValue()));
            Collections.reverse(localScores);
        }
    }

    /**
     * rewrite the scores to scores.txt
     * @throws IOException
     */
    public void writeScores() throws IOException {
        if (scoresFile.exists()) {
            logger.info("Writing scores");

            scoresFile.delete();
            var output = new PrintWriter(new FileWriter(scoresFile));
            for (Pair<String, Integer> score : localScores) {
                output.println(score.getKey() + ":" + score.getValue());
                logger.info("Writing score " + score.toString());
            }
            output.close();
        } else {
            var output = new PrintWriter(new FileWriter(scoresFile));
            for (int i=0; i < 10; i++) {
                output.println("test:" + i);
            }
            output.close();
        }
    }

    /**
     * loads online scores
     */
    public void loadOnlineScores() {
        //network protocol message to receive online high score list
        communicator.send("HISCORES");
    }

    /**
     * handles messages from the server
     * @param message
     */
    public void receiveCommunication(String message) {
        if (message.contains("HISCORES")) {
            //remove the formatting--forms arr list with name, score, ...
            String message1 = message.replaceFirst("HISCORES ", "");
            var split = message1.split("[\n:]+");
            List<String> arrList = new ArrayList<>();
            Collections.addAll(arrList, split);

            while (!arrList.isEmpty()) {
                //takes name and score string and adds them to local scores
                logger.info("Loading online score: " + arrList.get(0) + "," + arrList.get(1));
                Pair<String, Integer> score = new Pair<>(arrList.get(0), Integer.valueOf(arrList.get(1)));
                remoteScores.add(score);
                arrList.remove(0);
                arrList.remove(0);
            }
        }
    }

    /**
     * sends score to server
     * @param name
     */
    public void writeOnlineScore(String name) {
        communicator.send("HISCORE <" + name + ">:<" + game.getScoreProperty() + ">");
    }

    /**
     * displays the online scores in the UI
     */
    public void displayOnlineScores() {
        //UI component used to reveal the scores
        var onlineScoresList = new ScoresList();
        onlineScoresList.getScores().bind(remoteScores);
        onlineScoresList.setAlignment(Pos.CENTER_RIGHT);
        mainPane.setRight(onlineScoresList);

        //checking if score is within top 10
        int onlineIndex = 0;
        for (Pair<String, Integer> score : remoteScores) {
            if (game.getScoreProperty() > score.getValue()) { onlineIndex++; }
        }

        if (onlineIndex != 0) {
            logger.info("Score within online top 10");
            var onlineTextfield = new TextField("Enter your online name:");
            onlineTextfield.getStyleClass().add("scorer");
            onlineTextfield.setMinSize(200, 50);
            onlineScoresList.getChildren().add(onlineTextfield);

            int finalIndex = 10 - onlineIndex;
            onlineTextfield.setOnKeyPressed((e) -> {
                if (e.getCode() == KeyCode.ENTER) {
                    //write online scores
                    writeOnlineScore(onlineTextfield.getText());

                    onlineScoresList.getChildren().remove(onlineTextfield);
                    remoteScores.remove(9);
                    Pair<String, Integer> UserScore = new Pair<>(onlineTextfield.getText(), game.getScoreProperty());
                    remoteScores.add(finalIndex, UserScore);

                    onlineScoresList.reveal();

                    //return to main menu
                    var menuTimer = new Timer();
                    var timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> gameWindow.startMenu());
                            menuTimer.cancel();
                        }
                    };
                    menuTimer.schedule(timerTask, 8000);
                }
            });
        } else {
            onlineScoresList.reveal();

            //return to main menu
            var menuTimer = new Timer();
            var timerTask = new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> gameWindow.startMenu());
                    menuTimer.cancel();
                }
            };
            menuTimer.schedule(timerTask, 8000);
        }
    }

    /**
     * displays local scores in the scene
     */
    public void displayLocalScores() {
        //sets UI component on the left
        var localScoresList = new ScoresList();
        localScoresList.getScores().bind((localScores));
        localScoresList.setAlignment(Pos.CENTER_LEFT);
        mainPane.setLeft(localScoresList);

        try {
            loadScores();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int index = 0;
        for (Pair<String, Integer> score : localScores) {
            if (game.getScoreProperty() > score.getValue()) {
                index++;
            }
        }

        if (index != 0) {
            logger.info("Score within local top 10!");

            var textfield = new TextField("Enter your local name:");
            textfield.getStyleClass().add("scorer");
            textfield.setMinSize(200, 50);
            localScoresList.getChildren().add(textfield);
            localScoresList.setAlignment(Pos.CENTER_LEFT);

            int finalIndex = 10 - index;
            textfield.setOnKeyPressed((e) -> {
                if (e.getCode() == KeyCode.ENTER) {
                    localScoresList.getChildren().remove(textfield);
                    localScores.remove(9);
                    Pair<String, Integer> UserScore = new Pair<>(textfield.getText(), game.getScoreProperty());
                    localScores.add(finalIndex, UserScore);

                    localScoresList.reveal();

                    try {
                        writeScores();

                        //display online scores
                        var timer = new Timer();
                        var onlineTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> displayOnlineScores());
                                timer.cancel();
                            }
                        };
                        timer.schedule(onlineTimerTask, 8000);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
        } else {
            localScoresList.reveal();
            try {
                writeScores();

                //display online scores
                var timer = new Timer();
                var onlineTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> displayOnlineScores());
                        timer.cancel();
                    }
                };
                timer.schedule(onlineTimerTask, 8000);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
