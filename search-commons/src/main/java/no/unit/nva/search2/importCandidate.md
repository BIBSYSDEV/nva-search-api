# Tickets

## Data model

```json
{
  "importStatus": {
    "candidateStatus": "NOT_IMPORTED",
    "modifiedDate": "2023-11-20T19:38:32.362135196Z"
  },
  "collaborationType": "NonCollaborative",
  "type": "ImportCandidateSummary",
  "publicationInstance": {
    "volume": "60",
    "issue": "3",
    "articleNumber": "036102",
    "type": "AcademicArticle"
  },
  "associatedArtifacts": [],
  "journal": {
    "id": "https://api.dev.nva.aws.unit.no/publication-channels-v2/journal/899497CD-FC96-431D-BE38-5B10F1428969/2021",
    "type": "Journal"
  },
  "createdDate": "2023-11-20T19:38:32.361612653Z",
  "totalVerifiedContributors": 0,
  "mainTitle": "All-optical multi-wavelength regenerator based on four-wave mixing",
  "organizations": [],
  "additionalIdentifiers": [
    {
      "sourceName": "Scopus",
      "type": "AdditionalIdentifier",
      "value": "2-s2.0-85104787031"
    },
    {
      "sourceName": "Cristin",
      "type": "AdditionalIdentifier",
      "value": "3212342"
    }
  ],
  "publicationYear": "2021",
  "id": "https://api.dev.nva.aws.unit.no/publication/import-candidate/018bee3ddae4-653812a8-ed19-469b-8078-c3b488f71f74",
  "contributors": [
    {
      "sequence": 1,
      "role": {
        "type": "Creator"
      },
      "identity": {
        "name": "Muhammad Usama Khan",
        "type": "Identity"
      },
      "correspondingAuthor": false,
      "affiliations": [
        {
          "type": "Organization",
          "labels": {
            "en": "National University of Sciences and Technology, School of Electrical Engineering and Computer Science"
          }
        }
      ],
      "type": "Contributor"
    },
    {
      "sequence": 2,
      "role": {
        "type": "Creator"
      },
      "identity": {
        "name": "Abdulah Jeza Aljohani",
        "type": "Identity"
      },
      "correspondingAuthor": false,
      "affiliations": [
        {
          "id": "https://api.dev.nva.aws.unit.no/cristin/organization/54400004.0.0.0",
          "type": "Organization",
          "labels": {
            "nb": "King Abdul Aziz University",
            "en": "King Abdul Aziz University"
          }
        }
      ],
      "type": "Contributor"
    },
    {
      "sequence": 3,
      "role": {
        "type": "Creator"
      },
      "identity": {
        "name": "Aamir Gulistan",
        "orcId": "https://orcid.org/0000-0002-9520-4211",
        "type": "Identity"
      },
      "correspondingAuthor": false,
      "affiliations": [
        {
          "id": "https://api.dev.nva.aws.unit.no/cristin/organization/20277.0.0.0",
          "type": "Organization",
          "labels": {
            "nb": "Simula Metropolitan Center for Digital Engineering"
          }
        },
        {
          "id": "https://api.dev.nva.aws.unit.no/cristin/organization/7498.0.0.0",
          "type": "Organization",
          "labels": {
            "nb": "Simula Research Laboratory"
          }
        }
      ],
      "type": "Contributor"
    },
    {
      "sequence": 4,
      "role": {
        "type": "Creator"
      },
      "identity": {
        "name": "Salman Ghafoor",
        "orcId": "https://orcid.org/0000-0002-1031-4471",
        "type": "Identity"
      },
      "correspondingAuthor": true,
      "affiliations": [
        {
          "type": "Organization",
          "labels": {
            "en": "National University of Sciences and Technology, School of Electrical Engineering and Computer Science"
          }
        }
      ],
      "type": "Contributor"
    }
  ],
  "doi": "https://doi.org/10.1117/1.OE.60.3.036102",
  "totalContributors": 4
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

| key                                                         | queryKind | scope                    | paths                                                                |
|-------------------------------------------------------------|-----------|--------------------------|----------------------------------------------------------------------|
| cristin_identifier / cristinIdentifier                      | CUSTOM    | ALL_ITEMS                | CRISTIN_IDENTIFIER                                                   |
| scopus_identifier / scopusIdentifier                        | CUSTOM    | ALL_ITEMS                | SCOPUS_IDENTIFIER                                                    |
| additional_identifiers_not / additionalIdentifiersNot       | KEYWORD   | NO_ITEMS                 | additionalIdentifiers.value.keyword                                  |
| additional_identifiers_should / additionalIdentifiersShould | TEXT      | ONE_OR_MORE_ITEM         | additionalIdentifiers.value                                          |
| category / category                                         | KEYWORD   | ALL_ITEMS                | publicationInstance.type                                             |
| category_not / categoryNot                                  | KEYWORD   | NO_ITEMS                 | publicationInstance.type                                             |
| category_should / categoryShould                            | TEXT      | ONE_OR_MORE_ITEM         | publicationInstance.type                                             |
| created_date / createdDate                                  | DATE      | ALL_ITEMS                | createdDate                                                          |
| contributor / contributor                                   | KEYWORD   | ALL_ITEMS                | contributors.identity.id.keyword, contributors.identity.name.keyword |
| contributor_not / contributorNot                            | KEYWORD   | NO_ITEMS                 | contributors.identity.id.keyword, contributors.identity.name.keyword |
| contributor_should / contributorShould                      | TEXT      | ONE_OR_MORE_ITEM         | contributors.identity.id, contributors.identity.name                 |
| contributor_name / contributorName                          | KEYWORD   | ALL_ITEMS                | contributors.identity.name.keyword                                   |
| contributor_name_not / contributorNameNot                   | KEYWORD   | NO_ITEMS                 | contributors.identity.name.keyword                                   |
| contributor_name_should / contributorNameShould             | TEXT      | ONE_OR_MORE_ITEM         | contributors.identity.name                                           |
| collaboration_type / collaborationType                      | KEYWORD   | ALL_ITEMS                | collaborationType.keyword                                            |
| collaboration_type_not / collaborationTypeNot               | KEYWORD   | NO_ITEMS                 | collaborationType.keyword                                            |
| collaboration_type_should / collaborationTypeShould         | TEXT      | ONE_OR_MORE_ITEM         | collaborationType                                                    |
| doi / doi                                                   | KEYWORD   | ALL_ITEMS                | doi.keyword                                                          |
| doi_not / doiNot                                            | TEXT      | NO_ITEMS                 | doi                                                                  |
| doi_should / doiShould                                      | TEXT      | ONE_OR_MORE_ITEM         | doi                                                                  |
| id / id                                                     | KEYWORD   | ALL_ITEMS                | id.keyword                                                           |
| id_not / idNot                                              | KEYWORD   | NO_ITEMS                 | id.keyword                                                           |
| id_should / idShould                                        | TEXT      | ONE_OR_MORE_ITEM         | id                                                                   |
| import_status / importStatus                                | KEYWORD   | ALL_ITEMS                | importStatus.candidateStatus.keyword                                 |
| import_status_not / importStatusNot                         | KEYWORD   | NO_ITEMS                 | importStatus.candidateStatus.keyword                                 |
| import_status_should / importStatusShould                   | TEXT      | ONE_OR_MORE_ITEM         | importStatus.candidateStatus                                         |
| instance_type / instanceType                                | KEYWORD   | ALL_ITEMS                | publicationInstance.type                                             |
| instance_type_not / instanceTypeNot                         | KEYWORD   | NO_ITEMS                 | publicationInstance.type                                             |
| instance_type_should / instanceTypeShould                   | TEXT      | ONE_OR_MORE_ITEM         | publicationInstance.type                                             |
| publication_year / publicationYear                          | KEYWORD   | ALL_ITEMS                | publicationYear.keyword                                              |
| publication_year_before / publicationYearBefore             | NUMBER    | LESS_THAN                | publicationYear                                                      |
| publication_year_since / publicationYearSince               | NUMBER    | GREATER_THAN_OR_EQUAL_TO | publicationYear                                                      |
| publisher / publisher                                       | KEYWORD   | ALL_ITEMS                | publisher.id.keyword                                                 |
| publisher_not / publisherNot                                | KEYWORD   | NO_ITEMS                 | publisher.id.keyword                                                 |
| publisher_should / publisherShould                          | TEXT      | ONE_OR_MORE_ITEM         | publisher.id                                                         |
| title / title                                               | TEXT      | ALL_ITEMS                | mainTitle                                                            |
| title_not / titleNot                                        | TEXT      | NO_ITEMS                 | mainTitle                                                            |
| title_should / titleShould                                  | TEXT      | ONE_OR_MORE_ITEM         | mainTitle                                                            |
| type / type                                                 | KEYWORD   | ALL_ITEMS                | type.keyword                                                         |

### Query parameters passed to sws/opensearch

| key        | queryKind         | scope                      |
|------------|-------------------|----------------------------|
| search_all | text with ranking | all_items accross document |
| fields     | list of keys      | user, tags, title          |

#### Pagination parameters

| key          | queryKind                   | example         |
|--------------|-----------------------------|-----------------|
| aggregation  | Enum                        | all, none       |
| page         | number                      | 0 to 10000/size |
| from         | number                      | 0 to 10000-size |
| size         | number                      | 0 to 1000       |
| sort         | key1:asc/desc,key2:desc/asc |                 |
| sort_order   | asc/desc                    |                 |
| search_after | sortindex                   | api only        |


