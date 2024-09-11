package no.unit.nva.search.resource;

import static no.unit.nva.constants.Defaults.objectMapperNoEmpty;
import static no.unit.nva.search.resource.ContributorNodeReducer.firstFewContributorsOrVerifiedOrNorwegian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;

class ContributorNodeReducerTest {
    @Test
    void shouldReduceContributorsToThree() throws JsonProcessingException {
        var source =
                """
{
  "entityDescription": {
    "mainTitle": "Investigation of the fine structure of European populations with applications to disease association studies",
    "language": "http://lexvo.org/id/iso639-3/eng",
    "contributors": [
      {
        "sequence": 1,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "NotVerified",
          "name": "SC Heath",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "type": "Contributor"
      },
      {
        "sequence": 2,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "NotVerified",
          "name": "IG Gut",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "type": "Contributor"
      },
      {
        "sequence": 3,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "NotVerified",
          "name": "P Brennan",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "type": "Contributor"
      },
      {
        "sequence": 4,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "Verified",
          "name": "Hans Einar Krokan",
          "id": "https://api.dev.nva.aws.unit.no/cristin/person/41890",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "affiliations": [
          {
            "countryCode": "NO",
            "id": "https://api.dev.nva.aws.unit.no/cristin/organization/194.65.15.0",
            "type": "Organization",
            "labels": {
              "nb": "Institutt for klinisk og molekylær medisin",
              "en": "Department of Clinical and Molecular Medicine"
            }
          }
        ],
        "type": "Contributor"
      },
      {
        "sequence": 5,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "NotVerified",
          "name": "Maiken Bratt Elvestad",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "affiliations": [
          {
            "countryCode": "NO",
            "id": "https://api.dev.nva.aws.unit.no/cristin/organization/194.65.15.0",
            "type": "Organization",
            "labels": {
              "nb": "Institutt for klinisk og molekylær medisin",
              "en": "Department of Clinical and Molecular Medicine"
            }
          },
          {
            "countryCode": "UK",
            "id": "https://api.dev.nva.aws.unit.no/cristin/organization/19.99.99.0",
            "type": "Organization"
          }
        ],
        "type": "Contributor"
      },
      {
        "sequence": 6,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": null,
          "name": "J Lissowska",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "type": "Contributor"
      },
      {
        "sequence": 7,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "name": "D Mates",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "type": "Contributor"
      },
      {
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "NotVerified",
          "name": "P Rudnai",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "type": "Contributor"
      },
      {
        "sequence": 9,
        "role": {
          "type": "Creator"
        },
        "identity": {
          "verificationStatus": "Verified",
          "name": "Frank Skorpen",
          "id": "https://api.dev.nva.aws.unit.no/cristin/person/42149",
          "type": "Identity"
        },
        "correspondingAuthor": false,
        "affiliations": [
          {
            "countryCode": "NO",
            "id": "https://api.dev.nva.aws.unit.no/cristin/organization/194.65.20.15",
            "type": "Organization",
            "labels": {
              "nb": "HUNT forskningssenter",
              "en": "HUNT Research Centre"
            }
          }
        ],
        "type": "Contributor"
      }
    ]
  }
}
""";
        var sourceNode = objectMapperNoEmpty.readTree(source);
        var transformed = firstFewContributorsOrVerifiedOrNorwegian().transform(sourceNode);
        var count = transformed.withArray("/entityDescription/contributors").size();

        assertThat(count, is(equalTo(6)));
    }
}
