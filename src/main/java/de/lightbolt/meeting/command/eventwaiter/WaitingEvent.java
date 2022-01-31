package de.lightbolt.meeting.command.eventwaiter;

import net.dv8tion.jda.api.events.GenericEvent;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Child class that represents a single event
 */
public class WaitingEvent<T extends GenericEvent> {
	final Predicate<T> condition;
	final Consumer<T> action;

	WaitingEvent(Predicate<T> condition, Consumer<T> action) {
		this.condition = condition;
		this.action = action;
	}

	boolean attempt(T event) {
		if (condition.test(event)) {
			action.accept(event);
			return true;
		}
		return false;
	}
}
