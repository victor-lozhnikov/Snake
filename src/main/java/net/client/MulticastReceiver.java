package net.client;

import javafx.application.Platform;
import main.java.net.protocol.SnakesProto;
import mvc.view.GamesListView;
import net.protocol.Constants;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MulticastReceiver implements Runnable {
    private GamesListView gamesListView;
    private final MulticastSocket socket;
    private Map<InetSocketAddress, SnakesProto.GameMessage.AnnouncementMsg> currentAnnouncements;

    public MulticastReceiver(GamesListView gamesListView) throws IOException {
        InetAddress group = InetAddress.getByName(Constants.MULTICAST_IP);
        socket = new MulticastSocket(Constants.MULTICAST_PORT);
        socket.joinGroup(new InetSocketAddress(group, Constants.MULTICAST_PORT),
                NetworkInterface.getByInetAddress(group));
        socket.setSoTimeout(Constants.MULTICAST_SOCKET_TIMEOUT);
        this.gamesListView = gamesListView;
        currentAnnouncements = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(new byte[10000], 10000);
            try {
                socket.receive(packet);
                SnakesProto.GameMessage message = SnakesProto.GameMessage.parseFrom(
                        Arrays.copyOf(packet.getData(), packet.getLength()));
                currentAnnouncements.put((InetSocketAddress) packet.getSocketAddress(), message.getAnnouncement());
                Platform.runLater(() -> gamesListView.update());
            }
            catch (SocketTimeoutException ignored) {}
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        socket.close();
    }

    public Map<InetSocketAddress, SnakesProto.GameMessage.AnnouncementMsg> getCurrentAnnouncements() {
        return currentAnnouncements;
    }
}
