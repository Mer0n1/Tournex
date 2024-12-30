package org.example;

import java.awt.*;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class STeam implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int totalSize = 128;

    public int id;
    public int base;
    public int score;
    public String TeamName;
    public String colorTeam;
    public boolean isInGame;

    public Map<Integer, Long> BlockedPoints;
    public List<Integer> players;
    public List<Integer> myPoints;

    public List<SPlayer> sPlayers;

    public STeam(int id, int base, int score, String teamName, String colorTeam,
                 boolean isInGame, Map<Integer, Long> BlockedPoints,
                 List<Integer> players, List<Integer> myPoints) {
        this.id = id;
        this.base = base;
        this.score = score;
        TeamName = teamName;
        this.colorTeam = colorTeam;
        this.isInGame = isInGame;
        this.BlockedPoints = BlockedPoints;
        this.players = players;
        this.myPoints = myPoints;
        sPlayers = new ArrayList<>();
    }

    public byte[] toByteArray() {
        // Определяем количество байтов для строк и списков
        byte[] nameBytes  = TeamName.getBytes(StandardCharsets.UTF_8);
        byte[] colorBytes = colorTeam.getBytes(StandardCharsets.UTF_8);


        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        // Запись полей
        buffer.putInt(id);
        buffer.putInt(base);
        buffer.putInt(score);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(colorBytes.length);
        buffer.put(colorBytes);
        buffer.put((byte) (isInGame ? 1 : 0));

        // Сериализация  BlockedPoints
        buffer.putInt(BlockedPoints.size());
        for (Map.Entry<Integer, Long> entry : BlockedPoints.entrySet()) {
            buffer.putInt(entry.getKey());
            buffer.putLong(entry.getValue());
        }

        // Сериализация списка players
        buffer.putInt(players.size());
        for (Integer player : players) {
            buffer.putInt(player);
        }

        // Сериализация списка myPoints
        buffer.putInt(myPoints.size());
        for (Integer point : myPoints) {
            buffer.putInt(point);
        }

        return buffer.array();
    }

    public static STeam fromByteArray(byte[] array, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(array, offset, totalSize);

        // Чтение полей
        int id = buffer.getInt();
        int base = buffer.getInt();
        int score = buffer.getInt();

        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String teamName = new String(nameBytes, StandardCharsets.UTF_8);

        int colorLength = buffer.getInt();
        byte[] colorBytes = new byte[colorLength];
        buffer.get(colorBytes);
        String colorTeam = new String(colorBytes, StandardCharsets.UTF_8);

        boolean isInGame = buffer.get() == 1;

        // Десериализация списка BlockedPoints
        int blockedPointsSize = buffer.getInt();
        Map<Integer, Long> blockedPoints = new HashMap<>();
        for (int i = 0; i < blockedPointsSize; i++) {
            int key = buffer.getInt();
            long value = buffer.getLong();
            blockedPoints.put(key, value);
        }

        // Десериализация списка players
        int playersSize = buffer.getInt();
        List<Integer> players = new ArrayList<>();
        for (int i = 0; i < playersSize; i++) {
            players.add(buffer.getInt());
        }

        // Десериализация списка myPoints
        int myPointsSize = buffer.getInt();
        List<Integer> myPoints = new ArrayList<>();
        for (int i = 0; i < myPointsSize; i++) {
            myPoints.add(buffer.getInt());
        }

        return new STeam(id, base, score, teamName, colorTeam, isInGame, blockedPoints, players, myPoints);
    }

    @Override
    public String toString() {
        return "STeam{" +
                "id=" + id +
                ", base=" + base +
                ", score=" + score +
                ", TeamName='" + TeamName + '\'' +
                ", colorTeam='" + colorTeam + '\'' +
                ", isInGame=" + isInGame +
                ", BlockedPoints=" + BlockedPoints +
                ", players=" + players +
                ", myPoints=" + myPoints +
                ", sPlayers=" + sPlayers +
                '}';
    }
}
