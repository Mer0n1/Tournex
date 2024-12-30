package io.github.capoints.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.capoints.objects.Player;
import io.github.capoints.objects.Point;

import java.util.Random;


public class PlayerView extends View {

    private Color myColor;
    private Player player;

    /*Доп координаты которые мы прибавляем к существующим */
    private Vector2 addCoord;

    public PlayerView(Point point, Player player) {
        super((int) point.getView().coord.x + 18, (int) point.getView().coord.y + 18,
            new Texture("player_point.png"));
        this.player = player;
        this.sprite.setColor(player.getMyTeam().getColorTeam());
        myColor = player.getMyTeam().getColorTeam();
        width  = 5;
        height = 5;

        addCoord = new Vector2();
        addCoord.x = randomCoord(40);
        addCoord.y = randomCoord(40);
    }

    @Override
    public void render(SpriteBatch batch) {

        if (player.getCoordinate() != null) {
            sprite.setColor(myColor);
            sprite.setX(player.getCoordinate().getView().coord.x + addCoord.x);
            sprite.setY(player.getCoordinate().getView().coord.y + addCoord.y);
            sprite.setSize(width, height);

            sprite.draw(batch);
        }
    }

    private int randomCoord(int limit) {
        Random rand = new Random();
        return rand.nextInt(limit);
    }

}
