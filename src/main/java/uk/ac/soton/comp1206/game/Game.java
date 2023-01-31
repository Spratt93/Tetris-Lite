package uk.ac.soton.comp1206.game;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.GameBlockCoordinate;
import uk.ac.soton.comp1206.event.GameLoopListener;
import uk.ac.soton.comp1206.event.HighScoreListener;
import uk.ac.soton.comp1206.event.LineClearedListener;
import uk.ac.soton.comp1206.event.NextPieceListener;

import java.util.*;

/**
 * The Game class handles the main logic, state and properties of the TetrECS game. Methods to manipulate the game state
 * and to handle actions made by the player should take place inside this class.
 */
public class Game {

    private static final Logger logger = LogManager.getLogger(Game.class);

    /**
     * Number of rows
     */
    protected final int rows;

    /**
     * Number of columns
     */
    protected final int cols;

    /**
     * The grid model linked to the game
     */
    protected final Grid grid;

    private Random rand = new Random();

    private GamePiece currentPiece = spawnPiece();

    private GamePiece followingPiece = spawnPiece();

    protected IntegerProperty score = new SimpleIntegerProperty(0);

    protected IntegerProperty level = new SimpleIntegerProperty(0);

    protected IntegerProperty lives = new SimpleIntegerProperty(3);

    protected IntegerProperty multiplier = new SimpleIntegerProperty(1);

    //boolean for multiplier logic
    protected boolean prevPieceCleared = false;

    //int for level logic
    protected int prevScore = 0;

    protected NextPieceListener pieceListener;

    //initial position when using WASD
    protected int currentX = 0;
    protected int currentY = 0;

    protected LineClearedListener lineClearedListener;

    protected Timer gameTimer;

    protected GameLoopListener gameLoopListener;

    //if true stops the game
    protected boolean stop = false;

    private HighScoreListener highScoreListener;

    /**
     * Create a new game with the specified rows and columns. Creates a corresponding grid model.
     * @param cols number of columns
     * @param rows number of rows
     */
    public Game(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;

        //Create a new grid model to represent the game state
        this.grid = new Grid(cols,rows);
    }

    /**
     * Start the game
     */
    public void start() {
        logger.info("Starting game");
        initialiseGame();
    }

    /**
     * Initialise a new game and set up anything that needs to be done at the start
     */
    public void initialiseGame() {
        logger.info("Initialising game");
        logger.info("Current piece is " + currentPiece.toString());

        gameLoop();
    }

    /**
     * Handle what should happen when a particular block is clicked
     * @param gameBlock the block that was clicked
     */
    public void blockClicked(GameBlock gameBlock) {
        int x = gameBlock.getX();
        int y = gameBlock.getY();

        if (grid.canPlayPiece(currentPiece, x, y)) {
            grid.playPiece(currentPiece, x, y);
            Multimedia.playAudio("place.wav");
            afterPiece();
            gameLoop();
            nextPiece();
        }
    }

    /**
     * Get the grid model inside this game representing the game state of the board
     * @return game grid model
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Get the number of columns in this game
     * @return number of columns
     */
    public int getCols() {
        return cols;
    }

    /**
     * Get the number of rows in this game
     * @return number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * returns the current piece
     * @return
     */
    public GamePiece getCurrentPiece() { return currentPiece; }

    /**
     * returns the following piece
     * @return
     */
    public GamePiece getFollowingPiece() { return followingPiece; }

    /**
     * creates random gamepiece
     * @return
     */
    public GamePiece spawnPiece() {
        int random = rand.nextInt(15);
        var gamePiece = GamePiece.createPiece(random);
        return gamePiece;
    }

    /**
     * create the next gamepiece
     */
    public void nextPiece() {
        currentPiece = followingPiece;
        logger.info("Current piece is " + currentPiece.toString());

        int random = rand.nextInt(15);
        var newPiece = GamePiece.createPiece(random);
        followingPiece = newPiece;

        if (pieceListener != null) {
            pieceListener.nextPiece(currentPiece);
            pieceListener.nextPiece(followingPiece);
        }
    }

    /**
     * cycles through each column and row and clears a column or row if full
     */
    public void afterPiece() {
        boolean lineCleared = false;
        //checking row lines
        for (int row=0; row < grid.getRows(); row++) {
            int count = 0;
            Set<GameBlockCoordinate> rowCoordinates = new HashSet<>();
            for (int col=0; col < grid.getCols(); col++) {
                var coordinate = new GameBlockCoordinate(col, row);
                rowCoordinates.add(coordinate);
                if (grid.get(col, row) != 0) {
                    count++;
                }
            }
            if (count == grid.getCols()) {
                logger.info("Clearing row " + row);
                score(1, grid.getCols());
                lineCleared = true;
                lineCleared(rowCoordinates);
                Multimedia.playAudio("clear.wav");
                getHighScore();
            }
        }
        //checking column lines
        for (int col=0; col < grid.getCols(); col++) {
            int count = 0;
            Set<GameBlockCoordinate> colCoordinates = new HashSet<>();
            for (int row=0; row < grid.getRows(); row++) {
                var coordinate = new GameBlockCoordinate(col, row);
                colCoordinates.add(coordinate);
                if (grid.get(col, row) != 0) {
                    count++;
                }
            }
            if (count == grid.getRows()) {
                logger.info("Clearing column " + col);
                score(1, grid.getRows());
                lineCleared = true;
                lineCleared(colCoordinates);
                Multimedia.playAudio("clear.wav");
                getHighScore();
            }
        }
        if (lineCleared) {
            prevPieceCleared = true;
            setMultiplierProperty(getMultiplierProperty()+1);
            logger.info("Increasing multiplier");
        } else {
            prevPieceCleared = false;
            setMultiplierProperty(1);
            logger.info("Resetting multiplier");
        }
    }

    public IntegerProperty scoreProperty() { return score; }

    public IntegerProperty levelProperty() { return level; }

    public IntegerProperty livesProperty() { return lives; }

    public IntegerProperty multiplierProperty() { return multiplier; }

    public int getScoreProperty() { return score.get(); }

    public int getLevelProperty() { return level.get(); }

    public int getLivesProperty() { return lives.get(); }

    public int getMultiplierProperty() { return multiplier.get(); }

    public void setScoreProperty(int value) { score.set(value);}

    public void setLevelProperty(int value) { level.set(value);}

    public void setLivesProperty(int value) { lives.set(value);}

    public void setMultiplierProperty(int value) { multiplier.set(value);}

    /**
     * calculates the score
     * @param lines
     * @param blocks
     */
    public void score(int lines, int blocks) {
        setScoreProperty((lines * blocks * 10 * getMultiplierProperty()) + getScoreProperty());
        if (getScoreProperty() - prevScore >= 1000) {
            setLevelProperty(getLevelProperty()+1);
            prevScore += 1000;
            logger.info("Level set to: " + levelProperty().toString());
        }
    }

    /**
     * sets the piece listener
     * @param listener
     */
    public void setNextPieceListener(NextPieceListener listener) { this.pieceListener = listener; }

    /**
     * rotates the current piece
     */
    public void rotateCurrentPiece(int rotations) {
        currentPiece.rotate(rotations);
        Multimedia.playAudio("rotate.wav");
    }

    /**
     * swaps the current and next piece
     */
    public void swapCurrentPiece() {
        //temporary piece aiding with logic
        var tempPiece = currentPiece;
        currentPiece = followingPiece;
        followingPiece = tempPiece;
        Multimedia.playAudio("rotate.wav");
    }

    /**
     * returns current x pos when using WASD
     * @return
     */
    public int getCurrentX() { return currentX; }

    /**
     * returns current y pos when using WASD
     * @return
     */
    public int getCurrentY() { return currentY; }

    /**
     * updates the current x pos when using WASD
     * @param change
     */
    public void setCurrentX(int change) {
        if ((currentX + change) <= 0) {
            currentX = 0;
        }
        else if ((currentX + change) >= 4 ) {
            currentX = 4;
        }
        else {
            currentX += change;
        }
    }

    /**
     * updates current y pos when using WASD
     * @param change
     */
    public void setCurrentY(int change) {
        if ((currentY + change) <= 0) {
            currentY = 0;
        }
        else if ((currentY + change) >= 4 ) {
            currentY = 4;
        }
        else {
            currentY += change;
        }
    }

    public void setLineClearedListener(LineClearedListener listener) { this.lineClearedListener = listener; }

    private void lineCleared(Set<GameBlockCoordinate> coordinates) {
        if (lineClearedListener != null) {
            lineClearedListener.lineCleared(coordinates);
        }
    }

    /**
     * resets the timer, resets mult and decr lives and changes curr piece
     */
    public void gameLoop() {
        if (gameTimer != null) {
            //reset timer
            gameTimer.cancel();
        }
        if (getLivesProperty() < 0) {
            stopGame();
            Platform.runLater(() -> gameLoopStart());
            return;
        }
        gameTimer = new Timer();
        var timerTask = new TimerTask() {
            @Override
            public void run() {
                logger.info("Game Loop Finished: set mult to 1, lives -1, curr piece discarded");
                setMultiplierProperty(1);
                setLivesProperty(getLivesProperty()-1);
                Multimedia.playAudio("lifelose.wav");
                nextPiece();
                gameLoop();
            }
        };
        gameTimer.schedule(timerTask, getTimerDelay());

        //handle game loop listener
        Platform.runLater(() -> gameLoopStart());
    }

    /**
     * returns delay between game loop call
     * @return
     */
    public long getTimerDelay() {
        return Math.max(2500, 12000 - (500 * getLevelProperty()));
    }

    public void setOnGameLoop(GameLoopListener listener) { this.gameLoopListener = listener; }

    public void gameLoopStart() {
        if (gameLoopListener != null) { gameLoopListener.gameLoopStart(); }
    }

    public void stopGame() {
        stop = true;
    }

    public boolean getStopGame() {
        return stop;
    }

    /**
     * stops the game timer -- used when esc key pressed
     */
    public void stopGameTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }

    public void setOnHighScore(HighScoreListener listener) { this.highScoreListener = listener; }

    private void getHighScore() {
        if (highScoreListener != null) {
            highScoreListener.getHighScore();
        }
    }

    public int getColour() {
        return grid.getColour();
    }
}
