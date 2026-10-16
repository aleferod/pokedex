package com.example.tasks.persistence;

import com.example.tasks.task.TaskStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * The list filters as criteria, added only when given: no {@code :param is null} tricks, which
 * PostgreSQL cannot type when the value is null.
 */
public final class TaskSpecifications {

  private TaskSpecifications() {}

  public static Specification<TaskEntity> matching(
      UUID ownerId, TaskStatus status, LocalDate dueBefore) {
    List<Specification<TaskEntity>> criteria = new ArrayList<>();
    criteria.add((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId));
    if (status != null) {
      criteria.add((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (dueBefore != null) {
      criteria.add((root, query, cb) -> cb.lessThan(root.<LocalDate>get("dueDate"), dueBefore));
    }
    return Specification.allOf(criteria);
  }
}
