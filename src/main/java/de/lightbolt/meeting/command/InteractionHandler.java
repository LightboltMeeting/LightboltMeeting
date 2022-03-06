package de.lightbolt.meeting.command;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.data.CommandDataLoader;
import de.lightbolt.meeting.command.data.context_commands.ContextCommandConfig;
import de.lightbolt.meeting.command.data.slash_commands.SlashCommandConfig;
import de.lightbolt.meeting.command.data.slash_commands.SlashCommandPrivilegeConfig;
import de.lightbolt.meeting.command.interfaces.IMessageContextCommand;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import de.lightbolt.meeting.command.interfaces.IUserContextCommand;
import de.lightbolt.meeting.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This listener is responsible for handling slash commands sent by users in
 * guilds where the bot is active, and responding to them by calling the
 * appropriate {@link ISlashCommand}.
 * <p>
 * The list of valid commands, and their associated handlers, are defined in
 * their corresponding YAML-file under the resources/commands directory.
 * </p>
 */
@Slf4j
public class InteractionHandler extends ListenerAdapter {

	/**
	 * Maps every command name and alias to an instance of the command, for
	 * constant-time lookup.
	 */
	private final Map<String, ISlashCommand> slashCommandIndex;

	private final Map<String, IUserContextCommand> userContextCommandIndex;
	private final Map<String, IMessageContextCommand> messageContextCommandIndex;

	/**
	 * Constructor of this class.
	 */
	public InteractionHandler() {
		this.slashCommandIndex = new HashMap<>();
		this.userContextCommandIndex = new HashMap<>();
		this.messageContextCommandIndex = new HashMap<>();
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) return;
		var command = this.slashCommandIndex.get(event.getName());
		if (command != null) {
			try {
				var interaction = command.handleSlashCommandInteraction(event);
				if (interaction != null) interaction.queue();
			} catch (ResponseException e) {
				handleResponseException(e, event);
			}
		}
	}

	@Override
	public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
		if (event.getGuild() == null) return;
		var command = this.userContextCommandIndex.get(event.getName());
		if (command != null) {
			try {
				command.handleUserContextCommandInteraction(event).queue();
			} catch (ResponseException e) {
				handleResponseException(e, event);
			}
		}
	}

	@Override
	public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
		if (event.getGuild() == null) return;
		var command = this.messageContextCommandIndex.get(event.getName());
		if (command != null) {
			try {
				command.handleMessageContextCommandInteraction(event).queue();
			} catch (ResponseException e) {
				handleResponseException(e, event);
			}
		}
	}

	private void handleResponseException(ResponseException e, CommandInteraction interaction) {
		switch (e.getType()) {
			case WARNING -> Responses.warning(interaction, e.getMessage()).queue();
			case ERROR -> Responses.error(interaction, e.getMessage()).queue();
		}
		if (e.getCause() != null) {
			e.printStackTrace();
			log.error("An exception occurred when {} issued the **{}** command", interaction.getUser().getAsMention(), interaction.getName());
		}
	}

	/**
	 * Registers all slash commands defined in the set YAML-files for the given guild
	 * so that users can see the commands when they type a "/".
	 * <p>
	 * It does this by attempting to add an entry to {@link InteractionHandler#slashCommandIndex}
	 * whose key is the command name, and whose value is a new instance of
	 * the handler class which the command has specified.
	 * </p>
	 *
	 * @param guild The guild to update commands for.
	 */
	public void registerCommands(Guild guild) {
		SlashCommandConfig[] slashCommandConfigs = CommandDataLoader.loadSlashCommandConfig("commands/commands.yaml");
		var contextConfigs = CommandDataLoader.loadContextCommandConfig(
				"commands/message.yaml",
				"commands/user.yaml"
		);
		var commandUpdateAction = this.updateCommands(slashCommandConfigs, contextConfigs, guild);

		commandUpdateAction.queue(commands -> {
			// Add privileges to the non-custom commands, after the commands have been registered.
			commands.removeIf(cmd -> cmd.getType() != Command.Type.SLASH);
			this.addCommandPrivileges(commands, slashCommandConfigs, guild);
		});
	}

	private CommandListUpdateAction updateCommands(SlashCommandConfig[] slashCommandConfigs, ContextCommandConfig[] contextConfigs, Guild guild) {
		log.info("{}[{}]{} Registering commands", Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
		if (slashCommandConfigs.length > Commands.MAX_SLASH_COMMANDS) {
			throw new IllegalArgumentException(String.format("Cannot add more than %s commands.", Commands.MAX_SLASH_COMMANDS));
		}
		if (Arrays.stream(contextConfigs).filter(p -> p.getEnumType() == Command.Type.USER).count() > Commands.MAX_USER_COMMANDS) {
			throw new IllegalArgumentException(String.format("Cannot add more than %s User Context Commands", Commands.MAX_USER_COMMANDS));
		}
		if (Arrays.stream(contextConfigs).filter(p -> p.getEnumType() == Command.Type.MESSAGE).count() > Commands.MAX_MESSAGE_COMMANDS) {
			throw new IllegalArgumentException(String.format("Cannot add more than %s Message Context Commands", Commands.MAX_MESSAGE_COMMANDS));
		}
		CommandListUpdateAction commandUpdateAction = guild.updateCommands();
		for (SlashCommandConfig config : slashCommandConfigs) {
			if (config.getHandler() != null && !config.getHandler().isEmpty()) {
				try {
					Class<?> handlerClass = Class.forName(config.getHandler());
					this.slashCommandIndex.put(config.getName(), (ISlashCommand) handlerClass.getConstructor().newInstance());
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			} else {
				log.warn("Slash Command \"{}\" does not have an associated handler class. It will be ignored.", config.getName());
			}
			commandUpdateAction.addCommands(config.toData());
		}
		for (ContextCommandConfig config : contextConfigs) {
			if (config.getHandler() != null && !config.getHandler().isEmpty()) {
				try {
					Class<?> handlerClass = Class.forName(config.getHandler());
					if (config.getEnumType() == Command.Type.USER) {
						this.userContextCommandIndex.put(config.getName(), (IUserContextCommand) handlerClass.getConstructor().newInstance());
					} else if (config.getEnumType() == Command.Type.MESSAGE) {
						this.messageContextCommandIndex.put(config.getName(), (IMessageContextCommand) handlerClass.getConstructor().newInstance());
					} else {
						log.warn("Unknown Context Command Type.");
					}
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			} else {
				log.warn("Context Command ({}) \"{}\" does not have an associated handler class. It will be ignored.", config.getEnumType(), config.getName());
			}
			commandUpdateAction.addCommands(config.toData());
		}
		return commandUpdateAction;
	}

	private void addCommandPrivileges(List<Command> commands, SlashCommandConfig[] slashCommandConfigs, Guild guild) {
		log.info("{}[{}]{} Adding command privileges",
				Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);

		Map<String, List<CommandPrivilege>> map = new HashMap<>();
		for (Command command : commands) {
			List<CommandPrivilege> privileges = getCommandPrivileges(guild, findCommandConfig(command.getName(), slashCommandConfigs));
			if (!privileges.isEmpty()) {
				map.put(command.getId(), privileges);
			}
		}

		guild.updateCommandPrivileges(map)
				.queue(success -> log.info("Commands updated successfully"), error -> log.info("Commands update failed"));
	}

	@NotNull
	private List<CommandPrivilege> getCommandPrivileges(Guild guild, SlashCommandConfig config) {
		if (config == null || config.getPrivileges() == null) return Collections.emptyList();
		List<CommandPrivilege> privileges = new ArrayList<>();
		for (SlashCommandPrivilegeConfig privilegeConfig : config.getPrivileges()) {
			privileges.add(privilegeConfig.toData(guild, Bot.config));
			log.info("\t{}[{}]{} Registering privilege: {}",
					Constants.TEXT_WHITE, config.getName(), Constants.TEXT_RESET, privilegeConfig);
		}
		return privileges;
	}

	private SlashCommandConfig findCommandConfig(String name, SlashCommandConfig[] configs) {
		for (SlashCommandConfig config : configs) {
			if (name.equals(config.getName())) {
				return config;
			}
		}
		log.warn("Could not find CommandConfig for command: {}", name);
		return null;
	}
}
