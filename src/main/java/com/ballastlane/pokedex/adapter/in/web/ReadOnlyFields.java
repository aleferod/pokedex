package com.ballastlane.pokedex.adapter.in.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a request body whose resource has members clients may read but not write. A member of
 * {@link #value()} that the annotated request does not declare is read-only: sending it gets {@link
 * #reason()} instead of the generic "is not an accepted field". JSON member names must equal the
 * record component names (no renaming).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnlyFields {

  /** The resource's representation, a record. */
  Class<? extends Record> value();

  /** The message for a read-only member. */
  String reason() default "is read-only";
}
