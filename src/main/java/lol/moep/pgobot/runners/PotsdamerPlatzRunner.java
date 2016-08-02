package lol.moep.pgobot.runners;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.util.Actions;

public class PotsdamerPlatzRunner extends AbstractPgoBotRunner {
    public PotsdamerPlatzRunner(PokemonGo go) {
        super(go);
    }

    @Override
    public void startTour() throws LoginFailedException, RemoteServerException {
        // Arbeit
        GeoCoordinate c1 = new GeoCoordinate(52.506993, 13.357878);
        // Potsdamer Platz
        GeoCoordinate c2 = new GeoCoordinate(52.509370, 13.374196);
        GeoCoordinate c3 = new GeoCoordinate(52.505198, 13.372222);

        // Wegpunkte
        GeoCoordinate x1 = new GeoCoordinate(52.509899, 13.359208);

        System.out.println("=== Potsdamer Platz (looting) ===");

        this.go.setLocation(c1.getLat(), c1.getLon(), 0);

        lootAllPokestopsWithinRadius(50);
        moveTo(x1);
        moveTo(c2);
        lootAllPokestopsWithinRadius(250);
        moveTo(x1);
        moveTo(c1);
        lootAllPokestopsWithinRadius(50);
        Actions.tradeInTrashMobs(go, sc);

        System.out.println("=== / Potsdamer Platz (looting) ===");
    }

}
