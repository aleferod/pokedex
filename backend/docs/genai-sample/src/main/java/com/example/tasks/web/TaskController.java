package com.example.tasks.web;

import com.example.tasks.task.NewTask;
import com.example.tasks.task.TaskChanges;
import com.example.tasks.task.TaskService;
import com.example.tasks.task.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

  private final TaskService service;

  public TaskController(TaskService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTaskRequest request) {
    var task =
        service.create(
            jwt.getSubject(),
            new NewTask(request.title(), request.description(), request.status(), request.dueDate()));
    return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.getId()))
        .body(TaskMapper.toResponse(task));
  }

  @GetMapping
  public PageResponse<TaskResponse> list(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueBefore,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return TaskMapper.toResponse(service.list(jwt.getSubject(), status, dueBefore, page, size));
  }

  @GetMapping("/{id}")
  public TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return TaskMapper.toResponse(service.get(jwt.getSubject(), id));
  }

  @PutMapping("/{id}")
  public TaskResponse replace(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateTaskRequest request) {
    return TaskMapper.toResponse(
        service.replace(
            jwt.getSubject(),
            id,
            new TaskChanges(
                request.title(),
                request.description(),
                request.status(),
                request.dueDate(),
                request.version())));
  }

  @PatchMapping("/{id}/status")
  public TaskResponse changeStatus(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody StatusRequest request) {
    return TaskMapper.toResponse(service.changeStatus(jwt.getSubject(), id, request.status()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    service.delete(jwt.getSubject(), id);
    return ResponseEntity.noContent().build();
  }
}
