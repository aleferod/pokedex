package com.example.tasks.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.tasks.persistence.TaskEntity;
import com.example.tasks.persistence.TaskRepository;
import com.example.tasks.persistence.UserEntity;
import com.example.tasks.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-10T10:00:00Z");
  private static final LocalDate TODAY = LocalDate.parse("2026-03-10");

  @Mock private TaskRepository tasks;
  @Mock private UserRepository users;

  private TaskService service;
  private UserEntity alice;

  @BeforeEach
  void setUp() {
    service = new TaskService(tasks, users, Clock.fixed(NOW, ZoneOffset.UTC));
    alice = new UserEntity(UUID.randomUUID(), "alice");
    given(users.findByUsername("alice")).willReturn(Optional.of(alice));
  }

  @Test
  void createsTaskWithDefaultStatus() {
    given(tasks.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    TaskEntity task = service.create("alice", new NewTask("  Write tests  ", null, null, TODAY));

    assertThat(task.getTitle()).isEqualTo("Write tests");
    assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    assertThat(task.getOwner()).isEqualTo(alice);
  }

  @Test
  void rejectsPastDueDateOnCreate() {
    assertThatThrownBy(
            () -> service.create("alice", new NewTask("Late", null, null, TODAY.minusDays(1))))
        .isInstanceOf(InvalidDueDateException.class);
  }

  @Test
  void allowsUpdateOfOverdueTaskWhenDueDateIsUnchanged() {
    TaskEntity overdue =
        new TaskEntity(
            UUID.randomUUID(), alice, "Old", null, TaskStatus.TODO, TODAY.minusDays(5), NOW);
    given(tasks.findByIdAndOwnerId(overdue.getId(), alice.getId())).willReturn(Optional.of(overdue));

    TaskEntity updated =
        service.replace(
            "alice",
            overdue.getId(),
            new TaskChanges("Old, edited", null, TaskStatus.IN_PROGRESS, TODAY.minusDays(5), 0));

    assertThat(updated.getTitle()).isEqualTo("Old, edited");
  }

  @Test
  void rejectsUpdateThatMovesDueDateIntoThePast() {
    TaskEntity task =
        new TaskEntity(UUID.randomUUID(), alice, "Task", null, TaskStatus.TODO, TODAY, NOW);
    given(tasks.findByIdAndOwnerId(task.getId(), alice.getId())).willReturn(Optional.of(task));

    assertThatThrownBy(
            () ->
                service.replace(
                    "alice",
                    task.getId(),
                    new TaskChanges("Task", null, TaskStatus.TODO, TODAY.minusDays(1), 0)))
        .isInstanceOf(InvalidDueDateException.class);
  }

  @Test
  void anotherUsersTaskIsNotFound() {
    UUID foreignTask = UUID.randomUUID();
    given(tasks.findByIdAndOwnerId(foreignTask, alice.getId())).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("alice", foreignTask))
        .isInstanceOf(TaskNotFoundException.class);
  }

  @Test
  void staleVersionIsRejected() {
    TaskEntity task =
        new TaskEntity(UUID.randomUUID(), alice, "Task", null, TaskStatus.TODO, TODAY, NOW);
    given(tasks.findByIdAndOwnerId(task.getId(), alice.getId())).willReturn(Optional.of(task));

    assertThatThrownBy(
            () ->
                service.replace(
                    "alice", task.getId(), new TaskChanges("Task", null, TaskStatus.DONE, TODAY, 3)))
        .isInstanceOf(StaleTaskException.class);
  }
}
