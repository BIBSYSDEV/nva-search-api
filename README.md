# nva-search-api

Public search API for resources in NVA. The API is backed by Amazon OpenSearch.

## Query Parameters

### Passed to sws/opensearch

| key_name   | keyName   | queryKind         | scope                      |
|------------|-----------|-------------------|----------------------------|
| search_all | searchAll | text with ranking | all_items accross document |
| fields     | fields    | list of keys      | user, tags, title          |

### Pagination parameters

| key_name     | keyName     | queryKind                   | example         |
|--------------|-------------|-----------------------------|-----------------|
| aggregation  | aggregation | Enum                        | all, none       |
| page         | page        | number                      | 0 to 10000/size |
| from         | from        | number                      | 0 to 10000-size |
| size         | size        | number                      | 0 to 1000       |
| sort         | sort        | key1:asc/desc,key2:desc/asc |                 |
| sort_order   | sortOrder   | asc/desc                    |                 |
| search_after | searchAfter | sortindex                   | api only        |

## Resource

```http request
GET /search/resources HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json
```

Valid search parameters  
[/search/resources?parameters=...](search-commons/src/main/java/no/unit/nva/search2/resource.md)

## Resource (logged in user)

```http request
GET /search/customer/resources HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json
```

Valid search parameters  
[/search/customer/resources?parameters=...](search-commons/src/main/java/no/unit/nva/search2/resource.md)

**Structure in response from Response endpoints**

```JSON
{
  "id": "https://api.dev.nva.aws.unit.no/search/resources",
  "totalHits": 120387,
  "hits": [],
  "nextResults": "https://api.dev.nva.aws.unit.no/search/resources?aggregation=all&size=10&from=20",
  "previousResults": "https://api.dev.nva.aws.unit.no/search/resources?aggregation=all&size=10&from=0",
  "aggregations": {
    "type": [],
    "license": [],
    "contributor": [],
    "journal": [],
    "series": [],
    "contextType": [],
    "course": [],
    "publisher": [],
    "files": [],
    "fundingSource": [],
    "scientificIndex": [],
    "status": [],
    "topLevelOrganization": []
  },
  "@context": "https://bibsysdev.github.io/src/search/paginated-search-result.json"
}
```

## Ticket

```http request
GET /search/customer/tickets HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json
```

Valid search parameters  
[/search/customer/tickets?parameters=...](search-commons/src/main/java/no/unit/nva/search2/ticket.md)

**Structure in response from ticket endpoint**

```JSON
{
  "id": "https://api.dev.nva.aws.unit.no/search/customer/tickets",
  "totalHits": 120387,
  "hits": [],
  "nextResults": "https://api.dev.nva.aws.unit.no/search/customer/tickets?aggregation=all&size=10&from=20",
  "previousResults": "https://api.dev.nva.aws.unit.no/search/customer/tickets?aggregation=all&size=10&from=0",
  "aggregations": {
    "type": [],
    "notifications": [],
    "status": [],
    "byUserPending": []
  },
  "@context": "https://bibsysdev.github.io/src/search/paginated-search-result.json"
}
```

## Import-candidate

```http request
GET /search/import-candidates2 HTTP/1.1
Host: api.test.nva.aws.unit.no
Accept: application/json
```

Valid search parameters  
[/search/import-candidates2?parameters=...](search-commons/src/main/java/no/unit/nva/search2/importCandidate.md)

**Structure in response from import-candidate endpoint**

```JSON
{
  "id": "https://api.dev.nva.aws.unit.no/search/importcandidates2",
  "totalHits": 120387,
  "hits": [],
  "nextResults": "https://api.dev.nva.aws.unit.no/search/importcandidates2?aggregation=all&size=10&from=20",
  "previousResults": "https://api.dev.nva.aws.unit.no/search/importcandidates2?aggregation=all&size=10&from=0",
  "aggregations": {},
  "@context": "https://bibsysdev.github.io/src/search/paginated-search-result.json"
}
```

### All aggregations have the following format

```JSON
{
  "type": {
    "id": "https://unset/resource/search?type=RequestType&aggregation=all&size=10&from=0",
    "key": "RequestType",
    "count": 1,
    "labels": {
      "nb": "Første type",
      "en": "First kind"
    }
  }
}
```

**"id"** is the current query with the selected aggregation/filter added to it, useful for drill-downs into
sub-selections of the resource.    
If you want to filter on an aggregation without the current filter included, you can construct a search key by using the
aggregation name + key value .
(i.e. type=RequestType)
  
---

## Utility startpoints and flows ###
![](utilities_flow.png)
