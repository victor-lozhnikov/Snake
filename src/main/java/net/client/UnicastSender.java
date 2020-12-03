package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnicastSender implements Runnable {
    private Map<Long, MessageWithAdditionalInfo> messageQueue;
    private GameModel model;

    public UnicastSender(GameModel model) throws IOException {
        this.model = model;
        messageQueue = new ConcurrentHashMap<>();
    }

    public void sendMessage(SnakesProto.GameMessage message, InetAddress address, int port) {
        messageQueue.put(message.getMsgSeq(), new MessageWithAdditionalInfo(message, address, port));
    }

    public void removeMessageFromQueue(long seq) {
        messageQueue.remove(seq);
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
                try {
                    model.getUnicastSocket().send(packet);
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
        }
    }
}
