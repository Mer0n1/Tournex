package org.example;

import java.util.ArrayList;
import java.util.List;

/** Общая карта с расположением всех точек.
 *  Эти данные будут скидываться игрокам на первых этапах подключения и загрузки */
public class Map {
    private List<SPoint> pointList;
    private List<STeam> sTeams;

    //Загруженные команды. Их мы распределяем по игрокам, после чего обновляем sTeams
    private List<STeam> boot_teams;

    private static Map instance;
    private boolean isReady;

    private Map() {
        pointList  = new ArrayList<>();
        sTeams     = new ArrayList<>();
        boot_teams = new ArrayList<>();
        isReady = false;
    }

    public static Map getInstance() {
        if (instance == null)
            instance = new Map();

        return instance;
    }

    public void init(List<SPoint> points) {
        pointList = points;
        isReady = true;
    }

    public void mergeTeam(STeam sTeam) {
        if (sTeam != null) {
            boolean isExists = false;
            STeam currentTeam = null;

            for (int j = 0; j < sTeams.size(); j++)
                if (sTeams.get(j).id == sTeam.id) {
                    currentTeam = sTeams.get(j);
                    isExists = true;
                    break;
                }

            if (isExists) {
                currentTeam.BlockedPoints = sTeam.BlockedPoints;
                currentTeam.colorTeam = sTeam.colorTeam;
                currentTeam.id = sTeam.id;
                currentTeam.base = sTeam.base;
                currentTeam.TeamName = sTeam.TeamName;
                currentTeam.isInGame = sTeam.isInGame;
                currentTeam.myPoints = sTeam.myPoints;
                currentTeam.score = sTeam.score;
            } else
                sTeams.add(sTeam);
        }
    }

    public void mergePlayer(SPlayer sPlayer) {
        if (sPlayer != null) {
            STeam myTeam = null;
            int idTeam = sPlayer.myTeam;

            for (int j = 0; j < sTeams.size(); j++)
                if (sTeams.get(j).id == idTeam) {
                    myTeam = sTeams.get(j);
                    break;
                }

            if (myTeam == null)
                return;

            for (int j = 0; j < myTeam.sPlayers.size(); j++)
                if (myTeam.sPlayers.get(j).id == sPlayer.id) {
                    myTeam.sPlayers.remove(j);
                    break;
                }

            myTeam.sPlayers.add(sPlayer);
        }
    }

    public void mergePoint(SPoint sPoint) {
        if (sPoint != null) {
            boolean isExists = false;
            SPoint currentPoint = null;

            for (int j = 0; j < pointList.size(); j++)
                if (pointList.get(j).id == sPoint.id) {
                    currentPoint = pointList.get(j);
                    isExists = true;
                    break;
                }

            if (isExists) {
                currentPoint.connectedPoints = sPoint.connectedPoints;
                currentPoint.players = sPoint.players;
                currentPoint.id = sPoint.id;
                currentPoint.coord = sPoint.coord;
                currentPoint.invader = sPoint.invader;
                currentPoint.isBase = sPoint.isBase;
                currentPoint.isCapture = sPoint.isCapture;
                currentPoint.owner = sPoint.owner;
            } else
                pointList.add(sPoint);
        }
    }

    public STeam getTeam(int id) {
        return sTeams.stream().filter(x->x.id==id).findAny().orElse(null);
    }

    public void loadBoot_Teams(List<STeam> boot_teams) {
        this.boot_teams = boot_teams;
    }

    public List<SPoint> getPointList() {
        return pointList;
    }

    public List<STeam> getsTeams() {
        return sTeams;
    }

    public List<STeam> getBoot_teams() {
        return boot_teams;
    }

    public boolean isReady() {
        return isReady;
    }
}
