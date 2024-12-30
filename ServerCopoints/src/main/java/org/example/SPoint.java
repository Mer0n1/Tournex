package org.example;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SPoint implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final Integer totalSize = 128;

    public int id;
    public boolean isBase;
    public boolean isCapture;
    public Vector2 coord;
    public Integer owner;
    public Integer invader;
    public Set<Integer> connectedPoints;
    public List<Integer> players;

    public SPoint(int id, boolean isBase, boolean isCapture, Vector2 coord, Integer owner,
                  Integer invader, Set<Integer> connectedPoints, List<Integer> players) {
        this.id = id;
        this.isBase = isBase;
        this.isCapture = isCapture;
        this.coord = coord;
        this.owner = owner;
        this.invader = invader;
        this.connectedPoints = connectedPoints;
        this.players = players;
    }

    public byte[] toByteArray() {

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(id);
        buffer.put((byte) (isBase ? 1 : 0));
        buffer.put((byte) (isCapture ? 1 : 0));
        buffer.putInt((int) coord.x);
        buffer.putInt((int) coord.y);
        buffer.putInt(owner);
        buffer.putInt(invader);

        buffer.putInt(connectedPoints.size());
        for (Integer point : connectedPoints)
            buffer.putInt(point);

        buffer.putInt(players.size());
        for (Integer point : players)
            buffer.putInt(point);

        return buffer.array();
    }

    public static SPoint fromByteArray(byte[] array, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(array, offset, totalSize);

        int id = buffer.getInt();
        boolean isBase = buffer.get() == 1;
        boolean isCapture = buffer.get() == 1;
        int x = buffer.getInt();
        int y = buffer.getInt();
        int owner = buffer.getInt();
        int invader = buffer.getInt();

        int pointsSize = buffer.getInt();
        Set<Integer> connectedPoints = new HashSet<>();
        for (int i = 0; i < pointsSize; i++)
            connectedPoints.add(buffer.getInt());

        int playersSize = buffer.getInt();
        List<Integer> players = new ArrayList<>();
        for (int i = 0; i < playersSize; i++)
            players.add(buffer.getInt());


        return new SPoint(id, isBase, isCapture, new Vector2(x,y), owner, invader, connectedPoints, players);
    }

    @Override
    public String toString() {
        return "SPoint{" +
                "id=" + id +
                ", isBase=" + isBase +
                ", isCapture=" + isCapture +
                ", coord=" + coord +
                ", owner=" + owner +
                ", invader=" + invader +
                ", connectedPoints=" + connectedPoints +
                ", players=" + players +
                '}';
    }
}
