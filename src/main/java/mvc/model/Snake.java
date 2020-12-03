package mvc.model;

import main.java.net.protocol.SnakesProto;
import main.java.net.protocol.SnakesProto.Direction;

import java.util.ArrayList;
import java.util.List;

public class Snake {

    private Direction direction;
    private Direction nextDirection;
    private List<int[]> keyPoints;
    private GameModel model;
    private int id;
    private SnakesProto.GameState.Snake.SnakeState state;

    public Snake(GameModel model, int id, int headX, int headY, Direction direction) {
        this.model = model;
        this.id = id;
        this.direction = direction;
        this.nextDirection = direction;
        this.state = SnakesProto.GameState.Snake.SnakeState.ALIVE;
        keyPoints = new ArrayList<>();
        keyPoints.add(new int[] {headX, headY});
        switch (direction) {
            case UP:
                keyPoints.add(new int[] {0, 1});
                break;
            case DOWN:
                keyPoints.add(new int[] {0, -1});
                break;
            case LEFT:
                keyPoints.add(new int[] {1, 0});
                break;
            case RIGHT:
                keyPoints.add(new int[] {-1, 0});
                break;
        }
    }

    public Snake(SnakesProto.GameState.Snake snake, GameModel model) {
        this.model = model;
        this.id = snake.getPlayerId();
        this.direction = snake.getHeadDirection();
        this.nextDirection = snake.getHeadDirection();
        this.state = snake.getState();

        keyPoints = new ArrayList<>();
        for (SnakesProto.GameState.Coord coords : snake.getPointsList()) {
            keyPoints.add(new int[] {coords.getX(), coords.getY()});
        }
    }

    public List<int[]> getKeyPoints() {
        return keyPoints;
    }

    public Direction getDirection() {
        return direction;
    }

    public void trySetDirection(Direction newDirection) {
        if ((newDirection == Direction.UP && direction == Direction.DOWN) ||
                (newDirection == Direction.DOWN && direction == Direction.UP) ||
                (newDirection == Direction.LEFT && direction == Direction.RIGHT) ||
                (newDirection == Direction.RIGHT && direction == Direction.LEFT)) {
            return;
        }

        nextDirection = newDirection;
    }

    public void makeMove() {
        if (direction != nextDirection) {
            direction = nextDirection;
            keyPoints.add(1, new int[] {0, 0});
        }

        switch (direction) {
            case UP:
                keyPoints.get(0)[1]--;
                keyPoints.get(1)[1]++;
                break;
            case DOWN:
                keyPoints.get(0)[1]++;
                keyPoints.get(1)[1]--;
                break;
            case LEFT:
                keyPoints.get(0)[0]--;
                keyPoints.get(1)[0]++;
                break;
            case RIGHT:
                keyPoints.get(0)[0]++;
                keyPoints.get(1)[0]--;
                break;
        }

        keyPoints.get(0)[0] = Math.floorMod(keyPoints.get(0)[0], model.getFieldWidth());
        keyPoints.get(0)[1] = Math.floorMod(keyPoints.get(0)[1], model.getFieldHeight());

        if (model.getCells()[keyPoints.get(0)[0]][keyPoints.get(0)[1]] == GameModel.CellType.FOOD) {
            model.removeFood(keyPoints.get(0)[0], keyPoints.get(0)[1]);
            model.getPlayerMap().get(id).iterateScore();
            return;
        }

        if (keyPoints.get(keyPoints.size() - 1)[0] > 0) {
            keyPoints.get(keyPoints.size() - 1)[0]--;
        }
        if (keyPoints.get(keyPoints.size() - 1)[0] < 0) {
            keyPoints.get(keyPoints.size() - 1)[0]++;
        }
        if (keyPoints.get(keyPoints.size() - 1)[1] > 0) {
            keyPoints.get(keyPoints.size() - 1)[1]--;
        }
        if (keyPoints.get(keyPoints.size() - 1)[1] < 0) {
            keyPoints.get(keyPoints.size() - 1)[1]++;
        }
        if (keyPoints.get(keyPoints.size() - 1)[0] == 0 && keyPoints.get(keyPoints.size() - 1)[1] == 0) {
            keyPoints.remove(keyPoints.size() - 1);
        }
    }

    public int getId() {
        return id;
    }

    public SnakesProto.GameState.Snake convertSnakeForMsg() {
        SnakesProto.GameState.Snake.Builder builder = SnakesProto.GameState.Snake.newBuilder();
        builder.setState(state);
        builder.setPlayerId(id);
        for (int[] coords : keyPoints) {
            SnakesProto.GameState.Coord.Builder coordsBuilder = SnakesProto.GameState.Coord.newBuilder();
            coordsBuilder.setX(coords[0]);
            coordsBuilder.setY(coords[1]);
            builder.addPoints(coordsBuilder);
            builder.setHeadDirection(direction);
        }
        return builder.build();
    }
}
