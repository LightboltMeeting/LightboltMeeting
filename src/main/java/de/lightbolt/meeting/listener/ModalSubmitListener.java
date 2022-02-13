package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ModalSubmitListener extends ListenerAdapter {

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		String[] id = event.getInteraction().getModalId().split(":");
		event.deferReply(true).queue();
		var locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
		switch (id[0]) {
			case "meeting-create" -> handleMeetingCreation(event, locale).queue();
			case "meeting-edit" -> handleMeetingEdit(event, Integer.parseInt(id[1]), locale).queue();
			default -> Responses.error(event.getHook(), "").queue();
		}
	}

	private WebhookMessageAction<Message> handleMeetingCreation(ModalInteractionEvent event, LocaleConfig locale) {
		var createLocale = locale.getMeeting().getCreation();
		try (Connection con = Bot.dataSource.getConnection()) {
			var nameOption = event.getValue("meeting-name");
			var descriptionOption = event.getValue("meeting-description");
			var dateOption = event.getValue("meeting-date");
			var languageOption = event.getValue("meeting-language");
			if (nameOption == null || descriptionOption == null || dateOption == null || languageOption == null) {
				return Responses.error(event.getHook(), locale.getCommand().getMISSING_ARGUMENTS());
			}
			Meeting meeting = new Meeting();
			meeting.setGuildId(event.getGuild().getIdLong());
			meeting.setCreatedBy(event.getUser().getIdLong());
			meeting.setParticipants(new long[]{event.getUser().getIdLong()});
			meeting.setAdmins(new long[]{event.getUser().getIdLong()});
			meeting.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
			meeting.setActive(true);

			String title = nameOption.getAsString();
			meeting.setTitle(title);

			String description = descriptionOption.getAsString();
			meeting.setDescription(description);

			String date = dateOption.getAsString();
			var dueAt = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
			if (dueAt.isBefore(LocalDateTime.now()) || dueAt.isAfter(LocalDateTime.now().plusYears(2))) {
				return Responses.error(event.getHook(), createLocale.getCREATION_INVALID_DATE());
			}
			meeting.setDueAt(Timestamp.valueOf(dueAt));

			String language = languageOption.getAsString();
			if (!Language.isValidLanguage(language)) {
				return Responses.error(event.getHook(), String.format(createLocale.getCREATION_INVALID_LANGUAGE(), language,
						Arrays.stream(Language.values()).map(
								lang -> String.format("%s (%s)", lang.toString(), lang.getName())
						).collect(Collectors.joining(", "))));
			}
			meeting.setLanguage(language);
			var inserted = new MeetingRepository(con).insert(meeting);
			var category = Bot.config.get(event.getGuild()).getMeeting().getMeetingCategory();
			category.createTextChannel(String.format(MeetingManager.MEETING_LOG_NAME, inserted.getId())).queue(
					channel -> {
						channel.getManager().putMemberPermissionOverride((event.getUser().getIdLong()),
								Permission.getRaw(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), 0).queue();
						channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(inserted, event.getUser(), locale)).queue();
						try (var newCon = Bot.dataSource.getConnection()) {
							var repo = new MeetingRepository(newCon);
							repo.updateLogChannel(inserted, channel.getIdLong());
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}, e -> log.error("Could not create Log Channel for Meeting: " + meeting, e));
			category.createVoiceChannel(String.format(MeetingManager.MEETING_VOICE_NAME, inserted.getId(), inserted.getTitle())).queue(
					channel -> {
						channel.getManager().putMemberPermissionOverride(event.getUser().getIdLong(),
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
			return Responses.success(event.getHook(),
					createLocale.getCREATION_SUCCESS_TITLE(), String.format(createLocale.getCREATION_SUCCESS_DESCRIPTION(), inserted.getId()));
		} catch (SQLException exception) {
			log.error("Could not retrieve SQL Connection.", exception);
			return Responses.error(event.getHook(), createLocale.getCREATION_FAILED());
		}
	}

	private WebhookMessageAction<Message> handleMeetingEdit(ModalInteractionEvent event, int meetingId, LocaleConfig locale) {
		var editLocale = locale.getMeeting().getEdit();
		try (Connection con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			Optional<Meeting> meetingOptional = repo.findById(meetingId);
			if (meetingOptional.isEmpty()) {
				return Responses.error(event.getHook(), String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), meetingId));
			}
			Meeting meeting = meetingOptional.get();
			var nameOption = event.getValue("meeting-name");
			var descriptionOption = event.getValue("meeting-description");
			var dateOption = event.getValue("meeting-date");
			var languageOption = event.getValue("meeting-language");
			if (nameOption == null || descriptionOption == null || dateOption == null || languageOption == null) {
				return Responses.error(event.getHook(), locale.getCommand().getMISSING_ARGUMENTS());
			}
			String title = nameOption.getAsString();
			meeting.setTitle(title);

			String description = descriptionOption.getAsString();
			meeting.setDescription(description);

			String date = dateOption.getAsString();
			var dueAt = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
			if (dueAt.isBefore(LocalDateTime.now()) || dueAt.isAfter(LocalDateTime.now().plusYears(2))) {
				return Responses.error(event.getHook(), editLocale.getEDIT_INVALID_DATE());
			}
			meeting.setDueAt(Timestamp.valueOf(dueAt));

			String language = languageOption.getAsString();
			if (!Language.isValidLanguage(language)) {
				return Responses.error(event.getHook(), String.format(editLocale.getEDIT_INVALID_LANGUAGE(), language,
						Arrays.stream(Language.values()).map(
								lang -> String.format("%s (%s)", lang.toString(), lang.getName())
						).collect(Collectors.joining(", "))));
			}
			meeting.setLanguage(language);

			repo.updateLanguage(meeting, language);
			repo.updateName(meeting, title);
			repo.updateDescription(meeting, description);
			repo.updateDate(meeting, date);

			new MeetingManager(event.getJDA(), meeting).updateMeeting(event.getUser(), LocalizationUtils.getLocale(meeting.getLanguage()));
			return Responses.success(event.getHook(), editLocale.getEDIT_SUCCESS_TITLE(), editLocale.getEDIT_SUCCESS_DESCRIPTION());
		} catch (SQLException exception) {
			log.error("Could not retrieve SQL Connection.", exception);
			return Responses.error(event.getHook(), editLocale.getEDIT_FAILED());
		}
	}
}
