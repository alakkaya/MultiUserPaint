package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class CanvasPanel extends JPanel {

    public interface DrawListener {
        void onDraw(int x1, int y1, int x2, int y2, String hexColor, int size);
        void onCut(int x, int y, int w, int h);
        void onPaste(int x, int y, BufferedImage img);
        void onClear();
    }

    public DrawListener drawListener;

    private final BufferedImage image;
    private final Graphics2D g2d;
    private Color brushColor = Color.BLACK;
    private int brushSize = 4;
    private boolean selectMode = false;

    private int lastX = -1, lastY = -1;
    private Point selStart, selEnd;
    private Rectangle selection;
    private BufferedImage clipboard;
    private int popupX = 10, popupY = 10;

    public CanvasPanel(int w, int h) {
        setPreferredSize(new Dimension(w, h));
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clearCanvas();
        setupMouse();
        setupPopup();
    }

    //Çizim

    public void drawLine(int x1, int y1, int x2, int y2, Color c, int size) {
        g2d.setColor(c);
        g2d.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
        repaint();
    }

    public void applyRemoteDraw(int x1, int y1, int x2, int y2, String hex, int size) {
        drawLine(x1, y1, x2, y2, Color.decode(hex), size);
    }

    public void applyRemoteCut(int x, int y, int w, int h) {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, w, h);
        repaint();
    }

    public void applyRemotePaste(int x, int y, BufferedImage img) {
        g2d.drawImage(img, x, y, null);
        repaint();
    }

    public void clearCanvas() {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        repaint();
    }

    public void loadImage(BufferedImage img) {
        g2d.drawImage(img, 0, 0, null);
        repaint();
    }

    //Mouse 

    private void setupMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                if (selectMode) { selStart = e.getPoint(); selEnd = e.getPoint(); selection = null; }
                else            { lastX = e.getX(); lastY = e.getY(); }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (selectMode) {
                    selEnd = e.getPoint();
                    selection = makeRect(selStart, selEnd);
                    repaint();
                } else {
                    int cx = e.getX(), cy = e.getY();
                    drawLine(lastX, lastY, cx, cy, brushColor, brushSize);
                    if (drawListener != null) {
                        String hex = colorToHex(brushColor);
                        drawListener.onDraw(lastX, lastY, cx, cy, hex, brushSize);
                    }
                    lastX = cx; lastY = cy;
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (selectMode && selStart != null)
                    selection = makeRect(selStart, selEnd);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void setupPopup() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copy  = new JMenuItem("Kopyala");
        JMenuItem cut   = new JMenuItem("Kes");
        JMenuItem paste = new JMenuItem("Yapıştır");
        JMenuItem clear = new JMenuItem("Tuvali Temizle");
        menu.add(copy); menu.add(cut); menu.add(paste);
        menu.addSeparator(); menu.add(clear);

        copy.addActionListener(e  -> doCopy(false));
        cut.addActionListener(e   -> doCopy(true));
        paste.addActionListener(e -> doPaste());
        clear.addActionListener(e -> {
            clearCanvas();
            if (drawListener != null) drawListener.onClear();
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  {
                if (e.isPopupTrigger()) {
                    setPasteLocation(e.getX(), e.getY());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    setPasteLocation(e.getX(), e.getY());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void doCopy(boolean cut) {
        if (selection == null || selection.width < 2 || selection.height < 2) return;
        int sx = Math.max(0, selection.x);
        int sy = Math.max(0, selection.y);
        int sw = Math.min(selection.width,  image.getWidth()  - sx);
        int sh = Math.min(selection.height, image.getHeight() - sy);
        if (sw <= 0 || sh <= 0) return;

        BufferedImage sub = image.getSubimage(sx, sy, sw, sh);
        clipboard = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        clipboard.getGraphics().drawImage(sub, 0, 0, null);

        if (cut) {
            g2d.setColor(Color.WHITE);
            g2d.fill(selection);
            repaint();
            if (drawListener != null) drawListener.onCut(sx, sy, sw, sh);
        }
        selection = null;
    }

    private void doPaste() {
        if (clipboard == null) return;
        int px = popupX;
        int py = popupY;
        g2d.drawImage(clipboard, px, py, null);
        repaint();
        if (drawListener != null) drawListener.onPaste(px, py, clipboard);
    }

    // Helpers 
    private Rectangle makeRect(Point a, Point b) {
        return new Rectangle(Math.min(a.x,b.x), Math.min(a.y,b.y),
                             Math.abs(a.x-b.x), Math.abs(a.y-b.y));
    }

    private String colorToHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public void setBrushColor(Color c)    { brushColor = c; }
    public void setBrushSize(int s)       { brushSize = s; }
    public void setSelectMode(boolean b)  {
        selectMode = b;
        setCursor(b ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    : Cursor.getDefaultCursor());
    }
    public BufferedImage getImage()       { return image; }

    public void setPasteLocation(int x, int y) {
        popupX = x;
        popupY = y;
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
        if (selection != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(30, 144, 255, 35));
            g2.fill(selection);
            g2.setColor(new Color(30, 144, 255, 230));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{8, 5}, 0));
            g2.draw(selection);
        }
    }
}