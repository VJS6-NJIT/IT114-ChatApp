import java.net.*;
import java.io.*;
import java.util.*;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String currentRoom = "Lobby";

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

            broadcastToRoom("SERVER: " + username + " joined the room.");

            String message;

            while ((message = in.readLine()) != null) {

                // COMMAND: /list
                if (message.equalsIgnoreCase("/list")) {
                    sendUserList();
                }

                // COMMAND: /join
                else if (message.startsWith("/join ")) {

                String[] parts = message.split(" ", 2);

                 if (parts.length < 2) {

                      out.println("Usage: /join roomName");

                } else {

                    String oldRoom = currentRoom;

                    currentRoom = parts[1];

                    out.println("Joined room: " + currentRoom);

                    broadcastToRoom("SERVER: " + username + " joined the room.");

                    System.out.println(username + " moved from " +
                        oldRoom + " to " + currentRoom);
                    }
                }

                // COMMAND: /leave
                else if (message.equalsIgnoreCase("/leave")) {
                    out.println("Disconnecting...");
                    break;
                }

                // COMMAND: /dm
                else if (message.startsWith("/dm ")) {

                    handleDirectMessage(message);
                }

                // NORMAL CHAT
                else {
                    broadcastToRoom("[" + currentRoom + "] " + username + ": " + message);
                }
            }

        } catch (IOException e) {

            System.out.println(username + " disconnected unexpectedly.");

        } finally {

            disconnect();

        }
    }

    // ROOM-ONLY BROADCAST
    public void broadcastToRoom(String message) {

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client.currentRoom.equalsIgnoreCase(this.currentRoom)) {

                    client.out.println(message);
                }
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

    // DIRECT MESSAGE
    public void handleDirectMessage(String message) {

        String[] parts = message.split(" ", 3);

        // VALIDATION
        if (parts.length < 3) {
            out.println("Usage: /dm username message");
            return;
        }

        String targetUsername = parts[1];
        String dmMessage = parts[2];

        synchronized (clients) {

            for (ClientHandler client : clients) {

                if (client.username.equalsIgnoreCase(targetUsername)) {

                    // SEND TO TARGET
                    client.out.println("[DM from " + username + "]: " + dmMessage);

                    // SEND BACK TO SENDER
                    out.println("[DM to " + targetUsername + "]: " + dmMessage);

                    return;
                }
            }
        }

        out.println("User not found.");
    }

    // CLEAN DISCONNECT
    public void disconnect() {

        try {

            synchronized (clients) {
                clients.remove(this);
            }

            if (username != null) {
                broadcastToRoom("SERVER: " + username + " left the room.");
            }

            socket.close();

        } catch (IOException e) {

            System.out.println("Error closing socket.");
        }
    }
}