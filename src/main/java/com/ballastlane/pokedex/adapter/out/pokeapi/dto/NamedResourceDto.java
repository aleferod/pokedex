package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** PokeAPI {@code NamedAPIResource}: a name plus the URL of the full resource. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedResourceDto(String name, String url) {}
