import javax.swing.*;

// First window: Login tab and Register tab.
public class LoginFrame extends JFrame {
    private final LoginService service = new LoginService();
    private final JTabbedPane tabs = new JTabbedPane();

    private final JTextField loginUser = new JTextField(18);
    private final JPasswordField loginPass = new JPasswordField(18);
    private final JTextField loginPhone = new JTextField(18);
    private final JButton loginBtn = new JButton("Login");

    private final JTextField regUser = new JTextField(18);
    private final JPasswordField regPass = new JPasswordField(18);
    private final JPasswordField regConfirm = new JPasswordField(18);
    private final JTextField regName = new JTextField(18);
    private final JTextField regPhone = new JTextField(18);
    private final JTextField regAddress = new JTextField(18);
    private final JButton regBtn = new JButton("Create account");

    public LoginFrame() {
        super("Food Delivery - Welcome");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        tabs.addTab("Login", Ui.form(
            new String[]{"Username", "Password", "Phone number"},
            new JComponent[]{loginUser, loginPass, loginPhone}, loginBtn));
        tabs.addTab("Register (new customer)", Ui.form(
            new String[]{"Username", "Password", "Confirm password", "Full name", "Phone (10 digits)", "Delivery address"},
            new JComponent[]{regUser, regPass, regConfirm, regName, regPhone, regAddress}, regBtn));
        add(tabs);

        loginBtn.addActionListener(e -> doLogin());
        regBtn.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(loginBtn);
        tabs.addChangeListener(e ->
            getRootPane().setDefaultButton(tabs.getSelectedIndex() == 0 ? loginBtn : regBtn));

        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String u = loginUser.getText().trim();
        String p = new String(loginPass.getPassword());
        String ph = loginPhone.getText().trim();
        loginBtn.setEnabled(false);

        Ui.async(() -> service.login(u, p, ph), resp -> {
            loginBtn.setEnabled(true);
            switch (resp.result) {
                case SUCCESS:
                    openHome(resp.user);
                    break;
                case INVALID_PHONE_FORMAT:
                    Ui.warn(this, "Phone number must be exactly 10 digits.");
                    break;
                case DB_ERROR:
                    Ui.warn(this, "Cannot reach the database. Is MySQL running, and is the password in DBConnection.java correct?");
                    break;
                default:
                    // One generic message, so attackers cannot tell which part was wrong.
                    Ui.warn(this, "Invalid username, password or phone number.");
                    break;
            }
        }, ex -> {
            loginBtn.setEnabled(true);
            Ui.error(this, ex);
        });
    }

    private void doRegister() {
        String u = regUser.getText().trim();
        String p = new String(regPass.getPassword());
        String c = new String(regConfirm.getPassword());
        String name = regName.getText().trim();
        String phone = regPhone.getText().trim();
        String addr = regAddress.getText().trim();

        if (!p.equals(c)) {
            Ui.warn(this, "Passwords do not match.");
            return;
        }
        regBtn.setEnabled(false);

        Ui.async(() -> service.register(u, p, name, phone, addr), r -> {
            regBtn.setEnabled(true);
            switch (r) {
                case SUCCESS:
                    Ui.info(this, "Account created! Please log in.");
                    regUser.setText(""); regPass.setText(""); regConfirm.setText("");
                    regName.setText(""); regPhone.setText(""); regAddress.setText("");
                    loginUser.setText(u);
                    tabs.setSelectedIndex(0);
                    break;
                case INVALID_USERNAME: Ui.warn(this, "Username must be 3-30 letters, digits or underscores."); break;
                case WEAK_PASSWORD:    Ui.warn(this, "Password must be at least 6 characters."); break;
                case INVALID_NAME:     Ui.warn(this, "Please enter your full name."); break;
                case INVALID_PHONE:    Ui.warn(this, "Phone number must be exactly 10 digits."); break;
                case INVALID_ADDRESS:  Ui.warn(this, "Please enter your delivery address."); break;
                case USERNAME_TAKEN:   Ui.warn(this, "That username is taken. Try another."); break;
                default:               Ui.warn(this, "Could not save to the database. Is MySQL running?"); break;
            }
        }, ex -> {
            regBtn.setEnabled(true);
            Ui.error(this, ex);
        });
    }

    private void openHome(Customer user) {
        dispose();
        JFrame home = user.isAdmin() ? new AdminFrame(user) : new CustomerFrame(user);
        home.setVisible(true);
    }
}
