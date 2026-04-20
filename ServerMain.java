import java.net.*;
import java.io.*;

public class ServerMain {

    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Re-AIM Server Started...");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                client.start();
            }

        } catch (IOException e) {
            System.out.println("Server Error");
        }
        
    }

}