package uk.ac.soton.comp1206.scene;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.game.Multimedia;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import javafx.event.ActionEvent;

import java.security.Key;
import java.util.*;

public class LobbyScene extends BaseScene {

    private static final Logger logger = LogManager.getLogger(LobbyScene.class);

    private Communicator communicator = new Communicator("ws://discord.ecs.soton.ac.uk:9700");

    private BorderPane mainPane;

    //storing current games
    private List<String> arrList;

    private StackPane stack;

    private String username;

    private ObservableList<String> observableUsers = FXCollections.observableArrayList();
    private ListProperty<String> userList = new SimpleListProperty<>(observableUsers);

    private VBox messages = new VBox(10);

    private Timer getGamesTimer;

    private boolean isHost = false;

    public LobbyScene(GameWindow gameWindow) {
        super(gameWindow);
    }

    @Override
    public void initialise() {

    }

    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var menuPane = new StackPane();
        menuPane.setMaxWidth(gameWindow.getWidth());
        menuPane.setMaxHeight(gameWindow.getHeight());
        menuPane.getStyleClass().add("menu-background");
        root.getChildren().add(menuPane);

        mainPane = new BorderPane();
        menuPane.getChildren().add(mainPane);

        communicator.addListener(this::receiveCommunication);

        //set title text
        var text = new Text("Current Games");
        text.getStyleClass().add("scoreTitle");
        mainPane.setTop(text);

        getActiveGames();

        activeGameTimer();

        //handles esc key press to go back
        gameWindow.setOnKeyPressed(this::keyPressed);
    }

    /**
     * displays the user's options in the UI
     * |->host a new game or join an existing channel
     */
    public void options() {
        //vbox that holds current games
        var currentGames = new VBox(20);
        for (String channel: arrList) {
            var text = new Text(channel);
            text.getStyleClass().add("channelItem");
            text.setOnMouseClicked((e) -> {
                String game = text.getText();
                joinGame(game);
                getUsers();
            });

            currentGames.getChildren().add(text);
        }

        //host game
        var hostGame = new Text("Host a new game");
        hostGame.getStyleClass().add("hostGame");
        hostGame.setOnMouseClicked((e) -> {
            getGamesTimer.cancel();
            var name = new TextField();
            name.getStyleClass().add("scorer");
            name.setMinSize(200, 50);
            currentGames.getChildren().add(name);

            name.setOnKeyPressed((event) -> {
                if (event.getCode() == KeyCode.ENTER) {
                    activeGameTimer();
                    currentGames.getChildren().remove(name);
                    hostGame(name.getText());
                    getUsers();
                }
            });
        });
        currentGames.getChildren().add(hostGame);

        mainPane.setLeft(currentGames);
        logger.info("displaying current games");
    }

    /**
     * handles messages from the server
     */
    public void receiveCommunication(String message) {
        //receiving active games
        if (message.contains("CHANNELS")) {
            var message1 = message.replaceFirst("CHANNELS ", "");
            var split = message1.split("\n");
            arrList = new ArrayList<>();
            Collections.addAll(arrList, split);
            /**
             * maybe call options here
             */
            Platform.runLater(() -> options());
        }

        //when joining or hosting a game is requested
        if (message.contains("JOIN ")) {
            Platform.runLater(() -> displayChatBox());
            userList.clear();
        }

        //when hosting a game
        if (message.contains("HOST")) {
            isHost = true;
        }

        //changing nickname
        if (message.contains("NICK")) {
            var message1 = message.replaceFirst("NICK ", "");
            if (message.contains(":")) {
                var split = message1.split(":");
                username = split[1];
            } else {
                username = message1;
            }
        }

        //user list
        if (message.contains("USERS")) {
            var message1 = message.replaceFirst("USERS ", "");
            var users = message1.split("\n");
            for (String user : users) {
                logger.info("User from comm: " + user);
            }
            userList.clear();
            Collections.addAll(userList, users);
            for (String user : userList) {
                logger.info("User list contains: " + user);
            }
        }

        //when leaving a channel
        if (message.contains("PARTED")) {
            Platform.runLater(() -> clearChatBox());
        }

        //when receiving a chat message -- player:mssg
        if (message.contains("MSG")) {
            var message1 = message.replaceFirst("MSG ", "");
            var split = message1.split(":");
            Platform.runLater(() -> receiveMessage(split[0], split[1]));
        }

        //when starting a game
        if (message.contains("START")) {
            Platform.runLater(() -> enterGame());
        }
    }

    /**
     * sends message to communicator to get active games
     */
    public void getActiveGames() {
        communicator.send("LIST");
    }

    /**
     * sends message to communicator requesting for users in channel
     */
    public void getUsers() {
        communicator.send("USERS");
    }

    /**
     * sends message to communicator to join a game
     */
    public void joinGame(String game) {
        logger.info("Joining " + game + " Channel");
        communicator.send("JOIN " + game);
    }

    /**
     * sends message to communicator to host a game
     */
    public void hostGame(String game) {
        logger.info("Hosting " + game);
        communicator.send("CREATE " + game);
    }

    /**
     * uses stack pane to have a chat and user list within a box
     */
    public void displayChatBox() {
        logger.info("Drawing box border");

        //white rectangle that acts as the border
        var border = new Rectangle(300, 500);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.WHITE);

        //vbox to hold messages
        messages.getStyleClass().add("messages");
        messages.setAlignment(Pos.TOP_LEFT);
        messages.setPadding(new Insets(80, 10, 50, 10));

        var scroller = new ScrollPane();
        scroller.getStyleClass().add("scroller");
        scroller.setContent(messages);
        scroller.setFitToWidth(true);

        //textfield at bottom of the box
        var chat = new TextField();
        chat.getStyleClass().add("scorer");
        chat.prefWidthProperty().bind(border.widthProperty());
        chat.setOnKeyPressed((ev) -> {
            if (ev.getCode() == KeyCode.ENTER) {
                if (chat.getText().contains("/nick")) {
                    var message1 = chat.getText().replaceFirst("/nick ", "");
                    communicator.send("NICK " + message1);
                }
                else if (chat.getText().contains("/leave")) {
                    logger.info("Leaving game");
                    isHost = false;
                    communicator.send("PART");
                }
                else if (chat.getText().contains("/start") && isHost) {
                    logger.info("Starting game");
                    communicator.send("START");
                }
                else {
                    communicator.send("MSG " + chat.getText());
                }
                chat.clear();
            }
        });

        //instructions text
        var instructions = new Text("Type /nick <NEW_NAME> to change \n username \n Type /leave to leave");
        instructions.getStyleClass().add("instructions");
        messages.getChildren().add(instructions);
        if (isHost) {
            var hostText = new Text("Type /start to begin game");
            hostText.getStyleClass().add("instructions");
            messages.getChildren().add(hostText);
        }

        //users list
        var users = new Text();
        users.getStyleClass().add("users");
        users.textProperty().bind(userList.asString());
        messages.getChildren().add(users);

        stack = new StackPane();
        stack.getChildren().addAll(chat, border, scroller);
        stack.setAlignment(Pos.BOTTOM_CENTER);

        mainPane.setRight(stack);
    }

    /**
     * handles chat messages from the server
     * @param username
     * @param message
     */
    public void receiveMessage(String username, String message) {
        var mssg = new Text(username + ": " + message);
        mssg.getStyleClass().add("messages Text");
        messages.getChildren().add(mssg);
        Multimedia.playAudio("message.wav");
    }

    /**
     * loop that searches for active games
     */
    public void activeGameTimer() {
        getGamesTimer = new Timer();
        var gamesTask = new TimerTask() {
            @Override
            public void run() {
                getActiveGames();
                getGamesTimer.cancel();
                activeGameTimer();
            }
        };
        getGamesTimer.schedule(gamesTask, 3000);

    }

    /**
     * handles key events in the scene
     */
    public void keyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            logger.info("Returning to menu");
            communicator.send("QUIT");
            if (getGamesTimer != null) {
                getGamesTimer.cancel();
            }
            gameWindow.startMenu();
        }
    }

    /**
     * when chat is left clears the previous chat box
     */
    public void clearChatBox() {
        logger.info("leaving channel");
        mainPane.getChildren().remove(mainPane.getRight());
        stack.getChildren().clear();
        messages.getChildren().clear();
        userList.clear();
    }

    /**
     * when game started stop the searching for channels
     */

    /**
     * opens the next scene
     */
    public void enterGame() {
        if (getGamesTimer != null) {
            getGamesTimer.cancel();
        }
        gameWindow.startMultiplayerGame(communicator);
    }
}
