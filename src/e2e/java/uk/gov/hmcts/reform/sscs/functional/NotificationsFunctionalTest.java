package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class NotificationsFunctionalTest extends AbstractFunctionalTest {

    @Value("${notification.appealReceived.emailId}")
    private String appealReceivedEmailTemplateId;

    @Value("${notification.appealReceived.smsId}")
    private String appealReceivedSmsTemplateId;

    @Value("${notification.evidenceReceived.emailId}")
    private String evidenceReceivedEmailTemplateId;

    @Value("${notification.evidenceReceived.smsId}")
    private String evidenceReceivedSmsTemplateId;

    @Value("${notification.hearingAdjourned.emailId}")
    private String hearingAdjournedEmailTemplateId;

    @Value("${notification.hearingAdjourned.smsId}")
    private String hearingAdjournedSmsTemplateId;

    @Value("${notification.hearingPostponed.emailId}")
    private String hearingPostponedEmailTemplateId;

    @Value("${notification.appealLapsed.emailId}")
    private String appealLapsedEmailTemplateId;

    @Value("${notification.appealLapsed.smsId}")
    private String appealLapsedSmsTemplateId;

    @Value("${notification.appealWithdrawn.emailId}")
    private String appealWithdrawnEmailTemplateId;

    @Value("${notification.hearingBooked.emailId}")
    private String hearingBookedEmailTemplateId;

    @Value("${notification.hearingBooked.smsId}")
    private String hearingBookedSmsTemplateId;

    @Value("${notification.subscriptionCreated.smsId}")
    private String subscriptionCreatedSmsTemplateId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateId;

    @Value("${notification.online.responseReceived.emailId}")
    private String onlineResponseReceivedEmailId;

    @Value("${notification.online.responseReceived.smsId}")
    private String onlineResponseReceivedSmsId;

    @Test
    public void shouldSendAppealReceivedNotification() throws IOException, NotificationClientException {
        simulateCcdCallback(APPEAL_RECEIVED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                false,
                appealReceivedEmailTemplateId,
                appealReceivedSmsTemplateId
        );
    }

    @Test
    public void shouldSendEvidenceReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_RECEIVED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
                false,
                evidenceReceivedEmailTemplateId,
                evidenceReceivedSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingAdjournedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(ADJOURNED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
            false,
            hearingAdjournedEmailTemplateId,
            hearingAdjournedSmsTemplateId
        );
    }

    @Test
    public void shouldSendHearingPostponedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(POSTPONEMENT_NOTIFICATION);

        tryFetchNotificationsForTestCase(false, hearingPostponedEmailTemplateId);
    }

    @Test
    public void shouldSendAppealLapsedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_LAPSED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
            false,
            appealLapsedEmailTemplateId,
            appealLapsedSmsTemplateId
        );
    }

    @Test
    public void shouldSendAppealWithdrawnNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_WITHDRAWN_NOTIFICATION);

        tryFetchNotificationsForTestCase(false, appealWithdrawnEmailTemplateId);
    }

    @Test
    public void shouldSendHearingBookedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(HEARING_BOOKED_NOTIFICATION);

        tryFetchNotificationsForTestCase(
            false,
            hearingBookedEmailTemplateId,
            hearingBookedSmsTemplateId
        );
    }

    @Test
    public void shouldSendSubscriptionCreatedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SUBSCRIPTION_CREATED_NOTIFICATION);

        tryFetchNotificationsForTestCase(false, subscriptionCreatedSmsTemplateId);
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(SUBSCRIPTION_UPDATED_NOTIFICATION);

        tryFetchNotificationsForTestCase(false, subscriptionUpdatedEmailTemplateId);
    }

    @Test
    public void shouldSendOnlineDwpResponseReceivedNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(DWP_RESPONSE_RECEIVED_NOTIFICATION, "online-" + DWP_RESPONSE_RECEIVED_NOTIFICATION.getId() + "Callback.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(false, onlineResponseReceivedEmailId, onlineResponseReceivedSmsId);

        assertNotificationBodyContains(notifications, onlineResponseReceivedEmailId, caseData.getCaseReference());
    }
}
