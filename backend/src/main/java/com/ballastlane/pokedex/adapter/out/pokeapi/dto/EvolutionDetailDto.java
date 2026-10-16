package com.ballastlane.pokedex.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One way an evolution can happen ({@code evolution_details[]}): a trigger plus optional
 * conditions. Only the conditions the service describes are bound.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionDetailDto(
    NamedResourceDto trigger,
    @JsonProperty("min_level") Integer minLevel,
    NamedResourceDto item,
    @JsonProperty("held_item") NamedResourceDto heldItem,
    @JsonProperty("min_happiness") Integer minHappiness,
    @JsonProperty("min_affection") Integer minAffection,
    @JsonProperty("time_of_day") String timeOfDay,
    @JsonProperty("known_move_type") NamedResourceDto knownMoveType,
    @JsonProperty("known_move") NamedResourceDto knownMove,
    NamedResourceDto location,
    @JsonProperty("trade_species") NamedResourceDto tradeSpecies) {}
