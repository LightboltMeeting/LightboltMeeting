package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.annotations.MissingLocale;
import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingStatus;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.Constants;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Slf4j
public class ModalSubmitListener extends ListenerAdapter {

	public static final String TIMEZONE_LIST = "https://en.wikipedia.org/wiki/List_of_tz_database_time_zones";

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		String[] id = event.getInteraction().getModalId().split(":");
		event.deferReply(true).queue();
		LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
		switch (id[0]) {
			case "meeting-create" -> handleMeetingCreation(event, locale);
			case "meeting-edit" -> handleMeetingEdit(event, Integer.parseInt(id[1]), locale);
			default -> Responses.error(event.getHook(), locale.getCommand().getUNKNOWN_INTERACTION()).queue();
		}
	}

	private void handleMeetingCreation(ModalInteractionEvent event, LocaleConfig locale) {
		var createLocale = locale.getMeeting().getCreation();
		ModalMapping nameOption = event.getValue("meeting-name");
		ModalMapping descriptionOption = event.getValue("meeting-description");
		ModalMapping dateOption = event.getValue("meeting-date");
		ModalMapping languageOption = event.getValue("meeting-language");
		ModalMapping timezoneOption = event.getValue("meeting-timezone");
		if (nameOption == null || descriptionOption == null || dateOption == null || languageOption == null) {
			Responses.error(event.getHook(), locale.getCommand().getMISSING_ARGUMENTS()).queue();
			return;
		}
		Meeting meeting = new Meeting();
		meeting.setGuildId(event.getGuild().getIdLong());
		meeting.setCreatedBy(event.getUser().getIdLong());
		meeting.setParticipants(new long[]{event.getUser().getIdLong()});
		meeting.setAdmins(new long[]{event.getUser().getIdLong()});
		meeting.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		meeting.setStatus(MeetingStatus.SCHEDULED);

		// Title
		String title = nameOption.getAsString();
		meeting.setTitle(title);

		// Description
		String description = descriptionOption.getAsString();
		meeting.setDescription(description);

		// Timezone
		String timezoneString = timezoneOption == null ? "UTC" : timezoneOption.getAsString();
		if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(timezoneString)) {
			Responses.error(event.getHook(), String.format(createLocale.getCREATION_INVALID_TIMEZONE(), timezoneString, TIMEZONE_LIST));
			return;
		}
		TimeZone timezone = TimeZone.getTimeZone(timezoneString);
		meeting.setTimeZoneRaw(timezoneString);

		// Timestamp
		String date = dateOption.getAsString();
		ZonedDateTime dueAt;
		try {
			dueAt = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(Constants.DATETIME_FORMAT)).atZone(timezone.toZoneId());
		} catch (DateTimeParseException e) {
			Responses.error(event.getHook(), createLocale.getCREATION_INVALID_DATE()).queue();
			return;
		}
		var zonedDateTimeNow = LocalDateTime.now().atZone(timezone.toZoneId());
		if (dueAt.isBefore(zonedDateTimeNow) || dueAt.isAfter(zonedDateTimeNow.plusYears(2))) {
			Responses.error(event.getHook(), createLocale.getCREATION_INVALID_DATE()).queue();
			return;
		}
		System.out.println(Timestamp.from(dueAt.withZoneSameInstant(ZoneOffset.UTC).toInstant()));
		meeting.setDueAt(Timestamp.from(dueAt.withZoneSameInstant(ZoneOffset.UTC).toInstant()));

		// Language
		String language = languageOption.getAsString();
		if (!Language.isValidLanguage(language)) {
			Responses.error(event.getHook(), String.format(createLocale.getCREATION_INVALID_LANGUAGE(), language,
					Arrays.stream(Language.values()).map(
							lang -> String.format("%s (%s)", lang.toString(), lang.getName())
					).collect(Collectors.joining(", ")))).queue();
			return;
		}
		meeting.setLanguage(language);

		// Insertion
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			var inserted = dao.insert(meeting);
			var config = Bot.config.get(event.getGuild()).getMeeting();
			var manager = new MeetingManager(event.getJDA(), inserted);
			manager.createMeetingChannels(event.getGuild(), event.getUser(), locale, config);
			Bot.meetingStateManager.scheduleMeeting(inserted);
			Responses.success(event.getHook(), createLocale.getCREATION_SUCCESS_TITLE(), String.format(createLocale.getCREATION_SUCCESS_DESCRIPTION(), inserted.getId())).queue();
		});
	}

	private void handleMeetingEdit(ModalInteractionEvent event, int meetingId, LocaleConfig locale) {
		var editLocale = locale.getMeeting().getEdit();
		ModalMapping descriptionOption = event.getValue("meeting-description");
		ModalMapping timezoneOption = event.getValue("meeting-timezone");
		ModalMapping dateOption = event.getValue("meeting-date");
		ModalMapping languageOption = event.getValue("meeting-language");
		if (descriptionOption == null || dateOption == null || languageOption == null) {
			Responses.error(event.getHook(), locale.getCommand().getMISSING_ARGUMENTS()).queue();
			return;
		}

		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			Optional<Meeting> meetingOptional = dao.getById(meetingId);
			if (meetingOptional.isEmpty()) {
				Responses.error(event.getHook(), String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), meetingId)).queue();
				return;
			}
			Meeting meeting = meetingOptional.get();

			// Description
			String description = descriptionOption.getAsString();
			meeting.setDescription(description);

			// Timezone
			String timezoneString = timezoneOption == null ? "UTC" : timezoneOption.getAsString();
			if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(timezoneString)) {
				Responses.error(event.getHook(), String.format(editLocale.getEDIT_INVALID_TIMEZONE(), timezoneString, TIMEZONE_LIST)).queue();
				return;
			}
			TimeZone timezone = TimeZone.getTimeZone(timezoneString);
			meeting.setTimeZoneRaw(timezoneString);

			// Timestamp
			String date = dateOption.getAsString();
			ZonedDateTime dueAt;
			try {
				dueAt = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(Constants.DATETIME_FORMAT)).atZone(timezone.toZoneId());
			} catch (DateTimeParseException e) {
				Responses.error(event.getHook(), editLocale.getEDIT_INVALID_DATE()).queue();
				return;
			}
			var zonedDateTimeNow = LocalDateTime.now().atZone(timezone.toZoneId());
			if (dueAt.isBefore(zonedDateTimeNow) || dueAt.isAfter(zonedDateTimeNow.plusYears(2))) {
				Responses.error(event.getHook(), editLocale.getEDIT_INVALID_DATE()).queue();
				return;
			}
			meeting.setDueAt(Timestamp.from(dueAt.withZoneSameInstant(ZoneOffset.UTC).toInstant()));

			// Language
			String language = languageOption.getAsString();
			if (!Language.isValidLanguage(language)) {
				Responses.error(event.getHook(), String.format(editLocale.getEDIT_INVALID_LANGUAGE(), language,
						Arrays.stream(Language.values()).map(
								lang -> String.format("%s (%s)", lang.toString(), lang.getName())
						).collect(Collectors.joining(", ")))).queue();
				return;
			}
			meeting.setLanguage(language);

			// Update
			dao.update(meeting.getId(), meeting);
			MeetingManager manager = new MeetingManager(event.getJDA(), meeting);
			manager.updateMeeting(event.getUser(), Bot.config.get(event.getGuild()).getMeeting(), LocalizationUtils.getLocale(meeting.getLanguage()));
			Bot.meetingStateManager.updateMeetingSchedule(meeting);
			Responses.success(event.getHook(), editLocale.getEDIT_SUCCESS_TITLE(), editLocale.getEDIT_SUCCESS_DESCRIPTION()).queue();
		});
	}
}