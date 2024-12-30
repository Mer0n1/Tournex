package io.github.capoints.objects;

import io.github.capoints.StandardCallback;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.capoints.util.Algorithm.findPath;

public class BotAI {
    private Team myTeam;
    private List<Point> points;
    private List<Team> teams;

    //система приоритетов
    /* Система оценок определяет вес каждой точки и создает локальную
    * тактику (или сценарий, не путать с сценарием ниже) защиты или атаки точки.
    * Каждый режим либо увеличивает вес точки либо оставляет как есть. Например режим обороны
    * увеличивает вес собственных точек из за чего система оценок видя приоритет решает защищать точки
    * в случае если сценарий обороны.
    */
    private Map<Point, Float> weights; //вес точек
    private final float maxWeight = 100.0f;
    private final int step = 3; //расстояние от нашей территории чтобы проверять на противника

    //система режимов
    //2 режима - атака, оборона
    //оборона - в случае потери множества точек. Атака - стандарт
    public enum ActionMode {ATTACK, DEFEND}
    private ActionMode actionMode;

    //локальная тактика
    /* 5 игроков значит всего может быть 5 локальных тактик */
    Map<Player, LocalTactic> playerTactics;
    public enum ActionType {CAPTURE, DEFEND}


    //система сценариев (ЭВРИСТИКА)


    public BotAI(Team team, List<Point> points, List<Team> teams) {
        this.myTeam = team;
        this.points = points;
        this.teams = teams;
        actionMode = ActionMode.ATTACK;

        weights = new HashMap<>();
        playerTactics = new HashMap<>();


        for (Point point : points)
            weights.put(point,0f);
        weights.put(team.getBase(), maxWeight);

    }

    /* Есть также 2 вариант создания очереди локальной тактики
    *  Очередь локальной тактики ставит на первые места тактики с
    * высоким приоритетом и удаляет выполненные тактики
    * */
    public class LocalTactic implements StandardCallback {
        public Point targetPoint; // Цель тактики
        public ActionType action;
        public Player player;
        //приоритет локальной тактики

        public LocalTactic(Point targetPoint, ActionType action, Player player) {
            this.targetPoint = targetPoint;
            this.action = action;
            this.player = player;
System.out.println("created local_tactic id:" + player.getId() + " Point id from:" + player.getCoordinate() + " To:" + targetPoint.getId());
            player.addCaptureListener(this);
        }

        @Override
        public void callback() {
            CheckAchieveTactic(player);
        }
    }

    /*
    * Если ActionType.CAPTURE то захват точки уведомляет о выполненной
    * тактики notifyFinishLocalTactic которая вызывает defineLocalTactic
    *
    * defineLocalTactic определяет новую тактику
    *
    * assignTactic - обновляет приоритеты тактик и также вызывает
    * defineLocalTactic. Может заменять текущие тактики
    *
    * Скорее всего сделаем метод который будет вызывать обновление веса а затем
    * обновление тактик каждые 3 секунды например для каждого бота
    * */

    //метод проверки достижения тактики
    private void CheckAchieveTactic(Player player) {
        LocalTactic localTactic = playerTactics.get(player);

        if (player.getCoordinate() != null && player.getCoordinate() == localTactic.targetPoint) {
            //игрок выполнил цель тактики
            player.removeListener(localTactic);
            playerTactics.remove(player);
            //assignTactic(); //TODO
        }

    }

    public void assignTactic() {
        //playerTactics.clear();//test TODO
        Set<Point> needPoints = new HashSet<>();
        List<Point> queuePoints = new ArrayList<>(); //топ 5 важнейший точек

        //находим точки которые будем учитывать
        for (Point point : myTeam.myPoints) {
            //без режима подход - все точки
            needPoints.add(point);
            needPoints.addAll(point.getConnectedPoints());

        }
System.out.println("needPoints " + needPoints.size() + " " + needPoints);
        // Сортируем точки по весу и определяем топ 5 важнейших
        queuePoints = needPoints.stream()
            .sorted((p1, p2) -> Float.compare(weights.get(p2), weights.get(p1)))
            .limit(5)
            .collect(Collectors.toList());
System.out.println("queuePoints " + queuePoints.size() + " " + queuePoints);

        // Назначаем игрокам без задания важнейшие точки
        for (Player player : myTeam.players) {
            if (!playerTactics.containsKey(player)) { //первым делом отправляем игроков без задания на топовые по важности точки
                Point bestPoint = findBestPointForPlayer(queuePoints, player);
                if (bestPoint != null) {
                    defineLocalTactic(player, bestPoint);
                    queuePoints.remove(bestPoint);
                }
            } else {
                //стоит проверить текущую тактику и обновить её, если точка потеряла приоритет
                Point currentPoint = playerTactics.get(player).targetPoint;
                if (weights.get(currentPoint) < weights.get(queuePoints.get(0))) {
                    //точка на которой игрок работает, потеряла важность, переназначаем тактику
                    Point bestPoint = findBestPointForPlayer(queuePoints, player);
                    if (bestPoint != null) {
                        defineLocalTactic(player, bestPoint);
                        queuePoints.remove(bestPoint);
                    }
                }
            }
        }

    }

    private void defineLocalTactic(Player player, Point point) {
        ActionType type = ActionType.CAPTURE;
        if (actionMode == ActionMode.DEFEND)
            type = ActionType.DEFEND;

        playerTactics.put(player, new LocalTactic(point, type, player));
        myTeam.movePlayer(player, point);
    }

    // Метод для нахождения лучшей точки для игрока. Он находит ближайшую точку для игрока из приоритетных и назначает ее игроку
    private Point findBestPointForPlayer(List<Point> points, Player player) {
        Point bestPoint = null;
        float minDistance = Float.MAX_VALUE;

        for (Point point : points) {
            if (!isPointAssigned(point) && player.getCoordinate() != point) { //Проверяем, назначена ли точка другому игроку
                float distance = findPath(player.getCoordinate(), point).size();
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPoint = point;
                }
            }
        }
        return bestPoint;
    }

    // Проверка, назначена ли точка другому игроку
    private boolean isPointAssigned(Point point) {
        return playerTactics.values().stream()
            .anyMatch(tactic -> tactic.targetPoint.equals(point));
    }

    private Player findBestPlayerForTactic(Point point) {
        Player bestPlayer = null;
        float minDistance = Float.MAX_VALUE;

        for (Player player : myTeam.players)
            if (player.getCoordinate() != null) {
                float distance = findPath(player.getCoordinate(), point).size();
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPlayer = player;
                }
            }

        return bestPlayer;
    }

    /** Обновление веса точек
     *  Обновляется каждые 3 секунды
     * */
    public void updateWeights() {
        /*
        * Доделываем систему приоритетов. На этот раз у нас будет полностью динамическое изменение
        * веса точек. Система оценок работает так что она сортирует и использует только наиболее важные точки для цели
        * Поэтому нужно пару правил. Таким образом именно система приоритетов решает куда стоит уделить внимание
        * 1. База обладает наиболее большим коэффициентом для изменения веса. Если к ней будет приближаться
        * противник то вес очень быстро подскочит.
        * 2. После захвата точек они теряют часть веса, так чтобы нейтральные точки или точки противника имели
        * больше веса. Это означает "эту точку мы захватили, а значит сейчас нам нужна больше другая точка)
        * 3. Атака на нашу точку также повышает вес этой точки, настолько что ИИ может держать там
        * игрока для защиты. Также чем ближе противник к нашей точке тем выше вес этой точки и соединяющих (кроме наших)
        * 4. Если точка захвачена нами и имеет связи с точками захваченными нами тоже то ее вес уменьшается
        * */


        //сброс
        for (Point point : points)
            weights.put(point, 0f);
        weights.put(myTeam.getBase(), 100f);

        //1 алгоритм - вес зависит от расстояния до базы
        /* Стандартный вес точки - 0. Вес расстояния это 0.2 * 100 / путь */
        for (Point point : points)
            if (point != myTeam.getBase()) {
                float weight = 0.2f * maxWeight / findPath(myTeam.getBase(), point).size();
                if (weight < 1)
                    weight = 0;
                weights.merge(point, weight, Float::sum);
            }

        //2 алгоритм - вес увеличивается если противник рядом с точкой
        /* Смотрим ближайшие 3 точки от границы и смотрим противников на них.
        Затем меняем вес наших точек чем ближе к ним противник.
        * */
        Set<Point>   dangerous_points  = new HashSet<>();
        Map<Point, Integer> step_saver = new HashMap<>(); //Dangerous point, step from us

        for (Point point : myTeam.myPoints) {
            /*проверим ближайшие точки от нашей территории с точностью шага до 3*/
            dangerous_points = new HashSet<>();
            dangerous_points.addAll(findBorderUltra(0, point, step_saver)); //2 вариант

            //повышаем вес точки так как противник недалеко
            for (Point dang_point : dangerous_points)
                weights.put(point, weights.get(point) * (1.0f + 1.0f / (1 + step_saver.get(dang_point))));
            //System.out.println("id: " + point.id + " " + weights.get(point) + " " + dangerous_points.size());
        }

        for (Point point : points) {
            // 3 алгоритм - вес увеличивается, если точка является стратегически важной (имеет много дочерних связей)
            if (point != myTeam.getBase()) {
                int connectedPointsCount = point.getConnectedPoints().size();
                if (connectedPointsCount > 0) {
                    float criticalityBonus = 1 + 0.2f * connectedPointsCount; // Чем больше дочерних точек, тем выше вес
                    weights.put(point, weights.get(point) * criticalityBonus);
                }
            }

            // 4 алгоритм - вес увеличивается для точек, принадлежащих противнику и для базы противника
            if (point.getOwner() != null && point.getOwner() != myTeam)
                weights.put(point, weights.get(point) * 1.3f);

            if (point.getOwner() != myTeam && point.isBase())
                weights.put(point, weights.get(point) * 3f);
        }

        // 5 алгоритм - вес значительно увеличивается для точек, находящихся под атакой
        for (Point point : myTeam.myPoints)
            if (point.isCapture()) {
                float attackBonus = 1.5f; // Значительное увеличение веса
                weights.put(point, weights.get(point) * attackBonus);
            }

    }

    public void updateWeights2() {

        weights.clear();

        for (Point point : points) {
            weights.put(point, 5.0f); //standard

            float proximityWeight = 1.0f;
            if (point == myTeam.getBase())
                proximityWeight = 5.0f;
            float resultWeight = calculatePointWeight(point, proximityWeight);
            /*System.out.println(resultWeight + " " + weights.get(point) + " " + weights.get(point) * resultWeight);
            System.out.println();*/

            weights.put(point, weights.get(point) * resultWeight);
        }

        //2 алгоритм - вес увеличивается если противник рядом с точкой
        for (Point point : myTeam.myPoints) {

            float proximityWeight = 1.0f;
            if (point == myTeam.getBase())
                proximityWeight = 5.0f;

            Map<Point, Integer> step_saver = new HashMap<>();
            Set<Point> dangerous_points = new HashSet<>();
            dangerous_points.addAll(findBorderUltra(0, point, step_saver));

            //повышаем вес точки так как противник недалеко
            for (Point dang_point : dangerous_points) {
                /*System.out.println("continue " + dang_point.id + " " + point.id + " " + step_saver.get(dang_point) + " " + weights.get(point)
                    + " " + (1.0f + 1.0f / (1 + step_saver.get(dang_point))) * proximityWeight + " " + weights.get(point)
                    * (1.0f + 1.0f / (1 + step_saver.get(dang_point))) * proximityWeight);
                System.out.println();*/
                weights.put(point, weights.get(point) * (1.0f + 1.0f / (1 + step_saver.get(dang_point))) * proximityWeight);
            }
        }

    }

    /** Призрачный вес - это ценность точки. У базы это значение - 5, у обычной точки - 1
     *  При критических ситуациях вес у базы подскакивает в 5 раз выше чем у обычной точки*/
    private float calculatePointWeight(Point point, float proximityWeight) {
        float standardWeight = 1.0f; //Базовый вес

        //1 алгоритм - вес зависит от расстояния до базы
        int path = findPath(myTeam.getBase(), point).size();
        if (path == 0)
            path = 1;
        float weight = 0.03f * maxWeight / path;
        if (weight < 1)
            weight = 0;
        standardWeight += weight;
//System.out.println("1 " + point.id + " " + standardWeight);
        // 3 алгоритм - вес увеличивается, если точка является стратегически важной (имеет много дочерних связей)
        if (point != myTeam.getBase()) {
            int connectedPointsCount = point.getConnectedPoints().size();
            if (connectedPointsCount > 0) {
                float criticalityBonus = 0.4f * connectedPointsCount; // Чем больше дочерних точек, тем выше вес
                standardWeight += criticalityBonus;
            }
        }
//System.out.println("2 " + point.id + " " + standardWeight);
        // 4 алгоритм - вес увеличивается для точек, принадлежащих противнику и для базы противника
        if (point.getOwner() != null && point.getOwner() != myTeam)
            standardWeight += 0.45f;

        //if (point.owner != myTeam && point.isBase) //для базы противника
        //    weights.put(point, weights.get(point) * 3f);

//System.out.println("3 " + point.id + " " + standardWeight + " " + point.owner + " " + myTeam);
        // 5 алгоритм - вес значительно увеличивается для точек, находящихся под атакой
        if (point.isCapture())
            standardWeight += 2.0f * proximityWeight;
//System.out.println("4 " + point.id + " " + standardWeight);
        // алгоритм увеличения веса для точек нейтральных граничящих (на границе)
        if (point.getOwner() != myTeam)
            for (Point point1 : point.getConnectedPoints())
                if (point1.getOwner() == myTeam)
                    standardWeight *= 1.5f;
//System.out.println("5 " + point.id + " " + standardWeight);
        //алгоритм 0 веса для заблокированных точек
        /* Либо же правим систему оценок откладываю тактику */
        if (myTeam.BlockedPoints.containsKey(point))
            standardWeight = 0;

        //алгоритмы уменьшения веса
        if (!point.isBase() && point.getOwner() == myTeam)
            // 6 алгоритм - вес уменьшается для захваченных точек
            standardWeight *= 0.5f;
        // 7 алгоритм - вес уменьшается если точка имеет много связей с захваченными точками\
        if (point.getOwner() == myTeam)
            for (Point point1 : point.getConnectedPoints())
                if (point1.getOwner() == myTeam || point1 == myTeam.getBase())
                    standardWeight *= 0.9f;
//System.out.println("6 " + point.id + " " + standardWeight);


        return standardWeight;
    }

    private Set<Point> findBorder(int num_step, Point point) {
        Set<Point> pointSet = new HashSet<>();
        if (num_step >= step)
            return pointSet;

        for (Point point1 : point.getConnectedPoints()) {
            if (point1.getOwner() != myTeam /*!myTeam.myPoints.contains(point1)*/) {

                pointSet.add(point1);
                pointSet.addAll(findBorder(num_step + 1, point1));
            }
        }
        return pointSet;
    }

    /* Delux версия. Определяет только те точки где есть противник */
    private Set<Point> findBorderUltra(int num_step, Point point, Map<Point, Integer> step_saver) {
        Set<Point> pointSet = new HashSet<>();
        if (num_step >= step)
            return pointSet;

        for (Point point1 : point.getConnectedPoints()) {
            if (point1.getOwner() != myTeam) {

                pointSet.addAll(findBorderUltra(num_step + 1, point1, step_saver));

                boolean isHereEnemy = false;
                if (point1.getPlayers().size() == 0)
                    continue;
                for (Player player : point1.getPlayers())
                    if (player.getMyTeam() != myTeam)
                        isHereEnemy = true;
                if (!isHereEnemy)
                    continue;

                pointSet.add(point1);
                step_saver.put(point1, num_step);
            }
        }
        return pointSet;
    }


    //-------------getters and setters---------------
    public Map<Point, Float> getWeights() {
        return weights;
    }

    public Team getMyTeam() {
        return myTeam;
    }
}
