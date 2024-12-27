import java.io.*;
import java.net.Socket;

public class Simulator implements Runnable {
    private final int clientId;

    public Simulator(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket("localhost", 12345);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            String serverMessage;
            out.writeUTF("register");
            out.flush();
            System.out.println("Client " + clientId + ": Sent register");

            serverMessage = in.readUTF();
            System.out.println("Client " + clientId + ": " + serverMessage);

            out.writeUTF("user" + clientId + "\npassword" + clientId);
            out.flush();
            //out.writeUTF("password" + clientId);
            //out.flush();

            serverMessage = in.readUTF();
            System.out.println("Client " + clientId + ": " + serverMessage);

            out.writeUTF("put");
            out.flush();
            serverMessage = in.readUTF();
            System.out.println("Client " + clientId + ": " + serverMessage);

            out.writeUTF("key" + clientId + "\nvalue" + clientId + "END");
            out.flush();
            //out.writeUTF("value" + clientId + "END");
            //out.flush();

            serverMessage = in.readUTF();
            System.out.println("Client " + clientId + ": " + serverMessage);

        } catch (IOException e) {
            System.err.println("Client " + clientId + " encountered an error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int numberOfClients = 10; // Número de clientes para testar a concorrência
        Thread[] clients = new Thread[numberOfClients];

        for (int i = 0; i < numberOfClients; i++) {
            clients[i] = new Thread(new Simulator(i));
            clients[i].start();
        }

        // Espera que todas as threads terminem
        for (int i = 0; i < numberOfClients; i++) {
            try {
                clients[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}