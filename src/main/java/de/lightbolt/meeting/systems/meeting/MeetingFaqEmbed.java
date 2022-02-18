package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.annotations.MissingLocale;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

@RequiredArgsConstructor
public class MeetingFaqEmbed {
	private final LocaleConfig config;

	@MissingLocale
	public MessageEmbed build() {
		return new EmbedBuilder()
				.setTitle("Meeting FAQ (Frequently Asked Questions")
				.addField("When does this Meeting start?", "This Meeting will start at exactly <t:%s:R>.", false)
				.addField("How do I change the Meeting's Title/Description/Language/...?",
						"The Meeting Owner and all Meeting Administrators are able to always edit the Meeting using `/meeting edit`.", false)
				.addField("What is a Meeting Administrator?",
						"Meeting Administrators can be added via the `/meeting add-admin` command and are able to manually add or remove participants, edit the Meeting, or just start/end it manually.", false)
				.build();
	}
}
