# /Tickets

[back to NVA search api README](/README.md#nva-search-api)

## Data Model
<details>
<summary>JSON</summary>

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
* acrossFields
  * Search through all paths as it where one field
* custom

### Key details

| key_name                  | keyName                 | queryKind    | scope            | paths                                                                                                            |
|---------------------------|-------------------------|--------------|------------------|------------------------------------------------------------------------------------------------------------------|
| assignee                  | assignee                | custom       | all_items        | assignee.firstName.keyword, assignee.lastName.keyword, assignee.username.keyword                                 |
| assignee_not              | assigneeNot             | acrossFields | no_items         | assignee.firstName, assignee.lastName, assignee.username                                                         |
| by_user_pending           | byUserPending           | ignored      | all_items        | BY_USER_PENDING                                                                                                  |
| created_date              | createdDate             | date         | between          | createdDate                                                                                                      |
| customer_id               | customerId              | fuzzyKeyword | one_or_more_item | customerId                                                                                                       |
| customer_id_not           | customerIdNot           | fuzzyKeyword | not_one_item     | customerId                                                                                                       |
| id                        | id                      | fuzzyKeyword | one_or_more_item | id                                                                                                               |
| id_not                    | idNot                   | fuzzyKeyword | not_one_item     | id                                                                                                               |
| exclude_subunits          | excludeSubunits         | ignored      | one_or_more_item | organization.id, organization.identifier                                                                         |
| finalized_by              | finalizedBy             | acrossFields | all_items        | finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                                                |
| finalized_by_not          | finalizedByNot          | acrossFields | no_items         | finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                                                |
| messages                  | messages                | text         | all_items        | messages.text, messages.status                                                                                   |
| messages_not              | messagesNot             | text         | no_items         | messages.text, messages.status                                                                                   |
| modified_date             | modifiedDate            | date         | between          | modifiedDate                                                                                                     |
| organization_id           | organizationId          | custom       | one_or_more_item | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| organization_id_not       | organizationIdNot       | custom       | not_one_item     | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| owner                     | owner                   | acrossFields | one_or_more_item | owner.firstName, owner.lastName, owner.username                                                                  |
| owner_not                 | ownerNot                | acrossFields | not_one_item     | owner.firstName, owner.lastName, owner.username                                                                  |
| publication_id            | publicationId           | fuzzyKeyword | one_or_more_item | publication.id, publication.identifier                                                                           |
| publication_id_not        | publicationIdNot        | fuzzyKeyword | not_one_item     | publication.id, publication.identifier                                                                           |
| publication_instance      | publicationInstance     | fuzzyKeyword | not_one_item     | publication.publicationInstance.type                                                                             |
| publication_instance_not  | publicationInstanceNot  | fuzzyKeyword | not_one_item     | publication.publicationInstance.type                                                                             |
| publication_modified_date | publicationModifiedDate | date         | between          | publication.modifiedDate                                                                                         |
| publication_owner         | publicationOwner        | fuzzyKeyword | one_or_more_item | publication.owner                                                                                                |
| publication_owner_not     | publicationOwnerNot     | fuzzyKeyword | not_one_item     | publication.owner                                                                                                |
| publication_status        | publicationStatus       | keyword      | one_or_more_item | publication.status.keyword                                                                                       |
| publication_status_not    | publicationStatusNot    | keyword      | not_one_item     | publication.status.keyword                                                                                       |
| publication_title         | publicationTitle        | text         | all_items        | publication.mainTitle                                                                                            |
| status                    | status                  | keyword      | one_or_more_item | status.keyword                                                                                                   |
| status_not                | statusNot               | keyword      | not_one_item     | status.keyword                                                                                                   |
| type                      | type                    | keyword      | one_or_more_item | type.keyword                                                                                                     |
| type_not                  | typeNot                 | keyword      | not_one_item     | type.keyword                                                                                                     |
| viewed_by                 | viewedBy                | acrossFields | all_items        | viewedBy.firstName, viewedBy.lastName, viewedBy.username                                                         |
| viewed_by_not             | viewedByNot             | acrossFields | no_items         | viewedBy.firstName, viewedBy.lastName, viewedBy.username                                                         |
| search_all                | searchAll               | freeText     | all_items        | *                                                                                                                |

> [!NOTE]
> <p>Valid SortKeys </p>
>
> ```
> category, instanceType, createdDate, modifiedDate, publishedDate, publicationDate, title, unitId, user
> ```
