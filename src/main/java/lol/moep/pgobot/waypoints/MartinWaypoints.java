package lol.moep.pgobot.waypoints;

import java.util.ArrayList;
import java.util.List;

import lol.moep.pgobot.model.GeoCoordinate;

public final class MartinWaypoints {
	
	public static final GeoCoordinate ZIGARETTENFABRIK = new GeoCoordinate(52.569444, 13.413206);
	public static final GeoCoordinate S_GESUNDBRUNNEN = new GeoCoordinate(52.548093, 13.388064);
	public static final GeoCoordinate POTSDAMER_PLATZ = new GeoCoordinate(52.509554, 13.376723);
	public static final GeoCoordinate FERNSEHTURM = new GeoCoordinate(52.520871, 13.410002);

	/**
	 * Dratinispawnpunkt (selten?)
	 * 
	 * @return
	 */
	public static List<GeoCoordinate> pankowGanzKleineRunde() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();
		waypoints.add(ZIGARETTENFABRIK);
		waypoints.add(new GeoCoordinate(52.569304, 13.411436));
		waypoints.add(new GeoCoordinate(52.568296, 13.412090));
		// Kaisers
		waypoints.add(new GeoCoordinate(52.571666, 13.417556));
		// Amalienpark
		waypoints.add(new GeoCoordinate(52.572279, 13.414091));
		// Pankow Kirche
		waypoints.add(new GeoCoordinate(52.570734, 13.409048));
		// zurück
		waypoints.add(new GeoCoordinate(52.569304, 13.411436));
		waypoints.add(ZIGARETTENFABRIK);

		return waypoints;
	}

	/**
	 * Bisasamspawnpunkte
	 * 
	 * @return
	 */
	public static List<GeoCoordinate> humboldtHain() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();

		// nahe S Gesundbrunnen
		waypoints.add(S_GESUNDBRUNNEN);
		waypoints.add(new GeoCoordinate(52.545000, 13.380693));
		waypoints.add(new GeoCoordinate(52.545587, 13.384942));
		waypoints.add(new GeoCoordinate(52.542579, 13.382850));
		waypoints.add(new GeoCoordinate(52.544752, 13.390435));
		waypoints.add(new GeoCoordinate(52.545261, 13.385242));
		waypoints.add(new GeoCoordinate(52.546475, 13.389222));
		
		return waypoints;
	}

	public static List<GeoCoordinate> grosseRundePotsdamerPlatz() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();
		
		waypoints.add(POTSDAMER_PLATZ);
		// Mendelson-Bartholdy-Park
		waypoints.add(new GeoCoordinate(52.503138, 13.373655));
		// Potsdamer Brücke
		waypoints.add(new GeoCoordinate(52.505824, 13.368248));
		// Herkulesbrücke
		waypoints.add(new GeoCoordinate(52.505839, 13.352074));
		// Siegessäule
		waypoints.add(new GeoCoordinate(52.505839, 13.352074));
		// nahe Staatsbibliothek
		waypoints.add(new GeoCoordinate(52.508503, 13.369771));

		return waypoints;
	}

	public static List<GeoCoordinate> kleineRundePotsdamerPlatz() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();
		
		waypoints.add(POTSDAMER_PLATZ);
		// Mendelson-Bartholdy-Park
		waypoints.add(new GeoCoordinate(52.503138, 13.373655));
		// Potsdamer Brücke
		waypoints.add(new GeoCoordinate(52.505824, 13.368248));
		
		waypoints.add(new GeoCoordinate(52.504823, 13.371588));
		waypoints.add(new GeoCoordinate(52.507971, 13.373187));
		waypoints.add(new GeoCoordinate(52.512639, 13.370773));
		
		return waypoints;
	}

	// Ponitas und Dratinis? beobachten
	public static List<GeoCoordinate> koellnischerPark() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();
		
		waypoints.add(new GeoCoordinate(52.512804, 13.414654));
		waypoints.add(new GeoCoordinate(52.513215, 13.410888));
		waypoints.add(new GeoCoordinate(52.515291, 13.417701));
		
		return waypoints;
	}

	// Sichlors so weit das Auge reicht
	public static List<GeoCoordinate> volksparkFriedrichshain() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();
		
		waypoints.add(new GeoCoordinate(52.527789, 13.426224));
		waypoints.add(new GeoCoordinate(52.529793, 13.443186));
		waypoints.add(new GeoCoordinate(52.525469, 13.430569));
		waypoints.add(new GeoCoordinate(52.526484, 13.443594));
		waypoints.add(new GeoCoordinate(52.523556, 13.433917));
		
		return waypoints;
	}
	
	// Görlitzer Park - vereinzelt Magmas (und am Wasser Dratinis?)
	public static List<GeoCoordinate> goerlitzerPark() {
		final List<GeoCoordinate> waypoints = new ArrayList<GeoCoordinate>();
		
		waypoints.add(new GeoCoordinate(52.498658, 13.432125));
		waypoints.add(new GeoCoordinate(52.494164, 13.444601));
		waypoints.add(new GeoCoordinate(52.495385, 13.452637));
		waypoints.add(new GeoCoordinate(52.498968, 13.449955));
		waypoints.add(new GeoCoordinate(52.502103, 13.442826));
		waypoints.add(new GeoCoordinate(52.500745, 13.441158));
		
		return waypoints;
	}
}
