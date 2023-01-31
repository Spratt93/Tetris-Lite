package uk.ac.soton.comp1206.scene;

import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.game.Multimedia;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

import java.io.IOException;

/**
 * The main menu of the game. Provides a gateway to the rest of the game.
 */
public class MenuScene extends BaseScene {

    private static final Logger logger = LogManager.getLogger(MenuScene.class);

    /**
     * Create a new menu scene
     * @param gameWindow the Game Window this will be displayed in
     */
    public MenuScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Menu Scene");
    }

    /**
     * Build the menu layout
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var menuPane = new StackPane();
        menuPane.setMaxWidth(gameWindow.getWidth());
        menuPane.setMaxHeight(gameWindow.getHeight());
        menuPane.getStyleClass().add("menu-background");
        root.getChildren().add(menuPane);

        var mainPane = new BorderPane();
        menuPane.getChildren().add(mainPane);

        /**
         * TITLE
         */
        var title = new ImageView(new Image(this.getClass().getResource("/images/TetrECS.png").toExternalForm()));
        title.setFitWidth(gameWindow.getWidth()-300);
        title.setFitHeight(gameWindow.getHeight()-300);
        title.setPreserveRatio(true);
        title.setX(150);
        title.setY(100);

        var titlePane = new Pane();
        titlePane.getChildren().add(title);

        /**
         * moving title animation
         */
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(2000), title);
        rotateTransition.setFromAngle(-10f);
        rotateTransition.setToAngle(10f);
        rotateTransition.setCycleCount(Timeline.INDEFINITE);
        rotateTransition.setAutoReverse(true);

        rotateTransition.play();

        mainPane.setTop(titlePane);

        /**
         * SINGLE PLAYER
         */
        var play = new Text("Single Player");
        play.getStyleClass().add("menuItem");
        //binding the text with starting the game
        play.setOnMouseClicked((e) -> {
            rotateTransition.stop();
            startGame(e);
        });
        //hover effect
        play.setOnMouseEntered((e) -> textHover(play));
        play.setOnMouseExited((e) -> textExit(play));

        /**
         * INSTRUCTIONS
         */
        var howToPlay = new Text("How to play");
        howToPlay.getStyleClass().add("menuItem");
        howToPlay.setOnMouseClicked((e) -> {
            Multimedia.playAudio("rotate.wav");
            rotateTransition.stop();
            startInstructions(e);
        });
        howToPlay.setOnMouseEntered((e) -> textHover(howToPlay));
        howToPlay.setOnMouseExited((e) -> textExit(howToPlay));

        /**
         * MULTIPLAYER
         */
        var multiPlayer = new Text("Multi Player");
        multiPlayer.getStyleClass().add("menuItem");
        multiPlayer.setOnMouseClicked(event -> {
            rotateTransition.stop();
            startMultiPlayer(event);
        });
        multiPlayer.setOnMouseEntered((e) -> textHover(multiPlayer));
        multiPlayer.setOnMouseExited((e) -> textExit(multiPlayer));

        //Vbox that holds action text
        var options = new VBox();
        options.getChildren().addAll(play, multiPlayer, howToPlay);
        options.setAlignment(Pos.CENTER);

        mainPane.setCenter(options);

        Multimedia.playBackgroundMusic("menu.mp3");
    }

    /**
     * Initialise the menu
     */
    @Override
    public void initialise() {

    }

    /**
     * Handle when the Start Game button is pressed
     * @param event event
     */
    private void startGame(MouseEvent event) {
        Multimedia.closeBackgroundMusic();
        gameWindow.startChallenge();
    }

    private void startInstructions(MouseEvent event) {
        Multimedia.closeBackgroundMusic();
        gameWindow.startInstructions();
    }

    private void startMultiPlayer(MouseEvent event) {
        Multimedia.closeBackgroundMusic();
        gameWindow.startMultiplayer();
    }

    public void textHover(Text text) {
        text.setFill(Color.YELLOW);
    }

    public void textExit(Text text) {
        text.setFill(Color.WHITE);
    }

}
