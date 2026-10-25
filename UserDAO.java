import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// DAO = Data Access Object: the only class that writes SQL for the users table.
public class UserDAO {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Customer c) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, phone, address) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getUsername());
            ps.setString(2, c.getPasswordHash());
            ps.setString(3, c.getSalt());
            ps.setString(4, c.getFullName());
            ps.setString(5, c.getPhone());
            ps.setString(6, c.getAddress());
            ps.executeUpdate();
        }
    }

    // Returns null if there is no such user.
    public Customer findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, salt, full_name, phone, address, role "
                   + "FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Customer(
                    rs.getInt("user_id"), rs.getString("username"),
                    rs.getString("password_hash"), rs.getString("salt"),
                    rs.getString("full_name"), rs.getString("phone"),
                    rs.getString("address"), rs.getString("role"));
            }
        }
    }
}
