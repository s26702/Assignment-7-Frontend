package dk.dtu.compute.se.pisd.roborally.online.view;

import dk.dtu.compute.se.pisd.roborally.online.controller.OnlineController;
import dk.dtu.compute.se.pisd.roborally.online.model.Game;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Provides small modal dialogs for signing in, signing up and creating online games.
 */
public class AppDialogs {

    private OnlineController onlineController;

    /**
     * Creates the dialog helper for the online controller.
     *
     * @param onlineController the controller receiving the dialog results
     */
    public AppDialogs(OnlineController onlineController) {
        this.onlineController = onlineController;
    }

    /**
     * Opens the sign-in dialog and submits the entered name to the backend lookup.
     */
    public void signIn() {
        Stage stage = new Stage();

        Text text = new Text("Register as user for Online RoboRally with a (new) user name.");
        TextField userName = new TextField();

        Button cancel = new Button("Cancel");
        cancel.setOnAction( e -> stage.close() );
        Button register= new Button("Sign in");
        register.setOnAction(
                e -> {
                    String name = userName.getText();
                    if (name.length() >= 4) {
                        stage.close();
                        onlineController.signIn(name);
                    }
                }
        );
        HBox buttons = new HBox(cancel, register);

        VBox vbox = new VBox(text, userName, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Register for Online RoboRally");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    /**
     * Opens the sign-up dialog and submits the entered name as a new backend user.
     */
    public void signUp() {
        Stage stage = new Stage();

        Text text = new Text("Sign up as a new user for Online RoboRally.");
        TextField userName = new TextField();

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button signUp = new Button("Sign up");
        signUp.setOnAction(
                e -> {
                    String name = userName.getText();
                    if (name.length() >= 4) {
                        stage.close();
                        onlineController.signUp(name);
                    }
                }
        );
        HBox buttons = new HBox(cancel, signUp);

        VBox vbox = new VBox(text, userName, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Sign Up for Online RoboRally");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    /**
     * Opens the dialog for choosing which backend server this client uses.
     */
    public void configureBackend() {
        Stage stage = new Stage();

        Text text = new Text("Server address for Online RoboRally.");
        TextField backendUrl = new TextField(onlineController.getBackendUrl());
        backendUrl.setPrefColumnCount(32);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button save = new Button("Save");
        save.setOnAction(
                e -> {
                    onlineController.setBackendUrl(backendUrl.getText());
                    stage.close();
                }
        );
        HBox buttons = new HBox(cancel, save);

        VBox vbox = new VBox(text, backendUrl, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Online Server");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    /**
     * Opens the new-game dialog and sends the configured game to the online controller.
     */
    public void createNewGame() {
        Stage stage = new Stage();

        Text text = new Text("Add the needed data for your a new game.");

        Label labelGameName = new Label("Name:");
        TextField gameName = new TextField();
        HBox nameBox = new HBox(labelGameName,gameName);

        Label minLabel = new Label("min:");
        TextField min = new TextField();
        Label maxLabel = new Label("max:");
        TextField max = new TextField();
        HBox minMaxBox = new HBox(minLabel,min,maxLabel,max);


        Button cancel = new Button("Cancel");
        cancel.setOnAction( e -> stage.close() );
        Button create= new Button("Create");
        create.setOnAction(
                e -> {
                    try {
                        Game game = new Game();
                        game.setName(gameName.getText());
                        game.setMinPlayers(Integer.parseInt(min.getText()));
                        game.setMaxPlayers(Integer.parseInt(max.getText()));
                        onlineController.createGame(game);
                        stage.close();
                    } catch (Exception exception) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Invalid game");
                        alert.setHeaderText("Enter a game name and valid player numbers.");
                        alert.showAndWait();
                    }
                }
        );
        HBox buttons = new HBox(cancel, create);

        VBox vbox = new VBox(text, nameBox, minMaxBox, buttons);

        Scene scene = new Scene(vbox);
        stage.setTitle("Create New Online Game");
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

}
