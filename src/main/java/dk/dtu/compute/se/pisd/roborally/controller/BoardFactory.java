package dk.dtu.compute.se.pisd.roborally.controller;

import dk.dtu.compute.se.pisd.roborally.model.Board;
import dk.dtu.compute.se.pisd.roborally.model.Heading;
import dk.dtu.compute.se.pisd.roborally.model.Space;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory for creating boards. The factory itself is implemented as a singleton.
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 */
public class BoardFactory {

    /**
     * The single instance of this class, which is lazily instantiated on demand.
     */
    static private BoardFactory instance = null;

    /**
     * Constructor for BoardFactory. It is private in order to make the factory a singleton.
     */
    private BoardFactory() {
    }

    /**
     * Returns the single instance of this factory. The instance is lazily
     * instantiated when requested for the first time.
     *
     * @return the single instance of the BoardFactory
     */
    public static BoardFactory getInstance() {
        if (instance == null) {
            instance = new BoardFactory();
        }
        return instance;
    }


    /** The default board name used as a fallback when no valid name is provided. */
    private static final String DEFAULT_NAME = "<none>";

    /** The name identifier for the simple 8x8 board. */
    private static final String SIMPLE_BOARD_NAME = "Simple";

    /** The name identifier for the advanced 10x10 board. */
    private static final String ADVANCED_BOARD_NAME = "Advanced";


    /**
     * * This method implements a factory pattern to return different board
     *   configurations. If the name is unknown or null, a default "Simple"
     *   board is returned to ensure the game can always start (defensive programming).
     * @param name the name of the board to be created
     * @return the new board corresponding to the given name
     */
    public Board createBoard(String name) {
        if (name == null || name.equals(DEFAULT_NAME)) {
            return createDefaultBoard();
        } else if (name.equals(SIMPLE_BOARD_NAME)) {
            return createDefaultBoard();
        } else if (name.equals(ADVANCED_BOARD_NAME)) {
            return createAdvancedBoard();
        } else {
            return createDefaultBoard();
        }
    }


    /**
     * Creates a basic 8x8 board named "Simple" with a few walls and conveyor belts.
     *
     * @return a simple Board configuration
     */
    private Board createDefaultBoard() {
        Board board = new Board(8, 8, "Simple");

        for (int col = 0; col < 4; col++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.EAST);
            board.getSpace(col, 2).getActions().add(belt);
        }

        for (int col = 4; col < 8; col++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.WEST);
            board.getSpace(col, 5).getActions().add(belt);
        }

        for (int row = 3; row <= 5; row++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.NORTH);
            board.getSpace(2, row).getActions().add(belt);
        }


        for (int row = 2; row <= 4; row++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.SOUTH);
            board.getSpace(5, row).getActions().add(belt);
        }





        board.getSpace(2, 3).getWalls().add(Heading.EAST);
        board.getSpace(3, 3).getWalls().add(Heading.NORTH);

        board.getSpace(4, 5).getWalls().add(Heading.WEST);
        board.getSpace(5, 5).getWalls().add(Heading.SOUTH);
        board.getSpace(7, 5).getWalls().add(Heading.NORTH);



        board.getSpace(2, 1).getWalls().add(Heading.WEST);

        board.getSpace(1, 3).getWalls().add(Heading.SOUTH);
        board.getSpace(6, 1).getWalls().add(Heading.EAST);

        board.getSpace(4, 6).getWalls().add(Heading.SOUTH);
        board.getSpace(6, 5).getWalls().add(Heading.NORTH);

        Checkpoint checkpoint1 = new Checkpoint(1);
        board.getSpace(3, 1).getActions().add(checkpoint1);

        Checkpoint checkpoint2 = new Checkpoint(2);
        board.getSpace(6, 3).getActions().add(checkpoint2);

        Checkpoint checkpoint3 = new Checkpoint(3);
        checkpoint3.setLastCheckPoint(true);
        board.getSpace(1, 6).getActions().add(checkpoint3);

        return board;
    }


    /**
     * Creates a more complex 10x10 board named "Advanced".
     * This board reflects a typical RoboRally advanced arena with:
     * - Diagonal starting positions (0,0), (1,1), (2,2), etc.
     * - Multiple conveyor belt corridors creating traffic flow
     * - Strategic wall placements forming obstacles and channels
     * - Three checkpoints positioned to require navigation skill
     * - Hazard zones that challenge player planning
     *
     * @return an advanced Board configuration
     * @author Mikkel Hjelm
     */
    private Board createAdvancedBoard() {
        Board board = new Board(10, 10, "Advanced");

        for (int col = 0; col <= 4; col++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.EAST);
            board.getSpace(col, 3).getActions().add(belt);
        }

        for (int col = 3; col <= 7; col++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.WEST);
            board.getSpace(col, 6).getActions().add(belt);
        }

        for (int row = 2; row <= 5; row++) {
            ConveyorBelt belt = new ConveyorBelt();
            belt.setHeading(Heading.SOUTH);
            board.getSpace(7, row).getActions().add(belt);
        }

        ConveyorBelt trap1 = new ConveyorBelt();
        trap1.setHeading(Heading.NORTH);
        board.getSpace(9, 8).getActions().add(trap1);

        ConveyorBelt trap2 = new ConveyorBelt();
        trap2.setHeading(Heading.EAST);
        board.getSpace(1, 7).getActions().add(trap2);



        board.getSpace(2, 2).getWalls().add(Heading.NORTH);


        board.getSpace(1, 3).getWalls().add(Heading.SOUTH);
        board.getSpace(3, 3).getWalls().add(Heading.NORTH);
        board.getSpace(4, 3).getWalls().add(Heading.SOUTH);


        board.getSpace(4, 5).getWalls().add(Heading.WEST);

        board.getSpace(3, 6).getWalls().add(Heading.SOUTH);
        board.getSpace(5, 5).getWalls().add(Heading.SOUTH);
        board.getSpace(7, 6).getWalls().add(Heading.EAST);

        board.getSpace(5, 4).getWalls().add(Heading.NORTH);
        board.getSpace(5, 4).getWalls().add(Heading.WEST);
        board.getSpace(6, 5).getWalls().add(Heading.EAST);
        board.getSpace(6, 5).getWalls().add(Heading.SOUTH);

        board.getSpace(8, 2).getWalls().add(Heading.NORTH);
        board.getSpace(8, 2).getWalls().add(Heading.EAST);
        board.getSpace(8, 3).getWalls().add(Heading.EAST);

        board.getSpace(1, 9).getWalls().add(Heading.NORTH);
        board.getSpace(2, 8).getWalls().add(Heading.EAST);
        board.getSpace(2, 9).getWalls().add(Heading.NORTH);

        board.getSpace(3, 1).getWalls().add(Heading.SOUTH);
        board.getSpace(6, 1).getWalls().add(Heading.EAST);
        board.getSpace(8, 7).getWalls().add(Heading.NORTH);
        board.getSpace(9, 9).getWalls().add(Heading.WEST);


        Checkpoint checkpoint1 = new Checkpoint(1);
        board.getSpace(8, 1).getActions().add(checkpoint1);

        Checkpoint checkpoint2 = new Checkpoint(2);
        board.getSpace(5, 5).getActions().add(checkpoint2);

        Checkpoint checkpoint3 = new Checkpoint(3);
        checkpoint3.setLastCheckPoint(true);
        board.getSpace(1, 9).getActions().add(checkpoint3);

        return board;
    }


    /**
     * Returns a list of all available board names that this factory can create.
     * This method is used by the AppController to populate the board selection
     * dialog, allowing the user to choose between different game arenas.
     *
     * @return a List of Strings containing all available board names
     * @author Christoffer Sørensen
     */
    public List<String> getBoardNames() {
        List<String> underlyingList = new ArrayList<>();
        underlyingList.add(SIMPLE_BOARD_NAME);
        underlyingList.add(ADVANCED_BOARD_NAME);
        return Collections.unmodifiableList(underlyingList);
    }
}