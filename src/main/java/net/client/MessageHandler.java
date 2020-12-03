package net.client;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

import java.net.InetAddress;

public class MessageHandler {
    private GameModel model;

    public MessageHandler(GameModel model) {
        this.model = model;
    }

    public void handleMessage(SnakesProto.GameMessage message, InetAddress address, int port) {
        int playerId = model.findPlayerIdByIpAndPort(address, port);
        if (playerId > 0 && model.findMsgSeq(playerId, message.getMsgSeq())) {
            return;
        }
        switch (message.getTypeCase()) {
            case ACK:
                if (model.getMyId() < 0) {
                    model.setMyId(message.getReceiverId());
                }
                model.getUnicastSender().removeMessageFromQueue(message.getMsgSeq());
                break;
            case JOIN:
                int receiverId = model.tryJoin(message.getJoin().getName(), address, port);
                if (receiverId > 0) {
                    model.findMsgSeq(receiverId, message.getMsgSeq());
                    model.getUnicastSender().sendMessage(buildAckMsg(message, receiverId), address, port);
                }
                else {
                    model.getUnicastSender().sendMessage(buildErrorMsg(message, "Мест нет"),
                            address, port);
                }
                break;
            case STATE:
                model.setState(message.getState().getState());
                model.getUnicastSender().sendMessage(buildAckMsg(message, message.getSenderId()), address, port);
                break;
            case STEER:
                if (model.getLastSteerMsg().containsKey(message.getSenderId())) {
                    if (model.getLastSteerMsg().get(message.getSenderId()) > message.getMsgSeq()) {
                        return;
                    }
                }
                model.getLastSteerMsg().put(message.getSenderId(), message.getMsgSeq());
                model.addNewSteerMsg(message.getSenderId(), message.getSteer());
                model.getUnicastSender().sendMessage(buildAckMsg(message, message.getSenderId()), address, port);
                break;
            case ROLE_CHANGE:
                model.setMyNodeRole(message.getRoleChange().getReceiverRole());
                model.getUnicastSender().sendMessage(buildAckMsg(message, message.getSenderId()), address, port);
                break;
        }
    }

    private SnakesProto.GameMessage buildAckMsg(SnakesProto.GameMessage message, int receiverId) {
        SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage.newBuilder();
        builder.setAck(SnakesProto.GameMessage.AckMsg.getDefaultInstance());
        builder.setMsgSeq(message.getMsgSeq());
        builder.setReceiverId(receiverId);
        builder.setSenderId(model.getMyId());
        return builder.build();
    }

    private SnakesProto.GameMessage buildErrorMsg(SnakesProto.GameMessage message, String error) {
        SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.ErrorMsg.Builder errorBuilder = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        errorBuilder.setErrorMessage(error);
        builder.setError(errorBuilder);
        builder.setMsgSeq(message.getMsgSeq());
        return builder.build();
    }
}
