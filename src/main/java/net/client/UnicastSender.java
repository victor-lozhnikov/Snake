package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;
import mvc.model.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnicastSender implements Runnable {
    private final Map<Long, MessageWithAdditionalInfo> messageQueue;
    private final Map<Integer, Long> lastMessageSent;
    private final GameModel model;

    public UnicastSender(GameModel model) {
        this.model = model;
        messageQueue = new ConcurrentHashMap<>();
        lastMessageSent = new ConcurrentHashMap<>();
    }

    public void sendMessage(SnakesProto.GameMessage message, InetAddress address, int port) {
        messageQueue.put(message.getMsgSeq(), new MessageWithAdditionalInfo(message, address, port));
        //System.out.println(messageQueue.size());
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
                    long sentTime = System.currentTimeMillis();
                    lastMessageSent.put(model.findPlayerIdByIpAndPort(
                            message.getValue().getAddress(), message.getValue().getPort()), sentTime);
                    message.getValue().setLastSentTime(sentTime);
                    if (message.getValue().getMessage().hasAnnouncement() || message.getValue().getMessage().hasAck()) {
                        messageQueue.remove(message.getKey());
                    }
                }
                catch (SocketException ignored) {}
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            for (Player player : model.getPlayerMap().values()) {
                if (player.getId() == model.getMyId()) continue;
                if (!lastMessageSent.containsKey(player.getId()) ||
                        System.currentTimeMillis() - lastMessageSent.get(player.getId()) > model.getPingDelay()) {
                    sendPingMessage(player);
                }
            }
        }
    }

    public void readdressMessages(InetAddress fromAddress, int fromPort, InetAddress toAddress, int toPort) {
        for (Map.Entry<Long, MessageWithAdditionalInfo> message : messageQueue.entrySet()) {
            if (message.getValue().getAddress().equals(fromAddress) && message.getValue().getPort() == fromPort &&
            !message.getValue().getMessage().hasPing()) {
                messageQueue.put(message.getKey(),
                        new MessageWithAdditionalInfo(message.getValue().getMessage(), toAddress, toPort));
            }
        }
    }

    private void sendPingMessage(Player player) {
        SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.PingMsg msg = SnakesProto.GameMessage.PingMsg.getDefaultInstance();
        builder.setPing(msg);
        builder.setMsgSeq(model.getLastMsgSeq());
        model.iterateLastMsqSeq();
        try {
            sendMessage(builder.build(), InetAddress.getByName(player.getIpAddress()), player.getPort());
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
