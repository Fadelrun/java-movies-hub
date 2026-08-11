package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE_URL = "http://localhost:8080";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() throws IOException {
        MoviesStore store = new MoviesStore();

        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode(), "Статус должен быть 200 OK");

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        assertEquals("application/json; charset=UTF-8", contentType,
                "Content-Type должен быть application/json; charset=UTF-8");

        String body = response.body().trim();
        assertEquals("[]", body, "Тело должно быть пустым массивом");
    }

    @Test
    void getMovies_whenHasMovies_returnsMoviesList() throws Exception {
        addMovie("The Matrix", 1999);
        addMovie("Inception", 2010);
        addMovie("Interstellar", 2014);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        List<Movie> movies = GSON.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(3, movies.size(), "Должно быть 3 фильма");

        assertTrue(movies.stream().anyMatch(m ->
                m.getTitle().equals("The Matrix") && m.getYear() == 1999));

        assertTrue(movies.stream().anyMatch(m ->
                m.getTitle().equals("Inception") && m.getYear() == 2010));

        assertTrue(movies.stream().anyMatch(m ->
                m.getTitle().equals("Interstellar") && m.getYear() == 2014));
    }

    @Test
    void getMoviesByYear_whenExists_returnsMovies() throws Exception {
        addMovie("Pulp Fiction", 1994);
        addMovie("The Shawshank Redemption", 1994);
        addMovie("Forrest Gump", 1994);
        addMovie("The Godfather", 1972);
        addMovie("The Dark Knight", 2008);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=1994"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        List<Movie> movies = GSON.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(3, movies.size(), "Должно быть 3 фильма 1994 года");

        for (Movie movie : movies) {
            assertEquals(1994, movie.getYear(),
                    "Все фильмы должны быть 1994 года");
        }
    }

    @Test
    void getMoviesByYear_whenNotFound_returnsEmptyArray() throws Exception {
        addMovie("The Godfather", 1972);
        addMovie("The Dark Knight", 2008);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        assertEquals("[]", response.body().trim(),
                "Должен быть пустой массив");
    }

    @Test
    void getMoviesByYear_whenInvalidYear_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(),
                "Должен быть 400 Bad Request");

        assertTrue(response.body().contains("Некорректный параметр запроса"),
                "Должно быть сообщение об ошибке");
    }

    @Test
    void getMoviesByYear_whenUnknownParam_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?genre=action"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(),
                "Должен быть 400 Bad Request");
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie added = addMovie("Interstellar", 2014);
        int id = added.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode(),
                "Должен быть 404 Not Found");

        assertTrue(response.body().contains("Фильм не найден"),
                "Должно быть сообщение об ошибке");
    }

    @Test
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(400, response.statusCode(),
                "Должен быть 400 Bad Request");

        assertTrue(response.body().contains("Некорректный ID"),
                "Должно быть сообщение об ошибке");
    }

    @Test
    void postMovie_whenValid_returns201() throws Exception {
        String json = "{\"title\":\"The Dark Knight\",\"year\":2008}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode(),
                "Должен быть 201 Created");

        Movie movie = GSON.fromJson(response.body(), Movie.class);

        assertEquals("The Dark Knight", movie.getTitle(),
                "Название должно совпадать");
        assertEquals(2008, movie.getYear(),
                "Год должен совпадать");

        assertTrue(movie.getId() > 0,
                "ID должен быть положительным");
    }

    @Test
    void postMovie_whenEmptyTitle_returns422() throws Exception {
        String json = "{\"title\":\"\",\"year\":2020}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode(),
                "Должен быть 422 Unprocessable Entity");

        assertTrue(response.body().contains("название не должно быть пустым"),
                "Должна быть ошибка о пустом названии");
    }

    @Test
    void postMovie_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "A".repeat(101);
        String json = String.format("{\"title\":\"%s\",\"year\":2020}", longTitle);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());

        assertTrue(response.body().contains("название должно быть не длиннее 100 символов"),
                "Должна быть ошибка о длине названия");
    }

    @Test
    void postMovie_whenInvalidYear_returns422() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":1800}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());

        assertTrue(response.body().contains("год должен быть между 1888 и"),
                "Должна быть ошибка о диапазоне года");
    }

    @Test
    void postMovie_whenWrongContentType_returns415() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":2020}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, response.statusCode(),
                "Должен быть 415 Unsupported Media Type");
    }

    @Test
    void postMovie_whenEmptyBody_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(),
                "Должен быть 400 Bad Request");

        assertTrue(response.body().contains("Некорректный JSON"),
                "Должно быть сообщение об ошибке");
    }

    @Test
    void postMovie_whenMalformedJson_returns400() throws Exception {
        String malformedJson = "{\"title\":\"Test\",\"year\":2020";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(malformedJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode(),
                "Должен быть 400 Bad Request");
        assertTrue(response.body().contains("Некорректный JSON"),
                "Должно быть сообщение об ошибке");
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie added = addMovie("Star Wars", 1977);
        int id = added.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, response.statusCode(),
                "Должен быть 204 No Content");

        assertEquals("", response.body().trim(),
                "Тело должно быть пустым");

    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovie_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный ID"));
    }

    @Test
    void whenMethodNotAllowed_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, response.statusCode(),
                "Должен быть 405 Method Not Allowed");

        assertTrue(response.body().contains("Метод не поддерживается"),
                "Должно быть сообщение об ошибке");
    }

    private Movie addMovie(String title, int year) throws Exception {
        String json = String.format("{\"title\":\"%s\",\"year\":%d}", title, year);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode(),
                "addMovie() не удалось добавить фильм: " + title);

        return GSON.fromJson(response.body(), Movie.class);
    }
}