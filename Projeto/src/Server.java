import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_SESSIONS = 2; 
    public static final ClientManager cmanager = new ClientManager();
    private static final Queue<Socket> waitingQueue = new LinkedList<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition isAvailable = lock.newCondition();
    private static int activeSessions = 0;
    
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                lock.lock();
                try{
                    if(activeSessions >= MAX_SESSIONS){
                        waitingQueue.add(clientSocket);
                        startWaitingThread(clientSocket);
                    } else {
                        activeSessions++;
                        startWorker(clientSocket);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally{
            if(serverSocket != null){
                serverSocket.close();
            }
        }
    }

    private static void startWorker(Socket clienSocket){
        Worker worker = new Worker(clienSocket, cmanager);
        Thread t = new Thread(worker);
        t.start();
    }

    private static void startWaitingThread(Socket clientSocket) {
        Thread waitingThread = new Thread(() -> {
            try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                 DataInputStream in = new DataInputStream(clientSocket.getInputStream())) {
                out.writeUTF("Server is full. You are in the waiting queue. You can [register] while waiting.");
                out.flush();

                while (true) {
                    lock.lock();
                    try {
                        if (activeSessions < MAX_SESSIONS) {
                            activeSessions++;
                            startWorker(clientSocket);
                            break;
                        } else if (in.available() > 0) {
                            String message = in.readUTF();
                            if (message.equals("exit")) {
                                out.writeUTF("Goodbye!");
                                out.flush();
                                clientSocket.close();
                                break;
                            } else if (message.equals("register")) {
                                out.writeUTF("Enter username:");
                                out.flush();
                                String username = in.readUTF();
                                out.writeUTF("Enter password:");
                                out.flush();
                                String password = in.readUTF();

                                if (cmanager.registerUser(username, password)) {
                                    out.writeUTF("Registration successful! Please wait until a slot becomes available.");
                                } else {
                                    out.writeUTF("Username already taken. Try again.");
                                }
                                out.flush();
                            } else {
                                out.writeUTF("Invalid command. Use 'register' or 'exit'.");
                                out.flush();
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling waiting client: " + e.getMessage());
            }
        });
        waitingThread.start();
    }

    public static void notifySessionEnd(){
        lock.lock();
        try{
            activeSessions--;
            if(!waitingQueue.isEmpty()){
                Socket nextClient = waitingQueue.poll(); //usamos poll por causa do caso em que a lista é vazia
                activeSessions++;
                startWorker(nextClient);
            } else {
                isAvailable.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}