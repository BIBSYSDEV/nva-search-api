package no.unit.nva.search.common.records;

import no.unit.nva.commons.json.JsonSerializable;

/**
 * @author Stig Norland
 */
public record ResponseLogInfo(
    int totalHits,
    int hitsReturned,
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
        private int hitsReturned;
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

        public Builder withHitsReturned(int hitsReturned) {
            this.hitsReturned = hitsReturned;
            return this;
        }


        public Builder withSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
            return this;
        }

        public Builder withSwsResponse(SwsResponse response) {
            return
                this.withOpensearchResponseTime(response.took())
                    .withTotalHits(response.getTotalSize())
                    .withHitsReturned(response.getSearchHits().size());
        }

        public String toJsonString() {
            return new ResponseLogInfo(
                totalHits,
                hitsReturned,
                searchTime,
                fetchTime - searchTime,
                totalTime - fetchTime,
                totalTime,
                searchQuery
            ).toJsonString();
        }
    }
}
