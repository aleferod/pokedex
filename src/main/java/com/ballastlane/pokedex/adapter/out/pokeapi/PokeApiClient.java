package com.ballastlane.pokedex.adapter.out.pokeapi;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionChainDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.ResourceListDto;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin, polite HTTP client for PokeAPI: fixed base URL, connect and read timeouts, an identifying
 * {@code User-Agent}, and {@link PokeApiResilience} (retry, circuit breaker) around every request.
 * A 404 becomes a {@link PokeApiNotFoundException}; any other transport error, timeout, non-2xx
 * answer or empty body becomes a {@link PokeApiUnavailableException}, marked retryable for
 * connection failures, 5xx and 429. Callers never see Spring's HTTP exceptions.
 */
public final class PokeApiClient {

  private final RestClient http;
  private final PokeApiResilience resilience;

  private PokeApiClient(RestClient http, PokeApiResilience resilience) {
    this.http = http;
    this.resilience = resilience;
  }

  /** The production construction, also used by tests so timeouts and headers are exercised. */
  public static PokeApiClient create(
      String baseUrl,
      Duration connectTimeout,
      Duration readTimeout,
      String userAgent,
      PokeApiResilience resilience) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    return new PokeApiClient(
        RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build(),
        resilience);
  }

  ResourceListDto listPokemon(long offset, int limit) {
    return call(
        "list pokemon offset=" + offset + " limit=" + limit,
        () ->
            http.get()
                .uri(
                    uri ->
                        uri.path("/pokemon")
                            .queryParam("offset", offset)
                            .queryParam("limit", limit)
                            .build())
                .retrieve()
                .body(ResourceListDto.class));
  }

  PokemonDto pokemon(int id) {
    return pokemon(String.valueOf(id));
  }

  /** By number or lower-case name, as PokeAPI accepts both. */
  PokemonDto pokemon(String idOrName) {
    return call(
        "pokemon " + idOrName,
        () -> http.get().uri("/pokemon/{idOrName}", idOrName).retrieve().body(PokemonDto.class));
  }

  PokemonSpeciesDto species(int id) {
    return call(
        "pokemon-species " + id,
        () -> http.get().uri("/pokemon-species/{id}", id).retrieve().body(PokemonSpeciesDto.class));
  }

  EvolutionChainDto evolutionChain(int id) {
    return call(
        "evolution-chain " + id,
        () -> http.get().uri("/evolution-chain/{id}", id).retrieve().body(EvolutionChainDto.class));
  }

  private <T> T call(String what, Supplier<T> request) {
    return resilience.call(what, () -> attempt(what, request));
  }

  /** One HTTP attempt, its failure classified as transient (worth retrying) or not. */
  private static <T> T attempt(String what, Supplier<T> request) {
    T body;
    try {
      body = request.get();
    } catch (HttpClientErrorException.NotFound notFound) {
      throw new PokeApiNotFoundException("PokeAPI has no " + what, notFound);
    } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests overloaded) {
      throw new PokeApiUnavailableException("PokeAPI request failed: " + what, overloaded, true);
    } catch (ResourceAccessException io) {
      // A timeout has already cost the full timeout: repeating it would only double the wait.
      throw new PokeApiUnavailableException("PokeAPI request failed: " + what, io, !isTimeout(io));
    } catch (RestClientException failure) {
      throw new PokeApiUnavailableException("PokeAPI request failed: " + what, failure, false);
    }
    if (body == null) {
      throw new PokeApiUnavailableException("PokeAPI returned no body: " + what, null);
    }
    return body;
  }

  private static boolean isTimeout(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
        return true;
      }
    }
    return false;
  }
}
