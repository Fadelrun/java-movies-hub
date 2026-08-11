package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();

    private int nextId = 1;

    public Movie add(String title, int year) {
        int id = nextId++;
        Movie movie = new Movie(id, title, year);

        movies.put(id, movie);

        return movie;
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getByYear(int year) {
        List<Movie> result = new ArrayList<>();

        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }

        return result;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }

    public boolean isEmpty() {
        return movies.isEmpty();
    }

    public int size() {
        return movies.size();
    }
}