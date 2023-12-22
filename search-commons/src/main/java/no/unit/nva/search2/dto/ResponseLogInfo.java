package no.unit.nva.search2.dto;

import no.unit.nva.commons.json.JsonSerializable;

public record ResponseLogInfo(
    long opensearchResponseTime,
    long responseTime,
    int totalHits
)  implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }

    private ResponseLogInfo(Builder builder) {
        this( builder.opensearchResponseTime, builder.responseTime, builder.totalHits);
    }

    public static final class Builder {
        private int totalHits;
        private long responseTime;
        private long opensearchResponseTime;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withTotalHits(int totalHits) {
            this.totalHits = totalHits;
            return this;
        }

        public Builder withResponseTime(long responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public Builder withOpensearchResponseTime(long opensearchResponseTime) {
            this.opensearchResponseTime = opensearchResponseTime;
            return this;
        }

        public String toJsonString() {
            return new ResponseLogInfo(this).toJsonString();
        }
    }
}
