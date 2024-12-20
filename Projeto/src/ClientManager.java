import java.util.HashMap;
import java.util.Map;

public class ClientManager{
    private Map<String, String> registeredUsers = new HashMap<>();

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
}