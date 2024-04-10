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
|key |queryKind|scope|
|----|---|---|
|abstract|text|all_items|
|abstract_not|text|no_items|
|abstract_should|text|one_or_more_item|
|context_type|keyword|all_items|
|context_type_not|keyword|no_items|
|context_type_should|keyword|one_or_more_item|
|contributor|keyword|all_items|
|contributor_not|keyword|no_items|
|contributor_should|keyword|one_or_more_item|
|contributor_name|text|all_items|
|contributor_name_not|text|no_items|
|contributor_name_should|text|one_or_more_item|
|course|keyword|all_items|
|course_not|keyword|no_items|
|course_should|keyword|one_or_more_item|
|created_before|date|less_than|
|created_since|date|greater_than_or_equal_to|
|cristin_identifier|custom|	|
|doi_not|fuzzy_keyword|no_items|
|doi_should|text|one_or_more_item|
|exclude_subunits|boolean| |
|funding_identifier|keyword|all_items|
|funding_identifier_not|keyword|no_items|
|funding_identifier_should|fuzzy_keyword|one_or_more_item|
|funding_source|text|all_items|
|funding_source_not|text|no_items|
|funding_source_should|text|one_or_more_item|
|handle|fuzzy_keyword|all_items|
|handle_not|fuzzy_keyword|no_items|
|handle_should|text|one_or_more_item|
|files|keyword|all_items|
|id|keyword|one_or_more_item|
|id_not|keyword|no_items|
|id_should|text|one_or_more_item|
|instance_type|keyword|all_items|
|instance_type_not|keyword|no_items|
|instance_type_should|keyword|one_or_more_item|
|institution|text|all_items|
|institution_not|text|no_items|
|institution_should|text|one_or_more_item|
|isbn|keyword|one_or_more_item|
|isbn_not|keyword|not_one_item|
|isbn_should|fuzzy_keyword|one_or_more_item|
|issn|keyword|one_or_more_item|
|issn_not|keyword|not_one_item|
|issn_should|fuzzy_keyword|one_or_more_item|
|journal|fuzzy_keyword|all_items|
|journal_not|fuzzy_keyword|no_items|
|journal_should|fuzzy_keyword|one_or_more_item|
|license|custom|all_items|
|license_not|custom|no_items|
|license_should|keyword|one_or_more_item|
|modified_before|date|less_than|
|modified_since|date|greater_than_or_equal_to|
|orcid|keyword|all_items|
|orcid_not|keyword|no_items|
|orcid_should|text|one_or_more_item|
|parent_publication|keyword|all_items|
|parent_publication_should|text|one_or_more_item|
|project|keyword|one_or_more_item|
|project_not|keyword|not_one_item|
|project_should|fuzzy_keyword|one_or_more_item|
|publication_language|keyword|all_items|
|publication_language_not|keyword|no_items|
|publication_language_should|keyword|one_or_more_item|
|publication_year_before|number|less_than|
|publication_year_should|keyword|one_or_more_item) null)|
|publication_year_since|number|greater_than_or_equal_to|
|published_before|date|less_than|
|published_between|date|between|
|published_since|date|greater_than_or_equal_to|
|publisher|fuzzy_keyword|all_items|
|publisher_not|fuzzy_keyword|no_items|
|publisher_should|fuzzy_keyword|one_or_more_item|
|publisher_id|text|all_items|
|publisher_id_not|text|no_items|
|publisher_id_should|text|one_or_more_item|
|scientific_value|keyword|one_or_more_item|
|scientific_index_status|keyword|one_or_more_item|
|scientific_index_status_not|keyword|not_one_item|
|scientific_report_period_since|number|greater_than_or_equal_to|
|scientific_report_period_before|number|less_than|
|scopus_identifier|custom| 	|
|series|fuzzy_keyword|all_items|
|series_not|fuzzy_keyword|no_items|
|series_should|fuzzy_keyword|one_or_more_item|
|status|keyword|all_items|
|status_not|keyword|no_items|
|status_should|keyword|one_or_more_item|
|tags|text|all_items|
|tags_not|text|no_items|
|tags_should|text|one_or_more_item|
|title|text|all_items|
|title_not|text|no_items|
|title_should|text|one_or_more_item|
|top_level_organization|custom|one_or_more_item|
|unit|custom|all_items|
|unit_not|keyword|no_items|
|unit_should|text|one_or_more_item|
|user|keyword|all_items|
|user_not|keyword|no_items|
|user_should|text|one_or_more_item|
|user_affiliation|keyword|all_items|
|user_affiliation_not|keyword|all_items|
|user_affiliation_should|text|one_or_more_item|
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


