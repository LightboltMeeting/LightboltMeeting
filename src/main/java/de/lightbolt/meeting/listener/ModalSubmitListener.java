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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ModalSubmitListener extends ListenerAdapter {

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		String[] id = event.getInteraction().getModalId().split(":");
        event.deferReply(true).queue();
		switch (id[0]) {
			case "meeting-edit" -> handleMeetingEdit(event, Integer.parseInt(id[1]), LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()))).queue();
			default -> Responses.error(event.getHook(), "").queue();
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
						Arrays.stream(Language.values()).map(Language::toString).collect(Collectors.joining(", "))));
			}
			meeting.setLanguage(language);

			repo.updateLanguage(meeting, language);
			repo.updateName(meeting, title);
			repo.updateDescription(meeting, description);
			repo.updateDate(meeting, date);

			new MeetingManager(event.getJDA(), meeting).updateMeeting(event.getUser(), LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())));
			return Responses.success(event.getHook(), editLocale.getEDIT_SUCCESS_TITLE(), editLocale.getEDIT_SUCCESS_DESCRIPTION());
		} catch (SQLException exception) {
			log.error("Could not retrieve SQL Connection.", exception);
			return Responses.error(event.getHook(), editLocale.getEDIT_FAILED());
		}
	}
}
