package no.unit.nva.search.common.enums;

import static no.unit.nva.constants.Words.CHAR_UNDERSCORE;

import org.apache.commons.text.CaseUtils;

/**
 * Enum for defining the types of parameters that can be used in the search service.
 *
 * @author Stig Norland
 */
public enum ParameterKind {
  INVALID,
  /**
   * Ignored parameters are not processed by standard or custom handling. Normally used together
   * with other parameters in custom handlers or paging.
   */
  FUZZY_KEYWORD,
  KEYWORD,
  TEXT,
  FLAG,
  CUSTOM,
  NUMBER,
  DATE,
  DATE_TIME,
  ACROSS_FIELDS,
  BOOLEAN,
  EXISTS,
  FREE_TEXT,
  PART_OF,
  SORT_KEY;

  public String asCamelCase() {
    return CaseUtils.toCamelCase(this.name(), false, CHAR_UNDERSCORE);
  }
}
