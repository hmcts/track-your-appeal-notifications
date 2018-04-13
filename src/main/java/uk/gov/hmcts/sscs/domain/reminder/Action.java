package uk.gov.hmcts.sscs.domain.reminder;

import lombok.Value;
import org.json.JSONObject;

@Value
public class Action {

    private String url;
    private String body;
    private String method;

    public Action(String appealNumber, String reminderType, String callbackUrl) {
        this.url = callbackUrl;
        this.body = createJsonBody(appealNumber, reminderType);
        this.method = "POST";
    }

    private String createJsonBody(String caseId, String reminderType) {
        JSONObject json = new JSONObject();
        json.put("caseId", caseId);
        json.put("eventId", reminderType);

        return json.toString();
    }
}
