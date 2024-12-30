package io.github.capoints;

import com.badlogic.gdx.math.Vector2;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;
import io.github.capoints.view.LinkView;
import io.github.capoints.view.PointView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static io.github.capoints.util.ColorUtil.StringToColor;

public class SaveRedactor {

    List<PointSave> pointSaves;
    List<TeamSave> teamSaves;

    private static SaveRedactor instance;

    private SaveRedactor() {
        pointSaves   = new ArrayList<>();
        teamSaves     = new ArrayList<>();
    }

    public static SaveRedactor getInstance() {
        if (instance == null)
            instance = new SaveRedactor();

        return instance;
    }

    public void saveAll(List<Team> teams, List<io.github.capoints.objects.Point> points) throws ParserConfigurationException, TransformerException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element rootElement = doc.createElement("map");
        doc.appendChild(rootElement);

        Element teamListElements = doc.createElement("teams");
        rootElement.appendChild(teamListElements);

        for (Team team : teams) {
            Element teamElement = doc.createElement("team");
            teamElement.setAttribute("name",    team.getTeamName());
            teamElement.setAttribute("color",   team.getColorTeam().toString());
            teamElement.setAttribute("base",    String.valueOf(team.getBase().getId()));
            teamElement.setAttribute("points",  team.myPoints.toString());
            teamListElements.appendChild(teamElement);
        }

        for (io.github.capoints.objects.Point point : points) {
            Element pointElement = doc.createElement("point");
            pointElement.setAttribute("id",   String.valueOf(point.getId()));
            pointElement.setAttribute("base", String.valueOf(point.isBase()));
            pointElement.setAttribute("connectPoints", point.getConnectedPoints().toString());
            pointElement.setAttribute("x", String.valueOf(point.getView().getCoord().x));
            pointElement.setAttribute("y", String.valueOf(point.getView().getCoord().y));

            int idOwner = -1;
            if (point.getOwner() != null)
                idOwner = point.getOwner().getId();
            pointElement.setAttribute("owner", String.valueOf(idOwner));
            rootElement.appendChild(pointElement);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);

        StreamResult result = new StreamResult(new File("map.xml"));
        transformer.transform(source, result);

        System.out.println("File is created succesfully");
    }

    public TheSave loadSave(String filename) throws ParserConfigurationException, IOException, SAXException {
        File inputFile = new File(filename + ".xml");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        Element rootElement = doc.getDocumentElement();
        NodeList teamList = doc.getElementsByTagName("team");

        for (int i = 0; i < teamList.getLength(); i++) {
            Node teamNode = teamList.item(i);

            if (teamNode.getNodeType() == Node.ELEMENT_NODE) {
                Element teamElement = (Element) teamNode;

                String teamName = teamElement.getAttribute("name");
                String teamColor = teamElement.getAttribute("color");
                int teamBase = Integer.parseInt(teamElement.getAttribute("base"));

                teamSaves.add(new TeamSave(teamName, teamColor, teamBase));
            }
        }

        NodeList pointList = doc.getElementsByTagName("point");

        for (int i = 0; i < pointList.getLength(); i++) {
            Node pointNode = pointList.item(i);

            if (pointNode.getNodeType() == Node.ELEMENT_NODE) {
                Element pointElement = (Element) pointNode;

                Vector2 coord = new Vector2(Float.valueOf(pointElement.getAttribute("x")),
                    Float.valueOf(pointElement.getAttribute("y")));

                int pointId = Integer.parseInt(pointElement.getAttribute("id"));
                boolean isBase = Boolean.parseBoolean(pointElement.getAttribute("base"));
                int owner = Integer.parseInt(pointElement.getAttribute("owner"));
                String connectedPoints = pointElement.getAttribute("connectPoints");
                List<Integer> connectedPointIds = parseIds(connectedPoints);

                pointSaves.add(new PointSave(connectedPointIds, pointId, isBase, owner, coord));

            }
        }

        return buildData();
    }

    private static List<Integer> parseIds(String idsString) {
        List<Integer> ids = new ArrayList<>();

        // Убираем квадратные скобки, если они есть
        idsString = idsString.replaceAll("[\\[\\]]", "");

        // Если строка не пуста, разделяем её по запятым
        if (!idsString.trim().isEmpty()) {
            String[] idArray = idsString.split(",");
            for (String idStr : idArray) {
                ids.add(Integer.parseInt(idStr.trim()));
            }
        }

        return ids;
    }

    public TheSave buildData() {

        TheSave theSave = new TheSave();
        List<io.github.capoints.objects.Point> points = new ArrayList<>();
        List<Team> teams = new ArrayList<>();

        for (PointSave pointSave : pointSaves) {
            io.github.capoints.objects.Point point = new io.github.capoints.objects.Point();

            PointView pointView = new PointView((int) pointSave.coord.x,
                (int) pointSave.coord.y, pointSave.isBase, point);

            point.build(pointView, pointSave.pointId);
            points.add(point);
        }

        //добавление связей
        for (io.github.capoints.objects.Point point : points) {
            PointSave pointSave = pointSaves.stream().filter(x->x.pointId==point.getId()).findAny().orElse(null);

            if (pointSave == null)
                continue;

            for (int j = 0 ; j < pointSave.connectedPointIds.size();j++) {
                int id = pointSave.connectedPointIds.get(j);
                io.github.capoints.objects.Point connectPoint = points.stream().filter(x->x.getId()==id).findAny().orElse(null);
                point.connectPoints(connectPoint);

                LinkView linkView1 = new LinkView(point, connectPoint);
                point.getView().getLinkViews().add(linkView1);
            }

        }

        //создание команд
        for (TeamSave teamSave : teamSaves) {
            io.github.capoints.objects.Point base = points.stream().filter(x->x.getId() == teamSave.teamBase).findAny().orElse(null);

            if (base != null) {
                Team team = new Team(base, teamSave.teamName, StringToColor(teamSave.teamColor));
                teams.add(team);
            }
        }

        //игроков загружать не будем
        theSave.teams  = teams;
        theSave.points = points;
        theSave.pointSaves = pointSaves;
        theSave.teamSaves  = teamSaves;
        return theSave;
    }

    public static class TheSave {
        public List<Point> points;
        public List<Team> teams;

        List<PointSave> pointSaves;
        List<TeamSave> teamSaves;

        public TheSave() {
            points = new ArrayList<>();
            teams  = new ArrayList<>();
        }
    }

    public class PointSave {
        List<Integer> connectedPointIds;
        int pointId;
        boolean isBase;
        int owner;
        Vector2 coord;

        public PointSave(List<Integer> connectedPointIds, int pointId,
                         boolean isBase, int owner, Vector2 coord) {
            this.connectedPointIds = connectedPointIds;
            this.pointId = pointId;
            this.isBase = isBase;
            this.owner = owner;
            this.coord = coord;
        }
    }


    public class TeamSave {
        String teamName;
        String teamColor;
        int teamBase;

        public TeamSave(String teamName, String teamColor, int teamBase) {
            this.teamName = teamName;
            this.teamColor = teamColor;
            this.teamBase = teamBase;
        }
    }


}
