package uk.gov.hmcts.sscs.personalisation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.sscs.service.RegionalProcessingCenterService;

public class SubscriptionPersonalisationTest {

    SubscriptionPersonalisation personalisation;

    Subscription newAppellantSubscription;

    Subscription oldAppellantSubscription;

    CcdResponse newCcdResponse;

    CcdResponse oldCcdResponse;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Before
    public void setup() {
        initMocks(this);
        personalisation = new SubscriptionPersonalisation(config, macService, regionalProcessingCenterService);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = new RegionalProcessingCenter();
        rpc.createRegionalProcessingCenter("Venue", "HMCTS", "The Road", "Town", "City", "B23 1EH", "Birmingham");

        when(regionalProcessingCenterService.getByScReferenceCode("1234")).thenReturn(rpc);

        newAppellantSubscription = Subscription.builder()
                .firstName("Harry").surname("Kane").title("Mr").appealNumber("GLSCRR").email("test@email.com")
                .mobileNumber("07983495065").subscribeEmail(true).subscribeSms(true).build();

        oldAppellantSubscription = Subscription.builder()
                .firstName("Harry").surname("Kane").title("Mr").appealNumber("GLSCRR").email("test@email.com")
                .mobileNumber("07983495065").subscribeEmail(false).subscribeSms(false).build();

        newCcdResponse = CcdResponse.builder().caseId("54321").benefitType(PIP).caseReference("1234")
                .appellantSubscription(newAppellantSubscription).notificationType(SUBSCRIPTION_UPDATED).build();

        oldCcdResponse = CcdResponse.builder().caseId("54321").benefitType(PIP).caseReference("5432")
                .appellantSubscription(oldAppellantSubscription).notificationType(SUBSCRIPTION_UPDATED).build();
    }

    @Test
    public void customisePersonalisation() {
        Map<String, String> result = personalisation.create(CcdResponseWrapper.builder().newCcdResponse(newCcdResponse).oldCcdResponse(oldCcdResponse).build());

        assertEquals("PIP benefit", result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(APPEAL_REF));
        assertEquals("GLSCRR", result.get(APPEAL_ID));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("01234543225", result.get(PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(DWP_FUL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToMostRecentWhenNewSubscription() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());
        newCcdResponse.setEvents(events);

        personalisation.create(CcdResponseWrapper.builder().newCcdResponse(newCcdResponse).oldCcdResponse(oldCcdResponse).build());

        assertEquals(APPEAL_RECEIVED, newCcdResponse.getNotificationType());
    }

    @Test
    public void customisePersonalisationSetsNotificationTypeToDoNotSendWhenEmailHasNotChanged() {
        newAppellantSubscription.setSubscribeEmail(true);
        oldAppellantSubscription.setSubscribeEmail(true);

        personalisation.create(CcdResponseWrapper.builder().newCcdResponse(newCcdResponse).oldCcdResponse(oldCcdResponse).build());

        assertEquals(DO_NOT_SEND, newCcdResponse.getNotificationType());
    }

    @Test
    public void customisePersonalisationShouldLeaveNotificationTypeAsSubscriptionUpdatedWhenEmailHasChanged() {
        newAppellantSubscription.setSubscribeEmail(true);
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail(true);

        personalisation.create(CcdResponseWrapper.builder().newCcdResponse(newCcdResponse).oldCcdResponse(oldCcdResponse).build());

        assertEquals(SUBSCRIPTION_UPDATED, newCcdResponse.getNotificationType());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeSms(true);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsNotSet() {
        newAppellantSubscription.setSubscribeSms(false);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void emptyOldAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        oldCcdResponse.setAppellantSubscription(null);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void emptyNewAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        newCcdResponse.setAppellantSubscription(null);

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newCcdResponse, oldCcdResponse);

        assertFalse(result);
    }

    @Test
    public void setMostRecentEventTypeNotificationWhenEmailSubscribedIsFirstSet() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());
        newCcdResponse.setEvents(events);

        assertEquals(APPEAL_RECEIVED, personalisation.setEventTypeNotification(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void doNotSendEventTypeNotificationWhenEmailSubscribedIsAlreadySet() {
        oldAppellantSubscription.setSubscribeEmail(true);

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());
        newCcdResponse.setEvents(events);

        assertEquals(DO_NOT_SEND, personalisation.setEventTypeNotification(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void doNotUpdateMostRecentEventTypeNotificationWhenEventTypeIsNotKnown() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(null).build());
        newCcdResponse.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void emptyOldAppellantSubscriptionDoesNotUpdateNotificationType() {
        oldCcdResponse.setAppellantSubscription(null);

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());
        newCcdResponse.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void emptyNewAppellantSubscriptionDoesNotUpdateNotificationType() {
        newCcdResponse.setAppellantSubscription(null);

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());
        newCcdResponse.setEvents(events);

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void emptyEventsDoesNotUpdateNotificationType() {
        newCcdResponse.setEvents(new ArrayList<>());

        assertEquals(SUBSCRIPTION_UPDATED, personalisation.setEventTypeNotification(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void givenEmailNotChangedAndSubscriptionWasAndStillIsSubscribed_thenReturnTrue() {
        newAppellantSubscription.setSubscribeEmail(true);
        oldAppellantSubscription.setSubscribeEmail(true);

        assertEquals(true, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void givenEmailChangedAndSubscriptionWasAndStillIsSubscribed_thenReturnFalse() {
        newAppellantSubscription.setSubscribeEmail(true);
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail(true);

        assertEquals(false, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse));
    }

    @Test
    public void givenEmailChangedAndNowSubscribed_thenReturnFalse() {
        newAppellantSubscription.setSubscribeEmail(true);
        newAppellantSubscription.setEmail("changed@test.com");
        oldAppellantSubscription.setSubscribeEmail(false);

        assertEquals(false, personalisation.doNotSendEmailUpdatedNotificationWhenEmailNotChanged(newCcdResponse, oldCcdResponse));
    }
}
