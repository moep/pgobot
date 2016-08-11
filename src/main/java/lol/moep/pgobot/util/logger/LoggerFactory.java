package lol.moep.pgobot.util.logger;

import java.io.UnsupportedEncodingException;

/**
 * Created by moep on 11.08.16.
 */
public class LoggerFactory {

    private static final Logger instance = getLoggerInstance();

    public static Logger getLoggerInstance() {

        if (instance != null) {
            return instance;
        }

        try {
            return new ColorLogger();
        } catch (UnsupportedEncodingException e) {
            // TODO implement simple logger
            e.printStackTrace();
            return null;
        }
    }
}
