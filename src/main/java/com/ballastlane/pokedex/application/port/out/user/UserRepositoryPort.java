package com.ballastlane.pokedex.application.port.out.user;

import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import java.util.Optional;

/** Repository port for accounts. Values are already normalized by the domain value objects. */
public interface UserRepositoryPort {

  boolean existsByUsername(Username username);

  boolean existsByEmail(EmailAddress email);

  Optional<User> findByUsername(Username username);

  Optional<User> findById(UserId id);

  /**
   * @throws com.ballastlane.pokedex.domain.user.DuplicateUserException a concurrent registration
   *     took the username or email first
   */
  User save(User user);
}
