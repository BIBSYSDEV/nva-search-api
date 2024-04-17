# Import Candidate

<details>
<summary>JSON Data model</summary>

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
<p></p>

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

| key_name                      | keyName                     | queryKind | scope                    | paths                                                                |
|-------------------------------|-----------------------------|-----------|--------------------------|----------------------------------------------------------------------|
| cristin_identifier            | cristinIdentifier           | custom    | all_items                | CRISTIN_IDENTIFIER                                                   |
| scopus_identifier             | scopusIdentifier            | custom    | all_items                | SCOPUS_IDENTIFIER                                                    |
| additional_identifiers_not    | additionalIdentifiersNot    | keyword   | no_items                 | additionalIdentifiers.value.keyword                                  |
| additional_identifiers_should | additionalIdentifiersShould | text      | one_or_more_item         | additionalIdentifiers.value                                          |
| category                      | category                    | keyword   | all_items                | publicationInstance.type                                             |
| category_not                  | categoryNot                 | keyword   | no_items                 | publicationInstance.type                                             |
| category_should               | categoryShould              | text      | one_or_more_item         | publicationInstance.type                                             |
| created_date                  | createdDate                 | date      | all_items                | createdDate                                                          |
| contributor                   | contributor                 | keyword   | all_items                | contributors.identity.id.keyword, contributors.identity.name.keyword |
| contributor_not               | contributorNot              | keyword   | no_items                 | contributors.identity.id.keyword, contributors.identity.name.keyword |
| contributor_should            | contributorShould           | text      | one_or_more_item         | contributors.identity.id, contributors.identity.name                 |
| contributor_name              | contributorName             | keyword   | all_items                | contributors.identity.name.keyword                                   |
| contributor_name_not          | contributorNameNot          | keyword   | no_items                 | contributors.identity.name.keyword                                   |
| contributor_name_should       | contributorNameShould       | text      | one_or_more_item         | contributors.identity.name                                           |
| collaboration_type            | collaborationType           | keyword   | all_items                | collaborationType.keyword                                            |
| collaboration_type_not        | collaborationTypeNot        | keyword   | no_items                 | collaborationType.keyword                                            |
| collaboration_type_should     | collaborationTypeShould     | text      | one_or_more_item         | collaborationType                                                    |
| doi                           | doi                         | keyword   | all_items                | doi.keyword                                                          |
| doi_not                       | doiNot                      | text      | no_items                 | doi                                                                  |
| doi_should                    | doiShould                   | text      | one_or_more_item         | doi                                                                  |
| id                            | id                          | keyword   | all_items                | id.keyword                                                           |
| id_not                        | idNot                       | keyword   | no_items                 | id.keyword                                                           |
| id_should                     | idShould                    | text      | one_or_more_item         | id                                                                   |
| import_status                 | importStatus                | keyword   | all_items                | importStatus.candidateStatus.keyword                                 |
| import_status_not             | importStatusNot             | keyword   | no_items                 | importStatus.candidateStatus.keyword                                 |
| import_status_should          | importStatusShould          | text      | one_or_more_item         | importStatus.candidateStatus                                         |
| instance_type                 | instanceType                | keyword   | all_items                | publicationInstance.type                                             |
| instance_type_not             | instanceTypeNot             | keyword   | no_items                 | publicationInstance.type                                             |
| instance_type_should          | instanceTypeShould          | text      | one_or_more_item         | publicationInstance.type                                             |
| publication_year              | publicationYear             | keyword   | all_items                | publicationYear.keyword                                              |
| publication_year_before       | publicationYearBefore       | number    | less_than                | publicationYear                                                      |
| publication_year_since        | publicationYearSince        | number    | greater_than_or_equal_to | publicationYear                                                      |
| publisher                     | publisher                   | keyword   | all_items                | publisher.id.keyword                                                 |
| publisher_not                 | publisherNot                | keyword   | no_items                 | publisher.id.keyword                                                 |
| publisher_should              | publisherShould             | text      | one_or_more_item         | publisher.id                                                         |
| title                         | title                       | text      | all_items                | mainTitle                                                            |
| title_not                     | titleNot                    | text      | no_items                 | mainTitle                                                            |
| title_should                  | titleShould                 | text      | one_or_more_item         | mainTitle                                                            |
| type                          | type                        | keyword   | all_items                | type.keyword                                                         |

> [!NOTE]
> <p>Valid SortKeys </p>
>
> ```
> category, instanceType, createdDate, modifiedDate, publishedDate, publicationDate, title, unitId, user
> ```
