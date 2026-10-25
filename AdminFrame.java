import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Admin window: manage the menu and update order statuses.
public class AdminFrame extends JFrame {
    private final MenuDAO menuDao = new MenuDAO();
    private final OrderDAO orderDao = new OrderDAO();
    private List<MenuItem> menuList = new ArrayList<>();

    private final DefaultTableModel menuModel  = Ui.model("ID", "Name", "Category", "Price", "Available");
    private final DefaultTableModel orderModel =
        Ui.model("Order #", "Customer", "Items", "Total", "Status", "Placed", "Address");
    private final JTable menuTable  = new JTable(menuModel);
    private final JTable orderTable = new JTable(orderModel);

    private final JTextField nameField = new JTextField(14);
    private final JTextField catField = new JTextField(10);
    private final JTextField priceField = new JTextField(7);
    private final JComboBox<String> statusBox = new JComboBox<>(OrderTracker.FLOW);
    private final javax.swing.Timer refreshTimer;

    public AdminFrame(Customer admin) {
        super("Food Delivery - Admin (" + admin.getFullName() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage menu", buildMenuTab());
        tabs.addTab("All orders", buildOrdersTab());

        refreshTimer = new javax.swing.Timer(5000, e -> loadOrders());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            refreshTimer.stop();
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(new JLabel("Admin panel"), BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        refreshTimer.start();
        loadMenu();
        loadOrders();
    }

    // ---------- Menu management ----------
    private JPanel buildMenuTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillFormFromSelection();
        });

        JButton add = new JButton("Add new");
        JButton update = new JButton("Update selected");
        JButton toggle = new JButton("Hide / show selected");
        add.addActionListener(e -> addItem());
        update.addActionListener(e -> updateItem());
        toggle.addActionListener(e -> toggleItem());

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fields.add(new JLabel("Name"));     fields.add(nameField);
        fields.add(new JLabel("Category")); fields.add(catField);
        fields.add(new JLabel("Price"));    fields.add(priceField);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(add);
        buttons.add(update);
        buttons.add(toggle);
        JPanel south = new JPanel(new GridLayout(2, 1));
        south.add(fields);
        south.add(buttons);

        p.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void loadMenu() {
        Ui.async(() -> menuDao.getAll(false), list -> {
            menuList = list;
            menuModel.setRowCount(0);
            for (MenuItem m : list) {
                menuModel.addRow(new Object[]{m.getId(), m.getName(), m.getCategory(),
                        Ui.money(m.getPrice()), m.isAvailable() ? "Yes" : "No (hidden)"});
            }
        });
    }

    private void fillFormFromSelection() {
        int row = menuTable.getSelectedRow();
        if (row < 0 || row >= menuList.size()) return;
        MenuItem m = menuList.get(row);
        nameField.setText(m.getName());
        catField.setText(m.getCategory());
        priceField.setText(m.getPrice().toPlainString());
    }

    // Returns an error message, or null if the form is valid.
    private String validateForm() {
        String name = nameField.getText().trim();
        String cat = catField.getText().trim();
        if (name.isEmpty() || name.length() > 60) return "Name is required (max 60 characters).";
        if (cat.isEmpty() || cat.length() > 30)   return "Category is required (max 30 characters).";
        try {
            BigDecimal p = new BigDecimal(priceField.getText().trim());
            if (p.signum() <= 0 || p.scale() > 2 || p.compareTo(new BigDecimal("999999.99")) > 0)
                return "Price must be positive with at most 2 decimal places.";
        } catch (NumberFormatException e) {
            return "Price must be a number, for example 149.50";
        }
        return null;
    }

    private void addItem() {
        String err = validateForm();
        if (err != null) { Ui.warn(this, err); return; }
        String name = nameField.getText().trim();
        String cat = catField.getText().trim();
        BigDecimal price = new BigDecimal(priceField.getText().trim());
        Ui.async(() -> { menuDao.add(name, cat, price); return null; }, done -> {
            nameField.setText(""); catField.setText(""); priceField.setText("");
            loadMenu();
        });
    }

    private void updateItem() {
        int row = menuTable.getSelectedRow();
        if (row < 0) { Ui.warn(this, "Select an item in the table first."); return; }
        String err = validateForm();
        if (err != null) { Ui.warn(this, err); return; }
        int id = menuList.get(row).getId();
        String name = nameField.getText().trim();
        String cat = catField.getText().trim();
        BigDecimal price = new BigDecimal(priceField.getText().trim());
        Ui.async(() -> { menuDao.update(id, name, cat, price); return null; }, done -> loadMenu());
    }

    private void toggleItem() {
        int row = menuTable.getSelectedRow();
        if (row < 0) { Ui.warn(this, "Select an item in the table first."); return; }
        MenuItem m = menuList.get(row);
        Ui.async(() -> { menuDao.setAvailable(m.getId(), !m.isAvailable()); return null; }, done -> loadMenu());
    }

    // ---------- Orders ----------
    private JPanel buildOrdersTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(260);

        JButton set = new JButton("Set status");
        JButton refresh = new JButton("Refresh now");
        set.addActionListener(e -> setStatus());
        refresh.addActionListener(e -> loadOrders());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("New status for selected order:"));
        bottom.add(statusBox);
        bottom.add(set);
        bottom.add(refresh);

        p.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private int selectedOrderId() {
        int row = orderTable.getSelectedRow();
        return row < 0 ? -1 : (Integer) orderModel.getValueAt(row, 0);
    }

    private void loadOrders() {
        int keep = selectedOrderId();            // remember the selection across refreshes
        Ui.async(() -> orderDao.getOrders(null), list -> {
            orderModel.setRowCount(0);
            for (Order o : list) {
                orderModel.addRow(new Object[]{o.getOrderId(), o.getCustomer(), o.getItems(),
                        Ui.money(o.getTotal()), Ui.pretty(o.getStatus()),
                        Ui.time(o.getCreatedAt()), o.getAddress()});
            }
            for (int i = 0; i < orderModel.getRowCount(); i++) {
                if ((Integer) orderModel.getValueAt(i, 0) == keep) {
                    orderTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }, ex -> { /* ignore background refresh errors */ });
    }

    private void setStatus() {
        int id = selectedOrderId();
        if (id < 0) { Ui.warn(this, "Select an order first."); return; }
        String status = (String) statusBox.getSelectedItem();
        Ui.async(() -> { orderDao.setStatus(id, status); return null; }, done -> loadOrders());
    }
}
