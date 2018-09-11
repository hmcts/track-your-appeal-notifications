package uk.gov.hmcts.reform.sscs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

@Component
public class NotificationConfig {

    @Value("${manage.emails.link}")
    private String manageEmailsLink;
    @Value("${hmcts.phone.number}")
    private String hmctsPhoneNumber;
    @Value("${track.appeal.link}")
    private String trackAppealLink;
    @Value("${evidence.submission.info.link}")
    private String evidenceSubmissionInfoLink;
    @Value("${claiming.expenses.link}")
    private String claimingExpensesLink;
    @Value("${hearing.info.link}")
    private String hearingInfoLink;
    @Value("${online.hearing.link}")
    private String onlineHearingLink;

    @Autowired
    private Environment env;

    public String getHmctsPhoneNumber() {
        return hmctsPhoneNumber;
    }

    public Link getManageEmailsLink() {
        return Link.builder().linkUrl(manageEmailsLink).build();
    }

    public Link getTrackAppealLink() {
        return Link.builder().linkUrl(trackAppealLink).build();
    }

    public Link getEvidenceSubmissionInfoLink() {
        return Link.builder().linkUrl(evidenceSubmissionInfoLink).build();
    }

    public Link getClaimingExpensesLink() {
        return Link.builder().linkUrl(claimingExpensesLink).build();
    }

    public Link getHearingInfoLink() {
        return Link.builder().linkUrl(hearingInfoLink).build();
    }

    public Link getOnlineHearingLink() {
        return Link.builder().linkUrl(onlineHearingLink + "?email={email}").build();
    }

    public Template getTemplate(String emailTemplateName, String smsTemplateName, Benefit benefit) {
        return Template.builder().emailTemplateId(env.getProperty("notification." + emailTemplateName + ".emailId"))
                .smsTemplateId(env.getProperty("notification." + smsTemplateName + ".smsId"))
                .smsSenderTemplateId(env.getProperty("smsSender." + benefit.toString().toLowerCase())).build();
    }
}
