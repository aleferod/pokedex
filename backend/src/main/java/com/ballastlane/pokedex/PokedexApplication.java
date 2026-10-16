package com.ballastlane.pokedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Pokédex API. Component scanning starts here and covers every layer. */
@SpringBootApplication
public class PokedexApplication {

  public static void main(String[] args) {
    SpringApplication.run(PokedexApplication.class, args);
  }
}
