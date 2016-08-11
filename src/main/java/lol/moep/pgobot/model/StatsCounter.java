package lol.moep.pgobot.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import lol.moep.pgobot.util.PoGoLogger;

/**
 * Created by moep on 28.07.16.
 */
public class StatsCounter {
    private final static DecimalFormat distanceInKmFormat = initDecimalFormat();
    private static final PoGoLogger LOGGER = PoGoLogger.getInstance();

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
        // Map<ItemIdOuterClass.ItemId, Long> collect = lootResult.getItemsAwarded().stream()
        // .collect(Collectors.groupingBy(ItemAwardOuterClass.ItemAward::getItemId, Collectors.counting()));

        Map<String, Long> names = this.caughtPokemon.stream()
                .map(p -> Dictionary.getNameFromPokemonId(p.getPokemonId()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (String name : names.keySet()) {
            // TODO cast to pokemon?
        	LOGGER.logMessage(name + " (" + names.get(name) + ")");
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
    	LOGGER.logMessage("Zurückgelegte Strecke: " + this.getMetersTraveledAsString());
    	LOGGER.logMessage("XP: " + this.getXp());
    	LOGGER.logMessage("Gefangene Pokemon: ");
        printCaughtPokemon();
    }
}
