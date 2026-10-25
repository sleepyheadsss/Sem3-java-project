import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

// Small helpers shared by all screens.
public class Ui {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    public static String money(BigDecimal v)   { return "Rs. " + v.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    public static String pretty(String status) { return status.replace('_', ' '); }
    public static String time(Timestamp t)     { return t == null ? "" : t.toLocalDateTime().format(TIME_FMT); }

    // A table model whose cells cannot be edited by typing.
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Food Delivery", JOptionPane.INFORMATION_MESSAGE);
    }
    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Food Delivery", JOptionPane.WARNING_MESSAGE);
    }
    public static void error(Component parent, Exception e) {
        warn(parent, "Something went wrong: " + e.getMessage());
    }

    // Runs 'work' (slow database call) on a BACKGROUND thread, then hands the result to
    // onSuccess on the Swing event thread. This keeps the window from freezing.
    public static <T> void async(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                T result;
                try {
                    result = get();
                } catch (Exception e) {
                    Throwable cause = (e instanceof ExecutionException && e.getCause() != null) ? e.getCause() : e;
                    onError.accept(cause instanceof Exception ? (Exception) cause : new Exception(cause));
                    return;
                }
                onSuccess.accept(result);
            }
        }.execute();
    }

    public static <T> void async(Callable<T> work, Consumer<T> onSuccess) {
        async(work, onSuccess, e -> error(null, e));
    }

    // Label + field rows with one button underneath.
    public static JPanel form(String[] labels, JComponent[] fields, JButton button) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        for (int i = 0; i < labels.length; i++) {
            c.gridx = 0; c.gridy = i; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            p.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            p.add(fields[i], c);
        }
        c.gridx = 0; c.gridy = labels.length; c.gridwidth = 2; c.weightx = 0;
        c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.CENTER;
        p.add(button, c);
        return p;
    }

    // Calls 'action' every time the text in 'field' changes (live search).
    public static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { action.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }
}
