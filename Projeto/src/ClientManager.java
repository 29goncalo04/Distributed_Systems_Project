import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class ClientManager{
    private static Map<String, String> registeredUsers = new HashMap<String,String>(); // username e password
    private static Map<String, byte[]> dataStorage = new HashMap<String,byte[]>(); // dados enviados
    private static final ReentrantLock lock = new ReentrantLock();
    
    public boolean registerUser(String username, String password){
        boolean res = registeredUsers.containsKey(username);
        if(!res){
            registeredUsers.put(username, password);
            return true;
        }
        return false;
    }

    public boolean authenticateUser(String username, String password){
        boolean res = registeredUsers.containsKey(username);
        if(res){
            String pass = registeredUsers.get(username);
            if(pass.equals(password)) return true;
            return false;
        }
        return false;
    }


    // Operação de escrita simples
    public void put(String key, byte[] value) {
        lock.lock();
        try {
            dataStorage.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    // Operação de leitura simples
    public byte[] get(String key) {
        lock.lock();
        try {
            return dataStorage.get(key);  // Retorna null se a chave não for encontrada
        } finally {
            lock.unlock();
        }
    }

    // Operação de escrita composta (multiPut)
    public void multiPut(Map<String, byte[]> pairs) {
        lock.lock();
        try {
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                dataStorage.put(entry.getKey(), entry.getValue());
            }
        } finally {
            lock.unlock();
        }
    }

    // Operação de leitura composta (multiGet)
    public Map<String, byte[]> multiGet(Set<String> keys) {
        Map<String, byte[]> result = new HashMap<>();
        lock.lock();
        try {
            for (String key : keys) {
                if (dataStorage.containsKey(key)) {
                    result.put(key, dataStorage.get(key));
                }
            }
        } finally {
            lock.unlock();
        }
        return result;
    }


   public void dataHandle(Socket clientSocket, DataInputStream in, DataOutputStream out) throws IOException {
        while (true) {
            String option = in.readUTF();

            if (option.equals("put")) {
                // Receber a chave
                out.writeUTF("Write the key:");
                out.flush();
                String key = in.readUTF();

                // Receber os dados
                out.writeUTF("Write the data and finish with [END]");
                out.flush();

                StringBuilder currentData = new StringBuilder();

                while (true) {
                    try {
                        String line = in.readUTF();  // Lê toda a linha enviada
                        currentData.append(line);    // Adiciona a linha ao buffer
                        if (currentData.toString().contains("END")) {
                            break;  // Se encontrar "END", termina a leitura
                        }
                    } catch (IOException e) {
                        System.out.println("Error reading data: " + e.getMessage());
                        break;
                    }
                }

                // Pega a parte da string antes de "END"
                String finalData = currentData.toString().split("END")[0];

                // Converte o conteúdo final para byte[] e armazena
                byte[] data = finalData.getBytes("UTF-8");

                put(key,data);

                out.writeUTF("Data stored successfully with key: " + key + "\nChoose an option: [put], [get], [multiPut], [multiGet] or [exit]");
                out.flush();
            } 

            else if (option.equals("get")) {
                // Receber a chave
                out.writeUTF("Write the key to retrieve:");
                out.flush();
                String key = in.readUTF();

                if(!dataStorage.containsKey(key)){
                    out.writeUTF("Key not found\nChoose an option: [put], [get], [multiPut], [multiGet] or [exit]");
                    out.flush();
                } else {
                    byte[] value;

                    // Usar lock para proteger a leitura
                    value = get(key);

                    if (value != null) {
                        // Converte o valor recuperado para uma string
                        String result = new String(value, "UTF-8");
                        result = result.replace("\\n", "\n");

                        // Enviar a resposta formatada ao cliente
                        out.writeUTF("Value for key " + key + ":\n" + result + "\nChoose an option: [put], [get], [multiPut], [multiGet] or [exit]");
                        out.flush();
                    } else {
                        out.writeUTF("Key not found: " + key + ".\nChoose an option: [put], [get], [multiPut], [multiGet] or [exit]");
                        out.flush();
                    }
                }
            }
            // Opção "multiPut" - insere múltiplos pares chave-valor
            else if (option.equals("multiput")) {
                out.writeUTF("How many key-value pairs would you like to store?");
                out.flush();
                int numPairs = Integer.parseInt(in.readUTF());  // Lê o número de pares

                Map<String, byte[]> pairs = new HashMap<>();

                for (int i = 0; i < numPairs; i++) {
                    out.writeUTF("Write the key for pair " + (i + 1) + ":");
                    out.flush();
                    String key = in.readUTF();
    
                    out.writeUTF("Write the data for key " + key + " and finish with [END]:");
                    out.flush();
    
                    StringBuilder dataBuffer = new StringBuilder();

                     // Lê os dados até encontrar "END"
                    while (true) {
                        try {
                            String line = in.readUTF();
                            dataBuffer.append(line);
                            if (dataBuffer.toString().contains("END")) {
                                break;
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading data: " + e.getMessage());
                            break;
                        }
                    }
                    // Processa os dados e os armazena
                    String finalData = dataBuffer.toString().split("END")[0];
                    byte[] data = finalData.getBytes("UTF-8");
                    pairs.put(key, data);
                }

                // Armazena múltiplos pares chave-valor
                multiPut(pairs);

                out.writeUTF("Multiple data stored successfully.\nChoose an option: [put], [get], [multiPut], [multiGet], or [exit]");
                out.flush();
            } 
            // Opção "multiGet" - recupera múltiplos pares chave-valor
            else if (option.equals("multiget")) {
                out.writeUTF("How many keys would you like to retrieve?");
                out.flush();
                int numKeys = Integer.parseInt(in.readUTF());  // Lê o número de chaves

                Set<String> keys = new HashSet<>();

                for (int i = 0; i < numKeys; i++) {
                    out.writeUTF("Write key " + (i + 1) + ":");
                    out.flush();
                    String key = in.readUTF();
                    keys.add(key);
                }

                // Recupera múltiplos pares chave-valor
                Map<String, byte[]> result = multiGet(keys);

                // Envia os resultados
                if (!result.isEmpty()) {
                    // Construir a resposta como uma única mensagem
                    StringBuilder response = new StringBuilder("Retrieved values:\n");
                    for (Map.Entry<String, byte[]> entry : result.entrySet()) {
                        String key = entry.getKey();
                        String value = new String(entry.getValue(), "UTF-8").replace("\\n", "\n");
                        response.append("Value for key ").append(key).append(":\n").append(value).append("\n");
                    }
                    response.append("Choose an option: [put], [get], [multiPut], [multiGet], or [exit]");
                    out.writeUTF(response.toString());
                    out.flush();
                } else {
                    out.writeUTF("No keys found.\nChoose an option: [put], [get], [multiPut], [multiGet], or [exit]");
                    out.flush();
                }
            }
            else if (option.equals("exit")) {
                out.writeUTF("Goodbye!");
                out.flush();
                break;
            } else {
                out.writeUTF("Invalid option.\nChoose an option: [put], [get], [multiPut], [multiGet], or [exit]");
                out.flush();
            }
        }

        clientSocket.close();
    }
}