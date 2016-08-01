package lol.moep.pgobot.runners;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Inventory.Item.ItemAwardOuterClass;
import POGOProtos.Inventory.Item.ItemIdOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.Dictionary;
import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.Haversine;
import lol.moep.pgobot.model.StatsCounter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by moep on 28.07.16.
 */
public abstract class AbstractPgoBotRunner implements PgoBotRunner {
    protected final PokemonGo go;
    protected final StatsCounter sc;

    /**
     * Move distance in meters after which the onMove function is triggered. Must be a multiple of 10.
     */
    private final int moveActionDistance;

    protected enum SpeedWaitTime {
        WALK(2400),
        DRIVE(500);

        private final int val;

        private SpeedWaitTime(int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }
    }

    AbstractPgoBotRunner(PokemonGo go) {
        this.go = go;
        sc = new StatsCounter();

        this.moveActionDistance = 50;
    }

    @Override
    public StatsCounter getStatistics() {
        return this.sc;
    }

    private static List<GeoCoordinate> getInterpolatedCoordinates(GeoCoordinate c1, GeoCoordinate c2) {
        long distanceMeters = Math.round(Haversine.getDistanceInMeters(c1, c2));
        int amount = (int) (distanceMeters / 10);

        List<GeoCoordinate> ret = new ArrayList<>(amount + 2);

        ret.add(c1);

        double dLat = c2.getLat() - c1.getLat();
        double dLon = c2.getLon() - c1.getLon();

        double lat;
        double lon;
        for (double i = 0; i < amount; i++) {
            lat = c1.getLat() + dLat * (i / amount);
            lon = c1.getLon() + dLon * (i / amount);

            ret.add(new GeoCoordinate(lat, lon));
        }

        ret.add(c2);

        return ret;
    }

    private static void sleep(int timeMilis) {
        try {
            Thread.sleep(timeMilis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void walkTo(GeoCoordinate targetPosition) throws LoginFailedException, RemoteServerException {
        moveTo(targetPosition, SpeedWaitTime.WALK.getVal());
    }

    protected void driveTo(GeoCoordinate targetPosition) throws LoginFailedException, RemoteServerException {
        moveTo(targetPosition, SpeedWaitTime.DRIVE.getVal());
    }

    protected void moveTo(GeoCoordinate targetPosition) throws LoginFailedException, RemoteServerException {
        int sleepMillis;
        if (isBreeding()) {
            sleepMillis = SpeedWaitTime.WALK.getVal();
        } else {
            sleepMillis = SpeedWaitTime.DRIVE.getVal();
        }

        moveTo(targetPosition, sleepMillis);
    }

    protected void moveTo(GeoCoordinate targetPosition, int sleepMillisPer10Meters) {
        GeoCoordinate currentPosition = new GeoCoordinate(this.go.getLatitude(), this.go.getLongitude());
        double distance = Haversine.getDistanceInMeters(currentPosition, targetPosition);
        System.out.println("=== Strecke: " + currentPosition + " -> " + targetPosition + " -- " + (int) distance + "m @ 10m / " + sleepMillisPer10Meters + "ms");

        List<GeoCoordinate> interpolatedCoordinates = getInterpolatedCoordinates(currentPosition, targetPosition);

        int coordinatesVisited = 0;

        for (GeoCoordinate c : interpolatedCoordinates) {

            this.go.setLocation(c.getLat(), c.getLon(), 0);

            ++coordinatesVisited;
            sleep(sleepMillisPer10Meters);

            if (coordinatesVisited % (this.moveActionDistance / 10) == 0) {
                try {
                    onMove();
                } catch (LoginFailedException | RemoteServerException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        this.sc.addMetersTraveled(distance);

    }

    protected void onMove() throws LoginFailedException, RemoteServerException {
        findAndCatchPokemon();
    }

    private static int listSum(List<? extends Number> numbers) {
        int sum = 0;

        for (Number n : numbers) {
            sum += n.intValue();
        }

        return sum;
    }

    protected void findAndCatchPokemon() {
        List<CatchablePokemon> pokemons = null;
        try {
            pokemons = this.go.getMap().getCatchablePokemon();
        } catch (LoginFailedException | RemoteServerException e) {
            System.err.println("Serverfehler: Pokémon in der Nähe konnten nicht ermittelt werden.");
            return;
        }

        // TODO stardust stats
        int xp = 0;

        for (CatchablePokemon p : pokemons) {
            try {
                EncounterResult er = p.encounterPokemon();

                if (er.wasSuccessful()) {
                    CatchResult res = p.catchPokemon();
                    xp = AbstractPgoBotRunner.listSum(res.getXpList());
                    this.sc.addXp(xp);
                    this.sc.addCaughtPokemon(p);
                    switch (er.getStatus()) {
                        case ENCOUNTER_SUCCESS:
                            System.out.println("Gefangen: " + Dictionary.getNameFromPokemonId(p.getPokemonId()) + " (CP: " + er.getWildPokemon().getPokemonData().getCp() + ") ## XP: " +
                                    xp + " SD: " + listSum(res.getStardustList()));
                            break;
                        case ENCOUNTER_POKEMON_FLED:
                            System.out.println("Entkommen: " + Dictionary.getNameFromPokemonId(p.getPokemonId()) + " (CP: " + er.getWildPokemon().getPokemonData().getCp() + ")");
                            break;
                        default:
                    }
                }
            } catch (LoginFailedException | RemoteServerException e) {
                System.err.println("Serverfehler beim Fangen von " + p.getPokemonId().name());
            }

            // TODO more realistic value?
            sleep(1000);
        }
    }

    /**
     * @param radius Radius in meters
     * @throws LoginFailedException
     * @throws RemoteServerException
     */
    protected void lootAllPokestopsWithinRadius(int radius) throws LoginFailedException, RemoteServerException {
        GeoCoordinate playerPosition = new GeoCoordinate(this.go.getLatitude(), this.go.getLongitude());
        Collection<Pokestop> pokestops = this.go.getMap().getMapObjects().getPokestops();

        List<Pokestop> sortedPokestops = getSortedPokestops(playerPosition, pokestops, radius);
        int xp = 0;

        // TODO use this subset for the loop
        long numPokestops = pokestops.stream()
                .filter(p -> Haversine.getDistanceInMeters(playerPosition, new GeoCoordinate(p.getLatitude(), p.getLongitude())) <= radius)
                .count();

        int numLooted = 0;
        GeoCoordinate wp = null;
        for (Pokestop ps : sortedPokestops) {
            wp = new GeoCoordinate(ps.getLatitude(), ps.getLongitude());
            if (ps.canLoot(true)) {
                moveTo(wp);
                ++numLooted;
                PokestopLootResult lootResult = ps.loot();
                xp = lootResult.getExperience();
                System.out.println("Loote (" + numLooted + "/" + numPokestops + "): " + ps.getDetails().getName()
                        + " ## " + lootResult.getResult().name() + " ## " + xp + "EXP");

                this.sc.addXp(xp);
//
//                    for (ItemAwardOuterClass.ItemAward ia : lootResult.getItemsAwarded()) {
//                        System.out.println("  " + ia.getItemId().name() + "(" + ia.getItemCount() + ")");
//                    }

                Map<ItemIdOuterClass.ItemId, Long> collect = lootResult.getItemsAwarded().stream()
                        .collect(Collectors.groupingBy(ItemAwardOuterClass.ItemAward::getItemId, Collectors.counting()));

                for (ItemIdOuterClass.ItemId id : collect.keySet()) {
                    System.out.println("  " + id.name() + " (" + collect.get(id) + ")");
                }

            } else {
                System.out.println("Ignoriere: " + ps.getDetails().getName());
            }
        }
    }

    private List<Pokestop> getSortedPokestops(GeoCoordinate playerPosition, Collection<Pokestop> pokestops, int radius) {
        List<Pokestop> stopsInRange = pokestops.stream()
                .filter(p -> Haversine.getDistanceInMeters(playerPosition, new GeoCoordinate(p.getLatitude(), p.getLongitude())) <= radius)
                .collect(Collectors.toList());

        List<Pokestop> sorted = new ArrayList<>(stopsInRange.size());

        GeoCoordinate position = playerPosition;
        GeoCoordinate position2;
        Pokestop minStop;
        double minDistance = Double.MAX_VALUE;
        double distance;
        for (Pokestop start : stopsInRange) {
            minStop = start;

            for (Pokestop dest : stopsInRange) {
                if (sorted.contains(dest) || start.equals(dest)) {
                    continue;
                }

                position2 = new GeoCoordinate(dest.getLatitude(), dest.getLongitude());
                distance = Haversine.getDistanceInMeters(position, position2);
                if (distance < minDistance) {
                    minStop = dest;
                    minDistance = distance;
                }
            }

            sorted.add(minStop);
        }

        return sorted;
    }

    protected void tradeInTrashMobs() {
        System.out.println("Verschicke Trashmobs");
        final List<PokemonIdOuterClass.PokemonId> removeAllTypes = Arrays.asList(
                PokemonIdOuterClass.PokemonId.WEEDLE,
                PokemonIdOuterClass.PokemonId.PIDGEY,
                PokemonIdOuterClass.PokemonId.RATTATA,
                PokemonIdOuterClass.PokemonId.SPEAROW,
                PokemonIdOuterClass.PokemonId.ZUBAT,
                PokemonIdOuterClass.PokemonId.DROWZEE,
                PokemonIdOuterClass.PokemonId.DIGLETT,
                PokemonIdOuterClass.PokemonId.CATERPIE,
                PokemonIdOuterClass.PokemonId.JYNX,
                PokemonIdOuterClass.PokemonId.EEVEE
        );

        final List<PokemonIdOuterClass.PokemonId> removeDuplicateTypes = Arrays.asList(
                PokemonIdOuterClass.PokemonId.PIDGEOTTO,
                PokemonIdOuterClass.PokemonId.NIDORAN_FEMALE,
                PokemonIdOuterClass.PokemonId.NIDORAN_MALE,
                PokemonIdOuterClass.PokemonId.ODDISH,
                PokemonIdOuterClass.PokemonId.PARAS,
                PokemonIdOuterClass.PokemonId.VENONAT,
                PokemonIdOuterClass.PokemonId.POLIWAG,
                PokemonIdOuterClass.PokemonId.SEEL,
                PokemonIdOuterClass.PokemonId.SHELLDER,
                PokemonIdOuterClass.PokemonId.GASTLY,
                PokemonIdOuterClass.PokemonId.KRABBY,
                PokemonIdOuterClass.PokemonId.HORSEA,
                PokemonIdOuterClass.PokemonId.STARYU,
                PokemonIdOuterClass.PokemonId.GOLDEEN,
                PokemonIdOuterClass.PokemonId.SCYTHER,
                PokemonIdOuterClass.PokemonId.MAGIKARP
        );

        List<Pokemon> pokemons = this.go.getInventories().getPokebank().getPokemons();

        Stream<Pokemon> filtered = pokemons.stream()
                .filter(p -> removeAllTypes.contains(p.getPokemonId()))
                .filter(p -> !p.isFavorite());


        // TODO exception handling
        filtered.forEach(p -> {
                    System.out.println(Dictionary.getNameFromPokemonId(p.getPokemonId()) + " - " + p.getCp());
                    try {
                        ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result result = p.transferPokemon();
                        System.out.println(result.getValueDescriptor());
                        sleep(500);
                    } catch (LoginFailedException | RemoteServerException e) {
                        System.err.println("Fehler: " + e.getMessage());
                    }
                }
        );

        Map<PokemonIdOuterClass.PokemonId, List<Pokemon>> filtered2 = pokemons.stream()
                .filter(p -> removeDuplicateTypes.contains(p.getPokemonId()))
                .filter(p -> !p.isFavorite())
                .collect(Collectors.groupingBy(Pokemon::getPokemonId));
        
        // TODO exception handling
        for(PokemonIdOuterClass.PokemonId id : filtered2.keySet()) {
            System.out.println("ID: " + Dictionary.getNameFromPokemonId(id));
            filtered2.get(id).stream()
                    .sorted(Comparator.comparing(Pokemon::getCp).reversed())
                    .skip(1)
                    .forEach(p -> {
                        System.out.println("  " + p.getCp());
                        try {
                            p.transferPokemon();
                        } catch (LoginFailedException | RemoteServerException e) {
                            System.err.println("Fehler: " + e.getMessage());
                        }
                        sleep(500);
                    });

        }


    }

    private boolean isBreeding() {
        List<EggIncubator> incubators = this.go.getInventories().getIncubators();
        for (EggIncubator i : incubators) {
            if (i.isInUse()) {
                return true;
            }
        }

        return false;
    }

    protected void printEggStatus() {
        Set<EggPokemon> eggs = this.go.getInventories().getHatchery().getEggs();
        System.out.println("Eier");
        for (EggPokemon e : eggs) {
            if (e.isIncubate()) {
                System.out.println(e.getEggKmWalked() + "/" + e.getEggKmWalkedTarget());
            }
        }
    }

}
