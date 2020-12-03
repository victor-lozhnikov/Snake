package mvc.model;

import main.java.net.protocol.SnakesProto;

import java.net.InetAddress;

public class Player {
    private String name;
    private int id;
    private String ipAddress;
    private int port;
    private SnakesProto.NodeRole nodeRole;
    private int score;

    public Player(String name, int id, String ipAddress, int port, SnakesProto.NodeRole nodeRole, int score) {
        this.name = name;
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.nodeRole = nodeRole;
        this.score = score;
    }

    public Player(SnakesProto.GamePlayer player) {
        this.name = player.getName();
        this.id = player.getId();
        this.ipAddress = player.getIpAddress();
        this.port = player.getPort();
        this.nodeRole = player.getRole();
        this.score = player.getScore();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public SnakesProto.NodeRole getNodeRole() {
        return nodeRole;
    }

    public void setNodeRole(SnakesProto.NodeRole nodeRole) {
        this.nodeRole = nodeRole;
    }

    public int getScore() {
        return score;
    }

    public void iterateScore() {
        score++;
    }

    public SnakesProto.GamePlayer convertPlayerForMsg() {
        SnakesProto.GamePlayer.Builder builder = SnakesProto.GamePlayer.newBuilder();
        builder.setName(name);
        builder.setId(id);
        builder.setIpAddress(ipAddress);
        builder.setPort(port);
        builder.setRole(nodeRole);
        builder.setScore(score);
        return builder.build();
    }
}
