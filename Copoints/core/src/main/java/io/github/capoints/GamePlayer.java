package io.github.capoints;

import io.github.capoints.objects.Team;

public class GamePlayer {
    private static Team team;

    private GamePlayer() {}

    public static void init(Team team) {
        GamePlayer.team = team;
    }

    public static Team getTeam() {
        return team;
    }

    public static void setTeam(Team team) {
        GamePlayer.team = team;
    }
}
