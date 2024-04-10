# Tickets

## Data model

```json
  {
  "type": "PublishingRequest",
  "customerId": "https://api.dev.nva.aws.unit.no/customer/bb3d0c0c-5065-4623-9b98-5810983c2478",
  "modifiedDate": "2024-02-28T09:25:39.223178587Z",
  "createdDate": "2024-02-27T14:28:06.168047052Z",
  "workflow": "RegistratorPublishesMetadataOnly",
  "approvedFiles": [],
  "filesForApproval": [],
  "id": "https://api.dev.nva.aws.unit.no/publication/018d64b6415e-59ac68b4-f801-490d-8c16-b7b1052b3d6c/ticket/018deaf73598-3b01bbf7-754a-44b0-892a-e8ae13826d57",
  "messages": [],
  "viewedBy": [
    {
      "type": "Person",
      "firstName": "Terje",
      "lastName": "Hellesvik",
      "username": "1269057@20754.0.0.0"
    }
  ],
  "publication": {
    "owner": "1492596@20754.0.0.0",
    "status": "DRAFT",
    "publicationInstance": {
      "type": "MovingPicture",
      "subtype": {
        "type": "ShortFilm"
      },
      "description": "adawd",
      "outputs": [
        {
          "type": "Broadcast",
          "publisher": {
            "type": "UnconfirmedPublisher",
            "name": "NRK",
            "valid": true
          },
          "date": {
            "type": "Instant",
            "value": "2024-01-31T23:00:00Z"
          },
          "sequence": 1
        }
      ],
      "pages": {
        "type": "NullPages"
      }
    },
    "contributors": [ ],
    "id": "https://api.dev.nva.aws.unit.no/publication/018d64b6415e-59ac68b4-f801-490d-8c16-b7b1052b3d6c",
    "identifier": "018d64b6415e-59ac68b4-f801-490d-8c16-b7b1052b3d6c",
    "mainTitle": "Test filer"
  },
  "owner": {
    "type": "Person",
    "firstName": "Kir ",
    "lastName": "Truhacev",
    "username": "1492596@20754.0.0.0"
  },
  "organization": {
    "id": "https://api.dev.nva.aws.unit.no/cristin/organization/20754.3.1.0",
    "identifier": "20754.3.1.0",
    "partOf": [ ]
  },
  "status": "New"
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
|assignee|custom|all_items|
|assignee_not|text|no_items|
|by_user_pending| custom| |
|created_date|date|between|
|customer_id|fuzzy_keyword|one_or_more_item|
|customer_id_not|fuzzy_keyword|not_one_item|
|id|fuzzy_keyword|one_or_more_item|
|id_not|fuzzy_keyword|not_one_item|
|exclude_subunits|ignored|one_or_more_item|
|finalized_by|text|all_items|
|finalized_by_not|text|no_items|
|messages|text|all_items|
|messages_not|text|no_items|
|modified_date|date|between|
|organization_id|custom|one_or_more_item|
|organization_id_not|custom|not_one_item|
|owner|fuzzy_keyword|one_or_more_item|
|owner_not|fuzzy_keyword|not_one_item|
|publication_id|fuzzy_keyword|one_or_more_item|
|publication_id_not|fuzzy_keyword|not_one_item|
|publication_modified_date|date|between|
|publication_owner|fuzzy_keyword|one_or_more_item|
|publication_owner_not|fuzzy_keyword|not_one_item|
|publication_status|keyword|one_or_more_item|
|publication_status_not|keyword|not_one_item|
|publication_title|text|all_items|
|status|keyword|one_or_more_item|
|status_not|keyword|not_one_item|
|type|keyword|one_or_more_item|
|type_not|keyword|not_one_item|
|viewed_by|text|all_items|
|viewed_by_not|text|no_items|
###  Query parameters passed to sws/opensearch
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


