package client;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.beust.jcommander.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {

    public static void main(String[] args) {
        Args commandLine = new Args();
        JCommander.newBuilder()
                .addObject(commandLine)
                .build()
                .parse(args);
        Client client = new Client("127.0.0.1", 1025);
        if (commandLine.getInFile() != null) {

            try  {

                String jsonFile = Files.readString(Path.of("c:\\Users\\Администратор\\IdeaProjects\\" +
                        "JSON Database\\JSON Database\\task\\src\\client\\data\\" + commandLine.getInFile()));
                client.connect(jsonFile);

            }
            catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            client.connect(commandLine.getType(), commandLine.getIndex(), commandLine.getText());
        }





    }
}
class Client {

    final private String address;
    final private int port;
    public Client (String address, int port) {
        this.address = address;
        this.port = port;
        System.out.println("Client started!");
    }


    public void connect (JsonElement type, JsonElement key, JsonElement text) {

        try (Socket socket = new Socket (InetAddress.getByName(address), port);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ){
            System.out.print("Sent: ");

            Map<String, JsonElement> sendMap = new LinkedHashMap<>();
            Gson gson = new Gson();
            String gsonSend;

            if (text != null) {

                sendMap.put("type", type);
                sendMap.put("key",key);
                sendMap.put("value", text);

            } else if (key != null){

                sendMap.put("type", type);
                sendMap.put("key",key);

            } else {sendMap.put("type", type);
            }
            gsonSend = gson.toJson(sendMap);
            output.writeUTF(gsonSend);
            System.out.println(gsonSend);

            System.out.println("Received: " + input.readUTF());



        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void connect(String jsonFile) {
        try (Socket socket = new Socket (InetAddress.getByName(address), port);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.print("Sent: ");
            JsonObject jsonObject = JsonParser.parseString(jsonFile).getAsJsonObject();
            output.writeUTF(jsonObject.toString());
            System.out.println(jsonObject);
            System.out.println("Received: " + input.readUTF());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Args {


    @Parameter(names = { "-type", "-t" }, converter = JsonElementConverter.class, description = "type of the request")
    JsonElement type;


    @Parameter(names = {"-key", "-k"}, converter = JsonElementConverter.class, description = "key of the cell")
    JsonElement key = null;

    @Parameter(names = {"-value", "-v"}, converter = JsonElementConverter.class, description = "text request")
    JsonElement text;

    @Parameter(names = {"-in"}, description = "input file")
    String inFile;


    public JsonElement getType () {
        return type;
    }

    public JsonElement getIndex () {
        return key;
    }
    public JsonElement getText () {
        return text;
    }
    public String getInFile () {return inFile;}
}

class JsonElementConverter implements IStringConverter <JsonElement>{
    @Override
    public JsonElement convert (String value) {
        Gson gson = new Gson();
        String jsonValue = gson.toJson(value);
        return JsonParser.parseString(jsonValue);
    }
}