package mvc.model;

import javafx.application.Platform;
import main.java.net.protocol.SnakesProto;

import java.io.IOException;
import java.net.InetAddress;
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
            int ret = model.addSnakeHeadToField(snake);
            if (ret == 0) {
                continue;
            }
            if (ret == 2) {
                model.removeSnake(snake);
            }
            for (Snake snake1 : model.getSnakeMap().values()) {
                if (snake.equals(snake1)) {
                    continue;
                }
                if (Arrays.equals(snake.getKeyPoints().get(0), snake1.getKeyPoints().get(0))) {
                    model.removeSnake(snake1);
                }
            }
            model.removeSnake(snake);
        }

        model.addNecessaryFood();
        model.fillCells();

        sendGameStateMsg();

        if (model.getGameView() != null) {
            Platform.runLater(() -> model.getGameView().drawField());
        }
    }

    private void sendGameStateMsg() {
        SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.StateMsg.Builder stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder();
        SnakesProto.GameState.Builder gameState = SnakesProto.GameState.newBuilder();
        gameState.setStateOrder(model.getStateOrder());
        model.iterateStateOrder();
        for (Snake snake : model.getSnakeMap().values()) {
            gameState.addSnakes(snake.convertSnakeForMsg());
        }
        for (int[] food : model.getFood()) {
            SnakesProto.GameState.Coord.Builder coordsBuilder = SnakesProto.GameState.Coord.newBuilder();
            coordsBuilder.setX(food[0]);
            coordsBuilder.setY(food[1]);
            gameState.addFoods(coordsBuilder);
        }
        gameState.setPlayers(model.getGamePlayers());
        gameState.setConfig(model.getGameConfig());
        stateMsg.setState(gameState);
        builder.setState(stateMsg);
        builder.setMsgSeq(model.getLastMsgSeq());
        model.iterateLastMsqSeq();
        SnakesProto.GameMessage message = builder.build();

        for (SnakesProto.GamePlayer player : model.getGamePlayers().getPlayersList()) {
            if (player.getIpAddress().isBlank()) {
                continue;
            }
            try {
                model.getUnicastSender().sendMessage(message, InetAddress.getByName(player.getIpAddress()),
                        player.getPort());
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
