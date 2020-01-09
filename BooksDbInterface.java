

import com.mongodb.MongoException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * This interface declares methods for querying a Books database.
 * Different implementations of this interface handles the connection and
 * queries to a specific DBMS and database, for example a MySQL or a MongoDB
 * database.
 * 
 * @author anderslm@kth.se
 */
public interface BooksDbInterface {
    
    /**
     * Connect to the database.
     * @param database
     * @return true on successful connection.
     */
    public void connect() throws IOException, MongoException;
    
    public void disconnect() throws IOException, SQLException;
    
    public List<Book> searchBooksByTitle(String title) throws IOException, MongoException;
    
    public List<Book> searchBooksByISBN(String ISBN) throws IOException, MongoException;
    
    public List<Book> searchBooksByAuthor(String Author) throws IOException, MongoException;
    
    public List<Book> searchBooksByRating(String Rating) throws IOException, MongoException;
    
    public List<Book> searchBooksByGenre(String Genre) throws IOException, MongoException;
    
    public void addBookToDb(Book book) throws IOException, MongoException;

    public void addAuthorToExistingBook(Author author, String ISBN) throws MongoException;

    public boolean isLoggedIn();

    public User getCurrentUser();

    public boolean verifyAccount(User user) throws  MongoException;

    public void logoutAuthorizedUser() throws MongoException;

    public void addReview(String ISBN, String userText) throws MongoException;

    public void deleteBook(String ISBN) throws MongoException;
    // TODO: Add abstract methods for all inserts, deletes and queries 
    // mentioned in the instructions for the assignement.
}
