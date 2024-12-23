// import java.io.File;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.Map;
// import javax.json.Json;
// import javax.json.JsonArray;
// import javax.json.JsonArrayBuilder;
// import javax.json.JsonObject;
// import javax.json.JsonReader;
// import javax.json.JsonValue;


// public class Ficheiro {
//     public static void carregaDadosJSON(Map<String, String> registeredUsers) {
//         File file = new File("./data/users.json");

//         // Se o arquivo não existe, cria o arquivo vazio
//         if (!file.exists()) {
//             try {
//                 file.getParentFile().mkdirs(); // Cria os diretórios, se necessário
//                 try (FileWriter writer = new FileWriter(file)) {
//                     writer.write("[]"); // Inicializa o arquivo com um array JSON vazio
//                 }
//             } catch (IOException e) {
//                 e.printStackTrace();
//                 return;
//             }
//         }

//         // Verifica se o arquivo está vazio
//         if (file.length() == 0) {
//             try (FileWriter writer = new FileWriter(file)) {
//                 writer.write("[]"); // Inicializa com um array JSON vazio se estiver vazio
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
//         }

//         // Lê o arquivo JSON e carrega os dados no Map
//         try (FileReader reader = new FileReader(file);
//              JsonReader jsonReader = Json.createReader(reader)) {

//             JsonArray jsonArray = jsonReader.readArray();
//             // Itera sobre os objetos no array e adiciona os dados ao Map
//             for (JsonValue value : jsonArray) {
//                 JsonObject jsonObject = value.asJsonObject();
//                 String username = jsonObject.getString("username");
//                 String password = jsonObject.getString("password");
//                 registeredUsers.put(username, password);
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }



//     public static void adicionaDadosJSON(String username, String password) {
//         File file = new File("./data/users.json");

//         // Lê o arquivo JSON existente
//         JsonArray jsonArray = Json.createArrayBuilder().build();
//         // Verifica se o ficheiro existe e tem conteúdo
//         if (file.exists() && file.length() > 0) {
//             try (FileReader reader = new FileReader(file);
//                  JsonReader jsonReader = Json.createReader(reader)) {
//                 jsonArray = jsonReader.readArray(); // Lê o array JSON
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
//         } else {
//             System.out.println("O ficheiro JSON não existe ou está vazio. Criando novo ficheiro.");
//         }

//         // Adiciona novos usuários ao Map e ao array JSON
//         JsonArrayBuilder arrayBuilder = Json.createArrayBuilder(jsonArray);
//         JsonObject userObject = Json.createObjectBuilder()
//                 .add("username", username)
//                 .add("password", password)
//                 .build();
//         arrayBuilder.add(userObject);

//         jsonArray = arrayBuilder.build();

//         // Grava os dados atualizados no arquivo JSON
//         try (FileWriter writer = new FileWriter(file)) {
//             writer.write(jsonArray.toString());
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }