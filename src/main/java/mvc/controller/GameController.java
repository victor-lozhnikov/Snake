package mvc.controller;

import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

public final class GameController {

    private GameModel model;

    public GameController(GameModel model) {
        this.model = model;
    }

    public void moveUp() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.UP);
        if (model.getNodeRole() == SnakesProto.NodeRole.MASTER) {
            model.addNewSteerMsg(model.getMyId(), msg);
        }
        else {

        }
    }

    public void moveDown() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.DOWN);
        if (model.getNodeRole() == SnakesProto.NodeRole.MASTER) {
            model.addNewSteerMsg(model.getMyId(), msg);
        }
        else {

        }
    }

    public void moveRight() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.RIGHT);
        if (model.getNodeRole() == SnakesProto.NodeRole.MASTER) {
            model.addNewSteerMsg(model.getMyId(), msg);
        }
        else {

        }
    }

    public void moveLeft() {
        SnakesProto.GameMessage.SteerMsg msg = buildSteerMsg(SnakesProto.Direction.LEFT);
        if (model.getNodeRole() == SnakesProto.NodeRole.MASTER) {
            model.addNewSteerMsg(model.getMyId(), msg);
        }
        else {

        }
    }

    SnakesProto.GameMessage.SteerMsg buildSteerMsg(SnakesProto.Direction direction) {
        SnakesProto.GameMessage.SteerMsg.Builder steerMsgBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        steerMsgBuilder.setDirection(direction);
        return steerMsgBuilder.build();
    }
}
