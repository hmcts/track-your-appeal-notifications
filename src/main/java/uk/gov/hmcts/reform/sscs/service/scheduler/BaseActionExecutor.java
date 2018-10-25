package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public abstract class BaseActionExecutor<T> implements JobExecutor<T> {
    protected static final Logger LOG = getLogger(BaseActionExecutor.class);
    protected final NotificationService notificationService;
    protected final CcdService ccdService;
    protected final SscsCaseDataWrapperDeserializer deserializer;
    protected final IdamService idamService;
    protected final Environment environment;

    BaseActionExecutor(NotificationService notificationService, CcdService ccdService, SscsCaseDataWrapperDeserializer deserializer, IdamService idamService, Environment environment) {
        this.notificationService = notificationService;
        this.ccdService = ccdService;
        this.deserializer = deserializer;
        this.idamService = idamService;
        this.environment = environment;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, T payload) {

        // Get the infrastructure env and slot from properties
        String infraEnv = environment.getProperty("infrastructure.env.name");
        String slotName = environment.getProperty("slot.name");

        if (!("PROD".equalsIgnoreCase(infraEnv) && "STAGING".equalsIgnoreCase(slotName))) {
            long caseId = getCaseId(payload);
            LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

            IdamTokens idamTokens = idamService.getIdamTokens();

            SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

            if (caseDetails != null) {
                SscsCaseDataWrapper wrapper = deserializer.buildSscsCaseDataWrapper(buildCcdNode(caseDetails, eventId));

                notificationService.createAndSendNotification(getWrapper(wrapper, payload));
                updateCase(caseId, wrapper, idamTokens);
            } else {
                LOG.warn("Case id: {} could not be found for event: {}", caseId, eventId);
            }
        }
    }

    private ObjectNode buildCcdNode(SscsCaseDetails caseDetails, String jobName) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(caseDetails);
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node.set("case_details", jsonNode);
        node = node.put("event_id", jobName);

        return node;
    }

    protected abstract void updateCase(Long caseId, SscsCaseDataWrapper wrapper, IdamTokens idamTokens);

    protected abstract NotificationWrapper getWrapper(SscsCaseDataWrapper wrapper, T payload);

    protected abstract long getCaseId(T payload);
}
