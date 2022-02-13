package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.annotations.MissingLocale;
import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
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
import java.util.Optional;

@Slf4j
public class ModalSubmitListener extends ListenerAdapter {

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		String[] id = event.getInteraction().getModalId().split(":");
        event.deferReply(true).queue();
		switch (id[0]) {
			case "meeting-edit" -> handleMeetingEdit(event, Integer.parseInt(id[1])).queue();
			default -> Responses.error(event.getHook(), "").queue();
		}
	}

	@MissingLocale
	private WebhookMessageAction<Message> handleMeetingEdit(ModalInteractionEvent event, int meetingId) {
		try (Connection con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			Optional<Meeting> meetingOptional = repo.findById(meetingId);
			if (meetingOptional.isEmpty()) {
				return Responses.error(event.getHook(), "Unknown Meeting");
			}
			Meeting meeting = meetingOptional.get();
			var nameOption = event.getValue("meeting-name");
			var descriptionOption = event.getValue("meeting-description");
			var dateOption = event.getValue("meeting-date");
			var languageOption = event.getValue("meeting-language");
			if (nameOption == null || descriptionOption == null || dateOption == null || languageOption == null) {
				return Responses.error(event.getHook(), "Missing arguments.");
			}
			String title = nameOption.getAsString();
			meeting.setTitle(title);
			repo.updateName(meeting, title);

			String description = descriptionOption.getAsString();
			meeting.setDescription(description);
			repo.updateDescription(meeting, description);

			String date = dateOption.getAsString();
			meeting.setDueAt(Timestamp.valueOf(LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
			repo.updateDate(meeting, date);

			String language = languageOption.getAsString();
			meeting.setLanguage(language);
			repo.updateLanguage(meeting, language);

			new MeetingManager(event.getJDA(), meeting).updateMeeting(event.getUser());
			return Responses.success(event.getHook(), "MODAL_SUCCESS_TITLE", "MODAL_SUCCESS_DESC");
		} catch (SQLException exception) {
			log.error("Could not retrieve SQL Connection.", exception);
			return Responses.error(event.getHook(), "MODAL_FAIL");
		}
	}
}
