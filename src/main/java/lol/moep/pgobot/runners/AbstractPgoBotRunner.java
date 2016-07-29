package lol.moep.pgobot.runners;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Inventory.Item.ItemAwardOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.Dictionary;
import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.Haversine;
import lol.moep.pgobot.model.StatsCounter;

import java.util.*;
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
        WALK(3600),
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

    protected void moveTo(GeoCoordinate targetPosition, int sleepMillisPer10Meters) throws LoginFailedException, RemoteServerException {
        GeoCoordinate currentPosition = new GeoCoordinate(this.go.getLatitude(), this.go.getLongitude());
        double distance = Haversine.getDistanceInMeters(currentPosition, targetPosition);
        System.out.println("=== Strecke: " + currentPosition + " -> " + targetPosition + " -- " + (int) distance + "m @ 10m / " + sleepMillisPer10Meters + "ms");

        List<GeoCoordinate> interpolatedCoordinates = getInterpolatedCoordinates(currentPosition, targetPosition);

        int coordinatesVisited = 0;

        for (GeoCoordinate c : interpolatedCoordinates) {
            this.go.setLocation(c.getLat(), c.getLon(), 0);

            ++coordinatesVisited;
            sleep(sleepMillisPer10Meters);
        }

        this.sc.addMetersTraveled(distance);

        if (this.sc.getMetersTraveled() % this.moveActionDistance == 0) {
            onMove();
        }
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

    protected void findAndCatchPokemon() throws LoginFailedException, RemoteServerException {
//        System.out.println("Suche Pokémon");
        List<CatchablePokemon> pokemons = this.go.getMap().getCatchablePokemon();
//        System.out.println("Pokemon in der Nähe: " + pokemons.size());

        // TODO stardust stats
        int xp = 0;

        for (CatchablePokemon p : pokemons) {
            EncounterResult er = p.encounterPokemon();
            System.out.println("Fange " + Dictionary.getNameFromPokemonId(p.getPokemonId()) + " CP: " + er.getWildPokemon().getPokemonData().getCp());

            if (er.wasSuccessful()) {
                CatchResult res = p.catchPokemon();
                xp = AbstractPgoBotRunner.listSum(res.getXpList());
                this.sc.addXp(xp);
                System.out.println("Fangstatus: " + res.getStatus().name() + " ## XP: " + xp + " ## SD: " + listSum(res.getStardustList()));
                this.sc.addCaughtPokemon(p);
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
        List<GeoCoordinate> waypoints = new LinkedList<>();

        int xp = 0;

        // TODO use this subset for the loop
        long numPokestops = pokestops.stream()
                .filter(p -> Haversine.getDistanceInMeters(playerPosition, new GeoCoordinate(p.getLatitude(), p.getLongitude())) <= radius)
                //.filter(p -> p.canLoot())
                .count();

        int numLooted = 0;
        GeoCoordinate wp = null;
        for (Pokestop ps : pokestops) {
            wp = new GeoCoordinate(ps.getLatitude(), ps.getLongitude());
            if (Haversine.getDistanceInMeters(playerPosition, wp) <= radius) {
                if (ps.canLoot(true)) {
                    moveTo(wp);
                    ++numLooted;
                    System.out.println("Loote (" + numLooted + "/" + numPokestops + "): " + ps.getDetails().getName());
                    PokestopLootResult lootResult = ps.loot();
                    xp = lootResult.getExperience();
                    System.out.println(lootResult.getResult().name() + " ## " + xp + "EXP");

                    this.sc.addXp(xp);

                    for (ItemAwardOuterClass.ItemAward ia : lootResult.getItemsAwarded()) {
                        System.out.println("  " + ia.getItemId().name() + "(" + ia.getItemCount() + ")");
                    }
                } else {
                    System.out.println("Ignoriere: " + ps.getDetails().getName());
                }

            }
        }
    }

    protected void tradeInTrashMobs() {
        System.out.println("Verschicke Trashmobs");
        final List<PokemonIdOuterClass.PokemonId> banTypes = Arrays.asList(PokemonIdOuterClass.PokemonId.WEEDLE,
                PokemonIdOuterClass.PokemonId.PIDGEY,
                PokemonIdOuterClass.PokemonId.RATTATA,
                PokemonIdOuterClass.PokemonId.SPEAROW,
                PokemonIdOuterClass.PokemonId.ZUBAT,
                PokemonIdOuterClass.PokemonId.DROWZEE,
                PokemonIdOuterClass.PokemonId.DIGLETT,
                PokemonIdOuterClass.PokemonId.CATERPIE);

        List<Pokemon> pokemons = this.go.getInventories().getPokebank().getPokemons();
        Stream<Pokemon> pokemonStream = pokemons.stream();

        Stream<Pokemon> filtered = pokemonStream.filter(p -> banTypes.contains(p.getPokemonId()))
                .filter(p -> !p.isFavorite());


        // TODO exception handling
        // TODO candy statistics
        filtered.forEach(p -> {
                    System.out.println(Dictionary.getNameFromPokemonId(p.getPokemonId()) + " - " + p.getCp());
                    try {
                        ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result result = p.transferPokemon();
                        System.out.println(result.getValueDescriptor());
                        sleep(100);
                    } catch (LoginFailedException | RemoteServerException e) {
                        System.err.println("Fehler: " + e.getMessage());
                    }
                }
        );
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

}
