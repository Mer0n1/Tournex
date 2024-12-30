package io.github.capoints;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.capoints.objects.BotAI;
import io.github.capoints.objects.Point;
import io.github.capoints.view.ViewObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeveloperConsole implements ViewObject {

    private static DeveloperConsole console;
    private List<Point> mapPoints;
    private List<BitmapFont> bitmapFonts; //index = mapPoints index
    //private Map<Point, Float> weights;

    private DeveloperConsole() {
        bitmapFonts = new ArrayList<>();
    }

    public static DeveloperConsole getInstance() {
        if (console == null) {
            console = new DeveloperConsole();
        }

        return console;
    }

    public void init(List<Point> points, BotAI bot) {
        mapPoints = points;

        for (Point point : points) {
            BitmapFont font = new BitmapFont();
            font.setColor(Color.BLACK);
            font.getData().setScale(0.9f);
            bitmapFonts.add(font);
        }

        //weights = bot.getWeights();
    }

    @Override
    public void render(SpriteBatch batch) {
        for (int j = 0; j < mapPoints.size(); j++) {
            //bitmapFonts.get(j).draw(batch, String.valueOf(weights.get(mapPoints.get(j))) + " - " + mapPoints.get(j).getId(),
            //       mapPoints.get(j).getCoord().x, mapPoints.get(j).getCoord().y + 50);
            bitmapFonts.get(j).draw(batch, String.valueOf(mapPoints.get(j).getId()),
                mapPoints.get(j).getCoord().x, mapPoints.get(j).getCoord().y + 50);
        }
    }
}
