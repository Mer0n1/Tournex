package org.example;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;


public class SPlayer implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final Integer totalSize = 13;

    public int id;
    public boolean isMoving;
    public Integer myTeam;
    public Integer coordinate;

    public SPlayer(int id, boolean isMoving, Integer myTeam, Integer coordinate) {
        this.id = id;
        this.isMoving = isMoving;
        this.myTeam = myTeam;
        this.coordinate = coordinate;
    }


    public byte[] toByteArray() {

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(id);
        buffer.put((byte) (isMoving ? 1 : 0));
        buffer.putInt(myTeam);
        buffer.putInt(coordinate);

        return buffer.array();
    }

    public static SPlayer fromByteArray(byte[] array, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(array, offset, totalSize);

        int id = buffer.getInt();
        boolean isInGame = buffer.get() == 1;
        Integer myTeam = buffer.getInt();
        Integer coordinate = buffer.getInt();

        return new SPlayer(id, isInGame, myTeam, coordinate);
    }

    @Override
    public String toString() {
        return "SPlayer{" +
                "id=" + id +
                ", isMoving=" + isMoving +
                ", myTeam=" + myTeam +
                ", coordinate=" + coordinate +
                '}';
    }
}