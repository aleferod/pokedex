package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.BaseStats;
import com.ballastlane.pokedex.domain.pokemon.Height;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.Weight;

/**
 * DTO + Mapper between the nested {@link LocalPokemon} aggregate and the flat {@link
 * LocalPokemonJpaEntity}. Hand-written on purpose: MapStruct's value is matching flat field names,
 * which does not apply to nested records, so the explicit version is shorter and clearer. Rows are
 * rebuilt through the value objects, so invalid stored data fails loudly instead of leaking.
 */
final class LocalPokemonPersistenceMapper {

  private LocalPokemonPersistenceMapper() {}

  static LocalPokemonJpaEntity toEntity(LocalPokemon pokemon) {
    LocalPokemonJpaEntity entity = new LocalPokemonJpaEntity();
    entity.setId(pokemon.id().value());
    entity.setPokeapiId(pokemon.pokeapiId().value());
    UpstreamData upstream = pokemon.upstream();
    entity.setName(upstream.name());
    entity.setSpriteUrl(upstream.spriteUrl());
    entity.setImageUrl(upstream.imageUrl());
    entity.setCategory(upstream.category());
    entity.setTypes(upstream.types());
    entity.setHeightDm(upstream.height().decimetres());
    entity.setWeightHg(upstream.weight().hectograms());
    entity.setAbilities(upstream.abilities());
    entity.setHp(upstream.stats().hp());
    entity.setAttack(upstream.stats().attack());
    entity.setDefense(upstream.stats().defense());
    entity.setSpecialAttack(upstream.stats().specialAttack());
    entity.setSpecialDefense(upstream.stats().specialDefense());
    entity.setSpeed(upstream.stats().speed());
    ProprietaryData proprietary = pokemon.proprietary();
    entity.setLocalizedNames(proprietary.localizedNames().names());
    entity.setRegion(proprietary.region());
    entity.setHabitatNotes(proprietary.habitatNotes());
    entity.setInternalTags(proprietary.internalTags().tags());
    entity.setNotes(proprietary.notes());
    AuditInfo audit = pokemon.audit();
    entity.setCreatedAt(audit.createdAt());
    entity.setUpdatedAt(audit.updatedAt());
    entity.setSyncedAt(audit.syncedAt());
    entity.setCreatedBy(audit.createdBy());
    entity.setVersion(pokemon.version());
    return entity;
  }

  static LocalPokemon toDomain(LocalPokemonJpaEntity entity) {
    return new LocalPokemon(
        new LocalPokemonId(entity.getId()),
        new PokemonId(entity.getPokeapiId()),
        new UpstreamData(
            entity.getName(),
            entity.getSpriteUrl(),
            entity.getImageUrl(),
            entity.getCategory(),
            entity.getTypes(),
            Height.ofDecimetres(entity.getHeightDm()),
            Weight.ofHectograms(entity.getWeightHg()),
            entity.getAbilities(),
            new BaseStats(
                entity.getHp(),
                entity.getAttack(),
                entity.getDefense(),
                entity.getSpecialAttack(),
                entity.getSpecialDefense(),
                entity.getSpeed())),
        new ProprietaryData(
            LocalizedNames.of(entity.getLocalizedNames()),
            entity.getRegion(),
            entity.getHabitatNotes(),
            InternalTags.of(entity.getInternalTags()),
            entity.getNotes()),
        new AuditInfo(
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getSyncedAt(),
            entity.getCreatedBy()),
        entity.getVersion());
  }
}
