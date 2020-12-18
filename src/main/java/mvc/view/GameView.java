package mvc.view;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mvc.controller.GameController;
import mvc.model.GameModel;
import mvc.model.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameView {

    private final GameModel model;
    private final GameController controller;

    @FXML
    private Canvas cells;
    @FXML
    private ListView<Text> ratingList;
    @FXML
    private Button exitButton;
    private ListProperty<Text> ratingListProperty;
    private GraphicsContext graphicsContext;

    private double fieldWidth;
    private double fieldHeight;
    private double cellSize;

    private final static Map<GameModel.CellType, Color> cellColors;
    static {
        cellColors = new HashMap<>();
        cellColors.put(GameModel.CellType.EMPTY, Color.WHITE);
        cellColors.put(GameModel.CellType.MY_HEAD, Color.DARKGREEN);
        cellColors.put(GameModel.CellType.MY_BODY, Color.GREEN);
        cellColors.put(GameModel.CellType.ENEMY_HEAD, Color.DARKBLUE);
        cellColors.put(GameModel.CellType.ENEMY_BODY, Color.MEDIUMBLUE);
        cellColors.put(GameModel.CellType.ZOMBIE_HEAD, Color.GRAY);
        cellColors.put(GameModel.CellType.ZOMBIE_BODY, Color.DARKGRAY);
        cellColors.put(GameModel.CellType.FOOD, Color.RED);
    }

    public GameView(GameModel model) {
        this.model = model;
        controller = model.getController();
    }

    public void initialize() {
        calculateSizes();
        graphicsContext = cells.getGraphicsContext2D();
        ratingListProperty = new SimpleListProperty<>();
        ratingList.itemsProperty().bind(ratingListProperty);
        ratingList.setMouseTransparent(true);
        ratingList.setFocusTraversable(false);
        exitButton.setFocusTraversable(false);
        drawField();
        updatePlayers();
    }

    private void calculateSizes() {
        int cellsX = model.getFieldWidth();
        int cellsY = model.getFieldHeight();
        double maxFieldSize = 560.0;
        cellSize = (int) Math.min(maxFieldSize / cellsX, maxFieldSize / cellsY);
        fieldWidth = cellSize * cellsX;
        fieldHeight = cellSize * cellsY;
        cells.setLayoutX(20 + (maxFieldSize - fieldWidth) / 2);
        cells.setLayoutY(20 + (maxFieldSize - fieldHeight) / 2);
        cells.setWidth(fieldWidth);
        cells.setHeight(fieldHeight);
    }

    public void updatePlayers() {
        List<Text> rating = new ArrayList<>();
        for (Player player : model.getPlayerMap().values()) {
            Text text = new Text(
                    player.getName() + " " + player.getScore()
            );
            rating.add(text);
        }
        ratingListProperty.setValue(FXCollections.observableArrayList(rating));
    }

    public void drawField() {
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
        }
    }

    @FXML
    public void exitApplication() {
        model.destroy();
        Platform.exit();
    }

    public void exitGame(MouseEvent event) throws IOException {
        model.destroy();
        Parent parent = FXMLLoader.load(getClass().getResource("/fxml/menu.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
        parent.requestFocus();
    }
}
