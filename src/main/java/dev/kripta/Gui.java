package dev.kripta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/** kripta'nin koyu temali, platformlar arasi Swing masaustu arayuzu. */
public final class Gui extends JFrame {
    private static final Color BG = new Color(14, 17, 23);
    private static final Color SURFACE = new Color(23, 27, 35);
    private static final Color INPUT = new Color(29, 34, 44);
    private static final Color BORDER = new Color(52, 60, 75);
    private static final Color TEXT = new Color(238, 241, 247);
    private static final Color MUTED = new Color(154, 163, 181);
    private static final Color BLUE = new Color(43, 113, 246);
    private static final Color GREEN = new Color(48, 191, 113);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private enum Mode { ENCRYPT, DECRYPT, RUN }
    private Mode mode = Mode.ENCRYPT;

    private final JButton encryptTab = tab("Şifrele");
    private final JButton decryptTab = tab("Çöz");
    private final JButton runTab = tab("Şifreli JAR Çalıştır");
    private final JLabel dropTitle = label("Dosyanızı buraya sürükleyin veya seçin", 17, Font.BOLD);
    private final JLabel dropHint = muted("Tüm dosya türleri desteklenir");
    private final JPasswordField password = passwordField();
    private final JPasswordField confirm = passwordField();
    private final JLabel confirmLabel = label("Parola (tekrar)", 14, Font.BOLD);
    private final JCheckBox bind = new JCheckBox("Bu makineye mühürle", true);
    private final JTextField output = textField();
    private final JTextField mainClass = textField();
    private final JTextField arguments = textField();
    private final JLabel mainClassLabel = label("Ana sınıf", 14, Font.BOLD);
    private final JLabel argumentsLabel = label("Program argümanları", 14, Font.BOLD);
    private final JButton action = primary("Şifrele ve Kaydet");
    private final JProgressBar progress = new JProgressBar();
    private final JTextArea log = new JTextArea(5, 20);
    private final JPanel form = verticalPanel();
    private Path input;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            Gui gui = new Gui();
            gui.setVisible(true);
        });
    }

    private Gui() {
        super("kripta");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 720));
        setSize(1040, 820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(24, 30, 24, 30));
        root.add(header(), BorderLayout.NORTH);
        root.add(content(), BorderLayout.CENTER);
        add(root);

        encryptTab.addActionListener(e -> setMode(Mode.ENCRYPT));
        decryptTab.addActionListener(e -> setMode(Mode.DECRYPT));
        runTab.addActionListener(e -> setMode(Mode.RUN));
        action.addActionListener(e -> execute());
        setMode(Mode.ENCRYPT);
        installDropTarget();
        log("Uygulama hazır.", true);
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        JLabel brand = label("kripta", 28, Font.BOLD);
        brand.setForeground(TEXT);
        p.add(brand, BorderLayout.WEST);
        JPanel tabs = new JPanel(new GridLayout(1, 3, 1, 0));
        tabs.setBackground(BORDER);
        tabs.setBorder(BorderFactory.createLineBorder(BORDER));
        tabs.add(encryptTab);
        tabs.add(decryptTab);
        tabs.add(runTab);
        p.add(tabs, BorderLayout.CENTER);
        JLabel version = muted("v" + Kripta.VERSION);
        p.add(version, BorderLayout.EAST);
        return p;
    }

    private JPanel content() {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(dropZone(), BorderLayout.NORTH);

        form.setBorder(new EmptyBorder(0, 10, 0, 10));
        rebuildForm();
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(BG);
        formScroll.setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(14);
        content.add(formScroll, BorderLayout.CENTER);
        content.add(logPanel(), BorderLayout.SOUTH);
        return content;
    }

    private JPanel dropZone() {
        JPanel zone = new JPanel();
        zone.setPreferredSize(new Dimension(100, 150));
        zone.setBackground(SURFACE);
        zone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(BLUE, 1.5f, 6, 5, true),
                new EmptyBorder(25, 20, 25, 20)));
        zone.setLayout(new BoxLayout(zone, BoxLayout.Y_AXIS));
        dropTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        dropHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton choose = secondary("Dosya Seç");
        choose.setAlignmentX(Component.CENTER_ALIGNMENT);
        choose.addActionListener(e -> chooseInput());
        zone.add(Box.createVerticalGlue());
        zone.add(dropTitle);
        zone.add(Box.createVerticalStrut(7));
        zone.add(dropHint);
        zone.add(Box.createVerticalStrut(13));
        zone.add(choose);
        zone.add(Box.createVerticalGlue());
        return zone;
    }

    private JPanel logPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("İşlem günlüğü", 14, Font.BOLD), BorderLayout.WEST);
        JButton clear = secondary("Temizle");
        clear.addActionListener(e -> log.setText(""));
        top.add(clear, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        log.setEditable(false);
        log.setBackground(SURFACE);
        log.setForeground(MUTED);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setPreferredSize(new Dimension(100, 115));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void rebuildForm() {
        form.removeAll();
        addField("1", "Parola", password, null);
        if (mode == Mode.ENCRYPT) {
            addField("2", "Parola (tekrar)", confirm, null);
            addField("3", "Makine bağlama", bind,
                    "Açıksa şifreli dosya yalnızca bu bilgisayarda çözülebilir.");
            addField("4", "Çıktı konumu", output, "Şifreli dosyanın kaydedileceği konum.");
        } else if (mode == Mode.DECRYPT) {
            addField("2", "Çıktı konumu", output, "Çözülen dosyanın kaydedileceği konum.");
        } else {
            addField("2", "Ana sınıf", mainClass, "Örnek: com.ornek.Main");
            addField("3", "Program argümanları", arguments, "İsteğe bağlı; boşlukla ayırın.");
        }
        form.add(Box.createVerticalStrut(8));
        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        progress.setStringPainted(true);
        progress.setString("Hazır");
        progress.setForeground(BLUE);
        progress.setBackground(INPUT);
        progress.setBorderPainted(false);
        bottom.add(progress, BorderLayout.CENTER);
        bottom.add(action, BorderLayout.EAST);
        form.add(bottom);
        form.add(Box.createVerticalGlue());
        form.revalidate();
        form.repaint();
    }

    private void addField(String number, String title, JComponent component, String help) {
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, help == null ? 66 : 82));
        JLabel n = new JLabel(number, SwingConstants.CENTER);
        n.setOpaque(true);
        n.setBackground(BLUE);
        n.setForeground(Color.WHITE);
        n.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        n.setPreferredSize(new Dimension(34, 34));
        row.add(n, BorderLayout.WEST);
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setOpaque(false);
        body.add(label(title, 14, Font.BOLD), BorderLayout.NORTH);
        body.add(component, BorderLayout.CENTER);
        if (help != null) body.add(muted(help), BorderLayout.SOUTH);
        row.add(body, BorderLayout.CENTER);
        form.add(row);
        form.add(Box.createVerticalStrut(14));
    }

    private void setMode(Mode next) {
        mode = next;
        styleTab(encryptTab, next == Mode.ENCRYPT);
        styleTab(decryptTab, next == Mode.DECRYPT);
        styleTab(runTab, next == Mode.RUN);
        bind.setVisible(next == Mode.ENCRYPT);
        action.setText(switch (next) {
            case ENCRYPT -> "Şifrele ve Kaydet";
            case DECRYPT -> "Çöz ve Kaydet";
            case RUN -> "Güvenli Çalıştır";
        });
        input = null;
        dropTitle.setText("Dosyanızı buraya sürükleyin veya seçin");
        dropHint.setText(next == Mode.RUN ? "Şifreli JAR (.enc) seçin" : "Tüm dosya türleri desteklenir");
        output.setText("");
        password.setText("");
        confirm.setText("");
        rebuildForm();
    }

    private void chooseInput() {
        JFileChooser chooser = new JFileChooser();
        if (mode != Mode.ENCRYPT) chooser.setFileFilter(new FileNameExtensionFilter("Şifreli dosya (*.enc)", "enc"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) setInput(chooser.getSelectedFile().toPath());
    }

    private void setInput(Path path) {
        if (!Files.isRegularFile(path)) return;
        input = path.toAbsolutePath();
        dropTitle.setText(input.getFileName().toString());
        try { dropHint.setText(humanSize(Files.size(input)) + " • Hazır"); }
        catch (Exception e) { dropHint.setText("Dosya seçildi"); }
        if (mode != Mode.RUN) {
            String name = input.getFileName().toString();
            Path parent = input.getParent();
            if (mode == Mode.ENCRYPT) output.setText(parent.resolve(name + ".enc").toString());
            else {
                String decoded = name.endsWith(".enc") ? name.substring(0, name.length() - 4) : name + ".decoded";
                output.setText(parent.resolve(decoded).toString());
            }
        }
        log(input.getFileName() + " dosyası seçildi.", true);
    }

    private void installDropTarget() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) setInput(files.get(0).toPath());
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.dropComplete(false);
                    showError(ex.getMessage());
                }
            }
        }, true);
    }

    private void execute() {
        if (input == null) { showError("Önce bir dosya seçin."); return; }
        char[] pass = password.getPassword();
        if (pass.length == 0) { showError("Parola boş olamaz."); return; }
        if (mode == Mode.ENCRYPT && !Arrays.equals(pass, confirm.getPassword())) {
            Arrays.fill(pass, '\0');
            showError("Parolalar eşleşmiyor.");
            return;
        }
        if (mode != Mode.RUN && output.getText().isBlank()) {
            Arrays.fill(pass, '\0');
            showError("Çıktı konumu boş olamaz.");
            return;
        }
        action.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("İşleniyor…");
        log("İşlem başlatıldı: " + action.getText(), true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try {
                    byte[] source = Files.readAllBytes(input);
                    if (mode == Mode.ENCRYPT) {
                        byte[] pepper = bind.isSelected() ? MachineKey.fingerprint() : null;
                        byte[] result = Crypto.encrypt(source, pass, pepper);
                        Files.write(Path.of(output.getText()), result);
                        Arrays.fill(source, (byte) 0);
                    } else if (mode == Mode.DECRYPT) {
                        byte[] pepper = Crypto.isBound(source) ? MachineKey.fingerprint() : null;
                        byte[] result = Crypto.decrypt(source, pass, pepper);
                        Files.write(Path.of(output.getText()), result);
                        Arrays.fill(result, (byte) 0);
                    } else {
                        if (mainClass.getText().isBlank()) throw new IllegalArgumentException("Ana sınıf boş olamaz.");
                        String[] args = arguments.getText().isBlank() ? new String[0] : arguments.getText().trim().split("\\s+");
                        Loader.run(input.toString(), mainClass.getText().trim(), pass, args);
                    }
                    return null;
                } finally {
                    Arrays.fill(pass, '\0');
                }
            }

            @Override protected void done() {
                action.setEnabled(true);
                progress.setIndeterminate(false);
                try {
                    get();
                    progress.setValue(100);
                    progress.setString("Başarılı");
                    log("İşlem başarıyla tamamlandı.", true);
                    JOptionPane.showMessageDialog(Gui.this, "İşlem başarıyla tamamlandı.", "kripta", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    progress.setValue(0);
                    progress.setString("Başarısız");
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    log(cause.getMessage(), false);
                    showError(cause.getMessage());
                }
            }
        }.execute();
    }

    private void log(String message, boolean ok) {
        log.append(LocalTime.now().format(CLOCK) + "  " + (ok ? "BAŞARILI  " : "HATA      ") + message + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Bilinmeyen hata." : message,
                "kripta — Hata", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel verticalPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JTextField textField() {
        JTextField f = new JTextField();
        styleInput(f);
        return f;
    }

    private static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        styleInput(f);
        return f;
    }

    private static void styleInput(JTextField f) {
        f.setBackground(INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 11, 9, 11)));
        f.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private static JLabel label(String text, int size, int style) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(new Font(Font.SANS_SERIF, style, size));
        return l;
    }

    private static JLabel muted(String text) {
        JLabel l = label(text, 12, Font.PLAIN);
        l.setForeground(MUTED);
        return l;
    }

    private static JButton tab(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 18, 12, 18));
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void styleTab(JButton b, boolean selected) {
        b.setBackground(selected ? BLUE : SURFACE);
        b.setForeground(selected ? Color.WHITE : MUTED);
    }

    private static JButton primary(String text) {
        JButton b = tab(text);
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(12, 24, 12, 24));
        return b;
    }

    private static JButton secondary(String text) {
        JButton b = tab(text);
        b.setBackground(INPUT);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(7, 14, 7, 14)));
        return b;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static void installLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignore) {}
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("CheckBox.background", BG);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("ScrollPane.background", BG);
    }
}
