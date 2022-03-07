package de.lightbolt.meeting.systems.meeting.subcommands.manage;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.SystemsConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.Optional;

/**
 * <p>/meeting edit</p>
 * Command that allows Meeting Administrators to edit a single Meeting.
 */
public class EditMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, SystemsConfig.MeetingConfig config, MeetingRepository repo) throws SQLException {
		OptionMapping idOption = event.getOption("meeting-id");
		if (idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		int id = (int) idOption.getAsLong();
		Optional<Meeting> meetingOptional = repo.getById(id);
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), id));
		}
		Meeting meeting = meetingOptional.get();
		if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
			return Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION());
		}
		this.buildEditModal(event, meeting, locale).queue();
		return null;
	}

	private ModalCallbackAction buildEditModal(SlashCommandInteractionEvent event, Meeting meeting, LocaleConfig locale) {
		var editLocale = locale.getMeeting().getEdit();
		TextInput meetingDescription = TextInput.create("meeting-description", editLocale.getEDIT_DESCRIPTION_LABEL(), TextInputStyle.PARAGRAPH)
				.setValue(meeting.getDescription())
				.setRequired(true)
				.setMaxLength(256)
				.build();

		TextInput meetingDate = TextInput.create("meeting-date", editLocale.getEDIT_DATE_LABEL(), TextInputStyle.SHORT)
				.setValue(meeting.getDueAtFormatted())
				.setPlaceholder(locale.getMeeting().getEdit().getEDIT_DATE_PLACEHOLDER())
				.setRequired(true)
				.setMaxLength(16)
				.build();

		TextInput meetingTimezone = TextInput.create("meeting-timezone", editLocale.getEDIT_TIMEZONE_LABEL(), TextInputStyle.SHORT)
				.setValue(meeting.getTimeZoneRaw())
				.setPlaceholder(editLocale.getEDIT_TIMEZONE_PLACEHOLDER())
				.setRequired(false)
				.build();

		TextInput meetingLanguage = TextInput.create("meeting-language", editLocale.getEDIT_LANGUAGE_LABEL(), TextInputStyle.SHORT)
				.setValue(meeting.getLanguage())
				.setPlaceholder(locale.getMeeting().getEdit().getEDIT_LANGUAGE_PLACEHOLDER())
				.setRequired(true)
				.setMaxLength(2)
				.build();

		Modal modal = Modal.create("meeting-edit:" + meeting.getId(), String.format(editLocale.getEDIT_MODAL_HEADER(), meeting.getTitle()))
				.addActionRows(ActionRow.of(meetingDescription), ActionRow.of(meetingDate), ActionRow.of(meetingTimezone), ActionRow.of(meetingLanguage))
				.build();
		return event.replyModal(modal);
	}
}
