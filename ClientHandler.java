import java.net.*;
import java.io.*;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public static ArrayList<ClientHandler> clients = new ArrayList<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter Username:");
            username = in.readLine();

            synchronized (clients) {
                clients.add(this);
            }
            broadcast(username + " joined the chat.");

            String message;

            while ((message = in.readLine()) !=null) {
                broadcast(username + ": " + message);
            }

        } catch (IOException e) {
            System.out.println(username + " disconnected.");
        } finally {
            disconnect();
        }

    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }

        }

    }
    public void disconnect() {
        try {
            synchronized (clients) {
                clients.remove(this);
            }
            broadcast(username + " left the chat.");

            socket.close();

        } catch (IOException e) {
            System.out.println("Error Closing Client.");
        }
    
    }

}