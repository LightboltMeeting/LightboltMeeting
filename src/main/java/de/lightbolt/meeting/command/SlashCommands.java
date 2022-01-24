package de.lightbolt.meeting.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import de.lightbolt.meeting.Bot;

import de.lightbolt.meeting.command.data.CommandConfig;
import de.lightbolt.meeting.command.data.CommandDataLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * This listener is responsible for handling slash commands sent by users in
 * guilds where the bot is active, and responding to them by calling the
 * appropriate {@link SlashCommandHandler}.
 * <p>
 * The list of valid commands, and their associated handlers, are defined in
 * their corresponding YAML-file under the resources/commands directory.
 * </p>
 */
public class SlashCommands extends ListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

	/**
	 * Maps every command name and alias to an instance of the command, for
	 * constant-time lookup.
	 */
	private final Map<String, SlashCommandHandler> commandsIndex;

	public SlashCommands() {
		this.commandsIndex = new HashMap<>();
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (event.getGuild() == null) return;

		var command = this.commandsIndex.get(event.getName());
		if (command != null) {
			try {
				command.handle(event).queue();
			} catch (ResponseException e) {
				handleResponseException(e, event);
			}
		}
	}

	private void handleResponseException(ResponseException e, SlashCommandEvent event) {
		switch (e.getType()) {
			case WARNING -> Responses.warning(event, e.getMessage()).queue();
			case ERROR -> Responses.error(event, e.getMessage()).queue();
		}
		if (e.getCause() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.getCause().printStackTrace(pw);
			Bot.config.get(event.getGuild()).getModeration().getLogChannel().sendMessageFormat(
					"An exception occurred when %s issued the **%s** slash command in %s:\n```%s```\n",
					event.getUser().getAsMention(),
					event.getName(),
					event.getTextChannel().getAsMention(),
					sw.toString()
			).queue();
		}
	}

	/**
	 * Registers all slash commands defined in the set YAML-files for the given guild
	 * so that users can see the commands when they type a "/".
	 * <p>
	 * It does this by attempting to add an entry to {@link SlashCommands#commandsIndex}
	 * whose key is the command name, and whose value is a new instance of
	 * the handler class which the command has specified.
	 * </p>
	 *
	 * @param guild The guild to update commands for.
	 */
	public void registerSlashCommands(Guild guild) {
		CommandConfig[] commandConfigs = CommandDataLoader.load(
				"commands/economy.yaml",
				"commands/help.yaml",
				"commands/jam.yaml",
				"commands/qotw.yaml",
				"commands/staff.yaml",
				"commands/user.yaml"
		);
		this.updateCommands(commandConfigs, guild).queue(commands -> this.addCommandPrivileges(commands, commandConfigs, guild));
	}


	private CommandListUpdateAction updateCommands(CommandConfig[] commandConfigs, Guild guild) {
		log.info("[{}] Registering slash commands", guild.getName());
		if (commandConfigs.length > 100) throw new IllegalArgumentException("Cannot add more than 100 commands.");
		CommandListUpdateAction commandUpdateAction = guild.updateCommands();
		for (CommandConfig config : commandConfigs) {
			if (config.getHandler() != null && !config.getHandler().isEmpty()) {
				try {
					Class<?> handlerClass = Class.forName(config.getHandler());
					this.commandsIndex.put(config.getName(), (SlashCommandHandler) handlerClass.getConstructor().newInstance());
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			} else {
				log.warn("Command \"{}\" does not have an associated handler class. It will be ignored.", config.getName());
			}
			commandUpdateAction.addCommands(config.toData());
		}
		return commandUpdateAction;
	}

	private void addCommandPrivileges(List<Command> commands, CommandConfig[] commandConfigs, Guild guild) {
		log.info("[{}] Adding command privileges", guild.getName());
		Map<String, Collection<? extends CommandPrivilege>> map = new HashMap<>();
		for (Command command : commands) {
			List<CommandPrivilege> privileges = getCommandPrivileges(guild, findCommandConfig(command.getName(), commandConfigs));
			if (!privileges.isEmpty()) {
				map.put(command.getId(), privileges);
			}
		}
		guild.updateCommandPrivileges(map).queue(success -> log.info("Commands updated successfully"), error -> log.info("Commands update failed"));
	}

	@NotNull
	private List<CommandPrivilege> getCommandPrivileges(Guild guild, CommandConfig config) {
		if (config == null || config.getPrivileges() == null) return Collections.emptyList();
		List<CommandPrivilege> privileges = new ArrayList<>();
		for (var privilegeConfig : config.getPrivileges()) {
			privileges.add(privilegeConfig.toData(guild, Bot.config));
			log.info("\t[{}] Registering privilege: {}", config.getName(), privilegeConfig);
		}
		return privileges;
	}

	private CommandConfig findCommandConfig(String name, CommandConfig[] configs) {
		for (CommandConfig config : configs) {
			if (name.equals(config.getName())) {
				return config;
			}
		}
		log.warn("Could not find CommandConfig for command: {}", name);
		return null;
	}
}
