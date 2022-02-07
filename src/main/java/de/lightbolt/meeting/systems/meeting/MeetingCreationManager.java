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
import net.dv8tion.jda.api.entities.*;
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
import java.util.Collections;
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
	private final LocaleConfig locale;
	private LocaleConfig.MeetingConfig.MeetingCreationConfig meetingLocale;

	private int tries = 5;

	public void startMeetingFlow() {
		meetingLocale = locale.getMeeting().getCreation();
		log.info("{} started the Meeting Creation Flow", user.getAsTag());
		Meeting meeting = new Meeting();
		meeting.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		meeting.setCreatedBy(this.user.getIdLong());
		meeting.setParticipants(new long[]{user.getIdLong()});
		meeting.setAdmins(new long[]{user.getIdLong()});
		consumeGuild(user.getMutualGuilds(), meeting);
	}

	private void consumeGuild(List<Guild> mutualGuilds, Meeting meeting) {
		var selectMenu = SelectMenu.create("meeting-dm-guild")
				.setRequiredRange(1, 1)
				.setPlaceholder(meetingLocale.getCREATION_DM_STEP_1_SELECTION_MENU_PLACEHOLDER());
		user.getMutualGuilds().forEach(g -> selectMenu.addOption(
				g.getName(), g.getId(), String.format(meetingLocale.getCREATION_DM_STEP_1_SELECTION_MENU_DESCRIPTION(), g.getMemberCount())));
		channel.sendMessageEmbeds(buildMeetingCreationEmbed(1, meetingLocale,
						String.format(meetingLocale.getCREATION_DM_STEP_1_DESCRIPTION(), channel.getUser().getAsMention(), mutualGuilds.size())))
				.setActionRow(selectMenu.build())
				.queue();

		Bot.waiter.waitForEvent(
				SelectMenuInteractionEvent.class,
				p -> p.getUser().equals(this.user) && p.getComponentId().equals("meeting-dm-guild"),
				c -> {
					var guild = jda.getGuildById(c.getValues().get(0));
					c.reply(String.format(meetingLocale.getCREATION_DM_STEP_1_SUCCESS_EPHEMERAL(), guild.getName())).setEphemeral(true).queue();
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
				.setPlaceholder(meetingLocale.getCREATION_DM_STEP_2_SELECTION_MENU_PLACEHOLDER());
		for (Language language : Language.values()) {
			selectMenu.addOption(
					language.getName(),
					language.name(),
					String.format(meetingLocale.getCREATION_DM_STEP_2_SELECTION_MENU_DESCRIPTION(), language.getName())
			);
		}
		channel.sendMessageEmbeds(buildMeetingCreationEmbed(2, meetingLocale,
						String.format(meetingLocale.getCREATION_DM_STEP_2_DESCRIPTION(), guild.getName())))
				.setActionRow(selectMenu.build())
				.queue();
		Bot.waiter.waitForEvent(
				SelectMenuInteractionEvent.class,
				p -> p.getUser().equals(this.user) && p.getComponentId().equals("meeting-dm-language"),
				c -> {
					var language = Language.valueOf(c.getValues().get(0));
					c.reply(String.format(meetingLocale.getCREATION_DM_STEP_2_SUCCESS_EPHEMERAL(), language.getName())).setEphemeral(true).queue();
					log.info("{} set {} as the Meeting's Primary Language", user.getAsTag(), language.name());
					disableInteractions(c.getMessage());
					meeting.setLanguage(language.name());
					consumeDate(language, meeting);
				}, TIMEOUT_INT, TIMEOUT_UNIT, () -> sendTimeoutMessage(this.channel).queue()
		);
	}

	private void consumeDate(Language language, Meeting meeting) {
		channel.sendMessageEmbeds(buildMeetingCreationEmbed(3, meetingLocale,
				String.format(meetingLocale.getCREATION_DM_STEP_3_DESCRIPTION(), language.getName()))).queue();
		Bot.waiter.waitForEvent(
				MessageReceivedEvent.class,
				p -> p.getAuthor().equals(this.user),
				c -> {
					LocalDateTime dueAt;
					try {
						dueAt = LocalDateTime.parse(c.getMessage().getContentDisplay(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
						if (dueAt.isBefore(LocalDateTime.now()) || dueAt.isAfter(LocalDateTime.now().plusYears(2))) {
							this.channel.sendMessage(meetingLocale.getCREATION_DM_STEP_3_DATE_OUT_OF_RANGE()).queue();
							consumeDate(language, meeting);
							return;
						}
					} catch (DateTimeParseException e) {
						tries--;
						this.channel.sendMessage(String.format(meetingLocale.getCREATION_DM_STEP_3_INVALID_DATE(), tries)).queue();
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
		channel.sendMessageEmbeds(buildMeetingCreationEmbed(4, meetingLocale,
				String.format(meetingLocale.getCREATION_DM_STEP_4_DESCRIPTION(), time.toEpochSecond(ZoneOffset.UTC)))).queue();
		Bot.waiter.waitForEvent(
				MessageReceivedEvent.class,
				p -> p.getAuthor().equals(this.user),
				c -> {
					var title = c.getMessage().getContentRaw();
					if (title.length() > 32) {
						tries--;
						this.channel.sendMessage(String.format(meetingLocale.getCREATION_DM_STEP_4_INVALID_TITLE(), tries)).queue();
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
		channel.sendMessageEmbeds(buildMeetingCreationEmbed(5, meetingLocale,
				String.format(meetingLocale.getCREATION_DM_STEP_5_DESCRIPTION(), meeting.getTitle()))).queue();
		Bot.waiter.waitForEvent(
				MessageReceivedEvent.class,
				p -> p.getAuthor().equals(this.user),
				c -> {
					var description = c.getMessage().getContentRaw();
					if (description.length() > 256) {
						tries--;
						this.channel.sendMessage(String.format(meetingLocale.getCREATION_DM_STEP_5_INVALID_DESCRIPTION(), tries)).queue();
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

	// TODO: Cleanup
	private void consumeMeetingCheck(Meeting meeting) {
		channel.sendMessage(meetingLocale.getCREATION_DM_STEP_6_DESCRIPTION())
				.setEmbeds(MeetingManager.buildMeetingEmbed(meeting, user, locale))
				.setActionRow(
						Button.success("meeting-dm-button:save", meetingLocale.getCREATION_DM_STEP_6_BUTTON_SAVE_MEETING()),
						Button.danger("meeting-dm-button:discard", meetingLocale.getCREATION_DM_STEP_6_BUTTON_CANCEL_MEETING()))
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
						try (var con = Bot.dataSource.getConnection()) {
							Meeting inserted = new MeetingRepository(con).insert(meeting);
							category.createTextChannel(String.format("meeting-%s-log", inserted.getId())).queue(
									channel -> {
										channel.getManager().putMemberPermissionOverride(user.getIdLong(),
												Permission.getRaw(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), 0).queue();
										channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(inserted, user, locale)).queue();
										try (var newCon = Bot.dataSource.getConnection()) {
											var repo = new MeetingRepository(newCon);
											repo.updateLogChannel(inserted, channel.getIdLong());
										} catch (SQLException e) {
											e.printStackTrace();
										}
									}, e -> log.error("Could not create Log Channel for Meeting: " + meeting, e));
							category.createVoiceChannel(String.format("%s — %s", inserted.getId(), inserted.getTitle())).queue(
									channel -> {
										channel.getManager().putMemberPermissionOverride(user.getIdLong(),
												Collections.singleton(Permission.VIEW_CHANNEL),
												Collections.singleton(Permission.VOICE_CONNECT)
										).queue();
										try (var newCon = Bot.dataSource.getConnection()) {
											var repo = new MeetingRepository(newCon);
											repo.updateVoiceChannel(inserted, channel.getIdLong());
										} catch (SQLException e) {
											e.printStackTrace();
										}
									}, e -> log.error("Could not create Voice Channel for Meeting: " + meeting, e));
							c.reply(String.format(meetingLocale.getCREATION_DM_STEP_6_MEETING_SAVED(), inserted.getId())).queue();
							Bot.meetingStateManager.scheduleMeeting(new MeetingRepository(Bot.dataSource.getConnection()).findById(inserted.getId()).get());
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} else {
						c.reply(meetingLocale.getCREATION_DM_STEP_6_PROCESS_CANCELED()).queue();
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

	private MessageEmbed buildMeetingCreationEmbed(int step, LocaleConfig.MeetingConfig.MeetingCreationConfig config, String description) {
		return new EmbedBuilder()
				.setTitle(String.format(config.getCREATION_DM_DEFAULT_EMBED_TITLE(), step, 5))
				.setDescription(description)
				.setFooter(String.format(config.getCREATION_DM_DEFAULT_EMBED_FOOTER(),
						MeetingCreationManager.TIMEOUT_INT,
						MeetingCreationManager.TIMEOUT_UNIT.name()))
				.build();
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
						.setTitle(this.meetingLocale.getCREATION_DM_TIMED_OUT_TITLE())
						.setDescription(this.meetingLocale.getCREATION_DM_TIMED_OUT_DESCRIPTION())
						.build()
		);
	}

	private MessageAction sendTriesExceededMessage(PrivateChannel channel) {
		return channel.sendMessageEmbeds(
				new EmbedBuilder()
						.setTitle(this.meetingLocale.getCREATION_DM_NO_TRIES_LEFT_TITLE())
						.setDescription(this.meetingLocale.getCREATION_DM_NO_TRIES_LEFT_DESCRIPTION())
						.build()
		);
	}
}
