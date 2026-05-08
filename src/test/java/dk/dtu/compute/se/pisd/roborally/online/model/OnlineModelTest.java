package dk.dtu.compute.se.pisd.roborally.online.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineModelTest {

    @Test
    void gameDefaultsNullStateToSignupAndStoresRelationships() {
        Game game = new Game();
        User owner = user(1, "Owner");
        Player player = player(2, "Owner", owner, game);

        game.setUid(10);
        game.setName("Hosted");
        game.setMinPlayers(1);
        game.setMaxPlayers(4);
        game.setState(null);
        game.setOwner(owner);
        game.setPlayers(List.of(player));

        assertEquals("SIGNUP", game.getState());
        assertSame(owner, game.getOwner());
        assertEquals(List.of(player), game.getPlayers());
        assertTrue(game.toString().contains("Hosted"));
    }

    @Test
    void playerUserAndOnlineStateStoreValues() {
        Game game = new Game();
        game.setName("Hosted");
        User user = user(7, "Lucas");
        Player player = player(3, "Lucas", user, game);
        OnlineState state = new OnlineState();

        state.setSignedInUser(user);
        state.setOpenGames(List.of(game));

        assertSame(user, player.getUser());
        assertSame(game, player.getGame());
        assertEquals("Lucas", user.getName());
        assertSame(user, state.getSignedInUser());
        assertEquals(List.of(game), state.getOpenGames());
        assertTrue(player.toString().contains("Hosted"));
        assertTrue(user.toString().contains("Lucas"));
    }

    private User user(long uid, String name) {
        User user = new User();
        user.setUid(uid);
        user.setName(name);
        return user;
    }

    private Player player(long uid, String name, User user, Game game) {
        Player player = new Player();
        player.setUid(uid);
        player.setName(name);
        player.setUser(user);
        player.setGame(game);
        return player;
    }
}
