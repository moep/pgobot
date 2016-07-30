package lol.moep.pgobot.runners;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lol.moep.pgobot.model.GeoCoordinate;

import java.util.StringTokenizer;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.DoubleAccumulator;

import static spark.Spark.*;

/**
 * Does a run according to user input.
 */
public class InteractiveRunner extends AbstractPgoBotRunner {
    private final SynchronousQueue<String> tasks;

    public InteractiveRunner(PokemonGo go) {
        super(go);
        this.tasks = new SynchronousQueue<>();
        startWebserver();
    }

    @Override
    public void startTour() throws LoginFailedException, RemoteServerException {
        // Potsdamer Platz
        GeoCoordinate c1 = new GeoCoordinate(52.509370, 13.374196);

        System.out.println("=== WebRunner ===");
        this.go.setLocation(c1.getLat(), c1.getLon(), 0);
        tradeInTrashMobs();


        String task;
        while (true) {
            System.out.println("Warte auf Tasks");
            try {
                task = this.tasks.take();
                if(task.equalsIgnoreCase("ende")) {
                    break;
                } else if (task.startsWith("move")){
                    StringTokenizer t = new StringTokenizer(task);
                    if(t.countTokens() != 3) {
                        // TODO exception?
                        continue;
                    }

                    t.nextToken();
                    double lat = Double.valueOf(t.nextToken());
                    double lon = Double.valueOf(t.nextToken());

                    if (lat != 0.0 && lon != 0.0) {
                        moveTo(new GeoCoordinate(lat, lon));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(e.getMessage());
            } finally {
                continue;
            }

        }

//        System.out.println("=== /WebRunner ===");
    }

    private void startWebserver() {
        final SynchronousQueue<String> tasks = new SynchronousQueue<>();

        staticFileLocation("/");
        port(9090);
        get("/api/playerPosition", (req, res) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"lat\": ").append(this.go.getLatitude());
            sb.append(", ");
            sb.append(" \"lon\": ").append(this.go.getLongitude());
            sb.append("}");

            return sb.toString();
        });
        post("/api/moveTo/:lat/:lon", (req, res) -> {
            System.out.println("WS: moveTo");
            String lat = req.params("lat");
            String lon = req.params("lon");
            this.tasks.put("move " + lat + " " + lon);
            return "{ \"status\": \"success\" }";
        });
    }
}
