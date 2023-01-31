package uk.ac.soton.comp1206.game;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * The Grid is a model which holds the state of a game board. It is made up of a set of Integer values arranged in a 2D
 * arrow, with rows and columns.
 *
 * Each value inside the Grid is an IntegerProperty can be bound to enable modification and display of the contents of
 * the grid.
 *
 * The Grid contains functions related to modifying the model, for example, placing a piece inside the grid.
 *
 * The Grid should be linked to a GameBoard for it's display.
 */
public class Grid {

    private static final Logger logger = LogManager.getLogger(Grid.class);

    /**
     * The number of columns in this grid
     */
    private final int cols;

    /**
     * The number of rows in this grid
     */
    private final int rows;

    /**
     * The grid is a 2D arrow with rows and columns of SimpleIntegerProperties.
     */
    private final SimpleIntegerProperty[][] grid;

    private int colour = 1;

    /**
     * Create a new Grid with the specified number of columns and rows and initialise them
     * @param cols number of columns
     * @param rows number of rows
     */
    public Grid(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;

        //Create the grid itself
        grid = new SimpleIntegerProperty[cols][rows];

        //Add a SimpleIntegerProperty to every block in the grid
        for(var y = 0; y < rows; y++) {
            for(var x = 0; x < cols; x++) {
                grid[x][y] = new SimpleIntegerProperty(0);
            }
        }
    }

    /**
     * Get the Integer property contained inside the grid at a given row and column index. Can be used for binding.
     * @param x column
     * @param y row
     * @return the IntegerProperty at the given x and y in this grid
     */
    public IntegerProperty getGridProperty(int x, int y) {
        return grid[x][y];
    }

    /**
     * Update the value at the given x and y index within the grid
     * @param x column
     * @param y row
     * @param value the new value
     */
    public void set(int x, int y, int value) {
        grid[x][y].set(value);
    }

    /**
     * Get the value represented at the given x and y index within the grid
     * @param x column
     * @param y row
     * @return the value
     */
    public int get(int x, int y) {
        try {
            //Get the value held in the property at the x and y index provided
            return grid[x][y].get();
        } catch (ArrayIndexOutOfBoundsException e) {
            //No such index
            return -1;
        }
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
     * takes a gamepiece with a given x and y, and returns whether can be played or not
     * @param gamepiece
     * @param x
     * @param y
     * @return
     */
    public boolean canPlayPiece(GamePiece gamepiece, int x, int y) {
        for (int i=0; i < gamepiece.getBlocks().length; i++) {
            int col = x-1+i;
            for (int j=0; j < 3; j++) {
                int row = y - 1 + j;
                if (gamepiece.getBlocks()[i][j] == gamepiece.getValue()) {
                    if (this.get(col, row) != 0) {
                        logger.info("Can't play piece because " + col + "," + row + " is not free");
                        Multimedia.playAudio("fail.wav");
                        return false;
                    }
                    logger.info(col + "," + row + " is free");
                }
            }
        }
        logger.info("Possible to play piece");
        return true;
    }

    /**
     * paints the blocks for the given gamepiece
     * @param gamepiece
     * @param x
     * @param y
     */
    public void playPiece(GamePiece gamepiece, int x, int y) {
        if (colour > 15) {
            colour = 1;
        }

        for (int i=0; i < gamepiece.getBlocks().length; i++) {
            int col = x-1+i;
            for (int j=0; j < 3; j++) {
                int row = y - 1 + j;
                if (gamepiece.getBlocks()[i][j] == gamepiece.getValue()) {
                    int prevValue = this.get(col, row);
                    int newValue = prevValue + colour;
                    if (newValue > GamePiece.PIECES) {
                        newValue = 0;
                    }
                    this.set(col, row, newValue);
                    logger.info("Painting block at " + col + "," + row);
                }
            }
        }

        colour++;
    }

    public int getColour() {
        return colour;
    }

}
