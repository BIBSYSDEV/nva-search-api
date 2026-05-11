package no.unit.nva.search.common.bibtex;

import java.util.Comparator;
import java.util.regex.Pattern;

public record BibtexField(String key, String value) {

  public static final Comparator<BibtexField> BY_KEY = Comparator.comparing(BibtexField::key);

  private static final Pattern MATH_OR_ML =
      Pattern.compile(
          "\\\\\\(.*?\\\\\\)"
              + // \(...\) inline LaTeX
              "|\\\\\\[.*?\\\\]"
              + // \[...\] display LaTeX
              "|\\$\\$.*?\\$\\$"
              + // $$...$$ display LaTeX
              "|\\$[^$]+\\$"
              + // $...$ inline LaTeX
              "|<math[^>]*>.*?</math>"
              + // full MathML element
              "|<math[\\s>]", // dangling MathML opening tag
          Pattern.DOTALL);

  @Override
  public String toString() {
    return "  %s = {%s}".formatted(key, sanitize(value));
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BibtexField other && key.equals(other.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  static boolean containsLatexOrMathML(String s) {
    return MATH_OR_ML.matcher(s).find();
  }

  private static String sanitize(String text) {
    var matcher = MATH_OR_ML.matcher(text);
    var result = new StringBuilder();
    var cursor = 0;
    while (matcher.find()) {
      result.append(escape(text.substring(cursor, matcher.start())));
      result.append(matcher.group());
      cursor = matcher.end();
    }
    result.append(escape(text.substring(cursor)));
    return result.toString();
  }

  private static String escape(String text) {
    return text.replace("\\", "\\\\") // must be first!
        .replace("%", "\\%")
        .replace("#", "\\#")
        .replace("&", "\\&")
        .replace("{", "\\{")
        .replace("}", "\\}");
  }
}
