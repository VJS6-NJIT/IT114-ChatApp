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

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(socket.getOutputStream(), true);

            // USERNAME SETUP
            while (true) {

                out.println("Enter username:");

                String attemptedName = in.readLine();

                if (attemptedName == null) {
                    return;
                }

                if (isUsernameTaken(attemptedName)) {
                    out.println("Username already taken.");
                } else {
                    username = attemptedName;
                    break;
                }
            }

            synchronized (clients) {
                clients.add(this);
            }

            broadcast("SERVER: " + username + " joined the chat.");

            String message;

            while ((message = in.readLine()) != null) {

                // COMMAND: /list
                if (message.equalsIgnoreCase("/list")) {
                    sendUserList();
                }

                // COMMAND: /leave
                else if (message.equalsIgnoreCase("/leave")) {
                    out.println("Disconnecting...");
                    break;
                }

                // NORMAL CHAT
                else {
                    broadcast(username + ": " + message);
                }
            }

        } catch (IOException e) {

            System.out.println(username + " disconnected unexpectedly.");

        } finally {

            disconnect();

        }
    }

    // SEND TO ALL USERS
    public void broadcast(String message) {

        synchronized (clients) {

            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }
    }

    // CHECK UNIQUE USERNAME
    public boolean isUsernameTaken(String name) {

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client.username != null &&
                        client.username.equalsIgnoreCase(name)) {

                    return true;
                }
            }
        }

        return false;
    }

    // SEND ACTIVE USERS
    public void sendUserList() {

        StringBuilder users = new StringBuilder();

        users.append("Active users: ");

        synchronized (clients) {

            for (ClientHandler client : clients) {

                users.append(client.username).append(" ");
            }
        }

        out.println(users.toString());
    }

    // CLEAN DISCONNECT
    public void disconnect() {

        try {

            synchronized (clients) {
                clients.remove(this);
            }

            if (username != null) {
                broadcast("SERVER: " + username + " left the chat.");
            }

            socket.close();

        } catch (IOException e) {

            System.out.println("Error closing socket.");
        }
    }
}