package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
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
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class EditMeetingSubcommand extends MeetingSubcommand {

	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		OptionMapping idOption = event.getOption("meeting-id");
		if (idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		int id = (int) idOption.getAsLong();
		Optional<Meeting> meetingOptional = repo.findById(id);
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), id));
		}
		Meeting meeting = meetingOptional.get();
		if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
			return Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION());
		}
        buildModal(event, meeting, locale).queue();
		return null;
	}

    public ModalCallbackAction buildModal(SlashCommandInteractionEvent event, Meeting meeting, LocaleConfig locale) {
		var editLocale = locale.getMeeting().getEdit();
        TextInput meetingName = TextInput.create("meeting-name", editLocale.getEDIT_NAME_LABEL(), TextInputStyle.SHORT)
                .setValue(meeting.getTitle())
                .setRequired(false)
                .setMaxLength(64)
                .build();

        TextInput meetingDescription = TextInput.create("meeting-description", editLocale.getEDIT_DESCRIPTION_LABEL(), TextInputStyle.PARAGRAPH)
                .setValue(meeting.getDescription())
                .setRequired(false)
                .setMaxLength(256)
                .build();

        TextInput meetingDate = TextInput.create("meeting-date", editLocale.getEDIT_DATE_LABEL(), TextInputStyle.SHORT)
                .setValue(meeting.getDueAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setPlaceholder(locale.getMeeting().getEdit().getEDIT_DATE_PLACEHOLDER())
                .setRequired(false)
                .setMaxLength(17)
                .build();

        TextInput meetingLanguage = TextInput.create("meeting-language", editLocale.getEDIT_LANGUAGE_LABEL(), TextInputStyle.SHORT)
                .setValue(meeting.getLanguage())
                .setPlaceholder(locale.getMeeting().getEdit().getEDIT_LANGUAGE_PLACEHOLDER())
                .setRequired(false)
                .setMaxLength(2)
                .build();

        Modal modal = Modal.create("meeting-edit:" + meeting.getId(), editLocale.getEDIT_MODAL_HEADER())
                .addActionRows(ActionRow.of(meetingName), ActionRow.of(meetingDescription), ActionRow.of(meetingDate), ActionRow.of(meetingLanguage))
                .build();
       return event.replyModal(modal);
    }
}
