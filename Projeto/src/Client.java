import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to the server.");

            // Thread para ler mensagens do servidor
            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while (true) {
                        serverMessage = in.readUTF(); // Lê mensagens do servidor
                        if (serverMessage == null || serverMessage.equalsIgnoreCase("Goodbye!")) {  //ignora se é maiuscula ou minuscula
                            break; // Sai do loop se o servidor encerrar a conexão
                        }
                        System.out.println(serverMessage); // Printa a mensagem do servidor
                    }
                } catch (IOException e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            });

            // Thread para enviar mensagens ao servidor
            Thread writeThread = new Thread(() -> {
                try {
                    String userInput;
                    while (true) {
                        userInput = console.readLine();

                        if (userInput == null || userInput.equalsIgnoreCase("exit")) {
                            break; // Sai da thread se o user escrever "exit"
                        }

                        out.writeUTF(userInput); // Envia a mensagem ao servidor
                        out.flush(); // Garante que a mensagem seja enviada
                    }
                } catch (IOException e) {
                    System.out.println("Error writing to server: " + e.getMessage());
                }
            });

            // Inicia ambas as threads
            readThread.start();
            writeThread.start();

            // Aguarda a conclusão da thread de escrita
            writeThread.join();

            // Encerra a thread de leitura, se ainda estiver ativa
            readThread.interrupt();  //caso esteja encravada é mau usar join()
        } catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("Client disconnected.");
    }
}
