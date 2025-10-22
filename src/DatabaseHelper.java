import java.sql.*;

public class DatabaseHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/chatapp?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root"; // XAMPP varsayılan kullanıcı
    private static final String PASS = ""; // XAMPP genelde şifresizdir, eğer varsa seninki 1234 kalabilir

    public static Connection getConnection() throws SQLException {
        try {
            // MySQL sürücüsünü yükle (önemli!)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("❌ MySQL JDBC sürücüsü bulunamadı! mysql-connector-j .jar eklenmiş mi?");
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Kullanıcılar tablosu
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL UNIQUE
                )
            """);

            // Mesajlar tablosu
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    sender VARCHAR(100) NOT NULL,
                    receiver VARCHAR(100) NOT NULL,
                    message TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            System.out.println("✅ MySQL bağlantısı başarılı, tablolar hazır.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Kullanıcıyı ekle (varsa eklemez)
    public static void saveUser(String username) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT IGNORE INTO users(username) VALUES(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Mesaj kaydet
    public static void saveMessage(String sender, String receiver, String message) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO messages(sender, receiver, message) VALUES(?,?,?)")) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Kullanıcıya ait tüm mesajları çek
    public static ResultSet getMessagesForUser(String username) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT sender, receiver, message, timestamp FROM messages " +
                            "WHERE sender=? OR receiver=? ORDER BY id ASC");
            ps.setString(1, username);
            ps.setString(2, username);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
