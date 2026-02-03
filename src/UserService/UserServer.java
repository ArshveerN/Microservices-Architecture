package UserService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
/**
 * UserServer is an HTTP-based microservice responsible for user management.
 * <p>Users are stored with the following attributes:</p>
 * <ul>
 *   <li>id - unique integer identifier</li>
 *   <li>username - string username</li>
 *   <li>email - string email address</li>
 *   <li>password - string password</li>
 * </ul>
 *
 * <p><b>API Endpoint:</b> /user</p>
 *
 * <p><b>Supported Methods:</b></p>
 * <ul>
 *   <li>GET /user/{id} - Retrieve user by ID</li>
 *   <li>POST /user - Create, update, or delete user based on command field</li>
 * </ul>
 *
 *
 * @author Arshveer
 * @author Eshaan
 */
public class UserServer {
    /** The port number this server listens on */
    static Integer PORT;

    /** The IP address this server binds to */
    static String IP;

    /** Path to the config file */
    static String PATH;

    /**
     * In-memory storage for users.
     * Key: user ID
     * Value: username, email, password
     */
    static HashMap<Integer, ArrayList<String>> users = new HashMap<>();

    /**
     * Parses a JSON string into a HashMap of key-value pairs.
     * This is a simple JSON parser that handles flat JSON objects.
     *
     * @param json The JSON string to parse
     * @return HashMap containing the parsed key-value pairs, or null if invalid JSON
     */
    static HashMap<String, String> stringToMap(String json) {

        HashMap<String, String> mapOutput = new HashMap<>();

        json = json.trim();
        if (json.length() < 2 || json.charAt(0) != '{' || json.charAt(json.length() - 1) != '}') {
            return null;
        }

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) {
            return null;
        }

        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) continue; // skip malformed
            String key = kv[0].trim();
            String value = kv[1].trim();

            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            mapOutput.put(key, value);
        }

        return mapOutput;
    }

    /**
     * Parse a flat JSON object string into a map of keys to values.
     * This simple parser is intended for the assignment's constrained input format.
     *
     * @param json JSON object string
     * @return map of parsed string keys to string values, or null on invalid input
     */

    /**
     * Parses a nested configuration JSON string into a map of service configurations.
     * Each service has its own map of configuration values (ip, port, etc.).
     *
     * @param configJson The configuration JSON string containing service configs
     * @return HashMap where keys are service names and values are config maps
     */
    static HashMap<String, HashMap<String, String>> parseConfig(String configJson) {
        HashMap<String, HashMap<String, String>> result = new HashMap<>();
        configJson = configJson.trim();
        if (configJson.startsWith("{") && configJson.endsWith("}")) {
            configJson = configJson.substring(1, configJson.length() - 1).trim();
        }
        int idx = 0;
        while (idx < configJson.length()) {
            // Find key
            while (idx < configJson.length() && Character.isWhitespace(configJson.charAt(idx))) idx++;
            if (idx >= configJson.length()) break;
            if (configJson.charAt(idx) != '"') break;
            int keyStart = ++idx;
            while (idx < configJson.length() && configJson.charAt(idx) != '"') idx++;
            String key = configJson.substring(keyStart, idx);
            idx++; // skip closing quote
            while (idx < configJson.length() && (configJson.charAt(idx) == ' ' || configJson.charAt(idx) == ':')) idx++;
            // Now at value (should be '{')
            if (configJson.charAt(idx) != '{') break;
            int braceCount = 1;
            int valueStart = idx;
            idx++;
            while (idx < configJson.length() && braceCount > 0) {
                if (configJson.charAt(idx) == '{') braceCount++;
                else if (configJson.charAt(idx) == '}') braceCount--;
                idx++;
            }
            String valueBlock = configJson.substring(valueStart, idx);
            result.put(key, stringToMap(valueBlock));
            // Skip any trailing commas or whitespace
            while (idx < configJson.length() && (configJson.charAt(idx) == ',' || Character.isWhitespace(configJson.charAt(idx))))
                idx++;
        }
        return result;
    }

    /**
     * Main entry point for the UserServer microservice.
     * Reads configuration from the provided config file and starts the HTTP server.
     *
     * @param args Command line arguments. args[0] should be the path to config.json
     * @throws IOException If the config file cannot be read or server fails to start
     */
    public static void main(String[] args) throws IOException {
        PATH = args[0];
        // Get port of other servers
        String jsonConfig = Files.readString(Path.of(PATH));
        HashMap<String, HashMap<String, String>> configMap = parseConfig(jsonConfig);
        PORT = Integer.parseInt(configMap.get("UserService").get("port"));
        IP = configMap.get("UserService").get("ip");

        HttpServer server = HttpServer.create(new InetSocketAddress(IP, PORT), 0);
        server.createContext("/user", new UserServer.UserHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    /**
     * HTTP Handler for the /user endpoint.
     * Handles GET requests for user retrieval and POST requests for user management.
     * <p><b>Status Codes:</b></p>
     * <ul>
     *   <li>200 - Success</li>
     *   <li>400 - Bad request (missing/invalid fields)</li>
     *   <li>404 - User not found</li>
     *   <li>405 - Method not allowed</li>
     *   <li>409 - Conflict (user already exists)</li>
     * </ul>
     */
    static class UserHandler implements HttpHandler {

        /**
         * Handles incoming HTTP requests to the /user endpoint.
         *
         * @param exchange The HTTP exchange containing request and response
         * @throws IOException If an I/O error occurs
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                String[] tokenized_path = path.split("/");

                if (tokenized_path.length != 3) {
                    sendJsonwithCode(exchange, path, 400);
                    return;
                }

                String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                if (!body.trim().isEmpty()) {
                    String trimmed = body.trim();
                    HashMap<String, String> bodyMap = stringToMap(trimmed);
                    if (bodyMap != null) {
                        
                        String idInBody = bodyMap.get("id");
                        if (idInBody == null) {
                            sendJsonwithCode(exchange, "{}", 400);
                            return;
                        }
                        try {
                            int idBody = Integer.parseInt(idInBody);
                            int idPath = Integer.parseInt(tokenized_path[2]);
                            if (idBody != idPath) {
                                sendJsonwithCode(exchange, "{}", 400);
                                return;
                            }
                        } catch (NumberFormatException e) {
                            sendJsonwithCode(exchange, "{}", 400);
                            return;
                        }

                    }
                }

                int userID;
                try {
                    userID = Integer.parseInt(tokenized_path[2]);
                } catch (NumberFormatException e) {
                    sendJsonwithCode(exchange, "{}", 400);
                    return;
                }

                ArrayList<String> user = users.get(userID);
                if (user == null) {
                    sendJsonwithCode(exchange, "{}", 404);
                    return;
                }

                String json = "{"
                        + "\"id\": " + userID + ","
                        + "\"username\": \"" + user.get(0) + "\","
                        + "\"email\": \"" + user.get(1) + "\","
                        + "\"password\": \"" + user.get(2) + "\""
                        + "}";

                sendJson(exchange, json);
            }

            else if ("POST".equals(exchange.getRequestMethod())) {

                String path = exchange.getRequestURI().getPath();
                String[] tokenized_path = path.split("/");
                // POST must target the collection root: /product

                if (tokenized_path.length != 2) {
                    sendJsonwithCode(exchange, "{}", 400);
                    return;
                }

                // Parse the input string
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                HashMap<String, String> bodyMap = stringToMap(body);
                if (bodyMap == null) {
                    sendJsonwithCode(exchange, "{}", 400);
                    return;
                }

                int code = UserValidation(bodyMap, exchange);
                if (code != 200) {
                    sendJsonwithCode(exchange, "{}", code);
                    return;
                }
            }
            else {
                sendJsonwithCode(exchange, "{}", 400);
            }
        }

        /**
         * Validates user request and routes to appropriate handler.
         * Checks for required fields and valid command type.
         *
         * @param bodyMap The parsed request body as a HashMap
         * @return HTTP status code (200 for success, 400/404/409 for errors)
         */
        static int UserValidation(HashMap<String, String> bodyMap, HttpExchange exchange) throws IOException {

            String command = bodyMap.get("command");
            String idStr = bodyMap.get("id");
            String username = bodyMap.get("username");
            String email = bodyMap.get("email");
            String password = bodyMap.get("password");

            if (idStr == null) {
                return 400;
            }
            int id;
            try {
                id = Integer.parseInt(idStr);
                if (username != null) {
                    username = username.trim();
                }
                if (email != null) {
                    email = email.trim();
                }
                if (password != null) {
                    password = password.trim();
                }
            } catch (NumberFormatException e) {
                return 400;
            }

            if (command == null) {
                return 400;
            }

            switch (command) {
                case "create":
                    if (users.get(id) != null) {
                        return 409;
                    }
                    if (username == null || email == null || password == null) {
                        return 400;
                    }
                    if (email.indexOf('@') < 0) {
                        return 400;
                    }
                    createHandler(bodyMap, id, exchange);
                    break;

                case "update":
                    // checks if the user exists
                    if (users.get(id) == null) {
                        return 404;
                    }
                    updateHandler(bodyMap, id, exchange);
                    break;

                case "delete":
                    ArrayList<String> verifyInt = users.get(id);
                    if (verifyInt == null) {
                        return 404;
                    }

                    if (username == null || email == null || password == null) {
                        return 400;
                    }

                    String storedUsername = verifyInt.get(0);
                    String storedEmail = verifyInt.get(1);
                    String storedPassword = verifyInt.get(2);

                    if (!(username.equals(storedUsername) && email.equals(storedEmail) && hashSHA256(password).equals(storedPassword)))
                        return 404;

                    deleteHandler(exchange, bodyMap, id);
                    break;

                default:
                    return 400;
            }
            return 200;
        }

        /**
         * Creates a new user in the in-memory storage.
         *
         * @param bodyMap The request body containing user data
         * @param id The unique user ID
         */
        static void createHandler(HashMap<String, String> bodyMap, int id, HttpExchange exchange) throws IOException {
            ArrayList<String> values = new ArrayList<>();

            values.add(bodyMap.get("username"));
            values.add(bodyMap.get("email"));
            values.add(hashSHA256(bodyMap.get("password")));
            users.put(id, values);
            String payload = "{"
                    + "\"id\": " + id + ","
                    + "\"username\": \"" + bodyMap.get("username") + "\","
                    + "\"email\": \"" + bodyMap.get("email") + "\","
                    + "\"password\": \"" + users.get(id).get(2) + "\""
                    + "}";
            sendJsonwithCode(exchange, payload, 200);
        }

        /**
         * Updates an existing user's fields.
         * Only updates fields that are present in the request.
         *
         * @param bodyMap The request body containing fields to update
         * @param id The user ID to update
         */
        static void updateHandler(HashMap<String, String> bodyMap, int id, HttpExchange exchange) throws IOException {

            String username = bodyMap.get("username");
            if (username != null) {
                users.get(id).set(0, username);
            }

            String email = bodyMap.get("email");
            if (email != null) {
                if (email.indexOf('@') < 0) {
                    sendJsonwithCode(exchange, "{}", 400);
                    return;
                }
                users.get(id).set(1, email);
            }

            String rawPassword = bodyMap.get("password");
            if (rawPassword != null) {
                String hashed = hashSHA256(rawPassword);
                users.get(id).set(2, hashed);
            }
            String payload = "{"
                    + "\"id\": " + id + ","
                    + "\"username\": \"" + users.get(id).get(0) + "\","
                    + "\"email\": \"" + users.get(id).get(1) + "\","
                    + "\"password\": \"" + users.get(id).get(2) + "\""
                    + "}";
            sendJsonwithCode(exchange, payload, 200);
        }

        /**
         * Deletes a user from the in-memory storage.
         *
         * @param bodyMap The request body (used for validation before this call)
         * @param id The user ID to delete
         */
        static void deleteHandler(HttpExchange exchange, HashMap<String, String> bodyMap, int id) throws IOException {
            users.remove(id);
            sendJsonwithCode(exchange, "{}", 200);
        }
    }

    /**
     * Sends a JSON response to the client with HTTP 200 status.
     *
     * @param exchange The HTTP exchange
     * @param json The JSON string to send
     * @throws IOException If an I/O error occurs
     */
    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private static void sendJsonwithCode(HttpExchange exchange, String json, int code) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    public static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            // Should never happen since SHA-256 is guaranteed
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

}