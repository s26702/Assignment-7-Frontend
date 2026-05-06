package dk.dtu.compute.se.pisd.roborally.online.model;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

/*
    See https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
    for information as to why use JsonIdentity information!

    We can either use identity or managed references (and backwards references) to avoid
    deeply copying the object tree. Anyway, we should limit how much data is serialized
    in JSON (in the worst case the complete database is following suit).
 */

/**
 * Client-side representation of an online game returned by the backend.
 */
@JsonIdentityInfo(
        scope=Game.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uid")
public class Game {

    private long uid;

    private String name;

    private int minPlayers;

    private int maxPlayers;

    private String state = "SIGNUP";

    private List<Player> players;

    private User Owner;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getState() {
        return state == null ? "SIGNUP" : state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void setOwner(User Owner){this.Owner = Owner;}

    public User getOwner(){return this.Owner;}


    @Override
    public String toString() {
        return "Game{" +
                "uid=" + uid +
                ", name='" + name + '\'' +
                ", minPlayers=" + minPlayers +
                ", maxPlayers=" + maxPlayers +
                '}';
    }

}
