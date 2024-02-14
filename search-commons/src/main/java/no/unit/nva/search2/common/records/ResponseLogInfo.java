package no.unit.nva.search2.common.records;

import no.unit.nva.commons.json.JsonSerializable;

public record ResponseLogInfo(
    long opensearchTime,
    long processingTime,
    int totalHits

)  implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {

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

        public Builder withSwsResponse(SwsResponse response) {
            return
                this.withOpensearchResponseTime(response.took())
                    .withTotalHits(response.getTotalSize());
        }

        public String toJsonString() {
            return new ResponseLogInfo(
                opensearchResponseTime,
                responseTime,
                totalHits
            ).toJsonString();
        }
    }
}
