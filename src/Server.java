// ------------------- Server.java -------------------
import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Server {
    private int port;
    private String serverUUID;
    private static final int UDP_PORT = 5001;
    private Map<String, ClientHandler> clients = new HashMap<>();

    public Server(int port, String uuid) {
        this.port = port;
        this.serverUUID = uuid;
        DatabaseHelper.init(); // DB tablolarını oluştur
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            new Thread(this::broadcastPresence).start();
            System.out.println(" Sunucu başlatıldı, bağlantılar bekleniyor...");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastPresence() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] data = ("SERVER|" + serverUUID).getBytes();
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getByName("255.255.255.255"), UDP_PORT);
            while (true) {
                socket.send(packet);
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void addClient(String username, ClientHandler handler) {
        DatabaseHelper.saveUser(username); // DB’ye kaydet
        clients.put(username, handler);
        updateUserLists();
        System.out.println("👤 Yeni kullanıcı bağlandı: " + username);
    }

    public synchronized void removeClient(String username) {
        clients.remove(username);
        updateUserLists();
        System.out.println("❌ Kullanıcı ayrıldı: " + username);
    }

    private synchronized void updateUserLists() {
        String list = "USERLIST|" + String.join(",", clients.keySet());
        for (ClientHandler ch : clients.values()) {
            ch.sendMessage(list);
        }
    }

    // 🔹 Mesajı sadece hedef kullanıcıya gönder ve DB’ye kaydet
    public synchronized void sendPrivateMessage(String fromUser, String toUser, String message) {
        DatabaseHelper.saveMessage(fromUser, toUser, message); // DB kaydı
        ClientHandler target = clients.get(toUser);
        if (target != null) {
            target.sendMessage(fromUser + "|" + message);
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            username = in.readLine();
            server.addClient(username, this);

            loadPreviousMessages(); // eski mesajları yükle

            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        String target = parts[0];
                        String msg = parts[1];
                        server.sendPrivateMessage(username, target, msg);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("⚠ Bağlantı hatası: " + username);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            server.removeClient(username);
        }
    }

    private void loadPreviousMessages() {
        try (ResultSet rs = DatabaseHelper.getMessagesForUser(username)) {
            while (rs != null && rs.next()) {
                String sender = rs.getString("sender");
                String receiver = rs.getString("receiver");
                String message = rs.getString("message");
                this.sendMessage(sender + "|" + message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }
}
