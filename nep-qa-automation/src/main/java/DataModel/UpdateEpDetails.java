package DataModel;

import Actions.SimulatedAgentActions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateEpDetails {

        @JsonProperty("Name")
        private String name;
        @JsonProperty("HostName")
        private String hostName;
        @JsonProperty("OsTypeAndVersion")
        private String os;
        @JsonProperty("CustomerId")
        private String customerId;
        @JsonProperty("EndpointIp")
        private String ip;
        @JsonProperty("MacAddress")
        private String macAddress;
        @JsonProperty("BinVer")
        private String binVersion;
        @JsonProperty("ReportingStatus")
        private String reportingStatus;
        @JsonProperty("LastError")
        private EndpointErrorDetails lastError;

        //this field is used to verify rename CentCom call
        private String oldName;


    public UpdateEpDetails(){
        oldName=null;
       }


        public UpdateEpDetails(String customerId, String name ,String ip) {
            this.customerId = customerId;
            this.ip = ip;
            this.name = name;
            oldName=null;

        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOldName() {
        return oldName;
    }

        public void setOldName(String oldName) {
        this.oldName = oldName;
    }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public String getOsTypeAndVersion() {
            return os;
        }

        public void setOsTypeAndVersion(String osTypeAndVersion) {
            this.os = osTypeAndVersion;
        }
        public String getBinVer() {
            return binVersion;
        }

        public void setBinVer(String binVersion) {
            this.binVersion = binVersion;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public String getReportingStatus() {
            return reportingStatus;
        }

        public void setReportingStatus(String reportingStatus) {
            this.reportingStatus = reportingStatus;
        }

        public EndpointErrorDetails getLastError() {
            return lastError;
        }

        public void setLastError(EndpointErrorDetails lastError) {
            this.lastError = lastError;
        }




}
