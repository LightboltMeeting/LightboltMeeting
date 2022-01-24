package de.lightbolt.meeting;

import com.zaxxer.hikari.HikariDataSource;
import de.lightbolt.meeting.command.SlashCommands;
import de.lightbolt.meeting.data.config.BotConfig;
import de.lightbolt.meeting.data.h2db.DbHelper;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bot {
	/**
	 * The set of configuration properties that this bot uses.
	 */
	public static BotConfig config;
	/**
	 * A reference to the slash command listener that's the main point of
	 * interaction for users with this bot. It's marked as a publicly accessible
	 * reference so that {@link SlashCommands#registerSlashCommands} can
	 * be called wherever it's needed.
	 */
	public static SlashCommands slashCommands;
	/**
	 * A reference to the data source that provides access to the relational
	 * database that this bot users for certain parts of the application. Use
	 * this to obtain a connection and perform transactions.
	 */
	public static HikariDataSource dataSource;
	/**
	 * A general-purpose thread pool that can be used by the bot to execute
	 * tasks outside the main event processing thread.
	 */
	public static ScheduledExecutorService asyncPool;

	public static void main(String[] args) throws LoginException {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
		config = new BotConfig(Path.of("config"));
		dataSource = DbHelper.initDataSource(config);
		slashCommands = new SlashCommands();
		asyncPool = Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
		var jda = JDABuilder.createDefault(config.getSystems().getJdaBotToken())
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.setChunkingFilter(ChunkingFilter.ALL)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableCache(CacheFlag.ACTIVITY)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
				.addEventListeners(slashCommands)
				.build();
	}
}
