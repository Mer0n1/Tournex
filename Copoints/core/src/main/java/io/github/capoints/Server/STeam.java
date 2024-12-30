package io.github.capoints.Server;


import com.badlogic.gdx.graphics.Color;
import io.github.capoints.GameMap;
import io.github.capoints.GamePlayer;
import io.github.capoints.objects.Player;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;
import io.github.capoints.util.ColorUtil;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class STeam implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final Integer totalSize = 128;

    public int id;
    public int base;
    public int score;
    public String TeamName;
    public String colorTeam;
    public boolean isInGame;

    public Map<Integer, Long> BlockedPoints;
    public List<Integer> players;
    public List<Integer> myPoints;

    public Team team; //

    public STeam(int id, int base, int score, String teamName, String colorTeam,
                 boolean isInGame, Map<Integer, Long> blockedPoints,
                 List<Integer> players, List<Integer> myPoints) {
        this.id = id;
        this.base = base;
        this.score = score;
        TeamName = teamName;
        this.colorTeam = colorTeam;
        this.isInGame = isInGame;
        BlockedPoints = blockedPoints;
        this.players = players;
        this.myPoints = myPoints;
    }

    public STeam(Team team) {
        id        = team.getId();
        base      = Integer.valueOf(team.getBase().toString());
        score     = team.score;
        TeamName  = team.getTeamName();
        colorTeam = team.getColorTeam().toString();
        isInGame  = team.isInGame;

        players = new ArrayList<>();
        for (Player player : team.players)
            players.add(player.getId());

        myPoints = new ArrayList<>();
        for (Point point : team.myPoints)
            myPoints.add(point.getId());

        BlockedPoints = new HashMap<>();
        Iterator<Point> iterator = team.BlockedPoints.keySet().iterator();
        while (iterator.hasNext()) {
            Point point = iterator.next();
            Long value = BlockedPoints.get(point);

            BlockedPoints.put(point.getId(), value);
        }

        this.team = team;
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
        for (Map.Entry<Point, Long> entry : team.BlockedPoints.entrySet()) {
            buffer.putInt(entry.getKey().getId());
            buffer.putLong(entry.getValue());
        }

        // Сериализация списка players
        buffer.putInt(players.size());
        for (Integer player : players)
            buffer.putInt(player);

        // Сериализация списка myPoints
        buffer.putInt(myPoints.size());
        for (Integer point : myPoints)
            buffer.putInt(point);

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

    public Team convertToTeam(List<Point> points) {
        Point base_point = null;

        for (Point point : points) {
            if (base == point.getId()) {
                base_point = point;
                break;
            }
        }

        if (base_point == null)
            return null;

        Color color = ColorUtil.StringToColor(colorTeam);
        String name = TeamName;


        return new Team(base_point, name, color, this); //TODO баг, генерация айди игроков и тим отличается от серверной data
    }

    public void mergeSTeam(STeam sTeam) {
        score = sTeam.score;
        isInGame = sTeam.isInGame;

        this.BlockedPoints.clear();
        BlockedPoints.putAll(sTeam.BlockedPoints);

        this.myPoints.clear();
        myPoints.addAll(sTeam.myPoints);
    }

    public void updateMergeTeam() {

        if (GameMap.instance.getPoints().size() != 0) {
            team.synchronizeSTeam(this);
        }
    }

    @Override
    public String toString() {
        return "STeam{" +
            "id=" + id +
            ", TeamName='" + TeamName + '\'' +
            ", players=" + players +
            ", myPoints=" + myPoints +
            '}';
    }
}

