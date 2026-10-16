package com.ballastlane.pokedex.adapter.out.persistence.user;

import com.ballastlane.pokedex.application.port.out.user.UserRepositoryPort;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Repository adapter: implements {@link UserRepositoryPort} on PostgreSQL. A unique-constraint
 * violation (two registrations racing past the existence checks) is translated into the domain's
 * {@link DuplicateUserException}, so the race ends in 409 rather than 500.
 */
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

  private final SpringDataUserRepository repository;
  private final UserPersistenceMapper mapper;

  UserPersistenceAdapter(SpringDataUserRepository repository, UserPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public boolean existsByUsername(Username username) {
    return repository.existsByUsername(username.value());
  }

  @Override
  public boolean existsByEmail(EmailAddress email) {
    return repository.existsByEmail(email.value());
  }

  @Override
  public Optional<User> findByUsername(Username username) {
    return repository.findByUsername(username.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findById(UserId id) {
    return repository.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  public User save(User user) {
    try {
      return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(user)));
    } catch (DataIntegrityViolationException violation) {
      throw translate(violation);
    }
  }

  private static RuntimeException translate(DataIntegrityViolationException violation) {
    String detail = String.valueOf(violation.getMostSpecificCause().getMessage());
    String lowerCase = detail.toLowerCase(Locale.ROOT);
    if (lowerCase.contains("uk_users_username")) {
      return new DuplicateUserException("username");
    }
    if (lowerCase.contains("uk_users_email")) {
      return new DuplicateUserException("email");
    }
    return violation;
  }
}
