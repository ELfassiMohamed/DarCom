package com.gitlearn.service;

import com.gitlearn.model.Task;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory task service.
 * Acts as a bridge — in production, this would delegate to Convex
 * or a JPA persistence layer.
 */
@ApplicationScoped
public class TaskService {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public Optional<Task> getTask(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Task createTask(Task task) {
        String id = String.valueOf(counter.incrementAndGet());
        task.setId(id);
        tasks.put(id, task);
        return task;
    }

    public Optional<Task> updateTask(String id, Task updated) {
        return getTask(id).map(existing -> {
            existing.setTitle(updated.getTitle());
            existing.setDescription(updated.getDescription());
            existing.setCompleted(updated.isCompleted());
            return existing;
        });
    }

    public boolean deleteTask(String id) {
        return tasks.remove(id) != null;
    }
}
