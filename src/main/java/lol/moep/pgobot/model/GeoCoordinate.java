package lol.moep.pgobot.model;

public class GeoCoordinate {
    private double lat;
    private double lon;

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public GeoCoordinate(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(').append(this.lat).append(", ").append(this.lon).append(')');
        return sb.toString();
    }

}