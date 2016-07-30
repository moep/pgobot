package lol.moep.pgobot.runners;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.GeoCoordinate;

import java.util.Scanner;
import java.util.concurrent.SynchronousQueue;

import static spark.Spark.*;

/**
 * Does a run according to user input.
 */
public class InteractiveRunner extends AbstractPgoBotRunner {
    public InteractiveRunner(PokemonGo go) {
        super(go);
        startWebserver();
    }

    @Override
    public void startTour() throws LoginFailedException, RemoteServerException {
        // Potsdamer Platz
        GeoCoordinate c1 = new GeoCoordinate(52.509370, 13.374196);

        System.out.println("=== WebRunner ===");
        this.go.setLocation(c1.getLat(), c1.getLon(), 0);
        lootAllPokestopsWithinRadius(350);
        tradeInTrashMobs();

        System.out.println("=== /WebRunner ===");
    }

    private void startWebserver() {
        final SynchronousQueue<String> tasks = new SynchronousQueue<>();

        staticFileLocation("/");
        port(9090);
        System.out.println("init");
        get("/api/playerPosition", (req, res) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"lat\": ").append(this.go.getLatitude());
            sb.append(", ");
            sb.append(" \"lon\": ").append(this.go.getLongitude());
            sb.append("}");

            return sb.toString();
        });
    }
}
