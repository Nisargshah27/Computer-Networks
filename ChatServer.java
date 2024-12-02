import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    // Map to store username and plain-text password
    private static final Map<String, String> users = new HashMap<>();
    // Map to store active client handlers
    private static final Map<String, ClientHandler> onlineClients = new HashMap<>();
    // Map to store the status of all clients
    private static final Map<String, String> clientStatus = new HashMap<>();

    static {
        // Storing users with plain-text passwords
        users.put("alice", "pass123");
        users.put("bob", "pass456");
        users.put("charlie", "pass789");
        users.put("diana", "pass101");
        users.put("eve", "pass202");

        // Initialize all users as "Offline"
        for (String user : users.keySet()) {
            clientStatus.put(user, "Offline");
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server with Messaging and File Transfer is running...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected!");
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Authentication
                out.println("Enter username:");
                username = in.readLine();
                out.println("Enter password:");
                String password = in.readLine();

                if (authenticate(username, password)) {
                    out.println("Authentication successful!");
                    onlineClients.put(username, this);
                    updateStatus(username, "Online");
                    broadcast(username + " has joined the chat!");

                    // Handle commands
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("/msg")) {
                            handlePrivateMessage(message);
                        } else if (message.startsWith("/file")) {
                            handleFileTransfer(message);
                        } else if (message.equals("/status")) {
                            displayStatus();
                        } else if (message.equals("/logout")) {
                            break;
                        } else {
                            broadcast(username + ": " + message);
                        }
                    }
                } else {
                    out.println("Authentication failed! Disconnecting...");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                logout();
            }
        }

        private boolean authenticate(String username, String password) {
            return users.containsKey(username) && users.get(username).equals(password);
        }

        private void handlePrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                out.println("Invalid command. Use /msg <recipient> <message>");
                return;
            }
            String recipient = parts[1];
            String msg = parts[2];
            sendMessage(recipient, username + ": " + msg);
        }

        private void handleFileTransfer(String command) {
            String[] parts = command.split(" ", 3);
            if (parts.length < 3) {
                out.println("Invalid command. Use /file <recipient> <filepath>");
                return;
            }
            String recipient = parts[1];
            String filePath = parts[2];

            File file = new File(filePath);
            if (!file.exists()) {
                out.println("File does not exist: " + filePath);
                return;
            }

            ClientHandler recipientHandler = onlineClients.get(recipient);
            if (recipientHandler == null) {
                out.println("Recipient " + recipient + " is not online.");
                return;
            }

            try {
                // Send file to recipient
                recipientHandler.out.println("Receiving file from " + username + ": " + file.getName());
                recipientHandler.out.println("File size: " + file.length() + " bytes");

                BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
                OutputStream recipientOut = recipientHandler.socket.getOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) > 0) {
                    recipientOut.write(buffer, 0, bytesRead);
                }

                fileIn.close();
                recipientOut.flush();
                out.println("File sent successfully to " + recipient);
            } catch (IOException e) {
                out.println("Error sending file: " + e.getMessage());
            }
        }

        private void sendMessage(String recipient, String message) {
            ClientHandler clientHandler = onlineClients.get(recipient);
            if (clientHandler != null) {
                clientHandler.out.println(message);
            } else {
                out.println("User " + recipient + " is not online.");
            }
        }

        private void broadcast(String message) {
            for (ClientHandler client : onlineClients.values()) {
                client.out.println(message);
            }
        }

        private void displayStatus() {
            out.println("=== Client Status ===");
            for (Map.Entry<String, String> entry : clientStatus.entrySet()) {
                out.println(entry.getKey() + ": " + entry.getValue());
            }
        }

        private void updateStatus(String username, String status) {
            clientStatus.put(username, status);
            System.out.println("Updated status: " + username + " -> " + status);
        }

        private void logout() {
            if (username != null) {
                onlineClients.remove(username);
                updateStatus(username, "Offline");
                broadcast(username + " has left the chat!");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
