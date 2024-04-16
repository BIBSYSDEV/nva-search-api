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

| key                                                 | queryKind     | scope            | paths                                                                                                            |
|-----------------------------------------------------|---------------|------------------|------------------------------------------------------------------------------------------------------------------|
| assignee / assignee                                 | CUSTOM        | ALL_ITEMS        | assignee.type.keyword, assignee.firstName.keyword, assignee.lastName.keyword, assignee.username.keyword          |
| assignee_not / assigneeNot                          | TEXT          | NO_ITEMS         | assignee.type, assignee.firstName, assignee.lastName, assignee.username                                          |
| by_user_pending / byUserPending                     | BOOLEAN       | ALL_ITEMS        | BY_USER_PENDING                                                                                                  |
| created_date / createdDate                          | DATE          | BETWEEN          | createdDate                                                                                                      |
| customer_id / customerId                            | FUZZY_KEYWORD | ONE_OR_MORE_ITEM | customerId                                                                                                       |
| customer_id_not / customerIdNot                     | FUZZY_KEYWORD | NOT_ONE_ITEM     | customerId                                                                                                       |
| id / id                                             | FUZZY_KEYWORD | ONE_OR_MORE_ITEM | id                                                                                                               |
| id_not / idNot                                      | FUZZY_KEYWORD | NOT_ONE_ITEM     | id                                                                                                               |
| exclude_subunits / excludeSubunits                  | IGNORED       | ONE_OR_MORE_ITEM | organization.id, organization.identifier                                                                         |
| finalized_by / finalizedBy                          | TEXT          | ALL_ITEMS        | finalizedBy.type, finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                              |
| finalized_by_not / finalizedByNot                   | TEXT          | NO_ITEMS         | finalizedBy.type, finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                              |
| messages / messages                                 | TEXT          | ALL_ITEMS        | messages.type, messages.text, messages.status                                                                    |
| messages_not / messagesNot                          | TEXT          | NO_ITEMS         | messages.type, messages.text, messages.status                                                                    |
| modified_date / modifiedDate                        | DATE          | BETWEEN          | modifiedDate                                                                                                     |
| organization_id / organizationId                    | CUSTOM        | ONE_OR_MORE_ITEM | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| organization_id_not / organizationIdNot             | CUSTOM        | NOT_ONE_ITEM     | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| owner / owner                                       | FUZZY_KEYWORD | ONE_OR_MORE_ITEM | owner.type, owner.firstName, owner.lastName, owner.username                                                      |
| owner_not / ownerNot                                | FUZZY_KEYWORD | NOT_ONE_ITEM     | owner.type, owner.firstName, owner.lastName, owner.username                                                      |
| publication_id / publicationId                      | FUZZY_KEYWORD | ONE_OR_MORE_ITEM | publication.id, publication.identifier                                                                           |
| publication_id_not / publicationIdNot               | FUZZY_KEYWORD | NOT_ONE_ITEM     | publication.id, publication.identifier                                                                           |
| publication_modified_date / publicationModifiedDate | DATE          | BETWEEN          | publication.modifiedDate                                                                                         |
| publication_owner / publicationOwner                | FUZZY_KEYWORD | ONE_OR_MORE_ITEM | publication.owner                                                                                                |
| publication_owner_not / publicationOwnerNot         | FUZZY_KEYWORD | NOT_ONE_ITEM     | publication.owner                                                                                                |
| publication_status / publicationStatus              | KEYWORD       | ONE_OR_MORE_ITEM | publication.status.keyword                                                                                       |
| publication_status_not / publicationStatusNot       | KEYWORD       | NOT_ONE_ITEM     | publication.status.keyword                                                                                       |
| publication_title / publicationTitle                | TEXT          | ALL_ITEMS        | publication.mainTitle                                                                                            |
| status / status                                     | KEYWORD       | ONE_OR_MORE_ITEM | status.keyword                                                                                                   |
| status_not / statusNot                              | KEYWORD       | NOT_ONE_ITEM     | status.keyword                                                                                                   |
| type / type                                         | KEYWORD       | ONE_OR_MORE_ITEM | type.keyword                                                                                                     |
| type_not / typeNot                                  | KEYWORD       | NOT_ONE_ITEM     | type.keyword                                                                                                     |
| viewed_by / viewedBy                                | TEXT          | ALL_ITEMS        | viewedBy.type, viewedBy.firstName, viewedBy.lastName, viewedBy.username                                          |
| viewed_by_not / viewedByNot                         | TEXT          | NO_ITEMS         | viewedBy.type, viewedBy.firstName, viewedBy.lastName, viewedBy.username                                          |
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


