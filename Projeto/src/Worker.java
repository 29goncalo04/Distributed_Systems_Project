import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Worker implements Runnable{
    // Registo de client

    // Se o maxClients == S então fazemos .await()

    // Lock para controlar o acesso ao número de clientes simultâneos
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition notFull = lock.newCondition();

    private final Socket clientSocket;
    public ClientManager cmanager;
    public int MAX_SESSIONS;
    public int n_authenticatedUsers = 0;

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
            String username = "";
            String password = "";
            boolean authenticated = false;

            out.writeUTF("Choose an option: [register] or [login]");
            out.flush();

            while (!authenticated) {
                try{
                    String choice = in.readUTF();

                    switch (choice.toLowerCase()) {
                        case "register":
                            out.writeUTF("Enter username:");
                            out.flush();
                            username = in.readUTF();
                            out.writeUTF("Enter password:");
                            out.flush();
                            password = in.readUTF();
                            if (cmanager.registerUser(username, password)) {
                                out.writeUTF("Registration successful.\nChoose an option: [register] or [login]");
                                out.flush();
                            } else {
                                out.writeUTF("Username already taken. Try again.\nChoose an option: [register] or [login]");
                                out.flush();
                            }
                            break;

                        case "login":
                            // Verifica se o número máximo de sessões foi atingido
                            lock.lock();
                            try{
                                while(n_authenticatedUsers >= MAX_SESSIONS){
                                    // Aguarda até que haja espaço para novos usuários
                                    notFull.await();
                                }
                            } finally {
                                lock.unlock();
                            }

                            out.writeUTF("Enter username:");
                            out.flush();
                            username = in.readUTF();
                            out.writeUTF("Enter password:");
                            out.flush();
                            password = in.readUTF();

                            if (cmanager.authenticateUser(username, password)) {
                                // Autenticação bem-sucedida
                                n_authenticatedUsers++;
                                out.writeUTF("Login successful! Welcome, " + username + "!\nChoose an option: [put], [get], [multiget], [multiput], or [exit]");
                                out.flush();
                                authenticated = true;
                            } else {
                                out.writeUTF("Invalid credentials. Try again.\nChoose an option: [register] or [login]");
                                out.flush();
                            }
                            break;
                        default:
                            out.writeUTF("Invalid option. Please choose [register] or [login].");
                            out.flush();
                            break;
                    }
                } catch (Exception e){break;}
            }
            try{
                cmanager.dataHandle(clientSocket,in,out);
            } finally {
                // Quando o cliente desconectar, reduz a contagem de usuários autenticados
                lock.lock();
                try {
                    n_authenticatedUsers--;
                    notFull.signal(); // Notifica uma thread aguardando para permitir novo login
                } finally {
                    lock.unlock();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}