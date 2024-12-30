package io.github.capoints.util;

public class IdGenerator {

    private static int static_id_point = 0;
    private static int static_id_player = 0;
    private static int static_id_team = 0;

    public static int GenerateIdPoint() {
        return static_id_point++;
    }

    public static int GenerateIdTeam() {
        return static_id_team++;
    }

    public static int GenerateIdPlayer() {
        return static_id_player++;
    }
}
