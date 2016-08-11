package lol.moep.pgobot.util.logger;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Created by moep on 11.08.16.
 */
public class ColorLogger extends AbstractLogger implements Logger {
    private final PrintWriter writer;

    private static final String ANSI_RESET = "\u001B[0m";

    private static enum Color {
        NONE(""),
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        MANGENTA("\u001B[35m"),
        GRAY("\u001B[37m");

        private String color;

        Color(String color) {
            this.color = color;
        }

        @Override
        public String toString() {
            return this.color;
        }

    }
    public ColorLogger() throws UnsupportedEncodingException {
        this.writer = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true);
        System.out.println("Color logger initialized");
    }

    private synchronized void println(String message, Color color) {
        super.logMessage(message);

        this.writer.print(color);
        this.writer.print(getTimestampString() + message);
        this.writer.println(ANSI_RESET);
        this.writer.flush();
    }

    @Override
    public void info(final Object message) {
        println(String.valueOf(message), Color.NONE);
    }

    @Override
    public void red(final Object message) {
        println(String.valueOf(message), Color.RED);
    }

    @Override
    public void green(final Object message) {
        println(String.valueOf(message), Color.GREEN);
    }

    @Override
    public void yellow(final Object message) {
        println(String.valueOf(message), Color.YELLOW);
    }

    @Override
    public void blue(final Object message) {
        println(String.valueOf(message), Color.BLUE);
    }

    @Override
    public void mangenta(final Object message) {
        println(String.valueOf(message), Color.MANGENTA);
    }

    @Override
    public void gray(final Object message) {
        println(String.valueOf(message), Color.GRAY);
    }

}
