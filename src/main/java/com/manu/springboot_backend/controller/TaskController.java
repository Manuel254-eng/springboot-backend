package com.manu.springboot_backend.controller;

import com.manu.springboot_backend.exception.ResourceNotFoundException;
import com.manu.springboot_backend.model.Task;
import com.manu.springboot_backend.model.TaskStatus;
import com.manu.springboot_backend.repository.TaskRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})

@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Operation(summary = "Get all tasks", description = "Retrieves a list of all tasks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tasks"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Create a new task", description = "Adds a new task to the database")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task savedTask = taskRepository.save(task);
        return new ResponseEntity<>(savedTask, HttpStatus.CREATED);
    }

    @Operation(summary = "Get task by ID", description = "Retrieves a task based on the provided ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return ResponseEntity.ok(task);
    }

    @Operation(summary = "Update task", description = "Updates an existing task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setStatus(taskDetails.getStatus());
        task.setDueDate(taskDetails.getDueDate());

        Task updatedTask = taskRepository.save(task);
        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "Delete task", description = "Deletes a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        taskRepository.delete(task);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Task deleted successfully");
    }

    // Add a route to update the status of a task
    @Operation(summary = "Update task status", description = "Updates the status of an existing task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long id, @RequestBody TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(status);

        Task updatedTask = taskRepository.save(task);
        return ResponseEntity.ok(updatedTask);
    }
}
