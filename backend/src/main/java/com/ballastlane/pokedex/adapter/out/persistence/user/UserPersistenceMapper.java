package com.ballastlane.pokedex.adapter.out.persistence.user;

import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.PasswordHash;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.Username;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * DTO + Mapper between the domain {@link User} and {@link UserJpaEntity}. MapStruct generates the
 * field mapping at compile time (an unmapped field fails the build); the default methods tell it
 * how to unwrap and rebuild the value objects, which re-validates data read from the database.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserPersistenceMapper {

  UserJpaEntity toEntity(User user);

  User toDomain(UserJpaEntity entity);

  default UUID fromUserId(UserId id) {
    return id.value();
  }

  default UserId toUserId(UUID id) {
    return new UserId(id);
  }

  default String fromUsername(Username username) {
    return username.value();
  }

  default Username toUsername(String username) {
    return new Username(username);
  }

  default String fromEmail(EmailAddress email) {
    return email.value();
  }

  default EmailAddress toEmail(String email) {
    return new EmailAddress(email);
  }

  default String fromPasswordHash(PasswordHash hash) {
    return hash.value();
  }

  default PasswordHash toPasswordHash(String hash) {
    return new PasswordHash(hash);
  }
}
