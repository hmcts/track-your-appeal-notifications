package uk.gov.hmcts.sscs.personalisation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.*;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.sscs.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.sscs.service.coh.QuestionRounds;
import uk.gov.hmcts.sscs.service.coh.QuestionService;

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
        when(config.getOnlineHearingLink()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = new RegionalProcessingCenter();
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


        List<Events> events = new ArrayList<>();
        events.add(Events.builder().value(Event.builder().date(date).type(APPEAL_RECEIVED.getId()).build()).build());

        CcdResponse response = CcdResponse.builder()
                .caseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder().name(name).build())
                        .build())
                .subscriptions(subscriptions)
                .notificationType(APPEAL_RECEIVED)
                .events(events)
                .build();

        CcdResponseWrapper ccdResponseWrapper = CcdResponseWrapper.builder().newCcdResponse(response).build();

        String someHearingId = "someHearingId";
        String expectedRequiredByDate = "expectedRequiredByDate";
        IdamTokens idamTokens = IdamTokens.builder().build();

        when(questionService.getQuestionRequiredByDate(idamTokens, someHearingId)).thenReturn(expectedRequiredByDate);

        Map<String, String> placeholders = cohPersonalisation.create(new CohNotificationWrapper(idamTokens, someHearingId, ccdResponseWrapper));

        assertThat(placeholders, hasEntry("questions_end_date", expectedRequiredByDate));
    }

    @Test
    public void setsCorrectTemplatesForFirstQuestionRound() {
        String someHearingId = "someHearingId";
        IdamTokens idamTokens = IdamTokens.builder().build();
        QuestionRounds questionRounds = mock(QuestionRounds.class);
        when(questionService.getQuestionRounds(idamTokens, someHearingId)).thenReturn(questionRounds);
        when(questionRounds.getCurrentQuestionRound()).thenReturn(1);
        CohNotificationWrapper cohNotificationWrapper = new CohNotificationWrapper(
                idamTokens,
                someHearingId,
                CcdResponseWrapper.builder().newCcdResponse(
                        CcdResponse.builder().notificationType(EventType.QUESTION_ROUND_ISSUED).build()
                ).build());
        Template expectedTemplate = Template.builder().build();
        when(config.getTemplate(EventType.QUESTION_ROUND_ISSUED.getId(), EventType.QUESTION_ROUND_ISSUED.getId(), Benefit.PIP)).thenReturn(expectedTemplate);

        Template template = cohPersonalisation.getTemplate(cohNotificationWrapper, Benefit.PIP);
        assertThat(template, is(expectedTemplate));
    }

    @Test
    public void setsCorrectTemplatesForSecondQuestionRound() {
        String someHearingId = "someHearingId";
        IdamTokens idamTokens = IdamTokens.builder().build();
        QuestionRounds questionRounds = mock(QuestionRounds.class);
        when(questionService.getQuestionRounds(idamTokens, someHearingId)).thenReturn(questionRounds);
        when(questionRounds.getCurrentQuestionRound()).thenReturn(2);
        CohNotificationWrapper cohNotificationWrapper = new CohNotificationWrapper(idamTokens, someHearingId, CcdResponseWrapper.builder().build());
        Template expectedTemplate = Template.builder().build();
        when(config.getTemplate("follow_up_question_round_issued", "follow_up_question_round_issued", Benefit.PIP)).thenReturn(expectedTemplate);

        Template template = cohPersonalisation.getTemplate(cohNotificationWrapper, Benefit.PIP);
        assertThat(template, is(expectedTemplate));
    }
}