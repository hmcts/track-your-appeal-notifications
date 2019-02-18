package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCode;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.getSubscription;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isMandatoryLetter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.*;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@Service
@Slf4j
public class NotificationService {
    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;
    private final SendNotificationService sendNotificationService;

    @Autowired
    public NotificationService(
            NotificationFactory notificationFactory,
            ReminderService reminderService,
            NotificationValidService notificationValidService,
            NotificationHandler notificationHandler,
            OutOfHoursCalculator outOfHoursCalculator,
            NotificationConfig notificationConfig,
            SendNotificationService sendNotificationService) {
        this.notificationFactory = notificationFactory;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
        this.sendNotificationService = sendNotificationService;
    }

    public void manageNotificationAndSubscription(NotificationWrapper notificationWrapper) {
        NotificationEventType notificationType = notificationWrapper.getNotificationType();
        final String caseId = notificationWrapper.getCaseId();
        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);

        if (notificationWrapper.getNotificationType().isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
            sendNotificationPerSubscription(notificationWrapper, notificationType);
        } else {
            notificationHandler.scheduleNotification(notificationWrapper);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper,
                                                 NotificationEventType notificationType) {
        for (SubscriptionWithType subscriptionWithType : notificationWrapper.getSubscriptionsBasedOnNotificationType()) {

            if (isValidNotification(notificationWrapper, subscriptionWithType.getSubscription(), notificationType)) {
                sendNotification(notificationWrapper, subscriptionWithType);
                reminderService.createReminders(notificationWrapper);
                resendLastNotification(notificationWrapper, subscriptionWithType);
            }
        }
    }

    private void resendLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        if (shouldProcessLastNotification(notificationWrapper, subscriptionWithType)) {
            log.info("resending the last notification");
            scrubMobileAndSmsIfSubscribedBefore(notificationWrapper, subscriptionWithType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(
                    getNotificationByCcdEvent(notificationWrapper.getNewSscsCaseData().getEvents().get(0)
                            .getValue().getEventType()));
            sendNotification(notificationWrapper, subscriptionWithType);
        }
    }

    private void scrubMobileAndSmsIfSubscribedBefore(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Subscription oldSubscription = getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType());
        Subscription newSubscription = subscriptionWithType.getSubscription();
        String email = oldSubscription.isEmailSubscribed() ? null : newSubscription.getEmail();
        String mobile = oldSubscription.isSmsSubscribed() ? null : newSubscription.getMobile();
        subscriptionWithType.setSubscription(newSubscription.toBuilder().email(email).mobile(mobile).build());
    }

    private boolean shouldProcessLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return SUBSCRIPTION_UPDATED_NOTIFICATION.equals(notificationWrapper.getSscsCaseDataWrapper().getNotificationEventType())
                && !isPaperCase(notificationWrapper.getNewSscsCaseData().getAppeal().getHearingType())
                && hasCaseJustSubscribed(subscriptionWithType.getSubscription(), getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType()))
                && thereIsALastEventThatIsNotSubscriptionUpdated(notificationWrapper.getNewSscsCaseData());
    }

    static boolean isPaperCase(String hearingType) {
        return StringUtils.equalsIgnoreCase(AppealHearingType.PAPER.name(), hearingType);
    }

    static Boolean hasCaseJustSubscribed(Subscription newSubscription, Subscription oldSubscription) {
        return oldSubscription != null && newSubscription != null
                && (!oldSubscription.isEmailSubscribed() && newSubscription.isEmailSubscribed()
                || (!oldSubscription.isSmsSubscribed() && newSubscription.isSmsSubscribed()));
    }

    private static boolean thereIsALastEventThatIsNotSubscriptionUpdated(final SscsCaseData newSscsCaseData) {
        boolean thereIsALastEventThatIsNotSubscriptionUpdated = newSscsCaseData.getEvents() != null
                && !newSscsCaseData.getEvents().isEmpty()
                && newSscsCaseData.getEvents().get(0).getValue().getEventType() != null
                && !SUBSCRIPTION_UPDATED.equals(newSscsCaseData.getEvents().get(0).getValue().getEventType());
        if (!thereIsALastEventThatIsNotSubscriptionUpdated) {
            log.info("Not re-sending the last subscription as there is no last event on the case.");
        }
        return thereIsALastEventThatIsNotSubscriptionUpdated;
    }

    private void sendNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Notification notification = notificationFactory.create(notificationWrapper, subscriptionWithType);
        sendNotificationService.sendEmailSmsLetterNotification(notificationWrapper, subscriptionWithType.getSubscription(), notification);
        processOldSubscriptionNotifications(notificationWrapper, notification, subscriptionWithType);
    }

    private boolean isValidNotification(NotificationWrapper wrapper, Subscription
            subscription, NotificationEventType notificationType) {
        return (isMandatoryLetter(notificationType) || (subscription != null && subscription.doesCaseHaveSubscriptions()
                && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
                && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType)));
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType) {
        if (wrapper.getNotificationType() == NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION) {

            Subscription newSubscription = subscriptionWithType.getSubscription();
            Subscription oldSubscription = getSubscription(wrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType());

            String emailAddress = null;
            String smsNumber = null;

            if (null != newSubscription.getEmail() && !newSubscription.getEmail().equals(oldSubscription.getEmail())) {
                emailAddress = newSubscription.getEmail().equals(oldSubscription.getEmail()) ? null : oldSubscription.getEmail();
            }

            if (null != newSubscription.getMobile() && !newSubscription.getMobile().equals(oldSubscription.getMobile())) {
                smsNumber = newSubscription.getMobile().equals(oldSubscription.getMobile()) ? null : oldSubscription.getMobile();
            }

            Destination destination = Destination.builder().email(emailAddress).sms(smsNumber).build();

            Benefit benefit = getBenefitByCode(wrapper.getSscsCaseDataWrapper()
                    .getNewSscsCaseData().getAppeal().getBenefitType().getCode());

            Template template = notificationConfig.getTemplate(
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    benefit,
                    wrapper.getHearingType()
            );

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                    .destination(destination)
                    .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                    .appealNumber(notification.getAppealNumber())
                    .placeholders(notification.getPlaceholders()).build();

            sendNotificationService.sendEmailSmsLetterNotification(wrapper, oldSubscription, oldNotification);
        }
    }


}
