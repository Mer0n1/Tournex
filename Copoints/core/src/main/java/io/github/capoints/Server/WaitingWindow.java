package io.github.capoints.Server;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import io.github.capoints.*;
import io.github.capoints.objects.Player;
import io.github.capoints.objects.Team;
import io.github.capoints.util.ColorUtil;
import io.github.capoints.view.ViewObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaitingWindow {
    private static WaitingWindow instance;
    private volatile Simulator simulator;
    private volatile SaveRedactor.TheSave save;
    private boolean isActive;

    private List<Team> boot_teams;
    private Team myTeam;

    private Texture main_window_texture;
    private Texture icon_profile_texture;

    private Sprite window;
    private BitmapFont text;
    private List<Player_Icon_Interface> players;

    private WaitingWindow() {
        main_window_texture = new Texture("WaitingWindow.png");
        icon_profile_texture = new Texture("icon_profile.png");
        text = new BitmapFont();
        window = new Sprite(main_window_texture);

        players = new ArrayList<>();
        boot_teams = new ArrayList<>();

        /** Запуск игры */
        Controller.getInstance().addListener(Input.Keys.SHIFT_RIGHT, () -> {
            if (myTeam != null && boot_teams != null) {
                Server.getInstance().protocol_start_game();
                startGame();
            }
        });
    }

    public static WaitingWindow getInstance() {
        if (instance == null)
            instance = new WaitingWindow();

        return instance;
    }

    public void init(Simulator simulator, SaveRedactor.TheSave theSave) {
        this.simulator = simulator;
        save = theSave;
        //simulator.clear_teams(); //очищаем команды ботов чтобы на их месте загрузить серверные команды
    }

    public void initMyTeam(STeam sTeam) {

        Gdx.app.postRunnable(() -> {
            for (Team team : save.teams)
                GameMap.instance.removeTeam(team);

            Team myTeam = sTeam.convertToTeam(save.points/*simulator.getPoints()*/);

            for (Player player : myTeam.players)
                player.addDuelListener(new DuelCallback() {
                    @Override
                    public void callback(boolean isWin, int point) {
                        Server.getInstance().protocol_competition(point, isWin);
                    }
                });

            if (myTeam != null) {
                GamePlayer.init(myTeam);
                players.add(new Player_Icon_Interface(myTeam.getColorTeam()));

                this.myTeam = myTeam;
                boot_teams.add(myTeam);
            }
        });
    }

    public void render(SpriteBatch batch) {

        batch.begin();
        window.draw(batch);
        text.draw(batch, "Press shift to start the game...", 900, 50);

        for (int j = 0; j < players.size(); j++) {
            players.get(j).setCoord(100, 100 * ( j + 1 ));
            players.get(j).render(batch);
        }

        batch.end();
    }


    public void addUser(STeam sTeam) {
        Gdx.app.postRunnable(() -> {
            Team team = sTeam.convertToTeam(save.points/*simulator.getPoints()*/);
            sTeam.team = team;

            if (team != null) {
                players.add(new Player_Icon_Interface(team.getColorTeam()));
                boot_teams.add(team);
            }
        });
    }

    public void startGame() {
        Gdx.app.postRunnable(() -> {

            if (myTeam != null && boot_teams != null) {
                isActive = false;
                //simulator.loadTeams(boot_teams, myTeam);
                SaveRedactor.TheSave newSave = new SaveRedactor.TheSave();
                newSave.points = save.points;
                newSave.teams = boot_teams;
                simulator.loadMap(newSave, myTeam);
                simulator.startGame();
            }
        });
    }

    private class Player_Icon_Interface implements ViewObject {
        private Sprite icon;
        private BitmapFont text;
        private String name;

        private Vector2 coord;

        public Player_Icon_Interface(Color color) {
            icon = new Sprite(icon_profile_texture);
            text = new BitmapFont();
            text.setColor(color);
            icon.setColor(color);
            name = new String("Team " + ColorUtil.colorToString(color));
            coord = new Vector2();
        }

        @Override
        public void render(SpriteBatch batch) {
            icon.setX(coord.x);
            icon.setY(coord.y);

            icon.draw(batch);
            text.draw(batch, name, coord.x + 60, coord.y + 30);
        }

        public void setCoord(int x, int y) {
            coord.x = x;
            coord.y = y;
        }

        public Vector2 getCoord() {
            return coord;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void dispose() {
        main_window_texture.dispose();
        icon_profile_texture.dispose();
    }
}
