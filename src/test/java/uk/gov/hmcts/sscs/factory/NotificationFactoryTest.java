package uk.gov.hmcts.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.sscs.domain.notify.Notification;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.personalisation.Personalisation;
import uk.gov.hmcts.sscs.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.sscs.service.RegionalProcessingCenterService;

public class NotificationFactoryTest {

    private static final String CASE_ID = "54321";

    private NotificationFactory factory;

    private CcdResponseWrapper wrapper;

    private CcdResponse ccdResponse;

    private Personalisation personalisation;

    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private PersonalisationFactory personalisationFactory;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    private Subscription subscription;

    //TODO: Maybe sort out these objects as repeating stuff
    @Before
    public void setup() {
        initMocks(this);
        personalisation = new Personalisation(config, macService, regionalProcessingCenterService);
        subscriptionPersonalisation = new SubscriptionPersonalisation(config, macService, regionalProcessingCenterService);
        factory = new NotificationFactory(personalisationFactory);

        subscription = Subscription.builder()
                .firstName("Ronnie").surname("Scott").title("Mr").appealNumber("ABC").email("test@testing.com")
                .mobileNumber("07985858594").subscribeEmail(true).subscribeSms(false).build();

        ccdResponse = CcdResponse.builder().caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appellantSubscription(subscription).notificationType(APPEAL_RECEIVED).build();

        wrapper = CcdResponseWrapper.builder().newCcdResponse(ccdResponse).build();

        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(macService.generateToken("ABC", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = new RegionalProcessingCenter("Venue", "HMCTS", "The Road", "Town", "City", "B23 1EH", "Birmingham");
        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);
    }

    @Test
    public void buildNotificationFromCcdResponse() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(personalisation);
        when(config.getTemplate(APPEAL_RECEIVED.getId(), APPEAL_RECEIVED.getId())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(null).build());
        Notification result = factory.create(wrapper);

        assertEquals("123", result.getEmailTemplate());
        assertEquals("test@testing.com", result.getEmail());
        assertEquals("ABC", result.getAppealNumber());
    }

    @Test
    public void buildSubscriptionCreatedSmsNotificationFromCcdResponseWithSubscriptionUpdatedNotificationAndSmsFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED.getId(), SUBSCRIPTION_CREATED.getId())).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId("123").build());

        wrapper = CcdResponseWrapper.builder()
                .newCcdResponse(
                    ccdResponse.toBuilder()
                        .appellantSubscription(subscription.toBuilder().subscribeSms(true).subscribeEmail(false).build())
                        .notificationType(SUBSCRIPTION_UPDATED)
                    .build())
                .oldCcdResponse(
                    ccdResponse.toBuilder()
                        .appellantSubscription(subscription.toBuilder().subscribeSms(false).subscribeEmail(false).build())
                        .notificationType(SUBSCRIPTION_UPDATED)
                    .build())
                .build();

        Notification result = factory.create(wrapper);

        assertEquals("123", result.getSmsTemplate());
    }

    @Test
    public void buildSubscriptionUpdatedSmsNotificationFromCcdResponseWithSubscriptionUpdatedNotificationAndSmsAlreadySubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED.getId(), SUBSCRIPTION_UPDATED.getId())).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId("123").build());

        wrapper = CcdResponseWrapper.builder()
                .newCcdResponse(
                    ccdResponse.toBuilder()
                        .appellantSubscription(subscription.toBuilder().subscribeSms(true).subscribeEmail(false).build())
                        .notificationType(SUBSCRIPTION_UPDATED)
                    .build())
                .oldCcdResponse(
                    ccdResponse.toBuilder()
                        .appellantSubscription(subscription.toBuilder().subscribeSms(true).subscribeEmail(true).build())
                        .notificationType(SUBSCRIPTION_UPDATED)
                    .build())
                .build();

        Notification result = factory.create(wrapper);

        assertEquals("123", result.getSmsTemplate());
    }

    @Test
    public void buildLastEmailNotificationFromCcdResponseEventWhenEmailFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(APPEAL_RECEIVED.getId(), SUBSCRIPTION_CREATED.getId())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());

        wrapper = CcdResponseWrapper.builder()
                .newCcdResponse(
                    ccdResponse.toBuilder()
                        .appellantSubscription(subscription.toBuilder().subscribeSms(true).subscribeEmail(true).build())
                        .notificationType(SUBSCRIPTION_UPDATED)
                        .events(events)
                        .build())
                .oldCcdResponse(
                    ccdResponse.toBuilder()
                        .appellantSubscription(subscription.toBuilder().subscribeSms(false).subscribeEmail(false).build())
                        .notificationType(SUBSCRIPTION_UPDATED)
                        .build())
                .build();

        Notification result = factory.create(wrapper);

        assertEquals("123", result.getEmailTemplate());
    }

    @Test
    public void buildNoNotificationFromCcdResponseWhenSubscriptionUpdateReceivedWithNoChangeInEmailAddress() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(DO_NOT_SEND.getId(), SUBSCRIPTION_CREATED.getId())).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());

        wrapper = CcdResponseWrapper.builder()
            .newCcdResponse(
                ccdResponse.toBuilder()
                    .appellantSubscription(subscription.toBuilder().subscribeSms(true).subscribeEmail(true).build())
                    .notificationType(SUBSCRIPTION_UPDATED)
                    .events(events)
                    .build())
            .oldCcdResponse(
                ccdResponse.toBuilder()
                    .appellantSubscription(subscription.toBuilder().subscribeSms(false).subscribeEmail(true).build())
                    .notificationType(SUBSCRIPTION_UPDATED)
                    .build())
            .build();

        Notification result = factory.create(wrapper);

        assertNull(result.getEmailTemplate());
    }

    @Test
    public void buildSubscriptionUpdatedNotificationFromCcdResponseWhenEmailIsChanged() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(SUBSCRIPTION_UPDATED.getId(), SUBSCRIPTION_CREATED.getId())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(null).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().dateTime(ZonedDateTime.now()).eventType(APPEAL_RECEIVED).build());

        wrapper = CcdResponseWrapper.builder()
            .newCcdResponse(
                ccdResponse.toBuilder()
                    .appellantSubscription(subscription.toBuilder().email("changed@testing.com").subscribeSms(true).subscribeEmail(true).build())
                    .notificationType(SUBSCRIPTION_UPDATED)
                    .events(events)
                    .build())
            .oldCcdResponse(
                ccdResponse.toBuilder()
                    .appellantSubscription(subscription.toBuilder().subscribeSms(false).subscribeEmail(true).build())
                    .notificationType(SUBSCRIPTION_UPDATED)
                    .build())
            .build();

        Notification result = factory.create(wrapper);

        assertEquals("123", result.getEmailTemplate());
    }

    @Test
    public void returnNullIfPersonalisationNotFound() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(null);
        Notification result = factory.create(wrapper);

        assertNull(result);
    }
}
