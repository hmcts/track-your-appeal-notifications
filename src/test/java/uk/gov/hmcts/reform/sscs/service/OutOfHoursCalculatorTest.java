package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class OutOfHoursCalculatorTest {

    private static final int START_HOUR = 9;
    private static final int END_HOUR = 17;

    @Test
    public void isNotOutOfHours() throws NoSuchAlgorithmException {
        ZonedDateTime now = nowAtHour(12);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(false));
    }

    @Test
    public void isNotOutOfHoursAtStartTime() throws NoSuchAlgorithmException {
        ZonedDateTime now = nowAtHour(9);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(false));
    }

    @Test
    public void isOutOfHours() throws NoSuchAlgorithmException {
        ZonedDateTime now = nowAtHour(20);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(true));
    }

    @Test
    public void isOutOfHoursAtEndTime() throws NoSuchAlgorithmException {
        ZonedDateTime now = nowAtHour(17);
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).isItOutOfHours();

        assertThat(isOutOfHours, is(true));
    }

    @Test
    public void getStartOfNextInHoursPeriodWhenItIsTheNextDay() throws NoSuchAlgorithmException {
        ZonedDateTime now = nowAtHour(END_HOUR);
        ZonedDateTime nextInHoursTime = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).getStartOfNextInHoursPeriod();

        assertThat(nextInHoursTime.getYear(), is(2018));
        assertThat(nextInHoursTime.getMonthValue(), is(9));
        assertThat(nextInHoursTime.getDayOfMonth(), is(19));
        assertThat(nextInHoursTime.getHour(), is(START_HOUR));
        assertThat(new Double(nextInHoursTime.getMinute()), closeTo(0, 59));
    }

    @Test
    public void getStartOfNextInHoursPeriodWhenItIsTheSameDay() throws NoSuchAlgorithmException {
        ZonedDateTime now = nowAtHour(1);
        ZonedDateTime nextInHoursTime = new OutOfHoursCalculator(new FixedDateTimeProvider(now), START_HOUR, END_HOUR).getStartOfNextInHoursPeriod();

        assertThat(nextInHoursTime.getYear(), is(2018));
        assertThat(nextInHoursTime.getMonthValue(), is(9));
        assertThat(nextInHoursTime.getDayOfMonth(), is(18));
        assertThat(nextInHoursTime.getHour(), is(START_HOUR));
        assertThat(new Double(nextInHoursTime.getMinute()), closeTo(0, 59));
    }


    @Test
    public void isOutOfHoursAccountsForUtcConversionFromBstAtEndOfDay() throws NoSuchAlgorithmException {
        int endHour = 17;
        ZonedDateTime timeInUtc = ZonedDateTime.of(2018, 10, 5, endHour - 1, 1, 1, 1, ZoneId.of("UTC"));
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(timeInUtc), 9, endHour).isItOutOfHours();

        assertThat(isOutOfHours, is(true));
    }

    @Test
    public void isOutOfHoursAccountsForUtcConversionFromGmtAtEndOfDay() throws NoSuchAlgorithmException {
        int endHour = 17;
        ZonedDateTime timeInUtc = ZonedDateTime.of(2018, 12, 5, endHour - 1, 1, 1, 1, ZoneId.of("UTC"));
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(timeInUtc), 9, endHour).isItOutOfHours();

        assertThat(isOutOfHours, is(false));
    }

    @Test
    public void isOutOfHoursAccountsForUtcConversionFromBstAtStartOfDay() throws NoSuchAlgorithmException {
        int startHour = 9;
        ZonedDateTime timeInUtc = ZonedDateTime.of(2018, 10, 5, startHour - 1, 1, 1, 1, ZoneId.of("UTC"));
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(timeInUtc), startHour, 17).isItOutOfHours();

        assertThat(isOutOfHours, is(false));
    }

    @Test
    public void isOutOfHoursAccountsForUtcConversionFromGmtAtStartOfDay() throws NoSuchAlgorithmException {
        int startHour = 9;
        ZonedDateTime timeInUtc = ZonedDateTime.of(2018, 12, 5, startHour - 1, 1, 1, 1, ZoneId.of("UTC"));
        boolean isOutOfHours = new OutOfHoursCalculator(new FixedDateTimeProvider(timeInUtc), startHour, 17).isItOutOfHours();

        assertThat(isOutOfHours, is(true));
    }

    @Test
    public void getsNextStartHourInSameZoneAsProvided() throws NoSuchAlgorithmException {
        int endHour = 17;
        ZonedDateTime timeInUtc = ZonedDateTime.of(2018, 10, 5, endHour - 1, 1, 1, 1, ZoneId.of("UTC"));
        int startHour = 9;
        int startHourUtc = startHour - 1;
        ZonedDateTime startOfNextInHoursPeriod = new OutOfHoursCalculator(new FixedDateTimeProvider(timeInUtc), startHour, endHour).getStartOfNextInHoursPeriod();

        assertThat(startOfNextInHoursPeriod.getHour(), is(startHourUtc));
    }

    private ZonedDateTime nowAtHour(int hour) {
        return ZonedDateTime.of(2018, 9, 18, hour, 0, 0, 0, ZoneId.of("Europe/London"));
    }

    public static class FixedDateTimeProvider extends DateTimeProvider {

        private final ZonedDateTime now;

        FixedDateTimeProvider(ZonedDateTime now) {
            this.now = now;
        }

        @Override
        public ZonedDateTime now() {
            return now;
        }
    }
}