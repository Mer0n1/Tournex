package io.github.capoints.Server;

import io.github.capoints.objects.Player;
import io.github.capoints.objects.Team;

public interface ServerControlledPoint {
    void synchronizeSPoint(SPoint sPoint);
    void forceCapture(STeam team);
    //void forceLeave
}

