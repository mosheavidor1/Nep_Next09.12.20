package DataModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointErrorDetails {

        @JsonProperty("timestamp")
        private String timestamp;
        @JsonProperty("error_id")
        private int errorId;
        @JsonProperty("error_msg")
        private String errorMsg;

        public void UpdateEpErrorDetails(String timestamp, int errorId, String errorMsg) {
            this.timestamp = timestamp;
            this.errorId = errorId;
            this.errorMsg = errorMsg;

        }

        public EndpointErrorDetails(String timestamp, int errorId, String errorMsg){
            UpdateEpErrorDetails( timestamp,  errorId,  errorMsg);

        }

        public String getErrorMsg() {
        return errorMsg;
    }




}
