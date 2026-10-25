import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// MULTITHREADING: a pool of 4 background threads moves every order through
// PLACED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED, one step every STEP_SECONDS.
// Many orders are tracked at the same time, and no thread sits blocked in sleep().
public class OrderTracker {
    public static final String[] FLOW = {"PLACED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED"};
    private static final int STEP_SECONDS = 20;   // lower this to demo faster

    private final OrderDAO dao = new OrderDAO();

    // Daemon threads never stop the program from exiting.
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "order-tracker");
        t.setDaemon(true);
        return t;
    });

    // Thread-safe SET of order ids already being tracked, so no order is tracked twice.
    private final Set<Integer> tracking = ConcurrentHashMap.newKeySet();

    public void track(int orderId) {
        if (tracking.add(orderId)) {          // add() returns false if it was already there
            scheduleNextStep(orderId);
        }
    }

    // After a restart, carry on with orders that were not delivered yet.
    public void resumeActiveOrders() {
        try {
            for (int id : dao.getActiveOrderIds()) track(id);
        } catch (SQLException e) {
            System.err.println("Could not resume orders: " + e.getMessage());
        }
    }

    private void scheduleNextStep(int orderId) {
        pool.schedule(() -> step(orderId), STEP_SECONDS, TimeUnit.SECONDS);
    }

    private void step(int orderId) {
        try {
            String current = dao.getStatus(orderId);
            int idx = indexOf(current);
            if (idx >= 0 && idx < FLOW.length - 1) {
                dao.advanceStatus(orderId, current, FLOW[idx + 1]);
                if (idx + 1 < FLOW.length - 1) {
                    scheduleNextStep(orderId);   // not delivered yet, schedule the next step
                    return;
                }
            }
        } catch (SQLException e) {
            System.err.println("Tracker error for order " + orderId + ": " + e.getMessage());
        }
        tracking.remove(orderId);                // finished (or failed): stop tracking
    }

    private static int indexOf(String status) {
        for (int i = 0; i < FLOW.length; i++) {
            if (FLOW[i].equals(status)) return i;
        }
        return -1;
    }
}
