package lol.moep.pgobot.runners;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.Haversine;
import lol.moep.pgobot.util.Actions;
import lol.moep.pgobot.util.MapScanner;

/**
 * @author Hicks
 */
public class ExplorerRunner extends AbstractPgoBotRunner {
	
	private static final double RADIUS = 1000;
	private final Random random;
	
	private final GeoCoordinate origin;
	private final long tripTime;
	
	private Pokestop currentPokestop;
	private Pokestop nextPokestop;
	
    public ExplorerRunner(PokemonGo go, final double lat, final double lon, final long tripTime) {
        this(go, new GeoCoordinate(lat, lon), tripTime);
    }
    
    public ExplorerRunner(PokemonGo go, final GeoCoordinate origin, final long tripTime) {
    	super(go);
    	
    	this.origin = origin;
    	this.tripTime = tripTime;
    	this.random = new Random();
    }

    @Override
    public final void startTour() throws LoginFailedException, RemoteServerException {
    	String runnerName = getClass().getSimpleName();
    	if (runnerName.endsWith("Runner")) {
    		runnerName = runnerName.substring(0, runnerName.lastIndexOf("Runner"));
    	}
    	
        this.sc.logMessage(String.format("=== %s (looting) ===", runnerName));

        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + (tripTime * 1000 * 60);
        
        teleportTo(origin);
        Actions.sleep(1000);
        findAndCatchPokemon();
        
        GeoCoordinate nextWaypoint;
        do {
        	// lure Überprüfung hat zu einem Haufen Exceptions geführt... vielleicht später
//        	maybeStay();
        	nextWaypoint = findNextPointOfInterest();
        	if (nextWaypoint == null) {
        		break;
        	}
        	moveTo(nextWaypoint);
        	lootAllPokestopsWithinRadius(50);
        	
			this.currentPokestop = this.nextPokestop;
			this.nextPokestop = null;
			Actions.tradeInDuplicates(go, sc);
			Actions.tradeInTrashItems(go, sc);
        } while (System.currentTimeMillis() < endTime);
        
        Actions.tradeInDuplicates(go, sc);

        this.sc.logMessage(String.format("=== / %s (looting) ===", runnerName));
    }

    /**
     * Stay in the area if a nearby pokestop has a lure.
     * 
     * @throws LoginFailedException
     * @throws RemoteServerException
     */
	private void maybeStay() throws LoginFailedException, RemoteServerException {
		while(currentPokestop != null && hasLure(currentPokestop)) {
			// stay near lures for fun and profit
			findAndCatchPokemon();
			lootAllPokestopsWithinRadius(50);
			moveTo(new GeoCoordinate(currentPokestop.getLatitude(), currentPokestop.getLongitude()), 3600);
			Actions.sleep(10000);
		}
	}
    
    private GeoCoordinate findNextPointOfInterest() throws LoginFailedException, RemoteServerException {
    	double searchRadius = RADIUS;
    	
    	final Collection<Pokestop> pokestops = MapScanner.getPokestops(go);
    	if (pokestops.isEmpty()) {
    		return null;
    	}
    	
    	Pokestop[] pokestopsInVicinity;
    	do {
    		pokestopsInVicinity = findPokestopsInVicinity(pokestops, searchRadius);
    		searchRadius += RADIUS;
    	} while (pokestopsInVicinity != null && pokestopsInVicinity.length == 0);
 
    	if (pokestopsInVicinity == null) {
    		return null;
    	}
    	
    	final Pokestop nextPokestop = pokestopsInVicinity[random.nextInt(pokestopsInVicinity.length)];
    	this.nextPokestop = nextPokestop;
    	
    	return new GeoCoordinate(nextPokestop.getLatitude(), nextPokestop.getLongitude());
    }

	private Pokestop[] findPokestopsInVicinity(Collection<Pokestop> pokestops, final double searchRadius) throws LoginFailedException, RemoteServerException {
		final GeoCoordinate playerPosition = new GeoCoordinate(this.go.getLatitude(), this.go.getLongitude());

		// keine benutzbaren Pokestops in der Nähe :-(
		final Pokestop[] lootablePokestops = pokestops.stream()
				.filter(p -> p.canLoot(true)).toArray(Pokestop[]::new);
		if (lootablePokestops.length == 0) {
			return null;
		}

//		final Pokestop[] luringPokestopsInVicinity = pokestops.stream()
//				.filter(p -> p.canLoot(true) 
//						&& hasLure(p)
//						&& Haversine.getDistanceInMeters(playerPosition, new GeoCoordinate(p.getLatitude(), p.getLongitude())) <= searchRadius)
//				.toArray(Pokestop[]::new);
//		
//		if (luringPokestopsInVicinity.length > 0) {
//			return luringPokestopsInVicinity;
//		}

		final Pokestop[] pokestopsInVicinity = 
				Arrays.asList(lootablePokestops)
				.stream()
				.filter(p -> Haversine.getDistanceInMeters(playerPosition, new GeoCoordinate(p.getLatitude(), p.getLongitude())) <= searchRadius)
				.toArray(Pokestop[]::new);
		
		return pokestopsInVicinity;
	}
	
	private boolean hasLure(final Pokestop pokestop) {
		try {
			return pokestop.hasLure();
		} catch (LoginFailedException | RemoteServerException e) {
			this.sc.logError(e);
		}
		
		return false;
	}
	
	@Override
	protected void onMove() throws LoginFailedException, RemoteServerException {
		super.onMove();
		lootAllPokestopsWithinRadius(50);
	}
    
}
