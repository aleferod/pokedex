package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.localpokemon.BulkSyncResult;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncCatalogPage;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncResult;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.support.TestTokens;
import com.ballastlane.pokedex.support.WebSliceTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebSliceTest(controllers = LocalPokemonSyncController.class)
class LocalPokemonSyncControllerTest {

  private static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00Z");
  private static final LocalPokemonId ID = LocalPokemonId.random();
  private static final PokemonIdentifier PIKACHU = PokemonIdentifier.parse("pikachu");

  @Autowired private MockMvcTester mvc;
  @Autowired private TokenPort tokenPort;
  @Autowired private JwtEncoder jwtEncoder;

  @MockitoBean private SyncPokemon syncPokemon;
  @MockitoBean private SyncCatalogPage syncCatalogPage;

  private String ash;

  @BeforeEach
  void setUp() {
    ash = new TestTokens(tokenPort, jwtEncoder).bearer(TestTokens.user("ash", Role.USER));
  }

  // ---- sync one --------------------------------------------------------------------------------

  @Test
  void should_return201WithLocationEtagAndRecord_when_firstImport() {
    // Given
    given(syncPokemon.sync(PIKACHU, "ash")).willReturn(new SyncResult(imported(0L), true));

    // When
    MvcTestResult result = sync("pikachu", ash);

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.CREATED)
        .hasHeader(HttpHeaders.LOCATION, "/api/v1/local-pokemon/" + ID.value())
        .hasHeader(HttpHeaders.ETAG, "\"0\"");
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys(
            "id",
            "pokeapiId",
            "name",
            "spriteUrl",
            "imageUrl",
            "category",
            "types",
            "height",
            "weight",
            "abilities",
            "stats",
            "localizedNames",
            "region",
            "habitatNotes",
            "internalTags",
            "notes",
            "createdAt",
            "updatedAt",
            "syncedAt",
            "createdBy",
            "version")
        .containsEntry("id", ID.value().toString())
        .containsEntry("pokeapiId", 25)
        .containsEntry("localizedNames", Map.of())
        .containsEntry("internalTags", List.of())
        .containsEntry("createdBy", "ash");
    assertThat(result).bodyJson().extractingPath("$.stats.speed").asNumber().isEqualTo(90);
  }

  @Test
  void should_return200WithNewEtagAndNoLocation_when_resynced() {
    given(syncPokemon.sync(PokemonIdentifier.parse("25"), "ash"))
        .willReturn(new SyncResult(withProprietaryData(4L), false));

    MvcTestResult result = sync("25", ash);

    assertThat(result)
        .hasStatus(HttpStatus.OK)
        .hasHeader(HttpHeaders.ETAG, "\"4\"")
        .doesNotContainHeader(HttpHeaders.LOCATION);
    assertThat(result).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.localizedNames")
        .asMap()
        .containsEntry("pt-BR", "Pikachu");
  }

  @Test
  void should_return404_when_pokemonIsUnknown() {
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(syncPokemon.sync(missingno, "ash")).willThrow(new PokemonNotFoundException(missingno));

    MvcTestResult result = sync("missingno", ash);

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("No Pokémon matches 'missingno'.");
    then(syncPokemon).should().sync(missingno, "ash");
  }

  @Test
  void should_return400WithoutSyncing_when_identifierIsMalformed() {
    MvcTestResult result = sync("pika chu", ash);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("idOrName");
    then(syncPokemon).should(never()).sync(any(), anyString());
  }

  @Test
  void should_return503_when_pokeApiIsUnavailable() {
    given(syncPokemon.sync(PIKACHU, "ash"))
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    assertThat(sync("pikachu", ash))
        .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
        .containsHeader(HttpHeaders.RETRY_AFTER);
  }

  @Test
  void should_return409_when_concurrentChangesKeepWinning() {
    given(syncPokemon.sync(PIKACHU, "ash")).willThrow(new StaleLocalPokemonException(ID));

    MvcTestResult result = sync("pikachu", ash);

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#conflict");
  }

  @Test
  void should_return401WithoutSyncing_when_anonymous() {
    MvcTestResult result = mvc.post().uri("/api/v1/local-pokemon/sync/pikachu").exchange();

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    then(syncPokemon).shouldHaveNoInteractions();
  }

  // ---- bulk sync --------------------------------------------------------------------------------

  @Test
  void should_summariseEachEntry_when_bulkSyncing() {
    // Given
    given(syncCatalogPage.syncFirst(3, "ash"))
        .willReturn(
            new BulkSyncResult(
                List.of(
                    new BulkSyncResult.Entry(new PokemonId(1), "bulbasaur", true),
                    new BulkSyncResult.Entry(new PokemonId(2), "ivysaur", true),
                    new BulkSyncResult.Entry(new PokemonId(3), "venusaur", false))));

    // When
    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/local-pokemon/sync?limit=3")
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$.created").asNumber().isEqualTo(2);
    assertThat(result).bodyJson().extractingPath("$.updated").asNumber().isEqualTo(1);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.results[*].status")
        .asArray()
        .containsExactly("created", "created", "updated");
    assertThat(result).bodyJson().extractingPath("$.results[0].pokeapiId").asNumber().isEqualTo(1);
  }

  @Test
  void should_syncTenByDefault_when_noLimitIsGiven() {
    given(syncCatalogPage.syncFirst(10, "ash")).willReturn(new BulkSyncResult(List.of()));

    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/local-pokemon/sync")
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    assertThat(result).hasStatus(HttpStatus.OK);
    then(syncCatalogPage).should().syncFirst(10, "ash");
  }

  @Test
  void should_return400NamingLimit_when_limitIsTooHigh() {
    given(syncCatalogPage.syncFirst(21, "ash"))
        .willThrow(new InvalidFieldException("limit", "must be between 1 and 20"));

    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/local-pokemon/sync?limit=21")
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("limit");
  }

  private MvcTestResult sync(String idOrName, String bearer) {
    return mvc.post()
        .uri("/api/v1/local-pokemon/sync/{idOrName}", idOrName)
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .exchange();
  }

  private static LocalPokemon imported(long version) {
    LocalPokemon pokemon =
        LocalPokemon.importFrom(ID, new PokemonId(25), pikachu(60), "ash", IMPORTED);
    return new LocalPokemon(
        pokemon.id(),
        pokemon.pokeapiId(),
        pokemon.upstream(),
        pokemon.proprietary(),
        pokemon.audit(),
        version);
  }

  private static LocalPokemon withProprietaryData(long version) {
    return new LocalPokemon(
        ID,
        new PokemonId(25),
        pikachu(61),
        new ProprietaryData(
            LocalizedNames.of(Map.of("pt-BR", "Pikachu")),
            "Kanto",
            null,
            InternalTags.of(List.of("starter-line")),
            null),
        new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"),
        version);
  }
}
