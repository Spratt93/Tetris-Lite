package uk.ac.soton.comp1206.scene;

import javafx.animation.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.GameBlockCoordinate;
import uk.ac.soton.comp1206.component.GameBoard;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.game.GamePiece;
import uk.ac.soton.comp1206.game.Multimedia;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

/**
 * The Single Player challenge scene. Holds the UI for the single player challenge mode in the game.
 */
public class ChallengeScene extends BaseScene {

    private static final Logger logger = LogManager.getLogger(ChallengeScene.class);
    protected Game game;
    private PieceBoard currentPiece;
    private PieceBoard nextPiece;
    protected GameBoard board;
    protected BorderPane mainPane;
    private File scoresFile = new File("./scores.txt");
    private Text highScoreText;

    /**
     * Create a new Single Player challenge scene
     * @param gameWindow the Game Window
     */
    public ChallengeScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Menu Scene");
    }

    /**
     * Build the Challenge window
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
        mainPane.setCenter(board);

        //Handle block on gameboard grid being clicked
        board.setOnBlockClick(this::blockClicked);

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

        var highScoreLabel = new Text("High Score: ");
        highScoreText = new Text();
        highScoreText.textProperty().bind(highScoreFromFile().asString());
        game.setOnHighScore(this::getHighScore);

        var multiplierLabel = new Text("Multiplier: ");
        var multiplierText = new Text();
        multiplierText.textProperty().bind(game.multiplierProperty().asString());

        scoreText.getStyleClass().add("score");
        levelText.getStyleClass().add("level");
        livesText.getStyleClass().add("lives");
        multiplierText.getStyleClass().add("multiplier");
        scoreLabel.getStyleClass().add("heading");
        levelLabel.getStyleClass().add("heading");
        livesLabel.getStyleClass().add("heading");
        multiplierLabel.getStyleClass().add("heading");
        highScoreLabel.getStyleClass().add("heading");
        highScoreText.getStyleClass().add("hiscore");
        hbox.getChildren().addAll(scoreLabel, scoreText, levelLabel, levelText, livesLabel, livesText, multiplierLabel, multiplierText, highScoreLabel, highScoreText);
        hbox.setSpacing(10);
        mainPane.setTop(hbox);

        /**
         * current piece displayed on the top right
         */
        var currentPieceLabel = new Text("Current Piece");
        currentPieceLabel.getStyleClass().add("pieceLabel");

        currentPiece = new PieceBoard(3, 3, gameWindow.getWidth()/5, gameWindow.getHeight()/5);
        currentPiece.setPadding(new Insets(30, 10, 30, 10));
        currentPiece.setDisplayPiece(game.getCurrentPiece());
        //displays the reference circle
        currentPiece.displayCentreCircle();

        /**
         * next piece displayed bottom right
         */
        var nextPieceLabel = new Text("Next Piece");
        nextPieceLabel.getStyleClass().add("pieceLabel");

        nextPiece = new PieceBoard(3, 3, gameWindow.getWidth()/7, gameWindow.getHeight()/7);
        nextPiece.setPadding(new Insets(30, 5, 30, 5));
        nextPiece.setDisplayPiece(game.getFollowingPiece());

        //vbox holding pieceboards
        var pieceBoards = new VBox();
        pieceBoards.getChildren().addAll(currentPieceLabel, currentPiece, nextPieceLabel, nextPiece);
        mainPane.setRight(pieceBoards);

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
    }

    /**
     * Handle when a block is clicked
     * @param gameBlock the Game Block that was clocked
     */
    private void blockClicked(GameBlock gameBlock) {
        game.blockClicked(gameBlock);
    }

    /**
     * Setup the game object and model
     */
    public void setupGame() {
        logger.info("Starting a new challenge");

        //Start new game
        game = new Game(5, 5);
    }

    /**
     * Initialise the scene and start the game
     */
    @Override
    public void initialise() {
        logger.info("Initialising Challenge");
        game.start();
        Multimedia.playBackgroundMusic("game.wav");
    }

    //if user clicks esc return to the menu
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

    }

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

    public void rightClick(MouseEvent event) {
        game.rotateCurrentPiece(1);
        nextPiece(game.getCurrentPiece());
    }

    public void leftClick(MouseEvent mouseEvent) {
        game.rotateCurrentPiece(1);
        nextPiece(game.getCurrentPiece());
    }

    public void lineCleared(Set<GameBlockCoordinate> coordinates) { board.fadeOut(coordinates); }

    /**
     * creates a progress bar at bottom of screen that is synced with game timer
     */
    public void gameLoopStart() {
        if (game.getStopGame()) {
            logger.info("Game finished");
            Multimedia.closeBackgroundMusic();
            gameWindow.startScores(game);
        }

        var timerBar = new Rectangle(gameWindow.getWidth(), 25);
        timerBar.setFill(Color.GREEN);

        var widthValue = new KeyValue(timerBar.widthProperty(), 0);
        var frame = new KeyFrame(Duration.millis(game.getTimerDelay()), widthValue);
        var timeline = new Timeline(frame);
        timeline.play();
        mainPane.setBottom(timerBar);

        /*timerBar = new Rectangle(400, 25);
        timerBar.setFill(Color.ORANGE);

        var widthValue2 = new KeyValue(timerBar.widthProperty(), 200);
        var frame2 = new KeyFrame(Duration.millis(game.getTimerDelay()/4), widthValue2);
        var timeline2 = new Timeline(frame2);
        //mainPane.setBottom(timerBar2);

        timerBar = new Rectangle(200, 25);
        timerBar.setFill(Color.RED);

        var widthValue3 = new KeyValue(timerBar.widthProperty(), 0);
        var frame3 = new KeyFrame(Duration.millis(game.getTimerDelay()/4), widthValue3);
        var timeline3 = new Timeline(frame3);
        //mainPane.setBottom(timerBar3);

        var sequence = new SequentialTransition(timeline, timeline2, timeline3);
        sequence.play();
        mainPane.setBottom(timerBar);*/
    }

    /**
     * returns the high score from the scores file
     * @throws IOException
     */
    public IntegerProperty highScoreFromFile() {
        logger.info("Checking high score");

        try {
            var input = new BufferedReader(new FileReader(scoresFile));
            var line = input.readLine();
            input.close();
            var split = line.split(":");

            IntegerProperty highScore = new SimpleIntegerProperty(Integer.valueOf(split[1]));

            return highScore;
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * checks whether game score is greater than high score
     * if so then replaces high score with game score
     */
    public void getHighScore() {
        if (game.getScoreProperty() > highScoreFromFile().get()) {
            logger.info("high score being set");
            highScoreText.textProperty().unbind();
            highScoreText.textProperty().bind(game.scoreProperty().asString());
        }
    }
}
