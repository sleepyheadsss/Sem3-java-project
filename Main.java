import javax.swing.*;

// Program entry point: start the order tracker, then show the login window.
public class Main {
    // One shared tracker for the whole app (the thread pool lives here).
    public static final OrderTracker TRACKER = new OrderTracker();

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to the default look
        }
        TRACKER.resumeActiveOrders();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
