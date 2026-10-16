package com.ballastlane.pokedex.adapter.in.web.localpokemon;

import com.ballastlane.pokedex.adapter.in.web.error.InvalidRequestException;
import com.ballastlane.pokedex.adapter.in.web.error.PreconditionRequiredException;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemFieldError;
import com.ballastlane.pokedex.application.port.in.localpokemon.ExpectedVersion;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;

/**
 * Reads {@code If-Match} into the version the client last saw. Accepts one strong or weak ETag
 * ({@code "3"}, {@code W/"3"}) or {@code *} (any version); anything else is a 400 naming the
 * header.
 */
final class IfMatch {

  private static final Pattern ETAG = Pattern.compile("(?:W/)?\"(\\d{1,18})\"");

  private IfMatch() {}

  /** For PUT and PATCH: without the header a client could overwrite changes it never saw. */
  static ExpectedVersion required(String header) {
    if (header == null) {
      throw new PreconditionRequiredException(HttpHeaders.IF_MATCH);
    }
    return parse(header);
  }

  /** For DELETE: without the header any version may be removed. */
  static ExpectedVersion optional(String header) {
    return header == null ? ExpectedVersion.any() : parse(header);
  }

  private static ExpectedVersion parse(String header) {
    String value = header.trim();
    if (value.equals("*")) {
      return ExpectedVersion.any();
    }
    Matcher etag = ETAG.matcher(value);
    if (!etag.matches()) {
      throw new InvalidRequestException(
          "The If-Match header is malformed.",
          List.of(
              new ProblemFieldError(
                  HttpHeaders.IF_MATCH, "must be one ETag from this API, such as \"3\", or *")));
    }
    return ExpectedVersion.exactly(Long.parseLong(etag.group(1)));
  }
}
