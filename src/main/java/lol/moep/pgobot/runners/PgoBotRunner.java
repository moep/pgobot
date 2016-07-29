package lol.moep.pgobot.runners;

import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.StatsCounter;

/**
 * Created by moep on 28.07.16.
 */
public interface PgoBotRunner {
    /**
     * Starts the bot's tour / programm
     */
    void startTour() throws LoginFailedException, RemoteServerException;

    /**
     * @return The runners associated statistics counter.
     */
    StatsCounter getStatistics();
}
