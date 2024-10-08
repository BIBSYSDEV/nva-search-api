# /Import Candidate

[back to NVA search api README](/README.md#nva-search-api)

## Data Model

<details>
<summary>JSON</summary>

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

</details>

## Examples

<details>
<summary>Search examples</summary>

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

</details>

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

| key_name                   | keyName                  | queryKind    | scope                    | paths                                                                                                                                                                                                                                                                            |
|----------------------------|--------------------------|--------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| additional_identifiers     | additionalIdentifiers    | fuzzyKeyword | any_of                   | additionalIdentifiers.value                                                                                                                                                                                                                                                      |
| additional_identifiers_not | additionalIdentifiersNot | keyword      | not_all_of               | additionalIdentifiers.value.keyword                                                                                                                                                                                                                                              |
| category                   | category                 | fuzzyKeyword | any_of                   | publicationInstance.type                                                                                                                                                                                                                                                         |
| category_not               | categoryNot              | keyword      | not_all_of               | publicationInstance.type.keyword                                                                                                                                                                                                                                                 |
| created_date               | createdDate              | date         | between                  | createdDate                                                                                                                                                                                                                                                                      |
| contributor                | contributor              | fuzzyKeyword | all_of                   | contributors.identity.id, contributors.identity.name                                                                                                                                                                                                                             |
| contributor_not            | contributorNot           | fuzzyKeyword | not_all_of               | contributors.identity.id, contributors.identity.name                                                                                                                                                                                                                             |
| collaboration_type         | collaborationType        | fuzzyKeyword | any_of                   | collaborationType                                                                                                                                                                                                                                                                |
| collaboration_type_not     | collaborationTypeNot     | keyword      | not_all_of               | collaborationType.keyword                                                                                                                                                                                                                                                        |
| cristin_identifier         | cristinIdentifier        | custom       | all_of                   | CRISTIN_IDENTIFIER                                                                                                                                                                                                                                                               |
| doi                        | doi                      | fuzzyKeyword | any_of                   | doi                                                                                                                                                                                                                                                                              |
| doi_not                    | doiNot                   | text         | not_all_of               | doi                                                                                                                                                                                                                                                                              |
| files                      | files                    | keyword      | all_of                   | filesStatus.keyword                                                                                                                                                                                                                                                              |
| id                         | id                       | fuzzyKeyword | any_of                   | id                                                                                                                                                                                                                                                                               |
| id_not                     | idNot                    | fuzzyKeyword | not_all_of               | id                                                                                                                                                                                                                                                                               |
| import_status              | importStatus             | fuzzyKeyword | any_of                   | importStatus.candidateStatus, importStatus.setBy                                                                                                                                                                                                                                 |
| import_status_not          | importStatusNot          | fuzzyKeyword | not_all_of               | importStatus.candidateStatus, importStatus.setBy                                                                                                                                                                                                                                 |
| license                    | license                  | custom       | all_of                   | associatedArtifacts.license.name.keyword, associatedArtifacts.license.value.keyword, associatedArtifacts.license.labels.en.keyword, associatedArtifacts.license.labels.nn.keyword, associatedArtifacts.license.labels.nb.keyword, associatedArtifacts.license.labels.sme.keyword |
| modified_date              | modifiedDate             | date         | between                  | importStatus.modifiedDate                                                                                                                                                                                                                                                        |
| license_not                | licenseNot               | custom       | not_all_of               | associatedArtifacts.license.name.keyword, associatedArtifacts.license.value.keyword, associatedArtifacts.license.labels.en.keyword, associatedArtifacts.license.labels.nn.keyword, associatedArtifacts.license.labels.nb.keyword, associatedArtifacts.license.labels.sme.keyword |
| publication_year           | publicationYear          | number       | between                  | publicationYear                                                                                                                                                                                                                                                                  |
| publication_year_before    | publicationYearBefore    | number       | less_than                | publicationYear                                                                                                                                                                                                                                                                  |
| publication_year_since     | publicationYearSince     | number       | greater_than_or_equal_to | publicationYear                                                                                                                                                                                                                                                                  |
| publisher                  | publisher                | keyword      | all_of                   | publisher.id.keyword                                                                                                                                                                                                                                                             |
| publisher_not              | publisherNot             | keyword      | not_all_of               | publisher.id.keyword                                                                                                                                                                                                                                                             |
| scopus_identifier          | scopusIdentifier         | custom       | all_of                   | SCOPUS_IDENTIFIER                                                                                                                                                                                                                                                                |
| top_level_organization     | topLevelOrganization     | keyword      | any_of                   | organizations.id.keyword                                                                                                                                                                                                                                                         |
| top_level_organization_not | topLevelOrganizationNot  | keyword      | not_all_of               | organizations.id.keyword                                                                                                                                                                                                                                                         |
| title                      | title                    | text         | any_of                   | mainTitle                                                                                                                                                                                                                                                                        |
| title_not                  | titleNot                 | text         | not_all_of               | mainTitle                                                                                                                                                                                                                                                                        |
| type                       | type                     | fuzzyKeyword | any_of                   | publicationInstance.type                                                                                                                                                                                                                                                         |
| type_not                   | typeNot                  | keyword      | not_all_of               | publicationInstance.type.keyword                                                                                                                                                                                                                                                 |
| search_all                 | searchAll                | freeText     | all_of                   | q                                                                                                                                                                                                                                                                                |

[!NOTE]
> <p>Valid SortKeys </p>
>
> ```
> category, instanceType, createdDate, modifiedDate, publishedDate, publicationDate, title, unitId, user
> ```
