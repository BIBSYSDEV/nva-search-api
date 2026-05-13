package no.unit.nva.search.common.bibtex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class BibtexFieldTest {

  // --- LinkedHashSet deduplication ---

  @Test
  void shouldIgnoreDuplicateKeys() {
    var set = new LinkedHashSet<BibtexField>();
    set.add(new BibtexField("key", "value1"));
    set.add(new BibtexField("key", "value2"));
    assertEquals(1, set.size());
  }

  // --- equals and hashCode ---

  @Test
  void equalityBasedOnKeyOnly() {
    assertEquals(new BibtexField("author", "Alice"), new BibtexField("author", "Bob"));
  }

  @Test
  void hashCodeBasedOnKeyOnly() {
    assertEquals(
        new BibtexField("author", "Alice").hashCode(), new BibtexField("author", "Bob").hashCode());
  }

  @Test
  void differentKeysAreNotEqual() {
    assertNotEquals(new BibtexField("author", "Alice"), new BibtexField("title", "Alice"));
  }

  // --- containsLatexOrMathML ---

  @ParameterizedTest(name = "detects LaTeX/MathML in: {0}")
  @ValueSource(
      strings = {
        // \(...\) style — most common in the dataset
        "Measurement of electrons from beauty-hadron decays in p-Pb collisions at \\(\\sqrt{s_{\\r"
            + "m NN}}=5.02\\) TeV",
        "D-meson production in \\(p\\)–Pb collisions at \\(\\sqrt{s_{\\rm NN}}=5.02\\) TeV",
        "\\(\\pi^0\\) and \\(\\eta\\) meson production in proton-proton collisions at"
            + " \\(\\sqrt{s}=8\\) TeV",
        "J/\\(psi\\) Elliptic Flow in Pb-Pb Collisions at \\(\\sqrt{s_{NN}}\\)=2.76 TeV",
        "W±-boson production in p–Pb collisions at \\( \\sqrt{s_{\\textrm{NN}}} \\) = 8.16 TeV",
        "First measurement of \\(\\Xi_{\\r"
            + "m c}^0\\) production in pp collisions at \\(\\mathbf{\\sqrt{s}}\\) = 7 TeV",
        "Measurement of jet quenching at \\({\\sqrt{\\bf{s}_{\\mathrm {\\bf{NN}}}}}\\) = 2.76 TeV",
        // $...$ style
        "Searches for the SM Higgs boson at LEP with $\\sqrt{s}\\leq 202$ GeV",
        "Searches for the SM Higgs boson at LEP with $\\sqrt{s}\\leq 189$ GeV",
        // MathML
        "Energy formula: <math"
            + " xmlns='http://www.w3.org/1998/Math/MathML'><msup><mi>E</mi></msup></math>",
        "Some title with <math> inside"
      })
  void detectsLatexOrMathML(String title) {
    assertTrue(BibtexField.containsLatexOrMathML(title));
  }

  @ParameterizedTest(name = "no LaTeX/MathML in: {0}")
  @ValueSource(
      strings = {
        // Plain-text sqrt notation — not LaTeX
        "Mid-Rapidity Neutral Pion Production at sqrt(s_NN) = 200 GeV",
        "Elliptic Flow of Identified Hadrons in Au+Au Collisions at sqrt(s_NN) = 200 GeV",
        "Scaling properties of proton production in sqrt(s_NN) = 200 GeV Au + Au collisions",
        "Reduction of (sqrt(5) x sqrt(5)R27-O surface oxide on Pd(100) and Pd75Ag25(100) with CO",
        // Completely plain titles
        "MAX-lab Activity Report 2011",
        "A study of particle production in heavy-ion collisions"
      })
  void doesNotDetectLatexOrMathMLInPlainText(String title) {
    assertFalse(BibtexField.containsLatexOrMathML(title));
  }

  // --- sanitize via toString ---

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          99% confidence | 99\\% confidence
          issue #42      | issue \\#42
          R&D department | R\\&D department
          """)
  void sanitizesSpecialCharInPlainText(String input, String expected) {
    assertEquals("  note = {%s}".formatted(expected), new BibtexField("note", input).toString());
  }

  @ParameterizedTest(name = "preserves {0}")
  @MethodSource("mathContentExamples")
  void preservesMathContentVerbatim(String value) {
    assertEquals("  title = {%s}".formatted(value), new BibtexField("title", value).toString());
  }

  static Stream<Arguments> mathContentExamples() {
    return Stream.of(
        Arguments.of(
            Named.of("\\(...\\) inline LaTeX", "Collisions at \\(\\sqrt{s_{NN}}\\) = 2.76 TeV")),
        Arguments.of(
            Named.of(
                "$...$ inline LaTeX",
                "Searches for the SM Higgs boson at LEP with $\\sqrt{s}\\leq 202$ GeV")),
        Arguments.of(
            Named.of(
                "<math> element",
                "Energy <math><msup><mi>E</mi><mn>2</mn></msup></math> formula")));
  }

  @Test
  void sanitizesBackslashInPlainText() {
    var value = "100% {braces} #hashtag \\backslash";
    var expected = "  title = {100\\% \\{braces\\} \\#hashtag \\\\backslash}";
    assertEquals(expected, new BibtexField("title", value).toString());
  }

  @Test
  void shouldProduceBalancedBracesWhenValueContainsLatexAndStrayBrace() {
    var value = "Unmatched {brace in \\(x^2\\)";
    var output = new BibtexField("title", value).toString();
    var stripped = output.replace("\\{", "").replace("\\}", "");
    var opens = stripped.chars().filter(character -> character == '{').count();
    var closes = stripped.chars().filter(character -> character == '}').count();
    assertEquals(opens, closes, "unbalanced braces will break BibTeX parsing of the entry");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("specialCharsInMathValues")
  void shouldEscapeSpecialCharEvenWhenValueContainsMath(String value, String specialChar) {
    var output = new BibtexField("title", value).toString();
    assertFalse(
        output.replace("\\" + specialChar, "").contains(specialChar),
        "expected '%s' to be escaped in: %s".formatted(specialChar, value));
  }

  static Stream<Arguments> specialCharsInMathValues() {
    return Stream.of(
        Arguments.of(
            Named.of(
                "unescaped percent is treated as a line comment when LaTeX renders the"
                    + " bibliography",
                "100% off in \\(x^2\\)"),
            "%"),
        Arguments.of(
            Named.of(
                "unescaped ampersand triggers misplaced alignment tab in LaTeX rendering",
                "R&D notes for \\(\\alpha\\)"),
            "&"),
        Arguments.of(
            Named.of(
                "unescaped hash is BibTeX's string concatenation operator and breaks downstream"
                    + " tooling",
                "Section #3 of \\(\\sqrt{s}\\)"),
            "#"),
        Arguments.of(
            Named.of(
                "percent outside MathML element must still be escaped",
                "100% off <math><mi>x</mi></math>"),
            "%"),
        Arguments.of(
            Named.of(
                "ampersand outside MathML element must still be escaped",
                "R&D notes <math><msup><mi>E</mi><mn>2</mn></msup></math>"),
            "&"),
        Arguments.of(
            Named.of(
                "hash outside MathML element with xmlns attribute must still be escaped",
                "Section #3 <math xmlns='http://www.w3.org/1998/Math/MathML'><mi>y</mi></math>"),
            "#"));
  }

  @Test
  void shouldEscapeStrayBraceWhenValueContainsMathML() {
    var value = "Unmatched {brace next to <math><mi>x</mi></math>";
    var output = new BibtexField("title", value).toString();
    var stripped = output.replace("\\{", "").replace("\\}", "");
    var opens = stripped.chars().filter(character -> character == '{').count();
    var closes = stripped.chars().filter(character -> character == '}').count();
    assertEquals(opens, closes, "unbalanced braces will break BibTeX parsing of the entry");
  }

  @Test
  void preservesMathMLElementVerbatimWhenSurroundedByEscapableText() {
    var mathMl = "<math><msup><mi>E</mi><mn>2</mn></msup></math>";
    var value = "100% off & #1 special " + mathMl + " trailing & text";
    var output = new BibtexField("title", value).toString();
    assertTrue(
        output.contains(mathMl),
        "MathML element must be preserved verbatim, got: %s".formatted(output));
  }

  @Test
  void preservesMathMLElementVerbatimWhenAttributeContainsEscapableChar() {
    var mathMl = "<math style=\"font-size:120%\"><mi>x</mi></math>";
    var output = new BibtexField("title", mathMl).toString();
    assertTrue(
        output.contains(mathMl),
        "MathML element with %% in attribute must be preserved verbatim, got: %s"
            .formatted(output));
  }
}
