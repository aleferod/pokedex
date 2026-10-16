package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemonCommand;
import com.ballastlane.pokedex.application.port.in.localpokemon.DeleteLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.ExpectedVersion;
import com.ballastlane.pokedex.application.port.in.localpokemon.GetLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.ListLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.PatchLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.ProprietaryValues;
import com.ballastlane.pokedex.application.port.in.localpokemon.ReplaceLocalPokemon;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.FieldPatch;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNamesPatch;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryPatch;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.support.TestTokens;
import com.ballastlane.pokedex.support.WebSliceTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebSliceTest(controllers = LocalPokemonController.class)
class LocalPokemonControllerTest {

  private static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00Z");
  private static final LocalPokemonId ID = LocalPokemonId.random();
  private static final String READ_ONLY =
      "is read-only: it comes from PokeAPI or is managed by the service";
  private static final MediaType MERGE_PATCH =
      MediaType.parseMediaType("application/merge-patch+json");

  @Autowired private MockMvcTester mvc;
  @Autowired private TokenPort tokenPort;
  @Autowired private JwtEncoder jwtEncoder;

  @MockitoBean private GetLocalPokemon getLocalPokemon;
  @MockitoBean private ListLocalPokemon listLocalPokemon;
  @MockitoBean private CreateLocalPokemon createLocalPokemon;
  @MockitoBean private ReplaceLocalPokemon replaceLocalPokemon;
  @MockitoBean private PatchLocalPokemon patchLocalPokemon;
  @MockitoBean private DeleteLocalPokemon deleteLocalPokemon;

  private String ash;
  private String admin;

  @BeforeEach
  void setUp() {
    TestTokens tokens = new TestTokens(tokenPort, jwtEncoder);
    ash = tokens.bearer(TestTokens.user("ash", Role.USER));
    admin = tokens.bearerFor(Role.ADMIN);
  }

  // ---- read one ---------------------------------------------------------------------------------

  @Test
  void should_returnRecordWithEtag_when_idExists() {
    given(getLocalPokemon.get(ID)).willReturn(withProprietaryData(7L));

    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/local-pokemon/{id}", ID.value())
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.OK)
        .hasContentType(MediaType.APPLICATION_JSON)
        .hasHeader(HttpHeaders.ETAG, "\"7\"");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.internalTags")
        .asArray()
        .containsExactly("starter-line");
  }

  @Test
  void should_return404_when_idIsUnknown() {
    given(getLocalPokemon.get(ID)).willThrow(new LocalPokemonNotFoundException(ID));

    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/local-pokemon/{id}", ID.value())
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("No local Pokémon with id " + ID.value() + ".");
  }

  @Test
  void should_return400NamingId_when_idIsNotAUuid() {
    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/local-pokemon/not-a-uuid")
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("id");
    then(getLocalPokemon).shouldHaveNoInteractions();
  }

  // ---- list ------------------------------------------------------------------------------------

  @Test
  void should_returnTheFilteredPageInTheEnvelope_when_listing() {
    // Given
    given(listLocalPokemon.list(new LocalPokemonFilter("starter", "pika"), new PageRequest(1, 5)))
        .willReturn(Page.of(List.of(withProprietaryData(7L)), new PageRequest(1, 5), 6));

    // When
    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/local-pokemon?page=1&size=5&tag=STARTER&name=pika")
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys("content", "page", "size", "totalElements", "totalPages")
        .containsEntry("page", 1)
        .containsEntry("size", 5)
        .containsEntry("totalElements", 6)
        .containsEntry("totalPages", 2);
    assertThat(result).bodyJson().extractingPath("$.content[0].pokeapiId").asNumber().isEqualTo(25);
    assertThat(result).bodyJson().extractingPath("$.content[0].version").asNumber().isEqualTo(7);
  }

  @Test
  void should_listTheFirstTwentyUnfiltered_when_noParametersAreGiven() {
    given(listLocalPokemon.list(LocalPokemonFilter.none(), new PageRequest(0, 20)))
        .willReturn(Page.of(List.of(), new PageRequest(0, 20), 0));

    MvcTestResult result =
        mvc.get().uri("/api/v1/local-pokemon").header(HttpHeaders.AUTHORIZATION, ash).exchange();

    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$.content").asArray().isEmpty();
    then(listLocalPokemon).should().list(LocalPokemonFilter.none(), new PageRequest(0, 20));
  }

  @Test
  void should_return400NamingEachParameter_when_pagingOrFiltersAreInvalid() {
    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/local-pokemon?page=-1&size=101&tag=" + "t".repeat(51))
            .header(HttpHeaders.AUTHORIZATION, ash)
            .exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("page", "size", "tag");
    then(listLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return401WithoutListing_when_anonymous() {
    assertThat(mvc.get().uri("/api/v1/local-pokemon").exchange())
        .hasStatus(HttpStatus.UNAUTHORIZED);
    then(listLocalPokemon).shouldHaveNoInteractions();
  }

  // ---- create ----------------------------------------------------------------------------------

  @Test
  void should_return201WithLocationEtagAndRecord_when_created() {
    // Given
    given(
            createLocalPokemon.create(
                new CreateLocalPokemonCommand(
                    25,
                    new ProprietaryValues(
                        Map.of("pt-BR", "Pikachu"), "Kanto", null, List.of("starter-line"), null)),
                "ash"))
        .willReturn(withProprietaryData(0L));

    // When
    MvcTestResult result =
        post(
            """
            {"pokeapiId": 25, "region": "Kanto", "internalTags": ["starter-line"],
             "localizedNames": {"pt-BR": "Pikachu"}}
            """);

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.CREATED)
        .hasHeader(HttpHeaders.LOCATION, "/api/v1/local-pokemon/" + ID.value())
        .hasHeader(HttpHeaders.ETAG, "\"0\"");
    assertThat(result).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
    assertThat(result).bodyJson().extractingPath("$.createdBy").isEqualTo("ash");
  }

  @Test
  void should_return409NamingPokeapiId_when_numberIsAlreadyStored() {
    given(createLocalPokemon.create(any(), anyString()))
        .willThrow(new DuplicateLocalPokemonException(new PokemonId(25)));

    MvcTestResult result = post("{\"pokeapiId\": 25}");

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#conflict");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Pokémon #25 is already stored locally.");
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("pokeapiId");
  }

  @Test
  void should_return400NamingPokeapiId_when_pokeApiHasNoSuchNumber() {
    given(createLocalPokemon.create(any(), anyString()))
        .willThrow(new InvalidFieldException("pokeapiId", "no Pokémon has this number"));

    MvcTestResult result = post("{\"pokeapiId\": 99999}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("pokeapiId");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo("no Pokémon has this number");
    then(createLocalPokemon).should().create(any(), anyString());
  }

  @Test
  void should_return400NamingPokeapiIdWithoutCreating_when_numberIsMissing() {
    MvcTestResult result = post("{}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("pokeapiId");
    then(createLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return400SayingReadOnlyWithoutCreating_when_bodySetsAnUpstreamField() {
    MvcTestResult result = post("{\"pokeapiId\": 25, \"weight\": 999}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("weight");
    assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo(READ_ONLY);
    then(createLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return400NotAccepted_when_bodyHasAMemberNoRepresentationHas() {
    MvcTestResult result = post("{\"pokeapiId\": 25, \"colour\": \"yellow\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("colour");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo("is not an accepted field");
  }

  // ---- replace ---------------------------------------------------------------------------------

  @Test
  void should_return200WithNewEtag_when_replacedAtTheCurrentVersion() {
    // Given
    given(
            replaceLocalPokemon.replace(
                ID,
                ExpectedVersion.exactly(3),
                new ProprietaryValues(null, "Johto", null, null, null)))
        .willReturn(withProprietaryData(4L));

    // When
    MvcTestResult result = put("\"3\"", "{\"region\": \"Johto\"}");

    // Then
    assertThat(result).hasStatus(HttpStatus.OK).hasHeader(HttpHeaders.ETAG, "\"4\"");
    assertThat(result).bodyJson().extractingPath("$.version").asNumber().isEqualTo(4);
  }

  @Test
  void should_return428WithoutReplacing_when_ifMatchIsMissing() {
    MvcTestResult result = put(null, "{\"region\": \"Johto\"}");

    assertThat(result).hasStatus(HttpStatus.PRECONDITION_REQUIRED);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.type")
        .asString()
        .endsWith("#precondition-required");
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("If-Match");
    then(replaceLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return409_when_replacingWithAStaleEtag() {
    given(replaceLocalPokemon.replace(any(), any(), any()))
        .willThrow(new StaleLocalPokemonException(ID));

    MvcTestResult result = put("\"2\"", "{\"region\": \"Johto\"}");

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#conflict");
    then(replaceLocalPokemon).should().replace(any(), any(), any());
  }

  @Test
  void should_return400SayingReadOnly_when_replaceSendsThePokeApiNumber() {
    MvcTestResult result = put("\"3\"", "{\"pokeapiId\": 26}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("pokeapiId");
    assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo(READ_ONLY);
    then(replaceLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return404_when_replacingAnUnknownId() {
    given(replaceLocalPokemon.replace(any(), any(), any()))
        .willThrow(new LocalPokemonNotFoundException(ID));

    MvcTestResult result = put("\"0\"", "{}");

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("No local Pokémon with id " + ID.value() + ".");
  }

  // ---- patch -----------------------------------------------------------------------------------

  @Test
  void should_translateTheMergePatch_when_contentTypeIsMergePatchJson() {
    // Given
    ProprietaryPatch expected =
        new ProprietaryPatch(
            LocalizedNamesPatch.merge(Map.of("fr", "Pikachu"), Set.of("ja")),
            FieldPatch.clear(),
            FieldPatch.keep(),
            FieldPatch.keep(),
            FieldPatch.set("Loves ketchup"));
    given(patchLocalPokemon.patch(ID, ExpectedVersion.exactly(3), expected))
        .willReturn(withProprietaryData(4L));

    // When
    MvcTestResult result =
        patch(
            MERGE_PATCH,
            "\"3\"",
            """
            {"notes": "Loves ketchup", "region": null,
             "localizedNames": {"fr": "Pikachu", "ja": null}}
            """);

    // Then
    assertThat(result).hasStatus(HttpStatus.OK).hasHeader(HttpHeaders.ETAG, "\"4\"");
    then(patchLocalPokemon).should().patch(ID, ExpectedVersion.exactly(3), expected);
  }

  @Test
  void should_acceptPlainJson_when_patching() {
    ProprietaryPatch expected =
        new ProprietaryPatch(
            LocalizedNamesPatch.clear(), null, null, FieldPatch.set(List.of("starter")), null);
    given(patchLocalPokemon.patch(ID, ExpectedVersion.exactly(3), expected))
        .willReturn(withProprietaryData(4L));

    MvcTestResult result =
        patch(
            MediaType.APPLICATION_JSON,
            "\"3\"",
            "{\"internalTags\": [\"starter\"], \"localizedNames\": null}");

    assertThat(result).hasStatus(HttpStatus.OK);
    then(patchLocalPokemon).should().patch(ID, ExpectedVersion.exactly(3), expected);
  }

  @Test
  void should_return400ListingEveryReadOnlyMember_when_patchTouchesUpstreamFields() {
    MvcTestResult result =
        patch(
            MERGE_PATCH, "\"3\"", "{\"weight\": 999, \"name\": \"raichu\", \"notes\": \"heavy\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("name", "weight");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].message")
        .asArray()
        .containsOnly(READ_ONLY);
    then(patchLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return400ListingWrongTypesAndUnknownMembers_when_patchIsMalformed() {
    MvcTestResult result =
        patch(
            MERGE_PATCH,
            "\"3\"",
            """
            {"region": 5, "internalTags": ["ok", 3], "colour": "yellow",
             "localizedNames": {"fr": 1}}
            """);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("colour", "internalTags[1]", "localizedNames.fr", "region");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].message")
        .asArray()
        .containsExactly(
            "is not an accepted field",
            "has the wrong type",
            "has the wrong type",
            "has the wrong type");
    then(patchLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return400_when_patchIsNotAJsonObject() {
    MvcTestResult result = patch(MERGE_PATCH, "\"3\"", "[\"notes\"]");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("body");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo("must be a JSON object");
    then(patchLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return400ListingEveryRuleBroken_when_patchedValuesAreInvalid() {
    given(patchLocalPokemon.patch(any(), any(), any()))
        .willThrow(
            new InvalidFieldsException(
                List.of(
                    new InvalidFieldException("region", "must be at most 50 characters"),
                    new InvalidFieldException("internalTags", "'Bad Tag!' must be ..."))));

    MvcTestResult result =
        patch(MERGE_PATCH, "\"3\"", "{\"region\": \"x\", \"internalTags\": [\"Bad Tag!\"]}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("internalTags", "region");
  }

  @Test
  void should_return428WithoutPatching_when_ifMatchIsMissing() {
    MvcTestResult result = patch(MERGE_PATCH, null, "{\"notes\": \"x\"}");

    assertThat(result).hasStatus(HttpStatus.PRECONDITION_REQUIRED);
    then(patchLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return409_when_patchingWithAStaleEtag() {
    given(patchLocalPokemon.patch(any(), any(), any()))
        .willThrow(new StaleLocalPokemonException(ID));

    MvcTestResult result = patch(MERGE_PATCH, "\"3\"", "{\"notes\": \"x\"}");

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
    then(patchLocalPokemon).should().patch(any(), any(), any());
  }

  @Test
  void should_return415_when_patchHasAnotherContentType() {
    MvcTestResult result = patch(MediaType.TEXT_PLAIN, "\"3\"", "notes=x");

    assertThat(result).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    then(patchLocalPokemon).shouldHaveNoInteractions();
  }

  // ---- If-Match parsing ------------------------------------------------------------------------

  @Test
  void should_readTheVersion_when_ifMatchIsAWeakEtag() {
    given(patchLocalPokemon.patch(eq(ID), eq(ExpectedVersion.exactly(3)), any()))
        .willReturn(withProprietaryData(4L));

    assertThat(patch(MERGE_PATCH, "W/\"3\"", "{}")).hasStatus(HttpStatus.OK);
  }

  @Test
  void should_acceptAnyVersion_when_ifMatchIsAStar() {
    given(patchLocalPokemon.patch(eq(ID), eq(ExpectedVersion.any()), any()))
        .willReturn(withProprietaryData(4L));

    assertThat(patch(MERGE_PATCH, "*", "{}")).hasStatus(HttpStatus.OK);
  }

  @ParameterizedTest
  @ValueSource(strings = {"3", "\"three\"", "\"1\", \"2\"", "\"99999999999999999999\""})
  void should_return400NamingIfMatch_when_headerIsNotASingleEtag(String ifMatch) {
    MvcTestResult result = patch(MERGE_PATCH, ifMatch, "{}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("If-Match");
    then(patchLocalPokemon).shouldHaveNoInteractions();
  }

  // ---- delete ----------------------------------------------------------------------------------

  @Test
  void should_return204WithoutBody_when_adminDeletes() {
    MvcTestResult result = delete(admin, null);

    assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(result.getResponse().getContentLength()).isZero();
    then(deleteLocalPokemon).should().delete(ID, ExpectedVersion.any());
  }

  @Test
  void should_passTheExpectedVersion_when_adminDeletesWithIfMatch() {
    assertThat(delete(admin, "\"5\"")).hasStatus(HttpStatus.NO_CONTENT);
    then(deleteLocalPokemon).should().delete(ID, ExpectedVersion.exactly(5));
  }

  @Test
  void should_return403WithoutDeleting_when_callerIsNotAdmin() {
    assertThat(delete(ash, null)).hasStatus(HttpStatus.FORBIDDEN);
    then(deleteLocalPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return404_when_deletingAnUnknownId() {
    willThrow(new LocalPokemonNotFoundException(ID))
        .given(deleteLocalPokemon)
        .delete(ID, ExpectedVersion.any());

    MvcTestResult result = delete(admin, null);

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("No local Pokémon with id " + ID.value() + ".");
  }

  @Test
  void should_return409_when_deletingWithAStaleEtag() {
    willThrow(new StaleLocalPokemonException(ID))
        .given(deleteLocalPokemon)
        .delete(ID, ExpectedVersion.exactly(4));

    assertThat(delete(admin, "\"4\"")).hasStatus(HttpStatus.CONFLICT);
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private MvcTestResult post(String json) {
    return mvc.post()
        .uri("/api/v1/local-pokemon")
        .header(HttpHeaders.AUTHORIZATION, ash)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private MvcTestResult put(String ifMatch, String json) {
    var request =
        mvc.put()
            .uri("/api/v1/local-pokemon/{id}", ID.value())
            .header(HttpHeaders.AUTHORIZATION, ash)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json);
    if (ifMatch != null) {
      request.header(HttpHeaders.IF_MATCH, ifMatch);
    }
    return request.exchange();
  }

  private MvcTestResult patch(MediaType contentType, String ifMatch, String body) {
    var request =
        mvc.patch()
            .uri("/api/v1/local-pokemon/{id}", ID.value())
            .header(HttpHeaders.AUTHORIZATION, ash)
            .contentType(contentType)
            .content(body);
    if (ifMatch != null) {
      request.header(HttpHeaders.IF_MATCH, ifMatch);
    }
    return request.exchange();
  }

  private MvcTestResult delete(String bearer, String ifMatch) {
    var request =
        mvc.delete()
            .uri("/api/v1/local-pokemon/{id}", ID.value())
            .header(HttpHeaders.AUTHORIZATION, bearer);
    if (ifMatch != null) {
      request.header(HttpHeaders.IF_MATCH, ifMatch);
    }
    return request.exchange();
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
