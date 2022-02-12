package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalSubmitInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

@Slf4j
public class ModalSubmitListener extends ListenerAdapter {

    @Override
    public void onModalSubmitInteraction(@NotNull ModalSubmitInteractionEvent event) {
        String[] args = event.getInteraction().getModalId().split(":");
        String modalID = args[0];

        if (modalID.equals("meetingEdit")) {
            String meetingID = args[1];
            System.out.println(args);
            System.out.println(meetingID);
            Meeting meeting = null;
            Connection con = null;
            try {
                con = Bot.dataSource.getConnection();
                meeting = new MeetingRepository(con).findById(Integer.parseInt(meetingID)).get();
            } catch (SQLException exception) {
                log.error("Could not retrieve SQL Connection.", exception);
            }
            String meetingName = event.getTextInputField("meetingName").getValue();
            if (!meetingName.isEmpty()) {
                new MeetingRepository(con).updateName(meeting, meetingName);
            }
            String meetingDescription = event.getTextInputField("meetingDescription").getValue();
            if (!meetingDescription.isEmpty()) {
                new MeetingRepository(con).updateDescription(meeting, meetingDescription);
            }
            String meetingDate = event.getTextInputField("meetingDate").getValue();
            if (!meetingDate.isEmpty()) {
                new MeetingRepository(con).updateDate(meeting, meetingDate);
            }
            String meetingLanguage = event.getTextInputField("meetingLanguage").getValue();
            if (!meetingLanguage.isEmpty()) {
                new MeetingRepository(con).updateLanguage(meeting, meetingLanguage);
            }

            event.reply("Meeting successfully updated").setEphemeral(true).queue();
        } else {
            event.reply("Unknown modal.").queue();
        }
    }
}
