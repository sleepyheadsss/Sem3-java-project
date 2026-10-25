import java.math.BigDecimal;
import java.util.Objects;

// One dish. equals/hashCode use only the id, so a MenuItem works as a HashMap key (the cart).
public class MenuItem {
    private final int id;
    private final String name, category;
    private final BigDecimal price;
    private final boolean available;

    public MenuItem(int id, String name, String category, BigDecimal price, boolean available) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.available = available;
    }

    public int getId()            { return id; }
    public String getName()       { return name; }
    public String getCategory()   { return category; }
    public BigDecimal getPrice()  { return price; }
    public boolean isAvailable()  { return available; }

    @Override public boolean equals(Object o) {
        return o instanceof MenuItem && ((MenuItem) o).id == id;
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
