package io.github.capoints.util;

import com.badlogic.gdx.graphics.Color;

public class ColorUtil {

    public static String colorToString(Color color) {
        if (color.equals(Color.BLACK)) return "Black";
        if (color.equals(Color.WHITE)) return "White";
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.YELLOW)) return "Yellow";
        if (color.equals(Color.CYAN)) return "Cyan";
        if (color.equals(Color.MAGENTA)) return "Magenta";

        return "Unknown Color";
    }

    public static Color StringToColor(String str) {
        if (Color.BLACK.toString().equals(str)) return Color.BLACK;
        if (Color.RED.toString().equals(str)) return Color.RED;
        if (Color.GREEN.toString().equals(str)) return Color.GREEN;
        if (Color.BLUE.toString().equals(str)) return Color.BLUE;
        if (Color.YELLOW.toString().equals(str)) return Color.YELLOW;
        if (Color.CYAN.toString().equals(str)) return Color.CYAN;
        if (Color.MAGENTA.toString().equals(str)) return Color.MAGENTA;

        return Color.WHITE;
    }

}
