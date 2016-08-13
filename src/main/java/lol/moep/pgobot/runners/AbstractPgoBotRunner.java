package lol.moep.pgobot.runners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import POGOProtos.Enums.PokemonIdOuterClass;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.encounter.EncounterResult;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
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
import lol.moep.pgobot.util.logger.Logger;
import lol.moep.pgobot.util.logger.LoggerFactory;

/**
 * Created by moep on 28.07.16.
 */
public abstract class AbstractPgoBotRunner implements PgoBotRunner {
	
	private static final Logger LOGGER = LoggerFactory.getLoggerInstance();
	
    protected final PokemonGo go;
    protected final StatsCounter sc;
    private GeoCoordinate lastKnownPosition = null;
    private boolean isOnMove = false;

    /**
     * Distance in meters between location updates
     */
    private final int metersPerMove;

    /**
     * Move distance in meters after which the onMove function is triggered. Must be a multiple of metersPerMove.
     */
    private final int moveActionDistance;

    protected enum MovementSpeed {
        WALK(2.0),
        DRIVE(20.0d);

        private final double val;

        private MovementSpeed(double val) {
            this.val = val;
        }

        public double getVal() {
            return this.val;
        }
    }

    AbstractPgoBotRunner(PokemonGo go) {
        this.go = go;
        sc = new StatsCounter();

        this.metersPerMove = 10;
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

    private long getSleepMillis(MovementSpeed speed) {
        return Math.round((this.metersPerMove / speed.getVal()) * 1000);
    }

    protected void walkTo(GeoCoordinate targetPosition) throws LoginFailedException, RemoteServerException {
        moveTo(targetPosition, MovementSpeed.WALK);
    }

    protected void driveTo(GeoCoordinate targetPosition) throws LoginFailedException, RemoteServerException {
        moveTo(targetPosition, MovementSpeed.DRIVE);
    }

    protected void moveTo(GeoCoordinate targetPosition) throws LoginFailedException, RemoteServerException {
        MovementSpeed speed;
        if (isBreeding()) {
            speed = MovementSpeed.WALK;
        } else {
            speed = MovementSpeed.DRIVE;
        }

        moveTo(targetPosition, speed);
    }

    protected void moveTo(GeoCoordinate targetPosition, MovementSpeed speed) {
        GeoCoordinate currentPosition = new GeoCoordinate(this.go.getLatitude(), this.go.getLongitude());
        double distance = Haversine.getDistanceInMeters(currentPosition, targetPosition);
        LOGGER.yellow("=== Strecke: " + currentPosition + " -> " + targetPosition + " -- " + (int) distance + "m @ " + speed.getVal() + "m/s");

        List<GeoCoordinate> interpolatedCoordinates = getInterpolatedCoordinates(currentPosition, targetPosition);

        int coordinatesVisited = 0;

        for (GeoCoordinate c : interpolatedCoordinates) {

            teleportTo(c);

            ++coordinatesVisited;
            Actions.sleep(getSleepMillis(speed));

            if (coordinatesVisited % (this.moveActionDistance / 10) == 0) {
				if (!isOnMove) {
					try {
						isOnMove = true;
						onMove();
					} catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
						LOGGER.logError(e);
					} finally {
						isOnMove = false;
					}
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
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
        	LOGGER.logError(e);
            return;
        }

        // TODO stardust stats
        int xp = 0;

        for (CatchablePokemon p : pokemons) {
            try {
                EncounterResult er = p.encounterPokemon();

                // https://github.com/Grover-c13/PokeGOAPI-Java/issues/406
                if (er.getStatus() == Status.ENCOUNTER_ALREADY_HAPPENED) {
                	LOGGER.red("Already happened");
                	continue;
                }

                if (er.wasSuccessful()) {
                    CatchResult res = p.catchPokemon();
                    xp = AbstractPgoBotRunner.listSum(res.getXpList());
                    this.sc.addXp(xp);
                    this.sc.addCaughtPokemon(p);
                    switch (res.getStatus()) {
                        case CATCH_SUCCESS:
                            String msg = "Gefangen: " + Dictionary.getNameFromPokemonId(p.getPokemonId()) +
                                    " (IV: " + er.getPokemonData().getIndividualAttack() + "/" + er.getPokemonData().getIndividualDefense() + "/" + er.getPokemonData().getIndividualStamina() +
                                    " XP: " + xp + " SD: " + listSum(res.getStardustList()) + ")";

                            if (er.getPokemonData().getIndividualAttack() + er.getPokemonData().getIndividualDefense() + er.getPokemonData().getIndividualStamina() >= 41) {
                                LOGGER.mangenta(msg);
                            } else {
                                LOGGER.green(msg);
                            }

                            if(p.getPokemonId().equals(PokemonIdOuterClass.PokemonId.PIKACHU)) {
                                printPikachu();
                            }

                            break;
                        case CATCH_FLEE:
                        case CATCH_ESCAPE:
                        case CATCH_MISSED:
                        	LOGGER.red("Entkommen: " + Dictionary.getNameFromPokemonId(p.getPokemonId()));
                            break;
                        default:
                        	LOGGER.red("Unbekanter Status: " + res.getStatus().name());
                        	break;
                    }
                }
            } catch (LoginFailedException | RemoteServerException | NoSuchItemException | AsyncPokemonGoException e) {
            	LOGGER.logError(e);
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
        String msg;

        for (Pokestop ps : sortedPokestops) {
            wp = new GeoCoordinate(ps.getLatitude(), ps.getLongitude());
            if (ps.canLoot(true)) {
                moveTo(wp);
                ++numLooted;
                PokestopLootResult lootResult = ps.loot();
                xp = lootResult.getExperience();
                msg = "Loote (" + numLooted + "/" + numPokestops + "): " + ps.getDetails().getName()
                        + " ## " + lootResult.getResult().name() + " ## " + xp + "EXP";

                if(xp >= 100) {
                    LOGGER.mangenta(msg);
                } else {
                    LOGGER.blue(msg);
                }

                this.sc.addXp(xp);

                Map<ItemIdOuterClass.ItemId, Long> collect = lootResult.getItemsAwarded().stream()
                        .collect(Collectors.groupingBy(ItemAwardOuterClass.ItemAward::getItemId, Collectors.counting()));

                for (ItemIdOuterClass.ItemId id : collect.keySet()) {
                    msg = "  " + Dictionary.getNameFromItemId(id) + " (" + collect.get(id) + ")";
                    if (xp >= 100) {
                        LOGGER.mangenta(msg);
                    } else {
                        LOGGER.blue(msg);
                    }

                }

            } else {
            	LOGGER.blue("Ignoriere: " + ps.getDetails().getName());
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
        } catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
        	LOGGER.logError(e);
        }

        return false;
    }

    protected void printEggStatus() {
        final Set<EggPokemon> eggs;
        try {
			eggs = this.go.getInventories().getHatchery().getEggs();
			LOGGER.yellow("Eier");
			for (EggPokemon e : eggs) {
				if (e.isIncubate()) {
					LOGGER.yellow(e.getEggKmWalked() + "/" + e.getEggKmWalkedTarget());
				}
			}
		} catch (LoginFailedException | RemoteServerException | AsyncPokemonGoException e) {
			LOGGER.logError(e);
		}
    }

    @Override
	public GeoCoordinate getCurrentPosition() {
    	// nicht direkt über das PokemonGo Objekt um Threading Probleme zu vermeiden
		return lastKnownPosition;
	}

	private static void printPikachu() {
        LOGGER.yellow("\n           ,     ,_\n" +
                "           |`\\    `;;,            ,;;'\n" +
                "           |  `\\    \\ '.        .'.'\n" +
                "           |    `\\   \\  '-\"\"\"\"-' /\n" +
                "           `.     `\\ /          |`\n" +
                "             `>    /;   _     _ \\ \n" +
                "              /   / |       .    ;\n" +
                "             <  (`\";\\ ()   ~~~  (/_\n" +
                "              ';;\\  `,     __ _.-'` )\n" +
                "                >;\\          `   _.'\n" +
                "                `;;\\          \\-'\n" +
                "                  ;/           \\ _\n" +
                "                  |   ,\"\".     .` \\\n" +
                "                  |      _|   '   /\n" +
                "                   ;    /\")     .;-,\n" +
                "                    \\    /  __   .-'\n" +
                "                     \\,_/-\"`  `-'");
    }

}
