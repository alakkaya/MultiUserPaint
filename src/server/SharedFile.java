package server;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.*;

public class SharedFile {
	private final String name;
	private final String owner;
	private BufferedImage canvas;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static final String SAVE_DIR = "server_records";

	public SharedFile(String name, String owner, BufferedImage initial) {
		this.name = name;
		this.owner = owner;
		if (initial != null) {
			this.canvas = initial;
		} else {
			this.canvas = new BufferedImage(800, 560, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = this.canvas.createGraphics();
			g.setColor(java.awt.Color.WHITE);
			g.fillRect(0, 0, 800, 560);
			g.dispose();
		}
		// 10 saniyede otomatik kaydediyoruz
		scheduler.scheduleAtFixedRate(this::saveToDisk, 10, 10, TimeUnit.SECONDS);
		System.out.println("SharedFile oluşturuldu: " + name);
	}

	public synchronized void applyDraw(int x1, int y1, int x2, int y2, java.awt.Color c, int size) {
		java.awt.Graphics2D g = canvas.createGraphics();
		g.setColor(c);
		g.setStroke(new java.awt.BasicStroke(size, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
		g.drawLine(x1, y1, x2, y2);
		g.dispose();
	}

	public synchronized void applyCut(int x, int y, int w, int h) {
		java.awt.Graphics2D g = canvas.createGraphics();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(x, y, w, h);
		g.dispose();
	}

	public synchronized void applyPaste(int x, int y, BufferedImage img) {
		java.awt.Graphics2D g = canvas.createGraphics();
		g.drawImage(img, x, y, null);
		g.dispose();
	}

	public synchronized void applyClear() {
		java.awt.Graphics2D g = canvas.createGraphics();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		g.dispose();
	}

	public synchronized String toBase64() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(canvas, "png", baos);
			return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (IOException e) {
			return "";
		}
	}

	public synchronized void saveToDisk() {
		try {
			File dir = new File(SAVE_DIR);
			dir.mkdirs();
			ImageIO.write(canvas, "png", new File(dir, name));
			System.out.println("Otomatik kaydedildi: " + name);
		} catch (IOException e) {
			System.err.println("Kayıt hatası: " + e.getMessage());
		}
	}

	public String getName() {
		return name;
	}

	public String getOwner() {
		return owner;
	}
}