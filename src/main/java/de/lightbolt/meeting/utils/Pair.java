package de.lightbolt.meeting.utils;

/**
 * Utility class that stores to variables.
 *
 * @param <F>    First generic.
 * @param <S>    Second generic.
 * @param first  First variable.
 * @param second Second variable.
 */
public record Pair<F, S>(F first, S second) {
}
