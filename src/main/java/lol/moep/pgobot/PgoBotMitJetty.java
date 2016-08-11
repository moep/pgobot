package lol.moep.pgobot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.GoogleUserCredentialProvider;

import lol.moep.pgobot.model.GeoCoordinate;
import lol.moep.pgobot.model.StatsCounter;
import lol.moep.pgobot.runners.Jumper;
import lol.moep.pgobot.runners.PgoBotRunner;
import lol.moep.pgobot.runners.RoundtripRunner;
import lol.moep.pgobot.runners.WaypointRunner;
import lol.moep.pgobot.util.PoGoLogger;
import lol.moep.pgobot.waypoints.MartinWaypoints;
import okhttp3.OkHttpClient;

/**
 * Created by moep on 26.07.16.
 */
public class PgoBotMitJetty {
	
	private static final PoGoLogger LOGGER = PoGoLogger.getInstance();

	private static final String TOKEN_PROPERTIES_FILE_NAME = "token.properties";
	
	private static Server server;
	private static PgoBotRunner runner;
	
	public static class HelloWorldHandler extends AbstractHandler {
		
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			if (target == null || !target.contains("RunnerData")) {
				return;
			}
			
			final StringBuilder responseText = new StringBuilder();
			GeoCoordinate currentPosition = new GeoCoordinate(1, 1);
			responseText.append("<root>");
			if (runner != null) {
				currentPosition = runner.getCurrentPosition();
				StatsCounter statistics = runner.getStatistics();
				responseText.append("<meterstravelled>" + statistics.getMetersTraveledAsString() + "</meterstravelled>");
				responseText.append("<xp>" + statistics.getXp() + "xp</xp>");
				responseText.append("<errors>" + LOGGER.getErrorCount() + " errors</errors>");
				responseText.append("<messages>");
				List<String> messages = LOGGER.getMessages();
				if (messages.size() > 32) {
					messages = messages.subList(messages.size() - 32, messages.size());
				}
				for (String message: messages) {
					responseText.append("<message>");
					responseText.append("<![CDATA[");
					responseText.append(message + "<br/>");
					responseText.append("]]>");
					responseText.append("</message>");
				}
				responseText.append("</messages>");
			} else {
				responseText.append("<meterstravelled>0m</meterstravelled>");
				responseText.append("<xp>0xp</xp>");
				responseText.append("<xp>0 errors</xp>");
				responseText.append("<messages></messages>");
			}
			
			if (currentPosition != null) {
				responseText.append("<currentposition>" + currentPosition.toString() + "</currentposition>");
				responseText.append("<lat>" + currentPosition.getLat() + "</lat>");
				responseText.append("<lon>" + currentPosition.getLon() + "</lon>");
			} else {
				responseText.append("<currentposition/>");
				responseText.append("<lat/>");
				responseText.append("<lon/>");
			}
			responseText.append("</root>");
			
			response.setContentType("text/xml");
			response.setHeader("Cache-Control", "no-cache");
//			response.setContentLength(responseText.length());
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(responseText.toString());
			response.flushBuffer();
			
			baseRequest.setHandled(true);
		}
	}
	
	public static void main(String[] args) throws Exception {
		server = new Server(8080);

		final ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[]{"jetty.html"});
		resourceHandler.setResourceBase("src/main/resources");
		
		final HandlerList handlerList = new HandlerList();
		handlerList.addHandler(resourceHandler);
		handlerList.addHandler(new HelloWorldHandler());
		server.setHandler(handlerList);
		
		server.start();
		startRunnerInThread();
		server.join();
	}
	
	private static void startRunnerInThread() {
		final Thread runnerThread = new Thread(() -> {startRunner(); stopServer();});
		runnerThread.run();
	}

	private static void startRunner() {
		try {
			final OkHttpClient httpClient = new OkHttpClient();
			final String refreshToken = getRefreshToken();
			
			final PokemonGo go = new PokemonGo(new GoogleUserCredentialProvider(httpClient, refreshToken), httpClient);
			
//		final PgoBotRunner r = new HumboldtHainRunner(go);
//			runner = new ExplorerRunner(go, MartinWaypoints.S_GESUNDBRUNNEN, 3);
			// nicht zu lange laufen lassen, nach gut 55 Minuten kam SocketTimeoutException
			runner = new RoundtripRunner(go, MartinWaypoints.glumandaAction(), 25);
//			runner = new WaypointRunner(go, MartinWaypoints.spreeTour());
//			runner = new MockRunner();
//			runner = new Jumper(go, 52.503138, 13.373655);
			runner.startTour();
			
			runner.getStatistics().print();
			LOGGER.logMessage("Fertig Meister!");
			
			// ein wenig warten, damit die Web Anwendung die letzte Aktualisierung mitbekommt
	        try {
	            Thread.sleep(10000);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void stopServer() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
    private static String getRefreshToken() {
        Properties properties = new Properties();
        String refreshToken = null;

        try {
            properties.load(new FileInputStream(TOKEN_PROPERTIES_FILE_NAME));
            refreshToken = properties.getProperty("refreshToken");
        } catch (IOException e) {
            // It's ok if the file cannot be accessed
        }

        return refreshToken;
    }
}
