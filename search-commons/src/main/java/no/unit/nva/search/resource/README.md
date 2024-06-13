# /Resource

[back to NVA search api README](/README.md#nva-search-api)

## Data Model
<details>
<summary>JSON</summary>

```json
{
  "type": "Publication",
  "publicationContextUris": [],
  "@context": "https://api.dev.nva.aws.unit.no/publication/context",
  "id": "https://api.dev.nva.aws.unit.no/publication/018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642",
  "scientificIndex": {
    "id": "https://api.sandbox.nva.aws.unit.no/scientific-index/period/2019",
    "type": "ScientificIndex",
    "status": "Reported",
    "year": "2023"
  },
  "filesStatus": "hasPublicFiles",
  "associatedArtifacts": [{
    "type": "PublishedFile",
    "administrativeAgreement": false,
    "identifier": "98893bfd-82d3-44d8-af10-9d1097c9510e",
    "license": "https://rightsstatements.org/page/inc/1.0",
    "mimeType": "application/pdf",
    "name": "20200017-03-TN_Release_area_variability.pdf",
    "publishedDate": "2023-10-31T11:25:43.223366513Z",
    "publisherAuthority": false,
    "size": 699309,
    "visibleForNonOwner": true
  }
  ],
  "createdDate": "2023-09-29T12:14:46Z",
  "contributorOrganizations": ["https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0", "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"],

  "entityDescription": {
    "type": "EntityDescription",
    "abstract": "The hazard mapping tool NAKSIN estimates ....",
    "alternativeAbstracts": {},
    "contributors": [{
      "type": "Contributor",
      "correspondingAuthor": false,
      "identity": {
        "id": "https://api.dev.nva.aws.unit.no/cristin/person/11111",
        "type": "Identity",
        "name": "Dieter Issler"
      },
      "role": {
        "type": "Creator"
      },
      "sequence": 1
    }
    ],
    "language": "http://lexvo.org/id/iso639-3/eng",
    "mainTitle": "A Simple Model for the Variability of Release Area Size",
    "publicationDate": {
      "type": "PublicationDate",
      "day": "24",
      "month": "08",
      "year": "2023"
    },
    "reference": {
      "type": "Reference",
      "doi": "https://doi.org/10.1371/journal.pone.0047855",
      "publicationContext": {
        "type": "Report",
        "publisher": {
          "id": "https://api.dev.nva.aws.unit.no/publication-channels-v2/publisher/944351B3-DC14-4F62-9939-7A2793D6F0B1/2023",
          "type": "Publisher",
          "isbnPrefix": "978-91-27",
          "name": "Natur och kultur",
          "sameAs": "https://kanalregister.hkdir.no/publiseringskanaler/KanalForlagInfo?pid=944351B3-DC14-4F62-9939-7A2793D6F0B1",
          "scientificValue": "LevelOne",
          "valid": true
        },
        "series": {
          "type": "UnconfirmedSeries",
          "title": "NGI-Rapport"
        },
        "seriesNumber": "20200017‐03‐TN"
      },
      "publicationInstance": {
        "type": "ReportResearch",
        "pages": {
          "type": "MonographPages",
          "illustrated": false,
          "pages": "16"
        }
      }
    },
    "tags": [
      "Avalanche-RnD",
      "Slope Stability"
    ]
  },
  "handle": "https://hdl.handle.net/11250/3093139",
  "identifier": "018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642",
  "modelVersion": "0.20.54",
  "nviType": "NonNviCandidate",
  "projects": [{
    "id": "https://api.dev.nva.aws.unit.no/cristin/project/14334631",
    "type": "ResearchProject",
    "name": "Utvikling av eplekaken"
  }
  ],
  "publishedDate": "2023-09-29T12:14:46Z",
  "publisher": {
    "id": "https://api.dev.nva.aws.unit.no/customer/f415cb81-ac56-4244-b31b-25e43dc3027e",
    "type": "Organization"
  },
  "resourceOwner": {
    "owner": "ngi@7452.0.0.0",
    "ownerAffiliation": "https://api.sandbox.nva.aws.unit.no/cristin/organization/7452.0.0.0"
  },
  "status": "PUBLISHED",
  "topLevelOrganizations": [{
    "id": "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0",
    "type": "Organization",
    "countryCode": "NO",
    "hasPart": [{
      "id": "https://api.dev.nva.aws.unit.no/cristin/organization/20754.1.0.0",
      "type": "Organization",
      "acronym": "UA",
      "labels": {
        "en": "The Education and Administration Division"
      },
      "partOf": {
        "id": "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0"
      }
    }
    ],
    "labels": {
      "nb": "Forsvarets høgskole",
      "en": "Norwegian Defence University College"
    }
  }
  ]
}
```
</details>

## Examples

<details>
<summary>Search examples</summary>

### By a specific contributor
```http request
GET https://api.test.nva.aws.unit.no/search/resources?contributor=2F538786
Accept: application/json
```

### By title
```http request
GET https://api.test.nva.aws.unit.no/search/resources?title=My+very+specific+title
Accept: application/json
```

### By category
```http request
GET https://api.test.nva.aws.unit.no/search/resources?category=AcademicArticle&category=AcademicMonograph
Accept: application/json
```

### Free text
```http request
GET https://api.test.nva.aws.unit.no/search/resources?query=Some+specific+phrase
Accept: application/json
```

</details>

## Available Sort Keys

| key_name         | keyName         | keyRegEx              | paths                                                        |
|------------------|-----------------|-----------------------|--------------------------------------------------------------|
| relevance        | relevance       | (?i)relevance         | _score                                                       |
| category         | category        | (?i)category          | entityDescription.reference.publicationInstance.type.keyword |
| instance_type    | instanceType    | (?i)instance.?type    | entityDescription.reference.publicationInstance.type.keyword |
| created_date     | createdDate     | (?i)created.?date     | createdDate                                                  |
| modified_date    | modifiedDate    | (?i)modified.?date    | modifiedDate                                                 |
| published_date   | publishedDate   | (?i)published.?date   | publishedDate                                                |
| publication_date | publicationDate | (?i)publication.?date | entityDescription.publicationDate.year.keyword               |
| title            | title           | (?i)title             | entityDescription.mainTitle.keyword                          |
| unit_id          | unitId          | (?i)unit.?id          | entityDescription.contributors.affiliations.id.keyword       |
| user             | user            | (?i)(user)(owner)     | resourceOwner.owner.keyword                                  |

## Available Keys (filters)

### QueryKind descriptions
* number
  * Integer
* date
  * DateTimeFormat -> <code> yyyy | yyyy-MM-dd | yyyy-MM-ddTHH:mm:ssZ | yyyy-MM-ddTHH:mm:ss.SSSZ</code>
* keyword
  * Only hit on complete field
* fuzzy_keyword
  * will hit on partial field, boost hits on complete field
* text
  * hits on any partial match in field(s), boosts on exact match and phrases
* free_text
  * Search through whole document
* custom

### Key details

| key_name                        | keyName                      | queryKind    | scope                    | paths                                                                                                                                                                                                                                                                                  |
|---------------------------------|------------------------------|--------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| abstract                        | abstract                     | text         | all_items                | entityDescription.abstract                                                                                                                                                                                                                                                             |
| abstract_not                    | abstractNot                  | text         | no_items                 | entityDescription.abstract                                                                                                                                                                                                                                                             |
| abstract_should                 | abstractShould               | text         | one_or_more_item         | entityDescription.abstract                                                                                                                                                                                                                                                             |
| context_type                    | contextType                  | keyword      | all_items                | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| context_type_not                | contextTypeNot               | keyword      | no_items                 | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| context_type_should             | contextTypeShould            | keyword      | one_or_more_item         | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| contributor                     | contributor                  | keyword      | all_items                | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_not                 | contributorNot               | keyword      | no_items                 | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_should              | contributorShould            | keyword      | one_or_more_item         | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_name                | contributorName              | text         | all_items                | entityDescription.contributors.identity.name, entityDescription.contributors.identity.id                                                                                                                                                                                               |
| contributor_name_not            | contributorNameNot           | text         | no_items                 | entityDescription.contributors.identity.name, entityDescription.contributors.identity.id                                                                                                                                                                                               |
| contributor_name_should         | contributorNameShould        | text         | one_or_more_item         | entityDescription.contributors.identity.name, entityDescription.contributors.identity.id                                                                                                                                                                                               |
| course                          | course                       | keyword      | all_items                | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| course_not                      | courseNot                    | keyword      | no_items                 | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| course_should                   | courseShould                 | keyword      | one_or_more_item         | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| created_before                  | createdBefore                | date         | less_than                | createdDate                                                                                                                                                                                                                                                                            |
| created_since                   | createdSince                 | date         | greater_than_or_equal_to | createdDate                                                                                                                                                                                                                                                                            |
| cristin_identifier              | cristinIdentifier            | custom       | all_items                | CRISTIN_IDENTIFIER                                                                                                                                                                                                                                                                     |
| doi                             | doi                          | fuzzyKeyword | all_items                | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| doi_not                         | doiNot                       | fuzzyKeyword | no_items                 | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| doi_should                      | doiShould                    | text         | one_or_more_item         | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| exclude_subunits                | excludeSubunits              | ignored      | all_items                | EXCLUDE_SUBUNITS                                                                                                                                                                                                                                                                       |
| funding                         | funding                      | custom       | all_items                | fundings.identifier.keyword, fundings.source.identifier.keyword                                                                                                                                                                                                                        |
| funding_identifier              | fundingIdentifier            | keyword      | all_items                | fundings.identifier.keyword                                                                                                                                                                                                                                                            |
| funding_identifier_not          | fundingIdentifierNot         | keyword      | no_items                 | fundings.identifier.keyword                                                                                                                                                                                                                                                            |
| funding_identifier_should       | fundingIdentifierShould      | fuzzyKeyword | one_or_more_item         | fundings.identifier                                                                                                                                                                                                                                                                    |
| funding_source                  | fundingSource                | text         | all_items                | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| funding_source_not              | fundingSourceNot             | text         | no_items                 | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| funding_source_should           | fundingSourceShould          | text         | one_or_more_item         | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| handle                          | handle                       | fuzzyKeyword | one_or_more_item         | handle, additionalIdentifiers.value                                                                                                                                                                                                                                                    |
| handle_not                      | handleNot                    | fuzzyKeyword | not_one_item             | handle, additionalIdentifiers.value                                                                                                                                                                                                                                                    |
| files                           | files                        | keyword      | all_items                | filesStatus.keyword                                                                                                                                                                                                                                                                    |
| id                              | id                           | keyword      | all_items                | identifier.keyword                                                                                                                                                                                                                                                                     |
| id_not                          | idNot                        | keyword      | no_items                 | identifier.keyword                                                                                                                                                                                                                                                                     |
| id_should                       | idShould                     | text         | one_or_more_item         | identifier                                                                                                                                                                                                                                                                             |
| instance_type                   | instanceType                 | keyword      | all_items                | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| instance_type_not               | instanceTypeNot              | keyword      | no_items                 | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| instance_type_should            | instanceTypeShould           | keyword      | one_or_more_item         | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| institution                     | institution                  | text         | all_items                | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| institution_not                 | institutionNot               | text         | no_items                 | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| institution_should              | institutionShould            | text         | one_or_more_item         | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| isbn                            | isbn                         | keyword      | one_or_more_item         | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| isbn_not                        | isbnNot                      | keyword      | not_one_item             | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| isbn_should                     | isbnShould                   | fuzzyKeyword | one_or_more_item         | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| issn                            | issn                         | keyword      | one_or_more_item         | entityDescription.reference.publicationContext.onlineIssn.keyword, entityDescription.reference.publicationContext.printIssn.keyword, entityDescription.reference.publicationContext.series.onlineIssn.keyword, entityDescription.reference.publicationContext.series.printIssn.keyword |
| issn_not                        | issnNot                      | keyword      | not_one_item             | entityDescription.reference.publicationContext.onlineIssn.keyword, entityDescription.reference.publicationContext.printIssn.keyword, entityDescription.reference.publicationContext.series.onlineIssn.keyword, entityDescription.reference.publicationContext.series.printIssn.keyword |
| issn_should                     | issnShould                   | fuzzyKeyword | one_or_more_item         | entityDescription.reference.publicationContext.onlineIssn, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.series.onlineIssn, entityDescription.reference.publicationContext.series.printIssn                                 |
| journal                         | journal                      | fuzzyKeyword | all_items                | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| journal_not                     | journalNot                   | fuzzyKeyword | no_items                 | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| journal_should                  | journalShould                | fuzzyKeyword | one_or_more_item         | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| license                         | license                      | fuzzyKeyword | all_items                | associatedArtifacts.license.name, associatedArtifacts.license.value, associatedArtifacts.license.labels.en, associatedArtifacts.license.labels.nn, associatedArtifacts.license.labels.nb, associatedArtifacts.license.labels.sme                                                       |
| license_not                     | licenseNot                   | fuzzyKeyword | no_items                 | associatedArtifacts.license.name, associatedArtifacts.license.value, associatedArtifacts.license.labels.en, associatedArtifacts.license.labels.nn, associatedArtifacts.license.labels.nb, associatedArtifacts.license.labels.sme                                                       |
| modified_before                 | modifiedBefore               | date         | less_than                | modifiedDate                                                                                                                                                                                                                                                                           |
| modified_since                  | modifiedSince                | date         | greater_than_or_equal_to | modifiedDate                                                                                                                                                                                                                                                                           |
| orcid                           | orcid                        | keyword      | all_items                | entityDescription.contributors.identity.orcId.keyword                                                                                                                                                                                                                                  |
| orcid_not                       | orcidNot                     | keyword      | no_items                 | entityDescription.contributors.identity.orcId.keyword                                                                                                                                                                                                                                  |
| orcid_should                    | orcidShould                  | text         | one_or_more_item         | entityDescription.contributors.identity.orcId                                                                                                                                                                                                                                          |
| parent_publication              | parentPublication            | keyword      | all_items                | entityDescription.reference.publicationInstance.corrigendumFor.keyword, entityDescription.reference.publicationInstance.manifestations.id.keyword, entityDescription.reference.publicationInstance.id.keyword                                                                          |
| parent_publication_should       | parentPublicationShould      | text         | one_or_more_item         | entityDescription.reference.publicationInstance.corrigendumFor, entityDescription.reference.publicationInstance.manifestations.id, entityDescription.reference.publicationInstance.id                                                                                                  |
| project                         | project                      | keyword      | one_or_more_item         | projects.id.keyword                                                                                                                                                                                                                                                                    |
| project_not                     | projectNot                   | keyword      | not_one_item             | projects.id.keyword                                                                                                                                                                                                                                                                    |
| project_should                  | projectShould                | fuzzyKeyword | one_or_more_item         | projects.id                                                                                                                                                                                                                                                                            |
| publication_language            | publicationLanguage          | fuzzyKeyword | one_or_more_item         | entityDescription.language                                                                                                                                                                                                                                                             |
| publication_language_not        | publicationLanguageNot       | fuzzyKeyword | not_one_item             | entityDescription.language                                                                                                                                                                                                                                                             |
| publication_language_should     | publicationLanguageShould    | fuzzyKeyword | one_or_more_item         | entityDescription.language                                                                                                                                                                                                                                                             |
| publication_year                | publicationYear              | number       | between                  | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_before         | publicationYearBefore        | number       | less_than                | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_should         | publicationYearShould        | keyword      | one_or_more_item         | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_since          | publicationYearSince         | number       | greater_than_or_equal_to | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| published_before                | publishedBefore              | date         | less_than                | publishedDate                                                                                                                                                                                                                                                                          |
| published_between               | publishedBetween             | date         | between                  | publishedDate                                                                                                                                                                                                                                                                          |
| published_since                 | publishedSince               | date         | greater_than_or_equal_to | publishedDate                                                                                                                                                                                                                                                                          |
| publisher                       | publisher                    | fuzzyKeyword | all_items                | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_not                   | publisherNot                 | fuzzyKeyword | no_items                 | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_should                | publisherShould              | fuzzyKeyword | one_or_more_item         | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_id                    | publisherId                  | text         | all_items                | publisher.id                                                                                                                                                                                                                                                                           |
| publisher_id_not                | publisherIdNot               | text         | no_items                 | publisher.id                                                                                                                                                                                                                                                                           |
| publisher_id_should             | publisherIdShould            | text         | one_or_more_item         | publisher.id                                                                                                                                                                                                                                                                           |
| referenced_id                   | referencedId                 | fuzzyKeyword | one_or_more_item         | entityDescription.reference.publicationContext.id                                                                                                                                                                                                                                      |
| scientific_value                | scientificValue              | keyword      | one_or_more_item         | entityDescription.reference.publicationContext.publisher.scientificValue.keyword, entityDescription.reference.publicationContext.scientificValue.keyword                                                                                                                               |
| scientific_index_status         | scientificIndexStatus        | keyword      | one_or_more_item         | scientificIndex.status.keyword                                                                                                                                                                                                                                                         |
| scientific_index_status_not     | scientificIndexStatusNot     | keyword      | not_one_item             | scientificIndex.status.keyword                                                                                                                                                                                                                                                         |
| scientific_report_period_since  | scientificReportPeriodSince  | number       | greater_than_or_equal_to | scientificIndex.year                                                                                                                                                                                                                                                                   |
| scientific_report_period_before | scientificReportPeriodBefore | number       | less_than                | scientificIndex.year                                                                                                                                                                                                                                                                   |
| scopus_identifier               | scopusIdentifier             | custom       | all_items                | SCOPUS_IDENTIFIER                                                                                                                                                                                                                                                                      |
| series                          | series                       | fuzzyKeyword | all_items                | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| series_not                      | seriesNot                    | fuzzyKeyword | no_items                 | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| series_should                   | seriesShould                 | fuzzyKeyword | one_or_more_item         | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| status                          | status                       | keyword      | all_items                | status.keyword                                                                                                                                                                                                                                                                         |
| status_not                      | statusNot                    | keyword      | no_items                 | status.keyword                                                                                                                                                                                                                                                                         |
| status_should                   | statusShould                 | keyword      | one_or_more_item         | status.keyword                                                                                                                                                                                                                                                                         |
| tags                            | tags                         | text         | all_items                | entityDescription.tags                                                                                                                                                                                                                                                                 |
| tags_not                        | tagsNot                      | text         | no_items                 | entityDescription.tags                                                                                                                                                                                                                                                                 |
| tags_should                     | tagsShould                   | text         | one_or_more_item         | entityDescription.tags                                                                                                                                                                                                                                                                 |
| title                           | title                        | text         | all_items                | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| title_not                       | titleNot                     | text         | no_items                 | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| title_should                    | titleShould                  | text         | one_or_more_item         | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| top_level_organization          | topLevelOrganization         | custom       | one_or_more_item         | topLevelOrganizations.id.keyword, contributorOrganizations.keyword                                                                                                                                                                                                                     |
| unit                            | unit                         | custom       | all_items                | entityDescription.contributors.affiliations.id.keyword, contributorOrganizations.keyword                                                                                                                                                                                               |
| unit_not                        | unitNot                      | keyword      | no_items                 | entityDescription.contributors.affiliations.id.keyword                                                                                                                                                                                                                                 |
| unit_should                     | unitShould                   | text         | one_or_more_item         | entityDescription.contributors.affiliations.id                                                                                                                                                                                                                                         |
| user                            | user                         | keyword      | all_items                | resourceOwner.owner.keyword                                                                                                                                                                                                                                                            |
| user_not                        | userNot                      | keyword      | no_items                 | resourceOwner.owner.keyword                                                                                                                                                                                                                                                            |
| user_should                     | userShould                   | text         | one_or_more_item         | resourceOwner.owner                                                                                                                                                                                                                                                                    |
| user_affiliation                | userAffiliation              | keyword      | all_items                | resourceOwner.ownerAffiliation.keyword                                                                                                                                                                                                                                                 |
| user_affiliation_not            | userAffiliationNot           | keyword      | all_items                | resourceOwner.ownerAffiliation.keyword                                                                                                                                                                                                                                                 |
| user_affiliation_should         | userAffiliationShould        | text         | all_items                | resourceOwner.ownerAffiliation                                                                                                                                                                                                                                                         |
| search_all                      | searchAll                    | custom       | all_items                | q                                                                                                                                                                                                                                                                                      |

## Notes

> [!NOTE]
> <p>Valid contextType (aliases; type/contextType/category)</p>
>
> ```
> AcademicArticle, AcademicChapter, AcademicLiteratureReview, AcademicMonograph, Architecture, ArtisticDesign,
> BookAbstracts, BookAnthology, BookMonograph, CaseReport, ChapterArticle, ChapterConferenceAbstract, ChapterInReport,
> ConferenceAbstract, ConferenceLecture, ConferencePoster, ConferenceReport, DataManagementPlan, DataSet,
> DegreeBachelor, DegreeLicentiate, DegreeMaster, DegreePhd, Encyclopedia, EncyclopediaChapter, ExhibitionCatalog,
> ExhibitionCatalogChapter, ExhibitionProduction, FeatureArticle, Introduction, JournalArticle, JournalCorrigendum,
> JournalInterview, JournalIssue, JournalLeader, JournalLetter, JournalReview, Lecture, LiteraryArts, MediaBlogPost,
> MediaFeatureArticle, MediaInterview, MediaParticipationInRadioOrTv, MediaReaderOpinion, MovingPicture,
> MusicPerformance, NonFictionChapter, NonFictionMonograph, OtherPresentation, OtherStudentWork, PerformingArts, PopularScienceArticle,
> PopularScienceChapter, PopularScienceMonograph, ProfessionalArticle, ReportBasic, ReportBookOfAbstract, ReportPolicy,
> ReportResearch, ReportWorkingPaper, StudyProtocol, Textbook, TextbookChapter, VisualArts
> ```

> [!NOTE]
> <p>Valid SortKeys </p>
>
> ```
> category, instanceType, createdDate, modifiedDate, publishedDate, publicationDate, title, unitId, user
> ```
