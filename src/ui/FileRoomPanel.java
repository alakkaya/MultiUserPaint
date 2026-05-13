package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public class FileRoomPanel extends JPanel {

    public interface FileRoomListener {
        void onShareFile(String fileName, BufferedImage image);
        void onJoinFile(String fileName);                       
    }

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(listModel);
    private FileRoomListener listener;

    public FileRoomPanel(String username, FileRoomListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(new Color(245, 247, 252));

        JLabel title = new JLabel("Hoş geldin, " + username + " 👋", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.BLACK);
        add(title, BorderLayout.NORTH);

        fileList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFixedCellHeight(36);
        fileList.setCellRenderer(new FileListRenderer());

        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(BorderFactory.createTitledBorder("Paylaşılan Dosyalar"));
        add(scroll, BorderLayout.CENTER);

        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String sel = fileList.getSelectedValue();
                    if (sel != null) {
                        String fileName = sel.split(" — ")[0];
                        listener.onJoinFile(fileName);
                    }
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);

        JButton newEmptyBtn = new JButton("+ Yeni Boş Tuval");
        JButton openFileBtn = new JButton("📁 Dosyadan Aç");
        JButton joinBtn     = new JButton("→ Seçilene Gir");

        styleBtn(newEmptyBtn, new Color(60, 160, 90));
        styleBtn(openFileBtn, new Color(90, 120, 200));
        styleBtn(joinBtn,     new Color(200, 120, 40));

        newEmptyBtn.addActionListener(e -> createNewCanvas());
        openFileBtn.addActionListener(e -> openFromFile());
        joinBtn.addActionListener(e -> {
            String sel = fileList.getSelectedValue();
            if (sel != null) listener.onJoinFile(sel.split(" — ")[0]);
            else JOptionPane.showMessageDialog(this, "Önce listeden bir dosya seçin.");
        });

        btnPanel.add(newEmptyBtn);
        btnPanel.add(openFileBtn);
        btnPanel.add(joinBtn);

        JLabel hint = new JLabel("  İpucu: Listedeki dosyaya çift tıklayarak da girebilirsiniz.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.BLACK);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(btnPanel, BorderLayout.NORTH);
        south.add(hint, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    public void addFile(String fileName, String owner) {
        String entry = fileName + " — " + owner;
        if (!listModel.contains(entry)) {
            listModel.addElement(entry);
        }
    }

    public void clearAndSetFiles(String[] entries) {
        listModel.clear();
        for (String e : entries) listModel.addElement(e);
    }

    private void createNewCanvas() {
        String name = JOptionPane.showInputDialog(this,
            "Tuval adı girin (örn: resim.png):", "Yeni Tuval", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        if (!name.endsWith(".png")) name += ".png";
        listener.onShareFile(name.trim(), null); // null → sunucu boş tuval oluşturur
    }

    private void openFromFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Paylaşılacak resmi seç");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Resim dosyaları", "png", "jpg", "jpeg", "bmp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            BufferedImage img = ImageIO.read(f);
            listener.onShareFile(f.getName(), img);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Dosya açılamadı: " + ex.getMessage());
        }
    }

    private void styleBtn(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
    }

    static class FileListRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean hasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, hasFocus);
            lbl.setIcon(UIManager.getIcon("FileView.fileIcon"));
            lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
            String[] parts = value.toString().split(" — ");
            lbl.setForeground(Color.BLACK);
            if (parts.length == 2)
                lbl.setText("<html><b>" + parts[0] + "</b>  <font color='black'>" + parts[1] + "</font></html>");
            return lbl;
        }
    }
}