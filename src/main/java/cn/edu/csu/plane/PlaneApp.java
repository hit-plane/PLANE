package cn.edu.csu.plane;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class PlaneApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();

        Scene scene = new Scene(root, 900, 700);
        stage.setTitle("Plane");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
