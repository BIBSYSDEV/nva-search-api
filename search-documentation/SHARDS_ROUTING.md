## **Routing vs. Shard ID i OpenSearch**

Når du bruker foreldrebarn-relasjoner (parent-child relationships) i en OpenSearch-indeks, er det essensielt å forstå
forskjellen mellom **Routing** og **Shard ID**. Dette er spesielt viktig for å sikre korrekt datadistribusjon og optimal
ytelse.

### **Grunnleggende Begreper**

#### 1. **Shards (Fragmenter)**

- **Definisjon:** I OpenSearch (og Elasticsearch) er en indeks delt opp i mindre, håndterbare deler kalt *shards* eller
  fragmenter.
- **Antall Shards:** Når du oppretter en indeks, spesifiserer du antall primærshards. Dette tallet bestemmer hvor mye
  dataindeksen kan skaleres horisontalt.
- **Shard ID:** Hver shard tildeles en unik identifikator (*shard ID*) innen indeksen, vanligvis numerisk (f.eks. 0, 1,
  2, ...).

#### 2. **Routing**

- **Definisjon:** *Routing* bestemmer hvilken shard et bestemt dokument skal tilhøre innen en indeks. Det er en nøkkel
  eller verdi som brukes til å fordele dokumenter jevnt over shards.
- **Standard Routing:** Hvis ingen routing spesifiseres, bruker OpenSearch dokumentets unike ID som routing-verdi.
- **Egendefinert Routing:** Brukere kan spesifisere en egen routing-verdi for å kontrollere dokumentplasseringen, noe
  som er spesielt nyttig for relaterte dokumenter (som foreldrebarn-relasjoner).

---

## **Routing vs. Shard ID: Hvordan de Henger Sammen**

### **Shard ID er Indirekte Bestemt av Routing**

- **Indre Mekanisme:** Når du indekserer et dokument, tar OpenSearch routing-verdien (enten den spesifiserte eller
  dokumentets ID) og bruker en konsistent hash-funksjon for å beregne hvilken shard dokumentet skal plasseres i. Denne
  prosessen avgjør *shard ID* basert på routing-verdien.

  **Formel:**
  ```
  shard_id = hash(routing) % number_of_primary_shards
  ```

- **Implikasjon:** Brukeren spesifiserer **routing key**, ikke direkte *shard ID*. Shard ID bestemmes automatisk basert
  på routing key og antall shards i indeksen.

### **Eksempel:**

Anta at du har en indeks med 5 primærshards (Shard ID: 0 til 4).

1. **Ingen Spesifisert Routing:**
    - **Dokument-ID:** `doc123`
    - **Routing key:** `doc123` (standard)
    - **Shard ID:** `hash('doc123') % 5 = 2` → Dokumentet lagres i Shard 2.

2. **Spesifisert Routing:**
    - **Dokument-ID:** `child456`
    - **Routing key:** `parent789` (for å plassere barn i samme shard som forelder)
    - **Shard ID:** `hash('parent789') % 5 = 3` → Dokumentet lagres i Shard 3.

   > **Merk:** For å sikre at både forelderen (`parent789`) og barnet (`child456`) er i samme shard, bruker du
   `parent789` som routing key for begge dokumentene. Dermed får begge dokumentene samme Shard ID (3 i dette tilfellet).

---

## **Hvorfor Bruke Routing?**

### 1. **Foreldrebarn-relasjoner**

- **Formål:** Sikre at relaterte dokumenter (foreldre og barn) lagres i samme shard for effektiv spørring og
  relasjonsstyring.
- **Implementasjon:** Når du indekserer et barnedokument, spesifiserer du routing key basert på foreldredokumentets ID.

  ```http
  POST /min_indeks/_doc/barne_id?routing=foreldre_id
  {
    "join_field": {
      "name": "child",
      "parent": "foreldre_id"
    },
    "andre_felt": "verdi"
  }
  ```

### 2. **Optimalisering av Søkeytelse**

- **Begrens Søk til Relevante Shards:** Ved spesifisering av routing ved søk kan spørringen begrenses til relevante
  shards, noe som reduserer søketiden.

  ```http
  GET /min_indeks/_search?routing=spesifikk_routing_key
  {
    "query": {
      "match_all": {}
    }
  }
  ```

### 3. **Effektiv Dokumenthenting og Oppdatering**

- **Raskere Tilgang:** Ved å spesifisere routing når du henter eller oppdaterer et dokument, kan OpenSearch direkte slå
  opp i riktig shard uten å måtte gjennomgå alle shards.

  ```http
  GET /min_indeks/_doc/dokument_id?routing=spesifikk_routing_key
  ```

---

## **Detaljert Sammenligning**

| **Aspekt**            | **Routing**                                                                                        | **Shard ID**                                                                                           |
|-----------------------|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| **Definisjon**        | En nøkkel som bestemmer hvor et dokument plasseres innen en indeks.                                | Unik identifikator for en shard innen en indeks.                                                       |
| **Brukerinteraksjon** | Brukeren spesifiserer en routing key ved indeksering eller spørring.                               | Shard ID tildeles automatisk av OpenSearch basert på routing key og indekskonfigurasjon.               |
| **Spesifikasjon**     | Eksplisitt spesifisering av routing key nødvendig for kontrollert plassering av dokumenter.        | Shard ID bestemmes internt, og brukeren trenger vanligvis ikke å kjenne til det direkte.               |
| **Formål**            | Kontrollere dokumentfordeling, sikre relasjoner, optimalisere søkeytelse.                          | Administrere dataoppdeling og distribusjon over flere noder for skalerbarhet.                          |
| **Avhengighet**       | Routing key påvirker hvilken shard et dokument havner i.                                           | Avhenger av routing key og antall shards i indekskonfigurasjonen.                                      |
| **Bruk i API-kall**   | Routing spesifiseres som parameter (`routing=verdi`) i mange API-kall som GET, UPDATE, DELETE osv. | Brukeren refererer indirekte til shard ID ved å bruke routing key; shard ID spesifiseres ikke direkte. |

---

## **Praktisk Anvendelse og Beste Praksis**

### 1. **Konsistent Bruk av Routing Key**

For å opprettholde relasjoner og optimalisere ytelsen, sørg for at du konsekvent bruker samme routing key for relaterte
dokumenter. Dette sikrer at de plasseres i samme shard og kan hentes ut effektivt.

### 2. **Unngå Direkte Shard ID Spesifisering**

OpenSearch administrerer shards internt. Forsøk på å spesifisere shard ID direkte er ikke støttet og kan føre til
inkonsistenser. Bruk alltid routing key for å påvirke shard plasseringen.

### 3. **Tenk På Indekskonfigurasjon**

Antallet shards bør planlegges nøye basert på forventet datamengde og søke-/skrivemønstre. Et høyt antall shards kan
føre til overfordeling og økt administrasjonskompleksitet, mens for få shards kan begrense skalerbarheten.

### 4. **Monitorer Shard Fordeling**

Bruk overvåkingsverktøy for å sikre at dokumenter er jevnt fordelt over shards. Unormal routing kan føre til skjev
shard-fordeling, noe som påvirker ytelse og ressursbruk.

### 5. **Optimalisering av Bulk-operasjoner**

Når du utfører bulk-indeksering eller oppdatering, spesifiser routing for hvert dokument for å sikre at operasjonene
skjer på riktige shards og unngår konkurrerende låsing eller ressurskonflikter.

---

## **Eksempelscenario: Foreldrebarn-relasjoner**

La oss illustrere hvordan Routing og Shard ID fungerer sammen i en foreldrebarn-relasjon.

### **Opprettelse av Foreldredokument**

```http
POST /min_indeks/_doc/foreldre_1
{
  "navn": "Foreldrenavn",
  "type": "forelder"
}
```

- **Routing Key:** `foreldre_1` (standard, dokumentets ID)
- **Shard ID:** `hash('foreldre_1') % antall_shards`

### **Opprettelse av Barnedokument**

```http
POST /min_indeks/_doc/barn_1?routing=foreldre_1
{
  "navn": "Barnavn",
  "type": "barn",
  "forelder": "foreldre_1"
}
```

- **Routing Key:** `foreldre_1` (samme som forelderen)
- **Shard ID:** `hash('foreldre_1') % antall_shards` (samme som forelderen)

> **Resultat:** Både foreldre- og barnedokumentet er plassert i samme shard, noe som forenkler relasjonsstyring og øker
> søkeytelsen.

---

## **Oppsummering**

- **Routing** er en brukerspesifisert nøkkel som påvirker hvor et dokument plasseres i en indeks ved å bestemme hvilken
  *shard* det tilhører.
- **Shard ID** er en intern identifikator som OpenSearch tildeler til hver shard basert på routing key og indeksens
  shard-konfigurasjon.
- **Brukere spesifiserer ikke direkte Shard ID**, men manipulerer shard-plasseringen via routing key.
- **Korrekt bruk av routing** er essensielt for å sikre optimal dataorganisering, spesielt når man håndterer relaterte
  dokumenter som i foreldrebarn-relasjoner.
- **Forståelsen av Routing vs. Shard ID** hjelper med å designe mer effektive og skalerbare OpenSearch-løsninger.

---

## **Videre Ressurser**

- [OpenSearch Dokumentasjon om Routing](https://opensearch.org/docs/latest/index-management/routing/)
- [Sharding i OpenSearch](https://opensearch.org/docs/latest/index-management/sharding/)
- [Forstå Routing i Elasticsearch (lignende prinsipper i OpenSearch)](https://www.elastic.co/guide/en/elasticsearch/reference/current/routing.html)

---

Hvis du har flere spørsmål eller trenger ytterligere utdyping om spesifikke aspekter, er det bare å spørre!