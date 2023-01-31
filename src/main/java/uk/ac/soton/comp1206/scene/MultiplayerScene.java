package uk.ac.soton.comp1206.scene;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.GameBoard;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.game.GamePiece;
import uk.ac.soton.comp1206.game.Multimedia;
import uk.ac.soton.comp1206.game.MultiplayerGame;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import uk.ac.soton.comp1206.ui.Leaderboard;

import java.util.*;

public class MultiplayerScene extends ChallengeScene {

    private static final Logger logger = LogManager.getLogger(MultiplayerScene.class);
    private PieceBoard currentPiece;
    private PieceBoard nextPiece;
    private StringProperty chat = new SimpleStringProperty("Press <T> to chat");
    private Communicator communicator;
    private VBox gameBoardVbox = new VBox(15);
    private Leaderboard leaderboard = new Leaderboard();

    private ObservableList<Pair<String, Integer>> observableList = FXCollections.observableArrayList();
    private ListProperty<Pair<String, Integer>> scores = new SimpleListProperty<>(observableList);

    private ObservableList<Pair<String, String>> observableLives = FXCollections.observableArrayList();
    private ListProperty<Pair<String, String>> livingStatus = new SimpleListProperty<>(observableLives);

    public MultiplayerScene(GameWindow gameWindow, Communicator comm) {
        super(gameWindow);
        communicator = comm;
    }

    /**
     * builds the scene
     * other than the chat and leaderboard
     * same as challenge scene
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        setupGame();

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var challengePane = new StackPane();
        challengePane.setMaxWidth(gameWindow.getWidth());
        challengePane.setMaxHeight(gameWindow.getHeight());
        challengePane.getStyleClass().add("menu-background");
        root.getChildren().add(challengePane);

        mainPane = new BorderPane();
        challengePane.getChildren().add(mainPane);

        board = new GameBoard(game.getGrid(),gameWindow.getWidth()/2,gameWindow.getWidth()/2);
        gameBoardVbox.getChildren().add(board);
        mainPane.setCenter(gameBoardVbox);

        //Handle block on gameboard grid being clicked
        board.setOnBlockClick(this::blockClicked);

        communicator.addListener(this::receiveCommunication);

        /**
         * hbox set to the top of the scene to hold game data
         */
        var hbox = new HBox();

        var scoreLabel = new Text("Score: ");
        var scoreText = new Text();
        scoreText.textProperty().bind(game.scoreProperty().asString());

        var levelLabel = new Text("Level: ");
        var levelText = new Text();
        levelText.textProperty().bind(game.levelProperty().asString());

        var livesLabel = new Text("Lives: ");
        var livesText = new Text();
        livesText.textProperty().bind(game.livesProperty().asString());

        scoreText.getStyleClass().add("score");
        levelText.getStyleClass().add("level");
        livesText.getStyleClass().add("lives");
        scoreLabel.getStyleClass().add("heading");
        levelLabel.getStyleClass().add("heading");
        livesLabel.getStyleClass().add("heading");
        hbox.getChildren().addAll(scoreLabel, scoreText, levelLabel, levelText, livesLabel, livesText);
        hbox.setSpacing(10);
        mainPane.setTop(hbox);

        /**
         * current piece displayed on the top right
         */
        var currentPieceLabel = new Text("Current Piece");
        currentPieceLabel.getStyleClass().add("pieceLabel");

        currentPiece = new PieceBoard(3, 3, gameWindow.getWidth()/5, gameWindow.getHeight()/5);
        currentPiece.setPadding(new Insets(30, 10, 30, 10));


        /**
         * next piece displayed bottom right
         */
        var nextPieceLabel = new Text("Next Piece");
        nextPieceLabel.getStyleClass().add("pieceLabel");

        nextPiece = new PieceBoard(3, 3, gameWindow.getWidth()/7, gameWindow.getHeight()/7);
        nextPiece.setPadding(new Insets(30, 5, 30, 5));


        /**
         * within this vbox want to have leaderboard
         * so that it stays in correct order will probs have to use binding
         */
        //vbox holding pieceboards
        var pieceBoards = new VBox();
        pieceBoards.getChildren().addAll(leaderboard, currentPieceLabel, currentPiece, nextPieceLabel, nextPiece);
        mainPane.setRight(pieceBoards);

        /**
         * CHAT
         * put gameboard in vbox
         * 1 line at a time
         * have a receive communication
         * probs use binding again -- simple string prop
         */
        var chatText = new Text();
        chatText.getStyleClass().add("users");
        chatText.textProperty().bind(chat);
        gameBoardVbox.getChildren().add(chatText);

        //when esc pressed call local keyPressed()
        gameWindow.setOnKeyPressed(this::keyPressed);

        //handle when next piece is in play
        game.setNextPieceListener(this::nextPiece);

        //when game board right clicked rotate the current piece
        board.setRightClickedListener(this::rightClick);

        //when current piece board left clicked it rotates
        currentPiece.setLeftClickedListener(this::leftClick);

        //when line is cleared call the fadeOut() animation in gameBoard
        game.setLineClearedListener(this::lineCleared);

        //when game loop finished print new progress bar
        game.setOnGameLoop(this::gameLoopStart);

        leaderboard.getScores().bind(scores);
        leaderboard.setPadding(new Insets(30));

        leaderboard.getLives().bind(livingStatus);

        leaderBoardTimer();
    }

    /**
     * initialises the scene
     */
    @Override
    public void initialise() {
        logger.info("Initialising Multiplayer");
        game.start();
        Multimedia.playBackgroundMusic("game.wav");

        currentPiece.setDisplayPiece(game.getCurrentPiece());
        //displays the reference circle
        currentPiece.displayCentreCircle();

        nextPiece.setDisplayPiece(game.getFollowingPiece());
    }

    /**
     * sets up the game
     * same as challenge scene but multiplayer game used instead
     */
    @Override
    public void setupGame() {
        logger.info("Starting a new multiplayer challenge");

        //start new multiplayer game
        game = new MultiplayerGame(5, 5, communicator);
    }

    /**
     * when block is clicked send that information to multiplayer game to handle
     * @param block
     */
    private void blockClicked(GameBlock block) { game.blockClicked(block); }

    /**
     * handles messages from server
     * listens for messages -- cos of game chat
     * @param message
     */
    public void receiveCommunication(String message) {
        if (message.contains("MSG")) {
            var message1 = message.replaceFirst("MSG ", "");
            Platform.runLater(() -> receiveChat(message1));
        }

        //updating leaderboard
        if (message.contains("SCORES")) {
            /**
             * gonna have to be PLATFORM RUN LATER + USE LEADERBOARD CLASS
             * have a display leaderboard method
             */
            Platform.runLater(() -> displayLeaderboard(message));
        }
    }

    /**
     * updates the UI to display most recent chat
     * @param mssg
     */
    public void receiveChat(String mssg) {
        chat.set(mssg);
    }

    public void getUserScores() {
        communicator.send("SCORES");
    }

    /**
     * displays the leaderboard
     */
    public void displayLeaderboard(String message) {
        logger.info("Updating leaderboard");
        var message1 = message.replaceFirst("SCORES ", "");
        var split = message1.split("[\n:]+");
        List<String> arrList = new ArrayList<>();
        Collections.addAll(arrList, split);

        //refreshing from last time
        scores.clear();
        livingStatus.clear();
        leaderboard.clear();
        /**
         * compare users based on score -- poss. Integer.valueOf(split[1])
         * remove prev vbox then replace with ordered
         * call this on a timer
         */
        while (!arrList.isEmpty()) {
            Pair<String, Integer> score = new Pair<>(arrList.get(0), Integer.valueOf(arrList.get(1)));
            Pair<String, String> alive = new Pair<>(arrList.get(0), arrList.get(2));

            scores.add(score);

            if (alive.getValue().contains("DEAD")) {
                livingStatus.add(alive);
            }

            arrList.remove(0);
            arrList.remove(0);
            arrList.remove(0);
        }
        //sorts the leaderboard in order of score
        scores.sort(Comparator.comparingInt(o -> o.getValue()));
        Collections.reverse(scores);

        /**
         * reveal leaderboard in UI
         */
        leaderboard.displayLeaderboard();

    }

    @Override
    public void keyPressed(KeyEvent e) {
        //esc to menu
        if (e.getCode() == KeyCode.ESCAPE) {
            logger.info("Returning to menu");
            game.stopGameTimer();
            Multimedia.closeBackgroundMusic();
            gameWindow.startMenu();
        }
        //rotate left
        if (e.getCode() == KeyCode.Q) {
            logger.info("Rotating current piece left");
            game.rotateCurrentPiece(3);
            nextPiece(game.getCurrentPiece());
            nextPiece(game.getFollowingPiece());
        }
        //rotate right
        if (e.getCode() == KeyCode.E) {
            logger.info("Rotating current piece right");
            game.rotateCurrentPiece(1);
            nextPiece(game.getCurrentPiece());
            nextPiece(game.getFollowingPiece());
        }
        //swap upcoming pieces
        if (e.getCode() == KeyCode.R) {
            logger.info("Swapping pieces");
            game.swapCurrentPiece();
            nextPiece(game.getCurrentPiece());
            nextPiece(game.getFollowingPiece());
        }
        //drop a piece
        if (e.getCode() == KeyCode.X) {
            blockClicked(board.getBlock(game.getCurrentX(), game.getCurrentY()));
        }
        //move current block up
        if (e.getCode() == KeyCode.W) {
            game.setCurrentY(-1);
            board.currentBlock(board.getBlock(game.getCurrentX(), game.getCurrentY()));
            logger.info("Current position is: (" + game.getCurrentX() + "," + game.getCurrentY() + ")");
        }
        //move current block down
        if (e.getCode() == KeyCode.S) {
            game.setCurrentY(1);
            board.currentBlock(board.getBlock(game.getCurrentX(), game.getCurrentY()));
            logger.info("Current position is: (" + game.getCurrentX() + "," + game.getCurrentY() + ")");
        }
        //move current block left
        if (e.getCode() == KeyCode.A) {
            game.setCurrentX(-1);
            board.currentBlock(board.getBlock(game.getCurrentX(), game.getCurrentY()));
            logger.info("Current position is: (" + game.getCurrentX() + "," + game.getCurrentY() + ")");
        }
        //move current block right
        if (e.getCode() == KeyCode.D) {
            game.setCurrentX(1);
            board.currentBlock(board.getBlock(game.getCurrentX(), game.getCurrentY()));
            logger.info("Current position is: (" + game.getCurrentX() + "," + game.getCurrentY() + ")");
        }
        //send chat message
        if (e.getCode() == KeyCode.T) {
            /**
             * open a textfield below
             * in vbox with gameboard
             * remove the textbox when done
             */
            var chatField = new TextField();
            chatField.getStyleClass().add("users");
            chatField.setMinSize(350, 40);
            gameBoardVbox.getChildren().add(chatField);

            chatField.setOnKeyPressed((event) -> {
                if (event.getCode() == KeyCode.ENTER) {
                    communicator.send("MSG " + chatField.getText());
                    gameBoardVbox.getChildren().remove(chatField);
                }
            });
        }
    }

    public void leaderBoardTimer() {
        var leaderBoardTimer = new Timer();
        var leaderBoardTask = new TimerTask() {
            @Override
            public void run() {
                getUserScores();
                if (leaderBoardTimer != null) {
                    leaderBoardTimer.cancel();
                }
                leaderBoardTimer();
            }
        };
        leaderBoardTimer.schedule(leaderBoardTask, 2000);
    }

    @Override
    public void nextPiece(GamePiece piece) {
        if (piece == game.getCurrentPiece()) {
            currentPiece.setColour(game.getColour());
            currentPiece.setDisplayPiece(piece);
            currentPiece.displayCentreCircle();
        } else {
            nextPiece.setColour(game.getColour());
            nextPiece.setDisplayPiece(piece);
        }
    }

    @Override
    public void rightClick(MouseEvent event) {
        game.rotateCurrentPiece(1);
        nextPiece(game.getCurrentPiece());
    }

    @Override
    public void leftClick(MouseEvent mouseEvent) {
        game.rotateCurrentPiece(1);
        nextPiece(game.getCurrentPiece());
    }

    /**
     * also update scores scene to show multiplayer game scores rather than local
     */
    @Override
    public void gameLoopStart() {
        if (game.getStopGame()) {
            logger.info("Game finished");
            Multimedia.closeBackgroundMusic();
            gameWindow.startMultiplayerScores(game, leaderboard);
        }

        var timerBar = new Rectangle(gameWindow.getWidth(), 25);
        timerBar.setFill(Color.GREEN);

        var widthValue = new KeyValue(timerBar.widthProperty(), 0);
        var frame = new KeyFrame(Duration.millis(game.getTimerDelay()), widthValue);
        var timeline = new Timeline(frame);
        timeline.play();
        mainPane.setBottom(timerBar);
    }

}
