package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.ReadOnlyMembers;
import com.ballastlane.pokedex.adapter.in.web.error.InvalidRequestException;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemFieldError;
import com.ballastlane.pokedex.domain.localpokemon.FieldPatch;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNamesPatch;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryPatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Turns a JSON Merge Patch (RFC 7396) body into a {@link ProprietaryPatch}. Reading the tree,
 * rather than binding a record, is what tells an absent member (keep) from {@code null} (clear),
 * and lets every problem be reported at once: read-only, unknown and wrongly typed members.
 */
final class MergePatchReader {

  static final String NOT_ACCEPTED = "is not an accepted field";
  static final String WRONG_TYPE = "has the wrong type";

  private MergePatchReader() {}

  static ProprietaryPatch read(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new InvalidRequestException(
          "A merge patch must be a JSON object.",
          List.of(new ProblemFieldError("body", "must be a JSON object")));
    }
    List<ProblemFieldError> errors = new ArrayList<>();
    LocalizedNamesPatch localizedNames = LocalizedNamesPatch.keep();
    FieldPatch<String> region = FieldPatch.keep();
    FieldPatch<String> habitatNotes = FieldPatch.keep();
    FieldPatch<List<String>> internalTags = FieldPatch.keep();
    FieldPatch<String> notes = FieldPatch.keep();
    for (Map.Entry<String, JsonNode> member : body.properties()) {
      String name = member.getKey();
      JsonNode value = member.getValue();
      switch (name) {
        case "localizedNames" -> localizedNames = localizedNames(value, errors);
        case "region" -> region = text(name, value, errors);
        case "habitatNotes" -> habitatNotes = text(name, value, errors);
        case "internalTags" -> internalTags = tags(value, errors);
        case "notes" -> notes = text(name, value, errors);
        default ->
            errors.add(
                new ProblemFieldError(
                    name,
                    ReadOnlyMembers.reason(ReplaceLocalPokemonRequest.class, name)
                        .orElse(NOT_ACCEPTED)));
      }
    }
    if (!errors.isEmpty()) {
      throw new InvalidRequestException("The merge patch has invalid members.", errors);
    }
    return new ProprietaryPatch(localizedNames, region, habitatNotes, internalTags, notes);
  }

  private static FieldPatch<String> text(
      String name, JsonNode value, List<ProblemFieldError> errors) {
    if (value.isNull()) {
      return FieldPatch.clear();
    }
    if (!value.isString()) {
      errors.add(new ProblemFieldError(name, WRONG_TYPE));
      return FieldPatch.keep();
    }
    return FieldPatch.set(value.stringValue());
  }

  private static FieldPatch<List<String>> tags(JsonNode value, List<ProblemFieldError> errors) {
    if (value.isNull()) {
      return FieldPatch.clear();
    }
    if (!value.isArray()) {
      errors.add(new ProblemFieldError("internalTags", WRONG_TYPE));
      return FieldPatch.keep();
    }
    List<String> tags = new ArrayList<>();
    for (int i = 0; i < value.size(); i++) {
      JsonNode tag = value.get(i);
      if (tag.isString()) {
        tags.add(tag.stringValue());
      } else {
        errors.add(new ProblemFieldError("internalTags[" + i + "]", WRONG_TYPE));
      }
    }
    return FieldPatch.set(tags);
  }

  /** Merged per locale: a string adds or replaces a name, {@code null} removes it. */
  private static LocalizedNamesPatch localizedNames(
      JsonNode value, List<ProblemFieldError> errors) {
    if (value.isNull()) {
      return LocalizedNamesPatch.clear();
    }
    if (!value.isObject()) {
      errors.add(new ProblemFieldError("localizedNames", WRONG_TYPE));
      return LocalizedNamesPatch.keep();
    }
    Map<String, String> put = new HashMap<>();
    Set<String> remove = new HashSet<>();
    for (Map.Entry<String, JsonNode> entry : value.properties()) {
      JsonNode name = entry.getValue();
      if (name.isNull()) {
        remove.add(entry.getKey());
      } else if (name.isString()) {
        put.put(entry.getKey(), name.stringValue());
      } else {
        errors.add(new ProblemFieldError("localizedNames." + entry.getKey(), WRONG_TYPE));
      }
    }
    return LocalizedNamesPatch.merge(put, remove);
  }
}
