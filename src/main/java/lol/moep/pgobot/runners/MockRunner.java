package lol.moep.pgobot.runners;

import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.StatsCounter;
import lol.moep.pgobot.waypoints.MartinWaypoints;

/**
 * MockRunner um das WebInterface zu testen.
 * 
 * @author martin.krebs
 *
 */
public class MockRunner implements PgoBotRunner {

	private StatsCounter statistics = new StatsCounter();

	@Override
	public void startTour() throws LoginFailedException, RemoteServerException {
		while (true) {
			try {
				Thread.sleep(2000);
				statistics.addMetersTraveled(10);
				statistics.addXp(50);
				statistics.logMessage("Noch eine Zeile.");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public GeoCoordinate getCurrentPosition() {
		return MartinWaypoints.POTSDAMER_PLATZ;
	}

	@Override
	public StatsCounter getStatistics() {
		return statistics;
	}

}
