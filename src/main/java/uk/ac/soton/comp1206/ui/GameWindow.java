package uk.ac.soton.comp1206.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.App;
import uk.ac.soton.comp1206.event.KeyListener;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.scene.*;

import java.io.IOException;

/**
 * The GameWindow is the single window for the game where everything takes place. To move between screens in the game,
 * we simply change the scene.
 *
 * The GameWindow has methods to launch each of the different parts of the game by switching scenes. You can add more
 * methods here to add more screens to the game.
 */
public class GameWindow {

    private static final Logger logger = LogManager.getLogger(GameWindow.class);

    private final int width;
    private final int height;

    private final Stage stage;

    private BaseScene currentScene;
    private Scene scene;

    final Communicator communicator;

    private KeyListener keyListener;

    /**
     * Create a new GameWindow attached to the given stage with the specified width and height
     * @param stage stage
     * @param width width
     * @param height height
     */
    public GameWindow(Stage stage, int width, int height) {
        this.width = width;
        this.height = height;

        this.stage = stage;

        //Setup window
        setupStage();

        //Setup resources
        setupResources();

        //Setup default scene
        setupDefaultScene();

        //Setup communicator
        communicator = new Communicator("ws://discord.ecs.soton.ac.uk:9700");

        //Go to menu
        startMenu();
    }

    /**
     * Setup the font and any other resources we need
     */
    private void setupResources() {
        logger.info("Loading resources");
        Font.loadFont(getClass().getResourceAsStream("/style/Orbitron-Regular.ttf"),32);
        Font.loadFont(getClass().getResourceAsStream("/style/Orbitron-Bold.ttf"),32);
        Font.loadFont(getClass().getResourceAsStream("/style/Orbitron-ExtraBold.ttf"),32);
    }

    /**
     * Display the main menu
     */
    public void startMenu() { loadScene(new MenuScene(this)); }

    /**
     * Display the instructions
     */
    public void startInstructions() { loadScene(new InstructionsScene(this)); }

    /**
     * Display the single player challenge
     */
    public void startChallenge() { loadScene(new ChallengeScene(this)); }

    /**
     * Display the scores
     */
    public void startScores(Game game) { loadScene(new ScoresScene(this, game)); }

    /**
     * Display multi player lobby
     */
    public void startMultiplayer() { loadScene(new LobbyScene(this)); }

    /**
     * Display multiplayer game
     */
    public void startMultiplayerGame(Communicator communicator) { loadScene(new MultiplayerScene(this, communicator)); }

    /**
     * starts multiplayer scores
     */
    public void startMultiplayerScores(Game game, Leaderboard leaderboard) { loadScene(new MultiplayerScoresScene(this, game, leaderboard)); }

    /**
     * Setup the default settings for the stage itself (the window), such as the title and minimum width and height.
     */
    public void setupStage() {
        stage.setTitle("TetrECS");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
        stage.setOnCloseRequest(ev -> App.getInstance().shutdown());
    }

    /**
     * Load a given scene which extends BaseScene and switch over.
     * @param newScene new scene to load
     */
    public void loadScene(BaseScene newScene) {
        //Cleanup remains of the previous scene
        cleanup();

        //Create the new scene and set it up
        newScene.build();
        currentScene = newScene;
        scene = newScene.setScene();
        stage.setScene(scene);

        //Initialise the scene when ready
        Platform.runLater(() -> currentScene.initialise());

        //listens for key press
        scene.setOnKeyPressed(event -> keyPressed(event));
    }

    /**
     * Setup the default scene (an empty black scene) when no scene is loaded
     */
    public void setupDefaultScene() {
        this.scene = new Scene(new Pane(),width,height, Color.BLACK);
        stage.setScene(this.scene);
    }

    /**
     * When switching scenes, perform any cleanup needed, such as removing previous listeners
     */
    public void cleanup() {
        logger.info("Clearing up previous scene");
        communicator.clearListeners();
    }

    /**
     * Get the current scene being displayed
     * @return scene
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * Get the width of the Game Window
     * @return width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the Game Window
     * @return height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Get the communicator
     * @return communicator
     */
    public Communicator getCommunicator() {
        return communicator;
    }
    
    /**
     * add listener to handle event when key is clicked
     * @param listener
     */
    public void setOnKeyPressed(KeyListener listener) { this.keyListener = listener; }

    //if user presses the esc key return to menu
    private void keyPressed(KeyEvent e) {
        if (keyListener != null) {
            keyListener.keyPressed(e);
        }
    }
}