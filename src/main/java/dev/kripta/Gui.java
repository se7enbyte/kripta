package dev.kripta;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
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
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int RADIUS = 22;

    private enum Mode { ENCRYPT, DECRYPT, RUN }
    private Mode mode = Mode.ENCRYPT;

    private final JButton encryptTab = tab("Şifrele");
    private final JButton decryptTab = tab("Çöz");
    private final JButton runTab = tab("Şifreli JAR Çalıştır");
    private final JLabel dropTitle = label("Dosyanızı buraya sürükleyin veya seçin", 17, Font.BOLD);
    private final JLabel dropHint = muted("Tüm dosya türleri desteklenir");
    private final JPasswordField password = passwordField();
    private final JPasswordField confirm = passwordField();
    private final JCheckBox bind = new ToggleSwitch("Bu makineye mühürle", true);
    private final JTextField output = textField();
    private final JTextField mainClass = textField();
    private final JTextField arguments = textField();
    private final JButton action = primary("Şifrele ve Kaydet");
    private final JProgressBar progress = new RoundedProgressBar();
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
        setMinimumSize(new Dimension(880, 680));
        setSize(1040, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(16, 26, 16, 26));
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
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel brand = label("kripta", 28, Font.BOLD);
        JLabel tagline = muted("Dosyaların. Anahtarın. Kontrolün.");
        identity.add(brand);
        identity.add(tagline);
        p.add(identity, BorderLayout.WEST);
        JPanel tabs = new JPanel(new GridLayout(1, 3, 1, 0));
        tabs.setOpaque(false);
        tabs.setBorder(new EmptyBorder(2, 2, 2, 2));
        tabs.add(encryptTab);
        tabs.add(decryptTab);
        tabs.add(runTab);
        p.add(tabs, BorderLayout.CENTER);
        JPanel status = new RoundedPanel(new Color(20, 48, 40), new Color(36, 112, 79), 18, false);
        status.setBorder(new EmptyBorder(7, 12, 7, 12));
        status.add(label("Yerel • Çevrimdışı  ·  v" + Kripta.VERSION, 12, Font.BOLD));
        p.add(status, BorderLayout.EAST);
        return p;
    }

    private JPanel content() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(dropZone(), BorderLayout.NORTH);

        form.setBorder(new EmptyBorder(0, 10, 0, 10));
        rebuildForm();
        content.add(form, BorderLayout.CENTER);
        content.add(logPanel(), BorderLayout.SOUTH);
        return content;
    }

    private JPanel dropZone() {
        JPanel zone = new RoundedPanel(SURFACE, BLUE, 26, true);
        zone.setPreferredSize(new Dimension(100, 120));
        zone.setBorder(new EmptyBorder(18, 20, 18, 20));
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
        scroll.setBorder(new OutlineBorder(BORDER, RADIUS, 1.2f));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.setPreferredSize(new Dimension(100, 92));
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
            addField("4", "Çıktı konumu", outputPicker(), "Şifreli dosyanın kaydedileceği konum.");
        } else if (mode == Mode.DECRYPT) {
            addField("2", "Çıktı konumu", outputPicker(), "Çözülen dosyanın kaydedileceği konum.");
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
        progress.setPreferredSize(new Dimension(100, 38));
        bottom.add(progress, BorderLayout.CENTER);
        bottom.add(action, BorderLayout.EAST);
        form.add(bottom);
        form.add(Box.createVerticalGlue());
        form.revalidate();
        form.repaint();
    }

    private JPanel outputPicker() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.add(output, BorderLayout.CENTER);
        JButton browse = secondary("Konum Seç");
        browse.addActionListener(e -> chooseOutput());
        p.add(browse, BorderLayout.EAST);
        return p;
    }

    private void addField(String number, String title, JComponent component, String help) {
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, help == null ? 54 : 68));
        JLabel n = new CircleLabel(number);
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
        form.add(Box.createVerticalStrut(8));
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

    private void chooseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Çıktı konumunu seçin");
        if (!output.getText().isBlank()) chooser.setSelectedFile(new File(output.getText()));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
            output.setText(chooser.getSelectedFile().getAbsolutePath());
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
        return new RoundedTextField();
    }

    private static JPasswordField passwordField() {
        return new RoundedPasswordField();
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
        return new RoundedButton(text, SURFACE, BORDER, 18, 12, 18);
    }

    private static void styleTab(JButton b, boolean selected) {
        b.setBackground(selected ? BLUE : SURFACE);
        b.setForeground(selected ? Color.WHITE : MUTED);
        if (b instanceof RoundedButton rb) rb.setPalette(selected ? BLUE : SURFACE,
                selected ? new Color(92, 150, 255) : BORDER);
    }

    private static JButton primary(String text) {
        JButton b = tab(text);
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        if (b instanceof RoundedButton rb) rb.setPalette(BLUE, new Color(92, 150, 255));
        b.setBorder(new EmptyBorder(12, 24, 12, 24));
        return b;
    }

    private static JButton secondary(String text) {
        JButton b = tab(text);
        b.setBackground(INPUT);
        b.setForeground(TEXT);
        if (b instanceof RoundedButton rb) rb.setPalette(INPUT, BORDER);
        b.setBorder(new EmptyBorder(7, 14, 7, 14));
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

    private static final class OutlineBorder implements Border {
        private final Color line;
        private final int radius;
        private final float width;

        OutlineBorder(Color line, int radius, float width) {
            this.line = line;
            this.radius = radius;
            this.width = width;
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(line);
            g2.setStroke(new BasicStroke(this.width));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
            g2.dispose();
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color line;
        private final int radius;
        private final boolean dashed;

        RoundedPanel(Color fill, Color line, int radius, boolean dashed) {
            this.fill = fill;
            this.line = line;
            this.radius = radius;
            this.dashed = dashed;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius);
            g2.setColor(line);
            g2.setStroke(dashed
                    ? new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{7f, 6f}, 0f)
                    : new BasicStroke(1.2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedTextField extends JTextField {
        RoundedTextField() {
            setForeground(TEXT);
            setCaretColor(TEXT);
            setOpaque(false);
            setBorder(new EmptyBorder(9, 12, 9, 12));
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }

        @Override protected void paintComponent(Graphics g) {
            paintField(g, this);
            super.paintComponent(g);
        }
    }

    private static final class RoundedPasswordField extends JPasswordField {
        RoundedPasswordField() {
            setForeground(TEXT);
            setCaretColor(TEXT);
            setOpaque(false);
            setBorder(new EmptyBorder(9, 12, 9, 12));
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }

        @Override protected void paintComponent(Graphics g) {
            paintField(g, this);
            super.paintComponent(g);
        }
    }

    private static void paintField(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(INPUT);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, RADIUS, RADIUS);
        g2.setColor(BORDER);
        g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, RADIUS, RADIUS);
        g2.dispose();
    }

    private static final class RoundedButton extends JButton {
        private Color fill;
        private Color line;
        private final int radius;

        RoundedButton(String text, Color fill, Color line, int radius, int vertical, int horizontal) {
            super(text);
            this.fill = fill;
            this.line = line;
            this.radius = radius;
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(vertical, horizontal, vertical, horizontal));
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setPalette(Color fill, Color line) {
            this.fill = fill;
            this.line = line;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = getModel().isPressed() ? fill.darker() : getModel().isRollover() ? fill.brighter() : fill;
            g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ToggleSwitch extends JCheckBox {
        ToggleSwitch(String text, boolean selected) {
            super(text, selected);
            setOpaque(false);
            setForeground(TEXT);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            setIconTextGap(12);
            setBorder(new EmptyBorder(2, 0, 2, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override public Insets getInsets() { return new Insets(3, 54, 3, 3); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - 24) / 2;
            g2.setColor(isSelected() ? BLUE : BORDER);
            g2.fillRoundRect(0, y, 43, 24, 24, 24);
            g2.setColor(Color.WHITE);
            g2.fillOval(isSelected() ? 22 : 3, y + 3, 18, 18);
            g2.dispose();

            g2 = (Graphics2D) g.create();
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 54, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class RoundedProgressBar extends JProgressBar {
        RoundedProgressBar() {
            setOpaque(false);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(INPUT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            if (isIndeterminate()) {
                int w = Math.max(70, getWidth() / 4);
                int x = (int) ((System.currentTimeMillis() / 8) % (getWidth() + w)) - w;
                g2.setColor(BLUE);
                g2.fillRoundRect(x, 0, w, getHeight(), 18, 18);
                repaint(35);
            } else if (getValue() > 0) {
                int w = Math.max(18, (getWidth() - 2) * getValue() / Math.max(1, getMaximum()));
                g2.setColor(getValue() >= 100 ? new Color(42, 174, 106) : BLUE);
                g2.fillRoundRect(1, 1, w, getHeight() - 2, 17, 17);
            }
            String s = getString();
            if (s != null) {
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setFont(getFont());
                g2.setColor(TEXT);
                g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    private static final class DarkScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = BORDER;
            trackColor = SURFACE;
        }
        @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
    }

    private static final class CircleLabel extends JLabel {
        CircleLabel(String text) {
            super(text, SwingConstants.CENTER);
            setForeground(Color.WHITE);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            setPreferredSize(new Dimension(34, 34));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BLUE);
            int d = Math.min(getWidth(), getHeight()) - 2;
            g2.fillOval(1, 1, d, d);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int tx = (d - fm.stringWidth(getText())) / 2 + 1;
            int ty = (d - fm.getHeight()) / 2 + fm.getAscent() + 1;
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }
}
