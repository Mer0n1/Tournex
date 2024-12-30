package io.github.capoints.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.capoints.objects.Player;
import io.github.capoints.objects.Point;

import java.util.ArrayList;
import java.util.List;

public class PointView extends View {

    private Point point;
    private List<LinkView> linkViews;
    private Color myColor;

    public PointView(int x, int y, boolean isBase, Point myPoint) {
        super(x, y, new Texture("point.png"));

        if (isBase) {
            this.texture = new Texture("base.png");
            sprite = new Sprite(texture);
        }

        linkViews = new ArrayList<>();
        point     = myPoint;
        myColor   = Color.WHITE;
        //myColor.a = 0.4f; //TODO test
    }

    public void renderShape() {
        for (LinkView view : linkViews)
            view.renderShape();
    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);
    }

    @Override
    public void clearColor() {
        sprite.setColor(myColor);
    }

    @Override
    public void setColor(Color color) {
        myColor = new Color(color);
        myColor.a = 0.4f;
        sprite.setColor(myColor);
    }

    public List<LinkView> getLinkViews() {
        return linkViews;
    }

    public void setLinkViews(List<LinkView> linkViews) {
        this.linkViews = linkViews;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

}
