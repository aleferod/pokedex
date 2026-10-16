package com.example.tasks.task;

import com.example.tasks.persistence.TaskEntity;
import com.example.tasks.persistence.TaskRepository;
import com.example.tasks.persistence.TaskSpecifications;
import com.example.tasks.persistence.UserEntity;
import com.example.tasks.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task use cases. Every operation is scoped to the calling user. */
@Service
public class TaskService {

  private static final Sort ORDER =
      Sort.by(Sort.Order.asc("dueDate").nullsLast(), Sort.Order.asc("createdAt"));

  private final TaskRepository tasks;
  private final UserRepository users;
  private final Clock clock;

  public TaskService(TaskRepository tasks, UserRepository users, Clock clock) {
    this.tasks = tasks;
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public TaskEntity create(String username, NewTask newTask) {
    UserEntity owner = user(username);
    if (newTask.dueDate() != null && newTask.dueDate().isBefore(today())) {
      throw new InvalidDueDateException();
    }
    TaskEntity task =
        new TaskEntity(
            UUID.randomUUID(),
            owner,
            newTask.title().trim(),
            newTask.description(),
            newTask.status() == null ? TaskStatus.TODO : newTask.status(),
            newTask.dueDate(),
            Instant.now(clock));
    return tasks.save(task);
  }

  @Transactional(readOnly = true)
  public Page<TaskEntity> list(
      String username, TaskStatus status, LocalDate dueBefore, int page, int size) {
    UserEntity owner = user(username);
    return tasks.findAll(
        TaskSpecifications.matching(owner.getId(), status, dueBefore),
        PageRequest.of(page, size, ORDER));
  }

  @Transactional(readOnly = true)
  public TaskEntity get(String username, UUID id) {
    return owned(username, id);
  }

  @Transactional
  public TaskEntity replace(String username, UUID id, TaskChanges changes) {
    TaskEntity task = owned(username, id);
    if (task.getVersion() != changes.version()) {
      throw new StaleTaskException(id);
    }
    if (changes.dueDate() != null
        && !Objects.equals(changes.dueDate(), task.getDueDate())
        && changes.dueDate().isBefore(today())) {
      throw new InvalidDueDateException();
    }
    task.replace(
        changes.title().trim(),
        changes.description(),
        changes.status(),
        changes.dueDate(),
        Instant.now(clock));
    return task;
  }

  @Transactional
  public TaskEntity changeStatus(String username, UUID id, TaskStatus status) {
    TaskEntity task = owned(username, id);
    task.changeStatus(status, Instant.now(clock));
    return task;
  }

  @Transactional
  public void delete(String username, UUID id) {
    tasks.delete(owned(username, id));
  }

  private TaskEntity owned(String username, UUID id) {
    UserEntity owner = user(username);
    return tasks.findByIdAndOwnerId(id, owner.getId()).orElseThrow(() -> new TaskNotFoundException(id));
  }

  private UserEntity user(String username) {
    return users.findByUsername(username).orElseThrow(() -> new UnknownUserException(username));
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
