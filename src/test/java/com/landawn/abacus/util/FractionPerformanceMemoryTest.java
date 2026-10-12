package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Fraction. */
@Tag("slow-test")
public class FractionPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G027 end ----
    // ---- perf review 2026-10-08 G030 begin ----
    @Nested
    class G030 {

        private String parsed(final String text) {
            try {
                final Fraction fraction = Fraction.of(text);
                return fraction.numerator() + "/" + fraction.denominator();
            } catch (final RuntimeException e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        private String terms(final java.util.function.Supplier<Fraction> operation) {
            try {
                final Fraction fraction = operation.get();
                return fraction.numerator() + "/" + fraction.denominator();
            } catch (final ArithmeticException e) {
                return "AE: " + e.getMessage();
            }
        }

        // Reference for every reducing factory/operation: exact reduction, sign moved onto the numerator, int range check.
        private String reducedReference(java.math.BigInteger numerator, java.math.BigInteger denominator) {
            if (denominator.signum() == 0) {
                return "AE: The denominator must not be zero";
            }
            if (numerator.signum() == 0) {
                return "0/1";
            }
            final java.math.BigInteger gcd = numerator.gcd(denominator);
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
            if (denominator.signum() < 0) {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }
            if (numerator.compareTo(java.math.BigInteger.valueOf(Integer.MIN_VALUE)) < 0
                    || numerator.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                    || denominator.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                return "AE: Fraction terms exceed the int range: " + numerator + "/" + denominator;
            }
            return numerator + "/" + denominator;
        }

        private java.math.BigInteger big(final long value) {
            return java.math.BigInteger.valueOf(value);
        }

        private int randomTerm(final Random random) {
            switch (random.nextInt(6)) {
                case 0:
                    return random.nextInt(41) - 20;
                case 1:
                    return random.nextInt(20001) - 10000;
                case 2:
                    return random.nextInt();
                case 3:
                    return (random.nextBoolean() ? 1 : -1) << random.nextInt(32);
                case 4:
                    return (random.nextInt(9) - 4) * (1 << random.nextInt(28));
                default:
                    return random.nextBoolean() ? Integer.MIN_VALUE + random.nextInt(3) : Integer.MAX_VALUE - random.nextInt(3);
            }
        }

        // G030-01: the "Y/Z" and "X Y/Z" forms parse their integer components in place (whole-number and decimal forms unchanged).
        @Test
        void testOfString_fractionFormsParsedInPlace() {
            assertEquals("3/4", parsed("3/4"));
            assertEquals("-7/8", parsed("-7/8"));
            assertEquals("2/4", parsed("2/4"));
            assertEquals("3/4", parsed("+3/+4"));
            assertEquals("-3/4", parsed("3/-4"));
            assertEquals("3/4", parsed("-3/-4"));
            assertEquals("12/4", parsed("0012/0004"));
            assertEquals("-2147483648/2147483647", parsed("-2147483648/2147483647"));
            assertEquals("2147483647/1", parsed("2147483647/1"));
            assertEquals("3/4", parsed("\u0663/\u0664"));
            assertEquals("7/4", parsed("1 3/4"));
            assertEquals("-7/3", parsed("-2 1/3"));
            assertEquals("6/4", parsed("1 2/4"));
            assertEquals("-6/4", parsed("-1 2/4"));
            assertEquals("15/2", parsed("7 1/2"));
            assertEquals("15/2", parsed("+7 1/2"));
            assertEquals("-1/2", parsed("-0 1/2"));
            assertEquals("-2/4", parsed("-0 2/4"));
            assertEquals("1/2", parsed("+0 1/2"));
            assertEquals("1/2", parsed("0 1/2"));
            assertEquals("0/2", parsed("-0 0/2"));
            assertEquals("0/1", parsed("0 0/1"));
            assertEquals("3/2", parsed("\u0661 \u0661/\u0662"));
            assertEquals("-2147483648/1", parsed("-2147483648 0/1"));
            assertEquals("5/1", parsed("5"));
            assertEquals("-12/1", parsed("-12"));
            assertEquals("7/1", parsed("+7"));
            assertEquals("0/1", parsed("-0"));
            assertEquals("-2147483648/1", parsed("-2147483648"));
            assertEquals("1/2", parsed("0.5"));
            assertEquals("1/4", parsed("0.25"));
            // the decimal form keeps Double.parseDouble's leniency
            assertEquals("1/2", parsed(" 0.5"));
            assertEquals("1/2", parsed("0.5d"));

            final Fraction unreduced = Fraction.of("-0 2/4");
            assertEquals(-2, unreduced.numerator());
            assertEquals(4, unreduced.denominator());
            assertNotSame(Fraction.ZERO, Fraction.of("0/1"));
            assertEquals(Fraction.ZERO, Fraction.of("0/1"));
        }

        // G030-01: a malformed component is still named exactly in the message, and the failure order is unchanged.
        @Test
        void testOfString_malformedComponentsKeepTheirMessages() {
            assertEquals("NumberFormatException: The fraction \"\" could not be parsed: \"\" is not an integer", parsed(""));
            assertEquals("NumberFormatException: The fraction \" 3\" could not be parsed: \" 3\" is not an integer", parsed(" 3"));
            assertEquals("NumberFormatException: The fraction \" \" could not be parsed: \" \" is not an integer", parsed(" "));
            assertEquals("NumberFormatException: The fraction \"x\" could not be parsed: \"x\" is not an integer", parsed("x"));
            assertEquals("NumberFormatException: The fraction \"-\" could not be parsed: \"-\" is not an integer", parsed("-"));
            assertEquals("NumberFormatException: The fraction \"1/\" could not be parsed: \"\" is not an integer", parsed("1/"));
            assertEquals("NumberFormatException: The fraction \"/1\" could not be parsed: \"\" is not an integer", parsed("/1"));
            assertEquals("NumberFormatException: The fraction \"/\" could not be parsed: \"\" is not an integer", parsed("/"));
            assertEquals("NumberFormatException: The fraction \"-/2\" could not be parsed: \"-\" is not an integer", parsed("-/2"));
            assertEquals("NumberFormatException: The fraction \"1/+\" could not be parsed: \"+\" is not an integer", parsed("1/+"));
            assertEquals("NumberFormatException: The fraction \"1/2/3\" could not be parsed: \"2/3\" is not an integer", parsed("1/2/3"));
            assertEquals("NumberFormatException: The fraction \"2147483648/1\" could not be parsed: \"2147483648\" is not an integer", parsed("2147483648/1"));
            assertEquals("NumberFormatException: The fraction \"1/-2147483649\" could not be parsed: \"-2147483649\" is not an integer",
                    parsed("1/-2147483649"));
            assertEquals("NumberFormatException: The fraction \"1.0/2.0\" could not be parsed: \"1.0\" is not an integer", parsed("1.0/2.0"));
            assertEquals("NumberFormatException: The fraction \"3/4.\" could not be parsed: \"4.\" is not an integer", parsed("3/4."));
            assertEquals("NumberFormatException: The fraction \"1\t1/2\" could not be parsed: \"1\t1\" is not an integer", parsed("1\t1/2"));
            assertEquals("NumberFormatException: The fraction \"1/2x\" could not be parsed: \"2x\" is not an integer", parsed("1/2x"));
            // "X Y/Z": whole part first, then the '/' check, then numerator, then denominator
            assertEquals("NumberFormatException: The fraction \"1/2 3/4\" could not be parsed: \"1/2\" is not an integer", parsed("1/2 3/4"));
            assertEquals("NumberFormatException: The fraction \"b 3/x\" could not be parsed: \"b\" is not an integer", parsed("b 3/x"));
            assertEquals("NumberFormatException: The fraction \"2 3\" could not be parsed as the format X Y/Z", parsed("2 3"));
            assertEquals("NumberFormatException: The fraction \"2 \" could not be parsed as the format X Y/Z", parsed("2 "));
            assertEquals("NumberFormatException: The fraction \"1 /2\" could not be parsed: \"\" is not an integer", parsed("1 /2"));
            assertEquals("NumberFormatException: The fraction \"1 1/\" could not be parsed: \"\" is not an integer", parsed("1 1/"));
            assertEquals("NumberFormatException: The fraction \"1 2 3/4\" could not be parsed: \"2 3\" is not an integer", parsed("1 2 3/4"));
            assertEquals("NumberFormatException: The fraction \"1  1/2\" could not be parsed: \" 1\" is not an integer", parsed("1  1/2"));
            assertEquals("NumberFormatException: The fraction \"1 1/2/3\" could not be parsed: \"2/3\" is not an integer", parsed("1 1/2/3"));
            assertEquals("NumberFormatException: The fraction \"1 x/0\" could not be parsed: \"x\" is not an integer", parsed("1 x/0"));
            assertEquals("NumberFormatException: The fraction \"1 1.5/2\" could not be parsed: \"1.5\" is not an integer", parsed("1 1.5/2"));
            assertEquals("NumberFormatException: The fraction \"1 1/2 \" could not be parsed: \"2 \" is not an integer", parsed("1 1/2 "));
            assertEquals("NumberFormatException: The fraction \"1 1/99999999999\" could not be parsed: \"99999999999\" is not an integer",
                    parsed("1 1/99999999999"));
            // component values that parse but are rejected by the factories
            assertEquals("ArithmeticException: The denominator must not be zero", parsed("1/0"));
            assertEquals("ArithmeticException: The denominator must not be zero", parsed("1 1/0"));
            assertEquals("ArithmeticException: The numerator must not be negative", parsed("1 -1/2"));
            assertEquals("ArithmeticException: The denominator must not be negative", parsed("1 1/-2"));
            assertEquals("ArithmeticException: Fraction terms exceed the int range: 2147483648/1", parsed("2147483647 1/1"));
            assertEquals("ArithmeticException: Fraction terms exceed the int range: 4294967294/2147483647", parsed("1 2147483647/2147483647"));
            assertEquals("ArithmeticException: Fraction terms exceed the int range: 2147483648/1", parsed("-2147483648/-1"));
            assertEquals("IllegalArgumentException: The string must not be null", parsed(null));
        }

        // G030-01: random tokens (digits, signs, spaces, '/', a letter, a non-ASCII digit) give the same result or the same
        // exception as an independent reading of the documented grammar.
        @Test
        void testOfString_randomTokensMatchComponentwiseReading() {
            final char[] alphabet = { '0', '1', '2', '3', '7', '9', '-', '+', ' ', '/', '/', 'x', '\u0663' };
            final Random random = new Random(20261008L);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20000; i++) {
                sb.setLength(0);
                final int length = random.nextInt(10);
                for (int j = 0; j < length; j++) {
                    sb.append(alphabet[random.nextInt(alphabet.length)]);
                }
                final String text = sb.toString();
                assertEquals(expectedParse(text), parsed(text), text);
            }
        }

        // Independent model of Fraction.of(String) for tokens without '.': component parsing via substrings.
        private String expectedParse(final String text) {
            final int slash = text.indexOf('/');
            final int space = text.indexOf(' ');
            try {
                if (space > 0) {
                    final int whole = component(text, text.substring(0, space));
                    final String rest = text.substring(space + 1);
                    final int restSlash = rest.indexOf('/');
                    if (restSlash < 0) {
                        return "NumberFormatException: The fraction \"" + text + "\" could not be parsed as the format X Y/Z";
                    }
                    final int numerator = component(text, rest.substring(0, restSlash));
                    final int denominator = component(text, rest.substring(restSlash + 1));
                    final Fraction mixed = Fraction.ofMixed(whole, numerator, denominator);
                    if (whole == 0 && text.charAt(0) == '-') {
                        return Fraction.of(-mixed.numerator(), mixed.denominator()).numerator() + "/" + mixed.denominator();
                    }
                    return mixed.numerator() + "/" + mixed.denominator();
                }
                if (slash < 0) {
                    return component(text, text) + "/1";
                }
                final Fraction simple = Fraction.of(component(text, text.substring(0, slash)), component(text, text.substring(slash + 1)));
                return simple.numerator() + "/" + simple.denominator();
            } catch (final RuntimeException e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        private int component(final String text, final String part) {
            try {
                return Integer.parseInt(part);
            } catch (final NumberFormatException e) {
                throw new NumberFormatException("The fraction \"" + text + "\" could not be parsed: \"" + part + "\" is not an integer");
            }
        }

        // G030-02: reducing factories and operations skip the division by a gcd of 1; pin coprime and non-coprime terms,
        // sign normalisation, the ZERO constant and the int-range failure message.
        @Test
        void testOfReduce_coprimeAndCommonFactorTerms() {
            assertEquals("3/4", terms(() -> Fraction.of(3, 4, true)));
            assertEquals("3/4", terms(() -> Fraction.of(6, 8, true)));
            assertEquals("-3/4", terms(() -> Fraction.of(3, -4, true)));
            assertEquals("-3/4", terms(() -> Fraction.of(-6, 8, true)));
            assertEquals("3/4", terms(() -> Fraction.of(-6, -8, true)));
            assertEquals("1/1", terms(() -> Fraction.of(7, 7, true)));
            assertEquals("-1/1", terms(() -> Fraction.of(7, -7, true)));
            assertEquals("1/1", terms(() -> Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE, true)));
            assertEquals("-1/1073741824", terms(() -> Fraction.of(2, Integer.MIN_VALUE, true)));
            assertEquals("AE: Fraction terms exceed the int range: 1/2147483648", terms(() -> Fraction.of(-1, Integer.MIN_VALUE, true)));
            assertEquals("AE: Fraction terms exceed the int range: -3/2147483648", terms(() -> Fraction.of(3, Integer.MIN_VALUE, true)));
            assertEquals("AE: Fraction terms exceed the int range: 2147483648/1", terms(() -> Fraction.of(Integer.MIN_VALUE, -1, true)));
            assertEquals("-2147483647/2147483646", terms(() -> Fraction.of(Integer.MAX_VALUE, -(Integer.MAX_VALUE - 1), true)));
            assertEquals("AE: The denominator must not be zero", terms(() -> Fraction.of(1, 0, true)));
            assertSame(Fraction.ZERO, Fraction.of(0, 7, true));
            assertSame(Fraction.ZERO, Fraction.of(0, -7, true));
            assertEquals("7/4", terms(() -> Fraction.ofMixed(1, 3, 4, true)));
            assertEquals("3/2", terms(() -> Fraction.ofMixed(1, 2, 4, true)));
            assertEquals("-3/2", terms(() -> Fraction.ofMixed(-1, 2, 4, true)));

            final Fraction half = Fraction.of(1, 2);
            final Fraction third = Fraction.of(1, 3);
            assertEquals("5/6", terms(() -> half.add(third)));
            assertEquals("1/1", terms(() -> half.add(half)));
            assertEquals("1/6", terms(() -> half.subtract(third)));
            assertEquals("0/1", terms(() -> half.subtract(half)));
            assertSame(Fraction.ZERO, half.subtract(half));
            assertEquals("1/6", terms(() -> half.multipliedBy(third)));
            assertEquals("1/2", terms(() -> Fraction.of(2, 3).multipliedBy(Fraction.of(3, 4))));
            assertEquals("3/2", terms(() -> half.dividedBy(third)));
            assertEquals("1/1", terms(() -> half.dividedBy(Fraction.of(2, 4))));
            assertEquals("-1/2", terms(() -> half.negate()));
            assertEquals("-1/2", terms(() -> Fraction.of(2, 4).negate()));
            assertEquals("2/1", terms(() -> half.invert()));
            assertEquals("-2/1", terms(() -> Fraction.of(-2, 4).invert()));
            assertEquals("AE: Fraction terms exceed the int range: 2147483648/1", terms(() -> Fraction.of(Integer.MIN_VALUE, 1).negate()));
            assertEquals("AE: Fraction terms exceed the int range: 2147483648/1", terms(() -> Fraction.of(Integer.MIN_VALUE, 1).abs()));
            // a coprime (gcd 1) sum whose terms do not fit in int
            assertEquals("AE: Fraction terms exceed the int range: 4294967293/4611686011984936962",
                    terms(() -> Fraction.of(1, Integer.MAX_VALUE).add(Fraction.of(1, Integer.MAX_VALUE - 1))));
            assertEquals("32/243", terms(() -> Fraction.of(2, 3).pow(5)));
            assertEquals("-2147483648/1", terms(() -> Fraction.of(-2, 1).pow(31)));
            assertEquals("243/32", terms(() -> Fraction.of(4, 6).pow(-5)));
        }

        // G030-02: every reducing operation agrees with an exact BigInteger reference on random terms (incl. the
        // int extremes, powers of two and small values, so both gcd == 1 and gcd > 1 occur often).
        @Test
        void testArithmetic_randomTermsMatchExactReference() {
            final Random random = new Random(8L);
            int coprime = 0;
            int common = 0;
            for (int i = 0; i < 20000; i++) {
                final int n1 = randomTerm(random);
                final int rawD1 = randomTerm(random);
                final int n2 = randomTerm(random);
                final int rawD2 = randomTerm(random);
                final int d1 = rawD1 == 0 ? 1 : rawD1;
                final int d2 = rawD2 == 0 ? 3 : rawD2;

                final String reduced = reducedReference(big(n1), big(d1));
                assertEquals(reduced, terms(() -> Fraction.of(n1, d1, true)), n1 + "/" + d1);
                if (big(n1).gcd(big(d1)).equals(java.math.BigInteger.ONE)) {
                    coprime++;
                } else {
                    common++;
                }

                final Fraction a;
                final Fraction b;
                try {
                    // the non-reducing factory rejects a negative denominator whose sign cannot move (MIN_VALUE terms)
                    a = Fraction.of(n1, d1);
                    b = Fraction.of(n2, d2);
                } catch (final ArithmeticException e) {
                    continue;
                }
                final long an = a.numerator(), ad = a.denominator(), bn = b.numerator(), bd = b.denominator();
                assertEquals(reducedReference(big(an).multiply(big(bd)).add(big(bn).multiply(big(ad))), big(ad).multiply(big(bd))), terms(() -> a.add(b)),
                        a + " + " + b);
                assertEquals(reducedReference(big(an).multiply(big(bd)).subtract(big(bn).multiply(big(ad))), big(ad).multiply(big(bd))),
                        terms(() -> a.subtract(b)), a + " - " + b);
                assertEquals(reducedReference(big(an).multiply(big(bn)), big(ad).multiply(big(bd))), terms(() -> a.multipliedBy(b)), a + " * " + b);
                if (bn != 0) {
                    assertEquals(reducedReference(big(an).multiply(big(bd)), big(ad).multiply(big(bn))), terms(() -> a.dividedBy(b)), a + " / " + b);
                    assertEquals(reducedReference(big(bd), big(bn)), terms(b::invert), "invert " + b);
                }
                assertEquals(reducedReference(big(-an), big(ad)), terms(a::negate), "negate " + a);
            }
            assertTrue(coprime > 1000, "coprime=" + coprime);
            assertTrue(common > 1000, "common=" + common);
        }
    }
}
