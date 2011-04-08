/*
 * Zmanim Java API
 * Copyright (C) 2011 Eliyahu Hershfeld
 * Copyright (C) September 2002 Avrom Finkelstien
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc. 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA or connect to: http://www.fsf.org/copyleft/gpl.html
 */
package net.sourceforge.zmanim.hebrewcalendar;

import java.util.*;

/**
 * The JewishDate class allows one to maintain an instance of a Gregorian date along with the corresponding Jewish date.
 * This class can use the standard Java Date and Calendar classes for setting it, but does not subclass these classes or
 * use them internally to any extensive use. This class also does not have a concept of a time (which the Date class
 * does). Please note that the calendar does not currently support dates prior to 1/1/1 Gregorian. Also keep in mind
 * that the Gregorian calendar started on October 15, 1582, so any calculations prior to that are suspect (at least from
 * a Gregorian perspective). While 1/1/1 Gregorian and forward are technically supported, any calculations prior to <a
 * href="http://en.wikipedia.org/wiki/Hillel_II">Hillel II's (Hakatan's</a>) calendar (4119 in the Jewish Calendar / 359
 * CE Julian as recorded by <a href="http://en.wikipedia.org/wiki/Hai_Gaon">Rav Hai Gaon</a>) would be just an
 * approximation.
 * 
 * This open source Java code was written by <a href="http://www.facebook.com/avromf">Avrom Finkelstien</a> from his C++
 * code. It was refactored to fit the KosherJava Zmanim API with simplification of the code, enhancements and some bug
 * fixing.
 * 
 * Some of Avrom's original C++ code was translated from <a href="http://emr.cs.uiuc.edu/~reingold/calendar.C">C/C++
 * code</a> in <a href="http://www.calendarists.com">Calendrical Calculations</a> by Nachum Dershowitz and Edward M.
 * Reingold, Software-- Practice & Experience, vol. 20, no. 9 (September, 1990), pp. 899- 928. Any method with the mark
 * "ND+ER" indicates that the method was taken from this source with minor modifications.
 * 
 * If you are looking for a class that implements a Jewish calendar version of the Calendar class, one is available from
 * the <a href="http://site.icu-project.org/" >ICU (International Components for Unicode)</a> project, formerly part of
 * IBM's DeveloperWorks.
 * 
 * @see net.sourceforge.zmanim.hebrewcalendar.JewishCalendar
 * @see net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter
 * @see java.util.Date
 * @see java.util.Calendar
 * @author &copy; Avrom Finkelstien 2002
 * @author &copy; Eliyahu Hershfeld 2011
 * @version 0.2.6
 */
public class JewishDate implements Comparable, Cloneable {
	public static final int NISSAN = 1;
	public static final int IYAR = 2;
	public static final int SIVAN = 3;
	public static final int TAMMUZ = 4;
	public static final int AV = 5;
	public static final int ELUL = 6;
	public static final int TISHREI = 7;
	public static final int CHESHVAN = 8;
	public static final int KISLEV = 9;
	public static final int TEVES = 10;
	public static final int SHEVAT = 11;
	public static final int ADAR = 12;
	public static final int ADAR_II = 13;

	static final int JEWISH_EPOCH = -1373429;

	private int jewishMonth;
	private int jewishDay;
	private int jewishYear;

	/**
	 * The month, where 1 == January, 2 == February, etc... Note that this is different than the Java's Calendar class
	 * where January ==0
	 */
	private int gregorianMonth;

	/**
	 * The day of the Gregorian month.
	 */
	private int gregorianDayOfMonth;

	/**
	 * The Gregorian year
	 */
	private int gregorianYear;

	/**
	 * 1 == Sunday, 2 == Monday, etc...
	 */
	private int dayOfWeek;

	private int gregorianAbsDate;

	/**
	 * Returns the number of days in a given month in a given month and year.
	 * 
	 * @param month
	 *            the month. As with other cases in this class, this is 1-based, not zero-based.
	 * @param year
	 *            the year (only impacts February)
	 * @return the number of days in the month in the given year
	 */
	private static int getLastDayOfGregorianMonth(int month, int year) {
		switch (month) {
		case 2:
			if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
				return 29;
			} else {
				return 28;
			}
		case 4:
		case 6:
		case 9:
		case 11:
			return 30;
		default:
			return 31;
		}
	}

	/**
	 * Computes the Gregorian date from the absolute date. ND+ER
	 */
	private void absDateToDate() {
		gregorianYear = gregorianAbsDate / 366; // Search forward year by year from approximate year
		while (gregorianAbsDate >= gregorianDateToAbsDate(gregorianYear + 1, 1, 1)) {
			gregorianYear++;
		}

		gregorianMonth = 1; // Search forward month by month from January
		while (gregorianAbsDate > gregorianDateToAbsDate(gregorianYear, gregorianMonth,
				getLastDayOfGregorianMonth(gregorianMonth, gregorianYear))) {
			gregorianMonth++;
		}

		gregorianDayOfMonth = gregorianAbsDate - gregorianDateToAbsDate(gregorianYear, gregorianMonth, 1) + 1;
	}

	/**
	 * Returns the absolute date (days since January 1, 0001 on the Gregorian calendar).
	 * 
	 * @return the number of days since January 1, 1
	 */
	protected int getAbsDate() {
		return gregorianAbsDate;
	}

	/**
	 * Computes the absolute date from a Gregorian date. ND+ER
	 * 
	 * @param year
	 *            the Gregorian year
	 * @param month
	 *            the Gregorian month. Unlike the Java Calendar where January has the value of 0,This expects a 1 for
	 *            January
	 * @param dayOfMonth
	 *            the day of the month (1st, 2nd, etc...)
	 * @return the absolute Gregorian day
	 */
	private static int gregorianDateToAbsDate(int year, int month, int dayOfMonth) {
		int absDate = dayOfMonth;
		for (int m = month - 1; m > 0; m--) {
			absDate += getLastDayOfGregorianMonth(m, year); // days in prior months of the year
		}
		return (absDate // days this year
				+ 365 * (year - 1) // days in previous years ignoring leap days
				+ (year - 1) / 4 // Julian leap days before this year...
				- (year - 1) / 100 // ...minus prior century years...
		+ (year - 1) / 400); // ...plus prior years divisible by 400
	}

	/**
	 * Returns if the year is a Jewish leap year.
	 * 
	 * @param year
	 *            the Jewish year.
	 * @return true if it is a leap year
	 */
	public static boolean isJewishLeapYear(int year) {
		return ((7 * year) + 1) % 19 < 7;
	}

	/**
	 * Returns if the year the calendar is set to is a Jewish leap year.
	 * 
	 * @return true if it is a leap year
	 */
	public boolean isJewishLeapYear() {
		return isJewishLeapYear(getJewishYear());
	}

	/**
	 * Returns the last month of a given Jewish year.
	 * 
	 * @param year
	 *            the Jewish year.
	 * @return 12 on a non leap-year or 13 on a leap-year
	 */
	private static int getLastMonthOfJewishYear(int year) {
		return isJewishLeapYear(year) ? 13 : 12;
	}

	/**
	 * Returns the number of days elapsed from the Sunday prior to the start of the Jewish calendar to the mean
	 * conjunction of Tishri of the Jewish year.ND+ER
	 * 
	 * @param year
	 *            the Jewish year
	 * @return the number of days elapsed from the Sunday prior to the start of the Jewish calendar to the mean
	 *         conjunction of Tishri of Jewish year.
	 */
	public static int getJewishCalendarElapsedDays(int year) {
		// Jewish lunar month = 29 days, 12 hours and 793 chalakim
		// Molad Tohu = BeHaRaD - Monday, 5 hours (11 PM) and 204 chalakim
		final int chalakimTashTZag = 793; // chalakim in a lunar month
		final int chalakimTohuRaD = 204; // chalakim from original molad Tohu BeHaRaD
		final int hoursTohuHa = 5; // hours from original molad Tohu BeHaRaD
		final int dayTohu = 1; // Monday (0 based)

		int monthsElapsed = (235 * ((year - 1) / 19)) // Months in complete 19 year lunar (Metonic) cycles so far
				+ (12 * ((year - 1) % 19)) // Regular months in this cycle
				+ ((7 * ((year - 1) % 19) + 1) / 19); // Leap months this cycle
		// start with Molad Tohu BeHaRaD
		// start with RaD of BeHaRaD and add TaShTzaG (793) chalakim plus elapse chalakim
		int partsElapsed = chalakimTohuRaD + chalakimTashTZag * (monthsElapsed % 1080);
		// start with Ha hours of BeHaRaD, add 12 hour remainder of lunar month add hours elapsed
		int hoursElapsed = hoursTohuHa + 12 * monthsElapsed + 793 * (monthsElapsed / 1080) + partsElapsed / 1080;
		// start with Monday of BeHaRaD = 1 (0 based), add 29 days of the lunar months elapsed
		int conjunctionDay = dayTohu + 29 * monthsElapsed + hoursElapsed / 24;
		int conjunctionParts = 1080 * (hoursElapsed % 24) + partsElapsed % 1080;
		int alternativeDay = conjunctionDay; //if no dechiyos
		// delay Rosh Hashana for the dechiyos of the Molad - new moon 1 - Molad Zaken, 2- GaTRaD 3- BeTuTaKFoT
		if ((conjunctionParts >= 19440) // Dechiya of Molad Zaken - molad is >= midday (18 hours * 1080 chalakim)
				|| (((conjunctionDay % 7) == 2) // start Dechiya of GaTRaD - Ga = is a Tuesday
						&& (conjunctionParts >= 9924) // TRaD = 9 hours, 204 parts or later (9 * 1080 + 204)
				&& !isJewishLeapYear(year)) // of a non-leap year - end Dechiya of GaTRaD
				|| (((conjunctionDay % 7) == 1) // start Dechiya of BeTuTaKFoT - Be = is on a Monday
						&& (conjunctionParts >= 16789) // TRaD = 15 hours, 589 parts or later (15 * 1080 + 589)
				&& (isJewishLeapYear(year - 1)))) { // in a year following a leap year - end Dechiya of BeTuTaKFoT
			alternativeDay = conjunctionDay + 1; // Then postpone Rosh HaShanah one day
		}
		// start 4th Dechiya - Lo ADU Rosh - Rosh Hashana can't occur on A- sunday, D- Wednesday, U - Friday
		if (((alternativeDay % 7) == 0)// If Rosh HaShanah would occur on Sunday,
				|| ((alternativeDay % 7) == 3) // or Wednesday,
				|| ((alternativeDay % 7) == 5)) { // or Friday - end 4th Dechiya - Lo ADU Rosh
			alternativeDay = alternativeDay + 1; // Then postpone it one (more) day
		}
		return alternativeDay;
	}
	
	/**
	 * Returns the number of days for a given Jewish year. ND+ER
	 * 
	 * @param year
	 *            the Jewish year
	 * @return the number of days for a given Jewish year.
	 * @see #isCheshvanLong(int)
	 * @see #isKislevShort(int)
	 */
	public static int getDaysInJewishYear(int year) {
		return getJewishCalendarElapsedDays(year + 1) - getJewishCalendarElapsedDays(year);
	}

	/**
	 * Returns the number of days for the current year that the calendar is set to
	 * 
	 * @see #isCheshvanLong(int)
	 * @see #isKislevShort(int)
	 * @see #isJewishLeapYear()
	 */
	public int getDaysInJewishYear() {
		return getDaysInJewishYear(getJewishYear());
	}

	/**
	 * Returns if Cheshvan is long in a given Jewish year. ND+ER
	 * 
	 * @param year
	 *            the year
	 * @return true if Cheshvan is long in Jewish year.
	 * @see #isCheshvanLong()
	 */
	public static boolean isCheshvanLong(int year) {
		return getDaysInJewishYear(year) % 10 == 5;
	}

	/**
	 * Returns if Cheshvan is long (30 days VS 29 days) for the current year that the calendar is set to
	 * 
	 * @return true if Cheshvan is long for the current year that the calendar is set to
	 * @see #isCheshvanLong(int)
	 */
	public boolean isCheshvanLong() {
		return isCheshvanLong(getJewishYear());
	}

	/**
	 * Returns if Kislev is short (29 days VS 30 days) in a given Jewish year. ND+ER.
	 * 
	 * @param year
	 *            the Jewish year
	 * @return true if Kislev is short for the given Jewish year.
	 * @see #isKislevShort()
	 */
	public static boolean isKislevShort(int year) {
		return getDaysInJewishYear(year) % 10 == 3;
	}

	/**
	 * Returns if the Kislev is short for the year that this class is set to
	 * 
	 * @return true if Kislev is short for the year that this class is set to
	 * @see #isKislevShort(int)
	 */
	public boolean isKislevShort() {
		return isKislevShort(getJewishYear());
	}

	/**
	 * Returns the number of days of a Jewish month for a given month and year.
	 * 
	 * @param month
	 *            the Jewish month
	 * @param year
	 *            the Jewish Year
	 * @return the number of days for a given Jewish month
	 */
	public static int getDaysInJewishMonth(int month, int year) {
		if ((month == IYAR) || (month == TAMMUZ) || (month == ELUL) || ((month == CHESHVAN) && !(isCheshvanLong(year)))
				|| ((month == KISLEV) && isKislevShort(year)) || (month == TEVES)
				|| ((month == ADAR) && !(isJewishLeapYear(year))) || (month == ADAR_II)) {
			return 29;
		} else {
			return 30;
		}
	}

	/**
	 * Returns the number of days of the Jewish month that the calendar is currently set to.
	 * 
	 * @return the number of days for a given Jewish month that the calendar is currently set to.
	 */
	public int getDaysInJewishMonth() {
		return getDaysInJewishMonth(getJewishMonth(), getJewishYear());
	}

	/**
	 * Computes the Jewish date from the absolute date. ND+ER
	 */
	private void absDateToJewishDate() {
		// Approximation from below
		jewishYear = (gregorianAbsDate + JEWISH_EPOCH) / 366;
		// Search forward for year from the approximation
		while (gregorianAbsDate >= jewishDateToAbsDate(jewishYear + 1, 7, 1)) {
			jewishYear++;
		}
		// Search forward for month from either Tishri or Nisan.
		if (gregorianAbsDate < jewishDateToAbsDate(jewishYear, 1, 1)) {
			jewishMonth = TISHREI;// Start at Tishri
		} else {
			jewishMonth = NISSAN;// Start at Nisan
		}
		while (gregorianAbsDate > jewishDateToAbsDate(jewishYear, jewishMonth,
				getDaysInJewishMonth(jewishMonth, jewishYear))) {
			jewishMonth++;
		}
		// Calculate the day by subtraction
		jewishDay = gregorianAbsDate - jewishDateToAbsDate(jewishYear, jewishMonth, 1) + 1;
	}

	/**
	 * Returns the absolute date of Jewish date. ND+ER
	 * 
	 * @param year
	 *            the Jewish year. The year can't be negative
	 * @param month
	 *            the Jewish month starting with Nisan. Nisan expects a value of 1 etc till Adar with a value of 12. For
	 *            a leap year, 13 will be the expected value for Adar II.
	 * @param dayOfMonth
	 *            the Jewish day of month. valid values are 1-30. If the day of month is set to 30 for a month that only
	 *            has 29 days, the day will be set as 29.
	 * @return the absolute date of the Jewish date.
	 */
	private static int jewishDateToAbsDate(int year, int month, int dayOfMonth) {
		int absDate = dayOfMonth;
		// Before Tishrei (from Nissan to Tishrei), add days in prior months
		if (month < TISHREI) {
			// this year before and after Nisan.
			for (int m = TISHREI; m <= getLastMonthOfJewishYear(year); m++) {
				absDate += getDaysInJewishMonth(m, year);
			}
			for (int m = NISSAN; m < month; m++) {
				absDate += getDaysInJewishMonth(m, year);
			}
		} else { // Add days in prior months this year
			for (int m = TISHREI; m < month; m++) {
				absDate += getDaysInJewishMonth(m, year);
			}
		}
		// Days in prior years + Days elapsed before absolute date 1
		return (absDate + getJewishCalendarElapsedDays(year) + JEWISH_EPOCH);
	}

	/**
	 * Creates a Jewish date based on a Jewish year, month and day of month.
	 * 
	 * @param jewishYear
	 *            the Jewish year
	 * @param jewishMonth
	 *            the Jewish month. The method expects a 1 for Nissan ... 12 for Adar and 13 for Adar II. Use the
	 *            constants {@link #NISSAN} ... {@link #ADAR} (or {@link #ADAR_II} for a leap year Adar II) to avoid any
	 *            confusion.
	 * @param jewishDayOfMonth
	 *            the Jewish day of month. If 30 is passed in for a month with only 29 days (for example {@link #IYAR},
	 *            or {@link #KISLEV} in a year that {@link #isKislevShort()}), the 29th (last valid date of the month)
	 *            will be set
	 * @throws IllegalArgumentException
	 *             if the day of month is < 1 or > 30, or a year of < 0 is passed in.
	 */
	public JewishDate(int jewishYear, int jewishMonth, int jewishDayOfMonth) {
		setJewishDate(jewishYear, jewishMonth, jewishDayOfMonth);
	}

	/**
	 * Default constructor will set a default date to the current system date.
	 */
	public JewishDate() {
		resetDate();
	}

	/**
	 * A constructor that initializes the date to the {@link java.util.Date Date} paremeter.
	 * 
	 * @param date
	 *            the <code>Date</code> to set the calendar to
	 * @throws IllegalArgumentException
	 *             if the date would fall prior to the year 1 AD
	 */
	public JewishDate(Date date) {
		setDate(date);
	}

	/**
	 * A constructor that initializes the date to the {@link java.util.Calendar Calendar} paremeter.
	 * 
	 * @param calendar
	 *            the <code>Calendar</code> to set the calendar to
	 * @throws IllegalArgumentException
	 *             if the {@link Calendar#ERA} is {@link GregorianCalendar#BC}
	 */
	public JewishDate(Calendar calendar) {
		setDate(calendar);
	}

	/**
	 * Sets the date based on a {@link java.util.Calendar Calendar} object. Modifies the Jewish date as well.
	 * 
	 * @param calendar
	 *            the <code>Calendar</code> to set the calendar to
	 * @throws IllegalArgumentException
	 *             if the {@link Calendar#ERA} is {@link GregorianCalendar#BC}
	 */
	public void setDate(Calendar calendar) {
		if (calendar.get(Calendar.ERA) == GregorianCalendar.BC) {
			throw new IllegalArgumentException("Calendars with a BC era are not supported. The year "
					+ calendar.get(Calendar.YEAR) + " BC is invalid.");
		}
		gregorianMonth = calendar.get(Calendar.MONTH) + 1;
		gregorianDayOfMonth = calendar.get(Calendar.DATE);
		gregorianYear = calendar.get(Calendar.YEAR);
		gregorianAbsDate = gregorianDateToAbsDate(gregorianYear, gregorianMonth, gregorianDayOfMonth); // init the date
		absDateToJewishDate();

		dayOfWeek = Math.abs(gregorianAbsDate % 7) + 1; // set day of week
	}

	/**
	 * Sets the date based on a {@link java.util.Date Date} object. Modifies the Jewish date as well.
	 * 
	 * @param date
	 *            the <code>Date</code> to set the calendar to
	 * @throws IllegalArgumentException
	 *             if the date would fall prior to the year 1 AD
	 */
	public void setDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		setDate(cal);
	}

	/**
	 * Sets the Gregorian Date, and updates the Jewish date accordingly. Confusingly unlike the Java Calendar where
	 * January has the value of 0,This class expects a 1 for January.
	 * 
	 * @param year
	 *            the Gregorian year
	 * @param month
	 *            the Gregorian month. Unlike the Java Calendar where January has the value of 0,This expects a 1 for
	 *            January
	 * @param dayOfMonth
	 *            the Gregorian day of month. If this is > the number of days in the month/year, the last valid date of
	 *            the month will be set
	 * @throws IllegalArgumentException
	 *             if a year of < 1, a month < 1 or > 12 or a day of month < 1 is passed in
	 */
	public void setGregorianDate(int year, int month, int dayOfMonth) {
		if (month > 12 || month < 1) {
			throw new IllegalArgumentException("The Gregorian month has to be between 1 - 12. " + month
					+ " is invalid.");
		}
		if (dayOfMonth <= 0) {
			throw new IllegalArgumentException("The day of month can't be less than 1. " + dayOfMonth + " is invalid.");
		}

		// make sure date is a valid date for the given month, if not, set to last day of month
		if (dayOfMonth > getLastDayOfGregorianMonth(month, year)) {
			dayOfMonth = getLastDayOfGregorianMonth(month, year);
		}
		if (year < 1) {
			throw new IllegalArgumentException("Years < 1 can't be claculated. " + year + " is invalid.");
		}
		// init month, date, year
		gregorianMonth = month;
		gregorianDayOfMonth = dayOfMonth;
		gregorianYear = year;

		gregorianAbsDate = gregorianDateToAbsDate(gregorianYear, gregorianMonth, gregorianDayOfMonth); // init date
		absDateToJewishDate();

		dayOfWeek = Math.abs(gregorianAbsDate % 7) + 1; // set day of week
	}

	/**
	 * Sets the Jewish Date and updates the Gregorian date accordingly.
	 * 
	 * @param year
	 *            the Jewish year. The year can't be negative
	 * @param month
	 *            the Jewish month starting with Nisan. A value of 1 is expected for Nissan ... 12 for Adar and 13 for
	 *            Adar II. Use the constants {@link #NISSAN} ... {@link #ADAR} (or {@link #ADAR_II} for a leap year Adar
	 *            II) to avoid any confusion.
	 * @param dayOfMonth
	 *            the Jewish day of month. valid values are 1-30. If the day of month is set to 30 for a month that only
	 *            has 29 days, the day will be set as 29.
	 * @throws IllegalArgumentException
	 *             if a year of < 3761, a month < 1 or > 12 (or 13 on a leap year) or the day of month is < 1 or > 30 is
	 *             passed in
	 */
	public void setJewishDate(int year, int month, int dayOfMonth) {
		if (month < 1 || month > getLastMonthOfJewishYear(year)) {
			throw new IllegalArgumentException("The Jewish month has to be between 1 and 12 (or 13 on a leap year). "
					+ month + " is invalid for the year " + year + ".");
		}
		if (dayOfMonth < 1 || dayOfMonth > 30) {
			throw new IllegalArgumentException("The Jewish day of month can't be < 1 or > 30.  " + dayOfMonth
					+ " is invalid.");
		}

		// if 30 is passed for a month that only has 29 days (for example by rolling the month from a month that had
		// 30 days to a month that only has 29) set the date to 29th
		if (dayOfMonth > getDaysInJewishMonth(month, year)) {
			dayOfMonth = getDaysInJewishMonth(month, year);
		}

		if (year <= 3761) { // FIXME: prior to 18 Teves, 3761 (1/1/1 AD) is iinvalid. There is room to allow the year
							// 3761 after 18 Teves
			throw new IllegalArgumentException("A Jewish years < 3761 can't be set. " + year + " is invalid.");
		}

		jewishMonth = month;
		jewishDay = dayOfMonth;
		jewishYear = year;

		gregorianAbsDate = jewishDateToAbsDate(jewishYear, jewishMonth, jewishDay); // reset Gregorian date
		absDateToDate();

		dayOfWeek = Math.abs(gregorianAbsDate % 7) + 1; // reset day of week
	}

	/**
	 * Returns this object's date as a java.util.Date object. <b>Note</b>: This class does not have a concept of time.
	 * 
	 * @return The <code>Date</code>
	 */
	public Date getTime() {
		Calendar cal = Calendar.getInstance();
		cal.set(gregorianYear, gregorianMonth - 1, gregorianDayOfMonth);
		return cal.getTime();
	}

	/**
	 * Resets this date to the current system date.
	 */
	public void resetDate() {
		Calendar calendar = Calendar.getInstance();
		setDate(calendar);
	}

	/**
	 * Returns a string containing the Jewish date in the form, "day Month, year" e.g. "21 Shevat, 5729". For more
	 * complex formatting, use the formatter classes.
	 * 
	 * @return the Jewish date in the form "day Month, year" e.g. "21 Shevat, 5729"
	 * @see HebrewDateFormatter#format(JewishDate)
	 */
	public String toString() {
		return new HebrewDateFormatter().format(this);
	}

	/**
	 * Rolls the date forward by 1 day. It modifies both the Gregorian and Jewish dates accordingly. The API does not
	 * currently offer the ability to forward more than one day t a time, or to forward by month or year. If such
	 * manipulation is required use the {@link Calendar} class {@link Calendar#add(int, int)} or
	 * {@link Calendar#roll(int, int)} methods in the following manner.
	 * 
	 * <pre>
	 * <code>
	 * 	Calendar cal = jewishDate.getTime(); // get a java.util.Calendar representation of the JewishDate
	 * 	cal.add(Calendar.MONTH, 3); // add 3 Gregorian months
	 * 	jewishDate.setDate(cal); // set the updated calendar back to this class
	 * </code>
	 * </pre>
	 * 
	 * @see #back()
	 * @see Calendar#add(int, int)
	 * @see Calendar#roll(int, int)
	 */
	public void forward() {
		// Change Gregorian date
		if (gregorianDayOfMonth == getLastDayOfGregorianMonth(gregorianMonth, gregorianYear)) {
			// if last day of year
			if (gregorianMonth == 12) {
				gregorianYear++;
				gregorianMonth = 1;
				gregorianDayOfMonth = 1;
			} else {
				gregorianMonth++;
				gregorianDayOfMonth = 1;
			}
		} else { // if not last day of month
			gregorianDayOfMonth++;
		}

		// Change the Jewish Date
		if (jewishDay == getDaysInJewishMonth(jewishMonth, jewishYear)) {
			// if it last day of elul (i.e. last day of Jewish year)
			if (jewishMonth == ELUL) {
				jewishYear++;
				jewishMonth++;
				jewishDay = 1;
			} else if (jewishMonth == getLastMonthOfJewishYear(jewishYear)) {
				// if it is the last day of Adar, or Adar II as case may be
				jewishMonth = NISSAN;
				jewishDay = 1;
			} else {
				jewishMonth++;
				jewishDay = 1;
			}
		} else { // if not last date of month
			jewishDay++;
		}

		if (dayOfWeek == 7) { // if last day of week, loop back to Sunday
			dayOfWeek = 1;
		} else {
			dayOfWeek++;
		}

		gregorianAbsDate++; // increment the absolute date
	}

	/**
	 * Rolls the date back by 1 day. It modifies both the Gregorian and Jewish dates accordingly. The API does not
	 * currently offer the ability to forward more than one day t a time, or to forward by month or year. If such
	 * manipulation is required use the {@link Calendar} class {@link Calendar#add(int, int)} or
	 * {@link Calendar#roll(int, int)} methods in the following manner.
	 * 
	 * <pre>
	 * <code>
	 * 	Calendar cal = jewishDate.getTime(); // get a java.util.Calendar representation of the JewishDate
	 * 	cal.add(Calendar.MONTH, -3); // subtract 3 Gregorian months
	 * 	jewishDate.setDate(cal); // set the updated calendar back to this class
	 * </code>
	 * </pre>
	 * 
	 * @see #back()
	 * @see Calendar#add(int, int)
	 * @see Calendar#roll(int, int)
	 */
	public void back() {
		// Change Gregorian date
		if (gregorianDayOfMonth == 1) { // if first day of month
			if (gregorianMonth == 1) { // if first day of year
				gregorianMonth = 12;
				gregorianYear--;
			} else {
				gregorianMonth--;
			}
			// change to last day of previous month
			gregorianDayOfMonth = getLastDayOfGregorianMonth(gregorianMonth, gregorianYear);
		} else {
			gregorianDayOfMonth--;
		}
		// change Jewish date
		if (jewishDay == 1) { // if first day of the Jewish month
			if (jewishMonth == NISSAN) {
				jewishMonth = getLastMonthOfJewishYear(jewishYear);
			} else if (jewishMonth == TISHREI) { // if Rosh Hashana
				jewishYear--;
				jewishMonth--;
			} else {
				jewishMonth--;
			}
			jewishDay = getDaysInJewishMonth(jewishMonth, jewishYear);
		} else {
			jewishDay--;
		}

		if (dayOfWeek == 1) { // if first day of week, loop back to Saturday
			dayOfWeek = 7;
		} else {
			dayOfWeek--;
		}
		gregorianAbsDate--; // change the absolute date
	}

	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof JewishDate)) {
			return false;
		}
		JewishDate jewishDate = (JewishDate) object;
		return gregorianAbsDate == jewishDate.getAbsDate();
	}

	/**
	 * Compares two dates as per the compareTo() method in the Comparable interface. Returns a value less than 0 if this
	 * date is "less than" (before) the date, greater than 0 if this date is "greater than" (after) the date, or 0 if
	 * they are equal.
	 */
	public int compareTo(Object o) {
		JewishDate hebDate = (JewishDate) o;
		return gregorianAbsDate < hebDate.getAbsDate() ? -1 : gregorianAbsDate > hebDate.getAbsDate() ? 1 : 0;
	}

	/**
	 * Returns the Gregorian month (between 1-12).
	 * 
	 * @return the Gregorian month (between 1-12). Unlike the java.util.Calendar, this will be 1 based and not 0 based.
	 */
	public int getGregorianMonth() {
		return gregorianMonth;
	}

	/**
	 * Returns the Gregorian day of the month.
	 * 
	 * @return the Gregorian day of the mont
	 */
	public int getGregorianDayOfMonth() {
		return gregorianDayOfMonth;
	}

	/**
	 * Returns the Gregotian year.
	 * 
	 * @return the Gregorian year
	 */
	public int getGregorianYear() {
		return gregorianYear;
	}

	/**
	 * Returns the Jewish month 1-12 (or 13 years in a leap year). The month count starts with 1 for Nisan and goes to
	 * 13 for Adar II
	 * 
	 * @return the Jewish month from 1 to 12 (or 13 years in a leap year). The month count starts with 1 for Nisan and
	 *         goes to 13 for Adar II
	 */
	public int getJewishMonth() {
		return jewishMonth;
	}

	/**
	 * Returns the Jewish day of month.
	 * 
	 * @return the Jewish day of the month
	 */
	public int getJewishDayOfMonth() {
		return jewishDay;
	}

	/**
	 * Returns the Jewish year.
	 * 
	 * @return the Jewish year
	 */
	public int getJewishYear() {
		return jewishYear;
	}

	/**
	 * Returns the day of the week as a number between 1-7.
	 * 
	 * @return the day of the week as a number between 1-7.
	 */
	public int getDayOfWeek() {
		return dayOfWeek;
	}

	/**
	 * Sets the Gregorian month.
	 * 
	 * @param month
	 *            the Gregorian month
	 * 
	 * @throws IllegalArgumentException
	 *             if a month < 1 or > 12 is passed in
	 */
	public void setGregorianMonth(int month) {
		setGregorianDate(gregorianYear, month, gregorianDayOfMonth);
	}

	/**
	 * sets the Gregorian year.
	 * 
	 * @param year
	 *            the Gregorian year.
	 * @throws IllegalArgumentException
	 *             if a year of < 1 is passed in
	 */
	public void setGregorianYear(int year) {
		setGregorianDate(year, gregorianMonth, gregorianDayOfMonth);
	}

	/**
	 * sets the Gregorian Day of month.
	 * 
	 * @param dayOfMonth
	 *            the Gregorian Day of month.
	 * @throws IllegalArgumentException
	 *             if the day of month of < 1 is passed in
	 */
	public void setGregorianDayOfMonth(int dayOfMonth) {
		setGregorianDate(gregorianYear, gregorianMonth, dayOfMonth);
	}

	/**
	 * sets the Jewish month.
	 * 
	 * @param month
	 *            the Jewish month from 1 to 12 (or 13 years in a leap year). The month count starts with 1 for Nisan
	 *            and goes to 13 for Adar II
	 * @throws IllegalArgumentException
	 *             if a month < 1 or > 12 (or 13 on a leap year) is passed in
	 */
	public void setJewishMonth(int month) {
		setJewishDate(jewishYear, month, jewishDay);
	}

	/**
	 * sets the Jewish year.
	 * 
	 * @param year
	 *            the Jewish year
	 * @throws IllegalArgumentException
	 *             if a year of < 3761 is passed in
	 */
	public void setJewishYear(int year) {
		setJewishDate(year, jewishMonth, jewishDay);
	}

	/**
	 * sets the Jewish day of month.
	 * 
	 * @param dayOfMonth
	 *            the Jewish day of month
	 * @throws IllegalArgumentException
	 *             if the day of month is < 1 or > 30 is passed in
	 */
	public void setJewishDayOfMonth(int dayOfMonth) {
		setJewishDate(jewishYear, jewishMonth, dayOfMonth);
	}

	/**
	 * A method that creates a <a href="http://en.wikipedia.org/wiki/Object_copy#Deep_copy">deep copy</a> of the object. <br />
	 * 
	 * @see java.lang.Object#clone()
	 * @since 1.1
	 */
	public Object clone() {
		JewishDate clone = null;
		try {
			clone = (JewishDate) super.clone();
		} catch (CloneNotSupportedException cnse) {
			// Required by the compiler. Should never be reached since we implement clone()
		}
		clone.setGregorianDate(gregorianYear, gregorianMonth, gregorianDayOfMonth);
		return clone;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int result = 17;
		result = 37 * result + getClass().hashCode(); // needed or this and
														// subclasses will
														// return identical hash
		result += 37 * result + gregorianAbsDate;
		return result;
	}
}