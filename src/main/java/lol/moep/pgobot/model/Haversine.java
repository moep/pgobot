package lol.moep.pgobot.model;

public class Haversine {
    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM

    public static double getDistanceInMeters(GeoCoordinate c1, GeoCoordinate c2) {

        double dLat = Math.toRadians((c2.getLat() - c1.getLat()));
        double dLong = Math.toRadians((c2.getLon() - c1.getLon()));

        double startLat = Math.toRadians(c1.getLat());
        double endLat = Math.toRadians(c2.getLat());

        double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c * 1000; // <-- d
    }

    public static double haversin(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }
}