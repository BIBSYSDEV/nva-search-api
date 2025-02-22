package no.unit.nva.search.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.PLUS;
import static no.unit.nva.constants.Words.SPACE;
import static no.unit.nva.search.common.constant.Functions.decodeUTF;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.ValueEncoding;

/**
 * This class operates on the queryKeys that a request provides.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public class QueryKeys<K extends Enum<K> & ParameterKey<K>> {

  protected final transient Set<K> otherRequired;
  private final transient Map<K, String> page;
  private final transient Map<K, String> search;
  private final K fields;

  public QueryKeys(K fields) {
    this.fields = fields;
    search = new ConcurrentHashMap<>();
    page = new ConcurrentHashMap<>();
    otherRequired = new HashSet<>();
  }

  public Stream<K> getSearchKeys() {
    return search.keySet().stream();
  }

  public Set<Map.Entry<K, String>> getSearchEntries() {
    return search.entrySet();
  }

  public Set<Map.Entry<K, String>> getPageEntries() {
    return page.entrySet();
  }

  /**
   * Query Parameters with string Keys.
   *
   * @return Map of String and String
   */
  public Map<String, String> asMap() {
    var results = new LinkedHashMap<String, String>();
    Stream.of(search.entrySet(), page.entrySet())
        .flatMap(Set::stream)
        .sorted(Comparator.comparingInt(o -> o.getKey().ordinal()))
        .forEach(entry -> results.put(toApiKey(entry), toApiValue(entry)));
    return results;
  }

  private String toApiKey(Map.Entry<K, String> entry) {
    return entry.getKey().asCamelCase();
  }

  private String toApiValue(Map.Entry<K, String> entry) {
    return entry.getValue().replace(SPACE, PLUS);
  }

  /**
   * Get value from Query Parameter Map with key.
   *
   * @param key to look up.
   * @return String content raw
   */
  public AsType<K> get(K key) {
    return new AsType<>(search.containsKey(key) ? search.get(key) : page.get(key), key);
  }

  /**
   * Add a key value pair to Parameters.
   *
   * @param key to add to.
   * @param value to assign
   */
  public void set(K key, String value) {
    if (nonNull(value)) {
      var decodedValue = key.valueEncoding() == ValueEncoding.NONE ? value : decodeUTF(value);
      if (isPagingValue(key)) {
        page.put(key, decodedValue);
      } else {
        search.put(key, decodedValue);
      }
    }
  }

  private boolean isPagingValue(K key) {
    return key.ordinal() >= fields.ordinal();
  }

  public AsType<K> remove(K key) {
    return new AsType<>(search.containsKey(key) ? search.remove(key) : page.remove(key), key);
  }

  public boolean isPresent(K key) {
    return search.containsKey(key) || page.containsKey(key);
  }

  public AsType<K> ifPresent(K key) {
    return isPresent(key) ? get(key) : null;
  }
}
