package lol.moep.pgobot.util.logger;

import java.util.List;

/**
 * Created by moep on 11.08.16.
 */
public interface Logger {

    void info(final Object message);

    void red(final Object message);

    void green(final Object message);

    void yellow(final Object message);

    void blue(final Object message);

    void mangenta(final Object message);

    void gray(final Object message);

    void logError(final Throwable t);

    void logError(final String message, final Throwable t);

    List<String> getMessages();

    int getErrorCount();

}
