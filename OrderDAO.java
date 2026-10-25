import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// All SQL for orders and order_items.
public class OrderDAO {

    // Saves the order header and all its lines as ONE transaction:
    // either everything is saved (commit) or nothing is (rollback).
    public int placeOrder(int userId, Map<MenuItem, Integer> cart, String address) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<MenuItem, Integer> e : cart.entrySet()) {
            total = total.add(e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }

        String insertOrder = "INSERT INTO orders (user_id, total, delivery_address) VALUES (?, ?, ?)";
        String insertLine  = "INSERT INTO order_items (order_id, item_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int orderId = -1;
                try (PreparedStatement ps = con.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setBigDecimal(2, total);
                    ps.setString(3, address);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(insertLine)) {
                    for (Map.Entry<MenuItem, Integer> e : cart.entrySet()) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, e.getKey().getId());
                        ps.setInt(3, e.getValue());
                        ps.setBigDecimal(4, e.getKey().getPrice());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return orderId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // userId == null returns ALL orders (admin view).
    public List<Order> getOrders(Integer userId) throws SQLException {
        String sql = "SELECT o.order_id, u.username, o.total, o.status, o.delivery_address, o.created_at, "
                   + "GROUP_CONCAT(CONCAT(oi.quantity, ' x ', m.name) SEPARATOR ', ') AS items "
                   + "FROM orders o "
                   + "JOIN users u ON u.user_id = o.user_id "
                   + "JOIN order_items oi ON oi.order_id = o.order_id "
                   + "JOIN menu_items m ON m.item_id = oi.item_id "
                   + (userId != null ? "WHERE o.user_id = ? " : "")
                   + "GROUP BY o.order_id, u.username, o.total, o.status, o.delivery_address, o.created_at "
                   + "ORDER BY o.order_id DESC";
        List<Order> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Order(rs.getInt("order_id"), rs.getString("username"),
                            rs.getString("items"), rs.getBigDecimal("total"),
                            rs.getString("status"), rs.getString("delivery_address"),
                            rs.getTimestamp("created_at")));
                }
            }
        }
        return list;
    }

    public String getStatus(int orderId) throws SQLException {
        String sql = "SELECT status FROM orders WHERE order_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    // Moves the status forward ONLY if it is still "from". If an admin changed it in the
    // meantime, 0 rows match and nothing is overwritten (safe against race conditions).
    public boolean advanceStatus(int orderId, String from, String to) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ? AND status = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, to);
            ps.setInt(2, orderId);
            ps.setString(3, from);
            return ps.executeUpdate() > 0;
        }
    }

    public void setStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    public List<Integer> getActiveOrderIds() throws SQLException {
        String sql = "SELECT order_id FROM orders WHERE status <> 'DELIVERED'";
        List<Integer> ids = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt("order_id"));
        }
        return ids;
    }
}
