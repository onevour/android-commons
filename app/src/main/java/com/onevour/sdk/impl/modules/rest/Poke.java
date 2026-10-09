package com.onevour.sdk.impl.modules.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** The parts of PokéAPI (pokeapi.co) the REST sample reads: only @Expose fields are parsed. */
public final class Poke {

    private Poke() {
    }

    public static class Named {
        @Expose public String name;
        @Expose public String url;
    }

    public static class Pokemon {
        @Expose public int id;
        @Expose public String name;
        @Expose public int height;
        @Expose public int weight;
        @Expose public List<TypeSlot> types;
        @Expose public Sprites sprites;
    }

    public static class TypeSlot {
        @Expose public int slot;
        @Expose public Named type;
    }

    public static class Sprites {
        @Expose @SerializedName("front_default") public String frontDefault;
    }

    public static class Species {
        @Expose public String name;
        @Expose public Named color;
        @Expose public Named generation;
        @Expose @SerializedName("flavor_text_entries") public List<FlavorText> flavorTexts;
    }

    public static class FlavorText {
        @Expose @SerializedName("flavor_text") public String text;
        @Expose public Named language;
    }

    public static class Page {
        @Expose public int count;
        @Expose public String next;
        @Expose public List<Named> results;
    }

    public static class Type {
        @Expose public String name;
        @Expose public List<TypePokemon> pokemon;
    }

    public static class TypePokemon {
        @Expose public Named pokemon;
    }
}
