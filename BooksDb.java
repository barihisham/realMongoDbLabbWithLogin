

/**
 *
 * This class implements the BooksDBInterface.
 * This class communicates directly with a mongo database.
 * The connect() method establishes a connection to the desired mongo database
 *
 */


import com.mongodb.*;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.io.IOException;
import java.sql.Date;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

public class BooksDb implements BooksDbInterface {


    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> booksCollection;
    private MongoCollection<Document> authorsCollection;
    private MongoCursor<Document> cursor;
    private Document docToFind;
    private MongoCursor<Document> authorCursor;
    private User currentUser = null;
    private boolean loggedIn = false;
    private MongoCollection<Document> collection;


    @Override
    public void connect() throws MongoException {

        this.mongoClient = new MongoClient("localhost", 27017);
        this.database = mongoClient.getDatabase("mylib");
        this.booksCollection = this.database.getCollection("books");
        this.authorsCollection = this.database.getCollection("authors");
    }

    @Override
    public void disconnect() throws MongoException {

        mongoClient.close();
        authorCursor.close();
        cursor.close();
    }

    private List<Book> searchResult(MongoCursor<Document> cursor) {

        List<Book> books = new ArrayList<>();
        Book book;

        while(cursor.hasNext()){
            docToFind = cursor.next();
            book = new Book(docToFind.getString("ISBN"),
                    docToFind.getString("title"),
                    Date.valueOf(docToFind.getString("published")),
                    BookGenre.valueOf(docToFind.getString("genre").toUpperCase()),
                    Integer.parseInt(docToFind.getString("rating")));
            book.addUser(new User(docToFind.getString("addedBy"), "UNKNOWN"));

            book.addReviews(this.getReviewsFromDb());
            books.add(book);

            List<ObjectId> ids = (List<ObjectId>) docToFind.get("authors");
            for(int i = 0; i < ids.size(); i++){
                authorCursor = authorsCollection.find(eq("_id", new ObjectId(ids.get(i).toString()))).iterator();
                while (authorCursor.hasNext()){
                    Document authorDoc = authorCursor.next();
                    book.addAuthor(new Author((authorDoc.getObjectId("_id").toString()),
                            authorDoc.getString("firstName"),
                            authorDoc.getString("lastName"),
                            Date.valueOf(authorDoc.getString("dateOfBirth")),
                            new User(authorDoc.getString("addedBy"), "UNKNOWN")));

                }
            }
        }
        return books;
    }

    private ArrayList<Review> getReviewsFromDb(){

        Document reviewDoc;
        this.collection = this.database.getCollection("reviews");
        MongoCursor<Document> reviewCursor = this.collection.find(eq("ISBN", this.docToFind.getString("ISBN"))).iterator();
        ArrayList<Review> reviews = new ArrayList<>();
        while(reviewCursor.hasNext()){
            reviewDoc = reviewCursor.next();
            reviews.add(new Review(reviewDoc.getString("userText"),
                    Date.valueOf(reviewDoc.getString("reviewDate")),
                    new User(reviewDoc.getString("username"), "UNKNOWN")));
        }
        return reviews;
    }


    @Override
    public List<Book> searchBooksByAuthor(String author) throws MongoException {

        MongoCursor<Document> tempCursor;
        Document tempDoc;
        List<Book> tempBooks = new ArrayList<>();
        author = author.toLowerCase();
        tempCursor = authorsCollection.find(or(eq("firstName", Pattern.compile(author)), eq("lastName", Pattern.compile(author)))).iterator();

        while(tempCursor.hasNext()){
            tempDoc = tempCursor.next();
            cursor = booksCollection.find(eq("authors", tempDoc.get("_id"))).iterator();
            List<Book> resultBooks = this.searchResult(cursor);
            for(int i = 0; i < resultBooks.size();i++){
                if(!tempBooks.contains(resultBooks.get(i))){
                    tempBooks.add(resultBooks.get(i));
                }
            }
        }
        return tempBooks;
    }

    @Override
    public List<Book> searchBooksByTitle(String title) throws MongoException {

        docToFind = new Document("title", Pattern.compile(title.toLowerCase()));
        this.cursor = booksCollection.find(docToFind).iterator();
        return this.searchResult(this.cursor);
    }

    @Override
    public List<Book> searchBooksByISBN(String ISBN) throws MongoException {

        this.docToFind = new Document("ISBN", Pattern.compile(ISBN));
        this.cursor = booksCollection.find(docToFind).iterator();
        return this.searchResult(this.cursor);
    }

    @Override
    public List<Book> searchBooksByRating(String rating) throws MongoException {

        this.docToFind = new Document("rating", rating);
        this.cursor = booksCollection.find(docToFind).iterator();
        return this.searchResult(this.cursor);
    }

    @Override
    public List<Book> searchBooksByGenre(String genre) throws MongoException {

        this.docToFind = new Document("genre", Pattern.compile(genre.toUpperCase()));
        this.cursor = booksCollection.find(docToFind).iterator();
        return this.searchResult(this.cursor);
    }


    private void addAuthorToDb(Author author){

        Document authorDoc = new Document("firstName", author.getFirstName())
                .append("lastName", author.getLastName())
                .append("dateOfBirth", author.getDob().toString())
                .append("addedBy", this.currentUser.getUsername());
        authorsCollection.insertOne(authorDoc);
    }

    @Override
    public void addReview(String ISBN, String userText) throws MongoException {

        Document reviewToInsert = new Document("ISBN", ISBN)
                .append("username", this.currentUser.getUsername())
                .append("reviewDate", new Date(Calendar.getInstance().getTime().getTime()).toString())
                .append("userText", userText);
        this.collection = this.database.getCollection("reviews");
        this.collection.insertOne(reviewToInsert);
    }

    @Override
    public void addAuthorToExistingBook(Author author, String ISBN) throws MongoException {

        if(!this.authorExist(author.getFirstName(), author.getLastName())){
            this.addAuthorToDb(author);
        }
        this.authorCursor = authorsCollection.find(and(eq("firstName", author.getFirstName())
                ,eq("lastName", author.getLastName()))).iterator();
        Document authorDoc = authorCursor.next();
        Document updateArray = new Document("authors",authorDoc.getObjectId("_id"));
        this.booksCollection.updateOne(eq("ISBN", ISBN), new Document().append("$addToSet", updateArray));
    }

    @Override
    public void addBookToDb(Book book) throws MongoException {

        Document author;
        List<ObjectId> authorIds = new ArrayList<>();
            for (int i = 0; i < book.getAuthors().size(); i++) {
                if (!authorExist(book.getAuthors().get(i).getFirstName(), book.getAuthors().get(i).getLastName())) {
                    author = new Document("firstName", book.getAuthors().get(i).getFirstName())
                            .append("lastName", book.getAuthors().get(i).getLastName())
                            .append("dateOfBirth", book.getAuthors().get(i).getDob().toString())
                            .append("addedBy", this.currentUser.getUsername());
                    authorsCollection.insertOne(author);
                    authorIds.add((ObjectId) author.get("_id"));
                } else {
                    this.authorCursor = authorsCollection.find(and(eq("firstName", book.getAuthors().get(i).getFirstName())
                            , eq("lastName", book.getAuthors().get(i).getLastName()))).iterator();
                    authorIds.add((ObjectId) this.authorCursor.next().get("_id"));
                }
            }

        Document document = new Document("ISBN", book.getISBN())
                .append("title", book.getTitle())
                .append("published", book.getPublished().toString())
                .append("genre", book.getGenre().toString())
                .append("rating", Integer.toString(book.getRating()))
                .append("authors", authorIds)
                .append("addedBy", currentUser.getUsername());
        booksCollection.insertOne(document);
    }

    private boolean authorExist(String firstName, String lastName){
        this.authorCursor = authorsCollection.find(and(eq("firstName", firstName), eq("lastName", lastName))).iterator();
        return this.authorCursor.hasNext();
    }

    @Override
    public boolean verifyAccount(User user) throws MongoException {

        this.collection = this.database.getCollection("users");
        this.cursor = collection.find(and(eq("username", user.getUsername()), eq("psw", user.getPassword()))).iterator();
        if(cursor.hasNext()){
            Document userDoc = cursor.next();
            this.currentUser = new User(userDoc.getString("username"), userDoc.getString("psw"));
            loggedIn = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    @Override
    public User getCurrentUser() {
        return this.currentUser;
    }

    @Override
    public void logoutAuthorizedUser() throws MongoException {
        this.loggedIn = false;
        this.currentUser = null;
    }

    @Override
    public void deleteBook(String ISBN) throws MongoException {

        Document docToDelete = new Document("ISBN", ISBN);
        this.booksCollection.findOneAndDelete(docToDelete);
        docToDelete = new Document("ISBN", ISBN);
        this.collection = this.database.getCollection("reviews");
        this.collection.deleteMany(docToDelete);
    }
}