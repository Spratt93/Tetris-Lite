package uk.ac.soton.comp1206.component;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.event.LeftClickListener;
import uk.ac.soton.comp1206.game.GamePiece;

public class PieceBoard extends GameBoard {

    private static final Logger logger = LogManager.getLogger(PieceBoard.class);

    private LeftClickListener leftClickListener;

    private int colour = 1;

    public PieceBoard(int cols, int rows, double width, double height) {
        super(cols, rows, width, height);
        this.setOnMouseClicked(event -> leftClick(event));
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    /**
     * sets display of next piece
     */
    public void setDisplayPiece(GamePiece piece) {
        logger.info("Displaying current piece");

        //clear the previous piece
        for (int i=0; i < grid.getCols(); i++) {
            for (int j=0; j < grid.getRows(); j++) {
                grid.set(i, j, 0);
            }
        }

        //paint the piece
        for (int i=0; i < piece.getBlocks().length; i++) {
            int col = i;
            for (int j=0; j < 3; j++) {
                int row = j;
                if (piece.getBlocks()[i][j] == piece.getValue()) {
                    int prevValue = grid.get(col, row);
                    int newValue = prevValue + colour;
                    if (newValue > GamePiece.PIECES) {
                        newValue = 0;
                    }
                    grid.set(col, row, newValue);
                }
            }
        }

        colour++;
    }

    public void setLeftClickedListener(LeftClickListener listener) { this.leftClickListener = listener; }

    private void leftClick(MouseEvent event) {
        if (leftClickListener != null && event.getButton() == MouseButton.PRIMARY) {
            leftClickListener.leftClick(event);
        }
    }

    /**
     * draw circle in middle of piece board
     */
    public void displayCentreCircle() {
        getBlock(1, 1).circle();
    }

}
