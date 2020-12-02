package net.client;

import main.java.net.protocol.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UnicastSender implements Runnable {
    private Set<MessageWithAdditionalInfo> messageQueue;
    private DatagramSocket socket;

    public UnicastSender() throws IOException {
        messageQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());
        socket = new DatagramSocket();
    }

    public void sendMessage(SnakesProto.GameMessage message, InetAddress address, int port) {
        messageQueue.add(new MessageWithAdditionalInfo(message, address, port));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (MessageWithAdditionalInfo message : messageQueue) {
                DatagramPacket packet = new DatagramPacket(message.getMessage().toByteArray(),
                        message.getMessage().toByteArray().length, message.getAddress(), message.getPort());
                try {
                    socket.send(packet);
                    if (message.getMessage().hasAnnouncement() || message.getMessage().hasAck()) {
                        System.out.println("Announcement sent");
                        messageQueue.remove(message);
                    }
                }
                catch (IOException ex) {
                    System.err.println("Can't send message");
                }
            }
        }
    }
}
