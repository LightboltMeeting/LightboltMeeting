package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MeetingCreationManager {
	public static int TIMEOUT_INT = 60;
	public static TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

	private final JDA jda;
	private final User user;
	private final PrivateChannel channel;
	private final LocaleConfig.MeetingCreationConfig locale;

	private int tries = 5;

	public void startMeetingFlow() {
		log.info("{} started the Meeting Creation Flow", user.getAsTag());
		Meeting meeting = new Meeting();
		meeting.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		meeting.setCreatedBy(this.user.getIdLong());
		consumeGuild(user.getMutualGuilds(), meeting);
	}

	private void consumeGuild(List<Guild> mutualGuilds, Meeting meeting) {
		var selectMenu = SelectMenu.create("meeting-dm-guild")
				.setRequiredRange(1, 1)
				.setPlaceholder(locale.getCREATION_DM_STEP_1_SELECTION_MENU_PLACEHOLDER());
		user.getMutualGuilds().forEach(g -> selectMenu.addOption(
				g.getName(), g.getId(), String.format(locale.getCREATION_DM_STEP_1_SELECTION_MENU_DESCRIPTION(), g.getMemberCount())));
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(1, locale,
						String.format(locale.getCREATION_DM_STEP_1_DESCRIPTION(), channel.getUser().getAsMention(), mutualGuilds.size())))
				.setActionRow(selectMenu.build())
				.queue();

		Bot.waiter.waitForEvent(
				SelectMenuInteractionEvent.class,
				p -> p.getUser().equals(this.user) && p.getComponentId().equals("meeting-dm-guild"),
				c -> {
					var guild = jda.getGuildById(c.getValues().get(0));
					c.reply(String.format(locale.getCREATION_DM_STEP_1_SUCCESS_EPHEMERAL(), guild.getName())).setEphemeral(true).queue();
					log.info("{} set {} as the Meeting's Guild", user.getAsTag(), guild.getName());
					disableInteractions(c.getMessage());
					meeting.setGuildId(guild.getIdLong());
					consumeLanguage(guild, meeting);
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue()
		);
	}

	private void consumeLanguage(Guild guild, Meeting meeting) {
		var selectMenu = SelectMenu.create("meeting-dm-language")
				.setRequiredRange(1, 1)
				.setPlaceholder(locale.getCREATION_DM_STEP_2_SELECTION_MENU_PLACEHOLDER());
		for (var language : Language.values()) {
			selectMenu.addOption(
					language.getName(),
					language.name(),
					String.format(locale.getCREATION_DM_STEP_2_SELECTION_MENU_DESCRIPTION(), language.getName())
			);
		}
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(2, locale,
						String.format(locale.getCREATION_DM_STEP_2_DESCRIPTION(), guild.getName())))
				.setActionRow(selectMenu.build())
				.queue();
		Bot.waiter.waitForEvent(
				SelectMenuInteractionEvent.class,
				p -> p.getUser().equals(this.user) && p.getComponentId().equals("meeting-dm-language"),
				c -> {
					var language = Language.valueOf(c.getValues().get(0));
					c.reply(String.format(locale.getCREATION_DM_STEP_2_SUCCESS_EPHEMERAL(), language.getName())).setEphemeral(true).queue();
					log.info("{} set {} as the Meeting's Primary Language", user.getAsTag(), language.name());
					disableInteractions(c.getMessage());
					meeting.setLanguage(language.name());
					consumeDate(language, meeting);
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue()
		);
	}

	private void consumeDate(Language language, Meeting meeting) {
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(3, locale,
				String.format(locale.getCREATION_DM_STEP_3_DESCRIPTION(), language.getName()))).queue();
		Bot.waiter.waitForEvent(
				MessageReceivedEvent.class,
				p -> p.getAuthor().equals(this.user),
				c -> {
					LocalDateTime dueAt;
					try {
						dueAt = LocalDateTime.parse(c.getMessage().getContentDisplay(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
					} catch (DateTimeParseException e) {
						tries--;
						this.channel.sendMessage(String.format(locale.getCREATION_DM_STEP_3_INVALID_DATE(), tries)).queue();
						if (tries < 1) {
							sendTriesExceededMessage(this.channel).queue();
						} else {
							consumeDate(language, meeting);
						}
						return;
					}
					log.info("{} set {} as the Meeting's Start Date", user.getAsTag(), dueAt);
					meeting.setDueAt(Timestamp.valueOf(dueAt));
					consumeTitle(dueAt, meeting);
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue());
	}

	private void consumeTitle(LocalDateTime time, Meeting meeting) {
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(4, locale,
				String.format(locale.getCREATION_DM_STEP_4_DESCRIPTION(), time.toEpochSecond(ZoneOffset.UTC)))).queue();
		Bot.waiter.waitForEvent(
				MessageReceivedEvent.class,
				p -> p.getAuthor().equals(this.user),
				c -> {
					var title = c.getMessage().getContentRaw();
					if (title.length() > 64) {
						tries--;
						this.channel.sendMessage(String.format(locale.getCREATION_DM_STEP_4_INVALID_TITLE(), tries)).queue();
						if (tries < 1) {
							sendTriesExceededMessage(this.channel).queue();
						} else {
							consumeTitle(time, meeting);
						}
						return;
					}
					log.info("{} set \"{}\" as the Meeting's Title", user.getAsTag(), title);
					meeting.setTitle(title);
					consumeDescription(meeting);
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue());
	}

	private void consumeDescription(Meeting meeting) {
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(5, locale,
				String.format(locale.getCREATION_DM_STEP_5_DESCRIPTION(), meeting.getTitle()))).queue();
		Bot.waiter.waitForEvent(
				MessageReceivedEvent.class,
				p -> p.getAuthor().equals(this.user),
				c -> {
					var description = c.getMessage().getContentRaw();
					if (description.length() > 256) {
						tries--;
						this.channel.sendMessage(String.format(locale.getCREATION_DM_STEP_5_INVALID_DESCRIPTION(), tries)).queue();
						if (tries < 1) {
							sendTriesExceededMessage(this.channel).queue();
						} else {
							consumeDescription(meeting);
						}
						return;
					}
					log.info("{} set \"{}\" as the Meeting's Description", user.getAsTag(), description);
					meeting.setDescription(description);
					consumeMeetingCheck(meeting);
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue());
	}

	private void consumeMeetingCheck(Meeting meeting) {
		channel.sendMessage(locale.getCREATION_DM_STEP_6_DESCRIPTION())
				.setEmbeds(
						new EmbedBuilder()
								.setAuthor(this.user.getAsTag(), null, this.user.getEffectiveAvatarUrl())
								.setTitle(meeting.getTitle())
								.setDescription(meeting.getDescription())
								.setFooter(locale.getCREATION_DM_STEP_6_FOOTER())
								.setTimestamp(meeting.getDueAt().toLocalDateTime())
								.build())
				.setActionRow(
						Button.success("meeting-dm-button:save", locale.getCREATION_DM_STEP_6_BUTTON_SAVE_MEETING()),
						Button.danger("meeting-dm-button:discard", locale.getCREATION_DM_STEP_6_BUTTON_CANCEL_MEETING()))
				.queue();
		Bot.waiter.waitForEvent(
				ButtonInteractionEvent.class,
				p -> p.getUser().equals(this.user) && p.getComponentId().contains("meeting-dm-button"),
				c -> {
					disableInteractions(c.getMessage());
					var id = c.getComponentId().split(":");
					if (id[1].equals("save")) {
						var guild = jda.getGuildById(meeting.getGuildId());
						var category = Bot.config.get(guild).getMeeting().getMeetingCategory();
						category.createTextChannel(String.format("%s-log", meeting.getTitle())).queue(
								text -> {
									text.getManager().putRolePermissionOverride(guild.getIdLong(), 0, Permission.ALL_PERMISSIONS)
											.putMemberPermissionOverride(user.getIdLong(), Permission.ALL_PERMISSIONS, 0)
											.queue();
									category.createVoiceChannel(String.format("%s", meeting.getTitle())).queue(
											voice -> {
												voice.getManager().putRolePermissionOverride(guild.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
												meeting.setLogChannelId(text.getIdLong());
												meeting.setVoiceChannelId(voice.getIdLong());
												try (var con = Bot.dataSource.getConnection()) {
													new MeetingRepository(con).insert(meeting);
												} catch (SQLException e) {
													e.printStackTrace();
												}
												c.reply(String.format(locale.getCREATION_DM_STEP_6_MEETING_SAVED(), text.getAsMention())).queue();
											}, e -> log.error("Could not create Voice Channel for meeting: " + meeting, e)
									);
								}, e -> log.error("Could not create Log Channel for meeting: " + meeting, e)
						);
					} else {
						c.reply(locale.getCREATION_DM_STEP_6_PROCESS_CANCELED()).queue();
						log.info("{} canceled the Meeting Creation Flow", user.getAsTag());
					}
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue());
	}

	private void disableInteractions(Message message) {
		message.editMessageComponents(message.getActionRows()
				.stream()
				.map(ActionRow::asDisabled)
				.collect(Collectors.toList())
		).queue();
	}

	private MessageAction sendTimeoutMessage(PrivateChannel channel) {
		var history = channel.getHistory();
		history.retrievePast(100).queue(
				messages -> {
					var list = messages.stream().filter(message -> !message.getAuthor().equals(jda.getSelfUser())).toList();
					list.forEach(this::disableInteractions);
				},
				e -> log.error("Could not retrieve messages from Private Channel")
		);
		return channel.sendMessageEmbeds(
				new EmbedBuilder()
						.setTitle(this.locale.getCREATION_DM_TIMED_OUT_TITLE())
						.setDescription(this.locale.getCREATION_DM_TIMED_OUT_DESCRIPTION())
						.build()
		);
	}

	private MessageAction sendTriesExceededMessage(PrivateChannel channel) {
		return channel.sendMessageEmbeds(
				new EmbedBuilder()
						.setTitle(this.locale.getCREATION_DM_NO_TRIES_LEFT_TITLE())
						.setDescription(this.locale.getCREATION_DM_NO_TRIES_LEFT_DESCRIPTION())
						.build()
		);
	}
}
