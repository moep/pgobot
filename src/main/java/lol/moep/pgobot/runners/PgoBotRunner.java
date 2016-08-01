package lol.moep.pgobot.runners;

import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import lol.moep.pgobot.model.GeoCoordinate;
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
     * 
     * @return the bot's current position
     */
    GeoCoordinate getCurrentPosition();
    
    /**
     * @return The runners associated statistics counter.
     */
    StatsCounter getStatistics();
}
