package uk.gov.hmcts.reform.sscs.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class HearingContactDateExtractorTest {

    @Mock
    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;

    private final ZonedDateTime dwpResponseReceivedDate =
            ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

    private HearingContactDateExtractor hearingContactDateExtractor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        hearingContactDateExtractor = new HearingContactDateExtractor(
                dwpResponseReceivedDateExtractor,
                60, 3600, 120);
    }

    @Test
    @Parameters({
            "DWP_RESPONSE_RECEIVED_NOTIFICATION, oral, 2018-01-01T14:02:18Z[Europe/London]",
            "DWP_RESPONSE_RECEIVED_NOTIFICATION, paper, 2018-01-01T14:03:18Z[Europe/London]",
            "POSTPONEMENT_NOTIFICATION, oral, 2018-01-01T14:02:18Z[Europe/London]"
    })
    public void extractsFirstHearingContactDate(NotificationEventType notificationEventType, String hearingType,
                                                String expectedHearingContactDate) {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(notificationEventType,
                hearingType);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData()))
                .thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor
                .extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(hearingContactDate.isPresent());
        assertEquals(ZonedDateTime.parse(expectedHearingContactDate), hearingContactDate.get());
    }

    @Test
    public void givenDwpResponseReceivedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertTrue(dwpResponseReceivedDate.isPresent());
        assertEquals(expectedHearingContactDate, dwpResponseReceivedDate.get());
    }

    @Test
    public void givenAdjournedEvent_thenExtractDateForReferenceEvent() {

        ZonedDateTime expectedHearingContactDate = ZonedDateTime.parse("2018-01-01T14:02:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(ADJOURNED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));

        Optional<ZonedDateTime> hearingContactDate =
                hearingContactDateExtractor.extractForReferenceEvent(
                        wrapper.getNewSscsCaseData(),
                        ADJOURNED_NOTIFICATION
                );

        assertTrue(hearingContactDate.isPresent());
        assertEquals(expectedHearingContactDate, hearingContactDate.get());
    }

    @Test
    public void returnsEmptyOptionalWhenDwpResponseReceivedDateIsNotPresent() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED_NOTIFICATION);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertFalse(dwpResponseReceivedDate.isPresent());
    }

    @Test
    public void returnsEmptyOptionalWhenNotificationEventTypeNotAcceptable() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(APPEAL_RECEIVED_NOTIFICATION);

        Optional<ZonedDateTime> dwpResponseReceivedDate = hearingContactDateExtractor.extract(wrapper.getSscsCaseDataWrapper());

        assertFalse(dwpResponseReceivedDate.isPresent());

        verify(dwpResponseReceivedDateExtractor, never()).extract(wrapper.getNewSscsCaseData());
    }

}
