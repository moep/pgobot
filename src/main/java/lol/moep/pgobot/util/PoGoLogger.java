package lol.moep.pgobot.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PoGoLogger {

	private static final PoGoLogger INSTANCE = new PoGoLogger();

	private AtomicInteger errorCount;
	private List<String> messages;

	private PoGoLogger() {
		this.messages = new ArrayList<>();
		this.errorCount = new AtomicInteger(0);
	}

	public static PoGoLogger getInstance() {
		return INSTANCE;
	}

	private String timestamp() {
		return new SimpleDateFormat("dd.MM.yyyy hh:mm ").format(new Date());
	}

	public void logMessage(final Object message) {
		final String msgAsString = timestamp() + String.valueOf(message);
		System.out.println(msgAsString);
		messages.add(msgAsString);
	}

	public void logError(final Throwable t) {
		messages.add(timestamp() + t.getMessage());
		t.printStackTrace();
		errorCount.incrementAndGet();
	}

	public void logError(final String message, final Throwable t) {
		messages.add(timestamp() + message);
		System.out.println(timestamp() + message);
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
