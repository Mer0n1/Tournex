package io.github.capoints;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;
import io.github.capoints.view.LinkView;
import io.github.capoints.view.PointView;

import java.util.List;

public class Redactor {
    private SaveRedactor saveRedactor;
    private List<Point> points;

    //Создание связи
    private boolean isConnectionMode; //режим создания связи
    private LinkView linkView;
    private Point linkPoint;

    public Redactor(List<Point> points) {
        saveRedactor = SaveRedactor.getInstance();
        this.points = points;


        Controller.getInstance().addListener(Input.Keys.EQUALS, () -> {
            Point newPoint = new Point();
            PointView newView  = new PointView(Controller.getInstance().getMouseX(),
                Controller.getInstance().getMouseY(), false, newPoint);
            newPoint.init(newView);
            points.add(newPoint);
        });

        /*Создать базу */
        Controller.getInstance().addListener(Input.Keys.NUM_0, () -> {
            Point newPoint = new Point();
            newPoint.setBase(true);
            PointView newView  = new PointView(Controller.getInstance().getMouseX(),
                Controller.getInstance().getMouseY(), true, newPoint);
            newPoint.init(newView);
            points.add(newPoint);

            Team team = new Team(newPoint, "GenerateName", Color.BLUE); //TODO генерировать
            GameMap.instance.getTeams().add(team);
        });


        /** Сохранение */
        /*Controller.getInstance().addListener(Input.Keys.ENTER, () -> {
            try {
                saveRedactor.saveAll(GameMap.getTeams(), points);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });*/
    }

    public void render() {
        if (linkView != null)
            linkView.renderShape();
        for (Point point : points)
            point.getView().renderShape();
    }


    public void interactive() {

        for (Point point : points) {
            if (point.getView().interactive(Controller.getInstance().getMouseX(), Controller.getInstance().getMouseY())) {
                point.getView().makeHighlighted();

                //Режим перемещения точки
                if (Controller.getInstance().isTouchMIDDLEDown())
                    point.getView().setCoord(new Vector2(Controller.getInstance().getMouseX() - point.getView().getWidth() / 2,
                        Controller.getInstance().getMouseY() - point.getView().getHeight() / 2));

                //Режим создания связи
                if (Controller.getInstance().isTouchRIGHTDown()) {
                    if (!isConnectionMode) {
                        isConnectionMode = true;

                        linkPoint = point;
                        linkView = new LinkView(point, new Vector2());
                    }
                } else if (isConnectionMode) {
                    //System.out.println(linkPoint + " " + linkView + " " + isConnectionMode); //TODO баг с созданием связей (не везде создаются)

                    //тут отпускаем клавишу там где мы навели, тобишь на другую точку, чтобы зафиксировать связь между точками
                    isConnectionMode = false;

                    if (linkPoint != point) {
                        linkPoint.getView().getLinkViews().add(linkView);
                        linkPoint.connectPoints(point);
                        linkView.setEndPoint(point);
                        linkView = null;
                    }
                }

            } else {
                //Режим создания связи
                if (isConnectionMode && Controller.getInstance().isTouchRIGHTDown()) { //тут алгоритмика изменения размеров link
                    linkView.getEndCoord().set(Controller.getInstance().getMouseX(), Controller.getInstance().getMouseY());
                } /*else if (!Controller.getInstance().isTouchRIGHTDown() && point == linkPoint) {
                isConnectionMode = false;
                linkView = null;
                linkPoint = null;
            }*/

            }

        }
    }
}
