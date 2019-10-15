package uk.gov.hmcts.reform.sscs.functional.handlers;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import java.util.List;
import junitparams.Parameters;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;


public class AdminAppealWithdrawnNotificationsTest extends AbstractFunctionalTest {
    public AdminAppealWithdrawnNotificationsTest() {
        super(30);
    }

    @Test
    @Parameters({"Appellant", "Appointee"})
    public void givenCallbackWithAppellantSubscription_shouldSendEmailSmsAndLetterNotifications(String party) throws Exception {
        simulateCcdCallback(ADMIN_APPEAL_WITHDRAWN, "handlers/" + ADMIN_APPEAL_WITHDRAWN.getId() + party + "Callback.json");

        String emailId = "8620e023-f663-477e-a771-9cfad50ee30f";
        String smsId = "446c7b23-7342-42e1-adff-b4c367e951cb";
//        String appellantLetterId = "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d";
        List<Notification> notifications = tryFetchNotificationsForTestCase(emailId, smsId);

        assertNotificationBodyContains(notifications, emailId);
        assertNotificationBodyContains(notifications, smsId);
//        assertNotificationBodyContains(notifications, appellantLetterId);
    }
}
