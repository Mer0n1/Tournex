package org.example;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    public int id;
    public int udp_port; //TESTING TODO
    public Socket socket;
    public BufferedReader in;
    public BufferedWriter out;
    public InetAddress inetAddress;
    public STeam myTeam;
    //public List<SPlayer> players;

    public Client(Socket socket, int id) throws IOException {
        this.socket = socket;
        this.id = id;
        udp_port = -1;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        inetAddress = socket.getInetAddress();

        //players = new ArrayList<>();
    }

    public boolean checkEqualAddr(InetAddress inetAddress_) {
        return inetAddress.getHostAddress().equals(inetAddress_.getHostAddress());
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", myTeam=" + (myTeam==null) +
                '}';
    }
}
