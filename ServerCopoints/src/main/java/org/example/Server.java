package org.example;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Server {

    //UDP
    private DatagramSocket udpSocket;
    private final int PORT_UDP = 5556;
    private final int UDP_CLIENT_PORT = 5557;

    //TCP
    private volatile ServerSocket serverSocket;
    private List<Client> clients;
    private Client host;
    private final int PORT_TCP = 5555;

    private int id_fix; //присваивает новый id
    private final int delay = 200; //задержка отправки запросов
    private final int maxSizePacket = 1025; //1024 + 1 байт (первый байт означающий для какого типа назначаются данные)
    private final int maxClients = 20;

    /** Переменные типа обьектов */
    private final byte STEAM_TYPE = 1;
    private final byte POINT_TYPE = 2;
    private final byte PLAYER_TYPE = 3;

    public Server() {
        try {
            udpSocket = new DatagramSocket(PORT_UDP);
            serverSocket = new ServerSocket(PORT_TCP);
            clients = new ArrayList<>();
            id_fix = 0;

        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /** Любое прослушивание TCP и UDP */
    public void listener() {

        /** Регистрация новых клиентов */
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
System.out.println("accepted " + socket.getInetAddress().getHostAddress());
                        //Проверим наличие неактуального зарегистрированного клиента с этого адреса
                        /*for (int j = 0; j < clients.size(); j++) //TODO testing
                            if (clients.get(j).checkEqualAddr(socket.getInetAddress()))
                                clients.remove(clients.get(j)); //удаляем чтобы после добавить актуального*/

                        if (clients.size() >= maxClients)
                            continue;

                        id_fix += 1;

                        Client client = new Client(socket, id_fix);
                        clients.add(client);

                        //отправка клиенту его клиентский id, а также подтверждение подключения
                        TCP_writeTo(client, String.valueOf(id_fix).getBytes());

                        //определение хоста
                        if (host == null) {
                            protocol_make_host(client);
                            host = client;
                        }

                        //протокол присваивание команды клиенту
                        if (Map.getInstance().getBoot_teams().size() != 0)
                            protocol_assign_avatar(client);

                    } catch (IOException e) {
                        System.err.println("Client disconnected: " + e.getMessage());
                    }
                }
            }
        }).start();

        /** Обработка TCP запросов */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    for (int j = 0; j < clients.size(); j++) {
                        try {
                            Client client = clients.get(j);
                            if (client.in.ready()) {

                                String request;
                                StringBuilder req1 = new StringBuilder();
                                int character;
                                while ((character = client.in.read()) != -1) {
                                    req1.append((char) character);

                                    if (!client.in.ready())
                                        break;
                                }
                                request = req1.toString();


                                JSONObject jsonObject = (JSONObject) JSONValue.parse(request);
                                if (jsonObject == null) continue;

                                String protocol = jsonObject.get("Protocol").toString();
                                System.out.println("Key: " + protocol);

                                if (protocol.equals("info")) { //TODO test protocol
                                    String valueStr = jsonObject.get("udp_port").toString();
                                    client.udp_port = Integer.valueOf(valueStr);
                                }


                                //протокол загрузки карты
                                if (protocol.equals("Map")) {
                                    if (client != host)
                                        continue;

                                    //преобразуем строку в байты
                                    String valueStr = jsonObject.get("Value").toString();
                                    byte[] bytes = Base64.getUrlDecoder().decode(valueStr);

                                    int size = bytes.length / SPoint.totalSize;

                                    List<SPoint> points = new ArrayList<>();
                                    for (int i = 0; i < size; i++) {
                                        SPoint sPoint = SPoint.fromByteArray(bytes, i * SPoint.totalSize);
                                        points.add(sPoint);
                                    }

                                    Map.getInstance().init(points); //инициализация по протоколу //завершение загрузки карты
                                    System.out.println("Map loading protocol completed. Map loaded " + points.size());
                                }

                                //2 этап - пересылка аватара
                                if (protocol.equals("LoadAvatars")) {
                                    if (client != host)
                                        continue;

                                    //преобразуем строку в байты
                                    String valueStr = jsonObject.get("Value").toString();
                                    byte[] bytes = Base64.getUrlDecoder().decode(valueStr);

                                    int size = bytes.length / STeam.totalSize;

                                    List<STeam> boot_teams = new ArrayList<>();
                                    for (int i = 0; i < size; i++) {
                                        STeam sTeam = STeam.fromByteArray(bytes, i * STeam.totalSize);
                                        boot_teams.add(sTeam);
                                    }

                                    //Инициализируем основные команды которые будут распределяться каждому игроку
                                    Map.getInstance().loadBoot_Teams(boot_teams);

                                    //распределяем команду игроку клиент-сервер
                                    protocol_assign_avatar(client);

                                    System.out.println("LoadAvatars retrieval protocol is complete. Avatars uploaded. " + boot_teams);
                                }


                                if (protocol.equals("start_game")) {
                                    //когда один из участников запускает игру то игра запускается у всех:
                                    for (Client client_ : clients)
                                        if (client_ != client)
                                            TCP_writeTo(client_, "{\"Protocol\":\"start_game\"}");
                                    System.out.println("Protocol start game is complete");
                                }

                                if (protocol.equals("Competition")) {
                                    String result = jsonObject.get("Result").toString();
                                    String idPoint = jsonObject.get("Point").toString();
                                    String idTeamOpponent = jsonObject.get("Team").toString();
                                    int idTeamOpponent_i = Integer.valueOf(idTeamOpponent);

                                    String newResult = (result.equals("win")) ? "lose" : "win";
                                    Client toClient = clients.stream().filter(x->x.myTeam.id==idTeamOpponent_i).findAny().orElse(null);

                                    System.out.println("protocol Competition " + idTeamOpponent_i + " " + toClient + " " + newResult + " " + result);
                                    if (toClient != null) {
                                        TCP_writeTo(toClient, "{\"Protocol\":\"Competition\",\"Result\":\"" + newResult +
                                                "\",\"Point\":\"" + idPoint + "\",\"Team\":\"" + idTeamOpponent +"\"}");
                                    }
                                }

                            }
                        } catch (IOException e) {
                            System.err.println("Client disconnected: " + e.getMessage());
                            clients.remove(clients.get(j));
                        }
                    }

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        }
        }).start();


        /** Задача потока обновлять данные аватара клиента. Держать данные аватара актуальными */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        udpSocket.receive(receivePacket);

                        InetAddress inetAddress = receivePacket.getAddress();
                        Client CurrentClient = null;
                        for (Client client : clients)
                            if (client.checkEqualAddr(inetAddress)) {
                                CurrentClient = client;
                                break;
                            }
                        if (CurrentClient == null)
                            throw new Exception("Запрос от незарегистрированного клиента");

                        byte typeId = receivePacket.getData()[0];

                        if (typeId == 1) { //тип аватара команды

                            STeam sTeam = STeam.fromByteArray(receivePacket.getData(), 1);
                            Map.getInstance().mergeTeam(sTeam);

                            STeam myTeam = Map.getInstance().getTeam(sTeam.id); //сформированный
                            if (myTeam != null) {

                                for (Client client : clients) //TODO тестирование
                                    if (client.myTeam != null)
                                        if (client.myTeam.id == myTeam.id)
                                            CurrentClient = client;

                                CurrentClient.myTeam = myTeam;
                            }
                        }

                        if (typeId == 2) {
                            for (int i = 0; i < receivePacket.getLength() / SPoint.totalSize; i++) {
                                SPoint sPoint = SPoint.fromByteArray(receivePacket.getData(), 1 + i * SPoint.totalSize);
                                Map.getInstance().mergePoint(sPoint);
                            }

                        }

                        if (typeId == 3) {
                            for (int i = 0; i < receivePacket.getLength() / SPlayer.totalSize; i++) {
                                SPlayer player = SPlayer.fromByteArray(receivePacket.getData(), 1 + i * SPlayer.totalSize);
                                Map.getInstance().mergePlayer(player);
                            }
                        }

                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }).start();
    }


    /** Отправка UDP датаграмм аватаров всех типов всем игрокам */
    public void engine() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {

                    try {
                        if (clients.size() != 0) {
                            //---------------------------сформировываем список STeam---------------------------
                            ByteBuffer buffer_teams = ByteBuffer.allocate(STeam.totalSize * clients.size() + 1);
                            buffer_teams.put(STEAM_TYPE);

                            for (Client client : clients)
                                if (client.myTeam != null)
                                    buffer_teams.put(client.myTeam.toByteArray());

                            byte[] bytes = buffer_teams.array();

                            for (Client client : clients)
                                UDP_writeTo(client, bytes);

                            //---------------------------сформировываем список игроков команд---------------------------
                            ByteBuffer buffer_players = ByteBuffer.allocate(1 + SPlayer.totalSize * clients.size() * 5);
                            buffer_players.put(PLAYER_TYPE);

                            for (Client client : clients)
                                if (client.myTeam != null)
                                    for (SPlayer sPlayer : client.myTeam.sPlayers)
                                        buffer_players.put(sPlayer.toByteArray());

                            byte[] bytes_players = buffer_players.array();

                            for (Client client : clients)
                                UDP_writeTo(client, bytes_players);

                            //---------------------------сформировываем важные точки для отправки---------------------------
                            Set<SPoint> points = new HashSet<>();
                            List<SPoint> all_points = Map.getInstance().getPointList();
                            List<STeam> all_teams = Map.getInstance().getsTeams();

                            for (STeam team : all_teams)  //для каждой команды
                                for (SPoint sPoint : all_points)
                                    if (team.id == sPoint.owner)
                                        points.add(sPoint);

                            if (points.size() != 0) {

                                ByteBuffer buffer_points = ByteBuffer.allocate(points.size() * SPoint.totalSize + 1);
                                buffer_points.put(POINT_TYPE);

                                for (SPoint sPoint : points)
                                    buffer_points.put(sPoint.toByteArray());

                                byte[] bytes_points = buffer_points.array();

                                for (Client client : clients)
                                    UDP_writeTo(client, bytes_points);
                            }

                        }

                        Thread.sleep(delay / 10);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }

            }
        }).start();

    }


    private void TCP_writeTo(Client client, byte[] bytes) {
        try {
            client.socket.getOutputStream().write(bytes);
            client.socket.getOutputStream().flush();

            Thread.sleep(delay);
        } catch (IOException e) {
            if (e.getMessage().equals("Connection reset by peer")) {
                clients.remove(client);

                if (client == host)
                    host = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void TCP_writeTo(Client client, String request) {
        try {
            client.out.write(request);
            client.out.flush();

            Thread.sleep(delay);
        } catch (IOException e) {
            if (e.getMessage().equals("Connection reset by peer")) {
                clients.remove(client);

                if (client == host)
                    host = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void UDP_writeTo(Client client, byte[] bytes) {
        try {
            if (client.udp_port < 5000) return; //TODO на время тестинга протоколов

            //делим данные bytes_points на пакеты по 1024 и отправляем (+1, 2 байта на типы данных)
            for (int i = 0; i < bytes.length; i += maxSizePacket) {
                int length = Math.min(maxSizePacket, bytes.length - i);

                byte[] packet = new byte[length];
                System.arraycopy(bytes, i, packet, 0, length);

                DatagramPacket packet_points = new DatagramPacket(packet, packet.length, client.inetAddress, /*UDP_CLIENT_PORT*/client.udp_port);
                udpSocket.send(packet_points);
            }

            Thread.sleep(delay);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /** Протокол уведомления о новом клиенте для других клиентов */
    private void protocol_add_client(Client client) {
        if (client.myTeam != null) {
            System.out.println("try addClient " + clients.size());
            byte[] bytes = client.myTeam.toByteArray();
            String result = Base64.getUrlEncoder().encodeToString(bytes);

            String request = "{\"Protocol\":\"AddPlayer\",\"Value\":\"" + result + "\",\"idClient\":\"" + client.id + "\"}";

            for (Client client_ : clients)
                if (client_ != client) {
                    System.out.println("to " + client_.id);
                    TCP_writeTo(client_, request);
                }

            ////разослать этому клиенты чужие аватары
            for (Client client_ : clients)
                if (client_ != client && client_.myTeam != null) {
                    System.out.println("to me " + client_.id);
                    bytes = client_.myTeam.toByteArray();
                    result = Base64.getUrlEncoder().encodeToString(bytes);
                    String request_ = "{\"Protocol\":\"AddPlayer\",\"Value\":\"" + result + "\",\"idClient\":\"" + client_.id + "\"}";
                    TCP_writeTo(client, request_);
                }
        }
    }

    private void protocol_assign_avatar(Client newClient) {
        if (Map.getInstance().getBoot_teams() != null && Map.getInstance().getBoot_teams().size() != 0) {
            List<STeam> freeTeam = Map.getInstance().getBoot_teams();

            STeam chooseTeam = freeTeam.get(0);
            freeTeam.remove(0);
            String result = Base64.getUrlEncoder().encodeToString(chooseTeam.toByteArray());
            newClient.myTeam = chooseTeam;

            TCP_writeTo(newClient, "{\"Protocol\":\"AssigningAvatar\",\"Value\":\"" + result + "\"}");
            protocol_add_client(newClient);
        }
    }

    private void protocol_make_host(Client client) {
        TCP_writeTo(client, "{\"Protocol\":\"makeHost\"}");
    }
}
