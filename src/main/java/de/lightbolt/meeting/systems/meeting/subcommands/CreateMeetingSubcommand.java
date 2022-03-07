package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.SystemsConfig;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.Constants;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>/meeting create</p>
 * Command that allows users to create new Meetings.
 */
public class CreateMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, SystemsConfig.MeetingConfig config, MeetingRepository repo) throws SQLException {
		var meetingLocale = locale.getMeeting().getCreation();
		if (!canCreateMeetings(event.getMember())) {
			return Responses.error(event, meetingLocale.getCREATION_NOT_PERMITTED_DESCRIPTION());
		}
		if (repo.getByUserId(event.getUser().getIdLong()).size() > config.getMaxMeetingsPerUser() + 1) {
			return Responses.error(event, meetingLocale.getCREATION_TOO_MANY_MEETINGS_DESCRIPTION());
		}
		this.buildCreateModal(event, locale, Language.fromLocale(event.getUserLocale())).queue();
		return null;
	}

	private ModalCallbackAction buildCreateModal(SlashCommandInteractionEvent event, LocaleConfig locale, Language language) {
		var createLocale = locale.getMeeting().getCreation();
		TextInput meetingName = TextInput.create("meeting-name", createLocale.getCREATION_NAME_LABEL(), TextInputStyle.SHORT)
				.setRequired(true)
				.setMaxLength(64)
				.build();

		TextInput meetingDescription = TextInput.create("meeting-description", createLocale.getCREATION_DESCRIPTION_LABEL(), TextInputStyle.PARAGRAPH)
				.setRequired(true)
				.setMaxLength(256)
				.build();

		TextInput meetingDate = TextInput.create("meeting-date", createLocale.getCREATION_DATE_LABEL(), TextInputStyle.SHORT)
				.setValue(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern(Constants.DATETIME_FORMAT)))
				.setPlaceholder(createLocale.getCREATION_DATE_PLACEHOLDER())
				.setRequired(true)
				.setMaxLength(17)
				.build();

		TextInput meetingTimezone = TextInput.create("meeting-timezone", createLocale.getCREATION_TIMEZONE_LABEL(), TextInputStyle.SHORT)
				.setValue(language.getTimezone())
				.setPlaceholder(createLocale.getCREATION_TIMEZONE_PLACEHOLDER())
				.setRequired(false)
				.build();

		TextInput meetingLanguage = TextInput.create("meeting-language", createLocale.getCREATION_LANGUAGE_LABEL(), TextInputStyle.SHORT)
				.setValue(Language.fromLocale(event.getUserLocale()).toString())
				.setPlaceholder(locale.getMeeting().getEdit().getEDIT_LANGUAGE_PLACEHOLDER())
				.setRequired(true)
				.setMaxLength(2)
				.build();
		Modal modal = Modal.create("meeting-create", createLocale.getCREATION_MODAL_HEADER())
				.addActionRows(ActionRow.of(meetingName), ActionRow.of(meetingDescription),
						ActionRow.of(meetingDate), ActionRow.of(meetingTimezone), ActionRow.of(meetingLanguage))
				.build();
		return event.replyModal(modal);
	}

	private boolean canCreateMeetings(Member member) {
		return !member.getUser().isSystem() && !member.getUser().isBot() && !member.isPending() && !member.isTimedOut();
	}
}
