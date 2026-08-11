package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) throws IOException {
        this.store = store;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/movies", new MoviesHttpHandler(store));
        this.server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("MovieHub сервер запущен на http://localhost:8080");
        System.out.println("Доступные маршруты:");
        System.out.println("  GET    /movies");
        System.out.println("  GET    /movies/{id}");
        System.out.println("  POST   /movies");
        System.out.println("  DELETE /movies/{id}");
        System.out.println("  GET    /movies?year=YYYY");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getStore() {
        return store;
    }
}