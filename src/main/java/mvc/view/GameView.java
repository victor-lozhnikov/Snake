package mvc.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import mvc.controller.GameController;
import mvc.model.GameModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GameView {

    GameModel model = GameModel.getInstance();
    GameController controller = GameController.getInstance(model);

    public Canvas cells;
    GraphicsContext graphicsContext;

    private double maxFieldSize = 560.0;
    private double fieldWidth;
    private double fieldHeight;
    private double cellSize;
    Timer timer;
    boolean pause = false;

    static Map<GameModel.CellType, Color> cellColors;
    static {
        cellColors = new HashMap<>();
        cellColors.put(GameModel.CellType.EMPTY, Color.WHITE);
        cellColors.put(GameModel.CellType.MY_HEAD, Color.DARKGREEN);
        cellColors.put(GameModel.CellType.MY_BODY, Color.GREEN);
        cellColors.put(GameModel.CellType.ENEMY_HEAD, Color.GRAY);
        cellColors.put(GameModel.CellType.ENEMY_BODY, Color.DARKGRAY);
        cellColors.put(GameModel.CellType.FOOD, Color.RED);
    }

    public void initialize() {
        calculateSizes();
        graphicsContext = cells.getGraphicsContext2D();
        drawField();
        initTimer();
    }

    public void initTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                model.updateSnakes();
                Platform.runLater(() -> drawField());
            }
        }, model.getStateDelay(), model.getStateDelay());
    }

    private void calculateSizes() {
        int cellsX = model.getFieldWidth();
        int cellsY = model.getFieldHeight();
        cellSize = (int) Math.min(maxFieldSize / cellsX, maxFieldSize / cellsY);
        fieldWidth = cellSize * cellsX;
        fieldHeight = cellSize * cellsY;
        cells.setLayoutX(20 + (maxFieldSize - fieldWidth) / 2);
        cells.setLayoutY(20 + (maxFieldSize - fieldHeight) / 2);
        cells.setWidth(fieldWidth);
        cells.setHeight(fieldHeight);
    }

    private void drawField() {
        for (int i = 0; i < model.getFieldWidth(); ++i) {
            for (int j = 0; j < model.getFieldHeight(); ++j) {
                graphicsContext.setFill(cellColors.get(model.getCellTypeByCoordinates(i, j)));
                graphicsContext.fillRect(i * cellSize, j * cellSize, cellSize, cellSize);
            }
        }
        graphicsContext.strokeRect(0, 0, fieldWidth, fieldHeight);
    }

    public void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
                controller.moveUp();
                break;
            case DOWN:
                controller.moveDown();
                break;
            case RIGHT:
                controller.moveRight();
                break;
            case LEFT:
                controller.moveLeft();
                break;
            case SPACE:
                if (!pause) {
                    timer.cancel();
                    pause = true;
                }
                else {
                    initTimer();
                    pause = false;
                }
        }
    }

    @FXML
    public void exitApplication() {
        timer.cancel();
        Platform.exit();
    }
}
