package uk.gov.hmcts.sscs.factory;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;
import uk.gov.hmcts.sscs.personalisation.Personalisation;

@Component
public class PersonalisationFactory implements Function<NotificationType, Personalisation> {

    private final NotificationConfig config;

    @Autowired
    public PersonalisationFactory(NotificationConfig config) {
        this.config = config;
    }

    @Override
    public Personalisation apply(NotificationType notificationType) {
        if (notificationType != null) {
            return new Personalisation(config);
        }
        return null;
    }
}
