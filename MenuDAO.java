import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// All SQL for the menu_items table.
public class MenuDAO {

    public List<MenuItem> getAll(boolean onlyAvailable) throws SQLException {
        String sql = "SELECT item_id, name, category, price, available FROM menu_items"
                   + (onlyAvailable ? " WHERE available = TRUE" : "")
                   + " ORDER BY category, name";
        List<MenuItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MenuItem(rs.getInt("item_id"), rs.getString("name"),
                        rs.getString("category"), rs.getBigDecimal("price"),
                        rs.getBoolean("available")));
            }
        }
        return list;
    }

    public void add(String name, String category, BigDecimal price) throws SQLException {
        String sql = "INSERT INTO menu_items (name, category, price) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setBigDecimal(3, price);
            ps.executeUpdate();
        }
    }

    public void update(int id, String name, String category, BigDecimal price) throws SQLException {
        String sql = "UPDATE menu_items SET name = ?, category = ?, price = ? WHERE item_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setBigDecimal(3, price);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    // We hide items instead of deleting them, because old orders still point to them.
    public void setAvailable(int id, boolean available) throws SQLException {
        String sql = "UPDATE menu_items SET available = ? WHERE item_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
