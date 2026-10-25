import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Customer window: Menu (search + sort) -> Cart -> My Orders (auto-refreshing status).
public class CustomerFrame extends JFrame {
    private final Customer user;
    private final MenuDAO menuDao = new MenuDAO();
    private final OrderDAO orderDao = new OrderDAO();

    // COLLECTIONS: cart = item -> quantity (a LinkedHashMap keeps the order items were added)
    private final Map<MenuItem, Integer> cart = new LinkedHashMap<>();
    private List<MenuItem> allItems = new ArrayList<>();
    private List<MenuItem> shownItems = new ArrayList<>();

    private final DefaultTableModel menuModel  = Ui.model("Item", "Category", "Price");
    private final DefaultTableModel cartModel  = Ui.model("Item", "Price", "Qty", "Subtotal");
    private final DefaultTableModel orderModel = Ui.model("Order #", "Items", "Total", "Status", "Placed");
    private final JTable menuTable  = new JTable(menuModel);
    private final JTable cartTable  = new JTable(cartModel);
    private final JTable orderTable = new JTable(orderModel);

    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> sortBox =
        new JComboBox<>(new String[]{"Sort: Name", "Sort: Price low to high", "Sort: Price high to low"});
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JLabel totalLabel = new JLabel("Total: Rs. 0.00");
    private final JTextField addressField;
    private final JButton placeBtn = new JButton("Place order");
    private final JTabbedPane tabs = new JTabbedPane();
    private final javax.swing.Timer refreshTimer;

    public CustomerFrame(Customer user) {
        super("Food Delivery - " + user.getFullName());
        this.user = user;
        this.addressField = new JTextField(user.getAddress(), 30);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 580);
        setLocationRelativeTo(null);

        tabs.addTab("Menu", buildMenuTab());
        tabs.addTab("Cart", buildCartTab());
        tabs.addTab("My Orders", buildOrdersTab());

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> logout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(new JLabel("Welcome, " + user.getFullName()), BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        // Re-load the orders table every 5 seconds so status changes show up by themselves.
        refreshTimer = new javax.swing.Timer(5000, e -> loadOrders());
        refreshTimer.start();

        loadMenu();
        loadOrders();
    }

    // ---------- Menu tab ----------
    private JPanel buildMenuTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        top.add(sortBox);
        Ui.onChange(searchField, this::applyFilter);
        sortBox.addActionListener(e -> applyFilter());

        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JButton add = new JButton("Add to cart");
        add.addActionListener(e -> addToCart());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(new JLabel("Quantity:"));
        bottom.add(qtySpinner);
        bottom.add(add);

        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void loadMenu() {
        Ui.async(() -> menuDao.getAll(true), list -> {
            allItems = list;
            applyFilter();
        });
    }

    private Comparator<MenuItem> currentSort() {
        switch (sortBox.getSelectedIndex()) {
            case 1:  return Comparator.comparing(MenuItem::getPrice);
            case 2:  return Comparator.comparing(MenuItem::getPrice).reversed();
            default: return Comparator.comparing(m -> m.getName().toLowerCase());
        }
    }

    // Filter + sort the in-memory list, then redraw the table.
    private void applyFilter() {
        String q = searchField.getText().trim().toLowerCase();
        shownItems = allItems.stream()
            .filter(m -> m.getName().toLowerCase().contains(q) || m.getCategory().toLowerCase().contains(q))
            .sorted(currentSort())
            .collect(Collectors.toList());
        menuModel.setRowCount(0);
        for (MenuItem m : shownItems) {
            menuModel.addRow(new Object[]{m.getName(), m.getCategory(), Ui.money(m.getPrice())});
        }
    }

    private void addToCart() {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            Ui.warn(this, "Select an item from the menu first.");
            return;
        }
        MenuItem item = shownItems.get(row);
        int qty = (Integer) qtySpinner.getValue();
        cart.merge(item, qty, Integer::sum);     // adds the quantity if the item is already in the cart
        refreshCart();
    }

    // ---------- Cart tab ----------
    private JPanel buildCartTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton remove = new JButton("Remove selected");
        JButton clear = new JButton("Clear cart");
        remove.addActionListener(e -> removeSelectedFromCart());
        clear.addActionListener(e -> { cart.clear(); refreshCart(); });
        placeBtn.addActionListener(e -> placeOrder());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(remove);
        buttons.add(clear);
        JPanel addr = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addr.add(new JLabel("Deliver to:"));
        addr.add(addressField);
        JPanel pay = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pay.add(totalLabel);
        pay.add(placeBtn);

        JPanel south = new JPanel(new GridLayout(3, 1, 4, 4));
        south.add(buttons);
        south.add(addr);
        south.add(pay);

        p.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (Map.Entry<MenuItem, Integer> e : cart.entrySet()) {
            MenuItem m = e.getKey();
            int qty = e.getValue();
            BigDecimal subtotal = m.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);
            count += qty;
            cartModel.addRow(new Object[]{m.getName(), Ui.money(m.getPrice()), qty, Ui.money(subtotal)});
        }
        totalLabel.setText("Total: " + Ui.money(total));
        tabs.setTitleAt(1, count == 0 ? "Cart" : "Cart (" + count + ")");
    }

    private void removeSelectedFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            Ui.warn(this, "Select a cart row first.");
            return;
        }
        MenuItem item = new ArrayList<>(cart.keySet()).get(row);
        cart.remove(item);
        refreshCart();
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            Ui.warn(this, "Your cart is empty.");
            return;
        }
        String address = addressField.getText().trim();
        if (address.isEmpty() || address.length() > 200) {
            Ui.warn(this, "Please enter a delivery address (max 200 characters).");
            return;
        }
        Map<MenuItem, Integer> snapshot = new LinkedHashMap<>(cart);   // copy for the background thread
        placeBtn.setEnabled(false);

        Ui.async(() -> orderDao.placeOrder(user.getUserId(), snapshot, address), orderId -> {
            placeBtn.setEnabled(true);
            Main.TRACKER.track(orderId);          // hand the order to the background thread pool
            cart.clear();
            refreshCart();
            loadOrders();
            tabs.setSelectedIndex(2);
            Ui.info(this, "Order #" + orderId + " placed! Watch its status in 'My Orders'.");
        }, ex -> {
            placeBtn.setEnabled(true);
            Ui.error(this, ex);
        });
    }

    // ---------- Orders tab ----------
    private JPanel buildOrdersTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(320);
        JButton refresh = new JButton("Refresh now");
        refresh.addActionListener(e -> loadOrders());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refresh);
        p.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void loadOrders() {
        Ui.async(() -> orderDao.getOrders(user.getUserId()), list -> {
            orderModel.setRowCount(0);
            for (Order o : list) {
                orderModel.addRow(new Object[]{o.getOrderId(), o.getItems(), Ui.money(o.getTotal()),
                        Ui.pretty(o.getStatus()), Ui.time(o.getCreatedAt())});
            }
        }, ex -> { /* ignore background refresh errors */ });
    }

    private void logout() {
        refreshTimer.stop();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
