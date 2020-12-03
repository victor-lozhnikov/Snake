package mvc.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class MenuView {

    public void newGameButtonClicked(MouseEvent event) throws IOException {
        Parent parent = FXMLLoader.load(getClass().getResource("/fxml/new_game.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
        parent.requestFocus();
    }

    public void gamesListButtonClicked(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/games_list.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> ((GamesListView) loader.getController()).exitApplication());
        stage.show();
        parent.requestFocus();
    }

    public void exitButtonClicked(MouseEvent event) {
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}
