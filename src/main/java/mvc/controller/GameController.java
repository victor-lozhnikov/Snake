package mvc.controller;

import mvc.model.GameModel;
import mvc.model.Snake;

public final class GameController {

    private static GameController instance;
    private GameModel model;

    private GameController(GameModel model) {
        this.model = model;
    }

    public static synchronized GameController getInstance(GameModel model) {
        if (instance == null) {
            instance = new GameController(model);
        }
        return instance;
    }

    public void moveUp() {
        model.getMySnake().trySetDirection(Snake.Direction.UP);
    }

    public void moveDown() {
        model.getMySnake().trySetDirection(Snake.Direction.DOWN);
    }

    public void moveRight() {
        model.getMySnake().trySetDirection(Snake.Direction.RIGHT);
    }

    public void moveLeft() {
        model.getMySnake().trySetDirection(Snake.Direction.LEFT);
    }
}
