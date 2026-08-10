package ru.practicum.moviehub.http;


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
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            if (method.equals("GET") && path.equals("/movies") && query == null) {
                List<Movie> movies = store.getAll();
                sendJson(exchange, 200, movies);
            } else if (method.equals("GET") && path.equals("/movies") && query != null) {
                if (query.startsWith("year=")) {
                    try {
                        int year = Integer.parseInt(query.substring(5));
                        List<Movie> movies = store.getByYear(year);
                        sendJson(exchange, 200, movies);
                    } catch (NumberFormatException e) {
                        sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                    }
                }
            } else if (method.equals("GET") && path.startsWith("/movies/")) {
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
            } else if (method.equals("POST") && path.equals("/movies")) {
                if (!isJsonContentType(exchange)) {
                    sendError(exchange, 415, "Unsupported Media Type");
                    return;
                }
                String body = readBody(exchange);

                if (body == null || body.trim().isEmpty()) {
                    sendError(exchange, 400, "Некорректный JSON");
                    return;
                }

                if (body.trim().equals("null")) {
                    sendError(exchange, 400, "Некорректный JSON");
                    return;
                }

                try {
                    CreateMovieRequest request = parseJson(body, CreateMovieRequest.class);

                    List<String> errors = validate(request);

                    if (!errors.isEmpty()) {
                        sendError(exchange, 422, "Ошибка валидации", errors);
                        return;
                    }

                    Movie movie = store.add(request.getTitle(), request.getYear());

                    sendJson(exchange, 201, movie);

                } catch (com.google.gson.JsonSyntaxException e) {
                    sendError(exchange, 400, "Некорректный JSON");
                }
            } else if (method.equals("DELETE") && path.matches("/movies/\\d+")) {
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
            } else {
                sendError(exchange, 405, "Метод не поддерживается");
            }
        } catch (
                Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Внутренняя ошибка сервера");
        } finally {
            exchange.close();
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
