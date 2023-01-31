package uk.ac.soton.comp1206.scene;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.game.GamePiece;
import uk.ac.soton.comp1206.game.Multimedia;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

import java.io.IOException;

public class InstructionsScene extends BaseScene {

    private static final Logger logger = LogManager.getLogger(InstructionsScene.class);

    public InstructionsScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating instructions Scene");
    }

    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());
        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var instructionsPane = new StackPane();
        instructionsPane.setMaxWidth(gameWindow.getWidth());
        instructionsPane.setMaxHeight(gameWindow.getHeight());
        instructionsPane.getStyleClass().add("menu-background");
        root.getChildren().add(instructionsPane);

        var mainPane = new BorderPane();
        instructionsPane.getChildren().add(mainPane);

        //image with instructions on
        var instructions = new ImageView(new Image(this.getClass().getResource("/images/Instructions.png").toExternalForm()));
        instructions.setFitHeight(gameWindow.getHeight()/1.5);
        instructions.setFitWidth(gameWindow.getWidth()/1.5);
        instructions.setPreserveRatio(true);

        var hbox = new HBox();
        hbox.getChildren().add(instructions);
        hbox.setAlignment(Pos.CENTER);
        mainPane.setTop(hbox);

        //gridpane containing all possible pieces
        var possiblePieces = new GridPane();
        int currentPieceNumber = 0;
        for (int col  = 0; col < 5; col++) {
            for (int row = 0; row < 3; row++) {
                var currentPieceBoard = new PieceBoard(3, 3, gameWindow.getWidth()/10, gameWindow.getHeight()/10);
                var currentGamePiece = GamePiece.createPiece(currentPieceNumber);
                currentPieceBoard.setDisplayPiece(currentGamePiece);
                currentPieceBoard.setPadding(new Insets(10));
                possiblePieces.add(currentPieceBoard, col, row);
                currentPieceNumber++;
            }
        }
        possiblePieces.setAlignment(Pos.CENTER);
        mainPane.setBottom(possiblePieces);

        //if esc key pressed call local keyPressed()
        gameWindow.setOnKeyPressed(this::KeyPressed);
    }

    @Override
    public void initialise() {

    }

    /**
     * ESCAPE -> Main Menu
     * @param e
     */
    public void KeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            logger.info("Returning to menu");
            Multimedia.closeBackgroundMusic();
            gameWindow.startMenu();
        }
    }

}
