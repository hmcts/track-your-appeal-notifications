package uk.gov.hmcts.reform.sscs.factory;

import java.util.Objects;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public class CcdNotificationWrapper implements NotificationWrapper {
    private final SscsCaseDataWrapper responseWrapper;

    public CcdNotificationWrapper(SscsCaseDataWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    @Override
    public NotificationEventType getNotificationType() {
        return responseWrapper.getNotificationEventType();
    }

    @Override
    public SscsCaseData getNewSscsCaseData() {
        return responseWrapper.getNewSscsCaseData();
    }

    @Override
    public Subscription getAppellantSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
    }

    @Override
    public SscsCaseDataWrapper getSscsCaseDataWrapper() {
        return responseWrapper;
    }

    @Override
    public String getCaseId() {
        return responseWrapper.getNewSscsCaseData().getCaseId();
    }

    public AppealHearingType getHearingType() {
        if (responseWrapper.getNewSscsCaseData().getOnlinePanel() != null) {
            return AppealHearingType.ONLINE;
        } else {
            return AppealHearingType.REGULAR;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdNotificationWrapper that = (CcdNotificationWrapper) o;
        return Objects.equals(responseWrapper, that.responseWrapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseWrapper);
    }
}
