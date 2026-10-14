package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA row of {@code local_pokemon}: the aggregate flattened. Value lists are PostgreSQL arrays;
 * localized names live in a child table. {@code version} is {@code null} for new rows so Spring
 * Data persists (rather than merges) them.
 */
@Entity
@Table(name = "local_pokemon")
public class LocalPokemonJpaEntity {

  @Id private UUID id;

  @Column(name = "pokeapi_id", nullable = false)
  private int pokeapiId;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "sprite_url", length = 500)
  private String spriteUrl;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(length = 100)
  private String category;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(nullable = false, columnDefinition = "text[]")
  private List<String> types = new ArrayList<>();

  @Column(name = "height_dm", nullable = false)
  private int heightDm;

  @Column(name = "weight_hg", nullable = false)
  private int weightHg;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(nullable = false, columnDefinition = "text[]")
  private List<String> abilities = new ArrayList<>();

  @Column(nullable = false)
  private int hp;

  @Column(nullable = false)
  private int attack;

  @Column(nullable = false)
  private int defense;

  @Column(name = "special_attack", nullable = false)
  private int specialAttack;

  @Column(name = "special_defense", nullable = false)
  private int specialDefense;

  @Column(nullable = false)
  private int speed;

  // Batched: a listing page loads every record's names in one extra query, not one per record.
  @ElementCollection(fetch = FetchType.EAGER)
  @BatchSize(size = 100)
  @CollectionTable(
      name = "local_pokemon_localized_name",
      joinColumns = @JoinColumn(name = "local_pokemon_id"))
  @MapKeyColumn(name = "locale", length = 35)
  @Column(name = "name", nullable = false, length = 50)
  private Map<String, String> localizedNames = new HashMap<>();

  @Column(length = 50)
  private String region;

  @Column(name = "habitat_notes", length = 500)
  private String habitatNotes;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "internal_tags", nullable = false, columnDefinition = "text[]")
  private List<String> internalTags = new ArrayList<>();

  @Column(length = 2000)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  @Column(name = "created_by", nullable = false, length = 30)
  private String createdBy;

  @Version private Long version;

  // Getters and setters below are used by the hand-written LocalPokemonPersistenceMapper.

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public int getPokeapiId() {
    return pokeapiId;
  }

  public void setPokeapiId(int pokeapiId) {
    this.pokeapiId = pokeapiId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpriteUrl() {
    return spriteUrl;
  }

  public void setSpriteUrl(String spriteUrl) {
    this.spriteUrl = spriteUrl;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public List<String> getTypes() {
    return types;
  }

  public void setTypes(List<String> types) {
    this.types = new ArrayList<>(types);
  }

  public int getHeightDm() {
    return heightDm;
  }

  public void setHeightDm(int heightDm) {
    this.heightDm = heightDm;
  }

  public int getWeightHg() {
    return weightHg;
  }

  public void setWeightHg(int weightHg) {
    this.weightHg = weightHg;
  }

  public List<String> getAbilities() {
    return abilities;
  }

  public void setAbilities(List<String> abilities) {
    this.abilities = new ArrayList<>(abilities);
  }

  public int getHp() {
    return hp;
  }

  public void setHp(int hp) {
    this.hp = hp;
  }

  public int getAttack() {
    return attack;
  }

  public void setAttack(int attack) {
    this.attack = attack;
  }

  public int getDefense() {
    return defense;
  }

  public void setDefense(int defense) {
    this.defense = defense;
  }

  public int getSpecialAttack() {
    return specialAttack;
  }

  public void setSpecialAttack(int specialAttack) {
    this.specialAttack = specialAttack;
  }

  public int getSpecialDefense() {
    return specialDefense;
  }

  public void setSpecialDefense(int specialDefense) {
    this.specialDefense = specialDefense;
  }

  public int getSpeed() {
    return speed;
  }

  public void setSpeed(int speed) {
    this.speed = speed;
  }

  public Map<String, String> getLocalizedNames() {
    return localizedNames;
  }

  public void setLocalizedNames(Map<String, String> localizedNames) {
    this.localizedNames = new HashMap<>(localizedNames);
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getHabitatNotes() {
    return habitatNotes;
  }

  public void setHabitatNotes(String habitatNotes) {
    this.habitatNotes = habitatNotes;
  }

  public List<String> getInternalTags() {
    return internalTags;
  }

  public void setInternalTags(List<String> internalTags) {
    this.internalTags = new ArrayList<>(internalTags);
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getSyncedAt() {
    return syncedAt;
  }

  public void setSyncedAt(Instant syncedAt) {
    this.syncedAt = syncedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
