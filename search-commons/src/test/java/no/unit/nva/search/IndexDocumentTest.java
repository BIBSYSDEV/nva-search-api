package no.unit.nva.search;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Level;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Range;
import no.unit.nva.publication.PublicationGenerator;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.PublicationGenerator.OPEN_ACCESS;
import static no.unit.nva.publication.PublicationGenerator.PEER_REVIEWED;
import static no.unit.nva.publication.PublicationGenerator.SAMPLE_ISSN;
import static no.unit.nva.publication.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.publication.PublicationGenerator.randomContributor;
import static no.unit.nva.publication.PublicationGenerator.randomDate;
import static no.unit.nva.publication.PublicationGenerator.randomEmail;
import static no.unit.nva.publication.PublicationGenerator.randomInteger;
import static no.unit.nva.publication.PublicationGenerator.randomMonth;
import static no.unit.nva.publication.PublicationGenerator.randomProjects;
import static no.unit.nva.publication.PublicationGenerator.randomPublicationDate;
import static no.unit.nva.publication.PublicationGenerator.randomString;
import static no.unit.nva.publication.PublicationGenerator.randomTitles;
import static no.unit.nva.publication.PublicationGenerator.randomUri;
import static no.unit.nva.publication.PublicationGenerator.sampleFileSet;
import static no.unit.nva.publication.PublicationGenerator.samplePublisher;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexDocumentTest {

    public static final Set<String> IGNORED_PUBLICATION_FIELDS = Set.of("doiRequest",
            "entityDescription.reference.publicationContext.linkedContext",
            "entityDescription.reference.publicationInstance.content",
            ".additionalIdentifiers");
    public static final Set<String> IGNORED_INDEXED_DOCUMENT_FIELDS = Set.of("publisher.name",
            "reference.publicationContext.linkedContext",
            "reference.publicationInstance.content",
            "publisher.labels");

    public static final String LEXVO_ENG = "http://lexvo.org/id/iso639-3/eng";


    @Test
    public void toIndexDocumentCreatesReturnsNewIndexDocumentWithNoMissingFields()
        throws MalformedURLException, InvalidIssnException {
        Publication publication = publicationWithIdentifier();
        assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(IGNORED_PUBLICATION_FIELDS));
        IndexDocument actualDocument = IndexDocument.fromPublication(publication);
        assertThat(actualDocument, doesNotHaveEmptyValuesIgnoringFields(IGNORED_INDEXED_DOCUMENT_FIELDS));
    }

    @Test
    public void toIndexDocumentReturnsIndexDocumentWithAllContributorsWhenPublicationHasManyContributors()
        throws MalformedURLException, InvalidIssnException {
        Publication publication = publicationWithIdentifier();
        List<Contributor> contributors = IntStream.range(0, 100).boxed()
                                             .map(attempt(PublicationGenerator::randomContributor))
                                             .map(Try::orElseThrow)
                                             .collect(Collectors.toList());

        publication.getEntityDescription().setContributors(contributors);
        IndexDocument indexDocument = IndexDocument.fromPublication(publication);

        List<IndexContributor> indexContributors = indexDocument.getContributors();

        for (int sequence = 0; sequence < indexContributors.size(); sequence++) {
            Contributor sourceContributor = contributors.get(sequence);
            IndexContributor indexContributor = indexContributors.get(sequence);
            assertThatIndexContributorHasCorrectData(sourceContributor, indexContributor, sequence);
        }
    }

    @Test
    public void toIndexDocumentReturnsIndexDocumentWitEmptyContributorListWhenPublicationHasNoContributors()
        throws MalformedURLException, InvalidIssnException {
        Publication publication = publicationWithIdentifier();

        publication.getEntityDescription().setContributors(Collections.emptyList());
        IndexDocument indexDocument = IndexDocument.fromPublication(publication);
        List<IndexContributor> indexContributors = indexDocument.getContributors();
        assertThat(indexContributors, is(empty()));
    }

    @Test
    public void toIndexDocumentReturnsIndexDocumentWithNoDateWithPublicationDateIsNull()
        throws MalformedURLException, InvalidIssnException {
        Publication publication = publicationWithIdentifier();
        publication.getEntityDescription().setDate(null);
        IndexDocument indexDocument = IndexDocument.fromPublication(publication);
        assertThat(indexDocument.getPublicationDate(),is(nullValue()));
    }

    @Test
    public void toIndexDocumentDereferencesJournalDataWhenPublicationIsJournalArticleWithPublisher()
            throws MalformedURLException, InvalidIssnException {

        final URI doi = randomUri();
        final SortableIdentifier identifier = SortableIdentifier.next();
        final Contributor contributor = Try.attempt(() -> randomContributor(1)).orElseThrow();
        final List<Contributor> contributors = List.of(contributor);
        final List<IndexContributor> indexContributors = contributors.stream()
                .map(IndexContributor::fromContributor)
                .collect(Collectors.toList());
        final String textAbstract = randomString();
        final String description = randomString();

        final URI journalLinkedContext = randomUri();
        final URI publisherLinkedContext = randomUri();

        final PublicationInstance publicationInstance = new JournalArticle.Builder()
                .withArticleNumber(randomInteger().toString())
                .withIssue(randomMonth())
                .withVolume(randomString())
                .withPages(randomRange())
                .build();

        final Journal publicationContext = new Journal.Builder()
                .withLevel(Level.LEVEL_2)
                .withTitle(randomString())
                .withOnlineIssn(SAMPLE_ISSN)
                .withOpenAccess(OPEN_ACCESS)
                .withPrintIssn(SAMPLE_ISSN)
                .withPeerReviewed(PEER_REVIEWED)
                .withUrl(randomUri().toURL())
                .withLinkedContext(journalLinkedContext)
                .build();


        final Reference reference = new Reference.Builder()
                .withPublicationInstance(publicationInstance)
                .withPublishingContext(publicationContext)
                .withDoi(doi)
                .build();

        final String mainTitle = randomString();
        PublicationDate publicationDate = randomPublicationDate();

        Map<String, String> alternativeTitles = randomTitles();
        List<String> tags = List.of(randomString(), randomString());
        EntityDescription entityDescription =  new EntityDescription.Builder()
                .withMainTitle(mainTitle)
                .withDate(publicationDate)
                .withReference(reference)
                .withContributors(contributors)
                .withAbstract(textAbstract)
                .withAlternativeTitles(alternativeTitles)
                .withDescription(description)
                .withLanguage(URI.create(LEXVO_ENG))
                .withMetadataSource(randomUri())
                .withNpiSubjectHeading(randomString())
                .withTags(tags)
                .build();




        Instant oneMinuteInThePast = Instant.now().minusSeconds(60L);
        List<ResearchProject> projects = randomProjects();

        final String owner = randomEmail();
        final Organization publisher = samplePublisher();
        final Instant indexedDate = randomDate().toInstant();
        final Instant publishedDate = randomDate().toInstant();

        Publication publication = new Publication.Builder()
                .withIdentifier(identifier)
                .withCreatedDate(oneMinuteInThePast)
                .withModifiedDate(oneMinuteInThePast)
                .withOwner(owner)
                .withStatus(PublicationStatus.DRAFT)
                .withPublisher(publisher)
                .withEntityDescription(entityDescription)
                .withFileSet(sampleFileSet())
                .withDoi(doi)
                .withIndexedDate(indexedDate)
                .withLink(randomUri())
                .withProjects(projects)
                .withHandle(randomUri())
                .withPublishedDate(publishedDate)
                .build();



        IndexDocument actualIndexDocument = IndexDocument.fromPublication(publication);


        IndexDocument expected = new IndexDocument.Builder()
                .withType(JournalArticle.class.getSimpleName())
                .withId(identifier)
                .withDoi(doi)
                .withContributors(indexContributors)
                .withTitle(mainTitle)
                .withAbstract(textAbstract)
                .withDescription(description)
                .withOwner(owner)
                .withPublicationDate(IndexDate.fromDate(publicationDate))
                .withPublisher(IndexPublisher.fromPublisher(publisher))
                .withModifiedDate(oneMinuteInThePast)
                .withAlternativeTitles(alternativeTitles)
                .withReference(reference)
                .withPublishedDate(publishedDate)
                .withTags(tags)
                .build();

        assertEquals(expected, actualIndexDocument);

    }

    private void assertThatIndexContributorHasCorrectData(Contributor sourceContributor,
                                                          IndexContributor indexContributor,
                                                          int sequence) {

        assertThat(indexContributor, doesNotHaveEmptyValues());
        assertThat(indexContributor.getName(), is(equalTo(sourceContributor.getIdentity().getName())));
        assertThat(indexContributor.getId(), is(equalTo(sourceContributor.getIdentity().getId())));
        assertThat(sequence, is(equalTo(sourceContributor.getSequence())));
    }


    private Range randomRange() {
        var startRange = randomInteger();
        var endRange = startRange + randomInteger() + 1;
        Range pages = new Range(startRange.toString(), Integer.toString(endRange));
        return pages;
    }
}
