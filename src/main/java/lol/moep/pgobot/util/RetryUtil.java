package lol.moep.pgobot.util;

import java.util.function.Consumer;

import lol.moep.pgobot.model.StatsCounter;
import lol.moep.pgobot.util.logger.Logger;
import lol.moep.pgobot.util.logger.LoggerFactory;

public class RetryUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLoggerInstance();

	public static void retry(final Runnable procedure, final int times, final StatsCounter statistics) {
		if (times <= 0) {
			return;
		}

		try {
			procedure.run();
		} catch (Exception e) {
			LOGGER.logError(e);
			retry(procedure, times - 1, statistics);
		}
	}

	public static <T> void retry(final Consumer<T> consumer, T consumed, final int times,
			final StatsCounter statistics) {
		if (times <= 0) {
			return;
		}

		try {
			consumer.accept(consumed);
		} catch (Exception e) {
			LOGGER.logError(e);
			retry(consumer, consumed, times - 1, statistics);
		}
	}

}
