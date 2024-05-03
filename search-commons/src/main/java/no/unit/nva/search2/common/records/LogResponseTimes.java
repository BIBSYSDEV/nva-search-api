package no.unit.nva.search2.common.records;

import no.unit.nva.commons.json.JsonSerializable;

public record LogResponseTimes(
    int totalHits,
    long queryDuration,
    long networkDuration,
    long prePostDuration,
    long totalDuration,
    String query
)  implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {

        private int totalHits;
        private long totalTime;
        private long fetchTime;
        private long searchTime;
        private String searchQuery;

        private Builder() {
        }

        public Builder withFetchTime(long fetchDuration) {
            this.fetchTime = fetchDuration;
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

        public Builder withSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
            return this;
        }

        public Builder withSwsResponse(SwsResponse response) {
            return
                this.withOpensearchResponseTime(response.took())
                    .withTotalHits(response.getTotalSize());
        }

        public String toJsonString() {
            return new LogResponseTimes(
                totalHits,
                searchTime,
                fetchTime - searchTime,
                totalTime - fetchTime,
                totalTime,
                searchQuery
            ).toJsonString();
        }
    }
}
