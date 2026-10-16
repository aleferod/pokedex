package com.example.tasks.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 30)
  private String username;

  protected UserEntity() {}

  public UserEntity(UUID id, String username) {
    this.id = id;
    this.username = username;
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }
}
