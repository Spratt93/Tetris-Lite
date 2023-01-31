package uk.ac.soton.comp1206.game;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.scene.MultiplayerScene;

import java.util.Timer;
import java.util.TimerTask;

public class MultiplayerGame extends Game {

    private Communicator communicator;

    private static final Logger logger = LogManager.getLogger(MultiplayerGame.class);

    private GamePiece currentpiece;
    private GamePiece nextpiece;
    private boolean isCurrent = true;

    public MultiplayerGame(int cols, int rows, Communicator comm) {
        super(cols, rows);
        communicator = comm;
        communicator.addListener(this::receiveCommunication);

        getPiece();
        getPiece();
    }

    /**
     * gets current and next piece from the communicator
     */
    public void getPiece() {
        communicator.send("PIECE");
    }

    public void receiveCommunication(String message) {
        if (message.contains("PIECE")) {
            String message1 = message.replaceFirst("PIECE ", "");
            Platform.runLater(() -> receivePieceCommunication(message1));
        }
    }

    @Override
    public void blockClicked(GameBlock block) {
        /**
         * send message to comm
         */
        int x = block.getX();
        int y = block.getY();

        if (grid.canPlayPiece(currentpiece, x, y)) {
            grid.playPiece(currentpiece, x, y);
            Multimedia.playAudio("place.wav");
            afterPiece();
            gameLoop();
            communicator.send("SCORE " + getScoreProperty());
            nextPiece();
        }
    }

    @Override
    public void gameLoop() {
        if (gameTimer != null) {
            //reset timer
            gameTimer.cancel();
        }
        if (getLivesProperty() < 0) {
            stopGame();
            //tells server that you're dead
            communicator.send("DIE");
            Platform.runLater(() -> gameLoopStart());
            return;
        }
        gameTimer = new Timer();
        var timerTask = new TimerTask() {
            @Override
            public void run() {
                logger.info("Multiplayer Game Loop Finished: set mult to 1, lives -1, curr piece discarded");
                setMultiplierProperty(1);
                setLivesProperty(getLivesProperty()-1);
                Multimedia.playAudio("lifelose.wav");
                //send lives and next piece
                communicator.send("LIVES " + getLivesProperty());
                nextPiece();

                gameLoop();
            }
        };
        gameTimer.schedule(timerTask, getTimerDelay());

        //handle game loop listener
        Platform.runLater(() -> gameLoopStart());
    }

    @Override
    public void nextPiece() {
        currentpiece = nextpiece;
        getPiece();

        if (pieceListener != null) {
            pieceListener.nextPiece(currentpiece);
        }
    }

     public void receivePieceCommunication(String mssg) {
         if (isCurrent) {
             currentpiece = GamePiece.createPiece(Integer.valueOf(mssg));
             logger.info(currentpiece.getBlocks());
             isCurrent = false;
         } else {
             nextpiece = GamePiece.createPiece(Integer.valueOf(mssg));
             pieceListener.nextPiece(nextpiece);
         }
    }

    @Override
    public GamePiece getCurrentPiece() { return currentpiece; }

    @Override
    public GamePiece getFollowingPiece() { return nextpiece; }

    @Override
    public int getColour() { return grid.getColour(); }

    @Override
    public void swapCurrentPiece() {
        //temporary piece aiding with logic
        var tempPiece = currentpiece;
        currentpiece = nextpiece;
        nextpiece = tempPiece;
        Multimedia.playAudio("rotate.wav");
    }

    @Override
    public void rotateCurrentPiece(int rotations) {
        currentpiece.rotate(rotations);
        Multimedia.playAudio("rotate.wav");
    }

    /**
     * same as game but...
     * use a queue request pieces as needed from server
     * populate current and next piece
     * listen for score updates and chat messages
     * send appropriate updates to communicator
     */
}
