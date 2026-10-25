import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// One place that knows how to reach MySQL. Every DAO calls getConnection().
public class DBConnection {
    // allowPublicKeyRetrieval + useSSL=false avoid a common login error on a LOCAL MySQL 8 server.
    private static final String URL =
        "jdbc:mysql://localhost:3306/food_delivery?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "your_mysql_password";   // <-- change this

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
