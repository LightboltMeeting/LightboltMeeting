package de.lightbolt.meetingtest;

import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalizationTests {

	@Test
	public static void main(String[] args) {
		for (var language : Language.values()) {
			StringBuilder sb = new StringBuilder(String.format("\n--------- %s (%s) ---------\nUsing file: %s\n", language.getName(), language, language.getPath()));
			var config = LocalizationUtils.getLocale(language);
			// COMMAND
			sb.append("\nCOMMAND:MISSING_ARGUMENTS: " + config.getCommand().getMISSING_ARGUMENTS());

			// MEETING
			sb.append("\nMEETING:MEETING_EMBED_FOOTER: " + config.getMeeting().getMEETING_EMBED_FOOTER());
			sb.append("\nMEETING:MEETING_NO_PERMISSION: " + config.getMeeting().getMEETING_NO_PERMISSION());

			// MEETING:CREATION
			var creation = config.getMeeting().getCreation();
			sb.append("\nMEETING:CREATION:CREATION_TOO_MANY_MEETINGS_DESCRIPTION: " + creation.getCREATION_TOO_MANY_MEETINGS_DESCRIPTION());
			sb.append("\nMEETING:CREATION:CREATION_NOT_PERMITTED_DESCRIPTION: " + creation.getCREATION_NOT_PERMITTED_DESCRIPTION());
			sb.append("\nMEETING:CREATION:CREATION_DATE_PLACEHOLDER " + creation.getCREATION_DATE_PLACEHOLDER());
			sb.append("\nMEETING:CREATION:CREATION_LANGUAGE_PLACEHOLDER: " + creation.getCREATION_LANGUAGE_PLACEHOLDER());
			sb.append("\nMEETING:CREATION:CREATION_MODAL_HEADER: " + creation.getCREATION_MODAL_HEADER());
			sb.append("\nMEETING:CREATION:CREATION_NAME_LABEL: " + creation.getCREATION_NAME_LABEL());
			sb.append("\nMEETING:CREATION:CREATION_DESCRIPTION_LABEL: " + creation.getCREATION_DESCRIPTION_LABEL());
			sb.append("\nMEETING:CREATION:CREATION_DATE_LABEL: " + creation.getCREATION_DATE_LABEL());
			sb.append("\nMEETING:CREATION:CREATION_LANGUAGE_LABEL: " + creation.getCREATION_LANGUAGE_LABEL());
			sb.append("\nMEETING:CREATION:CREATION_INVALID_DATE: " + creation.getCREATION_INVALID_DATE());
			sb.append("\nMEETING:CREATION:CREATION_INVALID_LANGUAGE: " + creation.getCREATION_INVALID_LANGUAGE());
			sb.append("\nMEETING:CREATION:CREATION_SUCCESS_TITLE: " + creation.getCREATION_SUCCESS_TITLE());
			sb.append("\nMEETING:CREATION:CREATION_SUCCESS_DESCRIPTION: " + creation.getCREATION_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:CREATION:CREATION_FAILED: " + creation.getCREATION_FAILED());

			// MEETING:COMMAND
			var command = config.getMeeting().getCommand();
			sb.append("\nMEETING:COMMAND:MEETING_NOT_FOUND: " + command.getMEETING_NOT_FOUND());
			sb.append("\nMEETING:COMMAND:MEETING_PARTICIPANT_ALREADY_ADDED: " + command.getMEETING_PARTICIPANT_ALREADY_ADDED());
			sb.append("\nMEETING:COMMAND:MEETING_PARTICIPANT_NOT_FOUND: " + command.getMEETING_PARTICIPANT_NOT_FOUND());
			sb.append("\nMEETING:COMMAND:MEETING_ADMIN_ALREADY_ADDED: " + command.getMEETING_ADMIN_ALREADY_ADDED());
			sb.append("\nMEETING:COMMAND:MEETING_ADMIN_NOT_FOUND: " + command.getMEETING_ADMIN_NOT_FOUND());
			sb.append("\nMEETING:COMMAND:MEETING_ADMIN_NOT_A_PARTICIPANT: " + command.getMEETING_ADMIN_NOT_A_PARTICIPANT());
			sb.append("\nMEETING:COMMAND:LIST_REPLY_TEXT: " + command.getLIST_REPLY_TEXT());
			sb.append("\nMEETING:COMMAND:LIST_PARTICIPANTS: " + command.getLIST_PARTICIPANTS());
			sb.append("\nMEETING:COMMAND:CANCEL_MEETING_TITLE: " + command.getCANCEL_MEETING_TITLE());
			sb.append("\nMEETING:COMMAND:CANCEL_MEETING_DESCRIPTION: " + command.getCANCEL_MEETING_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:MEETING_DISCARD_FAILED_DESCRIPTION: " + command.getMEETING_DISCARD_FAILED_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:PARTICIPANTS_ADD_SUCCESS_TITLE: " + command.getPARTICIPANTS_ADD_SUCCESS_TITLE());
			sb.append("\nMEETING:COMMAND:PARTICIPANTS_ADD_SUCCESS_DESCRIPTION: " + command.getPARTICIPANTS_ADD_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:PARTICIPANTS_REMOVE_SUCCESS_TITLE: " + command.getPARTICIPANTS_REMOVE_SUCCESS_TITLE());
			sb.append("\nMEETING:COMMAND:PARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION: " + command.getPARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:ADMINS_ADD_SUCCESS_TITLE: " + command.getADMINS_ADD_SUCCESS_TITLE());
			sb.append("\nMEETING:COMMAND:ADMINS_ADD_SUCCESS_DESCRIPTION: " + command.getADMINS_ADD_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:ADMINS_REMOVE_SUCCESS_TITLE: " + command.getADMINS_REMOVE_SUCCESS_TITLE());
			sb.append("\nMEETING:COMMAND:ADMINS_REMOVE_SUCCESS_DESCRIPTION: " + command.getADMINS_REMOVE_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:MEETING_START_SUCCESS_TITLE: " + command.getMEETING_START_SUCCESS_TITLE());
			sb.append("\nMEETING:COMMAND:MEETING_START_SUCCESS_DESCRIPTION: " + command.getMEETING_START_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:MEETING_END_SUCCESS_TITLE: " + command.getMEETING_END_SUCCESS_TITLE());
			sb.append("\nMEETING:COMMAND:MEETING_END_SUCCESS_DESCRIPTION: " + command.getMEETING_END_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:COMMAND:MEETING_END_FAILED_DESCRIPTION: " + command.getMEETING_END_FAILED_DESCRIPTION());

			// MEETING:LOG
			var log = config.getMeeting().getLog();
			sb.append("\nMEETING:LOG:LOG_PARTICIPANT_ADDED: " + log.getLOG_PARTICIPANT_ADDED());
			sb.append("\nMEETING:LOG:LOG_PARTICIPANT_REMOVED: " + log.getLOG_PARTICIPANT_REMOVED());
			sb.append("\nMEETING:LOG:LOG_ADMIN_ADDED: " + log.getLOG_ADMIN_ADDED());
			sb.append("\nMEETING:LOG:LOG_ADMIN_REMOVED: " + log.getLOG_ADMIN_REMOVED());
			sb.append("\nMEETING:LOG:LOG_REMINDER_TITLE: " + log.getLOG_REMINDER_TITLE());
			sb.append("\nMEETING:LOG:LOG_REMINDER_DESCRIPTION: " + log.getLOG_REMINDER_DESCRIPTION());
			sb.append("\nMEETING:LOG:LOG_START_TITLE: " + log.getLOG_START_TITLE());
			sb.append("\nMEETING:LOG:LOG_START_DESCRIPTION: " + log.getLOG_START_DESCRIPTION());
			sb.append("\nMEETING:LOG:LOG_TIMEUNIT_MINUTES: " + log.getLOG_TIMEUNIT_MINUTES());
			sb.append("\nMEETING:LOG:LOG_TIMEUNIT_HOURS: " + log.getLOG_TIMEUNIT_HOURS());
			sb.append("\nMEETING:LOG:LOG_MEETING_UPDATED: " + log.getLOG_MEETING_UPDATED());
			sb.append("\nMEETING:LOG:LOG_MEETING_MANUALLY_STARTED: " + log.getLOG_MEETING_MANUALLY_STARTED());
			sb.append("\nMEETING:LOG:LOG_MEETING_STARTED: " + log.getLOG_MEETING_STARTED());

			// MEETING:EDIT
			var edit = config.getMeeting().getEdit();
			sb.append("\nMEETING:EDIT:EDIT_DATE_PLACEHOLDER: " + edit.getEDIT_DATE_PLACEHOLDER());
			sb.append("\nMEETING:EDIT:EDIT_LANGUAGE_PLACEHOLDER: " + edit.getEDIT_LANGUAGE_PLACEHOLDER());
			sb.append("\nMEETING:EDIT:EDIT_MODAL_HEADER: " + edit.getEDIT_MODAL_HEADER());
			sb.append("\nMEETING:EDIT:EDIT_NAME_LABEL: " + edit.getEDIT_NAME_LABEL());
			sb.append("\nMEETING:EDIT:EDIT_DESCRIPTION_LABEL: " + edit.getEDIT_DESCRIPTION_LABEL());
			sb.append("\nMEETING:EDIT:EDIT_DATE_LABEL: " + edit.getEDIT_DATE_LABEL());
			sb.append("\nMEETING:EDIT:EDIT_LANGUAGE_LABEL: " + edit.getEDIT_LANGUAGE_LABEL());
			sb.append("\nMEETING:EDIT:EDIT_INVALID_DATE: " + edit.getEDIT_INVALID_DATE());
			sb.append("\nMEETING:EDIT:EDIT_INVALID_LANGUAGE: " + edit.getEDIT_INVALID_LANGUAGE());
			sb.append("\nMEETING:EDIT:EDIT_SUCCESS_TITLE: " + edit.getEDIT_SUCCESS_TITLE());
			sb.append("\nMEETING:EDIT:EDIT_SUCCESS_DESCRIPTION: " + edit.getEDIT_SUCCESS_DESCRIPTION());
			sb.append("\nMEETING:EDIT:EDIT_FAILED: " + edit.getEDIT_FAILED());

			var string = sb.toString();
			System.out.println(string);
			int nullCount = 0;
			Pattern p = Pattern.compile(": null");
			Matcher m = p.matcher(sb);
			while (m.find()) {
				nullCount++;
			}
			System.out.println(String.format("\nFound %s missing Strings.", nullCount));
		}
	}
}
