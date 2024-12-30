package io.github.capoints.Server;

import java.net.InetAddress;

public class Client {
    private STeam sTeam;
    private int myId;

    public Client(STeam sTeam, int myId) {
        this.sTeam = sTeam;
        this.myId = myId;
    }

    public STeam getsTeam() {
        return sTeam;
    }

    public int getMyId() {
        return myId;
    }

    public void setsTeam(STeam sTeam) {
        this.sTeam = sTeam;
    }

    public void setMyId(int myId) {
        this.myId = myId;
    }

}
