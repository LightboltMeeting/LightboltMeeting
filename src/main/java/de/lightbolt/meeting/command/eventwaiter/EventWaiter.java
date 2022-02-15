package de.lightbolt.meeting.command.eventwaiter;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that waits for a specific event and executes the given Consumer.
 * Highly inspired by the JDA-Utilities Eventwaiter.
 */
@Slf4j
public class EventWaiter implements EventListener {

	private final HashMap<Class<?>, Set<WaitingEvent>> waitingEvents;
	private final ScheduledExecutorService threadpool;

	public EventWaiter() {
		this(Executors.newSingleThreadScheduledExecutor(), true);
	}

	public EventWaiter(ScheduledExecutorService threadpool, boolean shutdownAutomatically) {
		this.waitingEvents = new HashMap<>();
		this.threadpool = threadpool;
	}


	/**
	 * Waits for a specific event and executes the given Consumer.
	 *
	 * @param classType The event's class.
	 * @param condition The condition that should be fulfilled.
	 * @param action    The Consumer that is executed once the given event is received.
	 * @param <T>       The generic type.
	 */
	public <T extends Event> void waitForEvent(Class<T> classType, Predicate<T> condition, Consumer<T> action) {
		waitForEvent(classType, condition, action, -1, null, null);
	}

	/**
	 * Same as {@link EventWaiter#waitForEvent(Class, Predicate, Consumer)}, but adds a Timeout.
	 *
	 * @param classType     The event's class.
	 * @param condition     The condition that should be fulfilled.
	 * @param action        The Consumer that is executed once the given event is received.
	 * @param timeout       The timeout as a single number.
	 * @param unit          The timeout's {@link TimeUnit}.
	 * @param timeoutAction A runnable that is executed when the event waiter timed out.
	 * @param <T>           The generic type.
	 */
	public <T extends Event> void waitForEvent(Class<T> classType, Predicate<T> condition, Consumer<T> action,
	                                           long timeout, TimeUnit unit, Runnable timeoutAction) {

		WaitingEvent we = new WaitingEvent<>(condition, action);
		Set<WaitingEvent> set = waitingEvents.computeIfAbsent(classType, c -> new HashSet<>());
		set.add(we);

		if (timeout > 0 && unit != null) {
			threadpool.schedule(() -> {
				try {
					if (set.remove(we) && timeoutAction != null) {
						timeoutAction.run();
					}
				} catch (Exception ex) {
					log.error("Failed to run timeoutAction", ex);
				}
			}, timeout, unit);
		}
	}

	@Override
	public void onEvent(GenericEvent event) {
		Class c = event.getClass();
		while (c != null) {
			if (waitingEvents.containsKey(c)) {
				Set<WaitingEvent> set = waitingEvents.get(c);
				WaitingEvent[] toRemove = set.toArray(new WaitingEvent[set.size()]);

				set.removeAll(Stream.of(toRemove).filter(i -> i.attempt(event)).collect(Collectors.toSet()));
			}
			if (event instanceof ShutdownEvent) threadpool.shutdown();
			c = c.getSuperclass();
		}
	}
}
