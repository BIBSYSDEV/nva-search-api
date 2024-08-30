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
    "contributors": [],
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
    "partOf": []
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

### Scope

* all_items
    * every search word must hit  (an AND search)
* no_items
    * inverted of 'all_items'
* one_or_more_item
    * any word can hit (an OR search)
* not_one_item
    * inverted of 'one_or_more_item'
* between
    * Numbers
        * <code>key=1000</code> -> hit all with this value
        * <code>key=,1000</code> -> hits all values below, including value
        * <code>key=1000,</code> -> hits all values over, including value
        * <code>key=500,1000</code> -> hits all values between numbers, including the values.
    * Dates
        * <code>key=2022</code> -> any date in 2022
        * <code>key=2022,2022</code> -> any date in 2022
        * <code>key=,2022</code> -> any date prior to 2023 (2022 and backward)
        * <code>key=2022,</code> -> any date after 2021 (2022 and onward)
        * <code>key=2022,2023</code> -> and date in 2022 or 2023

### Key details

| key_name                  | keyName                 | queryKind    | scope      | paths                                                                                                            |
|---------------------------|-------------------------|--------------|------------|------------------------------------------------------------------------------------------------------------------|
| assignee                  | assignee                | custom       | all_of     | assignee.firstName.keyword, assignee.lastName.keyword, assignee.username.keyword                                 |
| assignee_not              | assigneeNot             | acrossFields | not_all_of | assignee.firstName, assignee.lastName, assignee.username                                                         |
| by_user_pending           | byUserPending           | ignored      | all_of     | BY_USER_PENDING                                                                                                  |
| created_date              | createdDate             | date         | between    | createdDate                                                                                                      |
| customer_id               | customerId              | fuzzyKeyword | any_of     | customerId                                                                                                       |
| customer_id_not           | customerIdNot           | fuzzyKeyword | not_any_of | customerId                                                                                                       |
| id                        | id                      | fuzzyKeyword | any_of     | id                                                                                                               |
| id_not                    | idNot                   | fuzzyKeyword | not_any_of | id                                                                                                               |
| exclude_subunits          | excludeSubunits         | ignored      | any_of     | organization.id, organization.identifier                                                                         |
| finalized_by              | finalizedBy             | acrossFields | all_of     | finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                                                |
| finalized_by_not          | finalizedByNot          | acrossFields | not_all_of | finalizedBy.firstName, finalizedBy.lastName, finalizedBy.username                                                |
| messages                  | messages                | text         | all_of     | messages.text, messages.status                                                                                   |
| messages_not              | messagesNot             | text         | not_all_of | messages.text, messages.status                                                                                   |
| modified_date             | modifiedDate            | date         | between    | modifiedDate                                                                                                     |
| organization_id           | organizationId          | custom       | any_of     | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| organization_id_not       | organizationIdNot       | custom       | not_any_of | organization.id.keyword, organization.identifier.keyword, organization.partOf.id, organization.partOf.identifier |
| owner                     | owner                   | acrossFields | any_of     | owner.firstName, owner.lastName, owner.username                                                                  |
| owner_not                 | ownerNot                | acrossFields | not_any_of | owner.firstName, owner.lastName, owner.username                                                                  |
| publication_id            | publicationId           | fuzzyKeyword | any_of     | publication.id, publication.identifier                                                                           |
| publication_id_not        | publicationIdNot        | fuzzyKeyword | not_any_of | publication.id, publication.identifier                                                                           |
| publication_type          | publicationType         | fuzzyKeyword | any_of     | publication.publicationInstance.type                                                                             |
| publication_type_not      | publicationTypeNot      | fuzzyKeyword | not_any_of | publication.publicationInstance.type                                                                             |
| publication_modified_date | publicationModifiedDate | date         | between    | publication.modifiedDate                                                                                         |
| publication_owner         | publicationOwner        | fuzzyKeyword | any_of     | publication.owner                                                                                                |
| publication_owner_not     | publicationOwnerNot     | fuzzyKeyword | not_any_of | publication.owner                                                                                                |
| publication_status        | publicationStatus       | keyword      | any_of     | publication.status.keyword                                                                                       |
| publication_status_not    | publicationStatusNot    | keyword      | not_any_of | publication.status.keyword                                                                                       |
| publication_title         | publicationTitle        | text         | all_of     | publication.mainTitle                                                                                            |
| status                    | status                  | keyword      | any_of     | status.keyword                                                                                                   |
| status_not                | statusNot               | keyword      | not_any_of | status.keyword                                                                                                   |
| type                      | type                    | keyword      | any_of     | type.keyword                                                                                                     |
| type_not                  | typeNot                 | keyword      | not_any_of | type.keyword                                                                                                     |
| viewed_by                 | viewedBy                | acrossFields | all_of     | viewedBy.firstName, viewedBy.lastName, viewedBy.username                                                         |
| viewed_by_not             | viewedByNot             | acrossFields | not_all_of | viewedBy.firstName, viewedBy.lastName, viewedBy.username                                                         |
| search_all                | searchAll               | freeText     | all_of     | q                                                                                                                |

> [!NOTE]
> <p>Valid SortKeys </p>
>
> ```
> category, instanceType, createdDate, modifiedDate, publishedDate, publicationDate, title, unitId, user
> ```
