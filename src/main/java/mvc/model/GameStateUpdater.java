package mvc.model;

import javafx.application.Platform;
import main.java.net.protocol.SnakesProto;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameStateUpdater extends TimerTask {
    private GameModel model;

    public GameStateUpdater(GameModel model) {
        this.model = model;
    }

    @Override
    public void run() {
        Queue<Map.Entry<Integer, SnakesProto.GameMessage.SteerMsg>> currentQueue =
                new ConcurrentLinkedDeque<>(model.getSteerMsgQueue());
        model.clearSteerMsgQueue();
        for (Map.Entry<Integer, SnakesProto.GameMessage.SteerMsg> msg : currentQueue) {
            model.getSnakeById(msg.getKey()).trySetDirection(msg.getValue().getDirection());
        }
        for (Snake snake : model.getSnakeMap().values()) {
            snake.makeMove();
        }
        model.clearField();
        for (Snake snake : model.getSnakeMap().values()) {
            model.addSnakeBodyToField(snake);
        }
        for (Snake snake : model.getSnakeMap().values()) {
            model.addSnakeHeadToField(snake);
        }
        model.updateFood();
        if (model.getGameView() != null) {
            Platform.runLater(() -> model.getGameView().drawField());
        }
    }
}
