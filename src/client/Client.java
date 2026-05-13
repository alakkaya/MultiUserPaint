package client;

import java.io.*;
import java.net.*;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public interface MessageListener {
        void onMessage(String msg);
    }

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public void startListening(MessageListener listener) {
        Thread t = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null)
                    listener.onMessage(msg);
            } catch (IOException e) {
                listener.onMessage("ERROR|CONNECTION_LOST");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void disconnect() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}