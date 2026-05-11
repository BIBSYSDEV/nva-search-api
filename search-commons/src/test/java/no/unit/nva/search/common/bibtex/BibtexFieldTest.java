package no.unit.nva.search.common.bibtex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

  @Test
  void sanitizesPercentInPlainText() {
    assertEquals(
        "  note = {99\\% confidence}", new BibtexField("note", "99% confidence").toString());
  }

  @Test
  void sanitizesHashInPlainText() {
    assertEquals("  note = {issue \\#42}", new BibtexField("note", "issue #42").toString());
  }

  @Test
  void sanitizesAmpersandInPlainText() {
    assertEquals(
        "  note = {R\\&D department}", new BibtexField("note", "R&D department").toString());
  }

  @Test
  void doesNotSanitizeBackslashDelimitedLatex() {
    var value = "Collisions at \\(\\sqrt{s_{NN}}\\) = 2.76 TeV";
    assertEquals("  title = {%s}".formatted(value), new BibtexField("title", value).toString());
  }

  @Test
  void doesNotSanitizeDollarDelimitedLatex() {
    var value = "Searches for the SM Higgs boson at LEP with $\\sqrt{s}\\leq 202$ GeV";
    assertEquals("  title = {%s}".formatted(value), new BibtexField("title", value).toString());
  }

  @Test
  void doesNotSanitizeMathML() {
    var value = "Energy <math><msup><mi>E</mi><mn>2</mn></msup></math> formula";
    assertEquals("  title = {%s}".formatted(value), new BibtexField("title", value).toString());
  }

  @Test
  void sanitizesBackslashInPlainText() {
    var value = "100% {braces} #hashtag \\backslash";
    var expected = "  title = {100\\% \\{braces\\} \\#hashtag \\\\backslash}";
    assertEquals(expected, new BibtexField("title", value).toString());
  }
}
