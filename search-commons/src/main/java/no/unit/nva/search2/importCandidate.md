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

| key                                                         | queryKind | scope                | paths                                                                |
|-------------------------------------------------------------|-----------|----------------------|----------------------------------------------------------------------|
| cristin_identifier / cristinIdentifier                      | custom    | allItems             | CRISTIN_IDENTIFIER                                                   |
| scopus_identifier / scopusIdentifier                        | custom    | allItems             | SCOPUS_IDENTIFIER                                                    |
| additional_identifiers_not / additionalIdentifiersNot       | keyword   | noItems              | additionalIdentifiers.value.keyword                                  |
| additional_identifiers_should / additionalIdentifiersShould | text      | oneOrMoreItem        | additionalIdentifiers.value                                          |
| category / category                                         | keyword   | allItems             | publicationInstance.type                                             |
| category_not / categoryNot                                  | keyword   | noItems              | publicationInstance.type                                             |
| category_should / categoryShould                            | text      | oneOrMoreItem        | publicationInstance.type                                             |
| created_date / createdDate                                  | date      | allItems             | createdDate                                                          |
| contributor / contributor                                   | keyword   | allItems             | contributors.identity.id.keyword, contributors.identity.name.keyword |
| contributor_not / contributorNot                            | keyword   | noItems              | contributors.identity.id.keyword, contributors.identity.name.keyword |
| contributor_should / contributorShould                      | text      | oneOrMoreItem        | contributors.identity.id, contributors.identity.name                 |
| contributor_name / contributorName                          | keyword   | allItems             | contributors.identity.name.keyword                                   |
| contributor_name_not / contributorNameNot                   | keyword   | noItems              | contributors.identity.name.keyword                                   |
| contributor_name_should / contributorNameShould             | text      | oneOrMoreItem        | contributors.identity.name                                           |
| collaboration_type / collaborationType                      | keyword   | allItems             | collaborationType.keyword                                            |
| collaboration_type_not / collaborationTypeNot               | keyword   | noItems              | collaborationType.keyword                                            |
| collaboration_type_should / collaborationTypeShould         | text      | oneOrMoreItem        | collaborationType                                                    |
| doi / doi                                                   | keyword   | allItems             | doi.keyword                                                          |
| doi_not / doiNot                                            | text      | noItems              | doi                                                                  |
| doi_should / doiShould                                      | text      | oneOrMoreItem        | doi                                                                  |
| id / id                                                     | keyword   | allItems             | id.keyword                                                           |
| id_not / idNot                                              | keyword   | noItems              | id.keyword                                                           |
| id_should / idShould                                        | text      | oneOrMoreItem        | id                                                                   |
| import_status / importStatus                                | keyword   | allItems             | importStatus.candidateStatus.keyword                                 |
| import_status_not / importStatusNot                         | keyword   | noItems              | importStatus.candidateStatus.keyword                                 |
| import_status_should / importStatusShould                   | text      | oneOrMoreItem        | importStatus.candidateStatus                                         |
| instance_type / instanceType                                | keyword   | allItems             | publicationInstance.type                                             |
| instance_type_not / instanceTypeNot                         | keyword   | noItems              | publicationInstance.type                                             |
| instance_type_should / instanceTypeShould                   | text      | oneOrMoreItem        | publicationInstance.type                                             |
| publication_year / publicationYear                          | keyword   | allItems             | publicationYear.keyword                                              |
| publication_year_before / publicationYearBefore             | number    | lessThan             | publicationYear                                                      |
| publication_year_since / publicationYearSince               | number    | greaterThanOrEqualTo | publicationYear                                                      |
| publisher / publisher                                       | keyword   | allItems             | publisher.id.keyword                                                 |
| publisher_not / publisherNot                                | keyword   | noItems              | publisher.id.keyword                                                 |
| publisher_should / publisherShould                          | text      | oneOrMoreItem        | publisher.id                                                         |
| title / title                                               | text      | allItems             | mainTitle                                                            |
| title_not / titleNot                                        | text      | noItems              | mainTitle                                                            |
| title_should / titleShould                                  | text      | oneOrMoreItem        | mainTitle                                                            |
| type / type                                                 | keyword   | allItems             | type.keyword                                                         |

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


