package io.github.capoints.objects;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.capoints.*;
import io.github.capoints.Server.SPoint;
import io.github.capoints.Server.STeam;
import io.github.capoints.Server.ServerControlledPoint;
import io.github.capoints.util.IdGenerator;
import io.github.capoints.view.PointView;
import io.github.capoints.view.ViewObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Point implements ViewObject, ServerControlledPoint {
    private int id; // Уникальный идентификатор точки
    private boolean isBase; // Является ли эта точка базой
    private Vector2 coord;

    private Set<Point> connectedPoints; // Список связанных точек
    private Team owner; // Владелец точки (может быть null, если точка свободная)
    private PointView view;

    private Set<Player> players;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> captureTask;

    //Переменная отвечающая за то захватывается точка в данный момент или нет
    private boolean isCapture;
    private final int timeCapture = 1;
    private Player invader;

    private final int TimerBlockPoint = 20; //20 секунд

    public Point() {
        connectedPoints = new LinkedHashSet<>();
        players = new HashSet<>();
        owner = null;

        id = IdGenerator.GenerateIdPoint();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void render(SpriteBatch batch) {
        view.render(batch);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addPlayer(Player player) {

        if (!player.getMyTeam().isInGame())
            return;

        players.add(player);

        //точка уже захвачена
        if (owner != null)
            if (isHereEnemy(player)) { //пробегаемся по игрокам и если есть игрок с другой команды то дуэль
                System.out.println("The player on the point. Duel");
                Player enemy = getEnemy(player);
                if (enemy != null) {
                    Player winner = doDuel(enemy, player);
                    Player loser = player;

                    if (winner != enemy)
                        loser = enemy;

                    loser.getMyTeam().addBlockToPoint(this, TimerBlockPoint); //блокировка точки для этой команды
                    loser.notifyAllDuelListeners(false, this);
                    winner.notifyAllDuelListeners(true, this);

                    if (loser.getMyTeam().getBase() != this) { //для базы просто удаляем игроков
                        try {
                            System.out.println("leave losers");
                            System.out.println("players: " + players + " --1--  " + winner.getMyTeam().getTeamName() + " " + winner.getId() + " " +
                                loser.getMyTeam().getTeamName() + " " + loser.getId());
                            makeLosersToLeave(winner);
                            System.out.println("players: " + players + " --2--  " + winner.getMyTeam().getTeamName() + " " + winner.getId() + " " +
                                loser.getMyTeam().getTeamName() + " " + loser.getId());
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    if (winner != enemy)
                        capture(winner); //противнику не нужно захватывать точку которая уже принадлежит ему
                    System.out.println("Winner in duel is: " + winner.getMyTeam().getTeamName());

                    return;
                }
            }

        //Точка захватывается
        if (isCapture && invader != null)
            if (invader.getMyTeam() != player.getMyTeam()) {
                System.out.println("The fight for the point");
                cancelCapture(); //сбрасываем захват
                Player winner = doDuel(invader, player);
                makeLosersToLeave(winner);
                capture(winner);

                Player loser = player;
                if (winner != invader)
                    loser = invader;

                loser.getMyTeam().addBlockToPoint(this, TimerBlockPoint);
                loser.notifyAllDuelListeners(false, this);
                winner.notifyAllDuelListeners(true, this);

                System.out.println("------- ");
                System.out.println("Winner in duel is: " + winner.getMyTeam().getTeamName());
                return;
            }


        //точка является уже захваченной командой этого игрока
        if (player.getMyTeam() != owner)
            startTimerCapturePoint(player);
        else
            player.notifyFinishedOnPoint();
    }

    private void startTimerCapturePoint(Player player) {
        if (!isCapture) {
            isCapture = true;
            invader = player;

            captureTask = scheduler.schedule(() -> {

                if (owner != null) // Если точка не нейтральная, а принадлежит противнику
                    owner.myPoints.remove(Point.this);

                /*if (!player.getMyTeam().isInGame) //если команда уже проиграла
                    return;*/

                capture(player);

            }, timeCapture, TimeUnit.SECONDS); // Установка времени захвата
        }
    }

    public void connectPoints(Point toPoint) {
        boolean p1 = connectedPoints.add(toPoint);
        if (p1) {
            boolean p2 = toPoint.connectedPoints.add(this);
            if (!p2)
                connectedPoints.remove(toPoint);
        }
    }

    public void init(PointView view) {
        this.view = view;
        coord = view.getCoord();
    }

    /** Метод для SaveRedactor для сбора обьекта из сохранения*/
    public void build(PointView view, int id) {
        this.id = id;
        this.view = view;
        coord = view.getCoord();
    }

    public void setBase(boolean base) {
        isBase = base;
    }

    private void cancelCapture() {
        if (isCapture && captureTask != null)
            captureTask.cancel(true);
        isCapture = false;
        invader   = null;
    }

    private boolean isHereEnemy(Player newPlayer) {
        for (Player player : players)
            if (player != newPlayer)
                if (player.getMyTeam() != newPlayer.getMyTeam())
                    return true;
        return false;
    }

    private Player getEnemy(Player newPlayer) {
        for (Player player : players)
            if (player != newPlayer)
                if (player.getMyTeam() != newPlayer.getMyTeam())
                    return player;
        return null;
    }

    //дуэль рандома
    /** Возвращает победителя */
    private Player doDuel(Player player1, Player player2) {
        Random rand = new Random();
        int randomInteger;
        int upperLimit = 100;
        randomInteger = rand.nextInt(upperLimit);
        return (randomInteger % 2 != 0) ? player1 : player2;
    }

    private void makeLosersToLeave(Player winner) {
        //прогоняем игроков из той команды которая проиграла
        for (Player player_ : players) {
            if (player_.getMyTeam() != winner.getMyTeam())
                player_.leavePoint();
        }
    }

    /** Игрок захвативший точку. Метод используемый для завершения захвата */
    private void capture(Player player) {
        player.notifyFinishedOnPoint();

        //если захватывается база то точки этой базы присваиваются новой команде
        if (isBase && owner != null) {
            Team pastTeam = owner;

            for (Point point : owner.myPoints) {
                System.out.println(point.id);
                point.view.setColor(player.getMyTeam().getColorTeam());
                player.getMyTeam().myPoints.add(point);
                point.owner = player.getMyTeam();
            }
            isBase = false;
            GameMap.instance.removeTeam(pastTeam);
        }

        if (owner != null)
            owner.myPoints.remove(this);

        player.getMyTeam().myPoints.add(Point.this);
        owner = player.getMyTeam();

        view.setColor(owner.getColorTeam());
        isCapture = false;
        invader = null;
    }

    public int getId() {
        return id;
    }

    public boolean isBase() {
        return isBase;
    }

    public Vector2 getCoord() {
        return coord;
    }

    public Set<Point> getConnectedPoints() {
        return connectedPoints;
    }

    public Team getOwner() {
        return owner;
    }

    public PointView getView() {
        return view;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public boolean isCapture() {
        return isCapture;
    }

    public Player getInvader() {
        return invader;
    }

    public void setCoord(Vector2 coord) {
        this.coord = coord;
    }

    public void setOwner(Team owner) {
        this.owner = owner;
    }

    @Override
    public void synchronizeSPoint(SPoint sPoint) {
        if (GameMap.instance.isInit) {
            players.clear();

            for (Team team : GameMap.instance.getTeams()) {
                if (team.getId() == sPoint.owner) {
                    owner = team;
                    view.setColor(owner.getColorTeam());
                }

                for (Integer idPlayer : sPoint.players)
                    for (Player player : team.players) {
                        if (player.getId() == idPlayer)
                            players.add(player);

                        if (player.getId() == sPoint.invader)
                            invader = player;
                    }
            }


            isCapture = sPoint.isCapture;
        }
    }

    @Override
    public void forceCapture(STeam steam) {
        Team team = steam.team;
        List<Player> myPlayers = team.players;
        Player player = null;

        for (Player player1 : players)
            for (Player player2 : myPlayers)
                if (player1 == player2) {
                    player = player1;
                    break;
                }

        if (player == null)
            return;

        capture(player);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
