package dk.dtu.compute.se.pisd.roborally.controller;

import dk.dtu.compute.se.pisd.roborally.model.Player;
import dk.dtu.compute.se.pisd.roborally.model.Space;

/**
 * This class represents a checkpoint on the RoboRally board
 * A checkpoint is a field action that can be placed on a {@link Space}.
 * Each checkpoint has a unique number that indicates the order in which players must reach them
 *
 * Checkpoints are used to track player progress during the game.
 * @author Mikkel Hjelm
 */
public class Checkpoint extends FieldAction {

    private int number;

    private boolean lastCheckPoint;

    /**
     * The constructor for the checkpoint class
     * If the number instantiated is negative we throw an exception
     * @param number The number of the checkpoint
     * @author Mikkel Hjelm
     */
    public Checkpoint(int number) {
        if(number <= 0 ) {
            throw new IllegalArgumentException("Checkpoint number must be positive");
        }
        this.number = number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public boolean isLastCheckPoint() {
        return lastCheckPoint;
    }
    public void setLastCheckPoint(boolean lastCheckPoint) {
        this.lastCheckPoint = lastCheckPoint;
    }

    @Override
    public boolean doAction(GameController gameController, Space space) {
        if (space == null) return false;

        Player player = space.getPlayer();
        if (player == null) return false;

        int reached = player.getCheckpointsReached();

        if (reached == number - 1) {
            player.setCheckpointsReached(number);
            if(lastCheckPoint) {
                 gameController.finishGame(player);
            }
            return true;
        }
        return false;
    }
}
