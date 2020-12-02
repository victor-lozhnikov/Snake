package mvc.model;

import main.java.net.protocol.SnakesProto;
import mvc.controller.GameController;
import mvc.view.GameView;
import net.client.AnnouncementPinger;
import net.client.UnicastSender;
import net.protocol.Constants;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class GameModel {

    private final GameController controller;
    private GameView gameView;

    private final int fieldWidth;
    private final int fieldHeight;
    private final int foodStatic;
    private final float foodPerPlayer;
    private final int stateDelay;
    private final float deadFoodProb;
    private final int pingDelay;
    private final int nodeTimeout;

    private int myId;
    private Map<Integer, SnakesProto.GamePlayer> gamePlayers;
    private Map<Integer, Snake> snakeMap;
    private List<int[]> food;
    private Queue<Map.Entry<Integer, SnakesProto.GameMessage.SteerMsg>> steerMsgQueue;
    private int lastMsgSeq = 1;
    private int lastId = 1;

    private SnakesProto.GameConfig gameConfig;
    private SnakesProto.NodeRole nodeRole;
    private Timer announcementPinger;
    private UnicastSender unicastSender;
    private Thread unicastSenderThread;
    private DatagramSocket unicastSocket;
    private GameStateUpdater gameStateUpdater;
    private Timer gameStateUpdaterTimer;

    public enum CellType {
        EMPTY,
        MY_HEAD,
        MY_BODY,
        ENEMY_HEAD,
        ENEMY_BODY,
        FOOD
    }

    private final CellType[][] cells;

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
        this.nodeRole = nodeRole;

        initGameConfig();

        controller = new GameController(this);
        cells = new CellType[fieldWidth][fieldHeight];
        snakeMap = new ConcurrentHashMap<>();
        steerMsgQueue = new ConcurrentLinkedDeque<>();

        unicastSocket = new DatagramSocket(0);
        unicastSender = new UnicastSender(this);
        unicastSenderThread = new Thread(unicastSender);
        unicastSenderThread.start();
        announcementPinger = new Timer();
        announcementPinger.schedule(new AnnouncementPinger(this),
                0, Constants.ANNOUNCEMENT_PING_PERIOD);

        clearField();
        createFirstPlayer(name);
        initFood();

        if (nodeRole == SnakesProto.NodeRole.MASTER) {
            gameStateUpdater = new GameStateUpdater(this);
            gameStateUpdaterTimer = new Timer();
            gameStateUpdaterTimer.schedule(gameStateUpdater, stateDelay, stateDelay);
        }
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
        for (int i = 1; i < points.size(); ++i) {
            if (points.get(i)[0] > 0) {
                for (int j = 1; j <= points.get(i)[0]; ++j) {
                    cells[Math.floorMod(curX + j, fieldWidth)][Math.floorMod(curY, fieldHeight)] = CellType.MY_BODY;
                }
                curX += points.get(i)[0];
            }
            if (points.get(i)[0] < 0) {
                for (int j = -1; j >= points.get(i)[0]; --j) {
                    cells[Math.floorMod(curX + j, fieldWidth)][Math.floorMod(curY, fieldHeight)] = CellType.MY_BODY;
                }
                curX += points.get(i)[0];
            }
            if (points.get(i)[1] > 0) {
                for (int j = 1; j <= points.get(i)[1]; ++j) {
                    cells[Math.floorMod(curX, fieldWidth)][Math.floorMod(curY + j, fieldHeight)] = CellType.MY_BODY;
                }
                curY += points.get(i)[1];
            }
            if (points.get(i)[1] < 0) {
                for (int j = -1; j >= points.get(i)[1]; --j) {
                    cells[Math.floorMod(curX, fieldWidth)][Math.floorMod(curY + j, fieldHeight)] = CellType.MY_BODY;
                }
                curY += points.get(i)[1];
            }
        }

    }

    public void addSnakeHeadToField(Snake snake) {
        List<int[]> points = snake.getKeyPoints();
        if (cells[points.get(0)[0]][points.get(0)[1]] == CellType.EMPTY) {
            cells[points.get(0)[0]][points.get(0)[1]] = CellType.MY_HEAD;
        }
        else {
            //TODO: remove snake(s)
            return;
        }
    }

    public void initFood() {
        food = new ArrayList<>();
        List<int[]> emptyCells = getEmptyCells();
        int toAdd = Math.min(foodStatic + (int) foodPerPlayer, emptyCells.size());
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

    /*public void updateField() {
        mySnake.makeMove(this);
        clearField();
        addSnakeBodyToField(mySnake);
        addSnakeHeadToField(mySnake);
        updateFood();
    }*/

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

    public void initGameConfig() {
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

    public void createFirstPlayer(String name) {
        gamePlayers = new ConcurrentHashMap<>();
        SnakesProto.GamePlayer.Builder gamePlayerBuilder = SnakesProto.GamePlayer.newBuilder();
        gamePlayerBuilder.setName(name);
        gamePlayerBuilder.setId(1);
        gamePlayerBuilder.setIpAddress("");
        gamePlayerBuilder.setPort(unicastSocket.getPort());
        gamePlayerBuilder.setRole(nodeRole);
        gamePlayerBuilder.setScore(0);
        myId = 1;
        gamePlayers.put(myId, gamePlayerBuilder.build());
        snakeMap.put(myId, findPlaceAndCreateSnake(myId));
        addSnakeBodyToField(snakeMap.get(myId));
        addSnakeHeadToField(snakeMap.get(myId));
    }

    public int getMyId() {
        return myId;
    }

    public SnakesProto.NodeRole getNodeRole() {
        return nodeRole;
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

    public Map<Integer, SnakesProto.GamePlayer> getGamePlayersMap() {
        return gamePlayers;
    }

    public void destroy() {
        announcementPinger.cancel();
        unicastSenderThread.interrupt();
        gameStateUpdaterTimer.cancel();
        unicastSocket.close();
    }
}
