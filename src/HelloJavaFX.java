import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloJavaFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Create a UI component
        Button btn = new Button("Say 'Hello World'");
        btn.setOnAction(event -> System.out.println("Hello World!"));

        // 2. Create a layout pane and add the component
        StackPane root = new StackPane();
        root.getChildren().add(btn);

        // 3. Create the Scene (Width, Height)
        Scene scene = new Scene(root, 300, 250);

        // 4. Configure the Stage (The Window)
        primaryStage.setTitle("JavaFX Window");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}