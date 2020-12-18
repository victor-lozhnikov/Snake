package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;
import mvc.model.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnicastReceiver implements Runnable {
    private GameModel model;
    private MessageHandler messageHandler;
    private Map<Integer, Long> lastMessageReceive;

    public UnicastReceiver(GameModel model) {
        this.model = model;
        messageHandler = new MessageHandler(model);
        lastMessageReceive = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(new byte[10000], 10000);
            try {
                model.getUnicastSocket().receive(packet);
                SnakesProto.GameMessage message = main.java.net.protocol.SnakesProto.GameMessage.parseFrom(
                        Arrays.copyOf(packet.getData(), packet.getLength()));
                //System.out.println(message.getMsgSeq() + " " + message.getTypeCase());
                messageHandler.handleMessage(message, packet.getAddress(), packet.getPort());
                lastMessageReceive.put(model.findPlayerIdByIpAndPort(packet.getAddress(), packet.getPort()),
                        System.currentTimeMillis());
            }
            catch (SocketTimeoutException | SocketException ignored) {}
            catch (IOException ex) {
                ex.printStackTrace();
            }

            for (Player player : model.getPlayerMap().values()) {
                if (player.getId() == model.getMyId()) continue;
                if (!lastMessageReceive.containsKey(player.getId())) {
                    lastMessageReceive.put(player.getId(), System.currentTimeMillis());
                }
                if (System.currentTimeMillis() - lastMessageReceive.get(player.getId()) > model.getNodeTimeout()) {
                    model.removePlayer(player.getId());
                }
            }
        }
    }
}
