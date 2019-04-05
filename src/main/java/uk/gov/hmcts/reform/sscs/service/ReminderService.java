package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.reminder.ReminderHandler;

@Service
public class ReminderService {

    private List<ReminderHandler> reminderHandlers;

    @Autowired
    ReminderService(List<ReminderHandler> reminderHandlers) {
        this.reminderHandlers = reminderHandlers;
    }

    void createReminders(NotificationWrapper wrapper) {
        if (AppealHearingType.ONLINE != wrapper.getHearingType()) {
            for (ReminderHandler reminderHandler : reminderHandlers) {
                if (reminderHandler.canHandle(wrapper) && reminderHandler.canSchedule(wrapper)) {
                    reminderHandler.handle(wrapper);
                }
            }
        }
    }

}
