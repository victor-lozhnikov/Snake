package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Queue;

public class Transmitter implements Runnable {
    private DatagramSocket socket;
    private GameModel model;
    private Queue<MessageWithAdditionalInfo> messageQueue;

    public Transmitter(GameModel model) throws IOException {
        socket = new DatagramSocket();
        this.model = model;
    }

    public synchronized void sendMessage(SnakesProto.GameMessage message, InetAddress address, int port) {
        messageQueue.add(new MessageWithAdditionalInfo(message, address, port));
    }

    @Override
    public void run() {

    }
}
