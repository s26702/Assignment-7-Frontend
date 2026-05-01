package dk.dtu.compute.se.pisd.roborally.view;

import dk.dtu.compute.se.pisd.designpatterns.observer.Subject;
import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.Command;
import dk.dtu.compute.se.pisd.roborally.model.CommandCard;
import dk.dtu.compute.se.pisd.roborally.model.CommandCardField;
import dk.dtu.compute.se.pisd.roborally.model.Phase;
import dk.dtu.compute.se.pisd.roborally.model.Player;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * A tab view for a single player.
 *
 * The player view displays the player's program registers, command cards,
 * checkpoint status, and any interaction options that may be required during
 * the game.
 *
 * @author Ekkart Kindler
 */
public class PlayerView extends Tab implements ViewObserver {

    /**
     * Label displaying the number of checkpoints reached by the player.
     */
    private Label statusLabel;

    /**
     * The player associated with this view.
     */
    private Player player;

    /**
     * Root container for the entire player tab content.
     */
    private VBox top;

    /**
     * Label for the program register section.
     */
    private Label programLabel;

    /**
     * Pane containing the program register card fields.
     */
    private GridPane programPane;

    /**
     * Label for the command cards section.
     */
    private Label cardsLabel;

    /**
     * Pane containing the available command cards.
     */
    private GridPane cardsPane;

    /**
     * Visual representations of the player's program register fields.
     */
    private CardFieldView[] programCardViews;

    /**
     * Visual representations of the player's available command card fields.
     */
    private CardFieldView[] cardViews;

    /**
     * Panel containing buttons for player interaction choices.
     */
    private VBox playerInteractionPanel;

    /**
     * The game controller used for handling user actions.
     */
    private GameController gameController;

    /**
     * Creates a new player view for the given player.
     *
     * @param gameController the game controller responsible for game logic
     * @param player the player shown in this tab
     */
    public PlayerView(@NotNull GameController gameController, @NotNull Player player) {
        super(player.getName());
        this.setStyle("-fx-text-base-color: " + player.getColor() + ";");

        top = new VBox();
        this.setContent(top);

        this.gameController = gameController;
        this.player = player;

        programLabel = new Label("Program");

        programPane = new GridPane();
        programPane.setVgap(2.0);
        programPane.setHgap(2.0);
        programCardViews = new CardFieldView[Player.NO_REGISTERS];

        for (int i = 0; i < Player.NO_REGISTERS; i++) {
            CommandCardField cardField = player.getProgramField(i);
            if (cardField != null) {
                programCardViews[i] = new CardFieldView(gameController, cardField);
                programPane.add(programCardViews[i], i, 0);
            }
        }

        playerInteractionPanel = new VBox();
        playerInteractionPanel.setAlignment(Pos.CENTER_LEFT);
        playerInteractionPanel.setSpacing(3.0);

        cardsLabel = new Label("Command Cards");

        cardsPane = new GridPane();
        cardsPane.setVgap(2.0);
        cardsPane.setHgap(2.0);
        cardViews = new CardFieldView[Player.NO_CARDS];

        for (int i = 0; i < Player.NO_CARDS; i++) {
            CommandCardField cardField = player.getCardField(i);
            if (cardField != null) {
                cardViews[i] = new CardFieldView(gameController, cardField);
                cardsPane.add(cardViews[i], i, 0);
            }
        }

        top.getChildren().add(programLabel);
        top.getChildren().add(programPane);
        top.getChildren().add(cardsLabel);
        top.getChildren().add(cardsPane);

        if (player.board != null) {
            player.board.attach(this);

            statusLabel = new Label("Checkpoints: 0");
            top.getChildren().add(statusLabel);
            update(player.board);
        }
    }

    /**
     * Updates the player view when the observed board changes.
     *
     * This method updates checkpoint information, register borders, and
     * any player interaction buttons shown during the player interaction phase.
     *
     * @param subject the observed subject that triggered the update
     */
    @Override
    public void updateView(Subject subject) {
        if (subject == player.board) {

            statusLabel.setText("Checkpoints: " + player.getCheckpointsReached());

            for (int i = 0; i < Player.NO_REGISTERS; i++) {
                CardFieldView cardFieldView = programCardViews[i];
                if (cardFieldView != null) {
                    if (player.board.getPhase() == Phase.PROGRAMMING) {
                        cardFieldView.setBorder(CardFieldView.BORDER_DEFAULT);
                    } else {
                        if (i < player.board.getStep()) {
                            cardFieldView.setBorder(CardFieldView.BORDER_DONE);
                        } else if (i == player.board.getStep()) {
                            if (player.board.getCurrentPlayer() == player) {
                                cardFieldView.setBorder(CardFieldView.BORDER_ACTIVE);
                            } else if (player.board.getPlayerNumber(player.board.getCurrentPlayer()) >
                                    player.board.getPlayerNumber(player)) {
                                cardFieldView.setBorder(CardFieldView.BORDER_DONE);
                            } else {
                                cardFieldView.setBorder(CardFieldView.BORDER_READY);
                            }
                        } else {
                            cardFieldView.setBorder(CardFieldView.BORDER_DEFAULT);
                        }
                    }
                }
            }

            if (player.board.getPhase() == Phase.PLAYER_INTERACTION) {

                if (!programPane.getChildren().contains(playerInteractionPanel)) {
                    programPane.add(playerInteractionPanel, Player.NO_REGISTERS, 0);
                }

                playerInteractionPanel.getChildren().clear();

                if (player.board.getCurrentPlayer() == player) {
                    CommandCard card = player.getProgramField(player.board.getStep()).getCard();

                    if (card != null) {
                        for (Command option : card.command.getOptions()) {
                            Button optionButton = new Button(option.displayName);
                            optionButton.setOnAction(e ->
                                    gameController.executeCommandOptionAndContinue(option));
                            optionButton.setDisable(false);
                            playerInteractionPanel.getChildren().add(optionButton);
                        }
                    }
                }
            } else {
                programPane.getChildren().remove(playerInteractionPanel);
            }
        }
    }
}