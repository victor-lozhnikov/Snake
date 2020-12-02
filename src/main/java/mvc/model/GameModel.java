package mvc.model;

import javafx.application.Platform;
import main.java.net.protocol.SnakesProto;
import mvc.controller.GameController;
import mvc.view.GameView;
import net.client.AnnouncementPinger;
import net.client.UnicastReceiver;
import net.client.UnicastSender;
import net.protocol.Constants;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class GameModel {

    private GameController controller;
    private GameView gameView;

    private int fieldWidth;
    private int fieldHeight;
    private int foodStatic;
    private float foodPerPlayer;
    private int stateDelay;
    private float deadFoodProb;
    private int pingDelay;
    private int nodeTimeout;

    private int myId = -1;
    private Map<Integer, SnakesProto.GamePlayer> gamePlayers;
    private Map<Integer, Snake> snakeMap;
    private List<int[]> food;
    private Queue<Map.Entry<Integer, SnakesProto.GameMessage.SteerMsg>> steerMsgQueue;
    private int lastMsgSeq = 1;
    private int lastId = 1;
    private int stateOrder = 1;

    private SnakesProto.GameConfig gameConfig;
    private SnakesProto.NodeRole myNodeRole;
    private Timer announcementPinger;
    private UnicastSender unicastSender;
    private Thread unicastSenderThread;
    private UnicastReceiver unicastReceiver;
    private Thread unicastReceiverThread;
    private DatagramSocket unicastSocket;
    private GameStateUpdater gameStateUpdater;
    private Timer gameStateUpdaterTimer;

    private InetAddress masterInetAddress;
    private int masterPort;

    public enum CellType {
        EMPTY,
        MY_HEAD,
        MY_BODY,
        ENEMY_HEAD,
        ENEMY_BODY,
        FOOD
    }

    private CellType[][] cells;

    public GameModel(int fieldWidth, int fieldHeight, int foodStatic, float foodPerPlayer,
                     int stateDelay, float deadFoodProb, int pingDelay, int nodeTimeout,
                     SnakesProto.NodeRole nodeRole, String name) throws IOException {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.foodStatic = foodStatic;
        this.foodPerPlayer = foodPerPlayer;
        this.stateDelay = stateDelay;
        this.deadFoodProb = deadFoodProb;
        this.pingDelay = pingDelay;
        this.nodeTimeout = nodeTimeout;
        this.myNodeRole = nodeRole;

        initGameConfig();
        initFields();
        initUnicastClient();

        announcementPinger = new Timer();
        announcementPinger.schedule(new AnnouncementPinger(this),
                0, Constants.ANNOUNCEMENT_PING_PERIOD);

        createFirstPlayer(name);
        addFood(foodStatic + (int) foodPerPlayer);

        if (nodeRole == SnakesProto.NodeRole.MASTER) {
            gameStateUpdater = new GameStateUpdater(this);
            gameStateUpdaterTimer = new Timer();
            gameStateUpdaterTimer.schedule(gameStateUpdater, stateDelay, stateDelay);
        }
    }

    public GameModel (String name, InetSocketAddress masterAddress, SnakesProto.GameConfig config)
            throws IOException {
        gameConfig = config;
        this.fieldWidth = config.getWidth();
        this.fieldHeight = config.getHeight();
        this.foodStatic = config.getFoodStatic();
        this.foodPerPlayer = config.getFoodPerPlayer();
        this.stateDelay = config.getStateDelayMs();
        this.deadFoodProb = config.getDeadFoodProb();
        this.pingDelay = config.getPingDelayMs();
        this.nodeTimeout = config.getNodeTimeoutMs();

        this.masterInetAddress = masterAddress.getAddress();
        this.masterPort = masterAddress.getPort();

        initUnicastClient();
        initFields();

        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.JoinMsg.Builder joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder();
        joinMsg.setName(name);
        gameMessageBuilder.setJoin(joinMsg);
        gameMessageBuilder.setMsgSeq(lastMsgSeq);
        iterateLastMsqSeq();
        unicastSender.sendMessage(gameMessageBuilder.build(), masterInetAddress, masterPort);
    }

    private void initFields() {
        controller = new GameController(this);
        cells = new CellType[fieldWidth][fieldHeight];
        clearField();
        snakeMap = new ConcurrentHashMap<>();
        steerMsgQueue = new ConcurrentLinkedDeque<>();
        food = new ArrayList<>();
        gamePlayers = new ConcurrentHashMap<>();

    }

    private void initUnicastClient() throws IOException {
        unicastSocket = new DatagramSocket(0);
        unicastSocket.setSoTimeout(Constants.UNICAST_SOCKET_TIMEOUT);
        unicastSender = new UnicastSender(this);
        unicastSenderThread = new Thread(unicastSender);
        unicastSenderThread.start();
        unicastReceiver = new UnicastReceiver(this);
        unicastReceiverThread = new Thread(unicastReceiver);
        unicastReceiverThread.start();
    }

    private void fillCells() {
        clearField();
        for (Snake snake : snakeMap.values()) {
            addSnakeBodyToField(snake);
        }
        for (Snake snake : snakeMap.values()) {
            addSnakeHeadToField(snake);
        }
        updateFood();
    }

    public int getFieldWidth() {
        return fieldWidth;
    }

    public int getFieldHeight() {
        return fieldHeight;
    }

    private Snake findPlaceAndCreateSnake(int id) {
        int stX = -1;
        int stY = -1;
        for (int i = 0; i <= fieldWidth - 5; ++i) {
            for (int j = 0; j <= fieldHeight - 5; ++j) {
                boolean ok = true;
                for (int ii = i; ii < i + 5; ++ii) {
                    for (int jj = j; jj < j + 5; ++jj) {
                        if (cells[ii][jj] != CellType.EMPTY) {
                            ok = false;
                            break;
                        }
                    }
                }
                if (ok) {
                    stX = i;
                    stY = j;
                    break;
                }
            }
            if (stX >= 0) {
                break;
            }
        }
        if (stX < 0) {
            return null;
        }

        return new Snake(this, id, stX + 2, stY + 2,
                SnakesProto.Direction.values()[(new Random()).nextInt(4)]);
    }

    public void addSnakeBodyToField(Snake snake) {
        List<int[]> points = snake.getKeyPoints();
        int curX = points.get(0)[0];
        int curY = points.get(0)[1];

        CellType type;
        if (snake.getId() == myId) {
            type = CellType.MY_BODY;
        }
        else {
            type = CellType.ENEMY_BODY;
        }
        for (int i = 1; i < points.size(); ++i) {
            if (points.get(i)[0] > 0) {
                for (int j = 1; j <= points.get(i)[0]; ++j) {
                    cells[Math.floorMod(curX + j, fieldWidth)][Math.floorMod(curY, fieldHeight)] = type;
                }
                curX += points.get(i)[0];
            }
            if (points.get(i)[0] < 0) {
                for (int j = -1; j >= points.get(i)[0]; --j) {
                    cells[Math.floorMod(curX + j, fieldWidth)][Math.floorMod(curY, fieldHeight)] = type;
                }
                curX += points.get(i)[0];
            }
            if (points.get(i)[1] > 0) {
                for (int j = 1; j <= points.get(i)[1]; ++j) {
                    cells[Math.floorMod(curX, fieldWidth)][Math.floorMod(curY + j, fieldHeight)] = type;
                }
                curY += points.get(i)[1];
            }
            if (points.get(i)[1] < 0) {
                for (int j = -1; j >= points.get(i)[1]; --j) {
                    cells[Math.floorMod(curX, fieldWidth)][Math.floorMod(curY + j, fieldHeight)] = type;
                }
                curY += points.get(i)[1];
            }
        }

    }

    public void addSnakeHeadToField(Snake snake) {
        List<int[]> points = snake.getKeyPoints();
        CellType type;
        if (snake.getId() == myId) {
            type = CellType.MY_HEAD;
        }
        else {
            type = CellType.ENEMY_HEAD;
        }
        if (cells[points.get(0)[0]][points.get(0)[1]] == CellType.EMPTY) {
            cells[points.get(0)[0]][points.get(0)[1]] = type;
        }
        else {
            //TODO: remove snake(s)
            return;
        }
    }

    private void addFood(int count) {
        List<int[]> emptyCells = getEmptyCells();
        if (emptyCells.isEmpty()) {
            return;
        }
        int toAdd = Math.min(count, emptyCells.size());
        Random random = new Random();
        for (int i = 0; i < toAdd; ++i) {
            int j = random.nextInt(emptyCells.size());
            food.add(new int[] {emptyCells.get(j)[0], emptyCells.get(j)[1]});
            cells[emptyCells.get(j)[0]][emptyCells.get(j)[1]] = CellType.FOOD;
            emptyCells.remove(j);
        }
    }

    public void updateFood() {
        for (int[] f : food) {
            cells[f[0]][f[1]] = CellType.FOOD;
        }
    }

    public void addSingleFood() {
        List<int[]> emptyCells = getEmptyCells();
        if (emptyCells.isEmpty()) {
            return;
        }
        Random random = new Random();
        int j = random.nextInt(emptyCells.size());
        food.add(new int[] {emptyCells.get(j)[0], emptyCells.get(j)[1]});
        cells[emptyCells.get(j)[0]][emptyCells.get(j)[1]] = CellType.FOOD;
    }

    public List<int[]> getEmptyCells() {
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < fieldWidth; ++i) {
            for (int j = 0; j < fieldHeight; ++j) {
                if (cells[i][j] == CellType.EMPTY) {
                    result.add(new int[] {i, j});
                }
            }
        }
        return result;
    }

    public CellType getCellTypeByCoordinates(int x, int y) {
        return cells[x][y];
    }

    public void clearField() {
        for (int i = 0; i < fieldWidth; ++i) {
            for (int j = 0; j < fieldHeight; ++j) {
                cells[i][j] = CellType.EMPTY;
            }
        }
    }

    public int getFoodStatic() {
        return foodStatic;
    }

    public float getFoodPerPlayer() {
        return foodPerPlayer;
    }

    public int getStateDelay() {
        return stateDelay;
    }

    public float getDeadFoodProb() {
        return deadFoodProb;
    }

    public int getPingDelay() {
        return pingDelay;
    }

    public int getNodeTimeout() {
        return nodeTimeout;
    }

    public GameController getController() {
        return controller;
    }

    public CellType[][] getCells() {
        return cells;
    }

    public void removeFood(int x, int y) {
        for (int i = 0; i < food.size(); ++i) {
            if (food.get(i)[0] == x && food.get(i)[1] == y) {
                food.remove(i);
                break;
            }
        }
    }

    private void initGameConfig() {
        SnakesProto.GameConfig.Builder gameConfigBuilder = SnakesProto.GameConfig.newBuilder();
        gameConfigBuilder.setWidth(fieldWidth);
        gameConfigBuilder.setHeight(fieldHeight);
        gameConfigBuilder.setFoodStatic(foodStatic);
        gameConfigBuilder.setFoodPerPlayer(foodPerPlayer);
        gameConfigBuilder.setStateDelayMs(stateDelay);
        gameConfigBuilder.setDeadFoodProb(deadFoodProb);
        gameConfigBuilder.setPingDelayMs(pingDelay);
        gameConfigBuilder.setNodeTimeoutMs(nodeTimeout);
        gameConfig = gameConfigBuilder.build();
    }

    public SnakesProto.GameConfig getGameConfig() {
        return gameConfig;
    }

    public SnakesProto.GamePlayers getGamePlayers() {
        SnakesProto.GamePlayers.Builder gamePlayersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (SnakesProto.GamePlayer player : gamePlayers.values()) {
            gamePlayersBuilder.addPlayers(player);
        }
        return gamePlayersBuilder.build();
    }

    private void createFirstPlayer(String name) {
        myId = 1;
        addNewGamePlayer(findPlaceAndCreateSnake(myId), myId, name,
                "", unicastSocket.getPort(), SnakesProto.NodeRole.MASTER);
    }

    private void addNewGamePlayer(Snake snake, int id, String name,
                                 String ip, int port, SnakesProto.NodeRole nodeRole) {
        SnakesProto.GamePlayer.Builder gamePlayerBuilder = SnakesProto.GamePlayer.newBuilder();
        gamePlayerBuilder.setName(name);
        gamePlayerBuilder.setId(id);
        gamePlayerBuilder.setIpAddress(ip);
        gamePlayerBuilder.setPort(port);
        gamePlayerBuilder.setRole(nodeRole);
        gamePlayerBuilder.setScore(0);
        gamePlayers.put(id, gamePlayerBuilder.build());
        snakeMap.put(id, snake);
        addSnakeBodyToField(snake);
        addSnakeHeadToField(snake);
    }

    public int getMyId() {
        return myId;
    }

    public void setMyId(int myId) {
        this.myId = myId;
    }

    public SnakesProto.NodeRole getNodeRole() {
        return myNodeRole;
    }

    public void addNewSteerMsg(int id, SnakesProto.GameMessage.SteerMsg msg) {
        steerMsgQueue.add(new AbstractMap.SimpleEntry<>(id, msg));
    }

    public UnicastSender getUnicastSender() {
        return unicastSender;
    }

    public int getLastMsgSeq() {
        return lastMsgSeq;
    }

    public void iterateLastMsqSeq() {
        lastMsgSeq++;
    }

    public DatagramSocket getUnicastSocket() {
        return unicastSocket;
    }

    public Queue<Map.Entry<Integer, SnakesProto.GameMessage.SteerMsg>> getSteerMsgQueue() {
        return steerMsgQueue;
    }

    public void clearSteerMsgQueue() {
        steerMsgQueue.clear();
    }

    public Snake getSnakeById (int id) {
        return snakeMap.get(id);
    }

    public Map<Integer, Snake> getSnakeMap() {
        return snakeMap;
    }

    public GameView getGameView() {
        return gameView;
    }

    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }

    public int getStateOrder() {
        return stateOrder;
    }

    public void iterateStateOrder() {
        stateOrder++;
    }

    public List<int[]> getFood() {
        return food;
    }

    public InetAddress getMasterInetAddress() {
        return masterInetAddress;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public Map<Integer, SnakesProto.GamePlayer> getGamePlayersMap() {
        return gamePlayers;
    }

    public int tryJoin (String name, InetAddress address, int port) {
        Snake newSnake = findPlaceAndCreateSnake(lastId + 1);
        if (newSnake == null) {
            return -1;
        }
        addNewGamePlayer(newSnake, lastId + 1, name, address.getHostName(), port, SnakesProto.NodeRole.NORMAL);
        lastId++;
        return lastId;
    }

    public void setState(SnakesProto.GameState state) {
        if (state.getStateOrder() <= stateOrder) {
            return;
        }

        stateOrder = state.getStateOrder();

        snakeMap.clear();
        for (SnakesProto.GameState.Snake snake : state.getSnakesList()) {
            snakeMap.put(snake.getPlayerId(), new Snake(snake, this));
        }

        food.clear();
        for (SnakesProto.GameState.Coord foodCoords : state.getFoodsList()) {
            food.add(new int[] {foodCoords.getX(), foodCoords.getY()});
        }

        gamePlayers.clear();
        for (SnakesProto.GamePlayer player : state.getPlayers().getPlayersList()) {
            gamePlayers.put(player.getId(), player);
        }

        fillCells();

        if (gameView != null) {
            Platform.runLater(() -> gameView.drawField());
        }
    }

    public void destroy() {
        if (announcementPinger != null) {
            announcementPinger.cancel();
        }
        if (gameStateUpdaterTimer != null) {
            gameStateUpdaterTimer.cancel();
        }
        unicastSenderThread.interrupt();
        unicastReceiverThread.interrupt();
        unicastSocket.close();
    }
}
