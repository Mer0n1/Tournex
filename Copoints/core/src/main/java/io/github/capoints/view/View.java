package io.github.capoints.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.capoints.view.ViewObject;


public class View implements ViewObject {
    protected Texture texture;
    protected Sprite sprite;
    protected Vector2 coord;
    protected int width = 40, height = 40;

    public View(int x, int y, Texture texture) {
        coord = new Vector2(x,y);
        this.texture = texture;
        sprite = new Sprite(texture);
    }

    @Override
    public void render(SpriteBatch batch) {
        sprite.setX(coord.x);
        sprite.setY(coord.y);
        sprite.setSize(width, height);

        sprite.draw(batch);
    }

    public void setColor(Color color)  {
        sprite.setColor(color);
    }

    public void makeHighlighted() {
        sprite.setColor(Color.BLUE);
    }

    public void clearColor() {
        sprite.setColor(Color.WHITE);
    }

    public boolean interactive(int x, int y) {
        if (x > coord.x && x < coord.x + width &&
            y > coord.y && y < coord.y + height)
            return true;
        else
            return false;
    }

    public void addCoord(int x, int y) {
        coord.x += x;
        coord.y += y;
    }

    public Vector2 getCoord() {
        return coord;
    }

    public void setCoord(Vector2 coord) {
        this.coord = coord;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

}
