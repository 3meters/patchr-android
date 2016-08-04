package com.patchr.utilities;

import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.patchr.R;
import com.patchr.components.StringManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTime {

	public static final  String DATE_NOW_FORMAT_FILENAME          = "yyyyMMdd_HHmmss";
	public static final  String DATE_NOW_FORMAT_FILENAME_EXTENDED = "yyyyMMdd_HHmmss_SSS";
	private static final String DATE_FORMAT_TIME_SINCE            = "MMM d";
	private static final String DATE_FORMAT_TIME_SINCE_WITH_YEAR  = "MMM d, yyyy";
	private static final String TIME_FORMAT_TIME_SINCE            = "h:mm";
	private static final String AMPM_FORMAT_TIME_SINCE            = "a";
	public static final  String DATE_FORMAT_DETAILED              = "MMM d, yyyy h:mm:ss.SSS";    // NO_UCD (unused code)
	public static final  String DATE_FORMAT_DEFAULT               = "MMMM d, yyyy h:mma";

	private static final String DATE_FORMAT_TIME_SINCE_TWITTER           = "dd MMM";
	private static final String DATE_FORMAT_TIME_SINCE_WITH_YEAR_TWITTER = "dd MMM y";

	public static String nowString(@NonNull String pattern) {
		final Calendar cal = Calendar.getInstance();
		final SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
		return sdf.format(cal.getTime());
	}

	public static String dateString(Long time, @NonNull String pattern) {
		final SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
		return sdf.format(time);
	}

	public static Date nowDate() {
		final Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}

	public static Long secondsAgo(Long time) {
		final Calendar cal = Calendar.getInstance();
		final Long nowMilliseconds = cal.getTimeInMillis();
		return (nowMilliseconds - time) / 1000;
	}

	public static long secondsSinceMidnight() {
		final Calendar cal = Calendar.getInstance();
		final Long now = cal.getTimeInMillis();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long passed = now - cal.getTimeInMillis();
		return passed / 1000;
	}

	@SuppressWarnings("deprecation")
	@NonNull public static String dateStringAt(Long time) {
		final Date date = new Date(time);

		SimpleDateFormat datePart = new SimpleDateFormat((date.getYear() != DateTime.nowDate().getYear())
		                                                 ? DATE_FORMAT_TIME_SINCE_WITH_YEAR
		                                                 : DATE_FORMAT_TIME_SINCE, Locale.US);

		final SimpleDateFormat timePart = new SimpleDateFormat(TIME_FORMAT_TIME_SINCE, Locale.US);
		final SimpleDateFormat ampmPart = new SimpleDateFormat(AMPM_FORMAT_TIME_SINCE, Locale.US);

		return timePart.format(time) + " "
				+ ampmPart.format(time).toUpperCase(Locale.US) + " " + StringManager.getString(R.string.symbol_bullet) + " "
				+ datePart.format(time);
	}

	@SuppressWarnings("deprecation") public static String interval(Long oldDateMs, Long newDateMs, IntervalContext context) {

		final Date dateOld = new Date(oldDateMs);

		final Long diff = newDateMs - oldDateMs;

		if (diff <= 0) return "now";
		final int seconds = (int) (diff / 1000);
		final int minutes = (int) ((diff / 1000) / 60);
		final int hours = (int) ((diff / 1000) / (60 * 60));
		final int days = (int) ((diff / 1000) / (60 * 60 * 24));

		String interval = "now";
		if (days >= 1) {
			SimpleDateFormat datePart = new SimpleDateFormat(DATE_FORMAT_TIME_SINCE, Locale.US);
			if (dateOld.getYear() != DateTime.nowDate().getYear()) {
				datePart = new SimpleDateFormat(DATE_FORMAT_TIME_SINCE_WITH_YEAR, Locale.US);
				return datePart.format(dateOld.getTime());
			}
			else {
				final SimpleDateFormat timePart = new SimpleDateFormat(TIME_FORMAT_TIME_SINCE, Locale.US);
				final SimpleDateFormat ampmPart = new SimpleDateFormat(AMPM_FORMAT_TIME_SINCE, Locale.US);
				return datePart.format(dateOld.getTime()) + " at "
						+ timePart.format(dateOld.getTime()) + " "
						+ ampmPart.format(dateOld.getTime()).toUpperCase(Locale.US);
			}
		}
		else if (hours == 1) /* x hours x minutes ago */ {
			interval = "1 hour";
		}
		else if (hours > 1) /* x hours x minutes ago */ {
			interval = String.valueOf(hours) + " hours";
		}
		else if (minutes == 1) /* x hours x minutes ago */ {
			interval = "1 minute";
		}
		else if (minutes > 1) /* x hours x minutes ago */ {
			interval = String.valueOf(minutes) + " minutes";
		}
		else if (seconds == 1) /* 1 second ago */ {
			interval = "1 second";
		}
		else if (seconds > 1) /* x hours x minutes ago */ {
			interval = String.valueOf(seconds) + " seconds";
		}
		if (context == IntervalContext.PAST) {
			interval += " ago";
		}
		return interval;
	}

	@SuppressWarnings("deprecation") public static String intervalCompact(Long oldDateMs, Long newDateMs) {

		final Long diff = newDateMs - oldDateMs;

		final Date dateOld = new Date(oldDateMs);
		final Date dateNew = new Date(newDateMs);

		final Boolean sameYear = (dateOld.getYear() == dateNew.getYear());

		if (diff <= 0) return "now";
		final int seconds = (int) (diff / DateUtils.SECOND_IN_MILLIS);
		final int minutes = (int) (diff / DateUtils.MINUTE_IN_MILLIS);
		final int hours = (int) (diff / DateUtils.HOUR_IN_MILLIS);
		final int days = (int) (diff / DateUtils.DAY_IN_MILLIS);

		String interval = "now";
		if (days >= 7) {
			if (!sameYear) {
				final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_TIME_SINCE_WITH_YEAR_TWITTER, Locale.US);
				interval = sdf.format(dateOld);
			}
			else {
				final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_TIME_SINCE_TWITTER, Locale.US);
				interval = sdf.format(dateOld);
			}
		}
		else if (days >= 1) {
			interval = days + "d";
		}
		else if (hours >= 1) {
			interval = hours + "h";
		}
		else if (minutes >= 1) {
			interval = minutes + "m";
		}
		else if (seconds >= 1) {
			interval = seconds + "s";
		}
		return interval;
	}

	public enum IntervalContext {
		PAST,
		FUTURE
	}
}
