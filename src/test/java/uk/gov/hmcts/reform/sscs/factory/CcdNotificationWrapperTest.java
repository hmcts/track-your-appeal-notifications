package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;

    @Test
    @Parameters({"paper, PAPER", "null, REGULAR", "oral, ORAL", "cor, ONLINE"})
    public void should_returnAccordingAppealHearingType_when_hearingTypeIsPresent(String hearingType,
                                                                                  AppealHearingType expected) {
        ccdNotificationWrapper = buildCcdNotificationWrapper(hearingType);

        assertThat(ccdNotificationWrapper.getHearingType(), is(expected));
    }

    private CcdNotificationWrapper buildCcdNotificationWrapper(String hearingType) {
        return new CcdNotificationWrapper(
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(SscsCaseData.builder()
                                .appeal(Appeal.builder()
                                        .hearingType(hearingType)
                                        .build())
                                .subscriptions(Subscriptions.builder()
                                        .appellantSubscription(Subscription.builder().build())
                                        .representativeSubscription(Subscription.builder().build())
                                        .build())
                                .build())
                        .build()
        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType) {
        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType("cor")
                        .rep(Representative.builder().build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }


    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType(hearingType)
                        .appellant(Appellant.builder().appointee(Appointee.builder().build()).build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .appointeeSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION","APPEAL_WITHDRAWN_NOTIFICATION","EVIDENCE_RECEIVED_NOTIFICATION","POSTPONEMENT_NOTIFICATION","HEARING_BOOKED_NOTIFICATION","SYA_APPEAL_CREATED_NOTIFICATION", "RESEND_APPEAL_CREATED_NOTIFICATION", "APPEAL_RECEIVED_NOTIFICATION", "ADJOURNED_NOTIFICATION", "APPEAL_DORMANT_NOTIFICATION"})
    public void givenSubscriptions_shouldGetSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION, cor", "DWP_RESPONSE_RECEIVED_NOTIFICATION, oral", "SUBSCRIPTION_UPDATED_NOTIFICATION, paper", "APPEAL_LAPSED_NOTIFICATION, paper"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(notificationEventType, hearingType);

        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredOnReps")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutReps(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @SuppressWarnings({"unused"})
    private Object[] getEventTypeFilteredOnReps() {
        return Arrays.stream(values())
            .filter(type -> !(type.equals(APPEAL_LAPSED_NOTIFICATION)
                || type.equals(APPEAL_WITHDRAWN_NOTIFICATION)
                || type.equals(EVIDENCE_RECEIVED_NOTIFICATION)
                || type.equals(SYA_APPEAL_CREATED_NOTIFICATION)
                || type.equals(RESEND_APPEAL_CREATED_NOTIFICATION)
                || type.equals(APPEAL_DORMANT_NOTIFICATION)
                || type.equals(ADJOURNED_NOTIFICATION)
                || type.equals(APPEAL_RECEIVED_NOTIFICATION)
                || type.equals(POSTPONEMENT_NOTIFICATION)
                || type.equals(HEARING_BOOKED_NOTIFICATION)
            )).toArray();
    }

}