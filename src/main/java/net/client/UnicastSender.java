package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;
import mvc.model.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnicastSender implements Runnable {
    private final Map<Long, MessageWithAdditionalInfo> messageQueue;
    private final Map<Player, Long> lastMessageSent;
    private final GameModel model;

    public UnicastSender(GameModel model) {
        this.model = model;
        messageQueue = new ConcurrentHashMap<>();
        lastMessageSent = new ConcurrentHashMap<>();
    }

    public void sendMessage(SnakesProto.GameMessage message, InetAddress address, int port) {
        messageQueue.put(message.getMsgSeq(), new MessageWithAdditionalInfo(message, address, port));
        System.out.println(messageQueue.size());
    }

    public void removeMessageFromQueue(long seq) {
        messageQueue.remove(seq);
    }

    public void clearMessageQueue() {
        messageQueue.clear();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (Map.Entry<Long, MessageWithAdditionalInfo> message : messageQueue.entrySet()) {

                if (System.currentTimeMillis() - message.getValue().getLastSentTime() < model.getPingDelay()) {
                    continue;
                }

                DatagramPacket packet = new DatagramPacket(message.getValue().getMessage().toByteArray(),
                        message.getValue().getMessage().toByteArray().length,
                        message.getValue().getAddress(), message.getValue().getPort());
                //System.out.println(message.getValue().getMessage() + "\n" + message.getValue().getAddress());
                try {
                    model.getUnicastSocket().send(packet);
                    //lastMessageSent.put()
                    //System.out.println("Sent " + message.getValue().getMessage().getTypeCase());
                    message.getValue().setLastSentTime(System.currentTimeMillis());
                    if (message.getValue().getMessage().hasAnnouncement() || message.getValue().getMessage().hasAck()) {
                        messageQueue.remove(message.getKey());
                    }
                }
                catch (SocketException ignored) {}
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            /*for (Player player : model.getPlayerMap().values()) {
                if (!lastMessageSent.containsKey(player)) {

                }
            }*/
        }
    }

    private void sendPingMessage() {

    }
}
