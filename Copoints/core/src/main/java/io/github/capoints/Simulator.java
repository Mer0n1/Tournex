package io.github.capoints;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.capoints.Server.Server;
import io.github.capoints.objects.BotAI;
import io.github.capoints.objects.Player;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;
import io.github.capoints.view.ViewObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Simulator implements ViewObject {

    private GameMap gameMap;
    private GamePlayer gamePlayer; //игрок, тобишь мы.
    private List<Point> points;
    private List<Team> teams;

    //Управление игроками
    private boolean isMovingPlayerMode;
    private Point MPModePoint;
    private Player MPModePlayer;

    private boolean isConsoleMode = true; //Консоль разработчика
    private boolean isRedactorMode;


    //Testing
    BotAI botAI;
    private List<BotAI> bots;

    //
    private SaveRedactor saveRedactor;
    private Interface anInterface;
    private DeveloperConsole dconsole;
    private Redactor redactor;

    private SaveRedactor.TheSave save; //сохранение для карты
    private Texture fon;

    public Simulator() {
        this.gameMap = GameMap.instance;
        points = new ArrayList<>();
        bots   = new ArrayList<>();
        dconsole = DeveloperConsole.getInstance();
        saveRedactor = SaveRedactor.getInstance();
        isRedactorMode = true;
        fon = new Texture("map.jpg");
        save = null;


        /*loadMap();
        GamePlayer.init(teams.get(0));
        anInterface  = new Interface(teams);
        dconsole.init(points, botAI); //TODO тест только если есть бот
        redactor = new Redactor(points);*/

        //-----------------------Controller привязки клавиш-----------------------

        Controller.getInstance().addListener(Input.Keys.K, () -> {
            isConsoleMode = !isConsoleMode;
        });

        Controller.getInstance().addListener(Input.Keys.G, () -> { //Test показ данных
            //for (Team team : teams)
            //    System.out.println("TEAM: " + team.getId() + " " + team.getTeamName() + " " + team.myPoints);

            for (Team team : teams)
                for (Player player : team.players)
                    System.out.println("TEAM: " + team.getId() + " " + player.getCoordinate() + " " + player.getId());

            for (Point point : points)
                System.out.println("Point: " + point.getId() + " " + point.getOwner() + " " + point.getPlayers());
        });
/*
        Controller.getInstance().addListener(Input.Keys.H, () -> {
            //botAI.updateWeights2();
            //botAI.assignTactic();

            for (BotAI botAI1 : bots) {
                botAI1.updateWeights2();
                botAI1.assignTactic();
            }
        });



        Controller.getInstance().addListener(Input.Keys.J, () -> { //Тест телепорт игрока
            //testTeam.movePlayer(0, points.get(5));
            //teams.get(0).players.get(0).setCoordinate(points.get(8));
            //points.get(0).addPlayer(teams.get(0).players.get(0));

            teams.get(0).movePlayer(teams.get(0).players.get(4), points.get(5));
        });
        * */


        /** Логика управления игроками */
        Controller.getInstance().setCallbackLeftDown(()->{
            if (!isMovingPlayerMode) {
                for (Point point01 : points) {
                    if (point01.getView().interactive(Controller.getInstance().getMouseX(), Controller.getInstance().getMouseY())) {
                        isMovingPlayerMode = true;

                        if (point01.getPlayers().size() != 0) {
                            MPModePoint = point01;
                            for (Player player : point01.getPlayers())
                                if (player.getMyTeam() == gamePlayer.getTeam()) //Test TODO
                                    MPModePlayer = player;
                            if (MPModePlayer == null) {
                                isMovingPlayerMode = false;
                                MPModePoint = null;
                            }

                        } else
                            isMovingPlayerMode = false;
                    }
                }
            } else {
                if (MPModePlayer != null && MPModePoint != null) {
                    for (Point point01 : points)
                        if (point01.getView().interactive(Controller.getInstance().getMouseX(), Controller.getInstance().getMouseY()))
                            if (point01 != MPModePoint)
                                MPModePlayer.getMyTeam().movePlayer(MPModePlayer, point01);

                    MPModePoint = null;
                    MPModePlayer = null;
                }
                isMovingPlayerMode = false;
            }

            if (anInterface != null)
                anInterface.setMPMode(isMovingPlayerMode);
        } );


    }

    @Override
    public void render(SpriteBatch batch) {

        batch.begin();
        batch.draw(fon, 0,0, 1200, 800);

        //ShapeRender
        if (isRedactorMode) {
            batch.end();
            redactor.render();
            batch.begin();
        }

        //Usual render
        for (Point point : points) {
            point.render(batch);
            point.getView().clearColor(); //сброс до базового колора
        }

        for (Team team : teams)
            team.render(batch);

        anInterface.render(batch);
        if (isConsoleMode)
            dconsole.render(batch);

        batch.end();
    }

    public void update() {
        if (isRedactorMode)
            redactor.interactive();


    }

    private void loadMap() {
        try {
            save = saveRedactor.loadSave("map");
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

        points = save.points;
        teams  = save.teams;
    }

    public void loadMap(SaveRedactor.TheSave save, Team myTeam) {

        points = save.points;
        teams  = save.teams;

        GamePlayer.init(myTeam);
        anInterface  = new Interface(teams);
        dconsole.init(points, botAI); //TODO тест только если есть бот
        redactor = new Redactor(points);
    }

    public void startGame() {

        gameMap.init(points, teams, new ArrayList<>()/*Collections.singletonList(botAI)*//*TODO*/);
        gameMap.startTimerScore();
        gameMap.startUpdatingBots();


        //botAI.assignTactic(); //TODO test

        /*
        botAI = new BotAI(teams.get(0), points, teams);
        BotAI botAI1 = new BotAI(teams.get(1), points, teams);
        BotAI botAI2 = new BotAI(teams.get(2), points, teams);
        BotAI botAI3 = new BotAI(teams.get(3), points, teams);
        bots.add(botAI);
        bots.add(botAI1);
        bots.add(botAI2);
        bots.add(botAI3);*/
    }


    public void loadTeams(List<Team> teams, Team myTeam) {
        GamePlayer.init(myTeam);
        this.teams = teams;
    }

    public void clear_teams() {
        for (Team team : teams)
            gameMap.removeTeam(team);
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Team> getTeams() {
        return teams;
    }

}
