package no.unit.nva.search.common.csv;

import com.opencsv.bean.CsvBindByName;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author Rurik Greenall
 * @author Aasarød
 * @author Stig Norland
 */
@CsvBindByNameOrder({"url", "title", "publicationDate", "type", "contributors"})
public class ExportCsv {
    public static final String DATE_SEPARATOR = "-";
    public static final String DELIMITER = ",";
    @CsvBindByName(column = "url")
    private String id;
    @CsvBindByName(column = "title")
    private String mainTitle;
    @CsvBindByName(column = "publicationDate")
    private String publicationDate;
    @CsvBindByName(column = "type")
    private String publicationInstance;
    @CsvBindByName(column = "contributors")
    private String contributors;

    @JacocoGenerated
    public ExportCsv() {
        // Bean constructor.
    }

    public ExportCsv(String id,
                     String mainTitle,
                     String year,
                     String month,
                     String day,
                     String publicationInstance,
                     List<String> contributors) {
        this.id = id;
        this.mainTitle = mainTitle;
        this.publicationDate = createPublicationDate(year, month, day);
        this.publicationInstance = publicationInstance;
        this.contributors = createContributorsString(contributors);
    }

    public String getId() {
        return id;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public String getPublicationInstance() {
        return publicationInstance;
    }

    public String getContributors() {
        return contributors;
    }

    public ExportCsv withId(String id) {
        this.id = id;
        return this;
    }

    public ExportCsv withMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public ExportCsv withPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
        return this;
    }

    public ExportCsv withPublicationInstance(String publicationInstance) {
        this.publicationInstance = publicationInstance;
        return this;
    }

    public ExportCsv withContributors(String contributors) {
        this.contributors = contributors;
        return this;
    }

    public final String createPublicationDate(String year, String month, String day) {
        var stringBuilder = new StringBuilder();

        if (StringUtils.isNotBlank(year)) {
            stringBuilder.append(year);
        }

        if (StringUtils.isNotBlank(month) && StringUtils.isNotBlank(year)) {
            stringBuilder.append(DATE_SEPARATOR).append(month);
        }

        if (StringUtils.isNotBlank(day) && StringUtils.isNotBlank(month) && StringUtils.isNotBlank(year)) {
            stringBuilder.append(DATE_SEPARATOR).append(day);
        }
        return stringBuilder.toString();
    }

    public final String createContributorsString(List<String> contributors) {
        return String.join(DELIMITER, contributors);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExportCsv exportCsv)) {
            return false;
        }
        return Objects.equals(getId(), exportCsv.getId())
                && Objects.equals(getMainTitle(), exportCsv.getMainTitle())
                && Objects.equals(getPublicationDate(), exportCsv.getPublicationDate())
                && Objects.equals(getPublicationInstance(), exportCsv.getPublicationInstance())
                && Objects.equals(getContributors(), exportCsv.getContributors());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getMainTitle(), getPublicationDate(), getPublicationInstance(), getContributors());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return "ExportCsv{id='%s', mainTitle='%s', publicationDate='%s', publicationInstance='%s', contributors='%s'}"
                   .formatted(id, mainTitle, publicationDate, publicationInstance, contributors);
    }
}
