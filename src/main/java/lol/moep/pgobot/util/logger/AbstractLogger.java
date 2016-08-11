package lol.moep.pgobot.util.logger;

import lol.moep.pgobot.util.PoGoLogger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by moep on 11.08.16.
 */
abstract class AbstractLogger implements Logger {
    private AtomicInteger errorCount;
    private List<String> messages;
    private static final DateFormat DATE_FORMAT =  new SimpleDateFormat("[dd.MM.yyyy@HH:mm:ss] ");

    AbstractLogger() {
        this.messages = new ArrayList<>();
        this.errorCount = new AtomicInteger(0);
    }

    protected String getTimestampString() {
        return DATE_FORMAT.format(new Date());
    }

    public void logMessage(final String message) {
        final String msgAsString = getTimestampString() + String.valueOf(message);
        messages.add(msgAsString);
    }

    public void logError(final Throwable t) {
        messages.add(getTimestampString() + t.getMessage());
        t.printStackTrace();
        errorCount.incrementAndGet();
    }

    public void logError(final String message, final Throwable t) {
        messages.add(getTimestampString() + message);
        System.err.println(getTimestampString() + message);
        t.printStackTrace();
        errorCount.incrementAndGet();
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    public int getErrorCount() {
        return errorCount.get();
    }

}
