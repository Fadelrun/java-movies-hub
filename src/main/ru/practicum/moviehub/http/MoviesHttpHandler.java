package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHttpHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHttpHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        try {
            if (method.equalsIgnoreCase("GET")) {
                processGet(exchange);
            } else if (method.equalsIgnoreCase("POST")) {
                processPost(exchange);
            } else if (method.equalsIgnoreCase("DELETE")) {
                processDelete(exchange);
            } else {
                sendError(exchange, 405, "Метод не поддерживается");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Внутренняя ошибка сервера");
        } finally {
            exchange.close();
        }
    }

    private void processGet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if (path.equals("/movies") && query == null) {
            List<Movie> movies = store.getAll();
            sendJson(exchange, 200, movies);
            return;
        }

        if (path.equals("/movies") && query != null) {
            if (query.startsWith("year=")) {
                String yearValue = query.substring(5);
                if (yearValue.isEmpty()) {
                    sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                    return;
                }

                try {
                    int year = Integer.parseInt(yearValue);
                    List<Movie> movies = store.getByYear(year);
                    sendJson(exchange, 200, movies);
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                }
                return;
            }

            sendError(exchange, 400, "Некорректный параметр запроса — '" + query + "'");
            return;
        }

        if (path.startsWith("/movies/")) {
            processGetById(exchange, path);
            return;
        }

        sendError(exchange, 405, "Метод не поддерживается");
    }

    private void processGetById(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/movies/")) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        int id = extractId(path);
        if (id <= 0) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        Movie movie = store.getById(id).orElse(null);
        if (movie != null) {
            sendJson(exchange, 200, movie);
        } else {
            sendError(exchange, 404, "Фильм не найден");
        }
    }

    private void processPost(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (!path.equals("/movies")) {
            sendError(exchange, 405, "Метод не поддерживается");
            return;
        }

        if (!isJsonContentType(exchange)) {
            sendError(exchange, 415, "Unsupported Media Type");
            return;
        }

        String body = readBody(exchange);
        if (body == null || body.trim().isEmpty() || body.trim().equals("null")) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        try {
            CreateMovieRequest request = parseJson(body, CreateMovieRequest.class);

            if (request == null) {
                sendError(exchange, 400, "Некорректный JSON");
                return;
            }

            List<String> errors = validate(request);
            if (!errors.isEmpty()) {
                sendError(exchange, 422, "Ошибка валидации", errors);
                return;
            }

            Movie movie = store.add(request.getTitle(), request.getYear());
            sendJson(exchange, 201, movie);

        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "Некорректный JSON");
        }
    }

    private void processDelete(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (!path.startsWith("/movies/")) {
            sendError(exchange, 405, "Метод не поддерживается");
            return;
        }

        if (path.equals("/movies/")) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        int id = extractId(path);
        if (id <= 0) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        boolean deleted = store.delete(id);
        if (deleted) {
            sendEmpty(exchange);
        } else {
            sendError(exchange, 404, "Фильм не найден");
        }
    }

    private List<String> validate(CreateMovieRequest request) {
        List<String> errors = new ArrayList<>();
        int currentYear = Year.now().getValue();

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            errors.add("название должно быть не длиннее 100 символов");
        }

        int year = request.getYear();
        if (year < 1888 || year > currentYear + 1) {
            errors.add(String.format("год должен быть между 1888 и %d", currentYear + 1));
        }

        return errors;
    }

    private static class CreateMovieRequest {
        String title;
        int year;

        public CreateMovieRequest() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }
    }
}