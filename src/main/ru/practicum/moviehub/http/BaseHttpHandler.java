package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    protected static final String JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = GSON.toJson(data);

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendEmpty(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(204, -1);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(message);
        sendJson(exchange, statusCode, error);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message, List<String> details) throws IOException {
        ErrorResponse error = new ErrorResponse(message, details);
        sendJson(exchange, statusCode, error);
    }

    protected <T> T parseJson(String body, Class<T> clazz) throws JsonSyntaxException {
        return GSON.fromJson(body, clazz);
    }

    protected String readBody(HttpExchange exchange) throws IOException {
        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    protected int extractId(String path) {
        try {
            String[] parts = path.split("/");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected boolean isJsonContentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.startsWith("application/json");
    }
}