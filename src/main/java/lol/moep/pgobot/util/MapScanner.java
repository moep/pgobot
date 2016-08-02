package lol.moep.pgobot.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

public class MapScanner {

	private static final long GET_MAP_OBJECTS_MIN_REFRESH_SECONDS = 5;
	private static final Object LOCK = new Object();

	private static Long lastMapScan = null;

	private static void sleepIfNeeded() {
		if (lastMapScan != null) {
			final long millisSinceLastScan = getMillisSinceLastScan();
			if (lastMapScan != null && millisSinceLastScan < (GET_MAP_OBJECTS_MIN_REFRESH_SECONDS * 1000)) {
				sleep(GET_MAP_OBJECTS_MIN_REFRESH_SECONDS * 1000 - millisSinceLastScan);
			}
		}
	}

	public static List<CatchablePokemon> getCatchablePokemon(final PokemonGo go)
			throws LoginFailedException, RemoteServerException {
		final List<CatchablePokemon> catchablePokemon;
		synchronized (LOCK) {
			sleepIfNeeded();
			catchablePokemon = new ArrayList<>(go.getMap().getCatchablePokemon());
			lastMapScan = System.currentTimeMillis();
		}
		return catchablePokemon;
	}

	public static Collection<Pokestop> getPokestops(final PokemonGo go)
			throws LoginFailedException, RemoteServerException {
		Collection<Pokestop> pokestops;
		synchronized (LOCK) {
			sleepIfNeeded();
			pokestops = go.getMap().getMapObjects().getPokestops();
			lastMapScan = System.currentTimeMillis();
		}
		return pokestops;
	}
	
	public static List<NearbyPokemon> bla(final PokemonGo go) throws LoginFailedException, RemoteServerException {
		final List<NearbyPokemon> nearbyPokemon;
		synchronized (LOCK) {
			sleepIfNeeded();
			nearbyPokemon = go.getMap().getNearbyPokemon();
			lastMapScan = System.currentTimeMillis();
		}
		return nearbyPokemon;
	}

	private static long getMillisSinceLastScan() {
		return System.currentTimeMillis() - lastMapScan;
	}

	protected static void sleep(long timeMilis) {
		try {
			Thread.sleep(timeMilis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
