package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class UnicastReceiver implements Runnable {
    private GameModel model;
    private MessageHandler messageHandler;

    public UnicastReceiver(GameModel model) {
        this.model = model;
        messageHandler = new MessageHandler(model);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(new byte[10000], 10000);
            try {
                model.getUnicastSocket().receive(packet);
                SnakesProto.GameMessage message = main.java.net.protocol.SnakesProto.GameMessage.parseFrom(
                        Arrays.copyOf(packet.getData(), packet.getLength()));
                //System.out.println(message);
                messageHandler.handleMessage(message, packet.getAddress(), packet.getPort());
            }
            catch (SocketTimeoutException | SocketException ignored) {}
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
