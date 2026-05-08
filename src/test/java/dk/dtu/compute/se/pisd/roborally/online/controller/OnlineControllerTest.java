package dk.dtu.compute.se.pisd.roborally.online.controller;

import dk.dtu.compute.se.pisd.roborally.RoboRally;
import dk.dtu.compute.se.pisd.roborally.controller.AppController;
import dk.dtu.compute.se.pisd.roborally.online.model.Game;
import dk.dtu.compute.se.pisd.roborally.online.model.Player;
import dk.dtu.compute.se.pisd.roborally.online.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineControllerTest {

    @Test
    void userInGameAndUserOwnsGameUseSignedInUser() {
        OnlineController controller = controllerSignedInAs(user(1, "Lucas"));
        Game game = game(10, "Hosted", 1, 4, "SIGNUP");
        game.setOwner(user(1, "Lucas"));
        game.setPlayers(List.of(player(20, user(1, "Lucas"), game)));

        assertTrue(controller.userInGame(game));
        assertTrue(controller.userOwnsGame(game));

        game.setOwner(user(2, "Other"));
        assertFalse(controller.userOwnsGame(game));
    }

    @Test
    void canStartOrPlayAllowsOwnersToStartAndPlayersToPlayActiveGames() {
        OnlineController ownerController = controllerSignedInAs(user(1, "Owner"));
        Game signupGame = game(10, "Signup", 2, 4, "SIGNUP");
        signupGame.setOwner(user(1, "Owner"));
        signupGame.setPlayers(List.of(
                player(20, user(1, "Owner"), signupGame),
                player(21, user(2, "Joined"), signupGame)));

        assertTrue(ownerController.canStartOrPlay(signupGame));

        OnlineController joinedController = controllerSignedInAs(user(2, "Joined"));
        assertFalse(joinedController.canStartOrPlay(signupGame));

        signupGame.setState("ACTIVE");
        assertTrue(joinedController.canStartOrPlay(signupGame));

        OnlineController outsiderController = controllerSignedInAs(user(3, "Outsider"));
        assertFalse(outsiderController.canStartOrPlay(signupGame));
    }

    @Test
    void canStartOrPlayRejectsSignupGameOutsidePlayerLimits() {
        OnlineController controller = controllerSignedInAs(user(1, "Owner"));
        Game notReady = game(10, "Not ready", 2, 4, "SIGNUP");
        notReady.setOwner(user(1, "Owner"));
        notReady.setPlayers(List.of(player(20, user(1, "Owner"), notReady)));

        assertFalse(controller.canStartOrPlay(notReady));
    }

    @Test
    void setBackendUrlAcceptsPlainLanAddress() {
        OnlineController controller = controllerSignedInAs(user(1, "Lucas"));

        controller.setBackendUrl("192.168.1.20:8080");

        assertEquals("http://192.168.1.20:8080/roborally/", controller.getBackendUrl());
    }

    private OnlineController controllerSignedInAs(User user) {
        OnlineController controller = new OnlineController(new AppController(new RoboRally()));
        controller.onlineState.setSignedInUser(user);
        return controller;
    }

    private Game game(long uid, String name, int min, int max, String state) {
        Game game = new Game();
        game.setUid(uid);
        game.setName(name);
        game.setMinPlayers(min);
        game.setMaxPlayers(max);
        game.setState(state);
        return game;
    }

    private User user(long uid, String name) {
        User user = new User();
        user.setUid(uid);
        user.setName(name);
        return user;
    }

    private Player player(long uid, User user, Game game) {
        Player player = new Player();
        player.setUid(uid);
        player.setName(user.getName());
        player.setUser(user);
        player.setGame(game);
        return player;
    }
}
