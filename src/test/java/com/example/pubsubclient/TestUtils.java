package com.example.pubsubclient;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

public class TestUtils {

    public static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        // fully consume the request body to avoid connection reset issues
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().set("Connection", "close");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
        // exchange.close();
    }
    
}
