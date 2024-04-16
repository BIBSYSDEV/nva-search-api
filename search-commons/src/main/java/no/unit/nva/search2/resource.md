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
  * hits on any partial match in field(s)
* free_text
  * Search through whole document
* custom
  * 

### All available filters

| key                                                            | queryKind     | scope                    | paths                                                                                                                                                                                                                                                                                  |
|----------------------------------------------------------------|---------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| abstract / abstract                                            | TEXT          | ALL_ITEMS                | entityDescription.abstract                                                                                                                                                                                                                                                             |
| abstract_not / abstractNot                                     | TEXT          | NO_ITEMS                 | entityDescription.abstract                                                                                                                                                                                                                                                             |
| abstract_should / abstractShould                               | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.abstract                                                                                                                                                                                                                                                             |
| context_type / contextType                                     | KEYWORD       | ALL_ITEMS                | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| context_type_not / contextTypeNot                              | KEYWORD       | NO_ITEMS                 | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| context_type_should / contextTypeShould                        | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.type.keyword                                                                                                                                                                                                                            |
| contributor / contributor                                      | KEYWORD       | ALL_ITEMS                | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_not / contributorNot                               | KEYWORD       | NO_ITEMS                 | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_should / contributorShould                         | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.contributors.identity.id.keyword                                                                                                                                                                                                                                     |
| contributor_name / contributorName                             | TEXT          | ALL_ITEMS                | entityDescription.contributors.identity.name                                                                                                                                                                                                                                           |
| contributor_name_not / contributorNameNot                      | TEXT          | NO_ITEMS                 | entityDescription.contributors.identity.name                                                                                                                                                                                                                                           |
| contributor_name_should / contributorNameShould                | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.contributors.identity.name                                                                                                                                                                                                                                           |
| course / course                                                | KEYWORD       | ALL_ITEMS                | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| course_not / courseNot                                         | KEYWORD       | NO_ITEMS                 | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| course_should / courseShould                                   | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.course.code.keyword                                                                                                                                                                                                                     |
| created_before / createdBefore                                 | DATE          | LESS_THAN                | createdDate                                                                                                                                                                                                                                                                            |
| created_since / createdSince                                   | DATE          | GREATER_THAN_OR_EQUAL_TO | createdDate                                                                                                                                                                                                                                                                            |
| cristin_identifier / cristinIdentifier                         | CUSTOM        | ALL_ITEMS                | CRISTIN_IDENTIFIER                                                                                                                                                                                                                                                                     |
| doi / doi                                                      | FUZZY_KEYWORD | ALL_ITEMS                | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| doi_not / doiNot                                               | FUZZY_KEYWORD | NO_ITEMS                 | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| doi_should / doiShould                                         | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.reference.doi, doi                                                                                                                                                                                                                                                   |
| exclude_subunits / excludeSubunits                             | IGNORED       | ALL_ITEMS                | EXCLUDE_SUBUNITS                                                                                                                                                                                                                                                                       |
| funding / funding                                              | CUSTOM        | ALL_ITEMS                | fundings.identifier.keyword, fundings.source.identifier.keyword                                                                                                                                                                                                                        |
| funding_identifier / fundingIdentifier                         | KEYWORD       | ALL_ITEMS                | fundings.identifier.keyword                                                                                                                                                                                                                                                            |
| funding_identifier_not / fundingIdentifierNot                  | KEYWORD       | NO_ITEMS                 | fundings.identifier.keyword                                                                                                                                                                                                                                                            |
| funding_identifier_should / fundingIdentifierShould            | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | fundings.identifier                                                                                                                                                                                                                                                                    |
| funding_source / fundingSource                                 | TEXT          | ALL_ITEMS                | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| funding_source_not / fundingSourceNot                          | TEXT          | NO_ITEMS                 | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| funding_source_should / fundingSourceShould                    | TEXT          | ONE_OR_MORE_ITEM         | fundings.identifier, fundings.source.identifier, fundings.source.labels.en, fundings.source.labels.nn, fundings.source.labels.nb, fundings.source.labels.sme                                                                                                                           |
| handle / handle                                                | FUZZY_KEYWORD | ALL_ITEMS                | handle                                                                                                                                                                                                                                                                                 |
| handle_not / handleNot                                         | FUZZY_KEYWORD | NO_ITEMS                 | handle                                                                                                                                                                                                                                                                                 |
| handle_should / handleShould                                   | TEXT          | ONE_OR_MORE_ITEM         | handle                                                                                                                                                                                                                                                                                 |
| files / files                                                  | KEYWORD       | ALL_ITEMS                | filesStatus.keyword                                                                                                                                                                                                                                                                    |
| id / id                                                        | KEYWORD       | ALL_ITEMS                | identifier.keyword                                                                                                                                                                                                                                                                     |
| id_not / idNot                                                 | KEYWORD       | NO_ITEMS                 | identifier.keyword                                                                                                                                                                                                                                                                     |
| id_should / idShould                                           | TEXT          | ONE_OR_MORE_ITEM         | identifier                                                                                                                                                                                                                                                                             |
| instance_type / instanceType                                   | KEYWORD       | ALL_ITEMS                | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| instance_type_not / instanceTypeNot                            | KEYWORD       | NO_ITEMS                 | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| instance_type_should / instanceTypeShould                      | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationInstance.type.keyword                                                                                                                                                                                                                           |
| institution / institution                                      | TEXT          | ALL_ITEMS                | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| institution_not / institutionNot                               | TEXT          | NO_ITEMS                 | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| institution_should / institutionShould                         | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.contributors.affiliations.id, entityDescription.contributors.affiliations.labels.en, entityDescription.contributors.affiliations.labels.nn, entityDescription.contributors.affiliations.labels.nb, entityDescription.contributors.affiliations.labels.sme            |
| isbn / isbn                                                    | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| isbn_not / isbnNot                                             | KEYWORD       | NOT_ONE_ITEM             | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| isbn_should / isbnShould                                       | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.isbnList                                                                                                                                                                                                                                |
| issn / issn                                                    | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.onlineIssn.keyword, entityDescription.reference.publicationContext.printIssn.keyword, entityDescription.reference.publicationContext.series.onlineIssn.keyword, entityDescription.reference.publicationContext.series.printIssn.keyword |
| issn_not / issnNot                                             | KEYWORD       | NOT_ONE_ITEM             | entityDescription.reference.publicationContext.onlineIssn.keyword, entityDescription.reference.publicationContext.printIssn.keyword, entityDescription.reference.publicationContext.series.onlineIssn.keyword, entityDescription.reference.publicationContext.series.printIssn.keyword |
| issn_should / issnShould                                       | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.onlineIssn, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.series.onlineIssn, entityDescription.reference.publicationContext.series.printIssn                                 |
| journal / journal                                              | FUZZY_KEYWORD | ALL_ITEMS                | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| journal_not / journalNot                                       | FUZZY_KEYWORD | NO_ITEMS                 | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| journal_should / journalShould                                 | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.name, entityDescription.reference.publicationContext.id, entityDescription.reference.publicationContext.printIssn, entityDescription.reference.publicationContext.onlineIssn                                                            |
| license / license                                              | CUSTOM        | ALL_ITEMS                | associatedArtifacts.license.keyword                                                                                                                                                                                                                                                    |
| license_not / licenseNot                                       | CUSTOM        | NO_ITEMS                 | associatedArtifacts.license.keyword                                                                                                                                                                                                                                                    |
| license_should / licenseShould                                 | KEYWORD       | ONE_OR_MORE_ITEM         | associatedArtifacts.license.keyword                                                                                                                                                                                                                                                    |
| modified_before / modifiedBefore                               | DATE          | LESS_THAN                | modifiedDate                                                                                                                                                                                                                                                                           |
| modified_since / modifiedSince                                 | DATE          | GREATER_THAN_OR_EQUAL_TO | modifiedDate                                                                                                                                                                                                                                                                           |
| orcid / orcid                                                  | KEYWORD       | ALL_ITEMS                | entityDescription.contributors.identity.orcId.keyword                                                                                                                                                                                                                                  |
| orcid_not / orcidNot                                           | KEYWORD       | NO_ITEMS                 | entityDescription.contributors.identity.orcId.keyword                                                                                                                                                                                                                                  |
| orcid_should / orcidShould                                     | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.contributors.identity.orcId                                                                                                                                                                                                                                          |
| parent_publication / parentPublication                         | KEYWORD       | ALL_ITEMS                | entityDescription.reference.publicationInstance.corrigendumFor.keyword, entityDescription.reference.publicationInstance.manifestations.id.keyword, entityDescription.reference.publicationInstance.id.keyword                                                                          |
| parent_publication_should / parentPublicationShould            | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationInstance.corrigendumFor, entityDescription.reference.publicationInstance.manifestations.id, entityDescription.reference.publicationInstance.id                                                                                                  |
| project / project                                              | KEYWORD       | ONE_OR_MORE_ITEM         | projects.id.keyword                                                                                                                                                                                                                                                                    |
| project_not / projectNot                                       | KEYWORD       | NOT_ONE_ITEM             | projects.id.keyword                                                                                                                                                                                                                                                                    |
| project_should / projectShould                                 | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | projects.id                                                                                                                                                                                                                                                                            |
| publication_language / publicationLanguage                     | KEYWORD       | ALL_ITEMS                | entityDescription.language.keyword                                                                                                                                                                                                                                                     |
| publication_language_not / publicationLanguageNot              | KEYWORD       | NO_ITEMS                 | entityDescription.language.keyword                                                                                                                                                                                                                                                     |
| publication_language_should / publicationLanguageShould        | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.language.keyword                                                                                                                                                                                                                                                     |
| publication_year_before / publicationYearBefore                | NUMBER        | LESS_THAN                | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_should / publicationYearShould                | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| publication_year_since / publicationYearSince                  | NUMBER        | GREATER_THAN_OR_EQUAL_TO | entityDescription.publicationDate.year                                                                                                                                                                                                                                                 |
| published_before / publishedBefore                             | DATE          | LESS_THAN                | publishedDate                                                                                                                                                                                                                                                                          |
| published_between / publishedBetween                           | DATE          | BETWEEN                  | publishedDate                                                                                                                                                                                                                                                                          |
| published_since / publishedSince                               | DATE          | GREATER_THAN_OR_EQUAL_TO | publishedDate                                                                                                                                                                                                                                                                          |
| publisher / publisher                                          | FUZZY_KEYWORD | ALL_ITEMS                | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_not / publisherNot                                   | FUZZY_KEYWORD | NO_ITEMS                 | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_should / publisherShould                             | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.publisher.name, entityDescription.reference.publicationContext.publisher.id, entityDescription.reference.publicationContext.publisher.isbnPrefix                                                                                        |
| publisher_id / publisherId                                     | TEXT          | ALL_ITEMS                | publisher.id                                                                                                                                                                                                                                                                           |
| publisher_id_not / publisherIdNot                              | TEXT          | NO_ITEMS                 | publisher.id                                                                                                                                                                                                                                                                           |
| publisher_id_should / publisherIdShould                        | TEXT          | ONE_OR_MORE_ITEM         | publisher.id                                                                                                                                                                                                                                                                           |
| scientific_value / scientificValue                             | KEYWORD       | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.publisher.scientificValue.keyword, entityDescription.reference.publicationContext.scientificValue.keyword                                                                                                                               |
| scientific_index_status / scientificIndexStatus                | KEYWORD       | ONE_OR_MORE_ITEM         | scientificIndex.status.keyword                                                                                                                                                                                                                                                         |
| scientific_index_status_not / scientificIndexStatusNot         | KEYWORD       | NOT_ONE_ITEM             | scientificIndex.status.keyword                                                                                                                                                                                                                                                         |
| scientific_report_period_since / scientificReportPeriodSince   | NUMBER        | GREATER_THAN_OR_EQUAL_TO | scientificIndex.year                                                                                                                                                                                                                                                                   |
| scientific_report_period_before / scientificReportPeriodBefore | NUMBER        | LESS_THAN                | scientificIndex.year                                                                                                                                                                                                                                                                   |
| scopus_identifier / scopusIdentifier                           | CUSTOM        | ALL_ITEMS                | SCOPUS_IDENTIFIER                                                                                                                                                                                                                                                                      |
| series / series                                                | FUZZY_KEYWORD | ALL_ITEMS                | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| series_not / seriesNot                                         | FUZZY_KEYWORD | NO_ITEMS                 | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| series_should / seriesShould                                   | FUZZY_KEYWORD | ONE_OR_MORE_ITEM         | entityDescription.reference.publicationContext.series.issn, entityDescription.reference.publicationContext.series.name, entityDescription.reference.publicationContext.series.title, entityDescription.reference.publicationContext.series.id                                          |
| status / status                                                | KEYWORD       | ALL_ITEMS                | status.keyword                                                                                                                                                                                                                                                                         |
| status_not / statusNot                                         | KEYWORD       | NO_ITEMS                 | status.keyword                                                                                                                                                                                                                                                                         |
| status_should / statusShould                                   | KEYWORD       | ONE_OR_MORE_ITEM         | status.keyword                                                                                                                                                                                                                                                                         |
| tags / tags                                                    | TEXT          | ALL_ITEMS                | entityDescription.tags                                                                                                                                                                                                                                                                 |
| tags_not / tagsNot                                             | TEXT          | NO_ITEMS                 | entityDescription.tags                                                                                                                                                                                                                                                                 |
| tags_should / tagsShould                                       | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.tags                                                                                                                                                                                                                                                                 |
| title / title                                                  | TEXT          | ALL_ITEMS                | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| title_not / titleNot                                           | TEXT          | NO_ITEMS                 | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| title_should / titleShould                                     | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.mainTitle                                                                                                                                                                                                                                                            |
| top_level_organization / topLevelOrganization                  | CUSTOM        | ONE_OR_MORE_ITEM         | topLevelOrganizations.id.keyword, contributorOrganizations.keyword                                                                                                                                                                                                                     |
| unit / unit                                                    | CUSTOM        | ALL_ITEMS                | entityDescription.contributors.affiliations.id.keyword, contributorOrganizations.keyword                                                                                                                                                                                               |
| unit_not / unitNot                                             | KEYWORD       | NO_ITEMS                 | entityDescription.contributors.affiliations.id.keyword                                                                                                                                                                                                                                 |
| unit_should / unitShould                                       | TEXT          | ONE_OR_MORE_ITEM         | entityDescription.contributors.affiliations.id                                                                                                                                                                                                                                         |
| user / user                                                    | KEYWORD       | ALL_ITEMS                | resourceOwner.owner.keyword                                                                                                                                                                                                                                                            |
| user_not / userNot                                             | KEYWORD       | NO_ITEMS                 | resourceOwner.owner.keyword                                                                                                                                                                                                                                                            |
| user_should / userShould                                       | TEXT          | ONE_OR_MORE_ITEM         | resourceOwner.owner                                                                                                                                                                                                                                                                    |
| user_affiliation / userAffiliation                             | KEYWORD       | ALL_ITEMS                | resourceOwner.ownerAffiliation.keyword                                                                                                                                                                                                                                                 |
| user_affiliation_not / userAffiliationNot                      | KEYWORD       | ALL_ITEMS                | resourceOwner.ownerAffiliation.keyword                                                                                                                                                                                                                                                 |
| user_affiliation_should / userAffiliationShould                | TEXT          | ALL_ITEMS                | resourceOwner.ownerAffiliation                                                                                                                                                                                                                                                         |
####  Query parameters passed to sws/opensearch
|key | queryKind         | scope                      |
|----|-------------------|----------------------------|
|search_all| text with ranking | all_items accross document |
|fields | list of keys | user, tags, title          |
####  Pagination parameters
|key | queryKind                   | example         |
|----|-----------------------------|-----------------|
| aggregation| Enum  | all, none       |
| page| number | 0 to 10000/size |
| from| number | 0 to 10000-size |
| size| number | 0 to 1000       |
| sort| key1:asc/desc,key2:desc/asc |
| sort_order| asc/desc           |
| search_after| sortindex          | api only        |


