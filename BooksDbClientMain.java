


import java.io.IOException;
import java.sql.SQLException;

import com.mongodb.MongoException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application start up.
 *
 * @author anderslm@kth.se
 */
public class BooksDbClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws MongoException, IOException {

        BooksDb booksDb = new BooksDb(); // model
        
        BooksPane root = new BooksPane(booksDb);

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Books Database Client");
        // add an exit handler to the stage (X) ?
        primaryStage.setOnCloseRequest(event -> {
            try {
                booksDb.disconnect();
            } catch (Exception e) {}
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
