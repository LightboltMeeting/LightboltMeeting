package de.lightbolt.meeting.data.h2db;

import java.sql.Connection;

/**
 * Simple Interface that handles transactions.
 */
public interface TransactionFunction {
	void execute(Connection c) throws Exception;
}
