package com.ballastlane.pokedex.support;

import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailErrorController;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailFactory;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailWriter;
import com.ballastlane.pokedex.infrastructure.security.CredentialsConfiguration;
import com.ballastlane.pokedex.infrastructure.security.SecurityConfiguration;
import com.ballastlane.pokedex.infrastructure.time.TimeConfiguration;
import com.ballastlane.pokedex.infrastructure.web.CrossOriginConfiguration;
import com.ballastlane.pokedex.infrastructure.web.WebFilterConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@code @WebMvcTest} plus the application's cross-cutting web components (filters, error handling,
 * CORS, clock), so every web slice test runs the same request pipeline as production, under the
 * {@code test} profile.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@ActiveProfiles("test")
@Import({
  WebFilterConfiguration.class,
  CrossOriginConfiguration.class,
  ProblemDetailFactory.class,
  ProblemDetailWriter.class,
  SecurityConfiguration.class,
  CredentialsConfiguration.class,
  TimeConfiguration.class
})
public @interface WebSliceTest {

  /**
   * Controllers to include. Defaults to the error controller only, so each test names the
   * controllers it exercises (and mocks only their use cases); probe controllers are added with
   * {@code @Import}.
   */
  @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
  Class<?>[] controllers() default {ProblemDetailErrorController.class};
}
