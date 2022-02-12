package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;

public class EditMeetingSubcommand extends MeetingSubcommand {

    @Override
    protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {

        OptionMapping idOption = event.getOption("meeting-id");

        TextInput meetingName = TextInput.create("meetingName", "Meeting Name", TextInputStyle.SHORT)
                .setRequired(false)
                .setMaxLength(64)
                .build();

        TextInput meetingDescription = TextInput.create("meetingDescription", "Meeting Description", TextInputStyle.PARAGRAPH)
                .setRequired(false)
                .setMaxLength(256)
                .build();

        TextInput meetingDate = TextInput.create("meetingDate", "Meeting Date", TextInputStyle.SHORT)
                .setPlaceholder(locale.getMeeting().getEdit().getEDIT_DATE_PLACEHOLDER())
                .setRequired(false)
                .setMaxLength(17)
                .build();

        TextInput meetingLanguage = TextInput.create("meetingLanguage", "Meeting Language", TextInputStyle.SHORT)
                .setPlaceholder(locale.getMeeting().getEdit().getEDIT_LANGUAGE_PLACEHOLDER())
                .setRequired(false)
                .setMaxLength(2)
                .build();

        Modal modal = Modal.create("meetingEdit:" + idOption.getAsLong(), "Edit Meeting")
                .addActionRows(ActionRow.of(meetingName), ActionRow.of(meetingDescription), ActionRow.of(meetingDate), ActionRow.of(meetingLanguage))
                .build();

        event.replyModal(modal).queue();
        return event.deferReply();
    }
}
