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
  * hits on any partial match in field(s), boosts on exact match and phrases
* free_text
  * Search through whole document
* custom
  * 

### All available filters

| key_name                  | keyName                 | queryKind    | scope         | paths                                                                                                            |
|---------------------------|-------------------------|--------------|---------------|------------------------------------------------------------------------------------------------------------------|
| assignee                  | assignee                | custom       | allItems      | assignee.type.keyword, assignee.firstName.keyword, assignee.lastName.keyword, assignee.username.keyword          |
| assignee_not              | assigneeNot             | text         | noItems       | assignee.type, assignee.firstName, assignee.lastName, assignee.username                                          |
| by_user_pending           | byUserPending           | ignored      | allItems      | BY_USER_PENDING                                                                                                  |
| created_date              | createdDate             | date         | between       | createdDate                                                                                                      |
| customer_id               | customerId              | fuzzyKeyword | oneOrMoreItem | customerId                                                                                                       |
| customer_id_not           | customerIdNot           | fuzzyKeyword | notOneItem    | customerId                                                                                                       |
| id                        | id                      | fuzzyKeyword | oneOrMoreItem | id                                                                                                               |
| id_not                    | idNot                   | fuzzyKeyword | notOneItem    | id                                                                                                               |
| exclude_subunits          | excludeSubunits         | ignored      | oneOrMoreItem | organization.id, organization.identifier                                                                         |
| finalized_by              | finalizedBy             | text         | allItems      | finalizedBy.type, finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                              |
| finalized_by_not          | finalizedByNot          | text         | noItems       | finalizedBy.type, finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                              |
| messages                  | messages                | text         | allItems      | messages.type, messages.text, messages.status                                                                    |
| messages_not              | messagesNot             | text         | noItems       | messages.type, messages.text, messages.status                                                                    |
| modified_date             | modifiedDate            | date         | between       | modifiedDate                                                                                                     |
| organization_id           | organizationId          | custom       | oneOrMoreItem | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| organization_id_not       | organizationIdNot       | custom       | notOneItem    | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| owner                     | owner                   | fuzzyKeyword | oneOrMoreItem | owner.type, owner.firstName, owner.lastName, owner.username                                                      |
| owner_not                 | ownerNot                | fuzzyKeyword | notOneItem    | owner.type, owner.firstName, owner.lastName, owner.username                                                      |
| publication_id            | publicationId           | fuzzyKeyword | oneOrMoreItem | publication.id, publication.identifier                                                                           |
| publication_id_not        | publicationIdNot        | fuzzyKeyword | notOneItem    | publication.id, publication.identifier                                                                           |
| publication_modified_date | publicationModifiedDate | date         | between       | publication.modifiedDate                                                                                         |
| publication_owner         | publicationOwner        | fuzzyKeyword | oneOrMoreItem | publication.owner                                                                                                |
| publication_owner_not     | publicationOwnerNot     | fuzzyKeyword | notOneItem    | publication.owner                                                                                                |
| publication_status        | publicationStatus       | keyword      | oneOrMoreItem | publication.status.keyword                                                                                       |
| publication_status_not    | publicationStatusNot    | keyword      | notOneItem    | publication.status.keyword                                                                                       |
| publication_title         | publicationTitle        | text         | allItems      | publication.mainTitle                                                                                            |
| status                    | status                  | keyword      | oneOrMoreItem | status.keyword                                                                                                   |
| status_not                | statusNot               | keyword      | notOneItem    | status.keyword                                                                                                   |
| type                      | type                    | keyword      | oneOrMoreItem | type.keyword                                                                                                     |
| type_not                  | typeNot                 | keyword      | notOneItem    | type.keyword                                                                                                     |
| viewed_by                 | viewedBy                | text         | allItems      | viewedBy.type, viewedBy.firstName, viewedBy.lastName, viewedBy.username                                          |
| viewed_by_not             | viewedByNot             | text         | noItems       | viewedBy.type, viewedBy.firstName, viewedBy.lastName, viewedBy.username                                          |

###  Query parameters passed to sws/opensearch

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
| sort         | key1:asc/desc,key2:desc/asc |                 |
| sort_order   | asc/desc                    |                 |
| search_after | sortindex                   | api only        |


