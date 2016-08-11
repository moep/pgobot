package lol.moep.pgobot.runners;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.util.Actions;
import lol.moep.pgobot.util.PoGoLogger;

/**
 * @author Hicks
 */
public class Jumper extends AbstractPgoBotRunner {

	private static final PoGoLogger LOGGER = PoGoLogger.getInstance();
	
	private final GeoCoordinate jumpPoint;

	public Jumper(final PokemonGo go, final double lat, final double lon) {
		super(go);

		this.jumpPoint = new GeoCoordinate(lat, lon);
	}

	@Override
	public final void startTour() throws LoginFailedException, RemoteServerException {
		String runnerName = getClass().getSimpleName();
		if (runnerName.endsWith("Runner")) {
			runnerName = runnerName.substring(0, runnerName.lastIndexOf("Runner"));
		}

		LOGGER.logMessage(String.format("=== %s (looting) ===", runnerName));

		teleportTo(jumpPoint);
		findAndCatchPokemon();
		lootAllPokestopsWithinRadius(50);

		Actions.tradeInDuplicates(go, sc);
		Actions.tradeInTrashItems(go, sc);

		LOGGER.logMessage(String.format("=== / %s (looting) ===", runnerName));
	}
	
	@Override
	protected void onMove() throws LoginFailedException, RemoteServerException {
		super.onMove();
		lootAllPokestopsWithinRadius(50);
	}

}
