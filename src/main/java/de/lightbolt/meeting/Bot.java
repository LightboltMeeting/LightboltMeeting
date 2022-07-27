package de.lightbolt.meeting;

import com.dynxsty.dih4jda.DIH4JDA;
import com.dynxsty.dih4jda.DIH4JDABuilder;
import com.dynxsty.dih4jda.exceptions.DIH4JDAException;
import com.zaxxer.hikari.HikariDataSource;
import de.lightbolt.meeting.data.config.BotConfig;
import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.listener.AutoCompleteListener;
import de.lightbolt.meeting.listener.ButtonListener;
import de.lightbolt.meeting.listener.ModalSubmitListener;
import de.lightbolt.meeting.listener.StartupListener;
import de.lightbolt.meeting.systems.meeting.MeetingStateManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

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
	 * A reference to the {@link JDA} instance.
	 */
	public static JDA jda;
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
	/**
	 * A reference to the bot's {@link MeetingStateManager}.
	 */
	public static MeetingStateManager meetingStateManager;

	public static void main(String[] args) throws LoginException, DIH4JDAException {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
		config = new BotConfig(Path.of("config"));
		dataSource = DbHelper.initDataSource(config);
		asyncPool = Executors.newScheduledThreadPool(config.getSystems().getAsyncPoolSize());
		jda = JDABuilder.createDefault(config.getSystems().getJdaBotToken())
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.setChunkingFilter(ChunkingFilter.ALL)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableCache(CacheFlag.ACTIVITY)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
				.build();
		DIH4JDA dih4jda = DIH4JDABuilder
				.setJDA(jda)
				.setCommandsPackage("de.lightbolt.meeting.systems")
				.build();
		addEventListener(jda);
	}

	private static void addEventListener(@NotNull JDA jda) {
		jda.addEventListener(
				new StartupListener(),
				new AutoCompleteListener(),
				new ModalSubmitListener(),
				new ButtonListener()
		);
	}
}
