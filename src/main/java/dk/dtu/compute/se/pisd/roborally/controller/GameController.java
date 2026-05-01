/*
 *  This file is part of the initial project provided for the
 *  course "Project in Software Development (02362)" held at
 *  DTU Compute at the Technical University of Denmark.
 *
 *  Copyright (C) 2019, 2020: Ekkart Kindler, ekki@dtu.dk
 *
 *  This software is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this project; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package dk.dtu.compute.se.pisd.roborally.controller;

import dk.dtu.compute.se.pisd.roborally.model.*;
import org.jetbrains.annotations.NotNull;

/**
 *
 * This controller represents the main game logic of RoboRally.
 * It manages the current state of the game and executes the rules
 * for player movement, command cards, phases, and board actions.
 *
 * The GameController coordinates the interaction between the board
 * and the players during the execution of a game.
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
public class GameController {

    final public Board board;

    /**
     * Creates a new game controller for the given board.
     * The controller uses the board as the central game state and
     * operates directly on its players, spaces, phases, and steps.
     *
     * @param board the board whose game state is controlled by this controller
     */
    public GameController(@NotNull Board board) {
        this.board = board;
    }

    /**
     * Moves the current player directly to the given space if the space is free.
     * If the target space is already occupied by any player, the move is ignored.
     * After a successful move, the move counter is incremented and the turn
     * advances to the next player.
     *
     * This method is primarily a simple helper for testing and setup and does not
     * represent the full RoboRally movement logic.
     *
     * @param space the space to which the current player should move
     */
    public void moveCurrentPlayerToSpace(@NotNull Space space)  {
        if(space == null) return;
        Player p = board.getCurrentPlayer();
        int Pnum = board.getPlayerNumber(p);
        int size = board.getPlayersNumber();
        for (int i = 0; i < size; i++) {
            Player other = board.getPlayer(i);
            if(other.getSpace() == null) continue;
            else if (other.getSpace().equals(space)) {
                if (other.equals(p)) {
                    System.out.println("Space already occupied by that player");
                } else {
                    System.out.println("Space occupied by another player");
                }
                return;
            }
        }
        p.setSpace(space);
        board.IncMovecounter();
        Pnum++;
        board.setCurrentPlayer(board.getPlayer(Pnum % size));
    }

    /**
     * Starts the programming phase of the game.
     * The phase is set to {@link Phase#PROGRAMMING}, the current player is reset
     * to the first player, and the step counter is reset to 0.
     *
     * All program registers are cleared and made visible, and each player's card
     * fields are filled with newly generated random command cards.
     */
    public void startProgrammingPhase() {
        board.setPhase(Phase.PROGRAMMING);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            if (player != null) {
                for (int j = 0; j < Player.NO_REGISTERS; j++) {
                    CommandCardField field = player.getProgramField(j);
                    field.setCard(null);
                    field.setVisible(true);
                }
                for (int j = 0; j < Player.NO_CARDS; j++) {
                    CommandCardField field = player.getCardField(j);
                    field.setCard(generateRandomCommandCard());
                    field.setVisible(true);
                }
            }
        }
    }

    /**
     * Generates a random command card from the available command types.
     *
     * @return a randomly generated command card
     */
    private CommandCard generateRandomCommandCard() {
        Command[] commands = Command.values();
        int random = (int) (Math.random() * commands.length);
        return new CommandCard(commands[random]);
    }

    /**
     * Ends the programming phase and starts the activation phase.
     * All program fields are first hidden, and then the first register is made
     * visible so that execution can begin. The current player and step counter
     * are reset to the first player and first register.
     */
    public void finishProgrammingPhase() {
        makeProgramFieldsInvisible();
        makeProgramFieldsVisible(0);
        board.setPhase(Phase.ACTIVATION);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);
    }

    /**
     * Makes the specified program register visible for all players.
     * If the register index is outside the valid range, nothing happens.
     *
     * @param register the index of the program register to reveal
     */
    private void makeProgramFieldsVisible(int register) {
        if (register >= 0 && register < Player.NO_REGISTERS) {
            for (int i = 0; i < board.getPlayersNumber(); i++) {
                Player player = board.getPlayer(i);
                CommandCardField field = player.getProgramField(register);
                field.setVisible(true);
            }
        }
    }

    /**
     * Hides all program registers for all players.
     * This is used when changing between programming and activation states.
     */
    private void makeProgramFieldsInvisible() {
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            for (int j = 0; j < Player.NO_REGISTERS; j++) {
                CommandCardField field = player.getProgramField(j);
                field.setVisible(false);
            }
        }
    }

    /**
     * Executes all remaining programmed commands automatically.
     * Step mode is disabled, and execution continues until the activation phase
     * ends or the game enters a state that pauses execution.
     */
    public void executePrograms() {
        board.setStepMode(false);
        continuePrograms();
    }

    /**
     * Executes exactly one step of the current activation sequence.
     * Step mode is enabled so that only a single step is processed before
     * execution pauses again.
     */
    public void executeStep() {
        board.setStepMode(true);
        continuePrograms();
    }

    /**
     * Continues program execution while the game remains in activation phase
     * and step mode is disabled.
     * This method repeatedly executes individual steps until execution should stop.
     */
    private void continuePrograms() {
        do {
            executeNextStep();
        } while (board.getPhase() == Phase.ACTIVATION && !board.isStepMode());
    }

    /**
     * Executes the next activation step in the current round.
     * If the current command card is interactive, execution pauses by switching
     * to {@link Phase#PLAYER_INTERACTION}. Otherwise, the command is executed
     * and control advances to the next player or next register.
     *
     * If the final register has been completed for all players, the game returns
     * to the programming phase for the next round.
     */
    private void executeNextStep() {
        if (board.getPhase() == Phase.FINISHED) {
            return;
        }

        Player currentPlayer = board.getCurrentPlayer();
        if (board.getPhase() == Phase.ACTIVATION && currentPlayer != null) {
            int step = board.getStep();
            if (step >= 0 && step < Player.NO_REGISTERS) {
                CommandCard card = currentPlayer.getProgramField(step).getCard();
                if (card != null) {
                    Command command = card.command;
                    if (command.isInteractive()) {
                        board.setPhase(Phase.PLAYER_INTERACTION);
                        return; // pause — wait for player to choose
                    }
                    executeCommand(currentPlayer, command);
                }
                int nextPlayerNumber = board.getPlayerNumber(currentPlayer) + 1;
                if (nextPlayerNumber < board.getPlayersNumber()) {
                    board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
                } else {
                    executeFieldActions();
                    step++;
                    if (step < Player.NO_REGISTERS) {
                        makeProgramFieldsVisible(step);
                        board.setStep(step);
                        board.setCurrentPlayer(board.getPlayer(0));
                    } else {
                        startProgrammingPhase();
                    }
                }
            } else {
                return;
            }
        } else {
            return;
        }
    }

    /**
     * Executes the chosen option of an interactive command card and resumes
     * the normal execution flow. This method is called when a player selects
     * their desired action during the {@link Phase#PLAYER_INTERACTION} phase.
     * After executing the chosen command, the game advances to the next player
     * or executes field actions if all players have completed the current
     * register. If the game is not in step mode, the execution loop resumes
     * automatically.
     *
     * @param option the {@link Command} option chosen by the current player
     */
    public void executeCommandOptionAndContinue(@NotNull Command option) {
        Player currentPlayer = board.getCurrentPlayer();
        board.setPhase(Phase.ACTIVATION); // resume normal flow
        executeCommand(currentPlayer, option);

        // Now advance to next player (same logic as in executeNextStep)
        int nextPlayerNumber = board.getPlayerNumber(currentPlayer) + 1;
        if (nextPlayerNumber < board.getPlayersNumber()) {
            board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
        } else {
            executeFieldActions();
            int step = board.getStep() + 1;
            if (step < Player.NO_REGISTERS) {
                makeProgramFieldsVisible(step);
                board.setStep(step);
                board.setCurrentPlayer(board.getPlayer(0));
            } else {
                startProgrammingPhase();
            }
        }

        if (!board.isStepMode()) {
            continuePrograms();
        }
    }

    /**
     * Executes all field actions for each player after a register has been completed.
     * Any actions associated with the space a player is currently standing on are
     * executed in the order they are stored on that space.
     *
     * Players without a current space are ignored.
     *
     * @author Mikkel Hjelm
     */
    private void executeFieldActions() {
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            Space space = player.getSpace();

            if (space != null) {
                for (FieldAction action : space.getActions()) {
                    action.doAction(this, space);
                }
            }
        }
    }

    /**
     * Executes a single command for the given player.
     * Supported commands include movement, rotation, and turning around.
     * Interactive commands are not handled here directly, but through the
     * activation flow that pauses execution and waits for player input.
     *
     * If the player is null, belongs to another board, or the command is null,
     * the method does nothing.
     *
     * @param player the player whose command should be executed
     * @param command the command to execute
     */
    private void executeCommand(@NotNull Player player, Command command) {
        if (player != null && player.board == board && command != null) {
            // XXX This is a very simplistic way of dealing with some basic cards and
            //     their execution. This should eventually be done in a more elegant way
            //     (this concerns the way cards are modelled as well as the way they are executed).

            switch (command) {
                case FORWARD:
                    if (canMove(player, player.getHeading())) {
                        moveForward(player, player.getHeading());
                    }
                    break;
                case RIGHT:
                    this.turnRight(player);
                    break;
                case LEFT:
                    this.turnLeft(player);
                    break;
                case FAST_FORWARD:
                    for (int i = 0; i < 2; i++) {
                        if (canMove(player, player.getHeading())) {
                            moveForward(player, player.getHeading());
                        } else {
                            break;
                        }
                    }
                    break;
                case BACK:
                    this.moveBack(player, player.getHeading());
                    break;
                case UTURN:
                    this.uturn(player);
                    break;

                default:
                    // DO NOTHING (for now)//
            }
        }
    }

    /**
     * Checks whether the given player can move one step in the specified heading.
     * If the target space is occupied by another player, the method recursively
     * checks whether that player can also be moved in the same direction.
     *
     * Movement is not possible if the player has no space, the heading is null,
     * the next space does not exist, or a wall blocks movement.
     *
     * @param player the player that attempts to move
     * @param heading the direction of the movement
     * @return true if the player can move; false otherwise
     * @author Mikkel Hjelm
     */
    boolean canMove(@NotNull Player player, Heading heading) {
        if(player.getSpace() == null || heading == null) {
            return false;
        }

        Space next = board.getNeighbour(player.getSpace(), heading);

        if (next == null || board.getNieghborwall(player.getSpace(), heading)) {
            return false;
        }

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player other = board.getPlayer(i);
            if (other.equals(player)) {
                continue;
            }
            if (next.equals(other.getSpace())) {
                return canMove(other, heading);
            }
        }
        return true;
    }

    /**
     * Moves a player one space forward in the given heading.
     * If another player occupies the target space, that player is moved first
     * in the same direction, creating a push chain.
     *
     * If movement is blocked by a wall, the edge of the board, or an immovable
     * player chain, the method does nothing.
     *
     * @param player the player to move
     * @param heading the direction of movement
     * @author Mikkel Hjelm
     */
    public void moveForward(Player player, Heading heading) {
        if(player == null || player.getSpace() == null || heading == null) {
            return;
        }
        Space next = board.getNeighbour(player.getSpace(), heading);
        if(next == null || board.getNieghborwall(player.getSpace(), heading)) {
            return;
        }

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player other = board.getPlayer(i);
            if (other == null ||other == player) {
                continue;
            }

            if (next.equals(other.getSpace())) {
                if (!canMove(other, heading)) return;
                moveForward(other, heading);
            }
        }
        player.setSpace(next);
    }

    /**
     * Moves the player up to two spaces forward in the given heading.
     * Each step is validated separately, so movement stops early if the second
     * step is blocked.
     *
     * @param player the player to move
     * @param heading the direction of movement
     */
    public void fastForward(@NotNull Player player, Heading heading) {
        for (int i = 0; i < 2; i++) {
            if (canMove(player, heading)) {
                moveForward(player, heading);
            } else {
                break;
            }
        }
    }

    /**
     * Rotates the player 90 degrees clockwise.
     *
     * @param player the player to rotate
     */
    public void turnRight(@NotNull Player player) {
        player.setHeading(player.getHeading().next());
    }

    /**
     * Rotates the player 90 degrees counterclockwise.
     *
     * @param player the player to rotate
     */
    public void turnLeft(@NotNull Player player) {
        player.setHeading(player.getHeading().prev());
    }

    /**
     * Moves the player one space backwards relative to their current heading.
     * The player's heading is restored afterwards, so the player only changes
     * position and not orientation.
     *
     * If backward movement is blocked, the method does nothing.
     *
     * @param player the player to move backwards
     * @param heading the player's current heading
     */
    public void moveBack(@NotNull Player player, Heading heading) {
        Heading NewHeading = heading.next().next();
        if(!canMove(player, NewHeading)) return;

        uturn(player);
        moveForward(player, player.getHeading());
        uturn(player);
    }

    /**
     * Rotates the player 180 degrees.
     *
     * @param player the player to turn around
     */
    public void uturn(@NotNull Player player) {
        player.setHeading(player.getHeading().next());
        player.setHeading(player.getHeading().next());

    }

    /**
     * Ends the game and declares the given player as the current player.
     * The board phase is changed to {@link Phase#FINISHED}.
     *
     * @param player the player who has won the game
     */
    public void finishGame(@NotNull Player player) {
        board.setCurrentPlayer(player);
        board.setPhase(Phase.FINISHED);
    }

}