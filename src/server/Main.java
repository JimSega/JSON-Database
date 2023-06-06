package server;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.*;
import server.request.Request;
import server.request.RequestGsonDeserializer;


class Input extends RuntimeException {


    public Input (String msg) {
        super(msg);
    }

    static String readInput (String type, JsonElement key, JsonElement text) {
        String result;
        if (type.equalsIgnoreCase("set") || type.equalsIgnoreCase("get")
                || type.equalsIgnoreCase("exit")
                || type.equalsIgnoreCase("delete")) {


            switch (type) {
                case "set":
                    result = Database.set(type, key, text);

                    break;
                case "get" , "delete":
                    result = Database.getDelete(type, key);
                    break;
                case "exit":
                    synchronized (Server.class) {
                        Server.exit = true;
                    }
                    Main.server.stop();
                    result = "{\"response\":\"OK\"}";

                    break;
                default:

                    throw new Input("ERROR");


            }
        }
        else {throw new Input("ERROR");}
        return result;
    }
}

public class Main{
    public static Server server;
    public static void main(String[] args) throws IOException{




        server = new Server("127.0.0.1", 1025);
        server.serverRun();



    }
}

class Database implements Base {
    static volatile Gson gson = new Gson();
    static volatile String databaseGson;
    static volatile JsonObject databaseJsonObject;

    static FileWriter myFileWriter;
    static ReadWriteLock lock = new ReentrantReadWriteLock();
    static final Lock readLock = lock.readLock();
    static final Lock writeLock = lock.writeLock();

    static void readFile () {
        if (myFile.exists()) {
            try {
                String fileJson = Files.readString(myFile.toPath());
                databaseJsonObject = new Gson().fromJson(fileJson, JsonObject.class);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }


        } else {databaseJsonObject = new JsonObject();}

    }

    static String  set (String method, JsonElement key, JsonElement parameter) throws IllegalArgumentException {
        Map <String, String> mapServerResponse = new LinkedHashMap<>();
        String result = "ERROR";
        Gson gsonServerResponse = new Gson();

        if (method.equals("set")) {

            writeLock.lock();

            JsonObject copyDatabase = databaseJsonObject;


            Gson gsonFromClient = new GsonBuilder().
                    registerTypeAdapter(Request.class, new RequestGsonDeserializer()).create();
            if (key.isJsonPrimitive()) {

                String jsonPrimitiveKeys = gsonFromClient.fromJson(key, String.class);
                copyDatabase.add(jsonPrimitiveKeys, parameter);
            } else {

                JsonArray jsonArrayKeys = gsonFromClient.fromJson(key, JsonArray.class);

                for (int i = 0; i < jsonArrayKeys.size() - 1; i++) {

                    String keyString = jsonArrayKeys.get(i).getAsString();

                    if (copyDatabase.has(keyString)) {
                        copyDatabase = copyDatabase.get(keyString).getAsJsonObject();


                    } else {
                        throw new IllegalArgumentException("{\"response\":\"ERROR\",\"reason\":\"No such key\"}");
                    }



                }
                copyDatabase.add(jsonArrayKeys.get(jsonArrayKeys.size() - 1).getAsString(), parameter);

            }
            try {

                myFileWriter = new FileWriter(myFile, false);

                myFileWriter.write(gson.toJson(databaseJsonObject));

                myFileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            result = "OK";
        }
        mapServerResponse.put("response", result);
        writeLock.unlock();

        return gsonServerResponse.toJson(mapServerResponse);
    }
    static String  getDelete (String method, JsonElement key) throws IllegalArgumentException{

        Map <String, String> mapServerResponse = new LinkedHashMap<>();
        Gson gsonServerResponse = new Gson();
        JsonObject copyDatabase = databaseJsonObject;
        Gson gsonFromClient = new GsonBuilder().
                registerTypeAdapter(Request.class, new RequestGsonDeserializer()).create();
        JsonArray jsonArrayKeys = new JsonArray();



        if (method.equals("get")) {
            readLock.lock();

            if (key.isJsonPrimitive() && copyDatabase.has(key.getAsString())
                    && copyDatabase.get(gsonFromClient.fromJson(key, String.class)).isJsonObject()) {
                String jsonPrimitiveKeys = gsonFromClient.fromJson(key, String.class);
                synchronized (readLock) {
                    readLock.unlock();
                    return "{\"response\":\"OK\"," + "\"value\":" +
                            new Gson().toJson(copyDatabase.get(jsonPrimitiveKeys).getAsJsonObject()) + "}";
                }

            } else if (key.isJsonPrimitive() && copyDatabase.has(key.getAsString()))  {
                mapServerResponse.put("response", "OK");
                mapServerResponse.put("value",
                        copyDatabase.get(gsonFromClient.fromJson(key, String.class)).getAsString());
                readLock.unlock();
            }
            else if (key.isJsonPrimitive() && !copyDatabase.has(key.getAsString())) {
                mapServerResponse.put("response", "ERROR");
                mapServerResponse.put("reason", "No such key");
                readLock.unlock();
            }


            else {
                jsonArrayKeys = gsonFromClient.fromJson(key, JsonArray.class);

                for (JsonElement jsonElementKey : jsonArrayKeys) {
                    String keyString = jsonElementKey.getAsString();
                    if (copyDatabase.has(keyString)) {

                        if(copyDatabase.get(keyString).isJsonPrimitive()) {
                            mapServerResponse.put("response", "OK");
                            mapServerResponse.put("value", copyDatabase.get(keyString).getAsString());
                            readLock.unlock();
                            return gsonServerResponse.toJson(mapServerResponse);
                        }
                        copyDatabase = copyDatabase.get(keyString).getAsJsonObject();
                    } else {
                        mapServerResponse.put("response", "ERROR");
                        mapServerResponse.put("reason", "No such key");
                        readLock.unlock();
                        return gsonServerResponse.toJson(mapServerResponse);
                    }
                }
                synchronized (readLock) {
                    readLock.unlock();
                    return "{\"response\":\"OK\"," + "\"value\":" + new Gson().toJson(copyDatabase) + "}";
                }
            }


        }
        if (method.equals("delete")) {
            writeLock.lock();
            if (key.isJsonPrimitive() && copyDatabase.has(key.getAsString())) {
                copyDatabase.remove(key.getAsString()).getAsJsonObject();
                mapServerResponse.put("response", "OK");
            } else if (key.isJsonPrimitive() && !copyDatabase.has(key.getAsString())) {
                mapServerResponse.put("response", "ERROR");
                mapServerResponse.put("reason", "No such key");
            } else {
                jsonArrayKeys = gsonFromClient.fromJson(key, JsonArray.class);
                for (int i = 0; i < jsonArrayKeys.size(); i++) {

                    String keyString =jsonArrayKeys.get(i).getAsString();

                    if (copyDatabase.has(keyString) && copyDatabase.get(keyString).isJsonObject()) {
                        copyDatabase = copyDatabase.get(keyString).getAsJsonObject();

                    } else if (!copyDatabase.has(keyString)) {
                        mapServerResponse.put("response", "ERROR");
                        mapServerResponse.put("reason", "No such key");
                        writeLock.unlock();
                        throw new IllegalArgumentException ("{\"response\":\"ERROR\",\"reason\":\"No such key\"}");
                    }

                }
                mapServerResponse.put("response", "OK");
                copyDatabase.remove(jsonArrayKeys.get(jsonArrayKeys.size() - 1).getAsString());


            }
            databaseGson = gson.toJson(databaseJsonObject);
            try {
                myFileWriter = new FileWriter(myFile, false);
                myFileWriter.write(databaseGson);
                myFileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            writeLock.unlock();
        }

        return gsonServerResponse.toJson(mapServerResponse);
    }

}

interface Base {

    File myFile = new File
            ("c:\\Users\\Администратор\\IdeaProjects\\JSON Database\\JSON Database\\task\\src\\server\\data\\db.json");


}

class Server{

    private final ServerSocket serverSocket;

    static volatile boolean exit = false;
    public Server (String address, int port) throws IOException {

        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(address));
        System.out.println("Server started!");
        Database.readFile();
    }
    ExecutorService executor = Executors.newFixedThreadPool(4);




    public void serverRun () {



        while (!exit) {

            try {

                executor.submit(new ServerThread(serverSocket.accept()));

            }
            catch (Exception e) {
                System.out.println(e.getMessage()); //Socked closed
            }

        }
        executor.shutdown();





    }
    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error Server");
        }
    }

}



class ServerThread implements Runnable{
    private final Socket socket;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run () {
        try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())
        ) {
            String jsonClient = inputStream.readUTF();
            Gson gsonClient = new Gson();

            Request mapClient = gsonClient.fromJson(jsonClient, Request.class);

            String type = mapClient.getType();

            JsonElement key = type.equalsIgnoreCase("exit") ? null : mapClient.getKey();
            JsonElement text = type.equalsIgnoreCase("set") ? mapClient.getValue() : null;

            try {
                outputStream.writeUTF(Input.readInput(type, key, text));



            } catch (Exception e) {
                outputStream.writeUTF(e.getMessage());
                System.out.println(e.getMessage());

            }
            finally {
                socket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error ServerThread");
        }

    }
}