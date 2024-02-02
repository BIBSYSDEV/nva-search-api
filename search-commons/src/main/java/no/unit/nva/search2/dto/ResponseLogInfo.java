package no.unit.nva.search2.dto;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search2.common.SwsResponse;

public record ResponseLogInfo(
    long opensearchTime,
    long processingTime,
    int totalHits,
    int payloadSize

)  implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {

        private int responseSize;
        private int totalHits;
        private long responseTime;
        private long opensearchResponseTime;

        private Builder() {
        }

        public Builder withTotalHits(int totalHits) {
            this.totalHits = totalHits;
            return this;
        }

        public Builder withResponseTime(long milliseconds) {
            this.responseTime = milliseconds;
            return this;
        }

        public Builder withOpensearchResponseTime(long milliseconds) {
            this.opensearchResponseTime = milliseconds;
            return this;
        }

        public Builder withResponseSize(int responseSize) {
            this.responseSize = responseSize;
            return this;
        }

        public Builder withSwsResponse(SwsResponse response) {
            return
                this.withOpensearchResponseTime(response.took())
                    .withTotalHits(response.getTotalSize())
                    .withResponseSize(response.toString().length());
        }

        public String toJsonString() {
            return new ResponseLogInfo(
                opensearchResponseTime,
                responseTime,
                totalHits,
                responseSize
            ).toJsonString();
        }
    }
}
