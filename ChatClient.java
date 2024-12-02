import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Scanner scanner = new Scanner(System.in);
            System.out.println("Connected to the server!");

            // Authentication
            System.out.println(in.readLine());
            out.println(scanner.nextLine());
            System.out.println(in.readLine());
            out.println(scanner.nextLine());

            String serverResponse = in.readLine();
            System.out.println(serverResponse);
            if (serverResponse.equals("Authentication failed!")) {
                return;
            }

            // Start a thread to listen for messages
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Sending messages
            while (true) {
                String message = scanner.nextLine();
                out.println(message);
                if (message.equals("/logout")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
