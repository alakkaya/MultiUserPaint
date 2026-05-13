package ui;

import client.Client;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private Client client;
    private String username;
    private LoginPanel loginPanel;
    private JTabbedPane tabbedPane;
    private FileRoomPanel fileRoomPanel;
    private JLabel statusLabel;

    private final Map<String, CanvasPanel> openCanvases = new HashMap<>();

    public MainFrame() {
        setTitle("MultiUserPaint");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);
        showLoginScreen();
        setVisible(true);
    }

    //Login

    private void showLoginScreen() {
        getContentPane().removeAll();
        loginPanel = new LoginPanel((user, host) -> connectToServer(host, user));
        add(loginPanel);
        revalidate(); repaint();
    }

    // Bağlan
    private void connectToServer(String host, String user) {
        try {
            client = new Client(host, 5005);
            this.username = user;
            client.sendMessage("CONNECT|" + user);
            client.startListening(msg ->
                SwingUtilities.invokeLater(() -> handleIncoming(msg)));
            showMainScreen();
        } catch (IOException e) {
            showLoginError("Sunucuya bağlanılamadı! " + e.getMessage());
        }
    }

    // Dosya odası

    private void showMainScreen() {
        getContentPane().removeAll();
        setTitle("MultiUserPaint — " + username);

        tabbedPane = new JTabbedPane();

        fileRoomPanel = new FileRoomPanel(username, new FileRoomPanel.FileRoomListener() {
            public void onShareFile(String fileName, BufferedImage image) {
                shareFile(fileName, image);
            }
            public void onJoinFile(String fileName) {
                joinFile(fileName);
            }
        });

        tabbedPane.addTab("🏠 Dosya Odası", fileRoomPanel);

        statusLabel = new JLabel("  Bağlı: " + username);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        add(tabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        revalidate(); repaint();
    }

    // Dosya paylaş

    private void shareFile(String fileName, BufferedImage image) {
        String b64 = image != null ? encodeImage(image) : "";
        client.sendMessage("SHARE_FILE|" + fileName + "|" + b64);
    }

    // Dosyaya katıl
    private void joinFile(String fileName) {
        if (openCanvases.containsKey(fileName)) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).contains(fileName)) {
                    tabbedPane.setSelectedIndex(i);
                    return;
                }
            }
        }
        client.sendMessage("JOIN_FILE|" + fileName);
    }

    // Sekme aç
    private void openCanvasTab(String fileName, BufferedImage initialImage) {
        if (openCanvases.containsKey(fileName)) {
            openCanvases.get(fileName).loadImage(initialImage);
            return;
        }

        int canvasW = getInitialCanvasWidth(initialImage);
        int canvasH = getInitialCanvasHeight(initialImage);

        CanvasPanel canvas = new CanvasPanel(canvasW, canvasH);
        if (initialImage != null) canvas.loadImage(initialImage);

        canvas.drawListener = new CanvasPanel.DrawListener() {
            public void onDraw(int x1, int y1, int x2, int y2, String hex, int size) {
                client.sendMessage("DRAW|" + fileName + "|" + x1 + "|" + y1
                    + "|" + x2 + "|" + y2 + "|" + hex + "|" + size);
            }
            public void onCut(int x, int y, int w, int h) {
                client.sendMessage("CUT|" + fileName + "|" + x + "|" + y + "|" + w + "|" + h);
            }
            public void onPaste(int x, int y, BufferedImage img) {
                client.sendMessage("PASTE|" + fileName + "|" + x + "|" + y + "|" + encodeImage(img));
            }
            public void onClear() {
                client.sendMessage("CLEAR|" + fileName);
            }
        };

        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(buildToolbar(canvas), BorderLayout.NORTH);
        tabPanel.add(new JScrollPane(canvas), BorderLayout.CENTER);

        openCanvases.put(fileName, canvas);
        int idx = tabbedPane.getTabCount();
        tabbedPane.addTab("🖼 " + fileName, tabPanel);

        tabbedPane.setTabComponentAt(idx, buildTabHeader(fileName, idx));
        tabbedPane.setSelectedIndex(idx);
    }

    // Sekme başlığı

    private JPanel buildTabHeader(String fileName, int tabIndex) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel lbl = new JLabel("🖼 " + fileName);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(Color.BLACK);
        JButton close = new JButton("×");
        close.setFont(new Font("SansSerif", Font.BOLD, 12));
        close.setPreferredSize(new Dimension(20, 20));
        close.setMargin(new Insets(0, 0, 1, 0));
        close.setToolTipText("Sekmeyi kapat");
        close.setFocusPainted(false);
        close.setContentAreaFilled(true);
        close.setBackground(new Color(255, 240, 240));
        close.setForeground(new Color(160, 30, 30));
        close.setBorder(BorderFactory.createLineBorder(new Color(220, 140, 140)));
        close.addActionListener(e -> {
            int i = tabbedPane.indexOfTabComponent(header);
            if (i >= 0) {
                tabbedPane.remove(i);
                openCanvases.remove(fileName);
                if (tabbedPane.getTabCount() > 0) {
                    tabbedPane.setSelectedIndex(Math.max(0, i - 1));
                }
            }
        });
        header.add(lbl);
        header.add(close);
        return header;
    }

    private int getInitialCanvasWidth(BufferedImage initialImage) {
        int available = Math.max(900, getWidth() - 40);
        if (initialImage == null) return available;
        return Math.max(initialImage.getWidth(), available);
    }

    private int getInitialCanvasHeight(BufferedImage initialImage) {
        int available = Math.max(600, getHeight() - 140);
        if (initialImage == null) return available;
        return Math.max(initialImage.getHeight(), available);
    }

    private JToolBar buildToolbar(CanvasPanel canvas) {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        JButton colorBtn = new JButton("🎨 Renk");
        colorBtn.setForeground(Color.BLACK);
        colorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Renk Seç", Color.BLACK);
            if (c != null) canvas.setBrushColor(c);
        });

        JSlider sizeSlider = new JSlider(1, 40, 4);
        sizeSlider.setPreferredSize(new Dimension(110, 24));
        sizeSlider.addChangeListener(e -> canvas.setBrushSize(sizeSlider.getValue()));

        JToggleButton selBtn = new JToggleButton("☐ Seç");
        selBtn.setForeground(Color.BLACK);
        selBtn.setToolTipText("Seçim modunu aç/kapat");
        selBtn.addItemListener(e -> {
            boolean selected = selBtn.isSelected();
            canvas.setSelectMode(selected);
            selBtn.setText(selected ? "✅ Seç" : "☐ Seç");
            selBtn.setBackground(selected ? new Color(220, 235, 255) : Color.WHITE);
            if (statusLabel != null) {
                statusLabel.setText(selected ? "  Seçim modu açık" : "  Bağlı: " + username);
            }
        });
        selBtn.setFocusPainted(false);

        JButton saveBtn = new JButton("💾 Kaydet");
        saveBtn.setForeground(Color.BLACK);
        saveBtn.addActionListener(e -> saveLocalFile(canvas));

        tb.add(colorBtn);
        JLabel sizeLabel = new JLabel("  Boyut:");
        sizeLabel.setForeground(Color.BLACK);
        tb.add(sizeLabel);
        tb.add(sizeSlider);
        tb.addSeparator();
        tb.add(selBtn);
        tb.addSeparator();
        tb.add(saveBtn);
        return tb;
    }

    // Gelen mesajları işle

    private void handleIncoming(String msg) {
        String[] p = msg.split("\\|", -1);
        switch (p[0]) {

            case "OK":
                statusLabel.setText("  Bağlı: " + username + " ✓");
                break;

            case "ERROR":
                String err = p.length > 1 ? p[1] : "";
                if (err.equals("USERNAME_TAKEN")) {
                    showLoginScreen();
                    JOptionPane.showMessageDialog(this,
                        "Bu kullanıcı adı zaten kullanımda!",
                        "Hata", JOptionPane.ERROR_MESSAGE);
                } else if (err.equals("CONNECTION_LOST")) {
                    JOptionPane.showMessageDialog(this, "Sunucu bağlantısı kesildi.");
                } else {
                    if (loginPanel != null && loginPanel.isShowing()) {
                        showLoginError("Hata: " + err);
                    } else {
                        JOptionPane.showMessageDialog(this, "Hata: " + err);
                    }
                }
                break;

            case "FILE_LIST":
                // FILE_LIST|dosya:sahip|dosya:sahip|...
                fileRoomPanel.clearAndSetFiles(new String[0]);
                for (int i = 1; i < p.length; i++) {
                    String[] parts = p[i].split(":");
                    if (parts.length == 2)
                        fileRoomPanel.addFile(parts[0], parts[1]);
                }
                break;

            case "FILE_ADDED":
                fileRoomPanel.addFile(p[1], p[2]);
                statusLabel.setText("  Yeni dosya paylaşıldı: " + p[1]);
                break;

            case "CANVAS_SYNC": {
                // CANVAS_SYNC|dosyaAdi|base64
                String fileName = p[1];
                BufferedImage img = decodeImage(p[2]);
                openCanvasTab(fileName, img);
                break;
            }

            case "DRAW": {
                // DRAW|dosya|x1|y1|x2|y2|renk|boyut
                CanvasPanel cv = openCanvases.get(p[1]);
                if (cv != null)
                    cv.applyRemoteDraw(
                        Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                        p[6], Integer.parseInt(p[7]));
                break;
            }

            case "CUT": {
                CanvasPanel cv = openCanvases.get(p[1]);
                if (cv != null)
                    cv.applyRemoteCut(
                        Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]), Integer.parseInt(p[5]));
                break;
            }

            case "PASTE": {
                CanvasPanel cv = openCanvases.get(p[1]);
                BufferedImage img = decodeImage(p[4]);
                if (cv != null && img != null)
                    cv.applyRemotePaste(Integer.parseInt(p[2]), Integer.parseInt(p[3]), img);
                break;
            }

            case "CLEAR": {
                CanvasPanel cv = openCanvases.get(p[1]);
                if (cv != null) cv.clearCanvas();
                break;
            }

            case "USER_JOINED":
                statusLabel.setText("  " + p[1] + " katıldı");
                break;

            case "USER_LEFT":
                statusLabel.setText("  " + p[1] + " ayrıldı");
                break;
        }
    }

    //Helpers

    private String encodeImage(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) { return ""; }
    }

    private BufferedImage decodeImage(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(b64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) { return null; }
    }

    private void saveLocalFile(CanvasPanel canvas) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("resim.png"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(canvas.getImage(), "png", fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Kaydedildi ✓");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
            }
        }
    }

    private void showLoginError(String message) {
        if (loginPanel != null && loginPanel.isShowing()) {
            loginPanel.setStatusMessage(message);
        } else {
            JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}