package net.client;

import net.protocol.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class MulticastReceiver implements Runnable {
    private MulticastSocket socket;

    public MulticastReceiver() throws IOException {
        InetAddress group = InetAddress.getByName(Constants.MULTICAST_IP);
        socket = new MulticastSocket(Constants.MULTICAST_PORT);
        socket.joinGroup(new InetSocketAddress(group, Constants.MULTICAST_PORT),
                NetworkInterface.getByInetAddress(group));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

        }
    }
}
