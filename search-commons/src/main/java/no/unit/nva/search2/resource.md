# Resource

## Data model

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
## Search

### By a specific contributor
```http request
GET /search/resources?contributor=https%3A%2F%2Fapi.test.nva.aws.unit.no%2Fcristin%2Fperson%2F538786 HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json

```

### By title
```http request
GET /search/resources?title=My+very+specific+title HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json

```

### By category
```http request
GET /search/resources?category=AcademicArticle&category=AcademicMonograph HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json

```

### Free text
```http request
GET /search/resources?query=Some+specific+phrase HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json

```
## Filters
* Filters are case-insensitive and can be spelled with camelCase as well
  * <code>context_type_should</code> could also be spelled <code>contextTypeShould</code>
* Every filter applied are joined with <code>AND</code> between them, making the query more restricted
### QueryKind
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
  * 

### All available filters

| key_name                        | keyName                      | queryKind    | scope                | paths                                                                                                                                                                                                                                                                                  |
|---------------------------------|------------------------------|--------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| abstract                        | abstract                     | text         | allItems             | entityDescription.abstract                                                                                                                                                                                                                                                             |
| abstract_not                    | abstractNot                  | text         | noItems              | entityDescription.abstract                                                                                                                                                                                                                                                             |
| abstract_should                 | abstractShould               | text         | oneOrMoreItem        | entityDescription.abstract                                                                                                                                                                                                                                                             |
| context_type                    | contextType                  | keyword      | allItems             | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| context_type_not                | contextTypeNot               | keyword      | noItems              | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| context_type_should             | contextTypeShould            | keyword      | oneOrMoreItem        | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| contributor                     | contributor                  | keyword      | allItems             | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_not                 | contributorNot               | keyword      | noItems              | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_should              | contributorShould            | keyword      | oneOrMoreItem        | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_name                | contributorName              | text         | allItems             | entityDescription.contributors.identity.name                                                                                                                                                                                                                                           |
| contributor_name_not            | contributorNameNot           | text         | noItems              | entityDescription.contributors.identity.name                                                                                                                                                                                                                                           |
| contributor_name_should         | contributorNameShould        | text         | oneOrMoreItem        | entityDescription.contributors.identity.name                                                                                                                                                                                                                                           |
| course                          | course                       | keyword      | allItems             | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| course_not                      | courseNot                    | keyword      | noItems              | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| course_should                   | courseShould                 | keyword      | oneOrMoreItem        | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| created_before                  | createdBefore                | date         | lessThan             | createdDate                                                                                                                                                                                                                                                                            |
| created_since                   | createdSince                 | date         | greaterThanOrEqualTo | createdDate                                                                                                                                                                                                                                                                            |
| cristin_identifier              | cristinIdentifier            | custom       | allItems             | CRISTIN_IDENTIFIER                                                                                                                                                                                                                                                                     |
| doi                             | doi                          | fuzzyKeyword | allItems             | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| doi_not                         | doiNot                       | fuzzyKeyword | noItems              | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| doi_should                      | doiShould                    | text         | oneOrMoreItem        | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| exclude_subunits                | excludeSubunits              | ignored      | allItems             | EXCLUDE_SUBUNITS                                                                                                                                                                                                                                                                       |
| funding                         | funding                      | custom       | allItems             | fundings.identifier.keyword, fundings.source.identifier.keyword                                                                                                                                                                                                                        |
| funding_identifier              | fundingIdentifier            | keyword      | allItems             | fundings.identifier.keyword                                                                                                                                                                                                                                                            |
| funding_identifier_not          | fundingIdentifierNot         | keyword      | noItems              | fundings.identifier.keyword                                                                                                                                                                                                                                                            |
| funding_identifier_should       | fundingIdentifierShould      | fuzzyKeyword | oneOrMoreItem        | fundings.identifier                                                                                                                                                                                                                                                                    |
| funding_source                  | fundingSource                | text         | allItems             | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| funding_source_not              | fundingSourceNot             | text         | noItems              | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| funding_source_should           | fundingSourceShould          | text         | oneOrMoreItem        | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| handle                          | handle                       | fuzzyKeyword | allItems             | handle                                                                                                                                                                                                                                                                                 |
| handle_not                      | handleNot                    | fuzzyKeyword | noItems              | handle                                                                                                                                                                                                                                                                                 |
| handle_should                   | handleShould                 | text         | oneOrMoreItem        | handle                                                                                                                                                                                                                                                                                 |
| files                           | files                        | keyword      | allItems             | filesStatus.keyword                                                                                                                                                                                                                                                                    |
| id                              | id                           | keyword      | allItems             | identifier.keyword                                                                                                                                                                                                                                                                     |
| id_not                          | idNot                        | keyword      | noItems              | identifier.keyword                                                                                                                                                                                                                                                                     |
| id_should                       | idShould                     | text         | oneOrMoreItem        | identifier                                                                                                                                                                                                                                                                             |
| instance_type                   | instanceType                 | keyword      | allItems             | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| instance_type_not               | instanceTypeNot              | keyword      | noItems              | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| instance_type_should            | instanceTypeShould           | keyword      | oneOrMoreItem        | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| institution                     | institution                  | text         | allItems             | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| institution_not                 | institutionNot               | text         | noItems              | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| institution_should              | institutionShould            | text         | oneOrMoreItem        | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| isbn                            | isbn                         | keyword      | oneOrMoreItem        | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| isbn_not                        | isbnNot                      | keyword      | notOneItem           | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| isbn_should                     | isbnShould                   | fuzzyKeyword | oneOrMoreItem        | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| issn                            | issn                         | keyword      | oneOrMoreItem        | entityDescription.reference.publicationContext.onlineIssn.keyword, entityDescription.reference.publicationContext.printIssn.keyword, entityDescription.reference.publicationContext.series.onlineIssn.keyword, entityDescription.reference.publicationContext.series.printIssn.keyword |
| issn_not                        | issnNot                      | keyword      | notOneItem           | entityDescription.reference.publicationContext.onlineIssn.keyword, entityDescription.reference.publicationContext.printIssn.keyword, entityDescription.reference.publicationContext.series.onlineIssn.keyword, entityDescription.reference.publicationContext.series.printIssn.keyword |
| issn_should                     | issnShould                   | fuzzyKeyword | oneOrMoreItem        | entityDescription.reference.publicationContext.onlineIssn, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.series.onlineIssn, entityDescription.reference.publicationContext.series.printIssn                                 |
| journal                         | journal                      | fuzzyKeyword | allItems             | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| journal_not                     | journalNot                   | fuzzyKeyword | noItems              | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| journal_should                  | journalShould                | fuzzyKeyword | oneOrMoreItem        | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| license                         | license                      | custom       | allItems             | associatedArtifacts.license.keyword                                                                                                                                                                                                                                                    |
| license_not                     | licenseNot                   | custom       | noItems              | associatedArtifacts.license.keyword                                                                                                                                                                                                                                                    |
| license_should                  | licenseShould                | keyword      | oneOrMoreItem        | associatedArtifacts.license.keyword                                                                                                                                                                                                                                                    |
| modified_before                 | modifiedBefore               | date         | lessThan             | modifiedDate                                                                                                                                                                                                                                                                           |
| modified_since                  | modifiedSince                | date         | greaterThanOrEqualTo | modifiedDate                                                                                                                                                                                                                                                                           |
| orcid                           | orcid                        | keyword      | allItems             | entityDescription.contributors.identity.orcId.keyword                                                                                                                                                                                                                                  |
| orcid_not                       | orcidNot                     | keyword      | noItems              | entityDescription.contributors.identity.orcId.keyword                                                                                                                                                                                                                                  |
| orcid_should                    | orcidShould                  | text         | oneOrMoreItem        | entityDescription.contributors.identity.orcId                                                                                                                                                                                                                                          |
| parent_publication              | parentPublication            | keyword      | allItems             | entityDescription.reference.publicationInstance.corrigendumFor.keyword, entityDescription.reference.publicationInstance.manifestations.id.keyword, entityDescription.reference.publicationInstance.id.keyword                                                                          |
| parent_publication_should       | parentPublicationShould      | text         | oneOrMoreItem        | entityDescription.reference.publicationInstance.corrigendumFor, entityDescription.reference.publicationInstance.manifestations.id, entityDescription.reference.publicationInstance.id                                                                                                  |
| project                         | project                      | keyword      | oneOrMoreItem        | projects.id.keyword                                                                                                                                                                                                                                                                    |
| project_not                     | projectNot                   | keyword      | notOneItem           | projects.id.keyword                                                                                                                                                                                                                                                                    |
| project_should                  | projectShould                | fuzzyKeyword | oneOrMoreItem        | projects.id                                                                                                                                                                                                                                                                            |
| publication_language            | publicationLanguage          | keyword      | allItems             | entityDescription.language.keyword                                                                                                                                                                                                                                                     |
| publication_language_not        | publicationLanguageNot       | keyword      | noItems              | entityDescription.language.keyword                                                                                                                                                                                                                                                     |
| publication_language_should     | publicationLanguageShould    | keyword      | oneOrMoreItem        | entityDescription.language.keyword                                                                                                                                                                                                                                                     |
| publication_year_before         | publicationYearBefore        | number       | lessThan             | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_should         | publicationYearShould        | keyword      | oneOrMoreItem        | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_since          | publicationYearSince         | number       | greaterThanOrEqualTo | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| published_before                | publishedBefore              | date         | lessThan             | publishedDate                                                                                                                                                                                                                                                                          |
| published_between               | publishedBetween             | date         | between              | publishedDate                                                                                                                                                                                                                                                                          |
| published_since                 | publishedSince               | date         | greaterThanOrEqualTo | publishedDate                                                                                                                                                                                                                                                                          |
| publisher                       | publisher                    | fuzzyKeyword | allItems             | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_not                   | publisherNot                 | fuzzyKeyword | noItems              | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_should                | publisherShould              | fuzzyKeyword | oneOrMoreItem        | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_id                    | publisherId                  | text         | allItems             | publisher.id                                                                                                                                                                                                                                                                           |
| publisher_id_not                | publisherIdNot               | text         | noItems              | publisher.id                                                                                                                                                                                                                                                                           |
| publisher_id_should             | publisherIdShould            | text         | oneOrMoreItem        | publisher.id                                                                                                                                                                                                                                                                           |
| scientific_value                | scientificValue              | keyword      | oneOrMoreItem        | entityDescription.reference.publicationContext.publisher.scientificValue.keyword, entityDescription.reference.publicationContext.scientificValue.keyword                                                                                                                               |
| scientific_index_status         | scientificIndexStatus        | keyword      | oneOrMoreItem        | scientificIndex.status.keyword                                                                                                                                                                                                                                                         |
| scientific_index_status_not     | scientificIndexStatusNot     | keyword      | notOneItem           | scientificIndex.status.keyword                                                                                                                                                                                                                                                         |
| scientific_report_period_since  | scientificReportPeriodSince  | number       | greaterThanOrEqualTo | scientificIndex.year                                                                                                                                                                                                                                                                   |
| scientific_report_period_before | scientificReportPeriodBefore | number       | lessThan             | scientificIndex.year                                                                                                                                                                                                                                                                   |
| scopus_identifier               | scopusIdentifier             | custom       | allItems             | SCOPUS_IDENTIFIER                                                                                                                                                                                                                                                                      |
| series                          | series                       | fuzzyKeyword | allItems             | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| series_not                      | seriesNot                    | fuzzyKeyword | noItems              | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| series_should                   | seriesShould                 | fuzzyKeyword | oneOrMoreItem        | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| status                          | status                       | keyword      | allItems             | status.keyword                                                                                                                                                                                                                                                                         |
| status_not                      | statusNot                    | keyword      | noItems              | status.keyword                                                                                                                                                                                                                                                                         |
| status_should                   | statusShould                 | keyword      | oneOrMoreItem        | status.keyword                                                                                                                                                                                                                                                                         |
| tags                            | tags                         | text         | allItems             | entityDescription.tags                                                                                                                                                                                                                                                                 |
| tags_not                        | tagsNot                      | text         | noItems              | entityDescription.tags                                                                                                                                                                                                                                                                 |
| tags_should                     | tagsShould                   | text         | oneOrMoreItem        | entityDescription.tags                                                                                                                                                                                                                                                                 |
| title                           | title                        | text         | allItems             | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| title_not                       | titleNot                     | text         | noItems              | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| title_should                    | titleShould                  | text         | oneOrMoreItem        | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| top_level_organization          | topLevelOrganization         | custom       | oneOrMoreItem        | topLevelOrganizations.id.keyword, contributorOrganizations.keyword                                                                                                                                                                                                                     |
| unit                            | unit                         | custom       | allItems             | entityDescription.contributors.affiliations.id.keyword, contributorOrganizations.keyword                                                                                                                                                                                               |
| unit_not                        | unitNot                      | keyword      | noItems              | entityDescription.contributors.affiliations.id.keyword                                                                                                                                                                                                                                 |
| unit_should                     | unitShould                   | text         | oneOrMoreItem        | entityDescription.contributors.affiliations.id                                                                                                                                                                                                                                         |
| user                            | user                         | keyword      | allItems             | resourceOwner.owner.keyword                                                                                                                                                                                                                                                            |
| user_not                        | userNot                      | keyword      | noItems              | resourceOwner.owner.keyword                                                                                                                                                                                                                                                            |
| user_should                     | userShould                   | text         | oneOrMoreItem        | resourceOwner.owner                                                                                                                                                                                                                                                                    |
| user_affiliation                | userAffiliation              | keyword      | allItems             | resourceOwner.ownerAffiliation.keyword                                                                                                                                                                                                                                                 |
| user_affiliation_not            | userAffiliationNot           | keyword      | allItems             | resourceOwner.ownerAffiliation.keyword                                                                                                                                                                                                                                                 |
| user_affiliation_should         | userAffiliationShould        | text         | allItems             | resourceOwner.ownerAffiliation                                                                                                                                                                                                                                                         |

####  Query parameters passed to sws/opensearch

| key        | queryKind         | scope                      |
|------------|-------------------|----------------------------|
| search_all | text with ranking | all_items accross document |
| fields     | list of keys      | user, tags, title          |

####  Pagination parameters

| key          | queryKind                   | example         |
|--------------|-----------------------------|-----------------|
| aggregation  | Enum                        | all, none       |
| page         | number                      | 0 to 10000/size |
| from         | number                      | 0 to 10000-size |
| size         | number                      | 0 to 1000       |
| sort         | key1:asc/desc,key2:desc/asc |
| sort_order   | asc/desc                    |
| search_after | sortindex                   | api only        |


