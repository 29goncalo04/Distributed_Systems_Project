import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClientManager implements Runnable{
    private static final Map<String, String> registeredUsers; // nome e password
    private int MAX_SESSIONS;

    // Registo de client

    // Se o maxClients == S então fazemos .await()

    // Lock para controlar o acesso ao número de clientes simultâneos
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition notFull = lock.newCondition();
    private static int currentClients = 0;

    private final Socket clientSocket;

    public ClientManager(Socket clientSocket, int MAX_SESSIONS) {
        this.clientSocket = clientSocket;
        this.MAX_SESSIONS = MAX_SESSIONS;
    }

    @Override
    public void run(){
        try{
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream())); 

            out.writeUTF("Welcome to the service. Please authenticate or register.");

            boolean authenticated = false;
            while (!authenticated) {
                out.writeUTF("Choose an option: [register] or [login]");
                String choice = in.readUTF();

                switch (choice.toLowerCase()) {
                    case "register":{
                        out.writeUTF("Enter username:");
                        String username = in.readUTF();

                        out.writeUTF("Enter password:");
                        String password = in.readUTF();

                        if (registerUser(username, password)) {
                            out.writeUTF("Registration successful. You can now log in.");
                        } else {
                            out.writeUTF("Username already taken. Try again.");
                        }
                    }
                    case "login":{
                        out.writeUTF("Enter username:");
                        String username = in.readUTF();

                        out.writeUTF("Enter password:");
                        String password = in.readUTF();

                        if (authenticateUser(username, password)) {
                            out.writeUTF("Login successful! Welcome, " + username + "!");
                            authenticated = true;
                        } else {
                            out.writeUTF("Invalid credentials. Try again.");
                        }
                    }
                    default:{
                        out.writeUTF("Invalid option. Please choose [register] or [login].");
                    }
                }
            }

            // Aguardar até que haja espaço disponível para clientes
            lock.lock();
            try {
                while (currentClients >= Server.MAX_SESSIONS) {
                    out.writeUTF("Server is full. Waiting for an available slot...");
                    notFull.await();
                }
                currentClients++;
            } finally {
                lock.unlock();
            }

            // Proseguir com o serviço após autenticação
            try {
                handleClientSession(out, in);
            } finally {
                lock.lock();
                try {
                    currentClients--;
                    notFull.signal(); // Notificar threads em espera
                } finally {
                    lock.unlock();
                }
            }

            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}