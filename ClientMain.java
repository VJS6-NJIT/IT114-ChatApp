import java.net.*;
import java.io.*;

public class ClientMain {
    public static void main(String[] args) {
        
        try (
            Socket socket = new Socket("localhost", 12345);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)

        ) {
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }

                } catch (IOException e) {
                    System.out.println("Disconnected.");
                }

            }).start();

            String userInput;

            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
            }

        } catch (IOException e) {
            System.out.println("Unable to connect.");
        }

    }

}