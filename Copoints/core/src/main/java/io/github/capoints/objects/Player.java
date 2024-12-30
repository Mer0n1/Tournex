package io.github.capoints.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.capoints.*;
import io.github.capoints.Server.SPlayer;
import io.github.capoints.util.Algorithm;
import io.github.capoints.util.IdGenerator;
import io.github.capoints.view.PlayerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Player {
    private int id;
    private volatile Point coordinate;
    private volatile boolean isMoving;
    private Team myTeam;
    private PlayerView view;

    private List<Point> queuePoints;
    private List<StandardCallback> listenersMovedPoint;
    private List<DuelCallback> listenersDuelResults;

    private ScheduledExecutorService scheduler;

    public Player(Point beginPoint, Team myTeam) {
        init(beginPoint, myTeam);
        id = IdGenerator.GenerateIdPlayer();
    }

    public Player(Point beginPoint, Team myTeam, int id) {
        init(beginPoint, myTeam);
        this.id = id;
    }

    private void init(Point beginPoint, Team myTeam) {
        this.myTeam = myTeam;
        coordinate = beginPoint;
        isMoving = false;
        queuePoints = new ArrayList<>();
        listenersMovedPoint = new ArrayList<>();
        listenersDuelResults = new ArrayList<>();

        coordinate.addPlayer(this);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        view = new PlayerView(coordinate, this);
    }

    public void render(SpriteBatch batch) {
        view.render(batch);
    }

    /*Устанавливается таймер перемещения а после срабатывает переход на точку */
    public void setTimerToPoint(Point toPoint) {
        if (!isMoving) {
            int long_points = Algorithm.findLongLink(coordinate.getView().getCoord().x,
                coordinate.getView().getCoord().y, toPoint.getView().getCoord().x,
                toPoint.getView().getCoord().y);
            //System.out.println("started walk " + toPoint + " " + long_points);
            isMoving = true;
            coordinate.removePlayer(Player.this);
            coordinate = null;

            scheduler.schedule(() -> {
                isMoving = false;
                //меняем точку
                coordinate = toPoint;
                //добавляем игрока в точку
                toPoint.addPlayer(Player.this);
                //удаляем из очереди
                queuePoints.remove(toPoint);

                //System.out.println("End walk ");
            }, 2/*long_points/5*/, TimeUnit.SECONDS); //TODO long_points seconds
        }
    }

    public void leavePoint() {
        System.out.println("pr0 leavepoints: " + coordinate.getId() + " " + coordinate.getConnectedPoints());
        for (Point point : coordinate.getConnectedPoints()) {
            System.out.println("pr1 leavepoints: " + point.getId() + " " + (point.getOwner() == myTeam) + " " +
                myTeam.myPoints + " " + myTeam.getTeamName() + " " + point.getOwner());
            if (point.getOwner() == myTeam) {
                System.out.println("pr2 leavepoints: " + coordinate.getId() + " " + point);
                moveToPoint(point);
                return;
            }
        }

        //на случай если точки нашей не будет телепортируем игрока на базу
        myTeam.getBase().getPlayers().add(this);
        coordinate = myTeam.getBase();
    }

    //findNewTarget - например когда путь заблокирован и нужно выбрать новую точку до куда идти
    public void moveToPoint(Point toPoint) {
        if (coordinate != toPoint && coordinate != null) {
            //Проверка что это соседняя точка
            boolean isExists = false;
            for (Point point : coordinate.getConnectedPoints())
                if (point == toPoint)
                    isExists = true;

            if (!isExists) //если это не соседняя точка
                queuePoints = Algorithm.findPath(coordinate, toPoint);
            else
                queuePoints.add(toPoint);

            if (queuePoints.contains(coordinate))
                queuePoints.remove(coordinate);

            setTimerToPoint(queuePoints.get(0));
        }
    }

    private void notifyAllStandardListeners() {
        for (StandardCallback callback : listenersMovedPoint)
            callback.callback();
    }

    public void notifyAllDuelListeners(boolean isWin, Point point) {
        for (DuelCallback callback : listenersDuelResults)
            callback.callback(isWin, point.getId());
    }

    public void notifyFinishedOnPoint() {
        notifyAllStandardListeners();

        if (queuePoints.size() > 0)
            setTimerToPoint(queuePoints.get(0));
    }

    public void addCaptureListener(StandardCallback callback) {
        listenersMovedPoint.add(callback);
    }
    public void addDuelListener(DuelCallback callback) {listenersDuelResults.add(callback); }

    public void removeListener(StandardCallback callback) {
        listenersMovedPoint.remove(callback);
    }

    public Point getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Point coordinate) {
        this.coordinate = coordinate;
    }

    public int getId() {
        return id;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public Team getMyTeam() {
        return myTeam;
    }

    public PlayerView getView() {
        return view;
    }

    public void synchronizeSPlayer(SPlayer sPlayer) {
        this.isMoving = sPlayer.isMoving;

        for (Point point : GameMap.instance.getPoints())
            if (point.getId() == sPlayer.coordinate) {
                this.coordinate = point;
                point.getPlayers().add(this);
                break;
            } else  if (sPlayer.coordinate == -1) {
                point.getPlayers().remove(this);
                coordinate = null;
            }

    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    /*@Override
    public void initPlayerForServer() {
        addListener(new StandardCallback() { //TODO
            @Override
            public void callback() {
                if (coordinate != null) {

                    boolean myWin = GamePlayer.getTeam().getId() == myTeam.getId();
                    Server.getInstance().protocol_competition(coordinate.getId(), myWin);
                }
            }
        });
    }*/
}
