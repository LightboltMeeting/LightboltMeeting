package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ListMeetingsSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		var meetings = repo.getByUserId(event.getUser().getIdLong());
		return event.replyEmbeds(buildMeetingListEmbed(event.getUser(), locale.getMeeting().getCommand(), meetings));
	}

	private MessageEmbed buildMeetingListEmbed(User user, LocaleConfig.MeetingConfig.MeetingCommandConfig locale, List<Meeting> meetings) throws SQLException {
		var embed = new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle(String.format(locale.getLIST_REPLY_TEXT(), meetings.size()));
		for (var m : meetings) {
			embed.addField(String.format("#%s — %s", m.getId(), m.getTitle()),
					String.format("\"%s\"\n\n%s: %s", m.getDescription(), locale.getLIST_PARTICIPANTS(),
							Arrays.stream(m.getParticipants())
									.mapToObj(user.getJDA()::getUserById)
									.filter(Objects::nonNull)
									.map(User::getAsMention)
									.collect(Collectors.joining(" "))),
					false);
		}
		return embed.build();
	}
}
