package com.example.tasks.persistence;

import com.example.tasks.task.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * A task row. The id is assigned by the application, so the entity tells Spring Data itself
 * whether it is new ({@link Persistable}); otherwise {@code save} would merge, selecting the row
 * before every insert.
 */
@Entity
@Table(name = "tasks")
public class TaskEntity implements Persistable<UUID> {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private UserEntity owner;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  @Transient private boolean isNew = true;

  protected TaskEntity() {}

  public TaskEntity(
      UUID id,
      UserEntity owner,
      String title,
      String description,
      TaskStatus status,
      LocalDate dueDate,
      Instant now) {
    this.id = id;
    this.owner = owner;
    this.title = title;
    this.description = description;
    this.status = status;
    this.dueDate = dueDate;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void replace(
      String title, String description, TaskStatus status, LocalDate dueDate, Instant now) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.dueDate = dueDate;
    this.updatedAt = now;
  }

  public void changeStatus(TaskStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }

  public UserEntity getOwner() {
    return owner;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
