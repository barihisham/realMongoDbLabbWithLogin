


import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import com.mongodb.MongoException;
import javafx.application.Platform;
import javafx.event.Event;

import static javafx.scene.control.Alert.AlertType.*;

/**
 * The controller is responsible for handling user requests and update the view
 * (and in some cases the model).
 *
 * @author anderslm@kth.se
 */
public class Controller{

    private final BooksPane booksView; // view
    private final BooksDbInterface booksDb; // model
    private final LoginDialog loginDialog;
    private final ReviewDialog reviewDialog;

    public Controller(BooksDbInterface booksDb, BooksPane booksView) {
        this.booksDb = booksDb;
        this.booksView = booksView;
        this.loginDialog = new LoginDialog(this);
        this.reviewDialog = new ReviewDialog();
    }

    protected void onSearchSelected(String searchFor, SearchMode mode) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (searchFor != null && searchFor.length() > 0) {
                        List<Book> result = null;
                        switch (mode) {
                            case Title:
                                result = booksDb.searchBooksByTitle(searchFor);
                                break;
                            case ISBN:
                                result = booksDb.searchBooksByISBN(searchFor);
                                break;
                            case Author:
                                result = booksDb.searchBooksByAuthor(searchFor);
                                break;
                            case Rating:
                                result = booksDb.searchBooksByRating(searchFor);
                                break;
                            case Genre:
                                result = booksDb.searchBooksByGenre(searchFor);
                                break;
                            default:
                        }
                        if (result == null || result.isEmpty()) {
                            Platform.runLater(()-> {booksView.showAlertAndWait("No results found.", INFORMATION); });
                        } 
                        else {
                            final List<Book> finalRes = result;
                            Platform.runLater(() -> { booksView.displayBooks(finalRes);});
                        }
                    } 
                    else {
                        Platform.runLater(() -> { booksView.showAlertAndWait("Enter a search string!", WARNING);});
                    }
                    } catch (IOException | MongoException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> { booksView.showAlertAndWait("Database error.",ERROR);});
                    }
                }
        }.start();
    }

    protected void handleAddAuthorExistingBook(String ISBN){

        if(booksDb.isLoggedIn()) {
            Optional<Author> result = booksView.getDialog().getAuthorDialog().showAndWait();
            new Thread() {
                @Override
                public void run() {
                    if (result.isPresent()) {
                        try {
                            booksDb.addAuthorToExistingBook(result.get(), ISBN);
                        } catch (MongoException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> { booksView.showAlertAndWait("Database error.", ERROR); });
                        }
                    }
                }
            }.start();
        }else{
            booksView.showAlertAndWait("You must be logged in to access this functionality", INFORMATION);
        }
    }
    
    
    protected void handleAddBookToDb() {
        if(booksDb.isLoggedIn()) {
            Optional<Book> result = booksView.getDialog().showAndWait();
            new Thread() {
                @Override
                public void run() {
                    if (result.isPresent()) {
                        try {
                            booksDb.addBookToDb(result.get());
                            Platform.runLater(() -> { booksView.showAlertAndWait("Book added!", INFORMATION); });
                        }  catch(com.mongodb.MongoWriteException e) {
                            Platform.runLater(() -> { booksView.showAlertAndWait("Book already exist in database", INFORMATION);});
                        } catch (IOException | MongoException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> { booksView.showAlertAndWait("Database error.", ERROR); });
                        }
                    }
                }
            }.start();
        }else{
            booksView.showAlertAndWait("You must be logged in to access this functionality", INFORMATION);
        }
    }


    protected void handleLogoutAuthorizedUser(){
        try {
            booksDb.logoutAuthorizedUser();
        } catch (MongoException e) {
            booksView.showAlertAndWait("Database error", WARNING);
            e.printStackTrace();
        }
    }

    protected boolean handleVerifyAccountExist(User user){
        boolean accountExist = false;
        accountExist = new Callable<Boolean>(){
                @Override
                public Boolean call() {
                    try {
                        if(!booksDb.verifyAccount(user)){
                            Platform.runLater(()-> booksView.showAlertAndWait("Wrong username/password", WARNING));
                            return false;
                        }
                        else{
                            return true;
                        }
                    } catch (MongoException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> { booksView.showAlertAndWait("Database error.", ERROR);});
                    }
                    return true;
                }
            }.call();
        return accountExist;
    }

    protected void handleLogin(){
        Optional<User> result = this.loginDialog.showAndWait();
    }

    protected void handleAddReview(String ISBN){
        if(booksDb.isLoggedIn()) {
            Optional<String> result = this.reviewDialog.showAndWait();
            new Thread() {
                @Override
                public void run() {
                    if (result.isPresent()) {
                        try {
                            booksDb.addReview(ISBN, result.get());
                        } catch(com.mongodb.MongoWriteException e){
                            Platform.runLater(()-> booksView.showAlertAndWait("You have already written a review", WARNING));
                        }
                        catch (MongoException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Platform.runLater(()-> booksView.showAlertAndWait("You must enter text", WARNING));
                    }
                }
            }.start();
        }else{
            booksView.showAlertAndWait("You must be logged in to access this functionality", INFORMATION);
        }
    }

    protected void handleWrongInput(String wrongInputText){
        booksView.showAlertAndWait(wrongInputText, WARNING);
    }


    protected void handleDeleteBook(String ISBN){
       if(booksDb.isLoggedIn()) {
           new Thread() {
               @Override
               public void run() {
                   try {
                       booksDb.deleteBook(ISBN);
                       Platform.runLater(()-> booksView.showAlertAndWait("Book deleted!", INFORMATION));
                   } catch (MongoException e) {
                       e.printStackTrace();
                       Platform.runLater(() -> { booksView.showAlertAndWait("Database error.", ERROR); });
                   }
               }
           }.start();
       }
       else{
           booksView.showAlertAndWait("You must be logged in to access this functionality", INFORMATION);
       }
    }


    protected void handleConnectToDb(){
        try {
            booksDb.connect();
        } catch (IOException e) {
            Platform.runLater(() -> { booksView.showAlertAndWait("Database error, could not connect to database", ERROR); });
            e.printStackTrace();
        }
    }

    protected void handleDisconnectToDb(){
        try {
            booksDb.disconnect();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> { booksView.showAlertAndWait("Database error.", ERROR); });
        }
    }


    // TODO:
    // Add methods for all types of user interaction (e.g. via  menus).

}
