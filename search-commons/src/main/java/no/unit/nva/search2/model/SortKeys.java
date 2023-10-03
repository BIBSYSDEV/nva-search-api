package no.unit.nva.search2.model;

import java.util.function.Predicate;

public enum SortKeys {
    CATEGORY("(?i)category", "entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR_ID( "(?i)contributor.?id", "entityDescription.contributors.identity.id"),
    CONTRIBUTOR_NAME( "(?i)contributor.?name", "entityDescription.contributors.identity.name"),
    CREATED("(?i)created.?date", "created"),
    FUNDING( "(?i)funding", "fundings.identifier|source.identifier"),
    ID( "(?i)id", "identifier"),
    INSTITUTION_ID("(?i)institution.?id", "entityDescription.contributors.affiliation.id"),
    INSTITUTION_NAME("(?i)institution.?name","entityDescription.contributors.affiliation.name"),
    MODIFIED("(?i)modified.?date", "modified"),
    PUBLISHED_YEAR("(?i)published.?year", "entityDescription.publicationDate.year"),
    TITLE("(?i)title", "entityDescription.mainTitle"),
    UNIT_ID("(?i)unit", "entityDescription.contributors.affiliation.id"),
    USER( "(?i)(user)|(owner)", "resourceOwner.owner");

    private final String pattern;
    private final String luceneField;

    SortKeys(String pattern, String luceneField) {
        this.pattern = pattern;
        this.luceneField = luceneField;
    }
    public String getPattern() {
        return pattern;
    }

    public String getLuceneField() {
        return luceneField;
    }

    public static Predicate<SortKeys> equalTo(String name) {
        return key -> name.matches(key.getPattern());
    }
}
