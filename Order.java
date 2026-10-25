import java.math.BigDecimal;
import java.sql.Timestamp;

// One order as shown in a table: who ordered, what, how much, current status.
public class Order {
    private final int orderId;
    private final String customer, items, status, address;
    private final BigDecimal total;
    private final Timestamp createdAt;

    public Order(int orderId, String customer, String items, BigDecimal total,
                 String status, String address, Timestamp createdAt) {
        this.orderId = orderId;
        this.customer = customer;
        this.items = items;
        this.total = total;
        this.status = status;
        this.address = address;
        this.createdAt = createdAt;
    }

    public int getOrderId()         { return orderId; }
    public String getCustomer()     { return customer; }
    public String getItems()        { return items; }
    public BigDecimal getTotal()    { return total; }
    public String getStatus()       { return status; }
    public String getAddress()      { return address; }
    public Timestamp getCreatedAt() { return createdAt; }
}
