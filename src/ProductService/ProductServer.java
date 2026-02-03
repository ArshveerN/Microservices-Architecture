package ProductService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * ProductServer is an HTTP microservice that manages products in-memory.
 *
 * <p>Exposed endpoints:</p>
 * <ul>
 *   <li>GET /product/{id} - retrieve product by id</li>
 *   <li>POST /product     - create, update, or delete products using a JSON command payload</li>
 * </ul>
 *
 * <p>Products are stored in a {@code HashMap<Integer, ArrayList<String>>} where each list
 * holds {@code [name, price, quantity, description]} as strings.</p>
 */
public class ProductServer {
    static Integer PORT;
    static String IP;
    static String PATH;

    static HashMap<Integer, ArrayList<String> > products = new HashMap<>();

    /**
     * Parse a flat JSON object string into a map of key->value strings.
     * This parser is intentionally minimal and only supports simple, flat
     * JSON objects used by the assignment (no nested objects or arrays).
     *
     * @param json JSON object text
     * @return map of string keys to string values, or null if the input is invalid
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
     * Parse a nested configuration JSON string into a map of service names to
     * configuration maps. Uses the simple {@link #stringToMap} to parse inner objects.
     *
     * @param configJson configuration JSON text
     * @return map where keys are service names and values are config maps
     */
    // Parses a config JSON string into a map of service names to config maps, using only stringToMap
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
     * Main entrypoint for the ProductServer. Reads configuration and starts the HTTP server.
     *
     * @param args command line arguments; args[0] must be the path to the config JSON
     * @throws IOException when configuration file cannot be read or server fails to start
     */
    public static void main(String[] args) throws IOException {
        PATH = args[0];
        String jsonConfig = Files.readString(Path.of(PATH));
        HashMap<String, HashMap<String, String>> configMap = parseConfig(jsonConfig);
        PORT = Integer.parseInt(configMap.get("ProductService").get("port"));
        IP = configMap.get("ProductService").get("ip");

        HttpServer server = HttpServer.create(new InetSocketAddress(IP, PORT), 0);
        server.createContext("/product", new ProductServer.ProductHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);

    }

    /**
     * HTTP handler for the /product endpoint. Supports GET and POST operations.
     */
    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                String[] tokenized_path = path.split("/");

                // require exactly one numeric segment after /product
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

                int prodID;
                try {
                    prodID = Integer.parseInt(tokenized_path[2]);
                } catch (NumberFormatException e) {
                    sendJsonwithCode(exchange, "{}", 400);
                    return;
                }

                ArrayList<String> product = products.get(prodID);
                if (product == null) {
                    sendJsonwithCode(exchange, "{}", 404);
                    return;
                }

                String json = "{"
                        + "\"id\": " + prodID + ","
                        + "\"name\": \"" + product.get(0) + "\","
                        + "\"description\": \"" + product.get(3) + "\","
                        + "\"price\": " + product.get(1) + ","
                        + "\"quantity\": " + product.get(2)
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

                int code = ProdValidation(bodyMap, exchange);
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
     * Validate a product JSON command payload.
     *
     * @param bodyMap parsed flat JSON body
     * @param exchange HttpExchange (used for handlers to send responses)
     * @return HTTP status code indicating validation result (200 on success)
     * @throws IOException on I/O errors
     */
    static int ProdValidation(HashMap<String, String> bodyMap, HttpExchange exchange) throws IOException {

        String command =  bodyMap.get("command");
        String idString = bodyMap.get("id");
        String descriptionString = bodyMap.get("description");
        String priceStr = bodyMap.get("price");
        String quantityStr = bodyMap.get("quantity");
        String productNameStr = bodyMap.get("name");

        if (idString == null) {
            return 400;
        }
        int id;
        try {
            id = Integer.parseInt(idString);
            if (priceStr != null) {
                double v = Double.parseDouble(priceStr);
                if (v < 0) {
                    return 400;
                }
            }
            if (quantityStr != null) {
                int v = Integer.parseInt(quantityStr);
                if (v < 1) {
                    return 400;
                }
            }
            if (productNameStr != null) {
                productNameStr = productNameStr.trim();
                if  (productNameStr.isEmpty()) {
                    return 400;
                }
            }
            if  (descriptionString != null) {
                descriptionString = descriptionString.trim();
                if (descriptionString.isEmpty()) {
                    return 400;
                }
            }
        } catch (NumberFormatException e) {
            return 400;
        }

        if (command == null) {
            return 400;
        }
        switch (command) {
            case "create":
                if (products.get(id) != null) {
                    return 409;
                }
                if (priceStr == null || quantityStr == null || productNameStr == null || descriptionString == null) {
                    return 400;
                }
                createHandler(bodyMap, id, exchange);
                break;

            case "update":
                if (products.get(id) == null) {
                    return 404;
                }
                updateHandler(bodyMap, id, exchange);
                break;

            case "delete":
                ArrayList<String> verifyInt = products.get(id);
                if (verifyInt == null) {
                    return 404;
                }

                String name = bodyMap.get("name");
                String price = bodyMap.get("price");
                String quantity = bodyMap.get("quantity");

                if (name == null || price == null || quantity == null) {
                    return 404;
                }

                String storedName = verifyInt.get(0);
                String storedPrice = verifyInt.get(1);
                String storedQuantity = verifyInt.get(2);

                if (!(name.equals(storedName)
                        && price.equals(storedPrice)
                        && quantity.equals(storedQuantity)))
                    return 401;

                deleteHandler(bodyMap, id, exchange);
                break;

            default:
                return 400;
        }
        return 200;
    }
    /**
     * Create a new product and send a JSON response with the created product.
     *
     * @param bodyMap parsed request body
     * @param id product id
     * @param exchange HttpExchange used to send the response
     * @throws IOException on write errors
     */
    static void createHandler(HashMap<String, String> bodyMap, int id, HttpExchange exchange) throws IOException {
        ArrayList<String> values = new ArrayList<>();

        values.add(bodyMap.get("name"));
        double value = Double.parseDouble(bodyMap.get("price"));
        String formatted = String.format("%.2f", value);
        values.add(formatted);
        values.add(bodyMap.get("quantity"));
        values.add(bodyMap.get("description"));

        products.put(id,  values);
        String payload = "{"
                + "\"id\": " + id + ","
                + "\"name\": \"" + bodyMap.get("name") + "\","
                + "\"description\": \"" + bodyMap.get("description") + "\","
                + "\"price\": " + formatted + ","
                + "\"quantity\": " + bodyMap.get("quantity")
                + "}";
        sendJsonwithCode(exchange, payload, 200);
    }
    /**
     * Update an existing product's fields and return the updated product JSON.
     *
     * @param bodyMap parsed request body
     * @param id product id
     * @param exchange HttpExchange used to send the response
     * @throws IOException on write errors
     */
    static void updateHandler(HashMap<String, String> bodyMap, int id, HttpExchange exchange) throws IOException {

        String name = bodyMap.get("name");
        if (name != null) {
           products.get(id).set(0, name);
        }

        String price = bodyMap.get("price");
        if (price != null) {
            products.get(id).set(1, price);
        }

        String quantity = bodyMap.get("quantity");
        if (quantity != null) {
            products.get(id).set(2, quantity);
        }
        String description = bodyMap.get("description");
        if (description != null) {
            products.get(id).set(3, description);
        }
        ArrayList<String> product = products.get(id);
        String payload = "{"
                    + "\"id\": " + id + ","
                    + "\"name\": \"" + product.get(0) + "\","
                    + "\"description\": \"" + product.get(3) + "\","
                    + "\"price\": " + product.get(1) + ","
                    + "\"quantity\": " + product.get(2)
                    + "}";
        sendJsonwithCode(exchange, payload, 200);

    }
        /**
         * Delete a product by id and send an empty JSON response with status 200.
         *
         * @param bodyMap parsed request body
         * @param id product id
         * @param exchange HttpExchange used to send the response
         * @throws IOException on write errors
         */
        static void deleteHandler(HashMap<String, String> bodyMap, int id, HttpExchange exchange) throws IOException {
            products.remove(id);
            sendJsonwithCode(exchange, "{}", 200);
        }
    }
    /**
     * Send JSON with HTTP 200 and Content-Type application/json.
     *
     * @param exchange http exchange
     * @param json JSON payload
     * @throws IOException on write errors
     */
    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
    /**
     * Send JSON with a specific HTTP status code and Content-Type application/json.
     *
     * @param exchange http exchange
     * @param json JSON payload
     * @param code HTTP status code to send
     * @throws IOException on write errors
     */
    private static void sendJsonwithCode(HttpExchange exchange, String json, int code) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
