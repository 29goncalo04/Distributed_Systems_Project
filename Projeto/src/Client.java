import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private boolean emEspera = false;
    private ReentrantLock lock = new ReentrantLock();
    Condition serverReady = lock.newCondition();

    public boolean isEmEspera() {
        lock.lock();
        try{
            return emEspera;
        } finally {
            lock.unlock();
        }
    }

    public void setEmEspera(boolean value) {
        lock.lock();
        try{
            emEspera = value;
        } finally {
            lock.unlock();
        }
    }
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))){
            

            Client client = new Client();
            
            System.out.println("Connected to the server.");

            // Thread para ler mensagens do servidor
            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readUTF()) != null) {
                        if(serverMessage.equalsIgnoreCase("Server is full. You are in the waiting queue. Please wait until a slot becomes available. Use [exit] to leave the queue")){
                            client.lock.lock();
                            try {
                                client.setEmEspera(true);
                            } finally {
                                client.lock.unlock();
                            }
                        } else if(serverMessage.equalsIgnoreCase("Choose an option: [register], [login] or [exit]")){
                            client.lock.lock(); 
                            try{
                                client.setEmEspera(false);
                                client.serverReady.signal(); 
                            } finally {
                                client.lock.unlock();
                            }
                        }
                        else if (serverMessage == null || serverMessage.equalsIgnoreCase("Goodbye!")) {  //ignora se é maiuscula ou minuscula
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
                    BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
                    while (true) {
                        client.lock.lock(); 
                        try{
                            while(client.isEmEspera()){
                                client.serverReady.await();   //espera até ser possível enviar mensagens ao server    
                            }

                            // Limpa o buffer do console após sair da espera
                            while (console.ready()) {
                                console.readLine(); // Descarta entradas não processadas
                            }
                            
                            if ((userInput = console.readLine()) != null) {
                                if (userInput.equalsIgnoreCase("exit")) {
                                    break; // Encerra o cliente
                                }
                            out.writeUTF(userInput); // Envia a mensagem ao servidor
                            out.flush();
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally{
                            client.lock.unlock();
                        }

                        // Lê a entrada do usuário após sair da espera
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
