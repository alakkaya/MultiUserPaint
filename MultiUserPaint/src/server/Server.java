package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    public static final int PORT = 5005;

    // Bağlı tüm istemciler
    public static Map<String, ClientHandler> sessions =
        new ConcurrentHashMap<>();

    // Paylaşılan dosyalar
    public static Map<String, SharedFile> sharedFiles =
        new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(PORT);
        System.out.println("Sunucu başladı: " + PORT);
        while (true) {
            Socket s = ss.accept();
            new Thread(new ClientHandler(s)).start();
        }
    }

    public static void broadcastToFile(String fileName, String msg, String senderName) {
        for (ClientHandler h : sessions.values()) {
            if (!h.getUsername().equals(senderName) && h.hasJoinedFile(fileName)) {
                h.send(msg);
            }
        }
    }

    public static void broadcastAll(String msg, String senderName) {
        for (ClientHandler h : sessions.values()) {
            if (!h.getUsername().equals(senderName)) {
                h.send(msg);
            }
        }
    }

    public static String buildFileListMsg() {
        if (sharedFiles.isEmpty()) return "FILE_LIST";
        StringBuilder sb = new StringBuilder("FILE_LIST");
        for (SharedFile f : sharedFiles.values()) {
            sb.append("|").append(f.getName()).append(":").append(f.getOwner());
        }
        return sb.toString();
    }
}