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
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import main.java.net.protocol.SnakesProto;
import net.client.MulticastReceiver;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GamesListView {
    @FXML
    private ListView<Text> gameListView;
    private ListProperty<Text> gameListProperty;
    private MulticastReceiver multicastReceiver;
    private Thread multicastReceiverThread;
    private Map<Text, SocketAddress> addressMap;

    public void initialize() {
        try {
            multicastReceiver = new MulticastReceiver(this);
            multicastReceiverThread = new Thread(multicastReceiver);
            multicastReceiverThread.start();
            gameListProperty = new SimpleListProperty<>();
            gameListView.itemsProperty().bind(gameListProperty);
            addressMap = new ConcurrentHashMap<>();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void update() {
        List<Text> gameList = new ArrayList<>();
        for (Map.Entry<SocketAddress, SnakesProto.GameMessage.AnnouncementMsg> entry :
                multicastReceiver.getCurrentAnnouncements().entrySet()) {
            Text text = new Text(
                    entry.getKey() + "\n" +
                            "Поле: " + entry.getValue().getConfig().getWidth() + "x" +
                            entry.getValue().getConfig().getHeight() + "\n" +
                            "Игроков: " + entry.getValue().getPlayers().getPlayersCount() + "\n" +
                            "Задержка: " + entry.getValue().getConfig().getStateDelayMs() + " мс"
                    );
            gameList.add(text);
            addressMap.put(text, entry.getKey());
        }
        gameListProperty.setValue(FXCollections.observableArrayList(gameList));
    }

    public void returnBack(MouseEvent event) throws IOException {
        multicastReceiverThread.interrupt();
        Parent parent = FXMLLoader.load(getClass().getResource("/fxml/menu.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
        parent.requestFocus();
    }

    @FXML
    public void exitApplication() {
        multicastReceiverThread.interrupt();
        Platform.exit();
    }

    public void onMouseClicked(MouseEvent event) {
        Text selectedText = gameListView.getSelectionModel().getSelectedItem();
        if (selectedText == null) return;

    }
}
