package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.annotations.MissingLocale;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles and renders all interactions regarding the button calendar.
 */
@MissingLocale
@Slf4j
public class MeetingCalendar {

	private final String ARROW_LEFT = "←";
	private final String ARROW_RIGHT = "→";
	private final String BUTTON_DAY_ID = "calendar:day:%s:%s:%s:%s:%s";

	private final Map<Long, List<LocalDate>> selections;

	/**
	 * This class's constructor which initializes a new {@link HashMap} for saving selections.
	 */
	public MeetingCalendar() {
		this.selections = new HashMap<>();
	}

	/**
	 * Handles all interactions regarding the calendar.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @param id    The button's id, split by ":".
	 * @return The {@link MessageAction}.
	 */
	public RestAction<Message> handleCalenderInteraction(ButtonInteraction event, String[] id) {
		event.deferEdit().queue();
		log.debug("{}: is {}/{} characters long", Arrays.toString(id), String.join(":", id).length(), Button.ID_MAX_LENGTH);
		return switch (id[1]) {
			case "day" -> this.handleCalenderDaySelection(event, id);
			case "month" -> this.handleCalendarMonthSelection(event, id);
			case "year" -> this.handleCalendarYearSelection(event, id);
			case "page" -> this.handlePageSelection(event, this.getMonthFromString(id[3]), this.getYearFromString(id[4]), Integer.parseInt(id[2]) + 1, Boolean.parseBoolean(id[5]));
			case "submit" -> this.handleSubmit(event);
			case "cancel" -> this.handleCancel(event);
			case "reset" -> this.handleReset(event, id);
			default -> this.handleUnknown(event);
		};
	}

	/**
	 * Renders a month's days, each as a separate button.
	 *
	 * @param user        The user that is currently using the calendar.
	 * @param multiSelect Whether it is possible to select multiple dates at once.
	 * @param month       The {@link Month} that should be rendered.
	 * @param year        The {@link Year} that should be rendered.
	 * @param page        Specifies what pages should be rendered.
	 * @return A List full of {@link ActionRow}s, containing the buttons.
	 */
	private List<ActionRow> renderDayCalender(User user, boolean multiSelect, Month month, Year year, int page) {
		if (!this.selections.containsKey(user.getIdLong())) {
			this.selections.put(user.getIdLong(), new ArrayList<>());
		}
		List<ActionRow> rows = new ArrayList<>();
		String monthString = month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		rows.add(ActionRow.of(
				Button.primary(String.format("calendar:month:%s:%s", year.getValue(), multiSelect), monthString),
				Button.secondary(String.format("calendar:year:left:%s:%s:%s", month.getValue(), year.getValue(), multiSelect), ARROW_LEFT),
				Button.primary("calendar:year", String.valueOf(year)),
				Button.secondary(String.format("calendar:year:right:%s:%s:%s", month.getValue(), year.getValue(), multiSelect), ARROW_RIGHT)
		));
		// one page = 1, two pages = 16, three pages = 31, ...
		int day = 1 + ((page - 1) * 15);
		List<LocalDate> selections = this.selections.get(user.getIdLong());
		// 3 action rows
		for (int a = 0; a < 3; a++) {
			List<Button> buttons = new ArrayList<>();
			// each row has 5 buttons
			for (int b = 0; b < 5; b++) {
				Button button;
				boolean selected = false;
				if (month.length(year.isLeap()) >= day) {
					// assign a temporary dummy value as the id
					button = Button.secondary("dummy", String.valueOf(day));
					if (selections.contains(LocalDate.of(year.getValue(), month, day))) {
						button = button.withStyle(ButtonStyle.SUCCESS);
						selected = true;
					}
				} else {
					// assign a temporary dummy value as the id
					button = Button.secondary("dummy", " ").asDisabled();
				}
				// change it to the actual id
				String buttonId = String.format(BUTTON_DAY_ID, day, month.getValue(), year.getValue(), multiSelect, selected);
				buttons.add(button.withId(buttonId));
				day++;
			}
			rows.add(ActionRow.of(buttons));
		}
		rows.add(ActionRow.of(
				Button.secondary(
						String.format("calendar:page:%s:%s:%s:%s", page, month.getValue(), year.getValue(), multiSelect),
						String.format("Page %s/%s", page, this.getMaxPages(month, year.isLeap()))),
				Button.danger("calendar:reset:" + multiSelect, "Reset"),
				Button.danger("calendar:cancel", "Cancel"),
				Button.success("calendar:submit", "Submit")
		));
		return rows;
	}

	/**
	 * Renders a month's days, each as a separate button.
	 *
	 * @param user        The user that is currently using the calendar.
	 * @param multiSelect Whether it is possible to select multiple dates at once.
	 * @return A List full of {@link ActionRow}s, containing the buttons.
	 */
	public List<ActionRow> renderDayCalender(User user, boolean multiSelect) {
		LocalDateTime time = LocalDateTime.now();
		return this.renderDayCalender(user, multiSelect, time.getMonth(), Year.from(time), 1);
	}

	/**
	 * Renders all months as separate buttons.
	 *
	 * @param year        The currently selected year.
	 * @param multiSelect Whether it is possible to select multiple dates at once.
	 * @return A List full of {@link ActionRow}s, containing the buttons.
	 */
	private List<ActionRow> renderMonthCalendar(int year, boolean multiSelect) {
		List<ActionRow> rows = new ArrayList<>();
		List<Button> buttons = new ArrayList<>();
		for (Month month : Month.values()) {
			String monthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
			Button b = Button.primary(String.format("calendar:month:%s:%s:%s", month.getValue(), year, multiSelect), monthName);
			buttons.add(b);
			// check if a row has 4 buttons. if so, create a new row.
			if (buttons.size() == 4) {
				rows.add(ActionRow.of(buttons));
				buttons.clear();
			}
		}
		return rows;
	}

	/**
	 * Submit the selections and closes the calendar.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleSubmit(ButtonInteraction event) {
		List<LocalDate> dates = this.selections.get(event.getUser().getIdLong());
		if (!dates.isEmpty()) {
			event.getHook().sendMessage("You've selected: " + dates.stream().map(d -> d.atStartOfDay().toEpochSecond(ZoneOffset.UTC)).map(l -> String.format("<t:%s:F>", l)).collect(Collectors.joining(", "))).queue();
		}
		this.selections.remove(event.getUser().getIdLong());
		return event.getMessage().editMessageComponents(this.buildCalenderClosedButton());
	}

	/**
	 * Cancels the current calendar interaction.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleCancel(ButtonInteraction event) {
		this.selections.remove(event.getUser().getIdLong());
		return event.getMessage().editMessageComponents(this.buildCalenderClosedButton());
	}

	/**
	 * Resets the current state of the calendar by clearing all selections.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @param id    The button's id, split by ":".
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleReset(ButtonInteraction event, String[] id) {
		this.selections.put(event.getUser().getIdLong(), new ArrayList<>());
		return event.getMessage().editMessageComponents(this.renderDayCalender(event.getUser(), Boolean.parseBoolean(id[2])));
	}

	/**
	 * Handles an unknown interaction.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleUnknown(ButtonInteraction event) {
		this.selections.remove(event.getUser().getIdLong());
		return event.getMessage().editMessageComponents(ActionRow.of(
				Button.secondary("calendar:unknown", "Unknown Interaction: " + event.getComponentId()).asDisabled()
		));
	}

	/**
	 * Updates the calendar's current page.
	 *
	 * @param interaction The {@link ButtonInteraction} that is triggered upon use.
	 * @param month       The calendar's currently selected {@link Month}.
	 * @param year        The calendar's currently selected {@link Year}.
	 * @param page        The new page number.
	 * @param multiSelect Whether it is possible to select multiple dates at once.
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handlePageSelection(ButtonInteraction interaction, Month month, Year year, int page, boolean multiSelect) {
		if (page > this.getMaxPages(month, year.isLeap())) page = 1;
		return interaction.getMessage().editMessageComponents(this.renderDayCalender(interaction.getUser(), multiSelect, month, year, page));
	}

	/**
	 * Updates the calendar's current day.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @param id    The button's id, split by ":".
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleCalenderDaySelection(ButtonInteraction event, String[] id) {
		Month month = this.getMonthFromString(id[3]);
		Year year = this.getYearFromString(id[4]);
		boolean multiSelect = Boolean.parseBoolean(id[5]);
		boolean selected = Boolean.parseBoolean(id[6]);
		int day = Integer.parseInt(id[2]);
		int page = 1;
		if (day >= 16 && day < 31) page = 2;
		if (day >= 31) page = 3;
		// don't do all of this stuff when the label is blank (not a valid day)
		if (!event.getButton().getLabel().isBlank()) {
			LocalDate date = LocalDate.of(year.getValue(), month, day);
			List<LocalDate> dates = new ArrayList<>();
			if (this.selections.containsKey(event.getUser().getIdLong())) {
				if (multiSelect) dates = this.selections.get(event.getUser().getIdLong());
			}
			// if the button is already selected unselect it, otherwise select it.
			if (selected) {
				dates.remove(date);
			} else {
				dates.add(date);
			}
			this.selections.put(event.getUser().getIdLong(), dates);
		}
		return event.getMessage().editMessageComponents(this.renderDayCalender(event.getUser(), multiSelect, month, year, page));
	}

	/**
	 * Updates calendar's current month (or opens a list with all available months)
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @param id    The button's id, split by ":".
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleCalendarMonthSelection(ButtonInteraction event, String[] id) {
		if (id.length == 4) {
			// calendar:month:2022:true
			return event.getMessage().editMessageComponents(this.renderMonthCalendar(Integer.parseInt(id[2]), Boolean.parseBoolean(id[3])));
		}
		// calendar:month:1:2022:true
		return event.getMessage().editMessageComponents(this.renderDayCalender(event.getUser(), Boolean.parseBoolean(id[4]), this.getMonthFromString(id[2]), this.getYearFromString(id[3]), 1));
	}

	/**
	 * Updates the calendar's current year.
	 *
	 * @param event The {@link ButtonInteraction} that is triggered upon use.
	 * @param id    The button's id, split by ":".
	 * @return The {@link MessageAction}.
	 */
	private MessageAction handleCalendarYearSelection(ButtonInteraction event, String[] id) {
		if (id.length == 2) return event.getMessage().editMessageComponents(event.getMessage().getActionRows());
		// calendar:year:left:1:2022:true
		Month month = this.getMonthFromString(id[3]);
		int year = Integer.parseInt(id[4]);
		boolean multiSelect = Boolean.parseBoolean(id[5]);
		// if id[2] equals "left" subtract one from the current year - otherwise add one
		Year newYear = Year.of(id[2].equals("left") ? year - 1 : year + 1);
		return event.getMessage().editMessageComponents(this.renderDayCalender(event.getUser(), multiSelect, month, newYear, 1));
	}

	/**
	 * Builds the "Calendar Closed" button.
	 *
	 * @return The {@link Button}, added to an {@link ActionRow}.
	 */
	private ActionRow buildCalenderClosedButton() {
		return ActionRow.of(Button.secondary("calendar:closed", "Calendar closed").asDisabled());
	}

	/**
	 * Converts a string to a {@link Year} object.
	 *
	 * @param s The string that should be converted.
	 * @return The {@link Year} object.
	 */
	private Year getYearFromString(String s) {
		return Year.of(Integer.parseInt(s));
	}

	/**
	 * Converts a string to a {@link Month} object.
	 *
	 * @param s The string that should be converted.
	 * @return The {@link Month} object.
	 */
	private Month getMonthFromString(String s) {
		return Month.of(Integer.parseInt(s));
	}

	/**
	 * Calculates the maximum amount of pages (one page can hold 15 buttons/days) for the given month.
	 *
	 * @param month      The month for which the amount of pages should be calculated.
	 * @param isLeapYear Whether the year is a leap year.
	 * @return The amount of pages.
	 */
	private int getMaxPages(Month month, boolean isLeapYear) {
		int maxPages = 0;
		int monthDays = month.length(isLeapYear);
		while (monthDays > 0) {
			// one page can hold up to 15 days
			monthDays = monthDays - 15;
			maxPages++;
		}
		return maxPages;
	}
}
