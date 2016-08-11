package lol.moep.pgobot.runners;

import java.util.List;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.util.Actions;
import lol.moep.pgobot.util.IVAnalyzer;
import lol.moep.pgobot.util.PoGoLogger;

/**
 * @author Hicks
 */
public class WaypointRunner extends AbstractPgoBotRunner {
	
	private static final PoGoLogger LOGGER = PoGoLogger.getInstance();

	private final List<GeoCoordinate> waypoints;

	public WaypointRunner(final PokemonGo go, final List<GeoCoordinate> waypoints) {
		super(go);

		this.waypoints = waypoints;
	}

	@Override
	public final void startTour() throws LoginFailedException, RemoteServerException {
		String runnerName = getClass().getSimpleName();
		if (runnerName.endsWith("Runner")) {
			runnerName = runnerName.substring(0, runnerName.lastIndexOf("Runner"));
		}

		LOGGER.logMessage(String.format("=== %s (looting) ===", runnerName));

		teleportTo(waypoints.get(0));
		IVAnalyzer.analyze(go);
		
		for (int i = 1; i < waypoints.size(); i++) {
			LOGGER.logMessage(String.format("Gehe zu Wegpunkt %d von %d", i, waypoints.size()));
			lootAllPokestopsWithinRadius(50);
			walkTo(waypoints.get(i));
			Actions.tradeInDuplicates2(go, sc);
			Actions.tradeInTrashItems(go, sc);
		}
		lootAllPokestopsWithinRadius(50);

		moveTo(waypoints.get(0));
		lootAllPokestopsWithinRadius(50);
		Actions.tradeInDuplicates2(go, sc);
		Actions.tradeInTrashItems(go, sc);
		IVAnalyzer.analyze(go);
		
		LOGGER.logMessage(String.format("=== / %s (looting) ===", runnerName));
	}
	
	@Override
	protected void onMove() throws LoginFailedException, RemoteServerException {
		super.onMove();
		lootAllPokestopsWithinRadius(50);
	}

}
