package lol.moep.pgobot.runners;

import java.util.List;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.util.Actions;

/**
 * Besucht alle Wegpunkte nacheinander und fängt danach wieder von vorne an, bis
 * die voreingestellte Zeit abgelaufen ist.
 * 
 * @author Hicks
 */
public class RoundtripRunner extends AbstractPgoBotRunner {

	private final long tripTime;
	private final List<GeoCoordinate> waypoints;

	/**
	 * 
	 * @param go
	 * @param waypoints
	 * @param tripTime
	 *            Laufzeit in Minuten
	 */
	public RoundtripRunner(PokemonGo go, final List<GeoCoordinate> waypoints, final long tripTime) {
		super(go);

		this.waypoints  = waypoints;
		this.tripTime = tripTime;

	}

	@Override
	public final void startTour() throws LoginFailedException, RemoteServerException {
		String runnerName = getClass().getSimpleName();
		if (runnerName.endsWith("Runner")) {
			runnerName = runnerName.substring(0, runnerName.lastIndexOf("Runner"));
		}

		this.sc.logMessage(String.format("=== %s (looting) ===", runnerName));

		teleportTo(waypoints.get(0));
		int i = 1;

		final long startTime = System.currentTimeMillis();
		final long endTime = startTime + (tripTime * 1000 * 60);

		while (System.currentTimeMillis() < endTime) {
			this.sc.logMessage(String.format("Gehe zu Wegpunkt %d von %d", i, waypoints.size()));
			lootAllPokestopsWithinRadius(50);
			moveTo(waypoints.get(i));
			Actions.tradeInTrashMobs(go, sc);
			Actions.tradeInTrashItems(go, sc);
			i = (i+1) % waypoints.size();
		}
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
