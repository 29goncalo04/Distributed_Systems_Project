import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345);
        
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        String serverMessage;
        String userInput;

        while (true) {
            try {
                serverMessage = in.readUTF();
                if (serverMessage == null || serverMessage.equals("Goodbye!")) {
                    break; // Sai do loop quando o servidor envia "Goodbye!" ou quando o servidor fecha
                }

                // Substitui os \n por quebra de linha e exibe
                //String formattedMessage = serverMessage.replace("\n", System.lineSeparator());
                //System.out.println(formattedMessage);
                System.out.println(serverMessage);
                userInput = console.readLine();
                out.writeUTF(userInput);
                out.flush();
            } catch (IOException e) {
                System.out.println("Error reading from server or sending data: " + e.getMessage());
                break;
            }
        }
        
        socket.shutdownOutput();
        socket.shutdownInput();
        socket.close();
    }
}