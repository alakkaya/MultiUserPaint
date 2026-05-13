package server;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    // Bu istemcinin açık olduğu dosyalar
    private final Set<String> joinedFiles = Collections.synchronizedSet(new HashSet<>());

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            System.out.println("İstemci bağlantısı koptu: " + username);
        } finally {
            cleanup();
        }
    }

    private void handleMessage(String msg) throws IOException {
        String[] p = msg.split("\\|", -1);
        switch (p[0]) {

            case "CONNECT": {
                String name = p[1];
                if (Server.sessions.containsKey(name)) {
                    send("ERROR|USERNAME_TAKEN");
                    socket.close();
                    return;
                }
                username = name;
                Server.sessions.put(username, this);
                send("OK|CONNECTED");
                send(Server.buildFileListMsg());
                Server.broadcastAll("USER_JOINED|" + username, username);
                System.out.println("Bağlandı: " + username);
                break;
            }

            case "SHARE_FILE": {
                // SHARE_FILE|dosyaAdi|base64icerik(yoksa boş)
                String fileName = p[1];
                BufferedImage initial = null;
                if (p.length > 2 && !p[2].isEmpty()) {
                    initial = decodeImage(p[2]);
                }
                SharedFile sf = new SharedFile(fileName, username, initial);
                Server.sharedFiles.put(fileName, sf);
                // Paylaşan kişiye de canvas sync gönder
                send("CANVAS_SYNC|" + fileName + "|" + sf.toBase64());
                joinedFiles.add(fileName);
                Server.broadcastAll("FILE_ADDED|" + fileName + "|" + username, username);
                System.out.println(username + " dosya paylaştı: " + fileName);
                break;
            }

            case "JOIN_FILE": {
                // JOIN_FILE|dosyaAdi
                String fileName = p[1];
                SharedFile sf = Server.sharedFiles.get(fileName);
                if (sf == null) {
                    send("ERROR|FILE_NOT_FOUND");
                    return;
                }
                joinedFiles.add(fileName);
                send("CANVAS_SYNC|" + fileName + "|" + sf.toBase64());
                break;
            }

            case "DRAW": {
                // DRAW|dosya|x1|y1|x2|y2|renk|boyut
                String fileName = p[1];
                SharedFile sf = Server.sharedFiles.get(fileName);
                if (sf == null) return;
                int x1   = Integer.parseInt(p[2]);
                int y1   = Integer.parseInt(p[3]);
                int x2   = Integer.parseInt(p[4]);
                int y2   = Integer.parseInt(p[5]);
                Color c  = Color.decode(p[6]);
                int size = Integer.parseInt(p[7]);
                sf.applyDraw(x1, y1, x2, y2, c, size);
                Server.broadcastToFile(fileName, msg, username);
                break;
            }

            case "CUT": {
                // CUT|dosya|x|y|w|h
                String fileName = p[1];
                SharedFile sf = Server.sharedFiles.get(fileName);
                if (sf == null) return;
                sf.applyCut(Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5]));
                Server.broadcastToFile(fileName, msg, username);
                break;
            }

            case "PASTE": {
                // PASTE|dosya|x|y|base64
                String fileName = p[1];
                SharedFile sf = Server.sharedFiles.get(fileName);
                if (sf == null) return;
                BufferedImage img = decodeImage(p[4]);
                if (img != null)
                    sf.applyPaste(Integer.parseInt(p[2]), Integer.parseInt(p[3]), img);
                Server.broadcastToFile(fileName, msg, username);
                break;
            }

            case "CLEAR": {
                String fileName = p[1];
                SharedFile sf = Server.sharedFiles.get(fileName);
                if (sf == null) return;
                sf.applyClear();
                Server.broadcastToFile(fileName, msg, username);
                break;
            }

            case "DISCONNECT":
                cleanup();
                socket.close();
                break;
        }
    }

    private void cleanup() {
        if (username != null) {
            Server.sessions.remove(username);
            Server.broadcastAll("USER_LEFT|" + username, username);
            // Kaydet
            for (String f : joinedFiles) {
                SharedFile sf = Server.sharedFiles.get(f);
                if (sf != null) sf.saveToDisk();
            }
        }
    }

    private BufferedImage decodeImage(String b64) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(b64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) { return null; }
    }

    public void send(String msg)              { out.println(msg); }
    public String getUsername()               { return username; }
    public boolean hasJoinedFile(String name) { return joinedFiles.contains(name); }
}