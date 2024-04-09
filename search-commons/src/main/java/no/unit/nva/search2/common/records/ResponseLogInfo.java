package no.unit.nva.search2.common.records;

import no.unit.nva.commons.json.JsonSerializable;

public record ResponseLogInfo(
    long queryTime,
    long fetchTime,
    long postFetchTime,
    long clientTime,
    long totalTime,
    int totalHits

)  implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {

        private int totalHits;
        private long totalTime;
        private long clientTime;
        private long fetchTime;
        private long searchTime;

        private Builder() {
        }

        public Builder withFetchTime(long fetchDuration) {
            this.fetchTime = fetchDuration;
            return this;
        }

        public Builder withClientTime(long milliseconds) {
            this.clientTime = milliseconds;
            return this;
        }

        public Builder withTotalTime(long milliseconds) {
            this.totalTime = milliseconds;
            return this;
        }

        public Builder withOpensearchResponseTime(long milliseconds) {
            this.searchTime = milliseconds;
            return this;
        }

        public Builder withTotalHits(int totalHits) {
            this.totalHits = totalHits;
            return this;
        }

        public Builder withSwsResponse(SwsResponse response) {
            return
                this.withOpensearchResponseTime(response.took())
                    .withTotalHits(response.getTotalSize());
        }

        public String toJsonString() {
            return new ResponseLogInfo(
                searchTime,
                fetchTime - searchTime,
                totalTime - fetchTime,
                clientTime,
                totalTime,
                totalHits
            ).toJsonString();
        }
    }
}
