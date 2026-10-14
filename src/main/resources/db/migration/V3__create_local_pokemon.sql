-- Local replica of selected Pokémon (add-local-sync, US03).
-- Upstream columns mirror PokeAPI and are replaced by every sync; proprietary columns belong to
-- the business and are never touched by sync. version drives optimistic locking (ETag).
CREATE TABLE local_pokemon (
    id              UUID          NOT NULL,
    pokeapi_id      INTEGER       NOT NULL,
    -- upstream (PokeAPI)
    name            VARCHAR(50)   NOT NULL,
    sprite_url      VARCHAR(500),
    image_url       VARCHAR(500),
    category        VARCHAR(100),
    types           TEXT[]        NOT NULL DEFAULT '{}',
    height_dm       INTEGER       NOT NULL,
    weight_hg       INTEGER       NOT NULL,
    abilities       TEXT[]        NOT NULL DEFAULT '{}',
    hp              INTEGER       NOT NULL,
    attack          INTEGER       NOT NULL,
    defense         INTEGER       NOT NULL,
    special_attack  INTEGER       NOT NULL,
    special_defense INTEGER       NOT NULL,
    speed           INTEGER       NOT NULL,
    -- proprietary
    region          VARCHAR(50),
    habitat_notes   VARCHAR(500),
    internal_tags   TEXT[]        NOT NULL DEFAULT '{}',
    notes           VARCHAR(2000),
    -- audit + locking
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    synced_at       TIMESTAMPTZ   NOT NULL,
    created_by      VARCHAR(30)   NOT NULL,
    version         BIGINT        NOT NULL,
    CONSTRAINT pk_local_pokemon PRIMARY KEY (id),
    CONSTRAINT uk_local_pokemon_pokeapi_id UNIQUE (pokeapi_id),
    CONSTRAINT ck_local_pokemon_pokeapi_id CHECK (pokeapi_id > 0),
    CONSTRAINT ck_local_pokemon_size CHECK (height_dm >= 0 AND weight_hg >= 0),
    CONSTRAINT ck_local_pokemon_stats CHECK (
        hp >= 0 AND attack >= 0 AND defense >= 0
        AND special_attack >= 0 AND special_defense >= 0 AND speed >= 0),
    CONSTRAINT ck_local_pokemon_tag_count CHECK (cardinality(internal_tags) <= 20)
);

-- Tag filtering (US04: ?tag=legendary) uses array containment.
CREATE INDEX ix_local_pokemon_internal_tags ON local_pokemon USING GIN (internal_tags);
CREATE INDEX ix_local_pokemon_name ON local_pokemon (name);

CREATE TABLE local_pokemon_localized_name (
    local_pokemon_id UUID         NOT NULL,
    locale           VARCHAR(35)  NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_local_pokemon_localized_name PRIMARY KEY (local_pokemon_id, locale),
    CONSTRAINT fk_local_pokemon_localized_name_pokemon
        FOREIGN KEY (local_pokemon_id) REFERENCES local_pokemon (id) ON DELETE CASCADE
);

COMMENT ON TABLE local_pokemon IS 'Locally owned copies of Pokémon: PokeAPI snapshot + proprietary fields.';
COMMENT ON TABLE local_pokemon_localized_name IS 'Proprietary: the Pokémon''s name per locale (e.g. pt-BR).';
