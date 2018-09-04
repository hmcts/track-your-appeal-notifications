package uk.gov.hmcts.reform.sscs.personalisation;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionService;

public class CohPersonalisationTest {

    private static final String CASE_ID = "54321";

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    public CohPersonalisation cohPersonalisation;

    @Before
    public void setup() {
        initMocks(this);
        when(config.getHmctsPhoneNumber()).thenReturn("01234543225");
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder().build();
        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);
    }

    @Test
    public void addsQuestionEndDate() {
        Subscription appellantSubscription = Subscription.builder()
                .tya("GLSCRR")
                .email("test@email.com")
                .mobile("07983495065")
                .subscribeEmail("Yes")
                .subscribeSms("No")
                .build();
        Name name = Name.builder().firstName("Harry").lastName("Kane").title("Mr").build();
        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

        String date = "2018-07-01T14:01:18.243";


        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(date).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
                .caseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .notificationType(APPEAL_RECEIVED)
                .events(events)
                .build();

        SscsCaseDataWrapper sscsCaseDataWrapper = SscsCaseDataWrapper.builder().newSscsCaseData(response).build();

        String someHearingId = "someHearingId";
        String expectedRequiredByDate = "expectedRequiredByDate";
        IdamTokens idamTokens = IdamTokens.builder().build();

        when(questionService.getQuestionRequiredByDate(idamTokens, someHearingId)).thenReturn(expectedRequiredByDate);

        Map<String, String> placeholders = cohPersonalisation.create(new CohNotificationWrapper(idamTokens, someHearingId, sscsCaseDataWrapper));

        assertThat(placeholders, hasEntry("questions_end_date", expectedRequiredByDate));
    }
}