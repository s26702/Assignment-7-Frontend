package dk.dtu.compute.se.pisd.roborally.online.controller;

import dk.dtu.compute.se.pisd.roborally.controller.AppController;
import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.Board;
import dk.dtu.compute.se.pisd.roborally.online.model.*;
import dk.dtu.compute.se.pisd.roborally.online.view.AppDialogs;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public OnlineController(AppController appController) {
        this.appController = appController;
        this.onlineState = new OnlineState();
        restClient = RestClient.builder().
                baseUrl(ROBORALLY_BACKEND_URL).
                build();
        this.appDialogs = new AppDialogs(this);
    }

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

    public void refreshGames() {
        try {
            JsonNode response = restClient.get().uri("game/game").retrieve().body(JsonNode.class);
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


    public void createGame(Game game) {
        if (!appController.isGameRunning() && onlineState.getSignedInUser() != null && gameSelectionOn) {

            try {

                User signedIn = onlineState.getSignedInUser();
                if(signedIn == null) return;
                game.setOwner(signedIn);

                restClient.post().uri("game/game").body(game).retrieve().body(JsonNode.class);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // update the game select view (which should get the new game from the backend)
            selectGame();
        }
    }

    public void createGame() {
        appDialogs.createNewGame();
    }



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
                    .body(Player.class);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            selectGame();
        }
    }
    // TODO Assignment 7d: delete the currently active user as a player
    //      for the given game (in the backend)
    public void leaveGame(Game game) {
        try {
            User signedIn = onlineState.getSignedInUser();
            if (signedIn == null || game == null || game.getPlayers() == null) return;

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

    // TODO Assignment 7d: delete the given game from the games
    //      in the backend
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




    public boolean userInGame(Game game) {
        User signedIn = onlineState.getSignedInUser();
        if(signedIn == null || game == null || game.getPlayers() == null) return false;


        for(Player p : game.getPlayers()){
            if (p.getUser() != null && signedIn.getUid() == p.getUser().getUid()) return true;
        }
        return false;
    }

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

    private List<Game> readGames(JsonNode response) {
        List<Game> games = new ArrayList<>();
        if (response == null || response.isNull()) {
            return games;
        }

        if (response.isArray()) {
            for (JsonNode gameNode : response) {
                games.add(readGame(gameNode));
            }
            return games;
        }

        JsonNode embedded = response.path("_embedded");
        if (embedded.isObject()) {
            embedded.fields().forEachRemaining(entry -> {
                JsonNode values = entry.getValue();
                if (values.isArray()) {
                    for (JsonNode gameNode : values) {
                        games.add(readGame(gameNode));
                    }
                }
            });
            return games;
        }

        games.add(readGame(response));
        return games;
    }

    private Game readGame(JsonNode node) {
        Game game = new Game();
        game.setUid(node.path("uid").asLong());
        game.setName(node.path("name").asText(null));
        game.setMinPlayers(node.path("minPlayers").asInt());
        game.setMaxPlayers(node.path("maxPlayers").asInt());

        if (!node.path("ownerUid").isMissingNode() && !node.path("ownerUid").isNull()) {
            User owner = new User();
            owner.setUid(node.path("ownerUid").asLong());
            owner.setName(node.path("ownerName").asText(null));
            game.setOwner(owner);
        }

        List<Player> players = readPlayers(node.path("players"), game);
        if (players.isEmpty() && !node.path("playerUids").isMissingNode()) {
            for (JsonNode playerUid : node.path("playerUids")) {
                Player player = new Player();
                player.setUid(playerUid.asLong());
                player.setGame(game);
                players.add(player);
            }
        }
        game.setPlayers(players);

        return game;
    }

    private List<Player> readPlayers(JsonNode playersNode, Game game) {
        List<Player> players = new ArrayList<>();
        if (playersNode == null || !playersNode.isArray()) {
            return players;
        }

        for (JsonNode playerNode : playersNode) {
            Player player = new Player();
            player.setUid(playerNode.path("uid").asLong());
            player.setName(playerNode.path("name").asText(null));
            player.setGame(game);

            if (!playerNode.path("userUid").isMissingNode() && !playerNode.path("userUid").isNull()) {
                User user = new User();
                user.setUid(playerNode.path("userUid").asLong());
                user.setName(playerNode.path("userName").asText(null));
                player.setUser(user);
            }

            players.add(player);
        }

        return players;
    }

}
