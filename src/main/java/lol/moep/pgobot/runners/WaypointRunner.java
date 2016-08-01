package lol.moep.pgobot.runners;

import java.util.List;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.util.Actions;

/**
 * @author Hicks
 */
public class WaypointRunner extends AbstractPgoBotRunner {

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

		this.sc.logMessage(String.format("=== %s (looting) ===", runnerName));

		teleportTo(waypoints.get(0));

		for (int i = 1; i < waypoints.size(); i++) {
			this.sc.logMessage(String.format("Gehe zu Wegpunkt %d von %d", i, waypoints.size()));
			lootAllPokestopsWithinRadius(50);
			moveTo(waypoints.get(i));
			Actions.tradeInTrashMobs(go, sc);
			Actions.tradeInWeaklings(go, sc);
			Actions.tradeInTrashItems(go, sc);
		}
		lootAllPokestopsWithinRadius(50);

		moveTo(waypoints.get(0));
		lootAllPokestopsWithinRadius(50);
		Actions.tradeInTrashMobs(go, sc);
		Actions.tradeInWeaklings(go, sc);
		Actions.tradeInTrashItems(go, sc);

		this.sc.logMessage(String.format("=== / %s (looting) ===", runnerName));
	}
	
	@Override
	protected void onMove() throws LoginFailedException, RemoteServerException {
		super.onMove();
		lootAllPokestopsWithinRadius(50);
	}

}
