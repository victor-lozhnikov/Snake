package net.client;

import main.java.net.protocol.SnakesProto;

import java.net.InetAddress;

public class MessageWithAdditionalInfo {
    private SnakesProto.GameMessage message;
    private InetAddress address;
    private int port;
    private long lastSentTime = 0L;

    public MessageWithAdditionalInfo(SnakesProto.GameMessage message, InetAddress address, int port) {
        this.message = message;
        this.address = address;
        this.port = port;
    }

    public SnakesProto.GameMessage getMessage() {
        return message;
    }

    public void setMessage(SnakesProto.GameMessage message) {
        this.message = message;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getLastSentTime() {
        return lastSentTime;
    }

    public void setLastSentTime(long lastSentTime) {
        this.lastSentTime = lastSentTime;
    }
}
