package dk.dtu.compute.se.pisd.roborally.view;

import dk.dtu.compute.se.pisd.designpatterns.observer.Subject;
import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.Board;
import dk.dtu.compute.se.pisd.roborally.model.Phase;
import dk.dtu.compute.se.pisd.roborally.model.Player;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * A view showing all players in separate tabs.
 *
 * This class also contains the shared game control buttons for finishing
 * programming, executing all programs, and executing the current register.
 *
 * The currently active player tab is selected automatically based on the
 * game state.
 *
 * @author Ekkart Kindler
 */
public class PlayersView extends BorderPane implements ViewObserver {

    /**
     * The board associated with this view.
     */
    private Board board;

    /**
     * The tab pane containing one tab per player.
     */
    private TabPane tabPane;

    /**
     * The individual player tabs.
     */
    private PlayerView[] playerViews;

    /**
     * Panel containing the shared control buttons.
     */
    private HBox buttonPanel;

    /**
     * Button for ending the programming phase.
     */
    private Button finishButton;

    /**
     * Button for executing all programmed commands.
     */
    private Button executeButton;

    /**
     * Button for executing only the current register.
     */
    private Button stepButton;

    /**
     * Creates a new players view for the given game controller.
     *
     * The view creates one tab per player and adds a shared button panel
     * below the tabs.
     *
     * @param gameController the game controller used to trigger game actions
     */
    public PlayersView(GameController gameController) {
        board = gameController.board;

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        playerViews = new PlayerView[board.getPlayersNumber()];
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            playerViews[i] = new PlayerView(gameController, board.getPlayer(i));
            tabPane.getTabs().add(playerViews[i]);
        }

        finishButton = new Button("Finish Programming");
        finishButton.setOnAction(e -> gameController.finishProgrammingPhase());

        executeButton = new Button("Execute Program");
        executeButton.setOnAction(e -> gameController.executePrograms());

        stepButton = new Button("Execute Current Register");
        stepButton.setOnAction(e -> gameController.executeStep());

        buttonPanel = new HBox(finishButton, executeButton, stepButton);
        buttonPanel.setAlignment(Pos.CENTER);
        buttonPanel.setSpacing(10.0);

        this.setCenter(tabPane);
        this.setBottom(buttonPanel);

        board.attach(this);
        update(board);
    }

    /**
     * Updates the players view whenever the observed board changes.
     *
     * The currently active player tab is selected automatically, and the
     * shared control buttons are enabled or disabled depending on the
     * current game phase.
     *
     * @param subject the observed subject that triggered the update
     */
    @Override
    public void updateView(Subject subject) {
        if (subject == board) {
            Player current = board.getCurrentPlayer();
            tabPane.getSelectionModel().select(board.getPlayerNumber(current));

            switch (board.getPhase()) {
                case INITIALISATION:
                    finishButton.setDisable(true);
                    executeButton.setDisable(false);
                    stepButton.setDisable(true);
                    break;

                case PROGRAMMING:
                    finishButton.setDisable(false);
                    executeButton.setDisable(true);
                    stepButton.setDisable(true);
                    break;

                case ACTIVATION:
                    finishButton.setDisable(true);
                    executeButton.setDisable(false);
                    stepButton.setDisable(false);
                    break;

                default:
                    finishButton.setDisable(true);
                    executeButton.setDisable(true);
                    stepButton.setDisable(true);
            }

            if (board.getPhase() == Phase.PLAYER_INTERACTION) {
                finishButton.setDisable(true);
                executeButton.setDisable(true);
                stepButton.setDisable(true);
            }
        }
    }
}