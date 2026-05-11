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
    public static HashMap<String, String> roomMoves = new HashMap<>();

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

                    loadChatHistory();

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

                // COMMAND: /rps
                else if (message.startsWith("/rps ")) {

                    handleRPS(message);
                }

                // NORMAL CHAT
                else {

                    String formattedMessage = "[" + currentRoom + "] " + username + ": " + message;

                    broadcastToRoom(formattedMessage);

                    saveMessage(formattedMessage);
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

    // SAVE CHAT TO FILE
    public void saveMessage(String message) {

        try {

            FileWriter writer =
                new FileWriter(currentRoom + ".txt", true);

            writer.write(message + "\n");

            writer.close();

        } catch (IOException e) {

            System.out.println("Error saving chat.");
        }
    }

    // LOAD CHAT HISTORY
    public void loadChatHistory() {

        try {

            BufferedReader reader =
                    new BufferedReader(
                        new FileReader(currentRoom + ".txt"));

            out.println("=== Chat History ===");

            String line;

        while ((line = reader.readLine()) != null) {

            out.println(line);
        }

            out.println("====================");

            reader.close();

        } catch (IOException e) {

        out.println("No previous chat history.");
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

    // ROCK PAPER SCISSORS
    public void handleRPS(String message) {

        String[] parts = message.split(" ", 2);

        if (parts.length < 2) {

            out.println("Usage: /rps rock|paper|scissors");
            return;
        }

        String move = parts[1].toLowerCase();

        // VALIDATE MOVE
        if (!move.equals("rock") &&
            !move.equals("paper") &&
            !move.equals("scissors")) {

            out.println("Invalid move.");
            return;
        }

            synchronized (roomMoves) {

            // FIRST PLAYER
            if (!roomMoves.containsKey(currentRoom)) {

                roomMoves.put(currentRoom, move);

                out.println("RPS move submitted. Waiting for opponent.");

            }

            // SECOND PLAYER
            else {

                String opponentMove = roomMoves.get(currentRoom);

                String result = determineWinner(opponentMove, move);

                broadcastToRoom("RPS Result in room [" + currentRoom + "]\n" + "Move 1: " + opponentMove + "\n" + "Move 2: " + move + "\n" + result);

                roomMoves.remove(currentRoom);
            }
        }
    }

    // DETERMINE WINNER
    public String determineWinner(String move1, String move2) {

        if (move1.equals(move2)) {

            return "Tie game!";
        }

        if (
            (move1.equals("rock") && move2.equals("scissors")) ||

                    (move1.equals("paper") && move2.equals("rock")) ||

                    (move1.equals("scissors") && move2.equals("paper"))
        ) {

            return "First player wins!";
        }

        return "Second player wins!";
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