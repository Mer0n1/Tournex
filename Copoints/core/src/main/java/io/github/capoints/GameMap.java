package io.github.capoints;

import com.badlogic.gdx.Game;
import io.github.capoints.objects.BotAI;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;

import java.util.ArrayList;
import java.util.List;

public class GameMap {
    private volatile List<Point> points; // Все точки на карте
    private volatile List<Team> teams;
    public volatile List<BotAI> bots;

    private boolean isTimerScoreStarted;
    public boolean isInit;

    // Всвязи с неизменяемым instance мы не используем метод ради удобства
    public static final GameMap instance = new GameMap();

    public GameMap() {
        points = new ArrayList<>();
        teams  = new ArrayList<>();
        bots   = new ArrayList<>();
    }

    //получение очков
    /** Каждую секунду  происходит начисление командам очков в зависимости от количества точек*/
    public void startTimerScore() {
        if (!isTimerScoreStarted) {
            isTimerScoreStarted = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        for (Team team : teams)
                            team.score += team.myPoints.size() + 5;

                    }
                }
            }).start();
        }
    }

    public void startUpdatingBots() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    for (Team team : teams)
                        team.updateBlockingSystem();

                    for (BotAI botAI : bots) {
                        botAI.updateWeights2();
                        //botAI.assignTactic(); //TODO
                    }
                }
            }
        }).start();

    }

    /** Проигравшая команда. Метод удаления команды */
    public void removeTeam(Team team) {
        if (team != null) {
            team.deleteTeam();
            team.setInGame(false);
            bots.remove(bots.stream().filter(x->x.getMyTeam() == team).findAny());
            teams.remove(team);
        }
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public boolean isIsTimerScoreStarted() {
        return isTimerScoreStarted;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public void init(List<Point> points, List<Team> teams, List<BotAI> bots) {
        this.points = points;
        this.teams  = teams;
        this.bots   = bots;
        isInit         = true;
    }
}
