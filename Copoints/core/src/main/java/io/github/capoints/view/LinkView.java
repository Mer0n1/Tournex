package io.github.capoints.view;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.capoints.objects.Point;

public class LinkView {
    private ShapeRenderer shapeRenderer;
    private Vector2 endCoord;
    private Point endPoint;
    private Point beginPoint;

    public LinkView(Point beginPoint, Vector2 endCoord) {
        this.beginPoint = beginPoint;
        this.endCoord   = endCoord;
        shapeRenderer = new ShapeRenderer();
        endPoint = null;
    }

    public LinkView(Point beginPoint, Point endPoint) {
        this.beginPoint = beginPoint;
        this.endPoint   = endPoint;
        shapeRenderer = new ShapeRenderer();
        endCoord = new Vector2();
    }

    public void renderShape() {
        if (endPoint != null) {

            endCoord.x = endPoint.getView().getCoord().x + endPoint.getView().getWidth() / 2;
            endCoord.y = endPoint.getView().getCoord().y + endPoint.getView().getHeight() / 2;
        }


        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.line(beginPoint.getView().getCoord().x + beginPoint.getView().getWidth() / 2,
            beginPoint.getView().getCoord().y + beginPoint.getView().getHeight() / 2,
            endCoord.x, endCoord.y);
        shapeRenderer.end();
    }

    public void setEndPoint(Point point) {
        endPoint = point;
    }

    public Vector2 getEndCoord() {
        return endCoord;
    }

    public Point getEndPoint() {
        return endPoint;
    }


}
