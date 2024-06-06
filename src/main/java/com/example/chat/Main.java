package com.example.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load Manager FXML
        FXMLLoader managerLoader = new FXMLLoader(getClass().getResource("/com/example/chat/manager.fxml"));
        AnchorPane managerPane = managerLoader.load();
        Stage managerStage = new Stage();
        managerStage.setTitle("Manager");
        managerStage.setScene(new Scene(managerPane));
        managerStage.show();

//        // Load Client FXML
//        FXMLLoader clientLoader = new FXMLLoader(getClass().getResource("/com/example/chat/client.fxml"));
//        AnchorPane clientPane = clientLoader.load();
//        Stage clientStage = new Stage();
//        clientStage.setTitle("Client");
//        clientStage.setScene(new Scene(clientPane));
//        clientStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}