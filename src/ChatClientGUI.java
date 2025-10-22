// ------------------- ChatClientGUI.java -------------------
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;

public class ChatClientGUI {
    private JFrame frame;
    private JPanel chatPanel, usersPanel;
    private JScrollPane scrollPane;
    private JTextField inputField, targetField;
    private JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String clientUUID = UUID.randomUUID().toString();
    private String serverIP = null;
    private final int TCP_PORT = 5000;
    private final int UDP_PORT = 5001;

    // Playfair
    private static final String PLAYFAIR_KEY = "ŞİFRELEME";
    private PlayfairCipher cipher = new PlayfairCipher(PLAYFAIR_KEY);

    // Kullanıcıya özel mesajları tutacak map
    private Map<String, List<String>> messages = new HashMap<>();

    // Seçili kullanıcı
    private String selectedUser = null;

    public ChatClientGUI() { createStartupGUI(); }

    private void createStartupGUI() {
        username = JOptionPane.showInputDialog(null, "Kullanıcı adınızı girin:");
        if(username == null || username.trim().isEmpty()) System.exit(0);
        JOptionPane.showMessageDialog(null, "Sunucu aranıyor...");
        serverIP = discoverServerOrPromote();
        if(serverIP == null) {
            JOptionPane.showMessageDialog(null,"Sunucu başlatıldı: Siz sunucusunuz!");
            serverIP = "127.0.0.1";
            new Thread(() -> new Server(TCP_PORT, clientUUID).startServer()).start();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        } else {
            JOptionPane.showMessageDialog(null,"Sunucu bulundu: " + serverIP);
        }
        createChatGUI(serverIP, TCP_PORT);
    }

    private String discoverServerOrPromote() {
        final int attempts = 5;
        try(DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(500);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.setBroadcast(true);
            for(int i=0;i<attempts;i++){
                sendSocket.send(new DatagramPacket("DISCOVER_SERVER".getBytes(),
                        "DISCOVER_SERVER".length(),
                        InetAddress.getByName("255.255.255.255"), UDP_PORT));
                long endTime = System.currentTimeMillis() + 500;
                while(System.currentTimeMillis() < endTime){
                    try {
                        socket.receive(packet);
                        String msg = new String(packet.getData(),0,packet.getLength());
                        if(msg.startsWith("SERVER|")) return packet.getAddress().getHostAddress();
                    } catch(SocketTimeoutException ignored){}
                }
            }
        } catch(Exception e){}
        try { Thread.sleep(new Random().nextInt(800)+200); } catch(InterruptedException ignored) {}
        return null;
    }

    private void createChatGUI(String serverIP, int port) {
        frame = new JFrame("💬 Modern Chat - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700,600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(34,34,34));

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(34,34,34));
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        frame.add(scrollPane, BorderLayout.CENTER);

        usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(new Color(50,50,50));
        usersPanel.setPreferredSize(new Dimension(150,0));
        frame.add(usersPanel, BorderLayout.EAST);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(34,34,34));
        targetField = new JTextField();
        targetField.setEditable(false);
        targetField.setPreferredSize(new Dimension(100,30));
        inputField = new JTextField();
        sendButton = new JButton("kGönder");
        inputPanel.add(targetField, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        connectToServer(serverIP, port);
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        frame.setVisible(true);
    }

    private void connectToServer(String ip, int port){
        try {
            Socket socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(username);

            new Thread(() -> {
                try {
                    String line;
                    while((line=in.readLine())!=null){
                        if(line.startsWith("USERLIST|")) {
                            String[] users = line.substring(9).split(",");
                            SwingUtilities.invokeLater(() -> updateUserList(users));
                        } else {
                            String[] parts = line.split("\\|", 2);
                            if(parts.length < 2) continue;
                            String sender = parts[0];
                            String msgContent = parts[1];
                            messages.computeIfAbsent(sender, k -> new ArrayList<>()).add(sender + " → " + msgContent);
                            if(sender.equals(selectedUser)){
                                addMessageBubble(sender + " → " + msgContent, getCurrentTime(), false, new Color(64,64,64,180));
                            }
                        }
                    }
                } catch(IOException e){ e.printStackTrace(); }
            }).start();

        } catch(Exception e){ e.printStackTrace(); }
    }

    private void sendMessage(){
        String target = targetField.getText().trim();
        String message = inputField.getText().trim();
        if(target.isEmpty() || message.isEmpty()){
            JOptionPane.showMessageDialog(frame,"Mesaj boş olamaz!","Hata",JOptionPane.WARNING_MESSAGE);
            return;
        }
        String encrypted = cipher.encrypt(message);
        out.println(target + "|" + encrypted);
        messages.computeIfAbsent(target, k -> new ArrayList<>()).add("Ben → " + encrypted);
        if(target.equals(selectedUser)){
            addMessageBubble("Ben → " + encrypted, getCurrentTime(), true, new Color(0,150,136,180));
        }
        inputField.setText("");
    }

    private String getCurrentTime(){
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    private void addMessageBubble(String message, String time, boolean isRight, Color color){
        SwingUtilities.invokeLater(() -> {
            JPanel bubble = new JPanel(new FlowLayout(isRight?FlowLayout.RIGHT:FlowLayout.LEFT)){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),15,15);
                    super.paintComponent(g);
                }
            };
            bubble.setOpaque(false);
            JLabel label = new JLabel("<html><body style='display:inline-block; max-width:220px; word-wrap: break-word; padding:2px 4px; margin:0;'>" + message + "<br><span style='font-size:9px;color:#DDDDDD;'>" + time + "</span></body></html>");
            label.setFont(new Font("Arial", Font.PLAIN,11));
            label.setForeground(Color.WHITE);
            label.setOpaque(false);

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                boolean decrypted = false;
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if(e.getClickCount() == 2){
                        if(!decrypted){
                            try{
                                String encryptedPart = message.contains("→") ? message.split("→",2)[1].trim() : message;
                                String decryptedText = cipher.decrypt(encryptedPart);
                                label.setText("<html><span style='color:lightgreen;'>" + decryptedText + "</span></html>");
                                decrypted = true;
                            } catch(Exception ex){
                                label.setText("❌ Çözülemedi");
                            }
                        } else {
                            label.setText("<html><body style='display:inline-block; max-width:220px; word-wrap: break-word; padding:2px 4px; margin:0;'>" + message + "<br><span style='font-size:9px;color:#DDDDDD;'>" + time + "</span></body></html>");
                            decrypted = false;
                        }
                    }
                }
            });

            bubble.add(label);
            JPanel wrapper = new JPanel(new FlowLayout(isRight?FlowLayout.RIGHT:FlowLayout.LEFT));
            wrapper.setOpaque(false);
            wrapper.add(bubble);
            chatPanel.add(wrapper);
            chatPanel.revalidate();
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void updateUserList(String[] users){
        usersPanel.removeAll();
        for(String user : users){
            if(user.equals(username)) continue;
            JLabel userLabel = new JLabel(user);
            userLabel.setForeground(Color.WHITE);
            userLabel.setOpaque(true);
            if(user.equals(selectedUser)){
                userLabel.setBackground(new Color(0,200,0));
            } else {
                userLabel.setBackground(new Color(70,70,70));
            }
            userLabel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            userLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            userLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    targetField.setText(user);
                    showMessagesForUser(user);
                    selectedUser = user;
                    Component[] comps = usersPanel.getComponents();
                    for(Component c : comps){
                        if(c instanceof JLabel){
                            JLabel lbl = (JLabel) c;
                            if(lbl.getText().equals(selectedUser)){
                                lbl.setBackground(new Color(0,200,0));
                            } else {
                                lbl.setBackground(new Color(70,70,70));
                            }
                        }
                    }
                }
            });

            usersPanel.add(userLabel);
        }
        usersPanel.revalidate();
        usersPanel.repaint();
    }

    private void showMessagesForUser(String user){
        chatPanel.removeAll();
        List<String> userMessages = messages.getOrDefault(user, new ArrayList<>());
        for(String msg : userMessages){
            boolean isRight = msg.startsWith("Ben");
            addMessageBubble(msg, getCurrentTime(), isRight, isRight ? new Color(0,150,136,180) : new Color(64,64,64,180));
        }
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    public static void main(String[] args){ new ChatClientGUI(); }
}
