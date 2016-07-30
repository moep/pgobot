package lol.moep.pgobot.model;

import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by moep on 28.07.16.
 */
public class StatsCounter {
    private final static DecimalFormat distanceInKmFormat = initDecimalFormat();

    private static DecimalFormat initDecimalFormat() {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        return df;
    }

    private final Map<PokemonFamilyIdOuterClass.PokemonFamilyId, Integer> numCandies;
    private long metersTraveled;
    private int xp;
    List<CatchablePokemon> caughtPokemon;

    public StatsCounter() {
        this.numCandies = new HashMap<>();
        this.metersTraveled = 0;
        this.xp = 0;
        this.caughtPokemon = new LinkedList<>();
    }

    public void reset() {
        numCandies.clear();
        this.metersTraveled = 0;
        this.xp = 0;
        this.caughtPokemon.clear();
    }

    public void addCandies(PokemonFamilyIdOuterClass.PokemonFamilyId familyId, int[] candies) {
        int sum = this.numCandies.get(familyId) == null ? 0 : this.numCandies.get(familyId);
//        int sum = 0;
        Integer numCandies = this.numCandies.get(familyId);

        for (int amount : candies) {
            sum += amount;
        }

        this.numCandies.put(familyId, sum);
    }

    public Integer getNumCandies(PokemonFamilyIdOuterClass.PokemonFamilyId familyId) {
        return this.numCandies.get(familyId) == null ? 0 : this.numCandies.get(familyId);
    }

    private void printNumCandies() {
        for (PokemonFamilyIdOuterClass.PokemonFamilyId fid : this.numCandies.keySet()) {
            System.out.println(PokemonFamilyIdOuterClass.PokemonFamilyId.forNumber(fid.getNumber()) + ": " + getNumCandies(fid));
        }
    }

    public void addMetersTraveled(double distance) {
        this.metersTraveled += Math.round(distance);
    }

    public long getMetersTraveled() {
        return this.metersTraveled;
    }

    public void addXp(int... xp) {
        for (int val : xp) {
            this.xp += val;
        }
    }

    public int getXp() {
        return this.xp;
    }

    public void addCaughtPokemon(CatchablePokemon pokemon) {
        this.caughtPokemon.add(pokemon);
    }

    public List<CatchablePokemon> getCaughtPokemon() {
        return this.caughtPokemon;
    }

    private void printCaughtPokemon() {
        for (CatchablePokemon p : this.caughtPokemon) {
            // TODO cast to pokemon?
            System.out.println(Dictionary.getNameFromPokemonId(p.getPokemonId()));
        }
    }

    public String getMetersTraveledAsString() {
        if (this.getMetersTraveled() < 1000) {
            return this.metersTraveled + "m";
        } else {
            return StatsCounter.distanceInKmFormat.format(this.metersTraveled / 1000.0d).concat("km");
        }
    }

    public void print() {
        System.out.println("Zurückgelegte Strecke: " + this.getMetersTraveledAsString());
        System.out.println("XP: " + this.getXp());
        System.out.println("Gefangene Pokemon: ");
        this.printCaughtPokemon();
    }
}
