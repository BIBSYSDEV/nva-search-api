package no.unit.nva.search.common.bibtex;

import java.util.Comparator;
import java.util.regex.Pattern;

public record BibtexField(String key, String value) {

  public static final Comparator<BibtexField> BY_KEY = Comparator.comparing(BibtexField::key);

  private static final Pattern MATH_OR_ML =
      Pattern.compile(
          "\\\\\\(.*?\\\\\\)"
              + // \(...\) inline LaTeX
              "|\\\\\\[.*?\\\\\\]"
              + // \[...\] display LaTeX
              "|\\$\\$.*?\\$\\$"
              + // $$...$$ display LaTeX
              "|\\$[^$]+\\$"
              + // $...$ inline LaTeX
              "|<math[\\s>]", // MathML
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

  private static String sanitize(String s) {
    if (containsLatexOrMathML(s)) {
      return s;
    }
    return s.replace("\\", "\\\\") // must be first!
        .replace("%", "\\%")
        .replace("#", "\\#")
        .replace("&", "\\&")
        .replace("{", "\\{")
        .replace("}", "\\}");
  }
}
