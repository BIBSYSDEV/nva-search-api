package no.unit.nva.indexing.testutils;

import no.unit.nva.search.common.csv.ExportCsv;

import nva.commons.core.ioutils.IoUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FakeSearchResponse {

    public static final String COMMA_DELIMITER = ", ";
    public static final Path SEARCH_RESPONSE_TEMPLATE =
            Path.of("search_response_template_json.tmpl");
    public static final Path SEARCH_HIT_TEMPLATE = Path.of("publication_hit_template_json.tmpl");
    public static final Path CONTRIBUTOR_TEMPLATE = Path.of("contributor_json.tmpl");

    public static String generateSearchResponseString(List<ExportCsv> csv, String scrollId) {
        var template = IoUtils.stringFromResources(SEARCH_RESPONSE_TEMPLATE);

        var hits =
                csv.stream()
                        .map(FakeSearchResponse::generateHit)
                        .collect(Collectors.joining(COMMA_DELIMITER));

        var scrollData = Objects.isNull(scrollId) ? "" : ",\"_scroll_id\":\"" + scrollId + "\"";
        var templateWithScroll = template.replace("__SCROLL__", scrollData);
        return templateWithScroll.replace("__HITS__", hits);
    }

    private static String generateHit(ExportCsv item) {
        // Not efficient
        var hitTemplate = IoUtils.stringFromResources(SEARCH_HIT_TEMPLATE);
        var date = new ParsedDate(item.getPublicationDate());
        var contributors = createContributorArray(item);
        return hitTemplate
                .replace("__ID__", item.getId())
                .replace("__TYPE__", item.getPublicationInstance())
                .replace("__TITLE__", item.getMainTitle())
                .replace("__YEAR__", "\"" + date.getYear() + "\"")
                .replace("__MONTH__", date.getMonth())
                .replace("__DAY__", date.getDay())
                .replace("__CONTRIBUTORS__", contributors);
    }

    private static String createContributorArray(ExportCsv item) {
        return Arrays.stream(item.getContributors().split(COMMA_DELIMITER))
                .map(FakeSearchResponse::createContributor)
                .collect(Collectors.joining(COMMA_DELIMITER));
    }

    private static String createContributor(String name) {
        var contributorTemplate = IoUtils.stringFromResources(CONTRIBUTOR_TEMPLATE);
        return contributorTemplate.replace("__AUTHOR__", name);
    }

    private static class ParsedDate {
        private final String year;
        private final String month;
        private final String day;

        public ParsedDate(String publicationDate) {
            var date = publicationDate.split("-");
            this.year = date[0];
            this.month = date.length > 1 ? "\"" + date[1] + "\"" : "null";
            this.day = date.length > 2 ? "\"" + date[2] + "\"" : "null";
        }

        public String getYear() {
            return year;
        }

        public String getMonth() {
            return month;
        }

        public String getDay() {
            return day;
        }
    }
}
