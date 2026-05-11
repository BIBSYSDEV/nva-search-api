package no.unit.nva.search.common.bibtex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BibtexFieldPropertyTest {

  private static final long SEED = 4242L;
  private static final int ITERATIONS = 500;
  private static final int MAX_PIECES = 5;
  private static final int MAX_TEXT_LENGTH = 12;
  private static final String FIELD_KEY = "title";
  private static final int VALUE_START = ("  " + FIELD_KEY + " = {").length();

  private static final Pattern MATH_SPAN =
      Pattern.compile(
          "\\\\\\(.*?\\\\\\)"
              + "|\\\\\\[.*?\\\\]"
              + "|\\$\\$.*?\\$\\$"
              + "|\\$[^$]+\\$"
              + "|<math[^>]*>.*?</math>",
          Pattern.DOTALL);

  private static final List<String> MATH_FRAGMENTS =
      List.of(
          "\\(x^2\\)",
          "\\(\\sqrt{s}\\)",
          "\\[a+b=c\\]",
          "$\\alpha$",
          "$$\\beta + \\gamma$$",
          "<math><msup><mi>E</mi></msup></math>");

  private static final List<Character> SPECIAL_CHARS = List.of('%', '#', '&');

  @Test
  void mathRegionsAppearVerbatimInOutput() {
    runProperty(
        (input, output) -> {
          var value = extractValue(output);
          var matcher = MATH_SPAN.matcher(input);
          while (matcher.find()) {
            var math = matcher.group();
            assertTrue(
                value.contains(math),
                "math region '%s' missing from output value '%s' (input: '%s')"
                    .formatted(math, value, input));
          }
        });
  }

  @Test
  void specialCharsOutsideMathAreEscaped() {
    runProperty(
        (input, output) -> {
          var inputOutsideMath = stripMathRegions(input);
          var valueOutsideMath = stripMathRegions(extractValue(output));
          for (var specialChar : SPECIAL_CHARS) {
            var inputCount = countChar(inputOutsideMath, specialChar);
            var escapedCount = countOccurrences(valueOutsideMath, "\\" + specialChar);
            assertEquals(
                inputCount,
                escapedCount,
                "expected %d escaped '\\%c' in output (input: '%s', output value: '%s')"
                    .formatted(inputCount, specialChar, input, extractValue(output)));
          }
        });
  }

  @Test
  void bracesOutsideMathAreEscaped() {
    runProperty(
        (input, output) -> {
          var inputOutsideMath = stripMathRegions(input);
          var valueOutsideMath = stripMathRegions(extractValue(output));
          assertEquals(
              countChar(inputOutsideMath, '{'),
              countOccurrences(valueOutsideMath, "\\{"),
              "open-brace escape count mismatch (input: '%s', output: '%s')"
                  .formatted(input, output));
          assertEquals(
              countChar(inputOutsideMath, '}'),
              countOccurrences(valueOutsideMath, "\\}"),
              "close-brace escape count mismatch (input: '%s', output: '%s')"
                  .formatted(input, output));
        });
  }

  @Test
  void structuralBracesBalanceAfterEscapesStripped() {
    runProperty(
        (input, output) -> {
          var stripped = output.replace("\\{", "").replace("\\}", "");
          var opens = stripped.chars().filter(character -> character == '{').count();
          var closes = stripped.chars().filter(character -> character == '}').count();
          assertEquals(
              opens,
              closes,
              "structural braces unbalanced in output '%s' (input: '%s')".formatted(output, input));
        });
  }

  @FunctionalInterface
  private interface PropertyCheck {
    void check(String input, String output);
  }

  private void runProperty(PropertyCheck check) {
    var random = new Random(SEED);
    IntStream.range(0, ITERATIONS)
        .forEach(
            iteration -> {
              var input = generateInput(random);
              var output = new BibtexField(FIELD_KEY, input).toString();
              check.check(input, output);
            });
  }

  private String generateInput(Random random) {
    var pieces = random.nextInt(MAX_PIECES) + 1;
    var builder = new StringBuilder();
    for (int piece = 0; piece < pieces; piece++) {
      if (random.nextBoolean()) {
        builder.append(randomText(random));
      } else {
        builder.append(MATH_FRAGMENTS.get(random.nextInt(MATH_FRAGMENTS.size())));
      }
    }
    return builder.toString();
  }

  private String randomText(Random random) {
    var length = random.nextInt(MAX_TEXT_LENGTH) + 1;
    var builder = new StringBuilder();
    for (int index = 0; index < length; index++) {
      var pick = random.nextInt(10);
      if (pick < 4) {
        builder.append((char) ('a' + random.nextInt(26)));
      } else if (pick == 4) {
        builder.append(SPECIAL_CHARS.get(random.nextInt(SPECIAL_CHARS.size())));
      } else if (pick == 5) {
        builder.append('{').append((char) ('a' + random.nextInt(26))).append('}');
      } else {
        builder.append(' ');
      }
    }
    return builder.toString();
  }

  private String extractValue(String output) {
    return output.substring(VALUE_START, output.length() - 1);
  }

  private String stripMathRegions(String text) {
    return MATH_SPAN.matcher(text).replaceAll("");
  }

  private long countChar(String text, char target) {
    return text.chars().filter(character -> character == target).count();
  }

  private long countOccurrences(String text, String needle) {
    long count = 0;
    int index = 0;
    while ((index = text.indexOf(needle, index)) != -1) {
      count++;
      index += needle.length();
    }
    return count;
  }
}
