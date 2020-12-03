package mvc.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import main.java.net.protocol.SnakesProto;
import mvc.model.GameModel;

import java.io.IOException;

public class NewGameView {

    public Slider slider1;
    public Slider slider2;
    public Slider slider3;
    public Slider slider4;
    public Slider slider5;
    public Slider slider6;
    public Slider slider7;
    public Slider slider8;

    public Label label1;
    public Label label2;
    public Label label3;
    public Label label4;
    public Label label5;
    public Label label6;
    public Label label7;
    public Label label8;

    public Button button;

    public TextField nameField;
    public Text nameErrorLabel;

    public void initialize() {
        slider1.setMin(10);
        slider1.setMax(100);
        slider1.setValue(40);
        label1.setText(String.format("%d", Math.round(slider1.getValue())));
        slider1.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label1.setText(String.format("%d", Math.round((double) new_val)));
        });

        slider2.setMin(10);
        slider2.setMax(100);
        slider2.setValue(30);
        label2.setText(String.format("%d", Math.round(slider2.getValue())));
        slider2.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label2.setText(String.format("%d", Math.round((double) new_val)));
        });

        slider3.setMin(0);
        slider3.setMax(100);
        slider3.setValue(1);
        label3.setText(String.format("%d", Math.round(slider3.getValue())));
        slider3.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label3.setText(String.format("%d", Math.round((double) new_val)));
        });

        slider4.setMin(0);
        slider4.setMax(100);
        slider4.setValue(1);
        label4.setText(String.format("%d", Math.round(slider4.getValue())));
        slider4.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label4.setText(String.format("%d", Math.round((double) new_val)));
        });

        slider5.setMin(100);
        slider5.setMax(10000);
        slider5.setValue(1000);
        label5.setText(String.format("%d", roundInt(Math.round(slider5.getValue()), 100)));
        slider5.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label5.setText(String.format("%d", roundInt(Math.round((double) new_val), 100)));
        });

        slider6.setMin(0);
        slider6.setMax(1);
        slider6.setValue(0.1);
        label6.setText(String.format("%.2f", slider6.getValue()).replace(",", "."));
        slider6.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label6.setText(String.format("%.2f", (double) new_val).replace(",", "."));
        });

        slider7.setMin(100);
        slider7.setMax(10000);
        slider7.setValue(100);
        label7.setText(String.format("%d", roundInt(Math.round(slider7.getValue()), 100)));
        slider7.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label7.setText(String.format("%d", roundInt(Math.round((double) new_val), 100)));
        });

        slider8.setMin(100);
        slider8.setMax(10000);
        slider8.setValue(800);
        label8.setText(String.format("%d", roundInt(Math.round(slider8.getValue()), 100)));
        slider8.valueProperty().addListener((observableValue, old_val, new_val) -> {
            label8.setText(String.format("%d", roundInt(Math.round((double) new_val), 100)));
        });
    }

    public long roundInt(long val, int del) {
        long rem = val % del;
        if (rem > del / 2) {
            val += del - rem;
        }
        else {
            val -= rem;
        }
        return val;
    }

    public void startNewGame(MouseEvent event) throws IOException {
        if (nameField.getText().isBlank()) {
            nameErrorLabel.setText("!");
            return;
        }

        GameModel model = new GameModel(
                Integer.parseInt(label1.getText()),
                Integer.parseInt(label2.getText()),
                Integer.parseInt(label3.getText()),
                Float.parseFloat(label4.getText()),
                Integer.parseInt(label5.getText()),
                Float.parseFloat(label6.getText()),
                Integer.parseInt(label7.getText()),
                Integer.parseInt(label8.getText()),
                SnakesProto.NodeRole.MASTER,
                nameField.getText()
        );

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game_view.fxml"));
        GameView gameView = new GameView(model);
        model.setGameView(gameView);
        loader.setControllerFactory(c -> gameView);
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> ((GameView) loader.getController()).exitApplication());
        stage.show();
        parent.requestFocus();
    }

    public void returnBack(MouseEvent event) throws IOException {
        Parent parent = FXMLLoader.load(getClass().getResource("/fxml/menu.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
        parent.requestFocus();
    }
}
