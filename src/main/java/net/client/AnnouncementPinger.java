package net.client;

import main.java.net.protocol.SnakesProto.GameMessage;
import mvc.model.GameModel;
import net.protocol.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

public class AnnouncementPinger extends TimerTask {
    private InetAddress multicastAddress;
    private GameModel model;

    public AnnouncementPinger(GameModel model) throws IOException {
        multicastAddress = InetAddress.getByName(Constants.MULTICAST_IP);
        this.model = model;
    }

    @Override
    public void run() {
        GameMessage.Builder gameMessage = GameMessage.newBuilder();
        GameMessage.AnnouncementMsg.Builder announcementMsg = GameMessage.AnnouncementMsg.newBuilder();
        announcementMsg.setConfig(model.getGameConfig());
        announcementMsg.setPlayers(model.getGamePlayers());
        gameMessage.setAnnouncement(announcementMsg);
        gameMessage.setMsgSeq(model.getLastMsgSeq());
        model.iterateLastMsqSeq();
        model.getUnicastSender().sendMessage(gameMessage.build(), multicastAddress, Constants.MULTICAST_PORT);
    }
}
