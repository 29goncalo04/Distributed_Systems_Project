import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Worker implements Runnable{
    private Map<String, String> registeredUsers = new HashMap<String,String>(); // username e password
    private Queue<String> autenticatedUsers = new LinkedList<String>();

    // Registo de client

    // Se o maxClients == S então fazemos .await()

    // Lock para controlar o acesso ao número de clientes simultâneos
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition notFull = lock.newCondition();

    private final Socket clientSocket;
    public int n_autenticatedUsers = 0;
    public ClientManager cmanager;
    public int MAX_SESSIONS;

    public Worker(Socket clientSocket, ClientManager cmanager ,int MAX_SESSIONS) {
        this.clientSocket = clientSocket;
        this.cmanager = cmanager;
        this.MAX_SESSIONS = MAX_SESSIONS;
    }

    @Override
    public void run(){
        try{
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream())); 

            boolean authenticated = false;
            while (!authenticated) {
                try{
                    out.writeUTF("Choose an option: [register] or [login]");
                    out.flush();
                    String choice = in.readUTF();

                    switch (choice.toLowerCase()) {
                        case "register":
                            boolean usernameTaken = false;
                            while(usernameTaken==false){
                                out.writeUTF("Enter username:");
                                out.flush();
                                String username = in.readUTF();

                                out.writeUTF("Enter password:");
                                out.flush();
                                String password = in.readUTF();

                                if (cmanager.registerUser(username, password)) {
                                    lock.lock();
                                    try{
                                        registeredUsers.put(username, password);
                                        out.writeUTF("Registration successful. You can now log in.");
                                        out.flush();
                                        usernameTaken = true;
                                    } finally{
                                        lock.unlock();
                                    }
                                } else {
                                    out.writeUTF("Username already taken. Try again.");
                                    out.flush();
                                }
                            break;
                        }
                        case "login":
                            out.writeUTF("Enter username:");
                            out.flush();
                            String username = in.readUTF();
                            boolean passwordInvalid = false;
                            while(passwordInvalid == false){
                                out.writeUTF("Enter password:");
                                out.flush();
                                String password = in.readUTF();

                                if (cmanager.authenticateUser(username, password)) {
                                    lock.lock();
                                    try{
                                        autenticatedUsers.add(username);
                                        n_autenticatedUsers++;
                                        out.writeUTF("Login successful! Welcome, " + username + "!");
                                        out.flush();
                                        authenticated = true;
                                    } finally {
                                        lock.unlock();
                                    }

                                } else {
                                    out.writeUTF("Invalid credentials. Try again.");
                                    out.flush();
                                    passwordInvalid = true;
                                }
                            break;
                        }
                        default:
                            out.writeUTF("Invalid option. Please choose [register] or [login].");
                            out.flush();
                            break;
                    }
                } catch (Exception e){break;}
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    System.out.println("Client socket closed.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}