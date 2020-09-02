package DataModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbJson {

    @JsonProperty("EndpointId")
    private String endpointId;
    @JsonProperty("DsLastCommunicationTime")
    private String dsLastCommunicationTime;
    @JsonProperty("DsInitialHost")
    private String dsInitialHost;
    @JsonProperty("EndpointCompName")
    private String endpointCompName;


    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getDsLastCommunicationTime() {
        return dsLastCommunicationTime;
    }

    public void setDsLastCommunicationTime(String dsLastCommunicationTime) {
        this.dsLastCommunicationTime = dsLastCommunicationTime;
    }

    public String getDsInitialHost() {
        return dsInitialHost;
    }

    public void setDsInitialHost(String dsInitialHost) {
        this.dsInitialHost = dsInitialHost;
    }

    public String getEndpointCompName() {
        return endpointCompName;
    }

    public void setEndpointCompName(String endpointCompName) {
        this.endpointCompName = endpointCompName;
    }
}
