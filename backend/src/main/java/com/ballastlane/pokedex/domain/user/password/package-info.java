/**
 * Password strength policy: a chain of {@link
 * com.ballastlane.pokedex.domain.user.password.PasswordRule} links evaluated by {@link
 * com.ballastlane.pokedex.domain.user.password.PasswordPolicy}. Independent of the rest of {@code
 * domain.user}, so the two packages have no cycle.
 */
package com.ballastlane.pokedex.domain.user.password;
