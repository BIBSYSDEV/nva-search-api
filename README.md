# nva-search-api
A common search API for NVA across resources

**Parameters to GET /search/resources**

|Parameter|Description|Default value |
|-----|-----|-----|
| query | Term to search for in resource. | * 
| orderBy | Field to order result by. |  modifiedDate 
| sortOrder | Order of search results (**asc**ending or **desc**ending). | desc
| from | Start posision for results, 0-based. | 0
| results | Maximum number of publications in response. | 10


**Structure in response from the search API**


```JSON
{
  "@context": "https://api.nva.unit.no/resources/search",
  "took": 7,
  "total": 2,
  "hits": [
    {
      "publicationType": "JournalArticle",
      "id": "7565828d-1de6-490b-bcf2-00b3de3ab5b3",
      "contributors": [
        {
          "name": "Nogueira, Flavio S."
        },
        {
          "name": "van den Brink, Jeroen"
        },
        {
          "name": "Sudbø, Asle"
        }
      ],
      "title": "Conformality loss and quantum criticality in topological Higgs electrodynamics in 2+1 dimension",
      "owner": "tehe@unit.no",
      "publishedDate": {
        "type": "IndexDate",
        "year": "2020",
        "month": "9",
        "day": "25"
      },
      "publisher": {
        "id": "https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934",
        "name": "Organization"
      },
      "modifiedDate": "2020-11-05T15:40:35.646028Z"
    },
    {
      "publicationType": "JournalArticle",
      "id": "ff8f8307-2958-4bb2-bb6d-d9a701f67849",
      "contributors": [
        {
          "id": "1600776277420",
          "name": "Hellesvik, Terje"
        },
        {
          "id": "90806386",
          "name": "Hellesvik, Terje"
        },
        {
          "name": "Paskin, N."
        },
        {
          "id": "3089669",
          "name": "Garshol, Jan Erik"
        }
      ],
      "title": "Toward unique identifiers",
      "owner": "tehe@unit.no",
      "publishedDate": {
        "type": "IndexDate",
        "year": "1999"
      },
      "publisher": {
        "id": "https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934",
        "name": "Organization"
      },
      "modifiedDate": "2020-11-05T15:32:32.974093Z"
    }
  ]
 }
```
---
### Utility startpoints and flows ###
![](utilities_flow.png)
