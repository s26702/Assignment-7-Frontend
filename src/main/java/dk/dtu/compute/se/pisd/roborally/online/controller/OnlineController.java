package dk.dtu.compute.se.pisd.roborally.online.controller;

import dk.dtu.compute.se.pisd.roborally.controller.AppController;
import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.Board;
import dk.dtu.compute.se.pisd.roborally.online.model.*;
import dk.dtu.compute.se.pisd.roborally.online.view.AppDialogs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles the online part of RoboRally.
 * It keeps the signed-in user, talks to the backend, and opens the game selection UI.
 */
public class OnlineController {



    public final AppController appController;

    public final OnlineState onlineState;

    private AppDialogs appDialogs;

    /**
     * The root URL of the backend for all the REST services.
     */
    public final String ROBORALLY_BACKEND_URL = "http://localhost:8080/roborally/";

    /**
     * The RestClient that can be used throughout all functions of this OnlineController
     * to communicate with the RoboRally backend.
     */
    private RestClient restClient;

    /**
     * Creates an online controller for the running app.
     *
     * @param appController the main application controller
     */
    public OnlineController(AppController appController) {
        this.appController = appController;
        this.onlineState = new OnlineState();
        restClient = RestClient.builder().
                baseUrl(ROBORALLY_BACKEND_URL).
                build();
        this.appDialogs = new AppDialogs(this);
    }

    /**
     * Looks for a backend user with the given name and signs them in if one is found.
     *
     * @param name the user name typed by the player
     */
    public void signIn(String name) {
        // FIXME the 4 below is a bit arbitray and should be a constant defines
        //       somewhere in the code or a configuration file!
        if (name.length() >= 4) {
            try {
                List<User> users = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("user/search")
                                .queryParam("name", name)
                                .build())
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<User>>() {});

                if (users != null && !users.isEmpty()) {
                    setOnlineUser(users.get(0));
                } else {
                    setOnlineUser(null);
                }

            } catch (Exception e) {
                setOnlineUser(null);
                e.printStackTrace();
            }
        } else {
            setOnlineUser(null);
        }
    }

    /**
     * Opens the sign-in dialog when the app is ready for online actions.
     */
    public void signIn() {
        if (appController.isGameRunning()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game running");
            alert.setHeaderText("You cannot sign in while a game is running!");
            alert.showAndWait();
        } else if (gameSelectionOn) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game selection is active");
            alert.setHeaderText(
                    "You cannot sign in while a game selection " +
                    "for a signed in user is active!");
            alert.showAndWait();
        } else {
            appDialogs.signIn();
        }
    }

    /**
     * Signs out the current online user after asking for confirmation.
     */
    public void signOut() {
        if (onlineState.getSignedInUser() != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Sign out?");
            alert.setContentText(
                    "Are you sure you want to sign out, " + onlineState.getSignedInUser().getName() + "?");
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // If the user confirms, sign the user out
                setOnlineUser(null);
            }
        }
    }

    /**
     * Stores the current online user and shows a small status dialog.
     *
     * @param user the user to sign in, or {@code null} to sign out
     */
    public void setOnlineUser(User user) {
        if (!appController.isGameRunning() && !gameSelectionOn) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (user != null) {
                alert.setTitle("User logged in!");
                alert.setHeaderText("Welcome " + user.getName());
            } else {
                alert.setTitle("Logged out!");
                alert.setHeaderText("You are logged out!");
            }
            onlineState.setSignedInUser(user);
            alert.showAndWait();
        }
    }

    /**
     * Reloads the open games from the backend.
     */
    public void refreshGames() {
        try {
            String response = restClient.get().uri("game/game").retrieve().body(String.class);
            List<Game> games = readGames(response);

            onlineState.setOpenGames(games);
            // TODO Assignment 7c/7e: And at some later point, this should only
            //      return the games open for registration (not started yet).
        } catch (Exception e) {
            onlineState.setOpenGames(null);
            e.printStackTrace();
        }
    }

    private boolean gameSelectionOn = false;

    /**
     * Shows the game selection screen for the signed-in user.
     */
    public void selectGame() {
        if (appController.isGameRunning()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game running");
            alert.setHeaderText("You cannot select a game while a game is running!");
            alert.showAndWait();
        } else if (onlineState.getSignedInUser() == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Not signed in");
            alert.setHeaderText(
                    "You cannot select a game when not signed in!\n" +
                    "Sign in first!");
            alert.showAndWait();
        } else {
            refreshGames();
            gameSelectionOn = true;
            appController.roboRally.createGameSelectionView(this);
        }
    }

    /**
     * Starts the selected game locally, or closes the selection screen if no game was selected.
     *
     * @param game the game chosen by the user
     */
    public void gameSelected(Game game) {
        if (!appController.isGameRunning() /* && onlineState.getSignedInUser() != null && gameSelectionOn */) {
            appController.roboRally.createGameSelectionView(null);
            gameSelectionOn = false;

            if (game != null) {

                // TODO Assignment 7e: make sure the game is set to the active state
                //      here and in the backend, so that no new players can sign up.

                // Then show the game board and the game (with uid from backend) is then started
                startGame(game);
            }
        }
    }


    /**
     * Creates a game on the backend with the signed-in user as owner.
     *
     * @param game the game details from the dialog
     */
    public void createGame(Game game) {
        if (!appController.isGameRunning() && onlineState.getSignedInUser() != null && gameSelectionOn) {

            try {

                User signedIn = onlineState.getSignedInUser();
                if(signedIn == null) return;
                game.setOwner(signedIn);

                restClient.post().uri("game/game").body(game).retrieve().toBodilessEntity();

            } catch (Exception e) {
                e.printStackTrace();
            }

            // update the game select view (which should get the new game from the backend)
            selectGame();
        }
    }

    /**
     * Opens the dialog for creating a new online game.
     */
    public void createGame() {
        appDialogs.createNewGame();
    }



    /**
     * Adds the signed-in user to a game if there is room.
     *
     * @param game the game to join
     */
    public void joinGame(Game game) {
        try {
            Player player = new Player();
            User signedIn = onlineState.getSignedInUser();
            List<Player> players = game.getPlayers() == null ? List.of() : game.getPlayers();
            if(signedIn == null ||
                    players.size() +1 > game.getMaxPlayers()) return;

            for(Player p: players){
                if(p.getUser() != null && signedIn.getUid() == p.getUser().getUid()) return;
            }

            player.setName(signedIn.getName());
            player.setUser(signedIn);
            player.setGame(game);
            restClient.post()
                    .uri("player")
                    .body(player)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

    /**
     * Removes the signed-in user from a game.
     * Owners get a clear error because they must delete the game instead.
     *
     * @param game the game to leave
     */
    public void leaveGame(Game game) {
        try {
            User signedIn = onlineState.getSignedInUser();
            if (signedIn == null || game == null || game.getPlayers() == null) return;

            if (userOwnsGame(game)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Cannot leave game");
                alert.setHeaderText("The owner cannot leave their own game.");
                alert.setContentText("Delete the game instead if you do not want to host it.");
                alert.setGraphic(null);
                alert.showAndWait();
                return;
            }

            for (Player player : game.getPlayers()) {
                if (player.getUser() != null &&
                        player.getUser().getUid() == signedIn.getUid()) {

                    restClient.delete()
                            .uri("player/{id}", player.getUid())
                            .retrieve()
                            .toBodilessEntity();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }

    /**
     * Deletes a game from the backend.
     *
     * @param game the game to delete
     */
    public void deleteGame(Game game) {
        try {

            if (game == null) return;

            restClient.delete()
                    .uri("game/game/{id}", game.getUid())
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }




    /**
     * Checks whether the signed-in user is already listed as a player in a game.
     *
     * @param game the game to check
     * @return {@code true} if the signed-in user is in the game
     */
    public boolean userInGame(Game game) {
        User signedIn = onlineState.getSignedInUser();
        if(signedIn == null || game == null || game.getPlayers() == null) return false;


        for(Player p : game.getPlayers()){
            if (p.getUser() != null && signedIn.getUid() == p.getUser().getUid()) return true;
        }
        return false;
    }

    /**
     * Checks whether the signed-in user owns a game.
     *
     * @param game the game to check
     * @return {@code true} if the signed-in user is the owner
     */
    public boolean userOwnsGame(Game game) {
        User signedIn = onlineState.getSignedInUser();
        if(signedIn == null || game == null) return false;
        User owner = game.getOwner();

        if(owner == null) return false;


        return owner.getUid() == signedIn.getUid();
    }

    private void startGame(Game game) {
        // TODO Assignment 7e: creation of the board should eventually depend
        //      on the board provided by the Game information.
        //      And every user who had joined the game should be able to start
        //      it in their client (individually -- no interactive gameplay
        //      required for Assignment 7)!
        Board board = new Board(8,8);
        GameController gameController = new GameController(board);
        int i = 0;
        List<Player> players = game.getPlayers() == null ? List.of() : game.getPlayers();
        for (Player player: players) {
            String name = player.getName();
            if (name == null) {
                name = "Player " + (i + 1);
            }
            dk.dtu.compute.se.pisd.roborally.model.Player p =
                    new dk.dtu.compute.se.pisd.roborally.model.Player(board, appController.PLAYER_COLOURS.get(i), name);
            board.addPlayer(p);
            p.setSpace(board.getSpace(i % board.width, i));
            i++;
        }

        gameSelectionOn = false;

        gameController.startProgrammingPhase();
        appController.roboRally.createBoardView(gameController);
    }

    // TODO still somethings to do here for the real game play.
    //      - where to save the game controller (here we just forget it); it should probably be
    //        part of the online state
    //      - who is owner (controlling game logic) and communicating that to the backend
    //      - coordinating with the backend and updating the game state accordingly
    //      - sending users choices to the backend
    //      - not showing the hidden data (hand cards and not ye played program cards)
    //        for the other players in view
    //      - ...
    //      But this is not part of the course 02324 and its assignment. This assignment is just
    //      about creating a game and different users joining it (coordinated by the backend).

    private List<Game> readGames(String response) {
        List<Game> games = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return games;
        }

        JsonElement root = JsonParser.parseString(response);
        if (root == null || root.isJsonNull()) {
            return games;
        }

        readGames(root, games);
        return games;
    }

    private void readGames(JsonElement response, List<Game> games) {
        if (response.isJsonArray()) {
            for (JsonElement gameNode : response.getAsJsonArray()) {
                games.add(readGame(gameNode));
            }
            return;
        }

        if (!response.isJsonObject()) {
            return;
        }

        JsonObject object = response.getAsJsonObject();
        if (object.has("_embedded") && object.get("_embedded").isJsonObject()) {
            JsonObject embedded = object.getAsJsonObject("_embedded");
            embedded.entrySet().forEach(entry -> {
                JsonElement values = entry.getValue();
                if (values.isJsonArray()) {
                    for (JsonElement gameNode : values.getAsJsonArray()) {
                        games.add(readGame(gameNode));
                    }
                }
            });
            return;
        }

        games.add(readGame(response));
    }

    private Game readGame(JsonElement element) {
        JsonObject node = element.getAsJsonObject();
        Game game = new Game();
        game.setUid(readLong(node, "uid"));
        game.setName(readString(node, "name"));
        game.setMinPlayers(readInt(node, "minPlayers"));
        game.setMaxPlayers(readInt(node, "maxPlayers"));

        if (hasValue(node, "ownerUid")) {
            User owner = new User();
            owner.setUid(readLong(node, "ownerUid"));
            owner.setName(readString(node, "ownerName"));
            game.setOwner(owner);
        }

        List<Player> players = readPlayers(node.get("players"), game);
        if (players.isEmpty() && node.has("playerUids") && node.get("playerUids").isJsonArray()) {
            for (JsonElement playerUid : node.getAsJsonArray("playerUids")) {
                Player player = new Player();
                player.setUid(playerUid.getAsLong());
                player.setGame(game);
                players.add(player);
            }
        }
        game.setPlayers(players);

        return game;
    }

    private List<Player> readPlayers(JsonElement playersNode, Game game) {
        List<Player> players = new ArrayList<>();
        if (playersNode == null || playersNode.isJsonNull() || !playersNode.isJsonArray()) {
            return players;
        }

        for (JsonElement playerElement : playersNode.getAsJsonArray()) {
            JsonObject playerNode = playerElement.getAsJsonObject();
            Player player = new Player();
            player.setUid(readLong(playerNode, "uid"));
            player.setName(readString(playerNode, "name"));
            player.setGame(game);

            if (hasValue(playerNode, "userUid")) {
                User user = new User();
                user.setUid(readLong(playerNode, "userUid"));
                user.setName(readString(playerNode, "userName"));
                player.setUser(user);
            }

            players.add(player);
        }

        return players;
    }

    private boolean hasValue(JsonObject object, String property) {
        return object.has(property) && !object.get(property).isJsonNull();
    }

    private String readString(JsonObject object, String property) {
        return hasValue(object, property) ? object.get(property).getAsString() : null;
    }

    private int readInt(JsonObject object, String property) {
        return hasValue(object, property) ? object.get(property).getAsInt() : 0;
    }

    private long readLong(JsonObject object, String property) {
        return hasValue(object, property) ? object.get(property).getAsLong() : 0;
    }

}
