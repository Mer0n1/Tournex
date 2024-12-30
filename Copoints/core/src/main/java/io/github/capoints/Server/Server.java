package io.github.capoints.Server;


import com.badlogic.gdx.Game;
import io.github.capoints.*;
import io.github.capoints.objects.Player;
import io.github.capoints.objects.Point;
import io.github.capoints.objects.Team;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;


public class Server {
    private static Server instance;

    private Socket client;
    private DatagramSocket udpSocket;
    private DatagramSocket receiveSocket;
    private InetAddress addressServer;

    private BufferedWriter out;
    private BufferedReader in;

    private final String ipServer = "192.168.0.103";
    private final int delay = 200; //задержка отправки любых данных в мс
    private int UDP_port = 5557;
    private final int UDP_server_port = 5556;
    private final int TCP_port = 5555;

    /** Все сетевые актуальные игроки */
    private List<Client> clients;
    private int myId;
    private STeam myTeam;
    private SaveRedactor.TheSave save; //загруженное сохранение для сервера

    /** Переменные типа обьектов */
    private final byte STEAM_TYPE = 1;
    private final byte POINT_TYPE = 2;
    private final byte PLAYER_TYPE = 3;

    private boolean serverIsOn;
    private boolean isHost;


    private Server() {
        clients = new ArrayList<>();

        try {
            UDP_port = findAvailableUDPPort(5556, 5560);
            System.out.println("UDP " + UDP_port);

            addressServer = InetAddress.getByName(ipServer);
            udpSocket     = new DatagramSocket();
            receiveSocket = new DatagramSocket(UDP_port);

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public static Server getInstance() {
        if (instance == null)
            instance = new Server();

        return instance;
    }

    public void init(SaveRedactor.TheSave save) {
        this.save = save;
    }

    /** Создаем TCP подключение и регистрируемся на сервере.
     *  Метод запускает также udp */
    public void connect() throws IOException {
        client = new Socket(ipServer, TCP_port);

        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        //PrintWriter out_ = new PrintWriter(client.getOutputStream(), true);

        StringBuilder req1 = new StringBuilder();
        int character;
        while ((character = in.read()) != -1) {
            req1.append((char) character);

            if (!in.ready())
                break;
        }

        myId = Integer.valueOf(req1.toString());
        serverIsOn = true;

        protocol_send_port();
        sync_my_avatar_with_server();
        listener();
    }


    /** Прослушивание udp датаграмм.**/
    public void listener() {

        /** Прием UDP датаграмм других клиентов */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] receiveData = new byte[1025];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        receiveSocket.receive(receivePacket);
                        byte typeId = receivePacket.getData()[0];

                        if (typeId == STEAM_TYPE) {
                            List<STeam> sTeams = new ArrayList<>();

                            int size = receivePacket.getLength();
                            for (int j = 0; j < size / STeam.totalSize; j++) {
                                STeam sTeam = STeam.fromByteArray(receiveData, 1 + STeam.totalSize * j);
                                if (myTeam != null && myTeam.id != sTeam.id)
                                    sTeams.add(sTeam);
                            }

                            for (STeam sTeam : sTeams) {
                                if (myTeam != null && sTeam.id != myTeam.id) //test блоки нужны только для нашей команды
                                    sTeam.BlockedPoints.clear();

                                for (Client client_ : clients)
                                    if (client_.getsTeam().id == sTeam.id) {
                                        client_.getsTeam().mergeSTeam(sTeam);
                                        client_.getsTeam().updateMergeTeam(); //обновляем основную версию Team
                                        break;
                                    }
                            }
                        }

                        if (typeId == PLAYER_TYPE) {
                            List<SPlayer> sPlayers = new ArrayList<>();

                            int size = receivePacket.getLength();
                            for (int j = 0; j < size / SPlayer.totalSize; j++) {
                                SPlayer sPlayer = SPlayer.fromByteArray(receiveData, 1 + SPlayer.totalSize * j);
                                if (myTeam != null && myTeam.id != sPlayer.myTeam)
                                    sPlayers.add(sPlayer);
                            }

                            for (SPlayer sPlayer : sPlayers)
                                sPlayer.updateMergePlayer();
                        }

                        if (typeId == POINT_TYPE) {
                            List<SPoint> sPoints = new ArrayList<>();

                            int size = receivePacket.getLength();
                            for (int j = 0; j < size / SPoint.totalSize; j++) {
                                SPoint sPoint = SPoint.fromByteArray(receiveData, 1 + SPoint.totalSize * j);
                                if (myTeam != null && myTeam.id != sPoint.owner)
                                    sPoints.add(sPoint);
                            }

                            for (SPoint sPoint : sPoints)
                                sPoint.updateMergePoint();
                        }

                    } catch (IOException e) {}
                }
            }
        }).start();


        //TCP Listener
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (in.ready()) {

                            String request;
                            StringBuilder req1 = new StringBuilder();
                            int character;
                            while ((character = in.read()) != -1) {
                                req1.append((char) character);

                                if (!in.ready())
                                    break;
                            }
                            request = req1.toString();
System.out.println("get " + request);

                            JSONObject jsonObject = (JSONObject) JSONValue.parse(request);
                            if (jsonObject == null) continue;

                            String protocol = jsonObject.get("Protocol").toString();
                            System.out.println("Key: " + protocol);

                            if (protocol.equals("makeHost")) {

                                protocol_send_map(save.points);
                                protocol_load_avatars(save.teams);
                                isHost = true;
                            }

                            //протокол добавления нового игрока (синхронизация)
                            if (protocol.equals("AddPlayer")) {
                                //добавление игроков возможно только в режиме ожидания игроков
                                if (WaitingWindow.getInstance().isActive()) {

                                    //преобразуем строку в байты
                                    String valueStr = jsonObject.get("Value").toString();
                                    String idClient = jsonObject.get("idClient").toString();
                                    byte[] bytes = Base64.getUrlDecoder().decode(valueStr);

                                    STeam sTeam = STeam.fromByteArray(bytes, 0);

                                    clients.add(new Client(sTeam, Integer.valueOf(idClient)));
                                    WaitingWindow.getInstance().addUser(sTeam);

                                    System.out.println("Player added " + idClient);
                                }
                            }

                            if (protocol.equals("AssigningAvatar")) {

                                String valueStr = jsonObject.get("Value").toString();
                                byte[] bytes = Base64.getUrlDecoder().decode(valueStr);

                                STeam sTeam = STeam.fromByteArray(bytes, 0);
                                WaitingWindow.getInstance().initMyTeam(sTeam);
                                myTeam = sTeam;
                                myTeam.team = GamePlayer.getTeam();

                                System.out.println("Protocol AssigningAvatar is completed " + sTeam.toString());
                            }

                            if (protocol.equals("start_game")) {
                                WaitingWindow.getInstance().startGame();
                                System.out.println("Protocol start game");
                            }

                            if (protocol.equals("Competition")) {
                                String valueStr = jsonObject.get("Result").toString();
                                int pointId = Integer.parseInt(jsonObject.get("Point").toString()); //id point
                                System.out.println("PROTOCOL: Competition " + valueStr);

                                if (valueStr.equals("win")) {
                                    List<Point> points = GameMap.instance.getPoints();
                                    points.stream().filter(x -> x.getId() == pointId).findAny().ifPresent(point -> point.forceCapture(myTeam));
                                }

                                if (valueStr.equals("lose")) {
                                    List<Player> players = GamePlayer.getTeam().players;
                                    players.stream().filter(x -> x.getCoordinate().getId() == pointId).findAny().ifPresent(Player::leavePoint);
                                }
                            }

                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    /** UDP пересылка игровых данных */
    public void sync_my_avatar_with_server() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {

                        Team myTeamT = GamePlayer.getTeam();

                        if (myTeamT != null) {

                            //Team
                            STeam myTeam = new STeam(myTeamT);

                            ByteBuffer buffer = ByteBuffer.allocate(STeam.totalSize + 1);
                            buffer.put(STEAM_TYPE);
                            buffer.put(myTeam.toByteArray());
                            byte[] bytes = buffer.array();

                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, addressServer, UDP_server_port);
                            udpSocket.send(packet);

                            //Players
                            List<Player> playerList = myTeamT.players;
                            List<SPlayer> playerList1 = new ArrayList<>();

                            for (Player player : playerList)
                                playerList1.add(new SPlayer(player));

                            ByteBuffer bufferPlayers = ByteBuffer.allocate(SPlayer.totalSize * playerList1.size() + 1);
                            bufferPlayers.put(PLAYER_TYPE);
                            for (int i = 0; i < 5; i++)
                                bufferPlayers.put(playerList1.get(i).toByteArray());
                            byte[] bytes1 = bufferPlayers.array();

                            DatagramPacket packet1 = new DatagramPacket(bytes1, bytes1.length, addressServer, UDP_server_port);
                            udpSocket.send(packet1);
                        }

                        //Map
                        //отправляем точки на которых находятся наши игроки
                        if (GamePlayer.getTeam() != null) {
                            Team team = GamePlayer.getTeam();
                            Set<Point> i_points = new HashSet<>(); //уникальность точек

                            /*for (Player player : team.players) в1 - важные точки это те где находятся наши игроки
                                if (player.getCoordinate() != null)
                                    i_points.add(player.getCoordinate());*/

                            i_points.addAll(team.myPoints); //в2 - важные точки все которые нам принадлежат

                            ByteBuffer bufferPoints = ByteBuffer.allocate(1 + SPoint.totalSize * i_points.size());
                            bufferPoints.put(POINT_TYPE);

                            for (Point point : i_points)
                                bufferPoints.put(new SPoint(point).toByteArray());

                            byte[] bytes_points = bufferPoints.array();
                            DatagramPacket packet = new DatagramPacket(bytes_points, bytes_points.length, addressServer, UDP_server_port);
                            udpSocket.send(packet);
                        }

                        Thread.sleep(delay);
                    } catch (Exception e) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }

                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void protocol_send_map(List<Point> points) {

        ByteBuffer buffer = ByteBuffer.allocate(points.size() * STeam.totalSize);
        for (Point point : points) {
            SPoint sPoint = new SPoint(point);
            buffer.put(sPoint.toByteArray());
        }
        //протокол загрузки карты
        byte[] bytes = buffer.array();
        String map = Base64.getUrlEncoder().encodeToString(bytes);

        tcp_write("{\"Protocol\":\"Map\",\"Value\":\"" + map + "\"}");
    }

    public void protocol_load_avatars(List<Team> teams) {
        new Thread(new Runnable() { //выносим в отдельный поток для асинхронности конкретно этого протокола
            @Override
            public void run() {

                ByteBuffer buffer = ByteBuffer.allocate(teams.size() * STeam.totalSize);
                for (Team team : teams) {
                    team.isInGame = true;
                    STeam sTeam = new STeam(team);
                    buffer.put(sTeam.toByteArray());
                }

                byte[] bytes = buffer.array();
                String result = Base64.getUrlEncoder().encodeToString(bytes);

                tcp_write("{\"Protocol\":\"LoadAvatars\",\"Value\":\"" + result + "\"}");
            }
        }).start();


    }

    private void protocol_send_port() {
        tcp_write("{\"Protocol\":\"info\",\"udp_port\":\"" + UDP_port + "\"}");
    }

    public void protocol_start_game() {
        tcp_write("{\"Protocol\":\"start_game\"}");
    }

    public void protocol_competition(int idPoint, boolean myWin) {

        String idTeamOpponent = null;
        A: for (Point point : GameMap.instance.getPoints())
            if (point.getId() == idPoint)
                for (Player player : point.getPlayers())
                    if (player.getMyTeam().getId() != myTeam.id) {
                        idTeamOpponent = String.valueOf(player.getMyTeam().getId());
                        break A;
                    }
        System.out.println("protocol_competition " + idPoint + " " + myWin + " " + idTeamOpponent);
        if (idTeamOpponent == null)
            return;

        String result = "lose";
        if (myWin)
            result = "win";

        String request = "{\"Protocol\":\"Competition\",\"Result\":\"" +
            result + "\",\"Point\":\"" + idPoint + "\",\"Team\":\"" + idTeamOpponent + "\"}";

        tcp_write(request);
    }

    public void tcp_write(String request) {
        try {
            out.write(request);
            out.flush();

            Thread.sleep(delay);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private int findAvailableUDPPort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                return port;
            } catch (IOException ignored) {
            }
        }
        throw new RuntimeException("Нет доступных UDP портов в диапазоне " + startPort + " - " + endPort);
    }

}
