package io.github.capoints.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.capoints.GameMap;
import io.github.capoints.Server.STeam;
import io.github.capoints.util.IdGenerator;

import java.util.*;

public class Team {
    private int id;
    public List<Player> players;
    public List<Point> myPoints;
    private Point base;

    //заблокированные точки для этой команды. Long - таймер
    public Map<Point, Long> BlockedPoints;

    private String TeamName;
    private Color colorTeam;
    public int score; //количество очков всего
    public boolean isInGame; //isAlive? База жива


    public Team(Point base, String name, Color color) {
        init(base, name, color);
        id = IdGenerator.GenerateIdTeam();

        //добавим 5 игроков.
        for (int j = 0; j < 5; j++)
            players.add(new Player(base, this));
    }

    public Team(Point base, String name, Color color, STeam sTeam) {
        init(base, name, color);
        id = sTeam.id;
        isInGame = sTeam.isInGame;

        //добавим 5 игроков.
        for (int j = 0; j < 5; j++)
            players.add(new Player(base, this, sTeam.players.get(j)));

    }

    private void init(Point base, String name, Color color) {
        players = new ArrayList<>();
        myPoints = new ArrayList<>();
        BlockedPoints = new HashMap<>();
        this.base = base;
        base.setOwner(this);
        base.setBase(true);
        myPoints.add(base);
        isInGame = true;
        TeamName = name;
        colorTeam = color;
    }

    public void render(SpriteBatch batch) {
        for (Player player : players)
            player.render(batch);
    }

    public void movePlayer(Player player, Point toPoint) {
        if (!players.contains(player)) //игрок принадлежит этой команде?
            return;

        //проверяем на заблокированные точки для нашей команды
        updateBlockingSystem();
        if (!BlockedPoints.containsKey(toPoint))
            player.moveToPoint(toPoint);
    }

    /** Обновляем блокировки чтобы удалить те таймер которых истек */
    public void updateBlockingSystem() {
        Iterator<Point> iterator = BlockedPoints.keySet().iterator();
        while (iterator.hasNext()) {
            Point point = iterator.next();
            Long value = BlockedPoints.get(point);

            if (System.currentTimeMillis() > value)
                iterator.remove();
        }
    }

    public void addBlockToPoint(Point point, int timer) { //timer - seconds
        BlockedPoints.put(point, System.currentTimeMillis() + timer * 1000L);
    }

    public boolean isThisPointBlocked(Point point) {
        return BlockedPoints.containsKey(point);
    }

    public boolean isInGame() {
        return isInGame;
    }

    public void setInGame(boolean inGame) {
        isInGame = inGame;
    }

    public void deleteTeam() {
        for (Player player : players) {
            if (player.getCoordinate() != null)
                player.getCoordinate().getPlayers().remove(player);
            player.setCoordinate(null);
        }

        base.setOwner(null);
    }

    public int getId() {
        return id;
    }

    public Point getBase() {
        return base;
    }

    public String getTeamName() {
        return TeamName;
    }

    public Color getColorTeam() {
        return colorTeam;
    }

    public void synchronizeSTeam(STeam sTeam) {
        List<Point> points = GameMap.instance.getPoints();

        BlockedPoints.clear();
        for (Integer idPoint : sTeam.BlockedPoints.keySet())
            for (Point point : points)
                if (point.getId() == idPoint) {
                    BlockedPoints.put(point, BlockedPoints.get(idPoint));
                    break;
                }

        myPoints.clear();
        for (Integer idPoint : sTeam.myPoints)
            for (Point point : points)
                if (idPoint == point.getId()) {
                    myPoints.add(point);
                    point.getView().setColor(colorTeam);
                    break;
                }

        isInGame = sTeam.isInGame;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
