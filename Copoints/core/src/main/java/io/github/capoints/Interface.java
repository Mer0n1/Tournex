package io.github.capoints;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;
import io.github.capoints.view.ViewObject;

import java.util.ArrayList;
import java.util.List;

import static io.github.capoints.util.ColorUtil.colorToString;

public class Interface implements ViewObject {

    private final List<ItemTeamInfo> itemTeamInfoList;
    private boolean isMPMode;

    private BitmapFont isMPModeFont;

    public Interface(List<Team> teams) {
        itemTeamInfoList = new ArrayList<>();

        Vector2 coord = new Vector2(940, 20);
        for (Team team : teams) {
            ItemTeamInfo item = new ItemTeamInfo(team, coord);
            itemTeamInfoList.add(item);
            coord.y += 20;
        }

        isMPMode = false;
        isMPModeFont = new BitmapFont();
        isMPModeFont.setColor(Color.RED);
    }

    @Override
    public void render(SpriteBatch batch) {
        for (ItemTeamInfo item : itemTeamInfoList)
            item.render(batch);

        isMPModeFont.draw(batch, "MPMode", 1130, 800);
    }

    public void setMPMode(boolean MPMode) {
        isMPMode = MPMode;
        if (isMPMode)
            isMPModeFont.setColor(Color.GREEN);
        else
            isMPModeFont.setColor(Color.RED);
    }

    class ItemTeamInfo implements ViewObject {
        private Team team;
        private BitmapFont font;
        private Vector2 coord;

        private List<Sprite> crosses;
        private Sprite cross;
        private Texture texture;

        public ItemTeamInfo(Team team, Vector2 coord) {
            this.team = team;
            this.coord = new Vector2(coord);
            font = new BitmapFont();
            font.setColor(Color.BLACK);

            texture = new Texture("cross.png");
            cross   = new Sprite(texture);
        }

        @Override
        public void render(SpriteBatch batch) {
            if (GamePlayer.getTeam() != null) {
                font.draw(batch, GamePlayer.getTeam().getTeamName() + ": Score: " + GamePlayer.getTeam().score
                    + " Color:" + colorToString(GamePlayer.getTeam().getColorTeam()), coord.x, coord.y);

                for (Point point : GamePlayer.getTeam().BlockedPoints.keySet()) {
                    cross.setX(point.getCoord().x);
                    cross.setY(point.getCoord().y);
                    cross.draw(batch);
                }
            }
        }

        public Vector2 getCoord() {
            return coord;
        }

    }
}
