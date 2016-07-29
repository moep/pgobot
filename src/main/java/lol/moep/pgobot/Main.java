package lol.moep.pgobot;

public class Main {
    private static int getAnzahlWerte(int[] messwerte) {
        return messwerte.length;
    }

    private static int getMin(int[] messwerte) {
        int min = messwerte[0];


        for (int i = 0; i < messwerte.length; i++) {
            if (messwerte[i] == 0) {
                break;
            }

            if (messwerte[i] < min) {
                min = messwerte[i];
            }
        }

        return min;
    }

    private static int getMax(int[] messwerte) {
        int max = messwerte[0];

        for (int i = 0; i < messwerte.length; i++) {
            if (messwerte[i] > max) {
                max = messwerte[i];
            }
        }

        return max;
    }

    private static double getArithmetischesMittel(int[] messwerte) {
        int sum = 0;

        for (int i = 0; i < messwerte.length; i++) {
            sum += messwerte[i];
        }

        return sum / messwerte.length;
    }

    private static double getStandardabweichung(int[] messwerte) {
        double faktor = 1 / (messwerte.length - 1);

        double sum = 0;
        double arithmetischesMittel = getArithmetischesMittel(messwerte);

        for (int i = 0; i < messwerte.length; i++) {
            sum += Math.pow((messwerte[i] - arithmetischesMittel), 2);
        }

        return Math.sqrt(sum);
    }

    public static void main(String[] args) {
        int[] messwerte = new int[5000];

        System.out.println("Min: " + getMin(messwerte));
        System.out.println("Max: " + getMax(messwerte));
        System.out.println("Arithmetisches Mittel: " + getArithmetischesMittel(messwerte));
        System.out.println("Standardabweichung: " + getStandardabweichung(messwerte));

    }
}