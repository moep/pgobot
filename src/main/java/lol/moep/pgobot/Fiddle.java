package lol.moep.pgobot;

import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import lol.moep.pgobot.model.StatsCounter;

/**
 * Created by moep on 26.07.16.
 */
public class Fiddle {
    public static void main(String[] args) {
        StatsCounter sc = new StatsCounter();
        sc.addCandies(PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_ABRA, new int[]{100, 0, 0});
        sc.addCandies(PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_RATTATA, new int[]{100, 0, 0});
        sc.printNumCandies();
    }


}
