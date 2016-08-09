package lol.moep.pgobot.runners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.encounter.EncounterResult;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.NoSuchItemException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Inventory.Item.ItemAwardOuterClass;
import POGOProtos.Inventory.Item.ItemIdOuterClass;
import POGOProtos.Networking.Responses.EncounterResponseOuterClass.EncounterResponse.Status;
import lol.moep.pgobot.model.Dictionary;
import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.Haversine;
import lol.moep.pgobot.model.StatsCounter;
import lol.moep.pgobot.util.Actions;
import lol.moep.pgobot.util.MapScanner;

/**
 * Created by moep on 28.07.16.
 */
public abstract class AbstractPgoBotRunner implements PgoBotRunner {
    protected final PokemonGo go;
    protected final StatsCounter sc;
    private GeoCoordinate lastKnownPosition = null;

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

    protected void teleportTo(final GeoCoordinate targetPosition) {
    	this.go.setLocation(targetPosition.getLat(), targetPosition.getLon(), 0);
    	this.lastKnownPosition = targetPosition;
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
        this.sc.logMessage("=== Strecke: " + currentPosition + " -> " + targetPosition + " -- " + (int) distance + "m @ 10m / " + sleepMillisPer10Meters + "ms");

        List<GeoCoordinate> interpolatedCoordinates = getInterpolatedCoordinates(currentPosition, targetPosition);

        int coordinatesVisited = 0;

        for (GeoCoordinate c : interpolatedCoordinates) {

            teleportTo(c);

            ++coordinatesVisited;
            Actions.sleep(sleepMillisPer10Meters);

            if (coordinatesVisited % (this.moveActionDistance / 10) == 0) {
                try {
                    onMove();
                } catch (LoginFailedException | RemoteServerException e) {
                    this.sc.logError(e);
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
            pokemons = MapScanner.getCatchablePokemon(go);
        } catch (LoginFailedException | RemoteServerException e) {
            this.sc.logError(e);
            return;
        }

        // TODO stardust stats
        int xp = 0;

        for (CatchablePokemon p : pokemons) {
            this.sc.logMessage("Fangversuch: " + Dictionary.getNameFromPokemonId(p.getPokemonId()));
            try {
                EncounterResult er = p.encounterPokemon();

                // https://github.com/Grover-c13/PokeGOAPI-Java/issues/406
                if (er.getStatus() == Status.ENCOUNTER_ALREADY_HAPPENED) {
                    this.sc.logMessage("Already happened");
                	continue;
                }

                if (er.wasSuccessful()) {
                    CatchResult res = p.catchPokemon();
                    xp = AbstractPgoBotRunner.listSum(res.getXpList());
                    this.sc.addXp(xp);
                    this.sc.addCaughtPokemon(p);
                    switch (res.getStatus()) {
                        case CATCH_SUCCESS:
                            this.sc.logMessage("Gefangen: " + Dictionary.getNameFromPokemonId(p.getPokemonId()) +
                                    " IV: " + er.getPokemonData().getIndividualAttack() + "/" + er.getPokemonData().getIndividualDefense() + "/" + er.getPokemonData().getIndividualStamina() +
                                    " XP: " + xp + " SD: " + listSum(res.getStardustList()));
                            break;
                        case CATCH_FLEE:
                        case CATCH_ESCAPE:
                        case CATCH_MISSED:
                            this.sc.logMessage("Entkommen: " + Dictionary.getNameFromPokemonId(p.getPokemonId()));
                            break;
                        default:
                        	this.sc.logMessage("Unbekanter Status: " + er.getStatus().name());
                        	break;
                    }
                }
            } catch (LoginFailedException | RemoteServerException | NoSuchItemException e) {
                this.sc.logError(e);
            }

            // TODO more realistic value?
            Actions.sleep(1000);
        }
    }

    /**
     * @param radius Radius in meters
     * @throws LoginFailedException
     * @throws RemoteServerException
     */
    protected void lootAllPokestopsWithinRadius(int radius) throws LoginFailedException, RemoteServerException {
        GeoCoordinate playerPosition = new GeoCoordinate(this.go.getLatitude(), this.go.getLongitude());
        Collection<Pokestop> pokestops = MapScanner.getPokestops(go);

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
                this.sc.logMessage("Loote (" + numLooted + "/" + numPokestops + "): " + ps.getDetails().getName()
                        + " ## " + lootResult.getResult().name() + " ## " + xp + "EXP");

                this.sc.addXp(xp);
//
//                    for (ItemAwardOuterClass.ItemAward ia : lootResult.getItemsAwarded()) {
//                        this.sc.logMessage("  " + ia.getItemId().name() + "(" + ia.getItemCount() + ")");
//                    }

                Map<ItemIdOuterClass.ItemId, Long> collect = lootResult.getItemsAwarded().stream()
                        .collect(Collectors.groupingBy(ItemAwardOuterClass.ItemAward::getItemId, Collectors.counting()));

                for (ItemIdOuterClass.ItemId id : collect.keySet()) {
                    this.sc.logMessage("  " + id.name() + " (" + collect.get(id) + ")");
                }

            } else {
                this.sc.logMessage("Ignoriere: " + ps.getDetails().getName());
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

    private boolean isBreeding() {
        try {
            List<EggIncubator> incubators = this.go.getInventories().getIncubators();
            for (EggIncubator i : incubators) {
                if (i.isInUse()) {
                    return true;
                }
            }
        } catch (LoginFailedException | RemoteServerException e) {
            this.sc.logError(e);
        }

        return false;
    }

    protected void printEggStatus() {
        final Set<EggPokemon> eggs;
        try {
			eggs = this.go.getInventories().getHatchery().getEggs();
			this.sc.logMessage("Eier");
			for (EggPokemon e : eggs) {
				if (e.isIncubate()) {
					this.sc.logMessage(e.getEggKmWalked() + "/" + e.getEggKmWalkedTarget());
				}
			}
		} catch (LoginFailedException | RemoteServerException e) {
			this.sc.logError(e);
		}
    }

    @Override
	public GeoCoordinate getCurrentPosition() {
    	// nicht direkt über das PokemonGo Objekt um Threading Probleme zu vermeiden
		return lastKnownPosition;
	}

}
