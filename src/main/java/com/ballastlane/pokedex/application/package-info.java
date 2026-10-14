/**
 * Application business rules: use cases and the ports through which they talk to the outside world.
 *
 * <p>Depends on {@code domain} and the JDK only, so every use case can be unit-tested without a
 * Spring context. Transactions, caching and resilience are applied around ports by
 * adapters/infrastructure.
 */
package com.ballastlane.pokedex.application;
