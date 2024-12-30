package io.github.capoints.util;

import io.github.capoints.objects.Point;

import java.util.*;

public class Algorithm {

    public static float findAngle(float x1, float y1, float x2, float y2) {
        float deltaX = x1 - x2;
        float deltaY = y1 - y2;
        return (float)(Math.atan2(deltaY, deltaX) * 180 / Math.PI);
    }

    public static int findLongLink(float x1, float y1, float x2, float y2) {
        float dx = Math.abs(x1 - x2);
        float dy = Math.abs(y1 - y2);
        return (int) Math.sqrt(dx*dx+dy*dy);
    }

    /** АЛГОРИТМ А */
    /*Определить ближайший маршрут и расстояние */
    public static List<Point> findPath(Point start, Point goal) {
        Map<Point, Integer> gScore = new HashMap<>(); // Стоимость пути до точки
        Map<Point, Integer> fScore = new HashMap<>(); // gScore + эвристика
        Map<Point, Point> cameFrom = new HashMap<>(); // Откуда пришли

        PriorityQueue<Point> openSet = new PriorityQueue<>(Comparator.comparingInt(fScore::get));
        gScore.put(start, 0);
        fScore.put(start, heuristic(start, goal)); // эвристика
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Point current = openSet.poll();

            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }

            for (Point neighbor : current.getConnectedPoints()) {
                int tentativeGScore = gScore.getOrDefault(current, Integer.MAX_VALUE) + 1; // вес ребра
                if (tentativeGScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, gScore.get(neighbor) + heuristic(neighbor, goal));

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return null; // Путь не найден
    }

    private static int heuristic(Point a, Point b) {
        // Простая эвристика - манхэттенское расстояние
        //return (int) (Math.abs(a.coord.y - b.coord.x) + Math.abs(a.coord.y - b.coord.y));
        //евклидово расстояние
        return (int) Math.sqrt(Math.pow(a.getCoord().x - b.getCoord().x, 2)
            + Math.pow(a.getCoord().y - b.getCoord().y, 2));
    }

    private static List<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        List<Point> path = new ArrayList<>();
        while (cameFrom.containsKey(current)) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}
