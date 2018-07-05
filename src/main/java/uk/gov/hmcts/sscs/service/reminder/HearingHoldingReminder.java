package uk.gov.hmcts.sscs.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.exception.ReminderException;
import uk.gov.hmcts.sscs.extractor.HearingContactDateExtractor;

@Component
public class HearingHoldingReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingHoldingReminder.class);

    private final HearingContactDateExtractor hearingContactDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler<String> jobScheduler;

    @Autowired
    public HearingHoldingReminder(
        HearingContactDateExtractor hearingContactDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler<String> jobScheduler
    ) {
        this.hearingContactDateExtractor = hearingContactDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
    }

    public boolean canHandle(CcdResponse ccdResponse) {
        return ccdResponse
            .getNotificationType()
            .equals(DWP_RESPONSE_RECEIVED);
    }

    public void handle(CcdResponse ccdResponse) {
        if (!canHandle(ccdResponse)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        scheduleReminder(ccdResponse, DWP_RESPONSE_RECEIVED, FIRST_HEARING_HOLDING_REMINDER);
        scheduleReminder(ccdResponse, FIRST_HEARING_HOLDING_REMINDER, SECOND_HEARING_HOLDING_REMINDER);
        scheduleReminder(ccdResponse, SECOND_HEARING_HOLDING_REMINDER, THIRD_HEARING_HOLDING_REMINDER);
        scheduleReminder(ccdResponse, THIRD_HEARING_HOLDING_REMINDER, FINAL_HEARING_HOLDING_REMINDER);
    }

    private void scheduleReminder(
        CcdResponse ccdResponse,
        EventType referenceEventType,
        EventType scheduledEventType
    ) {
        String caseId = ccdResponse.getCaseId();
        String eventId = scheduledEventType.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, referenceEventType);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            caseId,
            reminderDate
        ));

        LOG.info("Scheduled hearing holding reminder for case id: {} @ {}", caseId, reminderDate.toString());
    }

    private ZonedDateTime calculateReminderDate(CcdResponse ccdResponse, EventType referenceEventType) {

        Optional<ZonedDateTime> hearingContactDate =
                hearingContactDateExtractor.extractForReferenceEvent(ccdResponse, referenceEventType);

        if (hearingContactDate.isPresent()) {
            return hearingContactDate.get();
        }

        ReminderException reminderException = new ReminderException(
            new Exception("Could not find reminder date for case id: " + ccdResponse.getCaseId())
        );

        LOG.error("Reminder date not found", reminderException);
        throw reminderException;
    }

}
