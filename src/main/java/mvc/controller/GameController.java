package mvc.controller;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

public final class GameController {

    private final GameModel model;

    public GameController(GameModel model) {
        this.model = model;
    }

    public void moveUp() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.UP);
        sendSteerMsg(msg);
    }

    public void moveDown() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.DOWN);
        sendSteerMsg(msg);
    }

    public void moveRight() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.RIGHT);
        sendSteerMsg(msg);
    }

    public void moveLeft() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.LEFT);
        sendSteerMsg(msg);
    }

    SnakesProto.GameMessage.SteerMsg buildSteerMsg(SnakesProto.Direction direction) {
        SnakesProto.GameMessage.SteerMsg.Builder steerMsgBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        steerMsgBuilder.setDirection(direction);
        return steerMsgBuilder.build();
    }

    private void sendSteerMsg(SnakesProto.GameMessage.SteerMsg msg) {
        if (model.getNodeRole() == SnakesProto.NodeRole.VIEWER) {
            return;
        }

        if (model.getNodeRole() == SnakesProto.NodeRole.MASTER) {
            model.addNewSteerMsg(model.getMyId(), msg);
        }
        else {
            SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage.newBuilder();
            builder.setSteer(msg);
            builder.setSenderId(model.getMyId());
            builder.setMsgSeq(model.getLastMsgSeq());
            model.iterateLastMsqSeq();
            model.getUnicastSender().sendMessage(builder.build(), model.getMasterInetAddress(), model.getMasterPort());
        }
    }
}
