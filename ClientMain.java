import java.net.*;
import java.io.*;

public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try (
            Socket socket = new Socket(host, port);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in =new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            System.out.println("Connected to Server.");

            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }

                } catch (IOException e) {
                    System.out.println("Connection Closed.");
                }

            }).start();

            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
            }

        } catch (IOException e) {
            System.out.println("Unable to Connect to Server.");
        }
        
    }

}