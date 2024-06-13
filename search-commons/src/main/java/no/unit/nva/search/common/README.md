# /common

## /builder

Opensearch query builders

### ParameterValidator

This class will based on a definition (Enum), validate that all search keys are valid including their values.
It will also merge any duplicate keys

    key1=apples&key1=oranges -> key1=apples,oranges

a validated key set can build and return a Query

### Query

This class will go through every key provided and build OpensearchQuery for each of them.

- It will then compile a single Query,
- pass it on to a client
- fetch the result from the client
- format the result
- and forward the formated result back to the requester.

### OpensearchClient

input -> POST request
output -> Http response body
This class handles the HTTP request to OpenSearch, including 


