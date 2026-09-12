/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import static java.lang.Double.MIN_EXPONENT;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Math.abs;
import static java.lang.Math.getExponent;
import static java.lang.Math.min;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.type.Type;

/**
 * Static utilities for numeric conversion, parsing, formatting, rounding, comparison, integer arithmetic,
 * combinatorics, statistics, and selected mathematical functions.
 *
 * <p><b>Contracts are method-specific.</b> In particular, similarly named parsing and conversion methods do not
 * necessarily accept the same grammar or apply the same null, overflow, rounding, or floating-point policy.
 * Consult the selected overload rather than assuming a class-wide default.</p>
 *
 * <p><b>Contract overview:</b></p>
 * <ul>
 *   <li><b>Nulls and empty strings:</b> many {@code to*} converters return a primitive default, {@code decode*}/{@code parse*}/
 *       {@link #createNumber(String)} generally return {@code null} for {@code null} or exactly {@code ""}, {@link #tryParseInt(String)}/
 *       {@link #tryParseLong(String)}/{@link #tryParseFloat(String)}/{@link #tryParseDouble(String)} and {@link #tryCreateNumber(String)}
 *       return an empty optional, and mathematical operations generally reject invalid arguments. Whitespace-only
 *       strings are not empty and remain invalid for the creation methods.</li>
 *   <li><b>Parsing:</b> integer {@code to*}, {@code decode*}/{@code parse*}, {@link #createNumber(String)},
 *       their {@code try*} companions, {@link #isCreatable(String)}, and {@link #isParsable(String)} use distinct, documented grammars.
 *       See the parser policy and matrix below.</li>
 *   <li><b>Overflow and precision:</b> some methods throw, some saturate, and some follow Java primitive
 *       narrowing or IEEE-754 behavior. The method Javadoc states which policy applies.</li>
 *   <li><b>Floating point:</b> handling of {@code NaN}, infinity, signed zero, and decimal/binary conversion is
 *       documented per method; it is not uniform across all conversion and rounding families.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Mathematical operations
 * long factorial          = Numbers.saturatedFactorialToLong(10);   // returns 3628800L
 * BigInteger bigFactorial = Numbers.factorialToBigInteger(50);      // returns a 65-digit number
 * int binomial            = Numbers.binomialExact(10, 3);           // returns 120
 * double mean             = Numbers.mean(1, 2, 3, 4, 5);            // returns 3.0
 *
 * // Rounding operations with various modes
 * double rounded   = Numbers.round(3.14159, 2);                              // returns 3.14
 * double roundedUp = Numbers.round(3.14159, 2, RoundingMode.CEILING);        // returns 3.15
 * String formatted = Numbers.format(3.14159f, "##.##");                      // returns "3.14"
 *
 * // Type conversions with overflow detection
 * int roundedInt    = Numbers.roundToInt(3.7, RoundingMode.HALF_UP);         // returns 4
 * long roundedLong  = Numbers.roundToLong(3.7, RoundingMode.HALF_UP);        // returns 4L
 * BigInteger bigInt = Numbers.roundToBigInteger(3.7, RoundingMode.HALF_UP);  // returns 4
 *
 * // Fuzzy comparisons for floating-point numbers
 * boolean equal  = Numbers.fuzzyEquals(3.14159, 3.14160, 0.001);    // returns true
 * int comparison = Numbers.fuzzyCompare(3.14159, 3.14160, 0.001);   // returns 0
 *
 * // Number validation and checking
 * boolean isInteger = Numbers.isMathematicalInteger(3.0);   // returns true
 *
 * // Hyperbolic functions
 * double asinh = Numbers.asinh(1.0);                        // returns about 0.88137
 * double acosh = Numbers.acosh(2.0);                        // returns about 1.31696
 * double atanh = Numbers.atanh(0.5);                        // returns about 0.54931
 *
 * // Working with different number types
 * double doubleMean = Numbers.mean(1.5, 2.5, 3.5);          // returns 2.5
 * double longMean   = Numbers.mean(1L, 2L, 3L);             // returns 2.0
 * double intMean    = Numbers.mean(1, 2, 3);                // returns 2.0
 * }</pre>
 *
 * <p id="decimal-format-policy"><b>{@code DecimalFormat} pattern policy</b> &mdash; shared by every
 * {@code format(..., String)} overload:</p>
 * <ul>
 *   <li><b>Locale:</b> the pattern is locale-sensitive; grouping and decimal separator symbols follow the
 *       current default {@link java.util.Locale} ({@link java.util.Locale.Category#FORMAT}). All examples in
 *       this class assume US-style symbols.</li>
 *   <li><b>Caching:</b> each thread keeps a small access-ordered cache of the patterns it has actually used,
 *       valid for the current FORMAT locale; a later {@link Locale#setDefault} clears that thread's cache, so
 *       the next call uses the new symbols. The cache is bounded, so a thread that rotates through more
 *       patterns than it holds simply rebuilds the least recently used ones; nothing is retained per pattern
 *       globally.</li>
 *   <li><b>Rounding:</b> {@link java.math.RoundingMode#HALF_EVEN}, set explicitly (it is also the
 *       {@code DecimalFormat} default). For a different rounding mode, configure a {@code DecimalFormat}
 *       directly.</li>
 *   <li><b>Not the same operation as {@code round}:</b> {@code format(x, pattern)} and
 *       {@link #round(double, int)} both render a value to a number of decimal places, but they are two
 *       different roundings and can disagree. {@code format} rounds the binary {@code double} with
 *       {@code HALF_EVEN}; the two-argument {@code round} rounds the value's <em>canonical decimal string</em>
 *       ({@link Double#toString(double)}, or {@link Float#toString(float)} for the {@code float} overload)
 *       with {@code HALF_UP}. So {@code round(2.5, 0)} is {@code 3.0} while {@code format(2.5, "0")} is
 *       {@code "2"}, and {@code round(12.105f, 2)} is {@code 12.11f} while
 *       {@code format(12.105f, "0.00")} is {@code "12.10"} (a {@code float} is widened to {@code double}
 *       before formatting, so {@code format} sees {@code 12.104999542236328}). {@code HALF_UP} is the default
 *       only for the two-argument {@code round}; the three-argument
 *       {@link #round(double, int, RoundingMode)} applies whatever mode is supplied, including
 *       {@code HALF_EVEN}. Use {@code round} to obtain a rounded numeric value and {@code format} to render
 *       one for display.</li>
 *   <li><b>Signed zero:</b> the sign printed is the sign of the <em>value</em>, not of the rounded result,
 *       as {@code DecimalFormat} always does: a negative value that rounds to zero keeps its minus sign, so
 *       {@code format(-0.001, "0.00")} is {@code "-0.00"}, {@code format(-0.4, "0")} is {@code "-0"} and
 *       {@code format(-0.0, "0.00")} is {@code "-0.00"}. Round to the pattern's scale first and add
 *       {@code 0.0} if a bare {@code "0.00"} is wanted: {@code round(-0.001, 2)} is {@code -0.0}, and
 *       {@code -0.0 + 0.0} is {@code +0.0}.</li>
 *   <li><b>{@code NaN} and infinity:</b> a {@code float}/{@code double} {@code NaN} renders as the locale's
 *       NaN symbol ({@code "NaN"}) and {@code ±Infinity} as its infinity symbol ({@code "∞"} /
 *       {@code "-∞"}, U+221E); neither throws, and the pattern's digit, grouping and decimal-separator
 *       symbols are not applied to either. The two differ in one respect, as {@code DecimalFormat} defines
 *       them: an infinity still takes the pattern's <em>prefix and suffix</em>, so {@code format(x, "0.00%")}
 *       is {@code "∞%"} and {@code format(x, "$#,##0.00")} is {@code "$∞"} (negative:
 *       {@code "-$∞"}), whereas a {@code NaN} takes neither and is always the bare symbol.</li>
 *   <li><b>Literals:</b> characters that are not {@code DecimalFormat} pattern symbols are copied into the
 *       output as literals, so {@code format(1, "hello")} returns {@code "hello1"}.</li>
 *   <li><b>Invalid input:</b> a {@code null} pattern throws {@link IllegalArgumentException}, and so does a
 *       syntactically illegal pattern (for example an unmatched quote). Nothing else about a pattern is
 *       rejected.</li>
 *   <li><b>{@code null} value:</b> the boxed overloads ({@link #format(Integer, String)},
 *       {@link #format(Long, String)}, {@link #format(Float, String)}, {@link #format(Double, String)})
 *       return {@code null}; the primitive overloads cannot receive one.</li>
 * </ul>
 *
 * <p id="unknown-number-recovery"><b>Recovering the value of an unrecognized {@code Number} subtype</b>
 * &mdash; shared by {@link #toByte(Object)}, {@link #toShort(Object)}, {@link #toInt(Object)},
 * {@link #toLong(Object)} (and their {@code defaultValue} overloads) and by the integral and
 * arbitrary-precision targets of every {@code convert} overload. A {@code float}/{@code double} target never
 * reads the text: it takes the subtype's own {@code floatValue()}/{@code doubleValue()}, as
 * {@link #toFloat(Object)}/{@link #toDouble(Object)} do.</p>
 * <ul>
 *   <li><b>Why the text is consulted at all:</b> {@link Number#longValue()} is specified to be allowed to
 *       <em>wrap</em> for a subtype this class does not recognize, so it cannot be trusted for a range check.
 *       The subtype's {@code toString()} is used instead.</li>
 *   <li><b>The grammar is decimal only:</b> an optional sign, then digits with an optional decimal point, then
 *       an optional decimal exponent &mdash; exactly what {@link BigDecimal#BigDecimal(String)} accepts,
 *       restricted to ASCII digits. A hexadecimal ({@code 0x}, {@code #}) or {@code L}-suffixed spelling is
 *       <em>not</em> part of it. Those are Java source-literal forms that the {@code to*(String)} text parsers
 *       accept; no {@code Number} renders itself that way, and object conversion is a numeric coercion rather
 *       than a text parse.</li>
 *   <li><b>One grammar, every target:</b> the integral targets and the {@code BigInteger}/{@code BigDecimal}
 *       targets accept exactly the same texts, so no two targets can disagree about the same source value.</li>
 *   <li><b>Otherwise {@code doubleValue()}:</b> a {@code toString()} that is formatted, localized,
 *       unit-suffixed, empty or {@code null} falls back to the finite- and range-checked {@code doubleValue()}
 *       view rather than reporting a parse failure. Each target then treats that {@code double} exactly as it
 *       treats a {@code Double} source: the integral and {@code BigInteger} targets truncate its
 *       <em>exact</em> value, the {@code BigDecimal} target takes its canonical decimal spelling
 *       ({@link BigDecimal#valueOf(double)}). So the answer for such a subtype is the answer for
 *       {@code Double.valueOf(x.doubleValue())}, for every target. A target reached through the source's
 *       string form (see {@link #convert(Number, Class)}) applies the same fallback when the
 *       {@code toString()} is {@code null} or empty: the {@code doubleValue()} is rendered as a
 *       {@code Double} would be and handed to that target's parser, so a non-{@code null} source never
 *       yields {@code null}.</li>
 *   <li><b>Cost:</b> the text is truncated toward zero and range-checked lexically, so only the digits that
 *       survive the truncation are read. A token of any length costs one linear scan; no
 *       arbitrary-precision value is built for an integral target.</li>
 * </ul>
 *
 * <p><b>Numbers-supported numeric grammars:</b> string parsers and predicates in this class
 * ({@link #createNumber(String)}, {@link #tryCreateNumber(String)}, {@link #isCreatable(String)}, {@link #isParsable(String)},
 * {@code decodeInteger}/{@code decodeLong}/{@code decodeBigInteger},
 * {@code parseFloat}/{@code parseDouble}/{@code parseBigDecimal},
 * {@code tryParseInt}/{@code tryParseLong}/{@code tryParseFloat}/{@code tryParseDouble}) use distinct, method-specific grammars.
 * Common syntax and important differences include:</p>
 * <ul>
 *   <li>ASCII digits {@code '0'}–{@code '9'} only; non-ASCII Unicode digits are rejected</li>
 *   <li>optional leading {@code '+'} or {@code '-'}</li>
 *   <li>a decimal point (leading {@code .5} and trailing {@code 123.} are accepted by
 *       {@code isCreatable}/{@code createNumber}, {@code isParsable}, and the {@code parse*} methods)</li>
 *   <li>scientific notation ({@code 1.5e3}) — {@code isCreatable}, {@code createNumber},
 *       {@code isParsable}, and the {@code parse*} methods</li>
 *   <li>integer hexadecimal with {@code 0x}/{@code 0X} or the library-specific {@code #} prefix
 *       ({@code #FF}) for the {@code decode*} methods and {@code createNumber}; JDK hexadecimal floating-point
 *       forms such as {@code 0x1.0p2} for {@code parseFloat}, {@code parseDouble}, and {@code isParsable}</li>
 *   <li>leading-zero octal for the {@code decode*} methods and {@code createNumber} ({@code 010} is 8), but
 *       decimal for the {@code parse*} methods and {@code isParsable} ({@code 010} is 10)</li>
 *   <li>type suffixes {@code l}/{@code L}, {@code f}/{@code F}, {@code d}/{@code D} on
 *       <em>decimal</em> forms ({@code "123L"}, {@code "1.5f"}). Hex + {@code L} is not a combined form:
 *       {@code createNumber("0xFFL")} throws; use {@link #decodeLong(String)}</li>
 * </ul>
 * <p>{@code isParsable} is {@code true} if and only if the raw input is at most
 * {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units and {@link Double#parseDouble(String)} succeeds
 * (after an ASCII/whitespace pre-filter); it is not a nested subset of {@code isCreatable}.
 * {@code tryParseInt}/{@code tryParseLong} use the same decimal-first grammar as {@code toInt}/{@code toLong};
 * {@code tryParseFloat}/{@code tryParseDouble} use the same grammar and result as
 * {@code parseFloat}/{@code parseDouble}, respectively, for non-empty input;
 * {@code tryCreateNumber} uses the same grammar and type-selection policy as {@code createNumber} for non-empty input.
 * Each {@code try*} method returns an empty optional on a documented parse failure; actual arbitrary-precision
 * construction can still fail when an implementation magnitude or resource limit is exceeded.
 * Individual methods document the exact subset they accept.</p>
 *
 * <p><b>Typed parse/decode and {@code createNumber} policies:</b> {@link #decodeInteger(String)}, {@link #decodeLong(String)}, and
 * {@link #decodeBigInteger(String)} parse integer forms only (decimal, {@code 0x}/{@code #} hexadecimal,
 * leading-zero octal, and an optional sign). They reject decimal points, exponents, and type suffixes, except that
 * {@code decodeLong} accepts a trailing {@code L}/{@code l}. {@link #parseFloat(String)} and
 * {@link #parseDouble(String)} use the JDK floating-point grammar for raw inputs of at most
 * {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units, including decimal and hexadecimal
 * floating-point forms, exponents, suffixes, {@code NaN}, and infinity. {@link #parseBigDecimal(String)} accepts
 * decimal and exponent forms but not hexadecimal. {@link #createNumber(String)} selects a result type according to
 * the input form and magnitude. For every {@code decode*}/{@code parse*}/{@link #createNumber(String)} method,
 * {@code null} or exactly {@code ""} returns {@code null}; a non-empty invalid string, including a whitespace-only
 * string, throws {@link NumberFormatException}.</p>
 *
 * <table id="create-method-matrix" border="1">
 *   <caption>Canonical decode/parse/{@code createNumber} result matrix ({@code NFE} = throws {@link NumberFormatException})</caption>
 *   <tr><th>Input</th><th>{@code decodeInteger}</th><th>{@code decodeLong}</th><th>{@code parseFloat}</th><th>{@code parseDouble}</th><th>{@code decodeBigInteger}</th><th>{@code parseBigDecimal}</th><th>{@code createNumber}</th></tr>
 *   <tr><td>{@code "123"}</td><td>Integer 123</td><td>Long 123</td><td>Float 123.0</td><td>Double 123.0</td><td>BigInteger 123</td><td>BigDecimal 123</td><td>Integer 123</td></tr>
 *   <tr><td>{@code "1.5"}</td><td>NFE</td><td>NFE</td><td>Float 1.5</td><td>Double 1.5</td><td>NFE</td><td>BigDecimal 1.5</td><td>Double 1.5</td></tr>
 *   <tr><td>{@code "0xFF"}</td><td>Integer 255</td><td>Long 255</td><td>NFE</td><td>NFE</td><td>BigInteger 255</td><td>NFE</td><td>Integer 255</td></tr>
 *   <tr><td>{@code "010"}</td><td>Integer 8</td><td>Long 8</td><td>Float 10.0</td><td>Double 10.0</td><td>BigInteger 8</td><td>BigDecimal 10</td><td>Integer 8</td></tr>
 *   <tr><td>{@code "1e3"}</td><td>NFE</td><td>NFE</td><td>Float 1000.0</td><td>Double 1000.0</td><td>NFE</td><td>BigDecimal 1E+3</td><td>Double 1000.0</td></tr>
 *   <tr><td>{@code "123L"}</td><td>NFE</td><td>Long 123</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>Long 123</td></tr>
 *   <tr><td>{@code "0xFFL"}</td><td>NFE</td><td>Long 255</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE (hex path does not strip {@code L})</td></tr>
 *   <tr><td>{@code "1.5f"}</td><td>NFE</td><td>NFE</td><td>Float 1.5</td><td>Double 1.5</td><td>NFE</td><td>NFE</td><td>Float 1.5</td></tr>
 *   <tr><td>{@code "NaN"}</td><td>NFE</td><td>NFE</td><td>Float NaN</td><td>Double NaN</td><td>NFE</td><td>NFE</td><td>NFE</td></tr>
 *   <tr><td>{@code "0x1.0p2"}</td><td>NFE</td><td>NFE</td><td>Float 4.0</td><td>Double 4.0</td><td>NFE</td><td>NFE</td><td>NFE</td></tr>
 *   <tr><td>{@code " 123 "}</td><td>NFE</td><td>NFE</td><td>Float 123.0</td><td>Double 123.0</td><td>NFE</td><td>NFE</td><td>NFE</td></tr>
 *   <tr><td>{@code "99999999999999999999"} (beyond long)</td><td>NFE</td><td>NFE</td><td>Float 1.0E20</td><td>Double 1.0E20</td><td>BigInteger 99999999999999999999</td><td>BigDecimal 99999999999999999999</td><td>BigInteger 99999999999999999999</td></tr>
 *   <tr><td>{@code "9e99"}</td><td>NFE</td><td>NFE</td><td>Float Infinity</td><td>Double 9.0E99</td><td>NFE</td><td>BigDecimal 9E+99</td><td>Double 9.0E99</td></tr>
 *   <tr><td>{@code "abc"}</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td></tr>
 *   <tr><td>{@code "\n"}</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td></tr>
 *   <tr><td>{@code " "}</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td><td>NFE</td></tr>
 *   <tr><td>{@code ""}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td></tr>
 *   <tr><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td><td>{@code null}</td></tr>
 * </table>
 *
 * <p><b>{@code isCreatable} and {@code createNumber}:</b> {@link #isCreatable(String)} validates the complete
 * {@link #createNumber(String)} grammar and required {@link BigDecimal} scale range without constructing the represented
 * value. A {@code true} result does not guarantee that arbitrary-precision construction will fit the JDK implementation's
 * supported magnitude or the available memory. {@code isCreatable(null)} and {@code isCreatable("")} are
 * {@code false}, while {@code createNumber(null)} and {@code createNumber("")} return {@code null}.</p>
 *
 * <table id="is-creatable-matrix" border="1">
 *   <caption>{@code isCreatable(s)} versus {@code createNumber(s)}</caption>
 *   <tr><th>Input {@code s}</th><th>{@code isCreatable(s)}</th><th>{@code createNumber(s)}</th></tr>
 *   <tr><td>{@code "123"}</td><td>{@code true}</td><td>{@code Integer} 123</td></tr>
 *   <tr><td>{@code "0xFF"}</td><td>{@code true}</td><td>{@code Integer} 255 (hexadecimal)</td></tr>
 *   <tr><td>{@code "1.5e3"}</td><td>{@code true}</td><td>{@code Double} 1500.0 (scientific notation)</td></tr>
 *   <tr><td>{@code "01f"}</td><td>{@code true}</td><td>{@code Float} 1.0</td></tr>
 *   <tr><td>{@code "0123L"}</td><td>{@code true}</td><td>{@code Long} 83 (leading-zero octal)</td></tr>
 *   <tr><td>{@code "01e1"}</td><td>{@code true}</td><td>{@code Double} 10.0</td></tr>
 *   <tr><td>{@code "0xFFL"}</td><td>{@code false}</td><td>throws {@code NumberFormatException} (use {@code decodeLong})</td></tr>
 *   <tr><td>{@code "1e2147483648"}</td><td>{@code true}</td><td>{@code BigDecimal} 1E+2147483648 (scale {@link Integer#MIN_VALUE})</td></tr>
 *   <tr><td>{@code "1e2147483649"}</td><td>{@code false}</td><td>throws {@code NumberFormatException} (effective {@code BigDecimal} scale is out of range)</td></tr>
 *   <tr><td>{@code "0e999999999999999999999"}</td><td>{@code true}</td><td>{@code Double} 0.0 (a zero significand never requires {@code BigDecimal})</td></tr>
 *   <tr><td>{@code "1" + "0".repeat(646_456_993)}</td><td>{@code true}</td>
 *       <td>throws {@code ArithmeticException} (decimal {@code BigInteger} exceeds the reference JDK's supported magnitude)</td></tr>
 *   <tr><td>{@code "#8" + "0".repeat(536_870_911)}</td><td>{@code true}</td>
 *       <td>throws {@code ArithmeticException} (hexadecimal value is 2<sup>2147483647</sup>, outside the reference JDK's supported magnitude)</td></tr>
 *   <tr><td>{@code "abc"}, {@code "1.2.3"}</td><td>{@code false}</td><td>throws {@code NumberFormatException}</td></tr>
 *   <tr><td>non-ASCII digits</td><td>{@code false}</td><td>throws {@code NumberFormatException}</td></tr>
 *   <tr><td>{@code "\n"}</td><td>{@code false}</td><td>throws {@code NumberFormatException}</td></tr>
 *   <tr><td>{@code " "}</td><td>{@code false}</td><td>throws {@code NumberFormatException}</td></tr>
 *   <tr><td>{@code ""}</td><td>{@code false}</td><td>{@code null}</td></tr>
 *   <tr><td>{@code null}</td><td>{@code false}</td><td>{@code null}</td></tr>
 * </table>
 * <p>The two extreme-magnitude rows are input-construction recipes rather than literal source strings and assume the
 * input {@code String} itself can be allocated. With insufficient memory, allocation or number construction may instead
 * fail earlier with {@link OutOfMemoryError}.</p>
 *
 * <p><b>By design — integer {@code to*} vs {@code decode*} (hard radix policy):</b>
 * {@link #toByte(String)}, {@link #toShort(String)}, {@link #toInt(String)} and {@link #toLong(String)}
 * are a <em>different</em> family from {@link #decodeInteger(String)}/{@link #decodeLong(String)}/{@link #createNumber(String)}.
 * The {@code to*} methods are decimal-first coercions for user, config and CSV text: the token is an optional
 * sign, then decimal digits or prefixed hexadecimal ({@code 0x}/{@code 0X}/{@code #}), then an optional
 * trailing {@code L}/{@code l}. Leading zeros are decimal padding ({@code "010"} is 10, {@code "08"} is 8);
 * a leading {@code 0} is <b>never</b> octal on the {@code to*} path. A malformed token throws
 * {@code NumberFormatException}; a valid integer outside the target range throws {@code ArithmeticException}.
 * Use {@code decodeInteger}/{@code createNumber} to decode a Java-style integer literal (leading-zero octal
 * and {@code 0x}/{@code #} hex).</p>
 * <table border="1">
 *   <caption>{@code toInt}/{@code toLong} versus {@code decodeInteger}/{@code createNumber}</caption>
 *   <tr><th>Input</th><th>{@code toInt} / {@code toLong}</th><th>{@code decodeInteger} / {@code createNumber}</th></tr>
 *   <tr><td>{@code "010"}</td><td>10 (decimal)</td><td>8 (octal)</td></tr>
 *   <tr><td>{@code "0123L"}</td><td>123</td><td>{@code decodeLong}/{@code createNumber}: 83 (octal); {@code decodeInteger} throws (no {@code L} suffix)</td></tr>
 *   <tr><td>{@code "08"}</td><td>8</td><td>throws {@code NumberFormatException} (invalid octal)</td></tr>
 *   <tr><td>{@code "0x10"}</td><td>16 (hex prefix)</td><td>16 (hex)</td></tr>
 * </table>
 *
 * <p><b>By design — hex + {@code L} suffix (hard policy):</b> {@link #decodeLong(String)} excludes a trailing
 * {@code L}/{@code l} by index before applying {@link Long#decode(String)}-style radix rules, so
 * {@code "0xFFL"} is {@code 255L}.
 * {@link #createNumber(String)} (and {@link #decodeInteger(String)}) detect the {@code 0x}/{@code 0X}/{@code #}
 * prefix first and reject a trailing {@code L}/{@code l} on that path, so {@code "0xFFL"} and the wider
 * {@code "0xFFFFFFFFL"} both throw {@code NumberFormatException}. {@code isCreatable} is {@code false} for
 * the same inputs. {@link #toLong(String)} accepts hex+{@code L} ({@code "0xFFL"} is {@code 255L}).
 * Write {@code "0xFF"} or call {@code decodeLong} for a Java-style hex long literal.</p>
 * <table border="1">
 *   <caption>Hex + {@code L} versus decimal {@code L}</caption>
 *   <tr><th>Input</th><th>{@code toLong}</th><th>{@code decodeLong}</th><th>{@code createNumber}</th><th>{@code isCreatable}</th></tr>
 *   <tr><td>{@code "0xFF"}</td><td>255</td><td>255</td><td>Integer 255</td><td>{@code true}</td></tr>
 *   <tr><td>{@code "0xFFL"}</td><td>255</td><td>255</td><td>throws {@code NumberFormatException}</td><td>{@code false}</td></tr>
 *   <tr><td>{@code "0xFFFFFFFFL"}</td><td>4294967295</td><td>4294967295</td><td>throws {@code NumberFormatException}</td><td>{@code false}</td></tr>
 *   <tr><td>{@code "#FFL"}</td><td>255</td><td>255</td><td>throws {@code NumberFormatException}</td><td>{@code false}</td></tr>
 *   <tr><td>{@code "123L"}</td><td>123</td><td>123</td><td>Long 123</td><td>{@code true}</td></tr>
 * </table>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.</p>
 *
 * <p><b>Cross-library behavior differences (quick reference):</b> a number of methods deliberately differ from the
 * equivalent helper in the JDK ({@code java.lang.Math}/{@code Integer}/{@code java.math}), Apache Commons Lang
 * ({@code NumberUtils}), and Guava ({@code com.google.common.math.IntMath}/{@code LongMath}/{@code DoubleMath} and the
 * primitive {@code Ints}/{@code Longs}/{@code Doubles}/... helpers) &mdash; usually to add range/overflow checking,
 * ASCII-strict parsing, or {@code null}-safety. The table below summarizes the notable cases; each point where a
 * library diverges from {@code Numbers} is flagged with &#9888;&#65039;.</p>
 * <table border="1">
 *   <caption>How selected {@code Numbers} methods differ from JDK / Apache Commons Lang / Guava</caption>
 *   <thead>
 *     <tr><th>Method</th><th>Key divergence</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code toInt/toLong/toByte/toShort/toFloat/toDouble(String)}</td>
 *       <td><b><i>Numbers</i></b>: {@code null}/{@code ""}&rarr;{@code 0}, malformed&rarr;{@code NumberFormatException}, integer out-of-range&rarr;{@code ArithmeticException} (the {@code toFloat}/{@code toDouble} string parsers instead saturate to &plusmn;Infinity and never throw on overflow); integer {@code to*} is decimal-first so {@code "010"}&rarr;10, not octal 8 (that is {@code decodeInteger}) &middot; &#9888;&#65039; <b><i>JDK</i></b> throws for all &middot; &#9888;&#65039; <b><i>Commons</i></b> returns default for all &middot; &#9888;&#65039; <b><i>Guava</i></b> {@code tryParse}&rarr;{@code null}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code toByte/.../toLong(Object)} (fractional {@code Number})</td>
 *       <td><b><i>Numbers</i></b>: truncate-toward-zero + range-check ({@code 128.9f}&rarr;{@code ArithmeticException}) &middot; &#9888;&#65039; <b><i>JDK</i></b> {@code Number.byteValue()} silently wraps to {@code -128}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code isParsable(String)}</td>
 *       <td><b><i>Numbers</i></b>: {@code true} iff the raw input is at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units and {@link Double#parseDouble(String)} succeeds after an ASCII/whitespace pre-filter; {@code parseFloat} uses the same bounded lexical grammar, while {@code parseBigDecimal} is decimal-only and unbounded by this floating-point limit &middot; &#9888;&#65039; <b><i>Commons</i></b> {@code NumberUtils.isParsable} is a limited decimal grammar using {@code Character.isDigit} (Unicode digits; no exponent, suffix, or {@code NaN})</td>
 *     </tr>
 *     <tr>
 *       <td>{@code gcd}/{@code lcm} (int, long)</td>
 *       <td><b><i>Numbers</i></b>: takes abs of args ({@code gcd(-4,6)}&rarr;{@code 2}, {@code lcm(-4,6)}&rarr;{@code 12}) &middot; &#9888;&#65039; <b><i>Guava</i></b> {@code IntMath}/{@code LongMath} {@code gcd} throws {@code IllegalArgumentException} on any negative; those classes do not provide {@code lcm}</td>
 *     </tr>
 *     <tr>
 *       <td>{@code round(double, scale)}</td>
 *       <td><b><i>Numbers</i></b>: scale-based, {@code HALF_UP} away-from-zero ({@code round(-2.5,0)}&rarr;{@code -3.0}) &middot; &#9888;&#65039; <b><i>JDK</i></b> {@code Math.round}&rarr;{@code long}, toward {@code +Infinity} ({@code -2.5}&rarr;{@code -2}) &middot; &#9888;&#65039; <b><i>Guava</i></b> {@code DoubleMath.roundTo*} integer-only</td>
 *     </tr>
 *     <tr>
 *       <td>{@code powExact}/{@code saturatedPow}</td>
 *       <td><b><i>Numbers</i></b>: throw or saturate on overflow (no wrapping {@code pow}) &middot; &#9888;&#65039; <b><i>JDK</i></b> {@code Math.pow}&rarr;{@code double} only; <b><i>Guava</i></b> {@code IntMath.pow} wraps</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><b>By design:</b> conversion/parsing, Guava-style integer arithmetic, {@code DecimalFormat} helpers,
 * combinatorics, and selected extra functions (hyperbolic, primality) share this class rather than being
 * split across types. Related NaN-aware min/max lives in {@link IEEE754rUtil}; medians live in {@link Median}.</p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.IEEE754rUtil
 * @see com.landawn.abacus.util.Median
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.RegExUtil
 * @see java.lang.Math
 * @see java.math.BigDecimal
 * @see java.math.BigInteger
 * @see java.text.DecimalFormat
 * @see java.text.NumberFormat
 * @see java.math.RoundingMode
 */
@SuppressWarnings({ "java:S1192", "java:S2148" })
public final class Numbers {

    /** Reusable Byte constant for zero. */
    public static final Byte BYTE_ZERO = (byte) 0;
    /** Reusable Byte constant for one. */
    public static final Byte BYTE_ONE = (byte) 1;
    /** Reusable Byte constant for minus one. */
    public static final Byte BYTE_MINUS_ONE = (byte) -1;
    /** Reusable Short constant for zero. */
    public static final Short SHORT_ZERO = (short) 0;
    /** Reusable Short constant for one. */
    public static final Short SHORT_ONE = (short) 1;
    /** Reusable Short constant for minus one. */
    public static final Short SHORT_MINUS_ONE = (short) -1;
    /** Reusable Integer constant for zero. */
    public static final Integer INTEGER_ZERO = 0;
    /** Reusable Integer constant for one. */
    public static final Integer INTEGER_ONE = 1;
    /** Reusable Integer constant for two. */
    public static final Integer INTEGER_TWO = 2;
    /** Reusable Integer constant for minus one. */
    public static final Integer INTEGER_MINUS_ONE = -1;
    /** Reusable Long constant for zero. */
    public static final Long LONG_ZERO = 0L;
    /** Reusable Long constant for one. */
    public static final Long LONG_ONE = 1L;
    /** Reusable Long constant for minus one. */
    public static final Long LONG_MINUS_ONE = -1L;
    /** Reusable Float constant for zero. */
    public static final Float FLOAT_ZERO = 0.0f;
    /** Reusable Float constant for one. */
    public static final Float FLOAT_ONE = 1.0f;
    /** Reusable Float constant for minus one. */
    public static final Float FLOAT_MINUS_ONE = -1.0f;
    /** Reusable Double constant for zero. */
    public static final Double DOUBLE_ZERO = 0.0d;
    /** Reusable Double constant for one. */
    public static final Double DOUBLE_ONE = 1.0d;
    /** Reusable Double constant for minus one. */
    public static final Double DOUBLE_MINUS_ONE = -1.0d;

    /** Positive zero. */
    private static final double DOUBLE_POSITIVE_ZERO = 0d;

    /** Positive zero. */
    private static final float FLOAT_POSITIVE_ZERO = 0f;

    private Numbers() {
        // utility class.
    }

    private static final long ONE_BITS = doubleToRawLongBits(1.0);

    /** The biggest half-power of two that can fit in an unsigned int. */
    private static final int INT_MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333;

    /** The biggest half-power of two that fits into an unsigned long. */
    private static final long MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333F9DE6484L;

    /** The largest power of two representable as a signed {@code long}, i.e. 2^62. */
    private static final long MAX_SIGNED_POWER_OF_TWO = 1L << (Long.SIZE - 2);

    /** {@code floor(sqrt(Long.MAX_VALUE))}: the largest {@code long} whose square still fits in a {@code long}. */
    private static final long FLOOR_SQRT_MAX_LONG = 3037000499L;

    /** {@code floor(sqrt(Integer.MAX_VALUE))}: the largest {@code int} whose square still fits in an {@code int}. */
    private static final int FLOOR_SQRT_MAX_INT = 46340;

    // The mask for the significand, according to the {@link
    // Double#doubleToRawLongBits(double)} spec.
    private static final long SIGNIFICAND_MASK = 0x000fffffffffffffL;

    /** The number of explicitly stored significand bits in an IEEE-754 {@code double}. */
    private static final int SIGNIFICAND_BITS = 52;

    // The mask for the exponent, per the Double#doubleToRawLongBits(double) spec. Retained beside
    // SIGNIFICAND_MASK to keep the IEEE-754 layout stated in one place, though nothing in this class
    // reads it any more: isFinite/isNormal delegate to java.lang.Double instead of masking bits.
    @SuppressWarnings("unused")
    private static final long EXPONENT_MASK = 0x7ff0000000000000L;

    /**
     * The implicit 1 bit that is omitted in significands of normal doubles.
     */
    private static final long IMPLICIT_BIT = SIGNIFICAND_MASK + 1;

    private static final double MIN_INT_AS_DOUBLE = -0x1p31;

    private static final double MAX_INT_AS_DOUBLE = 0x1p31 - 1.0;

    private static final double MIN_LONG_AS_DOUBLE = -0x1p63;

    /*
     * We cannot store Long.MAX_VALUE as a double without losing precision. Instead, we store
     * Long.MAX_VALUE + 1 == -Long.MIN_VALUE, and then offset all comparisons by 1.
     */
    private static final double MAX_LONG_AS_DOUBLE_PLUS_ONE = 0x1p63;

    // int_maxLog10ForLeadingZeros[i] == floor(log10(2^(Integer.SIZE - i)))
    private static final byte[] int_maxLog10ForLeadingZeros = { 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0,
            0 };

    private static final int[] int_powersOf10 = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000 };

    private static final int[] int_factorials = { 1, 1, 2, 2 * 3, 2 * 3 * 4, 2 * 3 * 4 * 5, 2 * 3 * 4 * 5 * 6, 2 * 3 * 4 * 5 * 6 * 7, 2 * 3 * 4 * 5 * 6 * 7 * 8,
            2 * 3 * 4 * 5 * 6 * 7 * 8 * 9, 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10, 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11,
            2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 };

    // C(int_biggestBinomials[k], k) fits in an int, but C(int_biggestBinomials[k] + 1, k) does not.
    private static final int[] int_biggestBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, 65536, 2345, 477, 193, 110, 75, 58, 49, 43, 39, 37, 35, 34, 34,
            33 }; //NOSONAR

    // int_halfPowersOf10[i] = largest int less than 10^(i + 0.5)
    private static final int[] int_halfPowersOf10 = { 3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, Integer.MAX_VALUE };

    // maxLog10ForLeadingZeros[i] == floor(log10(2^(Long.SIZE - i)))
    private static final byte[] maxLog10ForLeadingZeros = { 19, 18, 18, 18, 18, 17, 17, 17, 16, 16, 16, 15, 15, 15, 15, 14, 14, 14, 13, 13, 13, 12, 12, 12, 12,
            11, 11, 11, 10, 10, 10, 9, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0 };

    private static final long[] powersOf10 = { 1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L, 10000000000L, 100000000000L,
            1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L };

    // halfPowersOf10[i] = largest long less than 10^(i + 0.5)
    private static final long[] halfPowersOf10 = { 3L, 31L, 316L, 3162L, 31622L, 316227L, 3162277L, 31622776L, 316227766L, 3162277660L, 31622776601L,
            316227766016L, 3162277660168L, 31622776601683L, 316227766016837L, 3162277660168379L, 31622776601683793L, 316227766016837933L,
            3162277660168379331L };

    private static final long[] long_factorials = { 1L, 1L, 2L, (long) 2 * 3, (long) 2 * 3 * 4, (long) 2 * 3 * 4 * 5, (long) 2 * 3 * 4 * 5 * 6,
            (long) 2 * 3 * 4 * 5 * 6 * 7, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19 * 20 };

    // C(biggestBinomials[k], k) fits in a long, but C(biggestBinomials[k] + 1, k) does not. Phrased in terms
    // of the coefficient rather than a method, as int_biggestBinomials above is: saturatedBinomialToLong
    // saturates, so saying its result "fits in a long" would be true of every input and state nothing.
    // binomialExactToLong is the method that observes this boundary -- it returns for the first argument and
    // throws for the second -- and saturatedBinomialToLong is what returns Long.MAX_VALUE past it.
    private static final int[] biggestBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 3810779, 121977, 16175, 4337, 1733, 887, 534, 361,
            265, 206, 169, 143, 125, 111, 101, 94, 88, 83, 79, 76, 74, 72, 70, 69, 68, 67, 67, 66, 66, 66, 66 };

    /*
     * Numbers.saturatedBinomialToLong(biggestSimpleBinomials[k], k) doesn't need to use the slower GCD-based impl, but
     * Numbers.saturatedBinomialToLong(biggestSimpleBinomials[k] + 1, k) does.
     */
    private static final int[] biggestSimpleBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 2642246, 86251, 11724, 3218, 1313, 684, 419,
            287, 214, 169, 139, 119, 105, 95, 87, 81, 76, 73, 70, 68, 66, 64, 63, 62, 62, 61, 61, 61 };

    /*
     * This bitmask is used as an optimization for cheaply testing for divisibility by 2, 3, or 5.
     * Each bit is set to 1 for all remainders that indicate divisibility by 2, 3, or 5, so
     * 1, 7, 11, 13, 17, 19, 23, 29 are set to 0. 30 and up don't matter because they won't be hit.
     */
    private static final int SIEVE_30 = ~((1 << 1) | (1 << 7) | (1 << 11) | (1 << 13) | (1 << 17) | (1 << 19) | (1 << 23) | (1 << 29));

    /*
     * If n <= millerRabinBaseSets[i][0], then testing n against bases millerRabinBaseSets[i][1..] suffices
     * to prove its primality. Values from miller-rabin.appspot.com.
     *
     * NOTE: We could get slightly better bases that would be treated as unsigned, but benchmarks
     * showed negligible performance improvements.
     */
    private static final long[][] millerRabinBaseSets = { { 291830, 126401071349994536L }, { 885594168, 725270293939359937L, 3569819667048198375L },
            { 273919523040L, 15, 7363882082L, 992620450144556L }, { 47636622961200L, 2, 2570940, 211991001, 3749873356L },
            { 7999252175582850L, 2, 4130806001517L, 149795463772692060L, 186635894390467037L, 3967304179347715805L },
            { 585226005592931976L, 2, 123635709730000L, 9233062284813009L, 43835965440333360L, 761179012939631437L, 1263739024124850375L },
            { Long.MAX_VALUE, 2, 325, 9375, 28178, 450775, 9780504, 1795265022 } };

    // Exclusive, truncation-aware bounds for the BigDecimal narrowing checks in numberConverterFuncMap:
    // for the byte/short/int/long ranges (which all span zero), trunc(x) fits in [MIN, MAX] iff
    // MIN - 1 < x < MAX + 1. Comparing a BigDecimal against these bounds is O(digits) and never
    // materializes the integer part, unlike toBigInteger(), which can allocate a huge BigInteger
    // (or throw a JDK-internal ArithmeticException) for inputs such as new BigDecimal("1e2147483647").
    private static final BigDecimal BIG_DECIMAL_WITH_MIN_BYTE_VALUE_MINUS_ONE = BigDecimal.valueOf(Byte.MIN_VALUE).subtract(BigDecimal.ONE);
    private static final BigDecimal BIG_DECIMAL_WITH_MIN_SHORT_VALUE_MINUS_ONE = BigDecimal.valueOf(Short.MIN_VALUE).subtract(BigDecimal.ONE);
    private static final BigDecimal BIG_DECIMAL_WITH_MIN_INT_VALUE_MINUS_ONE = BigDecimal.valueOf(Integer.MIN_VALUE).subtract(BigDecimal.ONE);
    private static final BigDecimal BIG_DECIMAL_WITH_MIN_LONG_VALUE_MINUS_ONE = BigDecimal.valueOf(Long.MIN_VALUE).subtract(BigDecimal.ONE);

    private static final BigDecimal BIG_DECIMAL_WITH_MAX_BYTE_VALUE_PLUS_ONE = BigDecimal.valueOf(Byte.MAX_VALUE).add(BigDecimal.ONE);
    private static final BigDecimal BIG_DECIMAL_WITH_MAX_SHORT_VALUE_PLUS_ONE = BigDecimal.valueOf(Short.MAX_VALUE).add(BigDecimal.ONE);
    private static final BigDecimal BIG_DECIMAL_WITH_MAX_INT_VALUE_PLUS_ONE = BigDecimal.valueOf(Integer.MAX_VALUE).add(BigDecimal.ONE);
    private static final BigDecimal BIG_DECIMAL_WITH_MAX_LONG_VALUE_PLUS_ONE = BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE);

    // Shared by the converter table and object-narrowing paths. BigInteger.valueOf only caches a small range,
    // so recreating these primitive bounds in each conversion would allocate for every target shown here.
    private static final BigInteger BIG_INTEGER_WITH_MIN_BYTE_VALUE = BigInteger.valueOf(Byte.MIN_VALUE);
    private static final BigInteger BIG_INTEGER_WITH_MIN_SHORT_VALUE = BigInteger.valueOf(Short.MIN_VALUE);
    private static final BigInteger BIG_INTEGER_WITH_MIN_INT_VALUE = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger BIG_INTEGER_WITH_MIN_LONG_VALUE = BigInteger.valueOf(Long.MIN_VALUE);

    private static final BigInteger BIG_INTEGER_WITH_MAX_BYTE_VALUE = BigInteger.valueOf(Byte.MAX_VALUE);
    private static final BigInteger BIG_INTEGER_WITH_MAX_SHORT_VALUE = BigInteger.valueOf(Short.MAX_VALUE);
    private static final BigInteger BIG_INTEGER_WITH_MAX_INT_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIG_INTEGER_WITH_MAX_LONG_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    /**
     * Maximum UTF-16 length accepted by the bounded {@code float}/{@code double} string parsers of this
     * class: {@value}. It is part of the public contract of {@link #toFloat(String)}, {@link #toDouble(String)},
     * {@link #parseFloat(String)}, {@link #parseDouble(String)}, {@link #tryParseFloat(String)},
     * {@link #tryParseDouble(String)}, {@link #isParsable(String)} and the {@code extractFirstDouble} family.
     *
     * <p><b>What is measured</b> differs by family. For the parsers and predicates above it is the raw
     * argument, before {@link String#trim()}-style whitespace removal, so surrounding whitespace counts
     * toward the limit. For the {@code extractFirstDouble} family it is the <em>matched token</em>, not the
     * argument the token was found in: searching a megabyte of text for a number is not itself the cost this
     * limit exists to bound, so {@code extractFirstDouble} accepts an argument of any length and rejects only
     * an over-long match.</p>
     *
     * <p><b>Why the cap exists:</b> it is an input-size policy for caller-supplied text, not a parser
     * protection. The JDK parser is linear in the token length: it clips the significand to its first 1,100
     * significant digits and folds the rest into a sticky bit, so there is no superlinear correction step, and
     * a near-tie token measures about one nanosecond per character at every length. The cap bounds the text
     * this class will scan on the caller's behalf, hand to a binary parser and embed in diagnostics, while
     * admitting far more digits than any {@code float} or {@code double} can distinguish. A larger value would
     * be just as safe; the limit is a contract, not a defence.</p>
     *
     * <p><b>Not applied by:</b> {@link #parseBigDecimal(String)}, an arbitrary-precision decimal parser that
     * accepts input of any length; the <a href="#unknown-number-recovery">unrecognized-{@code Number}
     * recovery</a>, which reads a subtype's own {@code toString()} rather than caller-supplied text; and the
     * {@code create} family, below.</p>
     *
     * <p><b>Ignored by the {@code create} family:</b> {@link #createNumber(String)},
     * {@link #tryCreateNumber(String)} and {@link #isCreatable(String)} never consider the length of a token.
     * They choose the result type from the <em>value</em> alone, so {@code createNumber("1." + "0".repeat(4095))}
     * is {@code Double.valueOf(1.0)} exactly like every shorter spelling of one, and only a value a
     * {@code double} cannot hold (overflow to infinity, or underflow to zero with non-zero digits present)
     * escalates to {@code BigDecimal}. A type suffix therefore never changes the result type of a token that
     * already takes the floating path: for a non-hexadecimal {@code s} carrying a decimal point or an exponent
     * and no type suffix of its own, {@code createNumber(s)} and {@code createNumber(s + "d")} agree on the
     * result type at any length.
     * <b>A type suffix is not a no-op in general, though</b>, because it selects the grammar and not merely
     * the width. For an <em>integral</em> {@code s} it moves the token onto the decimal path:
     * {@code createNumber("123")} is an {@code Integer} but {@code createNumber("123d")} is a {@code Double},
     * and {@code createNumber("010")} is octal {@code 8} while {@code createNumber("010d")} is {@code 10.0}.
     * For a <em>hexadecimal</em> {@code s} the suffix is itself a hexadecimal digit and is consumed as part
     * of the magnitude: {@code createNumber("0x10")} is {@code 16} but {@code createNumber("0x10d")} is
     * {@code 269}. An integral token of any length yields {@code BigInteger}, and a lexical zero of any
     * length yields the correctly signed floating zero.</p>
     *
     * <p><b>Note:</b> this is a compile-time constant, so by JLS 4.12.4 every caller inlines the literal
     * {@value} at compile time. Changing the value requires recompiling callers, not merely relinking them.</p>
     */
    public static final int MAX_FLOATING_POINT_TOKEN_LENGTH = 4096;

    /** Values above this size are summarized rather than fully rendered in overflow diagnostics. */
    private static final int MAX_NUMBER_ERROR_BIT_LENGTH = 192;

    /**
     * Upper bound on the characters of a {@link Number}'s own text that may appear in an overflow message.
     * Derived from {@link #MAX_NUMBER_ERROR_BIT_LENGTH} so that nothing rendered in full today is truncated:
     * a 192-bit magnitude is 58 decimal digits, and the widest {@code BigDecimal} that bit-length guard
     * admits renders as 72 characters (sign, leading digit, point, 57 more digits, and an {@code E}-notation
     * exponent of up to 10 digits). Anything longer belongs to a subtype whose {@code toString()} this class
     * cannot bound in advance, and is previewed like any other caller-supplied text.
     */
    private static final int MAX_NUMBER_ERROR_TEXT_LENGTH = 96;

    /**
     * {@code sqrt(Double.MAX_VALUE)}, the magnitude above which {@code a * a} overflows to infinity.
     * Used by {@link #asinh(double)} and {@link #acosh(double)} to switch to the {@code log(a) + log(2)}
     * asymptotic form; hoisted out of those methods so it is not recomputed on every call.
     */
    private static final double SQRT_MAX_DOUBLE = Math.sqrt(Double.MAX_VALUE);

    private static final double LN_10 = Math.log(10);

    private static final double LN_2 = Math.log(2);

    /*
     * The maximum number of bits in a square root for which we'll precompute an explicit half-power
     * of two. This can be any value, but higher values incur more class load time and linearly
     * increasing memory consumption.
     */
    static final int SQRT2_PRECOMPUTE_THRESHOLD = 256;

    /**
     * {@code floor(sqrt(2) * 2^SQRT2_PRECOMPUTE_THRESHOLD)} &mdash; note that this is a
     * {@code SQRT2_PRECOMPUTE_THRESHOLD + 1} bit number, not a {@code SQRT2_PRECOMPUTE_THRESHOLD} bit one.
     * The scale is what makes it usable: {@link #log2(BigInteger, RoundingMode)} shifts it right by
     * {@code SQRT2_PRECOMPUTE_THRESHOLD - k} to obtain {@code floor(2^(k + 0.5))}, the value it compares
     * against for the {@code HALF_*} modes.
     */
    static final BigInteger SQRT2_PRECOMPUTED_BITS = new BigInteger("16a09e667f3bcc908b2fb1366ea957d3e3adec17512775099da2f590b0667322a", 16);

    /** The largest {@code n} for which {@code n!} is finite as a {@code double}; {@code 171!} overflows to infinity. */
    private static final int MAX_FACTORIAL = 170;

    /** {@code everySixteenthFactorial[i] == (16 * i)!} as a {@code double}, used by {@link #factorialToDouble(int)}. */
    private static final double[] everySixteenthFactorial = { 0x1.0p0, 0x1.30777758p44, 0x1.956ad0aae33a4p117, 0x1.ee69a78d72cb6p202, 0x1.fe478ee34844ap295,
            0x1.c619094edabffp394, 0x1.3638dd7bd6347p498, 0x1.7cac197cfe503p605, 0x1.1e5dfc140e1e5p716, 0x1.8ce85fadb707ep829, 0x1.95d5f3d928edep945 };

    /**
     * The exactly representable positive powers of ten: 10^k is an exact double iff 0 &lt;= k &lt;= 22,
     * because 10^k = 2^k * 5^k requires 5^k &lt;= 2^53. Used by {@link #log10(double, RoundingMode)}.
     */
    private static final double[] EXACT_DOUBLE_POWERS_OF_TEN = { 1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16,
            1e17, 1e18, 1e19, 1e20, 1e21, 1e22 };

    /**
     * Maximum number of {@link DecimalFormat} instances a single thread keeps cached. Sixteen distinct patterns
     * is far more than any realistic call site rotates through, and it bounds what a long-lived pool worker can
     * retain (a {@code DecimalFormat} is on the order of a kilobyte).
     */
    private static final int DECIMAL_FORMAT_CACHE_CAPACITY = 16;

    /**
     * Per-thread {@link DecimalFormat} cache, keyed by pattern and valid for one FORMAT locale at a time.
     * {@code DecimalFormat} is not thread-safe, so each thread owns its own instances; reuse avoids rebuilding
     * one on every {@code format} call. Read only by {@link #getThreadLocalDecimalFormat(String)}.
     *
     * <p>Eviction is <b>access</b>-order, not insertion order: a pattern used on every call is never evicted by
     * a burst of one-off patterns, which is the failure mode an insertion-ordered cache has. A cyclic workload
     * with more distinct patterns than the capacity degrades to rebuilding per call, which is exactly the
     * behaviour it replaced &mdash; measured at parity there, and about 2.3x faster whenever the working set
     * fits.</p>
     *
     * <p>Only the current FORMAT locale is retained: a locale change clears the map so
     * {@link Locale#setDefault} cannot leave stale symbols behind, and a long-lived thread cannot accumulate
     * one map per locale it has ever seen.</p>
     *
     * <p>The value stored in each thread is a two-slot {@code Object[]} &mdash; the {@link Locale} the entries
     * were built for, then a {@link LinkedHashMap} from pattern to {@code DecimalFormat} &mdash; and not a
     * class of this library, deliberately. A {@code ThreadLocal} value lives in the thread, not in this class,
     * and outlives this class's loader: on a container's pool thread, a value whose class belongs to the
     * application keeps that application's class loader reachable after an undeploy (the classic
     * {@code ThreadLocal} leak). JDK-only value types cannot pin anything. Eviction is therefore done by hand
     * after each insertion rather than by a {@code removeEldestEntry} override, which would again be a
     * library-defined subclass.</p>
     */
    private static final ThreadLocal<Object[]> THREAD_LOCAL_DECIMAL_FORMATS = ThreadLocal
            .withInitial(() -> new Object[] { null, new LinkedHashMap<String, DecimalFormat>(DECIMAL_FORMAT_CACHE_CAPACITY * 2, 0.75f, true) });

    /** Slot of the {@link Locale} in a {@link #THREAD_LOCAL_DECIMAL_FORMATS} value. */
    private static final int CACHE_LOCALE = 0;

    /** Slot of the pattern-to-{@link DecimalFormat} map in a {@link #THREAD_LOCAL_DECIMAL_FORMATS} value. */
    private static final int CACHE_FORMATS = 1;

    /**
     * ASCII characters that may appear in a numeric literal this class parses: digits, {@code + - . #}, the
     * {@code x}/{@code X} radix markers, the hexadecimal letters and the {@code l}/{@code L} type markers.
     * Used by {@link #quickCheckForNumericParsing(String)} as a cheap rejection filter. Declared here, with
     * the other statics initialized at class-initialization time, rather than beside its only reader: a
     * {@code static} block placed further down the file would leave this table all-{@code false} for any
     * initializer that ran before it.
     */
    private static final boolean[] alphanumerics = new boolean[128];

    static {
        alphanumerics['0'] = true;
        alphanumerics['1'] = true;
        alphanumerics['2'] = true;
        alphanumerics['3'] = true;
        alphanumerics['4'] = true;
        alphanumerics['5'] = true;
        alphanumerics['6'] = true;
        alphanumerics['7'] = true;
        alphanumerics['8'] = true;
        alphanumerics['9'] = true;

        alphanumerics['+'] = true;
        alphanumerics['-'] = true;
        alphanumerics['.'] = true;
        alphanumerics['#'] = true;

        alphanumerics['x'] = true;
        alphanumerics['X'] = true;

        alphanumerics['a'] = true;
        alphanumerics['A'] = true;
        alphanumerics['b'] = true;
        alphanumerics['B'] = true;
        alphanumerics['c'] = true;
        alphanumerics['C'] = true;
        alphanumerics['d'] = true;
        alphanumerics['D'] = true;
        alphanumerics['e'] = true;
        alphanumerics['E'] = true;
        alphanumerics['f'] = true;
        alphanumerics['F'] = true;

        alphanumerics['l'] = true;
        alphanumerics['L'] = true;
    }

    /**
     * The single body behind {@link #toFloat(String)}, {@link #toFloat(String, float)} and
     * {@link #parseFloat(String)} for non-empty input: the length limit, the JDK grammar, and the bounded
     * failure reporting the rest of this class uses.
     *
     * <p>The JDK parser must never be called unwrapped here. Its own {@link NumberFormatException} embeds the
     * <em>entire</em> input, unescaped -- a kilobyte-long message for a 4 KB token, and a raw line break for
     * {@code toFloat("1\n2")} -- whereas {@link #notAValidNumber(String, String, NumberFormatException)}
     * emits a bounded, escaped preview and applies the same bounding and escaping to the cause it attaches.
     * Sharing a single body with {@code parseFloat} keeps the two from drifting apart again.</p>
     *
     * <p>No {@link #quickCheckForIsParsable(String)} pre-filter runs first, deliberately. On the success path
     * -- the hot one, since this backs the {@code toFloat} coercions -- it would re-scan every character to
     * buy nothing, and on the failure path the JDK parser is the authority on its own grammar. The pre-filter
     * belongs where a rejection is a returned value rather than a throw ({@link #isParsable(String)},
     * {@link #tryParseFloat(String)}, {@link #tryParseDouble(String)}), which is where it stays.</p>
     *
     * @param str the non-empty string to parse
     * @return the parsed float
     * @throws NumberFormatException if {@code str} exceeds {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16
     *         code units or is not a valid float
     */
    private static float parseFloatWithinLengthLimit(final String str) throws NumberFormatException {
        if (str.length() > MAX_FLOATING_POINT_TOKEN_LENGTH) {
            throw floatingPointTokenTooLong(str, "Float");
        }

        try {
            return Float.parseFloat(str);
        } catch (final NumberFormatException e) {
            throw notAValidNumber(str, "Float", e);
        }
    }

    /**
     * The single body behind {@link #toDouble(String)}, {@link #toDouble(String, double)} and
     * {@link #parseDouble(String)} for non-empty input. See
     * {@link #parseFloatWithinLengthLimit(String)} for why the JDK parser is never called unwrapped and why no
     * pre-filter runs in front of it.
     *
     * @param str the non-empty string to parse
     * @return the parsed double
     * @throws NumberFormatException if {@code str} exceeds {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16
     *         code units or is not a valid double
     */
    private static double parseDoubleWithinLengthLimit(final String str) throws NumberFormatException {
        if (str.length() > MAX_FLOATING_POINT_TOKEN_LENGTH) {
            throw floatingPointTokenTooLong(str, "Double");
        }

        try {
            return Double.parseDouble(str);
        } catch (final NumberFormatException e) {
            throw notAValidNumber(str, "Double", e);
        }
    }

    private static NumberFormatException floatingPointTokenTooLong(final String str, final String typeName) {
        return notAValidNumber(str, typeName, floatingPointTokenTooLongCause(str.length(), typeName));
    }

    /**
     * Over-length failure for a token located inside a larger input, for example a match found by
     * {@link #extractFirstDouble(String)}. The token is never copied: the message embeds only a bounded
     * {@linkplain #previewForErrorMessage(String, int, int) preview}, so rejecting a hostile multi-megabyte
     * match costs a constant amount of memory rather than materializing the token in order to throw it away.
     * A token matched by the numeric finders always contains a digit, so it is never blank and, unlike
     * {@link #notAValidNumber(String, String, NumberFormatException)}, needs no quoting.
     *
     * @param source the string the token was matched in
     * @param start the index of the token's first character
     * @param end the index after the token's last character
     * @param typeName the display name of the target numeric type
     * @return the {@link NumberFormatException} to throw
     */
    private static NumberFormatException floatingPointTokenTooLong(final String source, final int start, final int end, final String typeName) {
        final NumberFormatException nfe = new NumberFormatException(
                previewForErrorMessage(source, start, end) + " is not a valid " + canonicalNfeTypeName(typeName) + ".");
        nfe.initCause(floatingPointTokenTooLongCause(end - start, typeName));
        return nfe;
    }

    private static NumberFormatException floatingPointTokenTooLongCause(final int length, final String typeName) {
        return new NumberFormatException(typeName + " input length " + length + " exceeds limit " + MAX_FLOATING_POINT_TOKEN_LENGTH);
    }

    private static ArithmeticException numberOverflow(final String typeName, final Number num) {
        return numberOverflow(typeName, num, null);
    }

    /**
     * As {@link #numberOverflow(String, Number)}, for a caller that has already obtained {@code num.toString()}.
     * Passing it in avoids a second call: {@code toString()} is the subtype's own code, it may be arbitrarily
     * expensive, and on the path that reaches here it has just produced a string that can be megabytes long.
     *
     * @param typeName the target type's display name
     * @param num the value that does not fit
     * @param text {@code num.toString()} if the caller already has it, otherwise {@code null}
     * @return the {@link ArithmeticException} to throw
     */
    private static ArithmeticException numberOverflow(final String typeName, final Number num, final String text) {
        return new ArithmeticException(typeName + " overflow: " + describeNumberForError(num, text));
    }

    /**
     * Returns a bounded, escaped description of {@code num} for an overflow message. A {@code BigInteger}
     * or {@code BigDecimal} wider than {@link #MAX_NUMBER_ERROR_BIT_LENGTH} is summarized by signum and bit
     * length; anything else is previewed from its own text, capped at
     * {@link #MAX_NUMBER_ERROR_TEXT_LENGTH}.
     *
     * @param num the value that does not fit
     * @param knownText {@code num.toString()} when the caller already holds it, otherwise {@code null};
     *        passing it in avoids a second call, which for an application subtype may be arbitrarily
     *        expensive and may have just produced a megabyte-long string
     * @return a bounded description of {@code num}, never {@code null}
     */
    private static String describeNumberForError(final Number num, final String knownText) {
        if (num instanceof BigInteger) {
            final BigInteger bi = (BigInteger) num;

            if (bi.bitLength() > MAX_NUMBER_ERROR_BIT_LENGTH) {
                return "BigInteger[signum=" + bi.signum() + ", bitLength=" + bi.bitLength() + "]";
            }
        } else if (num instanceof BigDecimal) {
            final BigDecimal bd = (BigDecimal) num;
            final BigInteger unscaled = bd.unscaledValue();

            if (unscaled.bitLength() > MAX_NUMBER_ERROR_BIT_LENGTH) {
                return "BigDecimal[signum=" + bd.signum() + ", unscaledBitLength=" + unscaled.bitLength() + ", scale=" + bd.scale() + "]";
            }
        }

        // Every other Number prints itself, and an application subtype is free to print a formatted, huge or
        // control-character-carrying string. Bound and escape it exactly as the string-input diagnostics are:
        // without this, toInt((Object) n) on a Number with a 100 KB toString() produced a 100 KB exception
        // message, and one whose toString() held a line break injected that break into the log line.
        // MAX_NUMBER_ERROR_TEXT_LENGTH is above everything the bit-length guards above admit, so no message
        // that renders in full today changes.
        final String text = knownText == null ? num.toString() : knownText;

        if (text == null) {
            // A subtype may also return null from toString(); nothing about the value can be rendered then,
            // so name the type instead. Dereferencing it here would replace the documented
            // ArithmeticException with a NullPointerException raised from inside the very diagnostic that
            // exists to keep such a value safe. The explanation is appended outside the budget so that a
            // long class name truncates the name rather than the part that says what happened.
            final String className = num.getClass().getName();

            return previewForErrorMessage(className, 0, className.length(), MAX_NUMBER_ERROR_TEXT_LENGTH) + " (toString() returned null)";
        }

        return previewForErrorMessage(text, 0, text.length(), MAX_NUMBER_ERROR_TEXT_LENGTH);
    }

    private static final Map<Class<?>, Map<Class<?>, UnaryOperator<Number>>> numberConverterFuncMap = new HashMap<>();

    /**
     * The conversion to apply for each built-in target when the source's exact class is not one of the eight
     * {@link #numberConverterFuncMap} knows &mdash; an {@code AtomicInteger}, a {@code LongAdder}, a
     * {@code DoubleAdder}, an application's own fixed-point type. <b>Every</b> built-in target has an entry,
     * so {@link #convert(Number, Class)} applies the same documented rule to such a source as to a supported
     * one; only a target outside the built-in set falls through to that target's own {@code String} parser.
     *
     * <p>The integral entries are also the narrowing functions the table itself uses for its lossy source
     * types, so the truncate-then-range-check rule is stated once rather than once per (target, source) pair
     * with three different idioms, which is how it used to be written.</p>
     */
    private static final Map<Class<?>, UnaryOperator<Number>> unknownSourceConverterByTarget = new HashMap<>();

    static {
        // Widening conversions between integral primitives cannot overflow, so they stay direct method
        // references; every lossy source goes through the shared narrowing function.
        final UnaryOperator<Number> toByteFunc = it -> (byte) toLongWithinRange(it, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte");
        final UnaryOperator<Number> toShortFunc = it -> (short) toLongWithinRange(it, Short.MIN_VALUE, Short.MAX_VALUE, "short");
        final UnaryOperator<Number> toIntFunc = it -> (int) toLongWithinRange(it, Integer.MIN_VALUE, Integer.MAX_VALUE, "int");
        final UnaryOperator<Number> toLongFunc = it -> toLongWithinRange(it, Long.MIN_VALUE, Long.MAX_VALUE, "long");

        final UnaryOperator<Number> toBigDecimalFunc = it -> unknownNumberToBigDecimal(it, "BigDecimal");
        final UnaryOperator<Number> toBigIntegerFunc = Numbers::unknownNumberToBigInteger;

        unknownSourceConverterByTarget.put(byte.class, toByteFunc);
        unknownSourceConverterByTarget.put(Byte.class, toByteFunc);
        unknownSourceConverterByTarget.put(short.class, toShortFunc);
        unknownSourceConverterByTarget.put(Short.class, toShortFunc);
        unknownSourceConverterByTarget.put(int.class, toIntFunc);
        unknownSourceConverterByTarget.put(Integer.class, toIntFunc);
        unknownSourceConverterByTarget.put(long.class, toLongFunc);
        unknownSourceConverterByTarget.put(Long.class, toLongFunc);
        unknownSourceConverterByTarget.put(float.class, Number::floatValue);
        unknownSourceConverterByTarget.put(Float.class, Number::floatValue);
        unknownSourceConverterByTarget.put(double.class, Number::doubleValue);
        unknownSourceConverterByTarget.put(Double.class, Number::doubleValue);
        unknownSourceConverterByTarget.put(BigInteger.class, toBigIntegerFunc);
        unknownSourceConverterByTarget.put(BigDecimal.class, toBigDecimalFunc);

        Map<Class<?>, UnaryOperator<Number>> temp = new HashMap<>();
        temp.put(byte.class, UnaryOperator.identity());
        temp.put(short.class, toByteFunc);
        temp.put(int.class, toByteFunc);
        temp.put(long.class, toByteFunc);
        temp.put(float.class, toByteFunc);
        temp.put(double.class, toByteFunc);
        temp.put(BigInteger.class, toByteFunc);
        temp.put(BigDecimal.class, toByteFunc);

        numberConverterFuncMap.put(byte.class, temp);

        // ================ for short.class

        temp = new HashMap<>();
        temp.put(byte.class, Number::shortValue);
        temp.put(short.class, UnaryOperator.identity());
        temp.put(int.class, toShortFunc);
        temp.put(long.class, toShortFunc);
        temp.put(float.class, toShortFunc);
        temp.put(double.class, toShortFunc);
        temp.put(BigInteger.class, toShortFunc);
        temp.put(BigDecimal.class, toShortFunc);

        numberConverterFuncMap.put(short.class, temp);

        // ================ for int.class

        temp = new HashMap<>();
        temp.put(byte.class, Number::intValue);
        temp.put(short.class, Number::intValue);
        temp.put(int.class, UnaryOperator.identity());
        temp.put(long.class, toIntFunc);
        temp.put(float.class, toIntFunc);
        temp.put(double.class, toIntFunc);
        temp.put(BigInteger.class, toIntFunc);
        temp.put(BigDecimal.class, toIntFunc);

        numberConverterFuncMap.put(int.class, temp);

        // ================ for long.class

        temp = new HashMap<>();
        temp.put(byte.class, Number::longValue);
        temp.put(short.class, Number::longValue);
        temp.put(int.class, Number::longValue);
        temp.put(long.class, UnaryOperator.identity());
        temp.put(float.class, toLongFunc);
        temp.put(double.class, toLongFunc);
        temp.put(BigInteger.class, toLongFunc);
        temp.put(BigDecimal.class, toLongFunc);

        numberConverterFuncMap.put(long.class, temp);

        // ============================for float
        temp = new HashMap<>();

        temp.put(byte.class, Number::floatValue);
        temp.put(short.class, Number::floatValue);
        temp.put(int.class, Number::floatValue);
        temp.put(long.class, Number::floatValue);

        temp.put(float.class, UnaryOperator.identity());

        temp.put(double.class, Number::floatValue);

        temp.put(BigInteger.class, it -> {
            // A magnitude beyond the float range saturates to +-Infinity (IEEE-754), consistent with Numbers.toFloat.
            //noinspection UnnecessaryBoxing
            return Float.valueOf(it.floatValue());
        });

        temp.put(BigDecimal.class, it -> {
            // A magnitude beyond the float range saturates to +-Infinity (IEEE-754), consistent with Numbers.toFloat.
            //noinspection UnnecessaryBoxing
            return Float.valueOf(it.floatValue());
        });

        numberConverterFuncMap.put(float.class, temp);

        // ============================for double
        temp = new HashMap<>();

        temp.put(byte.class, Number::doubleValue);
        temp.put(short.class, Number::doubleValue);
        temp.put(int.class, Number::doubleValue);
        temp.put(long.class, Number::doubleValue);

        // The same decimal-spelling widening toDouble(Object) applies; one helper so the two cannot drift.
        temp.put(float.class, it -> floatToDoubleViaDecimal(it.floatValue()));

        temp.put(double.class, UnaryOperator.identity());

        temp.put(BigInteger.class, it -> {
            // A magnitude beyond the double range saturates to +-Infinity (IEEE-754), consistent with Numbers.toDouble.
            //noinspection UnnecessaryBoxing
            return Double.valueOf(it.doubleValue());
        });

        temp.put(BigDecimal.class, it -> {
            // A magnitude beyond the double range saturates to +-Infinity (IEEE-754), consistent with Numbers.toDouble.
            //noinspection UnnecessaryBoxing
            return Double.valueOf(it.doubleValue());
        });

        numberConverterFuncMap.put(double.class, temp);

        // ============================for BigInteger
        temp = new HashMap<>();

        temp.put(byte.class, it -> BigInteger.valueOf(it.byteValue()));
        temp.put(short.class, it -> BigInteger.valueOf(it.shortValue()));
        temp.put(int.class, it -> BigInteger.valueOf(it.intValue()));
        temp.put(long.class, it -> BigInteger.valueOf(it.longValue()));
        // An integral target has no decimal-spelling question: the answer is the source's value truncated
        // toward zero, so take the EXACT binary value -- see doubleToBigInteger, which is also what the
        // unknown-Number fallback applies to a doubleValue(). A float widens to double exactly.
        temp.put(float.class, it -> doubleToBigInteger(it.floatValue(), it, null));
        temp.put(double.class, it -> doubleToBigInteger(it.doubleValue(), it, null));
        temp.put(BigInteger.class, UnaryOperator.identity());
        temp.put(BigDecimal.class, it -> bigDecimalToBigInteger((BigDecimal) it));

        numberConverterFuncMap.put(BigInteger.class, temp);

        // ============================for BigDecimal
        temp = new HashMap<>();

        temp.put(byte.class, it -> BigDecimal.valueOf(it.byteValue()));
        temp.put(short.class, it -> BigDecimal.valueOf(it.shortValue()));
        temp.put(int.class, it -> BigDecimal.valueOf(it.intValue()));
        temp.put(long.class, it -> BigDecimal.valueOf(it.longValue()));
        temp.put(float.class, it -> {
            final float f = it.floatValue();

            if (Float.isNaN(f) || Float.isInfinite(f)) {
                throw numberOverflow("BigDecimal", it);
            }

            return new BigDecimal(Float.toString(f));
        });
        temp.put(double.class, it -> {
            final double d = it.doubleValue();

            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw numberOverflow("BigDecimal", it);
            }

            return BigDecimal.valueOf(d);
        });
        temp.put(BigInteger.class, it -> new BigDecimal((BigInteger) it));
        temp.put(BigDecimal.class, UnaryOperator.identity());

        numberConverterFuncMap.put(BigDecimal.class, temp);

        // =================================================================
        final BiMap<Class<?>, Class<?>> p2w = new BiMap<>();

        p2w.put(byte.class, Byte.class);
        p2w.put(short.class, Short.class);
        p2w.put(int.class, Integer.class);
        p2w.put(long.class, Long.class);
        p2w.put(float.class, Float.class);
        p2w.put(double.class, Double.class);

        final List<Class<?>> keys = new ArrayList<>(numberConverterFuncMap.keySet());

        for (final Class<?> cls : keys) {
            temp = numberConverterFuncMap.get(cls);

            final List<Class<?>> keys2 = new ArrayList<>(temp.keySet());

            for (final Class<?> cls2 : keys2) {
                if (p2w.containsKey(cls2)) {
                    temp.put(p2w.get(cls2), temp.get(cls2));
                }
            }

            if (p2w.containsKey(cls)) {
                numberConverterFuncMap.put(p2w.get(cls), temp);
            }
        }
    }

    /**
     * Converts the given number to the specified target type with overflow checking.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double)
     * and their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If a conversion to an integer type would overflow, an {@code ArithmeticException} is thrown; a
     * {@code float}/{@code double} target instead saturates to {@code ±Infinity} (IEEE-754 semantics).</p>
     *
     * <p>For the directly supported source types, converting a finite fractional value to an integral target
     * ({@code byte}, {@code short}, {@code int}, {@code long}, or {@code BigInteger}) truncates the fractional
     * part toward zero. For a bounded integral target, overflow is determined from that truncated value; for
     * example, {@code 123.45} becomes {@code 123} and {@code -0.9} becomes {@code 0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // A fractional value is truncated toward zero, then range-checked
     * Integer result = Numbers.convert(123.45, Integer.class);                              // returns 123
     * Long longValue = Numbers.convert(new BigInteger("9223372036854775807"), Long.class);  // returns 9223372036854775807L
     * Double widened = Numbers.convert(7, Double.class);                                    // returns 7.0
     *
     * // Edge cases: null yields the target type's default; an out-of-range or non-finite
     * // value throws for an integer/BigInteger target but saturates for float/double
     * Byte byteValue = Numbers.convert(null, Byte.class);          // returns null
     * byte primByteValue = Numbers.convert(null, byte.class);      // returns 0
     * Numbers.convert(1000, Byte.class);                           // throws ArithmeticException (outside byte range)
     * Numbers.convert(Double.NaN, Integer.class);                  // throws ArithmeticException (not representable)
     * Float saturated = Numbers.convert(1e300, Float.class);       // returns Infinity (IEEE-754 saturation)
     * Numbers.convert(1, Number.class);                            // throws IllegalArgumentException (unusable target type)
     * Numbers.convert(null, Number.class);                         // returns null (a null value is never checked against the target)
     * }</pre>
     *
     * <p><b>Note:</b> overflow handling depends on the target type. For an integer target
     * ({@code byte}/{@code short}/{@code int}/{@code long}), an out-of-range value — including {@code NaN} and
     * {@code ±Infinity} — throws {@link ArithmeticException}
     * (consistent with {@code toByte}/{@code toShort}/{@code toInt}/{@code toLong}). For a {@code float}/{@code double}
     * target, a magnitude beyond the type's range saturates to {@code ±Infinity} (IEEE-754 semantics, consistent with
     * {@link #toFloat(Object)}/{@link #toDouble(Object)}), and {@code NaN}/{@code ±Infinity} inputs are preserved —
     * no exception is thrown. For a {@code BigInteger}/{@code BigDecimal} target, a {@code NaN}/{@code ±Infinity} input
     * is not representable and throws {@link ArithmeticException} (it is rejected as overflow, like the integer
     * targets — only the {@code float}/{@code double} targets preserve a non-finite value).</p>
     *
     * <p><b>Note:</b> because {@code Integer.MAX_VALUE}/{@code Long.MAX_VALUE} are not exactly representable in
     * {@code float} (nor {@code Long.MAX_VALUE} in {@code double}), a value such as {@code (float) Integer.MAX_VALUE}
     * actually equals {@code 2^31} (i.e. {@code MAX_VALUE + 1}) and is therefore treated as out of range — it throws
     * {@link ArithmeticException}, consistent with {@link #toInt(Object)}/{@link #toLong(Object)} (it does NOT
     * saturate to {@code MAX_VALUE}).</p>
     *
     * <p><b>Note:</b> the directly supported {@code Number} types (the primitive wrappers, {@code BigInteger},
     * and {@code BigDecimal}) are handled by built-in conversion rules, and so is <em>any other</em>
     * {@code Number} subtype (for example {@code AtomicInteger}, {@code LongAdder}, {@code DoubleAdder}, or a
     * custom subclass) for every one of those targets. Targeting {@code byte}/{@code short}/{@code int}/{@code long}
     * or {@code BigInteger}, such a value is truncated toward zero and range-checked by exactly the same rule as
     * a supported type, so a fractional value converts rather than failing; targeting {@code float}/{@code double}
     * it uses the subtype's own {@code floatValue()}/{@code doubleValue()}. For an integral or
     * arbitrary-precision target its value is recovered from its
     * <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. Those targets all
     * accept the same texts, and each applies to the {@code doubleValue()} fallback exactly the rule it applies
     * to a {@code Double} source, so no two of them can answer differently for the same source; only the
     * {@code float}/{@code double} targets, which never read the text, can differ from them for a subtype
     * whose {@code toString()} and {@code doubleValue()} disagree. Only a target outside that set &mdash; some other {@code Number} class entirely &mdash; is
     * converted through the source's string form and the target type's parser; a subtype whose {@code toString()}
     * returns {@code null} or an empty string has no string form and is then converted as {@code Double.valueOf(value.doubleValue())}
     * would be, the same fallback the built-in targets apply, so a non-{@code null} source never yields
     * {@code null}. A value outside a standard bounded
     * integer target still throws {@link ArithmeticException}; {@link NumberFormatException} means that such a
     * target's parser cannot parse the source's string form; and {@link IllegalArgumentException} means that the
     * target type has no such parser at all, so no value of it can be created.</p>
     *
     * <p><b>Note:</b> the two arbitrary-precision targets read a {@code Float}/{@code Double} source
     * differently, because they are asking different questions.
     * A {@code BigDecimal} target is a decimal rendering, so it uses the value's canonical decimal string
     * ({@link Float#toString(float)} or {@link Double#toString(double)} / {@link BigDecimal#valueOf(double)}),
     * not the exact binary significand of {@code new BigDecimal(double)}: {@code convert(1.21f, BigDecimal.class)}
     * is {@code 1.21}, not {@code 1.21000003814697265625}. A {@code BigInteger} target has no spelling to
     * choose, only a value, so it truncates the <em>exact</em> value toward zero exactly as the
     * {@code byte}/{@code short}/{@code int}/{@code long} targets do &mdash;
     * {@code convert((float) Integer.MAX_VALUE, BigInteger.class)} is {@code 2147483648}, the same answer as
     * {@code convert((float) Integer.MAX_VALUE, Long.class)}. One consequence: above 2<sup>24</sup> for a
     * {@code float} (2<sup>53</sup> for a {@code double}), where the shortest round-tripping decimal is no
     * longer the exact value, {@code convert(v, BigDecimal.class).toBigInteger()} can differ from
     * {@code convert(v, BigInteger.class)}; the latter is the value.</p>
     *
     * <p><b>By design:</b> a {@code Float} source reaches a {@code Double} target through that same canonical
     * decimal string ({@link Double#parseDouble(String)} on {@link Float#toString(float)}), not IEEE-754
     * widening: {@code convert(1.21f, Double.class)} is {@code 1.21}, not {@code 1.2100000381469727}, which
     * matches {@link #toDouble(Object)} and the {@code BigDecimal} target. The opposite direction is a plain
     * IEEE-754 narrowing ({@code floatValue()}, ties to even), as in {@link #toFloat(Object)}: the shortest
     * decimal spelling of a {@code double} already narrows to the same {@code float} as the value itself
     * except when the value is exactly halfway between two floats, so a decimal round trip there would only
     * cost time and break ties-to-even. The {@code Float}&rarr;{@code Double} round trip costs roughly two
     * orders of magnitude more than the corresponding cast.</p>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the class object representing the target type
     * @return the converted number as an instance of the target type, or the default value of the target type
     *         if the input value is null; a {@code null} value takes that default without {@code targetType}
     *         being checked for usability, so {@code convert(null, Number.class)} is {@code null}
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if, for a non-null {@code value}, {@code targetType} is not a
     *         supported conversion target &mdash; that is, it is neither one of the built-in numeric conversions nor
     *         a {@code Number} type with a public {@code String} factory method or constructor (for example
     *         {@code Number} itself, or any other abstract {@code Number} subtype)
     * @throws NumberFormatException if a conversion routed through the target type's string parser cannot parse
     *         the source's string form; the message embeds only a bounded, escaped preview of that form, as
     *         every other parse failure reported by this class does
     * @throws ArithmeticException if the conversion to an integer ({@code byte}/{@code short}/{@code int}/{@code long})
     *         target would overflow, including a {@code NaN}/{@code ±Infinity} input; or if a {@code NaN}/{@code ±Infinity}
     *         value is converted to a {@code BigInteger} or
     *         {@code BigDecimal} target (a {@code float}/{@code double} target saturates to {@code ±Infinity} instead of throwing);
     *         or if a {@code BigInteger} result exceeds the JDK implementation's supported magnitude, as
     *         {@code convert(new BigDecimal("1e2147483647"), BigInteger.class)} does (a {@code BigDecimal}
     *         target has no such limit)
     * @see N#convert(Object, Class)
     */
    @MayReturnNull
    public static <T extends Number> T convert(final Number value, final Class<? extends T> targetType)
            throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (value == null) {
            return N.defaultValueOf(targetType);
        }

        final Number converted = applyBuiltInConversion(value, targetType);

        if (converted != null) {
            return (T) converted;
        }

        final String text = stringFormForTargetParser(value);

        try {
            return N.valueOf(text, targetType);
        } catch (final UnsupportedOperationException e) {
            throw unsupportedTargetType(targetType, e);
        } catch (final NumberFormatException e) {
            // The target's own parser embeds the whole source text in its message ("For input string: ...").
            // That text is a Number's rendering, which an application subtype can make arbitrarily long or
            // control-character-laden, so it gets the same bounded, escaped preview as every other parse
            // failure this class reports (a 100 KB toString() produced a 100 KB message here).
            throw notAValidNumber(Strings.nullToEmpty(text), targetType.getSimpleName(), e);
        }
    }

    /**
     * The {@link BigDecimal#scale()} above which {@link #bigDecimalToBigInteger(BigDecimal)} pre-empts
     * {@link BigDecimal#toBigInteger()} for a magnitude below one. Measured on {@code 1e-scale}, the plain
     * call costs 0.02 ms at scale 10 and 0.4 ms at 10<sup>4</sup>, then 10 ms at 10<sup>5</sup>, 156 ms at
     * 10<sup>6</sup>, 1.8 s at 10<sup>7</sup> and 37 s at 10<sup>8</sup> &mdash; it rescales by building
     * 10<sup>scale</sup>. Below this bound the plain call is cheaper than the check itself.
     */
    private static final int MAX_CHEAP_RESCALE_SCALE = 10_000;

    /**
     * Truncates {@code bd} toward zero to a {@code BigInteger}, short-circuiting the magnitudes below one that
     * {@link BigDecimal#toBigInteger()} answers expensively or not at all.
     *
     * <p>{@code toBigInteger()} rescales to zero, which for a large positive scale means dividing by
     * 10<sup>scale</sup> &mdash; a power it has to build first. So the case whose answer is most trivial was
     * the most expensive one: {@code new BigDecimal("1e-100000000")} took <b>37 s</b> to return {@code 0}, and
     * {@code new BigDecimal("1e-2147483647")} did not return at all, throwing
     * {@code ArithmeticException("BigInteger would overflow supported range")} for a value smaller than one.
     * That made the {@code BigInteger} target disagree with every other integral target on the same source,
     * which all answer {@code 0} &mdash; the same defect this class fixed for the {@code float}/{@code double}
     * sources.</p>
     *
     * <p>A non-zero {@code BigDecimal} has {@code precision - scale} digits before the decimal point, so
     * {@code |bd| < 1} exactly when that count is not positive. The subtraction is widened to {@code long}
     * defensively rather than out of necessity: the scale gate below already restricts it to a large
     * <em>positive</em> scale, where {@code precision - scale} cannot leave the {@code int} range. Without
     * that gate it could &mdash; a large <em>negative</em> scale (from a huge positive exponent, e.g.
     * {@code "1e2147483647"}) would overflow and misreport a colossal value as a sub-one one &mdash; so the
     * widening stays, to keep the guard correct on its own terms if the gate is ever loosened.</p>
     *
     * <p>The check is gated on {@link #MAX_CHEAP_RESCALE_SCALE} because {@link BigDecimal#precision()} is
     * itself {@code O(digits)} for a wide unscaled value, and paying it on every conversion measured worse
     * than the case it avoids. <b>Correctness does not depend on the gate</b>, only cost: a sub-one value
     * below the bound simply takes the plain path, which still returns zero.</p>
     *
     * @param bd the value to truncate; must not be {@code null}
     * @return {@code bd} truncated toward zero
     * @throws ArithmeticException if the truncated value genuinely exceeds the JDK implementation's supported
     *         {@code BigInteger} magnitude
     */
    private static BigInteger bigDecimalToBigInteger(final BigDecimal bd) throws ArithmeticException {
        if (bd.scale() > MAX_CHEAP_RESCALE_SCALE && (bd.signum() == 0 || bd.precision() - (long) bd.scale() <= 0)) {
            return BigInteger.ZERO;
        }

        return bd.toBigInteger();
    }

    /**
     * Applies the built-in numeric conversion for {@code value} to {@code targetType}, or returns {@code null}
     * when there is none and the caller must fall back to the target type's string parser.
     *
     * <p>A {@code Number} subtype the table does not know still reaches a built-in conversion for
     * <em>every</em> built-in target, through {@link #unknownSourceConverterByTarget}, so {@code convert}
     * applies the rule its Javadoc promises rather than whatever the target type's {@code String} factory
     * happens to accept. Only a target outside the built-in set returns {@code null} here.</p>
     */
    @MayReturnNull
    private static Number applyBuiltInConversion(final Number value, final Class<?> targetType) {
        final Map<Class<?>, UnaryOperator<Number>> temp = numberConverterFuncMap.get(targetType);
        final UnaryOperator<Number> func = temp == null ? null : temp.get(value.getClass());

        if (func != null) {
            return func.apply(value);
        }

        final UnaryOperator<Number> unknownSource = unknownSourceConverterByTarget.get(targetType);

        return unknownSource == null ? null : unknownSource.apply(value);
    }

    /**
     * Builds the failure for a {@code targetType} this class cannot produce: it is neither one of the built-in
     * numeric conversions nor a {@code Number} type with a public {@code String} factory method or constructor
     * (for example {@code Number} itself, or any other abstract {@code Number} subtype). Reported as
     * {@link IllegalArgumentException} so that an unusable {@code targetType} and a {@code null}
     * {@code targetType} raise the same exception type, and the underlying
     * {@link UnsupportedOperationException} is retained as the cause.
     *
     * @param targetType the unusable conversion target
     * @param cause the failure raised by the target type's string parser
     * @return the {@link IllegalArgumentException} to throw
     */
    private static IllegalArgumentException unsupportedTargetType(final Class<?> targetType, final UnsupportedOperationException cause) {
        final IllegalArgumentException iae = new IllegalArgumentException(
                "Unsupported target type: " + targetType.getName() + " cannot be created from the source value's string form");
        iae.initCause(cause);
        return iae;
    }

    /**
     * Returns the string form a conversion routed through the target type's own parser reads.
     *
     * <p>Normally that is the source's rendering, {@link N#stringOf(Object)}. A {@code Number} subtype may
     * legally return {@code null} <i>or the empty string</i> from {@code toString()}; it then has no string
     * form, and handing that to the target's parser silently produced a {@code null} result for a
     * non-{@code null} source (and skipped the unusable-target check, so {@code convert(x, Number.class)}
     * returned {@code null} instead of throwing). Such a source is rendered instead as
     * {@code Double.valueOf(x.doubleValue())} would be, which is the fallback every built-in target applies to
     * it (see the class-level <a href="#unknown-number-recovery">recovery rules</a>); the target's parser, or
     * its absence, then decides the outcome exactly as for a {@code Double} source.</p>
     *
     * <p>The test is emptiness, not just nullity, because that is the predicate the {@code Type} layer this
     * text is handed to applies: {@code AtomicLongType.valueOf("")} and {@code NumberType.valueOf("")} both
     * answer {@code null} <i>before</i> reaching the parser that would have thrown, so an empty rendering
     * reproduced the exact defect a {@code null} one used to.</p>
     *
     * @param value the non-{@code null} source
     * @return the text to hand to the target type's parser; never {@code null}
     */
    private static String stringFormForTargetParser(final Number value) {
        final String text = N.stringOf(value);

        return Strings.isEmpty(text) ? N.stringOf(Double.valueOf(value.doubleValue())) : text;
    }

    /**
     * Converts the given number to the specified target type with overflow checking.
     * If the input value is {@code null}, returns the provided default value.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double)
     * and their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If a conversion to an integer type would overflow, an {@code ArithmeticException} is thrown; a
     * {@code float}/{@code double} target instead saturates to {@code ±Infinity} (IEEE-754 semantics).</p>
     *
     * <p>For non-null input, this overload follows {@link #convert(Number, Class)}, including truncation toward
     * zero when a directly supported finite fractional value is converted to an integral target and string-parser
     * handling for other {@code Number} subtypes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // The default is used only for a null value; a present value converts as usual
     * Integer converted = Numbers.convert(123.45, Integer.class, -1);   // returns 123
     * Long widened = Numbers.convert(7, Long.class, 0L);                // returns 7L
     *
     * // Edge cases: null takes the supplied default, but an out-of-range value still throws
     * Integer result = Numbers.convert(null, Integer.class, -1);        // returns -1
     * Long longValue = Numbers.convert(null, Long.class, 0L);           // returns 0L
     * Numbers.convert(new BigInteger("1000"), Byte.class, (byte) 0);    // throws ArithmeticException (outside byte range)
     * }</pre>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the class object representing the target type
     * @param defaultValue the value to return if the input value is null
     * @return the converted number as an instance of the target type, or the specified default value if the
     *         input value is null (returned without {@code targetType} being checked for usability)
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if, for a non-null {@code value}, {@code targetType} is not a
     *         supported conversion target &mdash; that is, it is neither one of the built-in numeric conversions nor
     *         a {@code Number} type with a public {@code String} factory method or constructor (for example
     *         {@code Number} itself, or any other abstract {@code Number} subtype)
     * @throws NumberFormatException if a conversion routed through the target type's string parser cannot parse
     *         the source's string form; the message embeds only a bounded, escaped preview of that form, as
     *         every other parse failure reported by this class does
     * @throws ArithmeticException if the conversion to an integer target type would overflow, including a
     *         {@code NaN}/{@code ±Infinity} input; or if a {@code NaN}/{@code ±Infinity} value is converted to a
     *         {@code BigInteger} or {@code BigDecimal} target (a {@code float}/{@code double} target saturates to
     *         {@code ±Infinity} instead of throwing); or if a {@code BigInteger} result exceeds the JDK
     *         implementation's supported magnitude
     * @see #convert(Number, Class)
     */
    public static <T extends Number> T convert(final Number value, final Class<? extends T> targetType, final T defaultValue)
            throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (value == null) {
            return defaultValue;
        }

        return convert(value, targetType);
    }

    /**
     * Converts the given number to the specified target type using the provided Type instance.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double),
     * as well as their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If a conversion to an integer type would overflow, an {@code ArithmeticException} is thrown; a
     * {@code float}/{@code double} target instead saturates to {@code ±Infinity} (IEEE-754 semantics).</p>
     *
     * <p>For the directly supported source types, converting a finite fractional value to an integral target
     * ({@code byte}, {@code short}, {@code int}, {@code long}, or {@code BigInteger}) truncates the fractional
     * part toward zero. For a bounded integral target, overflow is determined from that truncated value; for
     * example, {@code 123.45} becomes {@code 123} and {@code -0.9} becomes {@code 0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // A fractional value is truncated toward zero, then range-checked
     * Type<Integer> intType = Type.of(Integer.class);
     * Integer result        = Numbers.convert(123.45, intType);                              // returns 123
     * Type<Long> longType   = Type.of(Long.class);
     * Long longValue        = Numbers.convert(new BigInteger("9223372036854775807"), longType);   // returns 9223372036854775807L
     *
     * // Edge cases: null yields the Type's default value; out of range still throws
     * Type<Byte> byteType = Type.of(Byte.class);
     * Byte byteValue      = Numbers.convert(null, byteType);            // returns null
     * Numbers.convert(1000, byteType);                                  // throws ArithmeticException (outside byte range)
     * }</pre>
     *
     * <p><b>Note:</b> overflow handling depends on the target type, exactly as for
     * {@link #convert(Number, Class)} &mdash; both overloads share one dispatch table. For an integer target
     * ({@code byte}/{@code short}/{@code int}/{@code long}), an out-of-range value &mdash; including {@code NaN}
     * and {@code ±Infinity} &mdash; throws {@link ArithmeticException}. For a {@code float}/{@code double}
     * target, a magnitude beyond the type's range saturates to {@code ±Infinity} and {@code NaN}/{@code ±Infinity}
     * inputs are preserved. For a {@code BigInteger}/{@code BigDecimal} target, a {@code NaN}/{@code ±Infinity}
     * input is not representable and throws {@link ArithmeticException}.</p>
     *
     * <p><b>Note:</b> because {@code Integer.MAX_VALUE}/{@code Long.MAX_VALUE} are not exactly representable in
     * {@code float} (nor {@code Long.MAX_VALUE} in {@code double}), a value such as {@code (float) Integer.MAX_VALUE}
     * actually equals {@code 2^31} (i.e. {@code MAX_VALUE + 1}) and is therefore treated as out of range &mdash; it
     * throws {@link ArithmeticException} (it does NOT saturate to {@code MAX_VALUE}).</p>
     *
     * <p><b>Note:</b> the directly supported {@code Number} types (the primitive wrappers, {@code BigInteger},
     * and {@code BigDecimal}) are handled by built-in conversion rules, and so is <em>any other</em>
     * {@code Number} subtype (for example {@code AtomicInteger}, {@code LongAdder}, {@code DoubleAdder}, or a
     * custom subclass) for every one of those targets. Targeting {@code byte}/{@code short}/{@code int}/{@code long}
     * or {@code BigInteger}, such a value is truncated toward zero and range-checked by exactly the same rule as
     * a supported type, so a fractional value converts rather than failing; targeting {@code float}/{@code double}
     * it uses the subtype's own {@code floatValue()}/{@code doubleValue()}. For an integral or
     * arbitrary-precision target its value is recovered from its
     * <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. Those targets all
     * accept the same texts, and each applies to the {@code doubleValue()} fallback exactly the rule it applies
     * to a {@code Double} source, so no two of them can answer differently for the same source; only the
     * {@code float}/{@code double} targets, which never read the text, can differ from them for a subtype
     * whose {@code toString()} and {@code doubleValue()} disagree. Only a target outside that set &mdash; some other {@code Number} class entirely &mdash; is
     * converted through the source's string form and the target type's parser; a subtype whose {@code toString()}
     * returns {@code null} or an empty string has no string form and is then converted as {@code Double.valueOf(value.doubleValue())}
     * would be, the same fallback the built-in targets apply, so a non-{@code null} source never yields
     * {@code null}. A value outside a standard bounded
     * integer target still throws {@link ArithmeticException}; {@link NumberFormatException} means that such a
     * target's parser cannot parse the source's string form; and {@link IllegalArgumentException} means that the
     * target type has no such parser at all, so no value of it can be created.</p>
     *
     * <p><b>Note:</b> the two arbitrary-precision targets read a {@code Float}/{@code Double} source
     * differently, because they are asking different questions.
     * A {@code BigDecimal} target is a decimal rendering, so it uses the value's canonical decimal string
     * ({@link Float#toString(float)} or {@link Double#toString(double)} / {@link BigDecimal#valueOf(double)}),
     * not the exact binary significand of {@code new BigDecimal(double)}: {@code convert(1.21f, BigDecimal.class)}
     * is {@code 1.21}, not {@code 1.21000003814697265625}. A {@code BigInteger} target has no spelling to
     * choose, only a value, so it truncates the <em>exact</em> value toward zero exactly as the
     * {@code byte}/{@code short}/{@code int}/{@code long} targets do &mdash;
     * {@code convert((float) Integer.MAX_VALUE, BigInteger.class)} is {@code 2147483648}, the same answer as
     * {@code convert((float) Integer.MAX_VALUE, Long.class)}. One consequence: above 2<sup>24</sup> for a
     * {@code float} (2<sup>53</sup> for a {@code double}), where the shortest round-tripping decimal is no
     * longer the exact value, {@code convert(v, BigDecimal.class).toBigInteger()} can differ from
     * {@code convert(v, BigInteger.class)}; the latter is the value.</p>
     *
     * <p><b>By design:</b> a {@code Float} source reaches a {@code Double} target through that same canonical
     * decimal string, while a {@code Double} source narrows to {@code Float} by plain IEEE-754 rounding. See
     * {@link #convert(Number, Class)}.</p>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the Type object representing the target type
     * @return the converted number as an instance of the target type, or the default value of the target type
     *         if the input value is null; a {@code null} value takes that default without {@code targetType}
     *         being checked for usability, so {@code convert(null, Number.class)} is {@code null}
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if, for a non-null {@code value}, {@code targetType} is not a
     *         supported conversion target &mdash; that is, it is neither one of the built-in numeric conversions nor
     *         a {@code Number} type with a public {@code String} factory method or constructor (for example
     *         {@code Number} itself, or any other abstract {@code Number} subtype)
     * @throws NumberFormatException if a conversion routed through the target type's string parser cannot parse
     *         the source's string form; the message embeds only a bounded, escaped preview of that form, as
     *         every other parse failure reported by this class does
     * @throws ArithmeticException if the conversion to an integer target type would overflow, including a
     *         {@code NaN}/{@code ±Infinity} input; or if a {@code NaN}/{@code ±Infinity} value is converted to a
     *         {@code BigInteger} or {@code BigDecimal} target (a {@code float}/{@code double} target saturates to
     *         {@code ±Infinity} instead of throwing); or if a {@code BigInteger} result exceeds the JDK
     *         implementation's supported magnitude
     * @see #convert(Number, Class)
     * @see N#convert(Object, com.landawn.abacus.type.Type)
     * @see com.landawn.abacus.type.Type
     */
    @MayReturnNull
    public static <T extends Number> T convert(final Number value, final Type<? extends T> targetType)
            throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (value == null) {
            return targetType.defaultValue();
        }

        final Number converted = applyBuiltInConversion(value, targetType.javaType());

        if (converted != null) {
            return (T) converted;
        }

        final String text = stringFormForTargetParser(value);

        try {
            return targetType.valueOf(text);
        } catch (final UnsupportedOperationException e) {
            throw unsupportedTargetType(targetType.javaType(), e);
        } catch (final NumberFormatException e) {
            // Bounded, escaped preview instead of the parser's own whole-text message; see convert(Number, Class).
            throw notAValidNumber(Strings.nullToEmpty(text), targetType.javaType().getSimpleName(), e);
        }
    }

    /**
     * Converts the given number to the specified target type using the provided Type instance, with a custom default value for {@code null}.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double),
     * as well as their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If a conversion to an integer type would overflow, an {@code ArithmeticException} is thrown; a
     * {@code float}/{@code double} target instead saturates to {@code ±Infinity} (IEEE-754 semantics).</p>
     *
     * <p>For non-null input, this overload follows {@link #convert(Number, Type)}, including truncation toward
     * zero when a directly supported finite fractional value is converted to an integral target and string-parser
     * handling for other {@code Number} subtypes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // The default is used only for a null value; a present value converts as usual
     * Type<Integer> intType = Type.of(Integer.class);
     * Integer converted     = Numbers.convert(123.45, intType, -1);     // returns 123
     *
     * // Edge cases: null takes the supplied default, but an out-of-range value still throws
     * Integer result      = Numbers.convert(null, intType, -1);         // returns -1
     * Type<Long> longType = Type.of(Long.class);
     * Long longValue      = Numbers.convert(null, longType, 0L);        // returns 0L
     * Type<Byte> byteType = Type.of(Byte.class);
     * Numbers.convert(new BigInteger("1000"), byteType, (byte) 0);      // throws ArithmeticException (outside byte range)
     * }</pre>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the Type object representing the target type
     * @param defaultValue the value to return if the input value is null
     * @return the converted number as an instance of the target type, or the specified default value if the
     *         input value is null (returned without {@code targetType} being checked for usability)
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if, for a non-null {@code value}, {@code targetType} is not a
     *         supported conversion target &mdash; that is, it is neither one of the built-in numeric conversions nor
     *         a {@code Number} type with a public {@code String} factory method or constructor (for example
     *         {@code Number} itself, or any other abstract {@code Number} subtype)
     * @throws NumberFormatException if a conversion routed through the target type's string parser cannot parse
     *         the source's string form; the message embeds only a bounded, escaped preview of that form, as
     *         every other parse failure reported by this class does
     * @throws ArithmeticException if the conversion to an integer target type would overflow, including a
     *         {@code NaN}/{@code ±Infinity} input; or if a {@code NaN}/{@code ±Infinity} value is converted to a
     *         {@code BigInteger} or {@code BigDecimal} target (a {@code float}/{@code double} target saturates to
     *         {@code ±Infinity} instead of throwing); or if a {@code BigInteger} result exceeds the JDK
     *         implementation's supported magnitude
     * @see #convert(Number, Type)
     * @see #convert(Number, Class, Number)
     * @see com.landawn.abacus.type.Type
     */
    public static <T extends Number> T convert(final Number value, final Type<? extends T> targetType, final T defaultValue)
            throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (value == null) {
            return defaultValue;
        }

        return convert(value, targetType);
    }

    /**
     * Formats the given int value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the integer value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(1234, "#,###");        // returns "1,234"
     * Numbers.format(1234, "0.00");         // returns "1234.00"
     * Numbers.format(1234, "$#,###.00");    // returns "$1,234.00"
     * Numbers.format(-1234, "#,###");       // returns "-1,234"
     *
     * // Edge cases
     * Numbers.format(1, "hello");           // returns "hello1" (non-pattern characters are literals)
     * Numbers.format(1234, (String) null);  // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * @param x the int value to be formatted
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the int value formatted according to the provided decimal format
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(double, String)
     * @see #format(Integer, String)
     * @see java.text.DecimalFormat#format(long)
     */
    public static String format(final int x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        return getThreadLocalDecimalFormat(decimalFormat).format(x);
    }

    /**
     * Formats the given Integer value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Integer value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p>If the {@code Integer} value is {@code null}, {@code null} is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(Integer.valueOf(1234), "#,###");       // returns "1,234"
     * Numbers.format(Integer.valueOf(1234), "0.00");        // returns "1234.00"
     * Numbers.format(Integer.valueOf(1234), "$#,###.00");   // returns "$1,234.00"
     * Numbers.format(Integer.valueOf(-1234), "#,###");      // returns "-1,234"
     *
     * // Edge cases
     * Numbers.format((Integer) null, "#,###");              // returns null
     * Numbers.format(Integer.valueOf(1), (String) null);    // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * @param x the Integer value to be formatted; if {@code null}, {@code null} is returned
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the Integer value formatted according to the provided decimal format,
     *         or {@code null} if {@code x} is {@code null}
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(int, String)
     * @see #format(Long, String)
     * @see java.text.DecimalFormat#format(long)
     */
    @MayReturnNull
    public static String format(final Integer x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        if (x == null) {
            return null;
        }

        return getThreadLocalDecimalFormat(decimalFormat).format(x.longValue());
    }

    /**
     * Formats the given long value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the long value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(123456789L, "#,###");        // returns "123,456,789"
     * Numbers.format(123456789L, "0.00");         // returns "123456789.00"
     * Numbers.format(123456789L, "$#,###.00");    // returns "$123,456,789.00"
     * Numbers.format(-123456789L, "#,###");       // returns "-123,456,789"
     *
     * // Edge cases
     * Numbers.format(1L, "hello");                // returns "hello1" (non-pattern characters are literals)
     * Numbers.format(123456789L, (String) null);  // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * @param x the long value to be formatted
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the long value formatted according to the provided decimal format
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(double, String)
     * @see #format(Long, String)
     * @see java.text.DecimalFormat#format(long)
     */
    public static String format(final long x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        return getThreadLocalDecimalFormat(decimalFormat).format(x);
    }

    /**
     * Formats the given Long value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Long value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p>If the {@code Long} value is {@code null}, {@code null} is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(Long.valueOf(123456789L), "#,###");       // returns "123,456,789"
     * Numbers.format(Long.valueOf(123456789L), "0.00");        // returns "123456789.00"
     * Numbers.format(Long.valueOf(123456789L), "$#,###.00");   // returns "$123,456,789.00"
     * Numbers.format(Long.valueOf(-123456789L), "#,###");      // returns "-123,456,789"
     *
     * // Edge cases
     * Numbers.format((Long) null, "#,###");                    // returns null
     * Numbers.format(Long.valueOf(1L), (String) null);         // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * @param x the Long value to be formatted; if {@code null}, {@code null} is returned
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the Long value formatted according to the provided decimal format,
     *         or {@code null} if {@code x} is {@code null}
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(long, String)
     * @see #format(Integer, String)
     * @see java.text.DecimalFormat#format(long)
     */
    @MayReturnNull
    public static String format(final Long x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        if (x == null) {
            return null;
        }

        return getThreadLocalDecimalFormat(decimalFormat).format(x.longValue());
    }

    /**
     * Formats the given float value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the float value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(12.105f, "0.00");      // returns "12.10"
     * Numbers.format(12.105f, "#.##");      // returns "12.1"
     * Numbers.format(0.121f, "#.##%");      // returns "12.1%"
     * Numbers.format(0.12156f, "#.##%");    // returns "12.16%"
     * Numbers.format(-12.105f, "0.00");     // returns "-12.10"
     *
     * // Edge cases
     * Numbers.format(-0.001f, "0.00");                  // returns "-0.00" (the value's sign is printed even when it rounds to zero)
     * Numbers.format(Float.NaN, "0.00");                // returns "NaN" (the locale's NaN symbol)
     * Numbers.format(Float.NEGATIVE_INFINITY, "0.00");  // returns "-∞" (the locale's infinity symbol, U+221E)
     * Numbers.format(1.0f, (String) null);              // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * <p><b>Note:</b> the {@code float} is widened to {@code double} before formatting, so a decimal
     * literal written as a {@code float} can round differently from the same literal written as a
     * {@code double}: {@code format(12.105f, "0.00")} is {@code "12.10"} because {@code 12.105f} is
     * really {@code 12.104999...}, while {@code format(12.105, "0.00")} is {@code "12.11"}.</p>
     *
     * @param x the float value to be formatted.
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null).
     * @return a string representation of the float value formatted according to the provided decimal format.
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(Float, String)
     * @see #format(double, String)
     * @see java.text.DecimalFormat#format(double)
     */
    public static String format(final float x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        return getThreadLocalDecimalFormat(decimalFormat).format(x);
    }

    /**
     * Formats the given Float value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Float value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.</p>
     *
     * <p>If the {@code Float} value is {@code null}, {@code null} is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(Float.valueOf(12.105f), "0.00");       // returns "12.10"
     * Numbers.format(Float.valueOf(12.105f), "#.##");       // returns "12.1"
     * Numbers.format(Float.valueOf(0.121f), "#.##%");       // returns "12.1%"
     * Numbers.format(Float.valueOf(0.12156f), "#.##%");     // returns "12.16%"
     * Numbers.format(Float.valueOf(-12.105f), "0.00");      // returns "-12.10"
     *
     * // Edge cases
     * Numbers.format(Float.valueOf(Float.NaN), "0.00");     // returns "NaN" (the locale's NaN symbol)
     * Numbers.format((Float) null, "0.00");                 // returns null
     * Numbers.format(Float.valueOf(1.0f), (String) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * <p><b>Note:</b> the {@code float} is widened to {@code double} before formatting, so a decimal
     * literal written as a {@code float} can round differently from the same literal written as a
     * {@code double}: {@code format(12.105f, "0.00")} is {@code "12.10"} because {@code 12.105f} is
     * really {@code 12.104999...}, while {@code format(12.105, "0.00")} is {@code "12.11"}.</p>
     *
     * @param x the Float value to be formatted; if {@code null}, {@code null} is returned.
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null).
     * @return a string representation of the Float value formatted according to the provided decimal format,
     *         or {@code null} if {@code x} is {@code null}
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(float, String)
     * @see #format(Double, String)
     * @see java.text.DecimalFormat#format(double)
     */
    @MayReturnNull
    public static String format(final Float x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        if (x == null) {
            return null;
        }

        return getThreadLocalDecimalFormat(decimalFormat).format(x.doubleValue());
    }

    /**
     * Formats the given double value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the double value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(12.105, "0.00");      // returns "12.11"
     * Numbers.format(12.105, "#.##");      // returns "12.11"
     * Numbers.format(0.121, "0.00%");      // returns "12.10%"
     * Numbers.format(0.121, "#.##%");      // returns "12.1%"
     * Numbers.format(0.12156, "0.00%");    // returns "12.16%"
     * Numbers.format(0.12156, "#.##%");    // returns "12.16%"
     * Numbers.format(-12.105, "0.00");     // returns "-12.11"
     *
     * // Edge cases
     * Numbers.format(-0.001, "0.00");                    // returns "-0.00" (the value's sign is printed even when it rounds to zero)
     * Numbers.format(Double.NaN, "0.00");                // returns "NaN" (the locale's NaN symbol)
     * Numbers.format(Double.POSITIVE_INFINITY, "0.00");  // returns "∞" (the locale's infinity symbol, U+221E)
     * Numbers.format(1.0, (String) null);                // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * @param x the double value to be formatted.
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null).
     * @return a string representation of the double value formatted according to the provided decimal format.
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(Double, String)
     * @see #format(int, String)
     * @see #format(long, String)
     * @see java.text.DecimalFormat#format(double)
     */
    public static String format(final double x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        return getThreadLocalDecimalFormat(decimalFormat).format(x);
    }

    /**
     * Formats the given Double value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Double value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.</p>
     *
     * <p>If the {@code Double} value is {@code null}, {@code null} is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.format(Double.valueOf(12.105), "0.00");       // returns "12.11"
     * Numbers.format(Double.valueOf(12.105), "#.##");       // returns "12.11"
     * Numbers.format(Double.valueOf(0.121), "#.##%");       // returns "12.1%"
     * Numbers.format(Double.valueOf(0.12156), "#.##%");     // returns "12.16%"
     * Numbers.format(Double.valueOf(-12.105), "0.00");      // returns "-12.11"
     *
     * // Edge cases
     * Numbers.format(Double.valueOf(Double.NaN), "0.00");   // returns "NaN" (the locale's NaN symbol)
     * Numbers.format((Double) null, "0.00");                // returns null
     * Numbers.format(Double.valueOf(1.0), (String) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Note:</b> locale sensitivity, the per-thread pattern cache, the
     * {@link java.math.RoundingMode#HALF_EVEN} rounding mode, literal (non-pattern) characters and invalid
     * patterns are all covered by the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * @param x the Double value to be formatted; if {@code null}, {@code null} is returned.
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null).
     * @return a string representation of the Double value formatted according to the provided decimal format,
     *         or {@code null} if {@code x} is {@code null}
     * @throws IllegalArgumentException if {@code decimalFormat} is {@code null} or is a syntactically illegal {@link java.text.DecimalFormat} pattern (for example an unmatched quote)
     * @see #format(double, String)
     * @see #format(Integer, String)
     * @see #format(Long, String)
     * @see java.text.DecimalFormat#format(double)
     */
    @MayReturnNull
    public static String format(final Double x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        if (x == null) {
            return null;
        }

        return getThreadLocalDecimalFormat(decimalFormat).format(x.doubleValue());
    }

    /**
     * Extracts the first integer value found in the given string.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstInteger(String)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of an integer value.
     * It uses a regular expression pattern to identify integer patterns, including negative numbers.
     * If no integer is found in the string, an empty OptionalInt is returned.</p>
     *
     * <p><b>By design:</b> a matched digit run that does not fit in an {@code int} throws
     * {@code NumberFormatException}. Empty means “no integer token found”, not “token present but out of range”.
     * {@code extractFirstInt("id=99999999999")} throws rather than returning empty.</p>
     *
     * <p><b>By design &mdash; exception type differs from the {@code to*} family:</b> this family reports an
     * out-of-range token as {@code NumberFormatException}, whereas {@link #toInt(String)} reports the same
     * condition as {@code ArithmeticException} (it reserves {@code NumberFormatException} for a malformed
     * token). The two are answering different questions: {@code toInt} is handed a string that is supposed to
     * be a number, so "malformed" and "out of range" are worth separating; the {@code extractFirst*} family
     * went looking inside arbitrary text, where the only failure worth naming is that the thing it found is
     * not usable as one. A caller that catches {@code ArithmeticException} for "out of range" will therefore
     * not catch it here &mdash; catch {@code NumberFormatException}, or use
     * {@link #extractFirstLong(String)} when the wider range is what you need.</p>
     *
     * <p><b>Radix:</b> decimal only. The matched token is an optional sign followed by ASCII digits, so a
     * {@code 0x}/{@code 0X}/{@code #} prefix is never honoured here &mdash; the scan simply stops at the
     * non-digit. {@code extractFirstInt("0x1F")} is {@code 0}, the leading digit run, where
     * {@link #toInt(String)} is {@code 31}. A leading zero is decimal padding, never octal
     * ({@code extractFirstInt("010")} is {@code 10}).</p>
     *
     * <p>A sign is attached only when it directly precedes the digits: {@code extractFirstInt("-.5")}
     * returns {@code 5}, not {@code -5}, because the {@code -} is not followed by a digit and is skipped
     * (unlike {@link #extractFirstDouble(String)}, which keeps the sign in that case).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstInt("abc123def");        // returns OptionalInt.of(123)
     * Numbers.extractFirstInt("price: $45.99");    // returns OptionalInt.of(45)
     * Numbers.extractFirstInt("total: -10");       // returns OptionalInt.of(-10)
     *
     * // Edge cases
     * Numbers.extractFirstInt("no numbers");       // returns OptionalInt.empty()
     * Numbers.extractFirstInt("-.5");              // returns OptionalInt.of(5) (the sign is skipped: it does not touch the digits)
     * Numbers.extractFirstInt("");                 // returns OptionalInt.empty()
     * Numbers.extractFirstInt(null);               // returns OptionalInt.empty()
     * Numbers.extractFirstInt("id=99999999999");   // throws NumberFormatException (a matched token outside int range)
     * }</pre>
     *
     * @param str the string to extract the int value from (may be {@code null} or empty).
     * @return the extracted int value wrapped in an OptionalInt, or an empty OptionalInt if no int value is found or if the input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched digit run represents a value outside the {@code int} range;
     *         the underlying pattern matches digit runs of any length, so for example {@code extractFirstInt("id=99999999999")} throws rather than returning empty
     * @see #extractFirstIntOrElse(String, int)
     * @see #extractFirstLong(String)
     * @see Strings#findFirstInteger(String)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#INTEGER_FINDER
     */
    // @ai-ignore review 2026-08-31: the extractFirst* family reporting an out-of-range token as
    // NumberFormatException, where the to* family reports ArithmeticException for the same condition, is
    // settled - by design / won't-fix. Documented in the "exception type differs from the to* family" block
    // above. Do not propose unifying the two families on one exception type.
    public static u.OptionalInt extractFirstInt(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return u.OptionalInt.empty();
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);

        if (matcher.find()) {
            return u.OptionalInt.of(parseExtractedInt(str, matcher.start(1), matcher.end(1)));
        }

        return u.OptionalInt.empty();
    }

    /**
     * Extracts the first integer value found in the given string, or returns a default value if no integer is found.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstInteger(String)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of an integer value.
     * It uses a regular expression pattern to identify integer patterns, including negative numbers.
     * If no integer is found in the string, the specified default value is returned.</p>
     *
     * <p><b>By design:</b> {@code defaultValue} is used only when no integer token is found ({@code null}, empty,
     * or no match). A matched digit run that does not fit in an {@code int} still throws
     * {@code NumberFormatException}; {@code extractFirstIntOrElse("id=99999999999", 0)} throws rather than
     * returning {@code 0}. This is not a parse-failure fallback.</p>
     *
     * <p><b>Radix:</b> decimal only. The matched token is an optional sign followed by ASCII digits, so a
     * {@code 0x}/{@code 0X}/{@code #} prefix is never honoured here &mdash; the scan simply stops at the
     * non-digit. {@code extractFirstIntOrElse("0x1F", 0)} is {@code 0}, the leading digit run, where
     * {@link #toInt(String)} is {@code 31}. A leading zero is decimal padding, never octal
     * ({@code extractFirstIntOrElse("010", 0)} is {@code 10}).</p>
     *
     * <p>A sign is attached only when it directly precedes the digits: {@code extractFirstIntOrElse("-.5", 0)}
     * returns {@code 5}, not {@code -5} (see {@link #extractFirstInt(String)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstIntOrElse("abc123def", 0);        // returns 123
     * Numbers.extractFirstIntOrElse("price: $45.99", 0);    // returns 45
     * Numbers.extractFirstIntOrElse("total: -10", 0);       // returns -10
     *
     * // Edge cases: the default covers "no token found" only
     * Numbers.extractFirstIntOrElse("no numbers", 0);       // returns 0
     * Numbers.extractFirstIntOrElse("-.5", 0);              // returns 5 (the sign is skipped: it does not touch the digits)
     * Numbers.extractFirstIntOrElse("", 0);                 // returns 0
     * Numbers.extractFirstIntOrElse(null, 0);               // returns 0
     * Numbers.extractFirstIntOrElse("id=99999999999", 0);   // throws NumberFormatException (a matched token outside int range)
     * }</pre>
     *
     * @param str the string to extract the int value from (may be {@code null} or empty).
     * @param defaultValue the value to return if no integer is found in the string
     * @return the first integer found in the string, or the specified default value if no integer is found or if the input string is {@code null}/empty
     * @throws NumberFormatException if the first matched digit run represents a value outside the {@code int} range;
     *         the underlying pattern matches digit runs of any length, so for example {@code extractFirstIntOrElse("id=99999999999", 0)} throws rather than returning the default value
     * @see #extractFirstInt(String)
     * @see #extractFirstLongOrElse(String, long)
     * @see Strings#findFirstInteger(String)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#INTEGER_FINDER
     */
    public static int extractFirstIntOrElse(final String str, final int defaultValue) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);

        if (matcher.find()) {
            return parseExtractedInt(str, matcher.start(1), matcher.end(1));
        }

        return defaultValue;
    }

    /**
     * Extracts the first long value from the given string.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstInteger(String)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of a long value.
     * It uses a regular expression pattern to identify integer patterns, including negative numbers.
     * If no long value is found in the string, an empty OptionalLong is returned.</p>
     *
     * <p><b>By design:</b> a matched digit run that does not fit in a {@code long} throws
     * {@code NumberFormatException}. Empty means “no integer token found”, not “token present but out of range”.</p>
     *
     * <p><b>Radix:</b> decimal only. The matched token is an optional sign followed by ASCII digits, so a
     * {@code 0x}/{@code 0X}/{@code #} prefix is never honoured here &mdash; the scan simply stops at the
     * non-digit. {@code extractFirstLong("0x1F")} is {@code 0L}, the leading digit run, where
     * {@link #toLong(String)} is {@code 31L}. A leading zero is decimal padding, never octal
     * ({@code extractFirstLong("010")} is {@code 10L}).</p>
     *
     * <p>A sign is attached only when it directly precedes the digits: {@code extractFirstLong("-.5")}
     * returns {@code 5L}, not {@code -5L}, because the {@code -} is not followed by a digit and is skipped
     * (unlike {@link #extractFirstDouble(String)}, which keeps the sign in that case).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstLong("abc123def");                // returns OptionalLong.of(123L)
     * Numbers.extractFirstLong("price: $4500000000");       // returns OptionalLong.of(4500000000L)
     * Numbers.extractFirstLong("total: -10000000000");      // returns OptionalLong.of(-10000000000L)
     *
     * // Edge cases
     * Numbers.extractFirstLong("no numbers");               // returns OptionalLong.empty()
     * Numbers.extractFirstLong("-.5");                      // returns OptionalLong.of(5L) (the sign is skipped: it does not touch the digits)
     * Numbers.extractFirstLong("");                         // returns OptionalLong.empty()
     * Numbers.extractFirstLong(null);                       // returns OptionalLong.empty()
     * Numbers.extractFirstLong("id=9999999999999999999");   // throws NumberFormatException (a matched token outside long range)
     * }</pre>
     *
     * @param str the string to extract the long value from (may be {@code null} or empty).
     * @return the extracted long value wrapped in an OptionalLong, or an empty OptionalLong if no long value is found or if the input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched digit run represents a value outside the {@code long} range;
     *         for example, {@code extractFirstLong("id=9999999999999999999")} throws rather than returning empty
     * @see #extractFirstLongOrElse(String, long)
     * @see #extractFirstInt(String)
     * @see Strings#findFirstInteger(String)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#INTEGER_FINDER
     */
    public static u.OptionalLong extractFirstLong(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return u.OptionalLong.empty();
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);

        if (matcher.find()) {
            return u.OptionalLong.of(parseExtractedLong(str, matcher.start(1), matcher.end(1)));
        }

        return u.OptionalLong.empty();
    }

    /**
     * Extracts the first long value from the given string, or returns a default value if no long value is found.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstInteger(String)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of a long value.
     * It uses a regular expression pattern to identify integer patterns, including negative numbers.
     * If no long value is found in the string, the specified default value is returned.</p>
     *
     * <p><b>By design:</b> {@code defaultValue} is used only when no integer token is found ({@code null}, empty,
     * or no match). A matched digit run that does not fit in a {@code long} still throws
     * {@code NumberFormatException}; it does not return {@code defaultValue}. This is not a parse-failure fallback.</p>
     *
     * <p><b>Radix:</b> decimal only. The matched token is an optional sign followed by ASCII digits, so a
     * {@code 0x}/{@code 0X}/{@code #} prefix is never honoured here &mdash; the scan simply stops at the
     * non-digit. {@code extractFirstLongOrElse("0x1F", 0L)} is {@code 0L}, the leading digit run, where
     * {@link #toLong(String)} is {@code 31L}. A leading zero is decimal padding, never octal
     * ({@code extractFirstLongOrElse("010", 0L)} is {@code 10L}).</p>
     *
     * <p>A sign is attached only when it directly precedes the digits: {@code extractFirstLongOrElse("-.5", 0L)}
     * returns {@code 5L}, not {@code -5L} (see {@link #extractFirstLong(String)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstLongOrElse("abc123def", 0L);                // returns 123L
     * Numbers.extractFirstLongOrElse("price: $4500000000", 0L);       // returns 4500000000L
     * Numbers.extractFirstLongOrElse("total: -10000000000", 0L);      // returns -10000000000L
     *
     * // Edge cases: the default covers "no token found" only
     * Numbers.extractFirstLongOrElse("no numbers", 0L);               // returns 0L
     * Numbers.extractFirstLongOrElse("-.5", 0L);                      // returns 5L (the sign is skipped: it does not touch the digits)
     * Numbers.extractFirstLongOrElse("", 0L);                         // returns 0L
     * Numbers.extractFirstLongOrElse(null, 0L);                       // returns 0L
     * Numbers.extractFirstLongOrElse("id=9999999999999999999", 0L);   // throws NumberFormatException (a matched token outside long range)
     * }</pre>
     *
     * @param str the string to extract the long value from (may be {@code null} or empty).
     * @param defaultValue the default value to return if no long value is found.
     * @return the extracted long value, or the specified default value if no long value is found or if the input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched digit run represents a value outside the {@code long} range;
     *         for example, {@code extractFirstLongOrElse("id=9999999999999999999", 0L)} throws rather than returning the default value
     * @see #extractFirstLong(String)
     * @see #extractFirstIntOrElse(String, int)
     * @see Strings#findFirstInteger(String)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#INTEGER_FINDER
     */
    public static long extractFirstLongOrElse(final String str, final long defaultValue) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);
        if (matcher.find()) {
            return parseExtractedLong(str, matcher.start(1), matcher.end(1));
        }

        return defaultValue;
    }

    /**
     * Extracts the first double value from the given string.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstDouble(String)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of a double value.
     * It uses a regular expression pattern to identify decimal number patterns. If no double value is found
     * in the string, an empty OptionalDouble is returned.</p>
     *
     * <p>The underlying pattern ({@link RegExUtil#NUMBER_FINDER}) matches an optional sign, then
     * either digits with an optional fraction (including a trailing-dot form such as {@code "1."})
     * or a leading-dot fraction. So {@code ".5"} is {@code 0.5} and {@code "-.5"} is {@code -0.5}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstDouble("abc123.45def");          // returns OptionalDouble.of(123.45)
     * Numbers.extractFirstDouble("value: -0.00123");       // returns OptionalDouble.of(-0.00123)
     * Numbers.extractFirstDouble("scientific: 1.23e10");   // returns OptionalDouble.of(1.23)
     * Numbers.extractFirstDouble(".5");                    // returns OptionalDouble.of(0.5)
     * Numbers.extractFirstDouble("x=-.5");                 // returns OptionalDouble.of(-0.5)
     *
     * // Edge cases
     * Numbers.extractFirstDouble("no numbers");            // returns OptionalDouble.empty()
     * Numbers.extractFirstDouble("");                      // returns OptionalDouble.empty()
     * Numbers.extractFirstDouble(null);                    // returns OptionalDouble.empty()
     * }</pre>
     *
     * <p><b>By design:</b> as in {@link #extractFirstInt(String)} and {@link #extractFirstLong(String)}, an
     * empty result means &ldquo;no numeric token found&rdquo;, never &ldquo;token found but not
     * representable&rdquo;. A matched token longer than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code
     * units throws {@code NumberFormatException} rather than being silently reported as absent. Unlike the
     * integer finders, a matched token whose <em>value</em> is out of range does not throw: it saturates to
     * {@code ±Infinity} and a token that underflows becomes a signed zero, exactly as in
     * {@link #toDouble(String)}. Those are IEEE-754 numeric results, not errors, so
     * {@code extractFirstDouble("9".repeat(400))} is {@code OptionalDouble.of(Double.POSITIVE_INFINITY)}
     * where {@code extractFirstInt("9".repeat(400))} throws.</p>
     *
     * @param str the string to extract the double value from (may be {@code null} or empty).
     * @return the extracted double value wrapped in an OptionalDouble, or an empty OptionalDouble if no double
     *         value is found or the input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched token is longer than
     *         {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units
     * @see #extractFirstDouble(String, boolean)
     * @see #extractFirstDoubleOrElse(String, double)
     * @see #extractFirstDoubleOrElse(String, double, boolean)
     * @see Strings#findFirstDouble(String)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#NUMBER_FINDER
     * @see RegExUtil#SCIENTIFIC_NUMBER_FINDER
     */
    public static u.OptionalDouble extractFirstDouble(final String str) throws NumberFormatException {
        return extractFirstDouble(str, false);
    }

    /**
     * Extracts the first double value from the given string.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstDouble(String, boolean)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of a double value.
     * It uses a regular expression pattern to identify decimal number patterns. If {@code allowScientificNotation}
     * is set to {@code true}, it will also consider scientific notation (e.g., {@code 1.23e10}) as valid double values.
     * If no double value is found in the string, an empty OptionalDouble is returned.</p>
     *
     * <p>The underlying patterns ({@link RegExUtil#NUMBER_FINDER} and
     * {@link RegExUtil#SCIENTIFIC_NUMBER_FINDER}) match an optional sign, then either digits with an
     * optional fraction (including a trailing-dot form such as {@code "1."}) or a leading-dot
     * fraction. So {@code ".5"} is {@code 0.5} and {@code "-.5"} is {@code -0.5}. When
     * {@code allowScientificNotation} is {@code true}, an exponent stays on that mantissa:
     * {@code ".5e2"} is {@code 50.0} and {@code "-.5e2"} is {@code -50.0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstDouble("abc123.45def", false);          // returns OptionalDouble.of(123.45)
     * Numbers.extractFirstDouble("value: -0.00123", false);       // returns OptionalDouble.of(-0.00123)
     * Numbers.extractFirstDouble("scientific: 1.23e10", false);   // returns OptionalDouble.of(1.23)
     * Numbers.extractFirstDouble("scientific: 1.23e10", true);    // returns OptionalDouble.of(1.23E10)
     * Numbers.extractFirstDouble(".5", false);                    // returns OptionalDouble.of(0.5)
     * Numbers.extractFirstDouble("x=-.5", false);                 // returns OptionalDouble.of(-0.5)
     * Numbers.extractFirstDouble(".5e2", true);                   // returns OptionalDouble.of(50.0)
     * Numbers.extractFirstDouble("x=-.5e2", true);                // returns OptionalDouble.of(-50.0)
     *
     * // Edge cases
     * Numbers.extractFirstDouble("no numbers", false);            // returns OptionalDouble.empty()
     * Numbers.extractFirstDouble("", false);                      // returns OptionalDouble.empty()
     * Numbers.extractFirstDouble(null, false);                    // returns OptionalDouble.empty()
     * }</pre>
     *
     * <p><b>By design:</b> as in {@link #extractFirstInt(String)} and {@link #extractFirstLong(String)}, an
     * empty result means &ldquo;no numeric token found&rdquo;, never &ldquo;token found but not
     * representable&rdquo;. A matched token longer than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code
     * units throws {@code NumberFormatException} rather than being silently reported as absent. Unlike the
     * integer finders, a matched token whose <em>value</em> is out of range does not throw: it saturates to
     * {@code ±Infinity} and a token that underflows becomes a signed zero, exactly as in
     * {@link #toDouble(String)}. Those are IEEE-754 numeric results, not errors, so
     * {@code extractFirstDouble("9".repeat(400))} is {@code OptionalDouble.of(Double.POSITIVE_INFINITY)}
     * where {@code extractFirstInt("9".repeat(400))} throws.</p>
     *
     * @param str the string to extract the double value from (may be {@code null} or empty).
     * @param allowScientificNotation if {@code true}, a match may also carry an exponent (for example
     *        {@code 1.23e4}); plain decimals are matched either way.
     * @return the extracted double value wrapped in an OptionalDouble, or an empty OptionalDouble if no double
     *         value is found or the input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched token is longer than
     *         {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units
     * @see #extractFirstDouble(String)
     * @see #extractFirstDoubleOrElse(String, double)
     * @see #extractFirstDoubleOrElse(String, double, boolean)
     * @see Strings#findFirstDouble(String)
     * @see Strings#findFirstDouble(String, boolean)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#NUMBER_FINDER
     * @see RegExUtil#SCIENTIFIC_NUMBER_FINDER
     */
    public static u.OptionalDouble extractFirstDouble(final String str, final boolean allowScientificNotation) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return u.OptionalDouble.empty();
        }

        final Matcher matcher = (allowScientificNotation ? RegExUtil.SCIENTIFIC_NUMBER_FINDER : RegExUtil.NUMBER_FINDER).matcher(str);

        if (matcher.find()) {
            final int start = matcher.start(1);
            final int end = matcher.end(1);

            if (end - start > MAX_FLOATING_POINT_TOKEN_LENGTH) {
                // A token that is present but unrepresentable is an error, not an absence -- the same rule
                // extractFirstInt/extractFirstLong apply to an out-of-range digit run.
                throw floatingPointTokenTooLong(str, start, end, "Double");
            }

            return u.OptionalDouble.of(Double.parseDouble(str.substring(start, end)));
        }

        return u.OptionalDouble.empty();
    }

    /**
     * Extracts the first double value from the given string. If no double value is found, it returns the specified default value.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstDouble(String)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of a double value.
     * It uses a regular expression pattern to identify decimal number patterns. If no double value is found
     * in the string, the specified default value is returned.</p>
     *
     * <p>The underlying pattern ({@link RegExUtil#NUMBER_FINDER}) matches an optional sign, then
     * either digits with an optional fraction (including a trailing-dot form such as {@code "1."})
     * or a leading-dot fraction. So {@code ".5"} is {@code 0.5} and {@code "-.5"} is {@code -0.5}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstDoubleOrElse("abc123.45def", 0.0);          // returns 123.45
     * Numbers.extractFirstDoubleOrElse("value: -0.00123", 0.0);       // returns -0.00123
     * Numbers.extractFirstDoubleOrElse("scientific: 1.23e10", 0.0);   // returns 1.23 (only mantissa extracted)
     * Numbers.extractFirstDoubleOrElse(".5", 0.0);                    // returns 0.5
     * Numbers.extractFirstDoubleOrElse("x=-.5", 0.0);                 // returns -0.5
     *
     * // Edge cases
     * Numbers.extractFirstDoubleOrElse("no numbers", 0.0);            // returns 0.0
     * Numbers.extractFirstDoubleOrElse("", 0.0);                      // returns 0.0
     * Numbers.extractFirstDoubleOrElse(null, 0.0);                    // returns 0.0
     * }</pre>
     *
     * <p><b>By design:</b> as in {@link #extractFirstIntOrElse(String, int)}, {@code defaultValue} covers
     * &ldquo;no numeric token found&rdquo; only. A matched token longer than
     * {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units throws {@code NumberFormatException} rather
     * than silently returning {@code defaultValue}. This is not a parse-failure fallback. Unlike the integer
     * finders, a matched token whose <em>value</em> is out of range does not throw either: it saturates to
     * {@code ±Infinity} and a token that underflows becomes a signed zero, exactly as in
     * {@link #toDouble(String)}. Those are IEEE-754 numeric results, not errors, so
     * {@code extractFirstDoubleOrElse("9".repeat(400), 0)} is {@code Double.POSITIVE_INFINITY}, not the
     * default.</p>
     *
     * @param str the string to extract the double value from (may be {@code null} or empty).
     * @param defaultValue the default value to return if no double value is found.
     * @return the extracted double value, or the specified default value if no double value is found or the
     *         input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched token is longer than
     *         {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units
     * @see #extractFirstDouble(String)
     * @see #extractFirstDouble(String, boolean)
     * @see #extractFirstDoubleOrElse(String, double, boolean)
     * @see Strings#findFirstDouble(String)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#NUMBER_FINDER
     * @see RegExUtil#SCIENTIFIC_NUMBER_FINDER
     */
    public static double extractFirstDoubleOrElse(final String str, final double defaultValue) throws NumberFormatException {
        return extractFirstDoubleOrElse(str, defaultValue, false);
    }

    /**
     * Extracts the first double value from the given string. If no double value is found, it returns the specified default value.
     *
     * <p>The result is the parsed <i>value</i>, not the matched text. Use
     * {@link Strings#findFirstDouble(String, boolean)} to obtain the matched text instead.</p>
     *
     * <p>This method searches through the provided string to find the first occurrence of a double value.
     * It uses a regular expression pattern to identify decimal number patterns. If {@code allowScientificNotation}
     * is set to {@code true}, it will also consider scientific notation (e.g., {@code 1.23e10}) as valid double values.
     * If no double value is found in the string, the specified default value is returned.</p>
     *
     * <p>The underlying patterns ({@link RegExUtil#NUMBER_FINDER} and
     * {@link RegExUtil#SCIENTIFIC_NUMBER_FINDER}) match an optional sign, then either digits with an
     * optional fraction (including a trailing-dot form such as {@code "1."}) or a leading-dot
     * fraction. So {@code ".5"} is {@code 0.5} and {@code "-.5"} is {@code -0.5}. When
     * {@code allowScientificNotation} is {@code true}, an exponent stays on that mantissa:
     * {@code ".5e2"} is {@code 50.0} and {@code "-.5e2"} is {@code -50.0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.extractFirstDoubleOrElse("abc123.45def", 0.0, false);          // returns 123.45
     * Numbers.extractFirstDoubleOrElse("value: -0.00123", 0.0, false);       // returns -0.00123
     * Numbers.extractFirstDoubleOrElse("scientific: 1.23e10", 0.0, false);   // returns 1.23 (only mantissa extracted)
     * Numbers.extractFirstDoubleOrElse("scientific: 1.23e10", 0.0, true);    // returns 1.23E10
     * Numbers.extractFirstDoubleOrElse(".5", 0.0, false);                    // returns 0.5
     * Numbers.extractFirstDoubleOrElse("x=-.5", 0.0, false);                 // returns -0.5
     * Numbers.extractFirstDoubleOrElse(".5e2", 0.0, true);                   // returns 50.0
     * Numbers.extractFirstDoubleOrElse("x=-.5e2", 0.0, true);                // returns -50.0
     *
     * // Edge cases
     * Numbers.extractFirstDoubleOrElse("no numbers", 0.0, false);            // returns 0.0
     * Numbers.extractFirstDoubleOrElse("", 0.0, false);                      // returns 0.0
     * Numbers.extractFirstDoubleOrElse(null, 0.0, false);                    // returns 0.0
     * }</pre>
     *
     * <p><b>By design:</b> as in {@link #extractFirstIntOrElse(String, int)}, {@code defaultValue} covers
     * &ldquo;no numeric token found&rdquo; only. A matched token longer than
     * {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units throws {@code NumberFormatException} rather
     * than silently returning {@code defaultValue}. This is not a parse-failure fallback. Unlike the integer
     * finders, a matched token whose <em>value</em> is out of range does not throw either: it saturates to
     * {@code ±Infinity} and a token that underflows becomes a signed zero, exactly as in
     * {@link #toDouble(String)}. Those are IEEE-754 numeric results, not errors, so
     * {@code extractFirstDoubleOrElse("9".repeat(400), 0)} is {@code Double.POSITIVE_INFINITY}, not the
     * default.</p>
     *
     * @param str the string to extract the double value from (may be {@code null} or empty).
     * @param defaultValue the default value to return if no double value is found.
     * @param allowScientificNotation if {@code true}, a match may also carry an exponent (for example
     *        {@code 1.23e4}); plain decimals are matched either way.
     * @return the extracted double value, or the specified default value if no double value is found or the
     *         input string is {@code null}/empty.
     * @throws NumberFormatException if the first matched token is longer than
     *         {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units
     * @see #extractFirstDouble(String)
     * @see #extractFirstDouble(String, boolean)
     * @see #extractFirstDoubleOrElse(String, double)
     * @see Strings#findFirstDouble(String)
     * @see Strings#findFirstDouble(String, boolean)
     * @see RegExUtil#findFirst(String, Pattern)
     * @see RegExUtil#findLast(String, Pattern)
     * @see RegExUtil#NUMBER_FINDER
     * @see RegExUtil#SCIENTIFIC_NUMBER_FINDER
     */
    public static double extractFirstDoubleOrElse(final String str, final double defaultValue, final boolean allowScientificNotation)
            throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        final Matcher matcher = (allowScientificNotation ? RegExUtil.SCIENTIFIC_NUMBER_FINDER : RegExUtil.NUMBER_FINDER).matcher(str);

        if (matcher.find()) {
            final int start = matcher.start(1);
            final int end = matcher.end(1);

            if (end - start > MAX_FLOATING_POINT_TOKEN_LENGTH) {
                // defaultValue covers "no token found" only, exactly as in extractFirstIntOrElse.
                throw floatingPointTokenTooLong(str, start, end, "Double");
            }

            return Double.parseDouble(str.substring(start, end));
        }

        return defaultValue;
    }

    // PLAIN_DECIMAL, not the DECIMAL_FIRST grammar the to*(String) parsers use: the token handed to these two
    // was matched by RegExUtil.INTEGER_FINDER, which is an optional sign followed by ASCII digits, so it can
    // never carry a 0x/# radix prefix. DECIMAL_FIRST would work identically -- its prefix branches simply
    // cannot fire on such a token -- but it would say the extract family accepts hexadecimal, which it does
    // not: extractFirstInt("0x1F") is 0, the leading decimal run, while toInt("0x1F") is 31.
    private static int parseExtractedInt(final String source, final int start, final int end) {
        final long value = scanIntegerTokenValueOrInvalid(source, start, end, IntegerTokenSyntax.PLAIN_DECIMAL, Integer.MIN_VALUE, Integer.MAX_VALUE);

        if (value == INVALID_INTEGER_TOKEN) {
            throw extractedTokenTooWide(source, start, end, "int");
        }

        return (int) value;
    }

    /** See {@link #parseExtractedInt(String, int, int)} for why the grammar is {@code PLAIN_DECIMAL}. */
    private static long parseExtractedLong(final String source, final int start, final int end) {
        final long value = scanIntegerTokenValueOrInvalid(source, start, end, IntegerTokenSyntax.PLAIN_DECIMAL, Long.MIN_VALUE, Long.MAX_VALUE);

        if (value != INVALID_INTEGER_TOKEN || isLongMinValueToken(source, start, end, IntegerTokenSyntax.PLAIN_DECIMAL)) {
            return value;
        }

        throw extractedTokenTooWide(source, start, end, "long");
    }

    /**
     * The failure the {@code extractFirstInt}/{@code extractFirstLong} family reports for a matched digit run
     * whose value does not fit the target type. Unlike the {@code to*} and {@code decode*} families this one
     * names the primitive type, because the sentence reads as one ("does not fit in int"); the two callers
     * share this builder so that wording, the bounded preview and the cause cannot drift apart.
     *
     * @param source the string the token was matched in
     * @param start the index of the token's first character
     * @param end the index after the token's last character
     * @param primitiveName {@code "int"} or {@code "long"}
     * @return the {@link NumberFormatException} to throw
     */
    private static NumberFormatException extractedTokenTooWide(final String source, final int start, final int end, final String primitiveName) {
        final NumberFormatException nfe = new NumberFormatException(
                "integer token '" + previewForErrorMessage(source, start, end) + "' does not fit in " + primitiveName);
        nfe.initCause(new NumberFormatException(primitiveName + " value is out of range"));
        return nfe;
    }

    /**
     * Converts the given string to a byte value.
     *
     * <p>This method attempts to convert the provided string to a byte. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a byte.
     * A trailing {@code 'L'} or {@code 'l'} suffix (Java long-literal style, e.g. {@code "12L"}) is accepted.</p>
     *
     * <p><b>Radix:</b> decimal-first, same policy as {@link #toInt(String)}. Leading zeros are decimal padding, not
     * octal ({@code "010"} is 10). A {@code 0x}/{@code 0X}/{@code #} prefix selects hexadecimal, which is still
     * checked against the byte range; every other token is decimal. Use {@link #decodeInteger(String)} for
     * leading-zero octal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toByte("12");            // returns 12
     * Numbers.toByte("-42");           // returns -42
     * Numbers.toByte("127");           // returns 127
     * Numbers.toByte("010");           // returns 10 (decimal, not octal)
     * Numbers.toByte("0x7F");          // returns 127 (the 0x prefix selects hexadecimal)
     *
     * // Edge cases
     * Numbers.toByte((String) null);   // returns 0
     * Numbers.toByte("");              // returns 0
     * Numbers.toByte("0x80");          // throws ArithmeticException (outside byte range)
     * }</pre>
     *
     * @param str the string to convert. This can be any instance of String.
     * @return the byte representation of the provided string, or {@code 0} if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the byte range.
     * @see #toByte(String, byte)
     * @see #toByte(Object)
     * @see #toInt(String)
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     */
    public static byte toByte(final String str) throws NumberFormatException, ArithmeticException {
        return toByte(str, (byte) 0);
    }

    /**
     * Converts the given object to a byte value.
     *
     * <p>This method attempts to convert the provided object to a byte. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as a byte, so an empty or {@code null} {@code toString()} also returns {@code 0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toByte((Object) Integer.valueOf(42));        // returns 42
     * Numbers.toByte((Object) Byte.valueOf((byte) 100));   // returns 100
     * Numbers.toByte((Object) "123");                      // returns 123
     * Numbers.toByte((Object) Double.valueOf(12.9));       // returns 12 (truncated toward zero)
     *
     * // Edge cases
     * Numbers.toByte((Object) null);                       // returns 0
     * Numbers.toByte((Object) "");                         // returns 0
     * Numbers.toByte((Object) Integer.valueOf(200));       // throws ArithmeticException (outside byte range)
     * Numbers.toByte((Object) "12.9");                     // throws NumberFormatException (not an integer token)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/{@code BigDecimal} whose truncated
     * value lies outside the {@code byte} range throws {@code ArithmeticException}, while a fractional value whose
     * truncation fits converts (e.g. {@code 127.9} &rarr; {@code 127}). {@code NaN} and infinite values throw
     * {@code ArithmeticException} (same policy as {@link #convert(Number, Class)}). A non-standard {@code Number} subtype is truncated and range-checked by
     * the same rule, recovered from its <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. That text is decimal only: a hexadecimal or {@code L}-suffixed
     * spelling is not a {@code Number}'s own rendering and takes the {@code doubleValue()} route.
     * <b>By design:</b> a {@code Number} may be fractional ({@code 12.9} &rarr; {@code 12}); the string {@code "12.9"}
     * is not an integer token and throws. Object conversion is a numeric coercion; {@link #toByte(String)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @return the byte representation of the provided object, or {@code 0} if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the byte range.
     * @see #toByte(String)
     * @see #toByte(String, byte)
     * @see #toByte(Object, byte)
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see #decodeInteger(String)
     */
    public static byte toByte(final Object obj) throws NumberFormatException, ArithmeticException {
        return toByte(obj, (byte) 0);
    }

    /**
     * Converts the given string to a byte value.
     *
     * <p>This method attempts to convert the provided string to a byte. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a byte.
     * A trailing {@code 'L'} or {@code 'l'} suffix (Java long-literal style, e.g. {@code "12L"}) is accepted.
     * Decimal-first radix, same as {@link #toInt(String, int)}: {@code "010"} is 10, not octal 8;
     * a {@code 0x}/{@code 0X}/{@code #} prefix selects range-checked hexadecimal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toByte("12", (byte) 0);            // returns 12
     * Numbers.toByte("-42", (byte) 0);           // returns -42
     * Numbers.toByte("127", (byte) 0);           // returns 127
     * Numbers.toByte("0x7F", (byte) 0);          // returns 127 (the 0x prefix selects hexadecimal)
     *
     * // Edge cases: the default covers only null and empty input
     * Numbers.toByte((String) null, (byte) 1);   // returns 1
     * Numbers.toByte("", (byte) 1);              // returns 1
     * Numbers.toByte("abc", (byte) 0);           // throws NumberFormatException
     * Numbers.toByte("128", (byte) 0);           // throws ArithmeticException (out of range)
     * }</pre>
     *
     * <p><b>By design:</b> {@code defaultValue} applies only to {@code null} or empty input. A malformed string
     * still throws {@code NumberFormatException} and an out-of-range value still throws {@code ArithmeticException}.
     * This is a missing-input fallback, not a parse-failure fallback.</p>
     *
     * <p><b>By design:</b> a string must be an integer token (optional sign, decimal or {@code 0x}/{@code #} hex,
     * optional {@code L} suffix). {@code "12.9"} throws {@code NumberFormatException}. A {@code Number} passed to
     * {@link #toByte(Object, byte)} is truncated toward zero and range-checked ({@code 12.9} &rarr; {@code 12}).
     * String parsing is a text grammar; Object conversion is a numeric coercion.</p>
     *
     * @param str the string to convert. This can be any instance of String.
     * @param defaultValue the default value to return if the string is {@code null} or empty.
     * @return the byte representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the byte range.
     * @see #toByte(String)
     * @see #toByte(Object)
     * @see #toByte(Object, byte)
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see #decodeInteger(String)
     */
    public static byte toByte(final String str, final byte defaultValue) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);

            if (result != null) {
                if (result < Byte.MIN_VALUE || result > Byte.MAX_VALUE) {
                    throw numberOverflow("byte", result);
                }

                return result.byteValue();
            }
        }

        return (byte) parseLongInRange(str, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte");
    }

    /**
     * Parses {@code str} as an integer and returns its value if it lies within {@code [min, max]}.
     * The token is an optional sign, then decimal digits or prefixed hexadecimal ({@code 0x}/{@code 0X}/
     * {@code #}), then an optional trailing {@code L}/{@code l} (Java long-literal style, e.g. {@code "123L"}).
     * Leading zeros are decimal padding, never octal, so {@code "010"} is 10 and {@code "08"} is 8
     * (unlike {@link #decodeBigInteger(String)}, which would treat a leading zero as octal).
     * A string that is a valid integer but out of the {@code [min, max]} range (or beyond the {@code long}
     * range) throws {@code ArithmeticException} ("&lt;typeName&gt; overflow: ..."); a string that is not a
     * valid integer throws {@code NumberFormatException}. This lets the integer {@code toByte/toShort/toInt/
     * toLong(String, ...)} methods report numeric overflow and malformed input distinctly, and consistently
     * with their {@code Number}/{@code Object} overloads.
     * Non-ASCII Unicode digits are rejected; {@link Long#parseLong(String)} and {@code new BigInteger(String, int)}
     * would otherwise accept them via {@code Character.digit}.
     *
     * <p>Valid tokens use an allocation-free primitive scan. Invalid or overflowing tokens are scanned again by
     * {@link #scanIntegerTokenQuiet(String, int, IntegerTokenSyntax)} to retain detailed failure diagnostics: no
     * {@code Long.parseLong} call (whose failure {@code NumberFormatException} would embed a copy of the whole token),
     * no suffix {@code substring}, and no {@code BigInteger}. A token of any length is therefore parsed in linear
     * time with constant auxiliary space, and exception messages embed only a bounded
     * {@linkplain #previewForErrorMessage(String) preview} of the input.</p>
     */
    private static long parseLongInRange(final String str, final long min, final long max, final String typeName) {
        final int end = integerTokenEnd(str);
        final long value = scanIntegerTokenValueOrInvalid(str, 0, end, IntegerTokenSyntax.DECIMAL_FIRST, min, max);

        if (value != INVALID_INTEGER_TOKEN || (min == Long.MIN_VALUE && isLongMinValueToken(str, 0, end, IntegerTokenSyntax.DECIMAL_FIRST))) {
            return value;
        }

        final IntegerTokenScan scan = scanIntegerTokenQuiet(str, end, IntegerTokenSyntax.DECIMAL_FIRST);

        if (scan.status == SCAN_MALFORMED) {
            throw notAValidNumber(str, typeName, malformedIntegerTokenCause(str, scan));
        }

        // Invariant: the two scanners accept exactly the same tokens, and the range-limited one above already
        // rejected this token, so what is left is either malformed (handled) or outside [min, max]. A
        // SCAN_VALID result inside the range would mean they disagree. Checked unconditionally rather than
        // with assert, which is disabled unless -ea is passed: if the invariant ever broke, the overflow
        // below would silently misreport a token that actually parses. Same guard as decodeLong(String).
        if (scan.status == SCAN_VALID && scan.value >= min && scan.value <= max) {
            throw new AssertionError("scanners disagree on " + previewForErrorMessage(str));
        }

        throw new ArithmeticException(typeName + " overflow: " + previewForErrorMessage(str));
    }

    /** Radix policies understood by {@link #scanIntegerTokenQuiet(String, int, IntegerTokenSyntax)}. */
    private enum IntegerTokenSyntax {
        /** Leading zeros are decimal padding; only {@code 0x}/{@code 0X}/{@code #} select hexadecimal. */
        DECIMAL_FIRST,
        /**
         * Follows {@link Long#decode(String)}: hexadecimal prefixes select radix 16 and a leading zero selects radix 8.
         */
        DECODE,
        /**
         * Decimal only: no radix prefix is recognized, so {@code "0x10"} and {@code "#10"} are malformed rather
         * than hexadecimal. Used where the text being scanned is a {@link Number}'s own {@code toString()}
         * rather than caller-supplied text; see {@link #unknownNumberToLongWithinRange(Number, long, long, String)}.
         */
        PLAIN_DECIMAL
    }

    /** Outcome categories of {@link #scanIntegerTokenQuiet(String, int, IntegerTokenSyntax)}. */
    private static final int SCAN_VALID = 0;
    private static final int SCAN_MALFORMED = 1;
    private static final int SCAN_OVERFLOW = 2;

    /** Sentinel used by the primitive-only scanner. A valid {@link Long#MIN_VALUE} token is disambiguated lexically. */
    private static final long INVALID_INTEGER_TOKEN = Long.MIN_VALUE;

    /**
     * Malformed detail kinds of {@link #scanIntegerTokenQuiet(String, int, IntegerTokenSyntax)}, used to rebuild the cause message.
     */
    private static final int MALFORMED_EMPTY_TOKEN = 0;
    private static final int MALFORMED_NO_DIGITS = 1;
    private static final int MALFORMED_INVALID_CHARACTER = 2;

    /** The non-throwing outcome of {@link #scanIntegerTokenQuiet(String, int, IntegerTokenSyntax)}. */
    private static final class IntegerTokenScan {
        long value;
        int status;
        int malformedKind;
        int errorIndex;
    }

    /**
     * Returns the exclusive end index of the numeric token in {@code str}, excluding an optional trailing
     * {@code L}/{@code l} (Java long-literal style, e.g. {@code "123L"}) by index rather than substring,
     * avoiding an input-sized copy.
     */
    private static int integerTokenEnd(final String str) {
        final int len = str.length();
        return (len > 1 && (str.charAt(len - 1) == 'L' || str.charAt(len - 1) == 'l')) ? len - 1 : len;
    }

    /**
     * Rebuilds the {@link NumberFormatException} detail message for a {@link #SCAN_MALFORMED} outcome,
     * distinguishing a missing-digits token (empty, or a sign/prefix with no digits) from an invalid
     * character (named, with its index).
     */
    private static NumberFormatException malformedIntegerTokenCause(final String str, final IntegerTokenScan scan) {
        return switch (scan.malformedKind) {
            case MALFORMED_EMPTY_TOKEN -> new NumberFormatException("empty integer token");
            case MALFORMED_NO_DIGITS -> new NumberFormatException("no digits in integer token " + previewForErrorMessage(str));
            default -> new NumberFormatException("invalid character '" + escapeForErrorMessage(str.charAt(scan.errorIndex)) + "' at index " + scan.errorIndex
                    + " of " + previewForErrorMessage(str));
        };
    }

    /**
     * Builds the public decode failure while retaining a bounded cause. Decode methods report both malformed
     * tokens and range overflow as {@link NumberFormatException}, unlike the integer {@code to*} methods.
     */
    private static NumberFormatException decodeIntegralFailure(final String str, final String typeName, final IntegerTokenScan scan) {
        final NumberFormatException cause = scan.status == SCAN_MALFORMED ? malformedIntegerTokenCause(str, scan)
                : new NumberFormatException(typeName + " value is out of range");
        return notAValidNumber(str, typeName, cause);
    }

    /**
     * Scans {@code str[0, end)} as a signed integer without throwing. {@code syntax} controls whether a leading
     * zero is decimal padding or an octal prefix; {@code 0x}/{@code 0X}/{@code #} select hexadecimal in either mode.
     * All token validation happens here; the outcome is reported as a status: {@link #SCAN_VALID} with the parsed
     * value; {@link #SCAN_MALFORMED} with a detail
     * kind ({@link #MALFORMED_EMPTY_TOKEN} or {@link #MALFORMED_NO_DIGITS} when no digits follow the optional
     * sign/prefix, otherwise {@link #MALFORMED_INVALID_CHARACTER} with the offending character's index in
     * {@code errorIndex}); or {@link #SCAN_OVERFLOW} when the value is outside the {@code long} range.
     * Digits are accumulated negatively (the same technique as {@link Long#parseLong(String)}), so
     * {@code Long.MIN_VALUE} is representable and overflow is detected during the scan. Scanning continues
     * past an overflow so that a token which is both too large <em>and</em> malformed reports malformed.
     */
    private static IntegerTokenScan scanIntegerTokenQuiet(final String str, final int end, final IntegerTokenSyntax syntax) {
        final IntegerTokenScan scan = new IntegerTokenScan();

        // Currently unreachable: every caller rejects an empty token first. Kept because it is what makes the
        // unconditional str.charAt(0) below safe if a future caller ever passes end == 0.
        if (end == 0) {
            scan.status = SCAN_MALFORMED;
            scan.malformedKind = MALFORMED_EMPTY_TOKEN;
            return scan;
        }

        int pos = 0;
        boolean negate = false;
        final char char0 = str.charAt(0);

        if (char0 == '-') {
            negate = true;
            pos = 1;
        } else if (char0 == '+') {
            pos = 1;
        }

        int radix = 10;

        if (syntax != IntegerTokenSyntax.PLAIN_DECIMAL) {
            if (pos + 1 < end && str.charAt(pos) == '0' && (str.charAt(pos + 1) == 'x' || str.charAt(pos + 1) == 'X')) {
                radix = 16;
                pos += 2;
            } else if (pos < end && str.charAt(pos) == '#') {
                radix = 16;
                pos++;
            } else if (syntax == IntegerTokenSyntax.DECODE && pos + 1 < end && str.charAt(pos) == '0') {
                radix = 8;
                pos++;
            }
        }

        if (pos >= end) {
            scan.status = SCAN_MALFORMED;
            scan.malformedKind = MALFORMED_NO_DIGITS;
            return scan;
        }

        final long limit = negate ? Long.MIN_VALUE : -Long.MAX_VALUE;
        final long multmin = limit / radix;
        long result = 0;
        boolean overflowed = false;

        while (pos < end) {
            final int digit = digitOf(str.charAt(pos++), radix);

            if (digit < 0) {
                scan.status = SCAN_MALFORMED;
                scan.malformedKind = MALFORMED_INVALID_CHARACTER;
                scan.errorIndex = pos - 1;
                return scan;
            }

            if (!overflowed) {
                if (result < multmin) {
                    overflowed = true;
                } else {
                    result *= radix;

                    if (result < limit + digit) {
                        overflowed = true;
                    } else {
                        result -= digit;
                    }
                }
            }
        }

        if (overflowed) {
            scan.status = SCAN_OVERFLOW;
            return scan;
        }

        scan.status = SCAN_VALID;
        scan.value = negate ? result : -result;
        return scan;
    }

    /**
     * Scans {@code str[start, end)} into the supplied inclusive range without allocating a result holder or
     * constructing an exception. The negative-accumulation algorithm detects overflow before it occurs.
     * {@link #INVALID_INTEGER_TOKEN} reports malformed or out-of-range input; because that value is also a
     * valid {@code long}, callers accepting the full long range must use
     * {@link #isLongMinValueToken(String, int, int, IntegerTokenSyntax)} to distinguish the one collision.
     */
    private static long scanIntegerTokenValueOrInvalid(final String str, final int start, final int end, final IntegerTokenSyntax syntax, final long min,
            final long max) {
        if (start >= end) {
            return INVALID_INTEGER_TOKEN;
        }

        int pos = start;
        boolean negate = false;
        final char first = str.charAt(pos);

        if (first == '-') {
            negate = true;
            pos++;
        } else if (first == '+') {
            pos++;
        }

        int radix = 10;

        if (syntax != IntegerTokenSyntax.PLAIN_DECIMAL) {
            if (pos + 1 < end && str.charAt(pos) == '0' && (str.charAt(pos + 1) == 'x' || str.charAt(pos + 1) == 'X')) {
                radix = 16;
                pos += 2;
            } else if (pos < end && str.charAt(pos) == '#') {
                radix = 16;
                pos++;
            } else if (syntax == IntegerTokenSyntax.DECODE && pos + 1 < end && str.charAt(pos) == '0') {
                radix = 8;
                pos++;
            }
        }

        if (pos >= end) {
            return INVALID_INTEGER_TOKEN;
        }

        final long limit = negate ? min : -max;
        final long multmin = limit / radix;
        long result = 0;

        while (pos < end) {
            final int digit = digitOf(str.charAt(pos++), radix);

            if (digit < 0 || result < multmin) {
                return INVALID_INTEGER_TOKEN;
            }

            result *= radix;

            if (result < limit + digit) {
                return INVALID_INTEGER_TOKEN;
            }

            result -= digit;
        }

        return negate ? result : -result;
    }

    /**
     * Returns whether the token is exactly a valid spelling of {@link Long#MIN_VALUE}. This is used only to
     * disambiguate that value from {@link #INVALID_INTEGER_TOKEN}; it performs no allocation and accepts the
     * same sign and radix-prefix rules as the primitive-only scanner.
     */
    private static boolean isLongMinValueToken(final String str, final int start, final int end, final IntegerTokenSyntax syntax) {
        int pos = start;

        if (pos >= end || str.charAt(pos++) != '-') {
            return false;
        }

        int radix = 10;

        if (syntax != IntegerTokenSyntax.PLAIN_DECIMAL) {
            if (pos + 1 < end && str.charAt(pos) == '0' && (str.charAt(pos + 1) == 'x' || str.charAt(pos + 1) == 'X')) {
                radix = 16;
                pos += 2;
            } else if (pos < end && str.charAt(pos) == '#') {
                radix = 16;
                pos++;
            } else if (syntax == IntegerTokenSyntax.DECODE && pos + 1 < end && str.charAt(pos) == '0') {
                radix = 8;
                pos++;
            }
        }

        while (pos < end && str.charAt(pos) == '0') {
            pos++;
        }

        final String magnitude = switch (radix) {
            case 8 -> "1000000000000000000000";
            case 16 -> "8000000000000000";
            default -> "9223372036854775808";
        };

        if (end - pos != magnitude.length()) {
            return false;
        }

        for (int i = 0; i < magnitude.length(); i++) {
            if (str.charAt(pos + i) != magnitude.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the value of ASCII character {@code ch} in the given radix (8, 10 or 16), or {@code -1}
     * if {@code ch} is not a valid digit. Unlike {@link Character#digit(char, int)}, non-ASCII
     * Unicode digits are rejected.
     */
    private static int digitOf(final char ch, final int radix) {
        if (ch >= '0' && ch <= '9') {
            final int d = ch - '0';
            return d < radix ? d : -1;
        }

        if (radix == 16) {
            if (ch >= 'a' && ch <= 'f') {
                return ch - 'a' + 10;
            }

            if (ch >= 'A' && ch <= 'F') {
                return ch - 'A' + 10;
            }
        }

        return -1;
    }

    /**
     * Converts the given object to a byte value.
     *
     * <p>This method attempts to convert the provided object to a byte. If the object is {@code null},
     * the provided default value is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as a byte, so an empty or {@code null} {@code toString()} also returns the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toByte((Object) Integer.valueOf(42), (byte) 0);        // returns 42
     * Numbers.toByte((Object) Byte.valueOf((byte) 100), (byte) 0);   // returns 100
     * Numbers.toByte((Object) "123", (byte) 0);                      // returns 123
     * Numbers.toByte((Object) Double.valueOf(12.9), (byte) 0);       // returns 12 (truncated toward zero)
     *
     * // Edge cases: the default covers only null and an empty toString()
     * Numbers.toByte((Object) null, (byte) 1);                       // returns 1
     * Numbers.toByte((Object) "", (byte) 5);                         // returns 5
     * Numbers.toByte((Object) "abc", (byte) 0);                      // throws NumberFormatException
     * Numbers.toByte((Object) Integer.valueOf(200), (byte) 0);       // throws ArithmeticException (outside byte range)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/{@code BigDecimal} whose truncated
     * value lies outside the {@code byte} range throws {@code ArithmeticException}, while a fractional value whose
     * truncation fits converts (e.g. {@code 127.9} &rarr; {@code 127}). {@code NaN} and infinite values throw
     * {@code ArithmeticException} (same policy as {@link #convert(Number, Class)}). A non-standard {@code Number} subtype is truncated and range-checked by
     * the same rule, recovered from its <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. That text is decimal only: a hexadecimal or {@code L}-suffixed
     * spelling is not a {@code Number}'s own rendering and takes the {@code doubleValue()} route.
     * The {@code defaultValue} applies
     * to a {@code null} object and to a non-{@code Number} whose {@code toString()} is empty or {@code null}; it does not
     * apply to {@code NaN}, and a malformed string still throws.
     * <b>By design:</b> a {@code Number} may be fractional ({@code 12.9} &rarr; {@code 12}); the string {@code "12.9"}
     * is not an integer token and throws. Object conversion is a numeric coercion; {@link #toByte(String, byte)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @param defaultValue the default value to return if the object is {@code null}, or if it is not a {@code Number} and {@code obj.toString()} is empty or {@code null}.
     * @return the byte representation of the provided object, or the default value if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the byte range.
     * @see #toByte(String)
     * @see #toByte(String, byte)
     * @see #toByte(Object)
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see #decodeInteger(String)
     */
    public static byte toByte(final Object obj, final byte defaultValue) throws NumberFormatException, ArithmeticException {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Byte) {
            return ((Byte) obj);
        } else if (obj instanceof Number) {
            return (byte) toLongWithinRange((Number) obj, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte");
        }

        return toByte(obj.toString(), defaultValue);
    }

    /**
     * Converts the given string to a short value.
     *
     * <p>This method attempts to convert the provided string to a short. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a short.
     * A trailing {@code 'L'} or {@code 'l'} suffix (Java long-literal style, e.g. {@code "1234L"}) is accepted.</p>
     *
     * <p><b>Radix:</b> decimal-first, same policy as {@link #toInt(String)}. Leading zeros are decimal padding, not
     * octal ({@code "010"} is 10). A {@code 0x}/{@code 0X}/{@code #} prefix selects hexadecimal, which is still
     * checked against the short range; every other token is decimal. Use {@link #decodeInteger(String)} for
     * leading-zero octal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toShort("1234");          // returns 1234
     * Numbers.toShort("-5678");         // returns -5678
     * Numbers.toShort("32767");         // returns 32767
     * Numbers.toShort("010");           // returns 10 (decimal, not octal)
     * Numbers.toShort("0x7FFF");        // returns 32767 (the 0x prefix selects hexadecimal)
     *
     * // Edge cases
     * Numbers.toShort((String) null);   // returns 0
     * Numbers.toShort("");              // returns 0
     * Numbers.toShort("0x8000");        // throws ArithmeticException (outside short range)
     * }</pre>
     *
     * @param str the string to convert. This can be any instance of String.
     * @return the short representation of the provided string, or {@code 0} if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the short range.
     * @see #toShort(String, short)
     * @see #toShort(Object)
     * @see #toInt(String)
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     */
    public static short toShort(final String str) throws NumberFormatException, ArithmeticException {
        return toShort(str, (short) 0);
    }

    /**
     * Converts the given object to a short value.
     *
     * <p>This method attempts to convert the provided object to a short. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as a short, so an empty or {@code null} {@code toString()} also returns {@code 0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toShort((Object) Integer.valueOf(1234));          // returns 1234
     * Numbers.toShort((Object) Short.valueOf((short) 32767));   // returns 32767
     * Numbers.toShort((Object) "5678");                         // returns 5678
     * Numbers.toShort((Object) Double.valueOf(12.9));           // returns 12 (truncated toward zero)
     *
     * // Edge cases
     * Numbers.toShort((Object) null);                           // returns 0
     * Numbers.toShort((Object) "");                             // returns 0
     * Numbers.toShort((Object) Integer.valueOf(32768));         // throws ArithmeticException (outside short range)
     * Numbers.toShort((Object) "12.9");                         // throws NumberFormatException (not an integer token)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/{@code BigDecimal} whose truncated
     * value lies outside the {@code short} range throws {@code ArithmeticException}, while a fractional value whose
     * truncation fits converts (e.g. {@code 32767.9} &rarr; {@code 32767}). {@code NaN} and infinite values throw
     * {@code ArithmeticException} (same policy as {@link #convert(Number, Class)}). A non-standard {@code Number} subtype is truncated and range-checked by
     * the same rule, recovered from its <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. That text is decimal only: a hexadecimal or {@code L}-suffixed
     * spelling is not a {@code Number}'s own rendering and takes the {@code doubleValue()} route.
     * <b>By design:</b> a {@code Number} may be fractional ({@code 12.9} &rarr; {@code 12}); the string {@code "12.9"}
     * is not an integer token and throws. Object conversion is a numeric coercion; {@link #toShort(String)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @return the short representation of the provided object, or {@code 0} if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the short range.
     * @see #toShort(String)
     * @see #toShort(String, short)
     * @see #toShort(Object, short)
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see #decodeInteger(String)
     */
    public static short toShort(final Object obj) throws NumberFormatException, ArithmeticException {
        return toShort(obj, (short) 0);
    }

    /**
     * Converts the given string to a short value.
     *
     * <p>This method attempts to convert the provided string to a short. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a short.
     * A trailing {@code 'L'} or {@code 'l'} suffix (Java long-literal style, e.g. {@code "1234L"}) is accepted.
     * Decimal-first radix, same as {@link #toInt(String, int)}: {@code "010"} is 10, not octal 8;
     * a {@code 0x}/{@code 0X}/{@code #} prefix selects range-checked hexadecimal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toShort("1234", (short) 0);          // returns 1234
     * Numbers.toShort("-5678", (short) 0);         // returns -5678
     * Numbers.toShort("32767", (short) 0);         // returns 32767
     * Numbers.toShort("0x7FFF", (short) 0);        // returns 32767 (the 0x prefix selects hexadecimal)
     *
     * // Edge cases: the default covers only null and empty input
     * Numbers.toShort((String) null, (short) 1);   // returns 1
     * Numbers.toShort("", (short) 1);              // returns 1
     * Numbers.toShort("abc", (short) 0);           // throws NumberFormatException
     * Numbers.toShort("32768", (short) 0);         // throws ArithmeticException (out of range)
     * }</pre>
     *
     * <p><b>By design:</b> {@code defaultValue} applies only to {@code null} or empty input. A malformed string
     * still throws {@code NumberFormatException} and an out-of-range value still throws {@code ArithmeticException}.
     * This is a missing-input fallback, not a parse-failure fallback.</p>
     *
     * <p><b>By design:</b> a string must be an integer token. {@code "12.9"} throws {@code NumberFormatException}.
     * A {@code Number} passed to {@link #toShort(Object, short)} is truncated toward zero and range-checked.
     * String parsing is a text grammar; Object conversion is a numeric coercion.</p>
     *
     * @param str the string to convert. This can be any instance of String.
     * @param defaultValue the default value to return if the string is {@code null} or empty.
     * @return the short representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the short range.
     * @see #toShort(String)
     * @see #toShort(Object)
     * @see #toShort(Object, short)
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see #decodeInteger(String)
     */
    public static short toShort(final String str, final short defaultValue) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);

            if (result != null) {
                if (result < Short.MIN_VALUE || result > Short.MAX_VALUE) {
                    throw numberOverflow("short", result);
                }

                return result.shortValue();
            }
        }

        return (short) parseLongInRange(str, Short.MIN_VALUE, Short.MAX_VALUE, "short");
    }

    /**
     * Converts the given object to a short value.
     *
     * <p>This method attempts to convert the provided object to a short. If the object is {@code null},
     * the provided default value is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as a short, so an empty or {@code null} {@code toString()} also returns the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toShort((Object) Integer.valueOf(1234), (short) 0);          // returns 1234
     * Numbers.toShort((Object) Short.valueOf((short) 32767), (short) 0);   // returns 32767
     * Numbers.toShort((Object) "5678", (short) 0);                         // returns 5678
     * Numbers.toShort((Object) Double.valueOf(12.9), (short) 0);           // returns 12 (truncated toward zero)
     *
     * // Edge cases: the default covers only null and an empty toString()
     * Numbers.toShort((Object) null, (short) 1);                           // returns 1
     * Numbers.toShort((Object) "", (short) 5);                             // returns 5
     * Numbers.toShort((Object) "abc", (short) 0);                          // throws NumberFormatException
     * Numbers.toShort((Object) Integer.valueOf(32768), (short) 0);         // throws ArithmeticException (outside short range)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/{@code BigDecimal} whose truncated
     * value lies outside the {@code short} range throws {@code ArithmeticException}, while a fractional value whose
     * truncation fits converts (e.g. {@code 32767.9} &rarr; {@code 32767}). {@code NaN} and infinite values throw
     * {@code ArithmeticException} (same policy as {@link #convert(Number, Class)}). A non-standard {@code Number} subtype is truncated and range-checked by
     * the same rule, recovered from its <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. That text is decimal only: a hexadecimal or {@code L}-suffixed
     * spelling is not a {@code Number}'s own rendering and takes the {@code doubleValue()} route.
     * The {@code defaultValue} applies
     * to a {@code null} object and to a non-{@code Number} whose {@code toString()} is empty or {@code null}; it does not
     * apply to {@code NaN}, and a malformed string still throws.
     * <b>By design:</b> a {@code Number} may be fractional ({@code 12.9} &rarr; {@code 12}); the string {@code "12.9"}
     * is not an integer token and throws. Object conversion is a numeric coercion; {@link #toShort(String, short)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @param defaultValue the default value to return if the object is {@code null}, or if it is not a {@code Number} and {@code obj.toString()} is empty or {@code null}.
     * @return the short representation of the provided object, or the default value if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the short range.
     * @see #toShort(String)
     * @see #toShort(String, short)
     * @see #toShort(Object)
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see #decodeInteger(String)
     */
    public static short toShort(final Object obj, final short defaultValue) throws NumberFormatException, ArithmeticException {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Short) {
            return ((Short) obj);
        } else if (obj instanceof Number) {
            return (short) toLongWithinRange((Number) obj, Short.MIN_VALUE, Short.MAX_VALUE, "short");
        }

        return toShort(obj.toString(), defaultValue);
    }

    /**
     * Converts the given string to an integer value.
     *
     * <p>This method attempts to convert the provided string to an integer. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as an integer.
     * A trailing {@code 'L'} or {@code 'l'} suffix (Java long-literal style, e.g. {@code "12345L"}) is accepted.</p>
     *
     * <p><b>Radix:</b> decimal-first. Leading zeros are <em>not</em> octal ({@code "010"} is 10, not 8;
     * {@code "08"} is 8). A {@code 0x}/{@code 0X}/{@code #} prefix selects hexadecimal (so {@code "0x10"} is 16);
     * every other token is decimal. This is deliberately different from {@link #decodeInteger(String)},
     * which follows {@link Integer#decode(String)} radix rules and reads {@code "010"} as 8. See the class-level
     * policy matrix.
     * Non-ASCII Unicode digits are rejected even though {@link Long#parseLong(String)} would accept them.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toInt("12345");         // returns 12345
     * Numbers.toInt("-98765");        // returns -98765
     * Numbers.toInt("2147483647");    // returns 2147483647
     * Numbers.toInt("010");           // returns 10 (decimal; not octal 8)
     * Numbers.toInt("08");            // returns 8
     * Numbers.toInt("0x10");          // returns 16 (the 0x prefix selects hexadecimal)
     *
     * // Edge cases
     * Numbers.toInt((String) null);   // returns 0
     * Numbers.toInt("");              // returns 0
     * Numbers.toInt("١٢٣");           // throws NumberFormatException (non-ASCII digits)
     * }</pre>
     *
     * @param str the string to convert. This can be any instance of String.
     * @return the integer representation of the provided string, or {@code 0} if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the int range.
     * @see #toInt(String, int)
     * @see #toInt(Object)
     * @see #tryParseInt(String)
     * @see #decodeInteger(String)
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     */
    public static int toInt(final String str) throws NumberFormatException, ArithmeticException {
        return toInt(str, 0);
    }

    /**
     * Converts the given object to an integer value.
     *
     * <p>This method attempts to convert the provided object to an integer. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as an integer, so an empty or {@code null} {@code toString()} also returns {@code 0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toInt((Object) Long.valueOf(12345));           // returns 12345
     * Numbers.toInt((Object) Integer.valueOf(2147483647));   // returns 2147483647
     * Numbers.toInt((Object) "98765");                       // returns 98765
     * Numbers.toInt((Object) Double.valueOf(12.9));          // returns 12 (truncated toward zero)
     *
     * // Edge cases
     * Numbers.toInt((Object) null);                          // returns 0
     * Numbers.toInt((Object) "");                            // returns 0
     * Numbers.toInt((Object) Long.valueOf(2147483648L));     // throws ArithmeticException (outside int range)
     * Numbers.toInt((Object) Double.valueOf(Double.NaN));    // throws ArithmeticException (not representable)
     * Numbers.toInt((Object) "12.9");                        // throws NumberFormatException (not an integer token)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/{@code BigDecimal} whose truncated
     * value lies outside the {@code int} range throws {@code ArithmeticException}, while a fractional value whose
     * truncation fits converts (e.g. {@code Double.valueOf(2147483647.9d)} &rarr; {@code 2147483647}). A {@code Float}
     * is checked at its already-rounded, representable value: {@code Integer.MAX_VALUE} is not representable as a
     * {@code float}, so {@code (float) Integer.MAX_VALUE} equals {@code 2^31} and throws {@code ArithmeticException}.
     * {@code NaN} and infinite values also throw {@code ArithmeticException} (same policy as
     * {@link #convert(Number, Class)}). A non-standard {@code Number} subtype is truncated and range-checked by the same rule, recovered from its <a
     * href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its {@code doubleValue()}, because {@code longValue()} is
     * allowed to wrap for such a type. That text is decimal only: a hexadecimal or {@code L}-suffixed spelling is not a {@code Number}'s own rendering and
     * takes the {@code doubleValue()} route.
     * <b>By design:</b> a {@code Number} may be fractional ({@code 12.9} &rarr; {@code 12}); the string {@code "12.9"}
     * is not an integer token and throws. Object conversion is a numeric coercion; {@link #toInt(String)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @return the integer representation of the provided object, or {@code 0} if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the int range.
     * @see #toInt(String)
     * @see #toInt(String, int)
     * @see #toInt(Object, int)
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     * @see #decodeInteger(String)
     */
    public static int toInt(final Object obj) throws NumberFormatException, ArithmeticException {
        return toInt(obj, 0);
    }

    /**
     * Converts the given string to an integer value.
     *
     * <p>This method attempts to convert the provided string to an integer. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as an integer.
     * A trailing {@code 'L'} or {@code 'l'} suffix (Java long-literal style, e.g. {@code "12345L"}) is accepted.
     * Decimal-first radix, same as {@link #toInt(String)}: {@code "010"} is 10, not octal 8;
     * {@code "0x10"} is 16 (a {@code 0x} prefix selects hexadecimal). See {@link #decodeInteger(String)} for
     * Java-literal octal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toInt("12345", 0);         // returns 12345
     * Numbers.toInt("-98765", 0);        // returns -98765
     * Numbers.toInt("2147483647", 0);    // returns 2147483647
     * Numbers.toInt("010", 0);           // returns 10 (decimal; not octal 8)
     *
     * // Edge cases: the default covers only null and empty input
     * Numbers.toInt((String) null, 1);   // returns 1
     * Numbers.toInt("", 1);              // returns 1
     * Numbers.toInt("abc", 0);           // throws NumberFormatException
     * Numbers.toInt("2147483648", 0);    // throws ArithmeticException (out of range)
     * }</pre>
     *
     * <p><b>By design:</b> {@code defaultValue} applies only to {@code null} or empty input. A malformed string
     * still throws {@code NumberFormatException} and an out-of-range value still throws {@code ArithmeticException}.
     * This is a missing-input fallback, not a parse-failure fallback.</p>
     *
     * <p><b>By design:</b> a string must be an integer token. {@code "12.9"} throws {@code NumberFormatException}.
     * A {@code Number} passed to {@link #toInt(Object, int)} is truncated toward zero and range-checked.
     * String parsing is a text grammar; Object conversion is a numeric coercion.</p>
     *
     * @param str the string to convert. This can be any instance of String.
     * @param defaultValue the default value to return if the string is {@code null} or empty.
     * @return the integer representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the int range.
     * @see #toInt(String)
     * @see #toInt(Object)
     * @see #toInt(Object, int)
     * @see #decodeInteger(String)
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     */
    public static int toInt(final String str, final int defaultValue) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);

            if (result != null) {
                return result;
            }
        }

        return (int) parseLongInRange(str, Integer.MIN_VALUE, Integer.MAX_VALUE, "int");
    }

    /**
     * Converts the given object to an integer value.
     *
     * <p>This method attempts to convert the provided object to an integer. If the object is {@code null},
     * the provided default value is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as an integer, so an empty or {@code null} {@code toString()} also returns the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toInt((Object) Long.valueOf(12345), 0);           // returns 12345
     * Numbers.toInt((Object) Integer.valueOf(2147483647), 0);   // returns 2147483647
     * Numbers.toInt((Object) "98765", 0);                       // returns 98765
     * Numbers.toInt((Object) Double.valueOf(12.9), 0);          // returns 12 (truncated toward zero)
     *
     * // Edge cases: the default covers only null and an empty toString()
     * Numbers.toInt((Object) null, 1);                          // returns 1
     * Numbers.toInt((Object) "", 5);                            // returns 5
     * Numbers.toInt((Object) "abc", 0);                         // throws NumberFormatException
     * Numbers.toInt((Object) Long.valueOf(2147483648L), 0);     // throws ArithmeticException (outside int range)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/{@code BigDecimal} whose truncated
     * value lies outside the {@code int} range throws {@code ArithmeticException}, while a fractional value whose
     * truncation fits converts (e.g. {@code Double.valueOf(2147483647.9d)} &rarr; {@code 2147483647}). A {@code Float}
     * is checked at its already-rounded, representable value: {@code Integer.MAX_VALUE} is not representable as a
     * {@code float}, so {@code (float) Integer.MAX_VALUE} equals {@code 2^31} and throws {@code ArithmeticException}.
     * {@code NaN} and infinite values also throw {@code ArithmeticException} (same policy as
     * {@link #convert(Number, Class)}). A non-standard {@code Number} subtype is truncated and range-checked by the same rule, recovered from its <a
     * href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its {@code doubleValue()}, because {@code longValue()} is
     * allowed to wrap for such a type. That text is decimal only: a hexadecimal or {@code L}-suffixed spelling is not a {@code Number}'s own rendering and
     * takes the {@code doubleValue()} route.
     * The {@code defaultValue}
     * applies to a {@code null} object and to a non-{@code Number} whose {@code toString()} is empty or {@code null};
     * it does not apply to {@code NaN}, and a malformed string still throws.
     * <b>By design:</b> a {@code Number} may be fractional ({@code 12.9} &rarr; {@code 12}); the string {@code "12.9"}
     * is not an integer token and throws. Object conversion is a numeric coercion; {@link #toInt(String, int)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @param defaultValue the default value to return if the object is {@code null}, or if it is not a {@code Number} and {@code obj.toString()} is empty or {@code null}.
     * @return the integer representation of the provided object, or the default value if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the int range.
     * @see #toInt(String)
     * @see #toInt(String, int)
     * @see #toInt(Object)
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     * @see #decodeInteger(String)
     */
    public static int toInt(final Object obj, final int defaultValue) throws NumberFormatException, ArithmeticException {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Integer) {
            return ((Integer) obj);
        } else if (obj instanceof Number) {
            return (int) toLongWithinRange((Number) obj, Integer.MIN_VALUE, Integer.MAX_VALUE, "int");
        }

        return toInt(obj.toString(), defaultValue);
    }

    /**
     * Narrows the given {@code Number} to a {@code long} whose value must lie within {@code [min, max]},
     * throwing {@code ArithmeticException} otherwise. A {@code Float}/{@code Double}/{@code BigDecimal} is first
     * truncated toward zero (JLS narrowing) and the resulting integer part is range-checked, so a fractional value
     * whose truncation fits converts (e.g. {@code 127.9 -> byte 127}) while an out-of-range integer part overflows.
     * {@code NaN} and infinite values throw (same policy as {@link #convert(Number, Class)}). {@code BigInteger}/
     * {@code BigDecimal} are range-checked via {@code compareTo} ({@code BigDecimal} against truncation-aware
     * exclusive bounds), not {@code longValue()} directly, which would silently wrap mod 2^64 for magnitudes
     * beyond the {@code long} range. The four primitive target ranges reuse the precomputed {@code BigInteger}
     * bounds, avoiding two temporary bound objects per {@code BigInteger} conversion. The standard wrappers
     * ({@code Byte}/{@code Short}/{@code Integer}/{@code Long}) use {@code longValue()} (exact for those types).
     * Any other {@code Number} subtype is narrowed by
     * {@link #unknownNumberToLongWithinRange(Number, long, long, String)}, which applies the same
     * truncate-then-range-check policy without trusting the subtype's {@code longValue()}.
     *
     * <p>This is the single statement of that policy: it backs {@link #toByte(Object, byte)},
     * {@link #toShort(Object, short)}, {@link #toInt(Object, int)}, {@link #toLong(Object, long)}, every lossy
     * integral entry of {@link #numberConverterFuncMap}, and the unknown-source fallback of
     * {@link #convert(Number, Class)}.</p>
     */
    private static long toLongWithinRange(final Number num, final long min, final long max, final String typeName) {
        if (num instanceof BigInteger) {
            final BigInteger bi = (BigInteger) num;
            final BigInteger minValue;
            final BigInteger maxValue;

            if (min == Byte.MIN_VALUE && max == Byte.MAX_VALUE) {
                minValue = BIG_INTEGER_WITH_MIN_BYTE_VALUE;
                maxValue = BIG_INTEGER_WITH_MAX_BYTE_VALUE;
            } else if (min == Short.MIN_VALUE && max == Short.MAX_VALUE) {
                minValue = BIG_INTEGER_WITH_MIN_SHORT_VALUE;
                maxValue = BIG_INTEGER_WITH_MAX_SHORT_VALUE;
            } else if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
                minValue = BIG_INTEGER_WITH_MIN_INT_VALUE;
                maxValue = BIG_INTEGER_WITH_MAX_INT_VALUE;
            } else if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
                minValue = BIG_INTEGER_WITH_MIN_LONG_VALUE;
                maxValue = BIG_INTEGER_WITH_MAX_LONG_VALUE;
            } else {
                minValue = BigInteger.valueOf(min);
                maxValue = BigInteger.valueOf(max);
            }

            if (bi.compareTo(minValue) < 0 || bi.compareTo(maxValue) > 0) {
                throw numberOverflow(typeName, num);
            }

            return bi.longValue();
        }

        if (num instanceof BigDecimal) {
            final BigDecimal bd = (BigDecimal) num;
            final boolean overflow;

            if (min == Byte.MIN_VALUE && max == Byte.MAX_VALUE) {
                overflow = bd.compareTo(BIG_DECIMAL_WITH_MIN_BYTE_VALUE_MINUS_ONE) <= 0 || bd.compareTo(BIG_DECIMAL_WITH_MAX_BYTE_VALUE_PLUS_ONE) >= 0;
            } else if (min == Short.MIN_VALUE && max == Short.MAX_VALUE) {
                overflow = bd.compareTo(BIG_DECIMAL_WITH_MIN_SHORT_VALUE_MINUS_ONE) <= 0 || bd.compareTo(BIG_DECIMAL_WITH_MAX_SHORT_VALUE_PLUS_ONE) >= 0;
            } else if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
                overflow = bd.compareTo(BIG_DECIMAL_WITH_MIN_INT_VALUE_MINUS_ONE) <= 0 || bd.compareTo(BIG_DECIMAL_WITH_MAX_INT_VALUE_PLUS_ONE) >= 0;
            } else if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
                overflow = bd.compareTo(BIG_DECIMAL_WITH_MIN_LONG_VALUE_MINUS_ONE) <= 0 || bd.compareTo(BIG_DECIMAL_WITH_MAX_LONG_VALUE_PLUS_ONE) >= 0;
            } else {
                final boolean belowMin = min <= 0 ? bd.compareTo(BigDecimal.valueOf(min).subtract(BigDecimal.ONE)) <= 0
                        : bd.compareTo(BigDecimal.valueOf(min)) < 0;
                final boolean aboveMax = max >= 0 ? bd.compareTo(BigDecimal.valueOf(max).add(BigDecimal.ONE)) >= 0 : bd.compareTo(BigDecimal.valueOf(max)) > 0;
                overflow = belowMin || aboveMax;
            }

            if (overflow) {
                throw numberOverflow(typeName, num);
            }

            return bd.longValue();
        }

        if (num instanceof Float || num instanceof Double) {
            // Truncate toward zero (JLS narrowing) and range-check the integer part: a fractional value whose
            // truncation fits converts (e.g. 127.9 -> byte 127). Reject values outside the long conversion
            // interval before the narrowing cast, which would otherwise silently saturate at Long.MIN/MAX_VALUE.
            return doubleToLongWithinRange(num, num.doubleValue(), min, max, typeName, null);
        }

        if (num instanceof Byte || num instanceof Short || num instanceof Integer || num instanceof Long) {
            final long lng = num.longValue();

            if (lng < min || lng > max) {
                throw numberOverflow(typeName, num);
            }

            return lng;
        }

        return unknownNumberToLongWithinRange(num, min, max, typeName);
    }

    /**
     * Truncates {@code d} toward zero (JLS narrowing) and range-checks the integer part, reporting {@code num}
     * itself in any overflow message. Values outside the {@code long} conversion interval are rejected before
     * the cast, which would otherwise silently saturate at {@code Long.MIN_VALUE}/{@code Long.MAX_VALUE}.
     *
     * @param knownText the source's already-computed rendering, or {@code null} when the caller has none.
     *        Passing it avoids a second {@code toString()}: it is the subtype's own code, may be arbitrarily
     *        expensive, and - for the mutable subtypes this recovery exists for - a second reading could render
     *        a different value again. The message then reports the rendering the recovery itself worked from,
     *        rather than a third view of the value. The {@code BigInteger}/{@code BigDecimal} recoveries
     *        already thread it through.
     */
    private static long doubleToLongWithinRange(final Number num, final double d, final long min, final long max, final String typeName,
            final String knownText) {
        if (Double.isNaN(d) || Double.isInfinite(d) || d < -0x1p63 || d >= 0x1p63) {
            throw numberOverflow(typeName, num, knownText);
        }

        final long truncated = (long) d;

        if (truncated < min || truncated > max) {
            throw numberOverflow(typeName, num, knownText);
        }

        return truncated;
    }

    /**
     * Narrows a {@code Number} subtype outside the eight this class converts directly ({@code AtomicInteger},
     * {@code LongAdder}, {@code DoubleAdder}, an application's own fixed-point type, ...). {@code longValue()}
     * is specified to be allowed to wrap for such a type, so the value is recovered from its canonical string
     * in three steps, each falling through to the next:
     *
     * <ol>
     *   <li><b>Plain integer token</b> &mdash; the common case ({@code AtomicInteger}, {@code AtomicLong},
     *       {@code LongAdder}). Scanned in place, with no substring, no allocation and no exception
     *       construction.</li>
     *   <li><b>Decimal token</b> &mdash; a fractional or exponent spelling ({@code DoubleAdder}, a fixed-point
     *       type), or an integer token that does not fit {@code [min, max]}. Located by
     *       {@link #scanDecimalText(String)} and truncated toward zero by
     *       {@link #truncateDecimalTextWithinRange(String, DecimalTextScan, Number, long, long, String)}, so
     *       {@code 12.9} becomes {@code 12} exactly as a {@code Double} would. Going straight to
     *       {@code doubleValue()} here would be wrong at the boundary: a value whose integer part is exactly
     *       {@code Long.MAX_VALUE} rounds up to 2<sup>63</sup> as a {@code double} and would be rejected even
     *       though its truncation fits.</li>
     *   <li><b>Non-canonical text</b> &mdash; a subtype whose {@code toString()} is formatted, localized,
     *       unit-suffixed or {@code null}. Falls back to the finite- and range-checked {@code doubleValue()}
     *       view rather than reporting a parse failure, because this is a numeric coercion and not a text
     *       parse.</li>
     * </ol>
     *
     * <p><b>The recovery grammar is {@link #scanDecimalText(String)} and nothing else</b>, which is what makes
     * {@code convert(x, Integer.class)} and {@code convert(x, BigInteger.class)} agree on the same source:
     * {@link #unknownNumberToBigDecimal(Number, String)} accepts exactly the same texts. Step 1 therefore uses
     * {@link IntegerTokenSyntax#PLAIN_DECIMAL} rather than the {@code DECIMAL_FIRST} grammar of
     * {@link #toInt(String)}: a hexadecimal or {@code L}-suffixed spelling is caller-supplied <em>text</em>,
     * never a {@code Number}'s canonical rendering, and accepting it here made the integral targets answer
     * {@code 255} for a subtype printing {@code "0xFF"} while the {@code BigInteger} target answered from
     * {@code doubleValue()}.</p>
     */
    private static long unknownNumberToLongWithinRange(final Number num, final long min, final long max, final String typeName) {
        final String text = num.toString();

        if (text == null) {
            // toString() may legally return null. There is then no canonical text to recover a value from,
            // so go straight to the doubleValue() view instead of dereferencing it (step 3).
            return doubleToLongWithinRange(num, num.doubleValue(), min, max, typeName, null);
        }

        final int end = text.length();
        final long value = scanIntegerTokenValueOrInvalid(text, 0, end, IntegerTokenSyntax.PLAIN_DECIMAL, min, max);

        if (value != INVALID_INTEGER_TOKEN || (min == Long.MIN_VALUE && isLongMinValueToken(text, 0, end, IntegerTokenSyntax.PLAIN_DECIMAL))) {
            return value;
        }

        final DecimalTextScan scan = scanDecimalText(text);

        if (scan == null) {
            // Hand the text on: it has just been computed, and an overflow message built from a second
            // toString() could describe a different value for a mutable subtype.
            return doubleToLongWithinRange(num, num.doubleValue(), min, max, typeName, text);
        }

        return truncateDecimalTextWithinRange(text, scan, num, min, max, typeName);
    }

    /**
     * Returns {@code str} as a {@code BigDecimal}, or {@code null} if it is not one. Used where a parse failure
     * is a routing decision rather than an error, so no exception is constructed for the common rejection.
     *
     * <p>The gate is {@link #scanDecimalText(String)}, whose accepted set is exactly the set
     * {@link BigDecimal#BigDecimal(String)} accepts restricted to ASCII digits, so the {@code catch} below is a
     * belt-and-braces guard rather than the routing decision it used to be &mdash; and, more importantly, the
     * integral recovery in {@link #unknownNumberToLongWithinRange(Number, long, long, String)} keys off the
     * same predicate, so both cannot disagree about whether a given text is usable.</p>
     */
    @MayReturnNull
    private static BigDecimal parseBigDecimalQuietly(final String str) {
        if (str == null || scanDecimalText(str) == null) {
            return null;
        }

        try {
            return new BigDecimal(str);
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * The maximum number of significant exponent digits {@link #scanDecimalText(String)} accepts, matching the
     * limit {@link BigDecimal#BigDecimal(String)} enforces ("Too many nonzero exponent digits"). Leading zeros
     * in the exponent do not count, so {@code "1e0000000005"} is accepted.
     */
    private static final int MAX_DECIMAL_TEXT_EXPONENT_DIGITS = 10;

    /**
     * Above this decimal point position the magnitude is at least 10<sup>19</sup>, which exceeds every
     * {@code long}, so the value can be rejected without looking at a single digit.
     */
    private static final int MAX_LONG_DECIMAL_DIGITS = 19;

    /**
     * The parts of a plain ASCII decimal token, as located by {@link #scanDecimalText(String)}.
     *
     * <p>The value the token denotes is
     * {@code (negative ? -1 : +1) * 0.<significand digits from firstDigit> * 10^pointPosition}, so
     * {@code pointPosition <= 0} means a magnitude below one and {@code pointPosition} above
     * {@value #MAX_LONG_DECIMAL_DIGITS} a magnitude above every {@code long}. Only indexes are recorded; no
     * part of the token is ever copied.</p>
     */
    private static final class DecimalTextScan {
        /** Index of the first significand digit that is not a leading zero, or {@code -1} when every digit is a zero. */
        int firstDigit;
        /** Index after the last significand digit, that is of the exponent marker or of the end of the text. */
        int significandEnd;
        /** Index of the decimal point, or {@link #significandEnd} when the token has none. */
        int pointIndex;
        /** Decimal point position relative to {@link #firstDigit}; see the class comment. */
        long pointPosition;
        /** Whether the token carries a {@code '-'} sign. */
        boolean negative;
    }

    /**
     * Locates the parts of {@code text} when it is a plain ASCII decimal token &mdash; an optional sign, then
     * digits with an optional decimal point, then an optional decimal exponent &mdash; and returns
     * {@code null} otherwise. Nothing is copied and nothing is parsed into an arbitrary-precision value: the
     * result is a handful of indexes plus the decimal exponent of the leading digit.
     *
     * <p><b>The accepted set is exactly {@link BigDecimal#BigDecimal(String)}'s, restricted to ASCII digits</b>
     * (that library method also accepts any character {@link Character#isDigit(char)} admits, which this class
     * rejects everywhere). That equality is the point of the method: it is the single definition of "the text
     * of a {@code Number} is usable", shared by the integral recovery in
     * {@link #unknownNumberToLongWithinRange(Number, long, long, String)} and by
     * {@link #parseBigDecimalQuietly(String)}, so no target can decide differently from another. The two
     * conditions beyond the grammar are {@code BigDecimal}'s own: at most
     * {@value #MAX_DECIMAL_TEXT_EXPONENT_DIGITS} significant exponent digits, and an effective scale
     * ({@code fraction digits - exponent}) within the {@code int} range.
     *
     * <p>Whitespace is not trimmed, deliberately: this text is a {@code Number}'s own rendering, and the
     * integral recovery has never trimmed either.</p>
     *
     * @param text the text to classify; must not be {@code null}
     * @return the located parts, or {@code null} when {@code text} is not a plain ASCII decimal token
     */
    @MayReturnNull
    private static DecimalTextScan scanDecimalText(final String text) {
        final int length = text.length();

        if (length == 0) {
            return null;
        }

        final char sign = text.charAt(0);
        final int significandStart = sign == '-' || sign == '+' ? 1 : 0;
        int pos = significandStart;
        int pointIndex = -1;
        int firstDigit = -1;
        int digits = 0;

        while (pos < length) {
            final char ch = text.charAt(pos);

            if (ch >= '0' && ch <= '9') {
                digits++;

                if (firstDigit < 0 && ch != '0') {
                    firstDigit = pos;
                }
            } else if (ch == '.') {
                if (pointIndex >= 0) {
                    return null;
                }

                pointIndex = pos;
            } else if (ch == 'e' || ch == 'E') {
                break;
            } else {
                return null;
            }

            pos++;
        }

        if (digits == 0) {
            return null;
        }

        final int significandEnd = pos;
        final int fractionDigits = pointIndex < 0 ? 0 : significandEnd - pointIndex - 1;
        final long exponent = pos == length ? 0 : scanDecimalTextExponent(text, pos);

        if (exponent == INVALID_DECIMAL_EXPONENT) {
            return null;
        }

        // BigDecimal stores the token as unscaledValue * 10^-scale and rejects a scale outside the int range;
        // rejecting the same texts here is what keeps this predicate and parseBigDecimalQuietly in step.
        final long scale = fractionDigits - exponent;

        if (scale < Integer.MIN_VALUE || scale > Integer.MAX_VALUE) {
            return null;
        }

        final DecimalTextScan scan = new DecimalTextScan();
        scan.negative = sign == '-';
        scan.firstDigit = firstDigit;
        scan.significandEnd = significandEnd;
        scan.pointIndex = pointIndex < 0 ? significandEnd : pointIndex;

        if (firstDigit >= 0) {
            // Digits of the significand that lie before the decimal point, counted from the first significant
            // one. Negative when the first significant digit is itself past the point ("0.00123" is
            // 0.123 * 10^-2), which is why this is a signed count rather than a length.
            final int digitsBeforePoint = firstDigit < scan.pointIndex ? scan.pointIndex - firstDigit : scan.pointIndex - firstDigit + 1;
            scan.pointPosition = digitsBeforePoint + exponent;
        }

        return scan;
    }

    /** {@link #scanDecimalTextExponent(String, int)} outcome for an absent, malformed or over-long exponent. */
    private static final long INVALID_DECIMAL_EXPONENT = Long.MIN_VALUE;

    /**
     * Reads the exponent of a decimal token whose exponent marker sits at {@code markerIndex}, or returns
     * {@link #INVALID_DECIMAL_EXPONENT} when what follows is not a signed run of at most
     * {@value #MAX_DECIMAL_TEXT_EXPONENT_DIGITS} significant ASCII digits.
     */
    private static long scanDecimalTextExponent(final String text, final int markerIndex) {
        final int length = text.length();
        int pos = markerIndex + 1;
        boolean negative = false;

        if (pos < length && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
            negative = text.charAt(pos) == '-';
            pos++;
        }

        if (pos >= length) {
            return INVALID_DECIMAL_EXPONENT;
        }

        while (pos < length && text.charAt(pos) == '0') {
            pos++;
        }

        final int digitsStart = pos;
        long exponent = 0;

        while (pos < length) {
            final char ch = text.charAt(pos);

            if (ch < '0' || ch > '9') {
                return INVALID_DECIMAL_EXPONENT;
            }

            if (pos - digitsStart < MAX_DECIMAL_TEXT_EXPONENT_DIGITS) {
                exponent = exponent * 10 + (ch - '0');
            }

            pos++;
        }

        if (length - digitsStart > MAX_DECIMAL_TEXT_EXPONENT_DIGITS) {
            return INVALID_DECIMAL_EXPONENT;
        }

        return negative ? -exponent : exponent;
    }

    /**
     * Truncates the decimal token located by {@link #scanDecimalText(String)} toward zero and range-checks the
     * integer part against {@code [min, max]}, reporting {@code num} itself in any overflow message.
     *
     * <p>Only the digits that survive the truncation are accumulated &mdash; at most
     * {@value #MAX_LONG_DECIMAL_DIGITS} of them &mdash; so a token of any length costs one linear scan and no
     * allocation. The {@code BigDecimal} this replaced was quadratic in the digit count and subject to no
     * length limit: a {@code toString()} of a million digits took about 12 s and allocated tens of megabytes
     * to answer a question that never needed more than 19 digits.</p>
     */
    private static long truncateDecimalTextWithinRange(final String text, final DecimalTextScan scan, final Number num, final long min, final long max,
            final String typeName) {
        // A zero significand, and a magnitude below one, both truncate toward zero to zero.
        if (scan.firstDigit < 0 || scan.pointPosition <= 0) {
            if (0 < min || 0 > max) {
                throw numberOverflow(typeName, num, text);
            }

            return 0;
        }

        if (scan.pointPosition > MAX_LONG_DECIMAL_DIGITS) {
            throw numberOverflow(typeName, num, text);
        }

        // Digits are accumulated negatively, as in scanIntegerTokenValueOrInvalid, so that Long.MIN_VALUE is
        // representable and overflow is detected before it happens.
        final long limit = scan.negative ? min : -max;
        final long multmin = limit / 10;
        long result = 0;
        int pos = scan.firstDigit;

        for (int i = 0; i < scan.pointPosition; i++) {
            if (pos == scan.pointIndex && pos < scan.significandEnd) {
                pos++; // the decimal point sits between two significand digits and is not one of them
            }

            // Past the last significand digit the token is padded with zeros: a positive exponent moves the
            // point right beyond the digits that were written ("1.5e3" is 1500).
            final int digit = pos < scan.significandEnd ? text.charAt(pos++) - '0' : 0;

            if (result < multmin) {
                throw numberOverflow(typeName, num, text);
            }

            result *= 10;

            if (result < limit + digit) {
                throw numberOverflow(typeName, num, text);
            }

            result -= digit;
        }

        return scan.negative ? result : -result;
    }

    /**
     * Renders a {@code Number} subtype outside the eight this class converts directly as an exact
     * {@code BigDecimal}, by the same recovery {@link #unknownNumberToLongWithinRange} uses: the canonical
     * string when {@link #scanDecimalText(String)} accepts it (a {@code null} or non-numeric one is not),
     * otherwise the {@code doubleValue()} view. That method's separate integer-token step is unnecessary here,
     * since a {@code BigDecimal} parses an integer token too. Without this the arbitrary-precision targets were
     * the only built-in ones such a source could not reach, so {@code convert(x, BigInteger.class)} threw
     * {@code NumberFormatException} for a fractional value that {@code convert(x, Integer.class)} truncated.
     *
     * <p><b>Both recoveries gate on the same predicate</b>, so this target and the integral ones can never
     * decide differently about whether a given text is usable &mdash; which they did while this one used the
     * {@code BigDecimal} grammar and the integral one used {@link #toInt(String)}'s: a subtype printing
     * {@code "0xFF"} converted to {@code 255} as an {@code Integer} and to its {@code doubleValue()} as a
     * {@code BigInteger}.</p>
     *
     * <p>The {@code doubleValue()} fallback is {@link BigDecimal#valueOf(double)}, the canonical decimal
     * spelling: the same rule the {@code BigDecimal} target applies to a {@code Double} source. It is the
     * {@code BigDecimal} target's rule only &mdash; the {@code BigInteger} target has its own
     * ({@link #unknownNumberToBigInteger(Number)}), because truncating this spelling is not truncating the
     * value once the magnitude passes 2<sup>53</sup>.</p>
     *
     * <p>A non-finite value has no arbitrary-precision representation and is rejected as overflow, exactly as
     * it is for a directly supported source.</p>
     *
     * @param num the value to render
     * @param typeName the target type's display name, used in the overflow message
     * @return {@code num} as a {@code BigDecimal}
     * @throws ArithmeticException if {@code num} is {@code NaN} or infinite
     */
    private static BigDecimal unknownNumberToBigDecimal(final Number num, final String typeName) throws ArithmeticException {
        final String text = num.toString();
        final BigDecimal decimal = parseBigDecimalQuietly(text);

        if (decimal != null) {
            return decimal;
        }

        final double d = num.doubleValue();

        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw numberOverflow(typeName, num, text);
        }

        return BigDecimal.valueOf(d);
    }

    /**
     * Truncates a {@code Number} subtype outside the eight this class converts directly toward zero to a
     * {@code BigInteger}: the canonical string when {@link #scanDecimalText(String)} accepts it, exactly as
     * {@link #unknownNumberToBigDecimal(Number, String)} reads it, otherwise the {@code doubleValue()} view.
     *
     * <p>The fallback goes through {@link #doubleToBigInteger(double, Number, String)}, the exact-value rule
     * the table applies to a {@code Double} source, and <em>not</em> through the {@code BigDecimal} target's
     * {@link BigDecimal#valueOf(double)} rendering. It used to: the shortest round-tripping decimal of a
     * {@code double} is not its value above 2<sup>53</sup>, so a subtype with an unusable {@code toString()}
     * and a {@code doubleValue()} of {@code 4611686018427388928.0} converted to {@code Long}
     * {@code 4611686018427388928} but to {@code BigInteger} {@code 4611686018427389000} &mdash; larger than the
     * source, so not a truncation, and a different answer from every other integral target for the same value
     * (73.7% of the doubles in (2<sup>53</sup>, 2<sup>63</sup>) disagreed). The text branch was never affected:
     * a usable text is exact in both.</p>
     *
     * @param num the value to truncate
     * @return {@code num} truncated toward zero
     * @throws ArithmeticException if {@code num} is {@code NaN} or infinite, or if the result exceeds the JDK
     *         implementation's supported {@code BigInteger} magnitude
     */
    private static BigInteger unknownNumberToBigInteger(final Number num) throws ArithmeticException {
        final String text = num.toString();
        final BigDecimal decimal = parseBigDecimalQuietly(text);

        if (decimal != null) {
            return bigDecimalToBigInteger(decimal);
        }

        return doubleToBigInteger(num.doubleValue(), num, text);
    }

    /**
     * The single statement of how a {@code double} value becomes a {@code BigInteger}: the <em>exact</em>
     * binary value truncated toward zero, the same answer the {@code byte}/{@code short}/{@code int}/{@code long}
     * targets give. It backs the {@code convert} table's {@code Float}/{@code Double} to {@code BigInteger}
     * entries and the unknown-{@code Number} fallback, so the rule cannot be re-derived differently at one of
     * them.
     *
     * <p>Routing this through the canonical decimal string instead ({@code Float.toString} /
     * {@code BigDecimal.valueOf}, as the {@code BigDecimal} target does) truncated the shortest round-tripping
     * spelling rather than the value: {@code convert((float) Integer.MAX_VALUE, BigInteger.class)} answered
     * {@code 2147483600} where {@code convert(v, Long.class)} answered {@code 2147483648}, and above
     * 2<sup>24</sup> ({@code float}) or 2<sup>53</sup> ({@code double}) the result could even <em>exceed</em>
     * the source ({@code 33554448f} to {@code 33554450}), which is not a truncation at all.</p>
     *
     * <p>A magnitude below one truncates to zero, so the exact form is never built for it. Worth the branch
     * because the exact form is widest exactly where the answer is most trivial: a subnormal expands to ~750
     * significant digits, which measured 1,363 ns/op against 301 ns for the canonical-string route; short
     * circuited, the same input costs less than either. {@code -0.0} lands there too and yields
     * {@link BigInteger#ZERO}, which is what truncating it gives ({@code BigInteger} has no negative zero).</p>
     *
     * <p>A {@code NaN}/{@code ±Infinity} input cannot be represented as an exact value, so it is rejected with
     * {@link ArithmeticException}, consistent with the integer targets (a {@code float}/{@code double}
     * <em>target</em> instead preserves them).</p>
     *
     * @param d the value to truncate
     * @param source the {@code Number} it came from, reported in the overflow message
     * @param knownText {@code source.toString()} when the caller already holds it, otherwise {@code null}; see
     *        {@link #numberOverflow(String, Number, String)}
     * @return {@code d} truncated toward zero
     * @throws ArithmeticException if {@code d} is {@code NaN} or infinite
     */
    private static BigInteger doubleToBigInteger(final double d, final Number source, final String knownText) throws ArithmeticException {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw numberOverflow("BigInteger", source, knownText);
        }

        if (d > -1.0d && d < 1.0d) {
            return BigInteger.ZERO;
        }

        return new BigDecimal(d).toBigInteger();
    }

    /**
     * Converts the given string to a long value.
     *
     * <p>This method attempts to convert the provided string to a long. If the string is {@code null} or empty,
     * default value {@code 0L} is returned. Otherwise, the method attempts to parse the string as a long.
     * This method also supports the {@code "L"} or {@code "l"} suffix for long literals (e.g., {@code "123L"}).</p>
     *
     * <p><b>Radix:</b> decimal-first, same policy as {@link #toInt(String)}. Leading zeros are <em>not</em> octal
     * ({@code "010"} is 10, {@code "0123L"} is 123). A {@code 0x}/{@code 0X}/{@code #} prefix selects hexadecimal;
     * every other token is decimal. This is deliberately different from {@link #decodeLong(String)}
     * / {@link #createNumber(String)}, which read {@code "010"} as 8 and {@code "0123L"} as 83. See the class-level
     * policy matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toLong("123456789");             // returns 123456789L
     * Numbers.toLong("-987654321");            // returns -987654321L
     * Numbers.toLong("9223372036854775807");   // returns 9223372036854775807L
     * Numbers.toLong("123L");                  // returns 123L
     * Numbers.toLong("010");                   // returns 10L (decimal; not octal 8)
     * Numbers.toLong("0123L");                 // returns 123L (decimal; not octal 83)
     *
     * // Edge cases
     * Numbers.toLong((String) null);           // returns 0L
     * Numbers.toLong("");                      // returns 0L
     * }</pre>
     *
     * @param str the string to convert. This can be any instance of String.
     * @return the long representation of the provided string, or {@code 0L} if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the long range.
     * @see #toLong(String, long)
     * @see #toLong(Object)
     * @see #tryParseLong(String)
     * @see #decodeLong(String)
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     */
    public static long toLong(final String str) throws NumberFormatException, ArithmeticException {
        return toLong(str, 0L);
    }

    /**
     * Converts the given object to a long value.
     *
     * <p>This method attempts to convert the provided object to a long. If the object is {@code null},
     * default value {@code 0L} is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as a long, so an empty or {@code null} {@code toString()} also returns {@code 0L}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toLong((Object) Integer.valueOf(123456));              // returns 123456L
     * Numbers.toLong((Object) Long.valueOf(9223372036854775807L));   // returns 9223372036854775807L
     * Numbers.toLong((Object) "987654321");                          // returns 987654321L
     * Numbers.toLong((Object) Double.valueOf(12.9));                 // returns 12L (truncated toward zero)
     *
     * // Edge cases
     * Numbers.toLong((Object) null);                                 // returns 0L
     * Numbers.toLong((Object) "");                                   // returns 0L
     * Numbers.toLong((Object) Double.valueOf(1e300));                // throws ArithmeticException (outside long range)
     * Numbers.toLong((Object) "12.9");                               // throws NumberFormatException (not an integer token)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #toInt(Object)} and {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/
     * {@code BigDecimal} whose truncated value lies outside the {@code long} range throws {@code ArithmeticException},
     * while a fractional value whose truncation fits converts (e.g. a {@code BigDecimal} whose integer part equals
     * {@code Long.MAX_VALUE} converts even with a fractional part). {@code NaN} and infinite values throw
     * {@code ArithmeticException} (same policy as {@link #convert(Number, Class)}; they do not saturate at
     * {@code Long.MIN_VALUE}/{@code Long.MAX_VALUE}). A non-standard {@code Number} subtype is truncated and
     * range-checked by the same rule, recovered from its
     * <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type.
     * <b>By design:</b> a {@code Number} may be fractional; the string {@code "12.9"} is not an integer token and throws.
     * Object conversion is a numeric coercion; {@link #toLong(String)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @return the long representation of the provided object, or {@code 0L} if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the long range.
     * @see #toLong(String)
     * @see #toLong(String, long)
     * @see #toLong(Object, long)
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     * @see #decodeLong(String)
     */
    public static long toLong(final Object obj) throws NumberFormatException, ArithmeticException {
        return toLong(obj, 0L);
    }

    /**
     * Converts the given string to a long value.
     *
     * <p>This method attempts to convert the provided string to a long. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a long.
     * This method also supports the {@code "L"} or {@code "l"} suffix for long literals (e.g., {@code "123L"}).
     * Decimal-first radix, same as {@link #toLong(String)}: {@code "010"} is 10, {@code "0123L"} is 123;
     * not octal. See {@link #decodeLong(String)} for Java-literal octal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toLong("123456789", 0L);             // returns 123456789L
     * Numbers.toLong("-987654321", 0L);            // returns -987654321L
     * Numbers.toLong("9223372036854775807", 0L);   // returns 9223372036854775807L
     * Numbers.toLong("123L", 0L);                  // returns 123L
     * Numbers.toLong("010", 0L);                   // returns 10L (decimal; not octal 8)
     *
     * // Edge cases: the default covers only null and empty input
     * Numbers.toLong((String) null, 1L);           // returns 1L
     * Numbers.toLong("", 1L);                      // returns 1L
     * Numbers.toLong("abc", 0L);                   // throws NumberFormatException
     * Numbers.toLong("9223372036854775808", 0L);   // throws ArithmeticException (out of range)
     * }</pre>
     *
     * <p><b>By design:</b> {@code defaultValue} applies only to {@code null} or empty input. A malformed string
     * still throws {@code NumberFormatException} and an out-of-range value still throws {@code ArithmeticException}.
     * This is a missing-input fallback, not a parse-failure fallback.</p>
     *
     * <p><b>By design:</b> a string must be an integer token. {@code "12.9"} throws {@code NumberFormatException}.
     * A {@code Number} passed to {@link #toLong(Object, long)} is truncated toward zero and range-checked.
     * String parsing is a text grammar; Object conversion is a numeric coercion.</p>
     *
     * @param str the string to convert. This can be any instance of String.
     * @param defaultValue the default value to return if the string is {@code null} or empty.
     * @return the long representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is not a valid integer.
     * @throws ArithmeticException if the string represents an integer value outside the long range.
     * @see #toLong(String)
     * @see #toLong(Object)
     * @see #toLong(Object, long)
     * @see #decodeLong(String)
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     */
    public static long toLong(final String str, final long defaultValue) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);
            if (result != null) {
                return result;
            }
        }

        return parseLongInRange(str, Long.MIN_VALUE, Long.MAX_VALUE, "long");
    }

    /**
     * Converts the given object to a long value.
     *
     * <p>This method attempts to convert the provided object to a long. If the object is {@code null},
     * the provided default value is returned. If the object is a {@code Number}, its integer part is truncated
     * toward zero and range-checked. Otherwise, the object's string representation is parsed as a long, so an empty or {@code null} {@code toString()} also returns the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toLong((Object) Integer.valueOf(123456), 0L);              // returns 123456L
     * Numbers.toLong((Object) Long.valueOf(9223372036854775807L), 0L);   // returns 9223372036854775807L
     * Numbers.toLong((Object) "987654321", 0L);                          // returns 987654321L
     * Numbers.toLong((Object) Double.valueOf(12.9), 0L);                 // returns 12L (truncated toward zero)
     *
     * // Edge cases: the default covers only null and an empty toString()
     * Numbers.toLong((Object) null, 1L);                                 // returns 1L
     * Numbers.toLong((Object) "", 5L);                                   // returns 5L
     * Numbers.toLong((Object) "abc", 0L);                                // throws NumberFormatException
     * Numbers.toLong((Object) Double.valueOf(1e300), 0L);                // throws ArithmeticException (outside long range)
     * }</pre>
     *
     * <p><b>Note:</b> a {@code Number}'s integer part (truncated toward zero, per JLS narrowing) is range-checked
     * (consistent with {@link #toInt(Object, int)} and {@link #convert(Number, Class)}): a {@code Float}/{@code Double}/
     * {@code BigDecimal} whose truncated value lies outside the {@code long} range throws {@code ArithmeticException},
     * while a fractional value whose truncation fits converts (e.g. a {@code BigDecimal} whose integer part equals
     * {@code Long.MAX_VALUE} converts even with a fractional part). {@code NaN} and infinite values throw
     * {@code ArithmeticException} (same policy as {@link #convert(Number, Class)}; they do not saturate at
     * {@code Long.MIN_VALUE}/{@code Long.MAX_VALUE}). A non-standard {@code Number} subtype is truncated and
     * range-checked by the same rule, recovered from its
     * <a href="#unknown-number-recovery">canonical decimal text</a> when it has one and otherwise from its
     * {@code doubleValue()}, because {@code longValue()} is allowed to wrap for such a type. The {@code defaultValue} applies to a {@code null} object
     * and to a non-{@code Number} whose {@code toString()} is empty or {@code null}; it does not apply to {@code NaN},
     * and a malformed string still throws.
     * <b>By design:</b> a {@code Number} may be fractional; the string {@code "12.9"} is not an integer token and throws.
     * Object conversion is a numeric coercion; {@link #toLong(String, long)} is a text parse.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @param defaultValue the default value to return if the object is {@code null}, or if it is not a {@code Number} and {@code obj.toString()} is empty or {@code null}.
     * @return the long representation of the provided object, or the default value if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is not a valid integer.
     * @throws ArithmeticException if the value is {@code NaN} or infinite, or if the value (a {@code Number}, or the integer parsed from its string representation) is outside the long range.
     * @see #toLong(String)
     * @see #toLong(String, long)
     * @see #toLong(Object)
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     * @see #decodeLong(String)
     */
    public static long toLong(final Object obj, final long defaultValue) throws NumberFormatException, ArithmeticException {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Long) {
            return ((Long) obj);
        }

        if (obj instanceof Number) {
            return toLongWithinRange((Number) obj, Long.MIN_VALUE, Long.MAX_VALUE, "long");
        }

        return toLong(obj.toString(), defaultValue);
    }

    /**
     * Converts the given string to a float value.
     *
     * <p>This method attempts to convert the provided string to a float. If the string is {@code null} or empty,
     * default value {@code 0.0f} is returned. Otherwise, the method attempts to parse the string as a float.</p>
     *
     * <p><b>Grammar:</b> the full {@link Float#parseFloat(String)} grammar applies — leading and trailing
     * characters {@code <= U+0020} are trimmed (as by {@link String#trim()}), and hexadecimal floating-point
     * literals ({@code "0x1.8p1"}) and type suffixes ({@code "1.5f"}, {@code "2.5d"}) are accepted. The raw input,
     * including surrounding whitespace, must contain at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16
     * code units. Finite overflow yields signed infinity and underflow yields signed zero; neither is a parse
     * failure. The integer {@code to*} parsers (for example {@link #toInt(String)}) are stricter here: they
     * reject whitespace and the {@code f}/{@code d} suffixes (they accept a trailing {@code L}/{@code l} instead).
     * Both families accept ASCII digits only.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toFloat("123.45");        // returns 123.45f
     * Numbers.toFloat("3.14159");       // returns 3.14159f
     * Numbers.toFloat("-42.5");         // returns -42.5f
     * Numbers.toFloat("1.23e10");       // returns 1.23E10f
     * Numbers.toFloat("NaN");           // returns Float.NaN
     * Numbers.toFloat("Infinity");      // returns Float.POSITIVE_INFINITY
     *
     * // Edge cases
     * Numbers.toFloat((String) null);   // returns 0.0f
     * Numbers.toFloat("");              // returns 0.0f
     * Numbers.toFloat("1e40");          // returns Float.POSITIVE_INFINITY (overflow saturates, never throws)
     * Numbers.toFloat("abc");           // throws NumberFormatException
     * }</pre>
     *
     * @param str the string to convert. This can be any instance of String.
     * @return the float representation of the string, or {@code 0.0f} if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is longer than the documented limit or cannot be parsed as a float.
     * @see #toFloat(Object)
     * @see #toFloat(String, float)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final String str) throws NumberFormatException {
        return toFloat(str, 0.0f);
    }

    /**
     * Converts the given object to a float value.
     *
     * <p>This method attempts to convert the provided object to a float. If the object is {@code null},
     * default value {@code 0.0f} is returned. If the object is a {@code Number}, its float value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a float, so an empty or {@code null} {@code toString()} also returns {@code 0.0f}.</p>
     *
     * <p><b>By design:</b> a {@code Double} is narrowed by plain IEEE-754 rounding ({@code floatValue()}, ties
     * to even), exactly as {@code (float) d} is, so {@code 1.21d} becomes {@code 1.21f}. This is deliberately
     * <em>not</em> the mirror image of {@link #toDouble(Object)}, which widens a {@code Float} through its
     * decimal spelling: a {@code float}'s shortest decimal usually differs from its widened binary value
     * ({@code 1.21f} widened is {@code 1.2100000381469727}), whereas a {@code double}'s shortest decimal
     * narrows to the very same {@code float} as the value itself for every {@code double} except one lying
     * exactly halfway between two floats &mdash; and there a decimal round trip would pick whichever side its
     * spelling fell on instead of the even significand.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toFloat((Object) Double.valueOf(123.45));         // returns 123.45f
     * Numbers.toFloat((Object) Float.valueOf(3.14f));           // returns 3.14f
     * Numbers.toFloat((Object) "98.76");                        // returns 98.76f
     *
     * // Edge cases
     * Numbers.toFloat((Object) null);                           // returns 0.0f
     * Numbers.toFloat((Object) "");                             // returns 0.0f
     * Numbers.toFloat((Object) Double.valueOf(Double.NaN));     // returns Float.NaN (never throws on a Number)
     * Numbers.toFloat((Object) Double.valueOf(1e300));          // returns Float.POSITIVE_INFINITY (saturates)
     * Numbers.toFloat((Object) "abc");                          // throws NumberFormatException
     * }</pre>
     *
     * <p><b>Note:</b> every {@code Number} &mdash; the primitive wrappers, {@code Double},
     * {@code BigInteger}, {@code BigDecimal}, and any other subtype such as {@code AtomicInteger} or a custom
     * one &mdash; is converted with {@link Number#floatValue()} semantics: a magnitude that exceeds
     * {@code Float.MAX_VALUE} yields {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY}
     * without throwing, and a {@code Float} {@code NaN} stays {@code NaN} (a {@code BigInteger} or
     * {@code BigDecimal} is never {@code NaN}). Unlike the integer {@code to*} family, an unrecognized
     * {@code Number} subtype is <em>not</em> re-read from its string form here; its own {@code floatValue()}
     * is taken at face value. {@link #convert(Number, Class)} behaves the same way for a
     * {@code float}/{@code double} target (it only throws on overflow for integer target types).</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @return the float representation of the object, or {@code 0.0f} if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is longer
     *         than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units or cannot be parsed as a float.
     * @see #toFloat(String)
     * @see #toFloat(String, float)
     * @see #toFloat(Object, float)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final Object obj) throws NumberFormatException {
        return toFloat(obj, 0F);
    }

    /**
     * Converts the given string to a float value.
     *
     * <p>This method attempts to convert the provided string to a float. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a float.</p>
     *
     * <p><b>Grammar:</b> the full {@link Float#parseFloat(String)} grammar applies — leading and trailing
     * characters {@code <= U+0020} are trimmed (as by {@link String#trim()}), and hexadecimal floating-point
     * literals ({@code "0x1.8p1"}) and type suffixes ({@code "1.5f"}, {@code "2.5d"}) are accepted. The raw input,
     * including surrounding whitespace, must contain at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16
     * code units. Finite overflow yields signed infinity and underflow yields signed zero; neither is a parse
     * failure or a reason to use {@code defaultValue}. The integer {@code to*} parsers (for example
     * {@link #toInt(String, int)}) are stricter here: they reject whitespace and the {@code f}/{@code d} suffixes
     * (they accept a trailing {@code L}/{@code l} instead). Both families accept ASCII digits only.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toFloat("123.45", 0.0f);        // returns 123.45f
     * Numbers.toFloat("3.14159", 0.0f);       // returns 3.14159f
     * Numbers.toFloat("-42.5", 0.0f);         // returns -42.5f
     * Numbers.toFloat("1.23e10", 0.0f);       // returns 1.23E10f
     * Numbers.toFloat("NaN", 0.0f);           // returns Float.NaN
     * Numbers.toFloat("Infinity", 0.0f);      // returns Float.POSITIVE_INFINITY
     *
     * // Edge cases: the default covers only null and empty input
     * Numbers.toFloat((String) null, 1.0f);   // returns 1.0f
     * Numbers.toFloat("", 1.0f);              // returns 1.0f
     * Numbers.toFloat("abc", 0.0f);           // throws NumberFormatException
     * }</pre>
     *
     * <p><b>By design:</b> {@code defaultValue} applies only to {@code null} or empty input. A malformed string
     * still throws {@code NumberFormatException}. This is a missing-input fallback, not a parse-failure fallback.</p>
     *
     * @param str the string to convert. This can be any instance of String.
     * @param defaultValue the default value to return if the string is {@code null} or empty.
     * @return the float representation of the string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is longer than the documented limit or cannot be parsed as a float.
     * @see #toFloat(String)
     * @see #toFloat(Object)
     * @see #toFloat(Object, float)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final String str, final float defaultValue) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        return parseFloatWithinLengthLimit(str);
    }

    /**
     * Converts the given object to a float value.
     *
     * <p>This method attempts to convert the provided object to a float. If the object is {@code null},
     * the provided default value is returned. If the object is a {@code Number}, its float value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a float, so an empty or {@code null} {@code toString()} also returns the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toFloat((Object) Double.valueOf(123.45), 0.0f);         // returns 123.45f
     * Numbers.toFloat((Object) Float.valueOf(3.14f), 0.0f);           // returns 3.14f
     * Numbers.toFloat((Object) "98.76", 0.0f);                        // returns 98.76f
     *
     * // Edge cases: the default covers only null and an empty toString()
     * Numbers.toFloat((Object) null, 1.0f);                           // returns 1.0f
     * Numbers.toFloat((Object) "", 5.0f);                             // returns 5.0f
     * Numbers.toFloat((Object) Double.valueOf(Double.NaN), 0.0f);     // returns Float.NaN (the default is not used)
     * Numbers.toFloat((Object) "abc", 0.0f);                          // throws NumberFormatException
     * }</pre>
     *
     * <p><b>Note:</b> every {@code Number} &mdash; the primitive wrappers, {@code Double},
     * {@code BigInteger}, {@code BigDecimal}, and any other subtype such as {@code AtomicInteger} or a custom
     * one &mdash; is converted with {@link Number#floatValue()} semantics: a magnitude that exceeds
     * {@code Float.MAX_VALUE} yields {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY}
     * without throwing, and a {@code Float} {@code NaN} stays {@code NaN} (a {@code BigInteger} or
     * {@code BigDecimal} is never {@code NaN}). Unlike the integer {@code to*} family, an unrecognized
     * {@code Number} subtype is <em>not</em> re-read from its string form here; its own {@code floatValue()}
     * is taken at face value. {@link #convert(Number, Class)} behaves the same way for a
     * {@code float}/{@code double} target (it only throws on overflow for integer target types).</p>
     *
     * <p><b>By design:</b> a {@code Double} is narrowed by plain IEEE-754 rounding ({@code floatValue()}), not
     * through its decimal spelling. See {@link #toFloat(Object)}.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @param defaultValue the default value to return if the object is {@code null}, or if it is not a {@code Number} and {@code obj.toString()} is empty or {@code null}.
     * @return the float representation of the object, or the default value if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is longer
     *         than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units or cannot be parsed as a float.
     * @see #toFloat(String)
     * @see #toFloat(String, float)
     * @see #toFloat(Object)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final Object obj, final float defaultValue) throws NumberFormatException {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Number) {
            // Plain IEEE-754 narrowing for every Number, a Double included. The Double case used to go through
            // Float.parseFloat(obj.toString()) "so that 1.21d becomes 1.21f", but (float) 1.21d already is
            // 1.21f: the shortest decimal of a double narrows to the same float as the double itself unless
            // the double is an exact float midpoint, where the decimal route broke ties-to-even (measured:
            // 0 of 5M random doubles differed; 38% of exact midpoints did, always to the odd significand),
            // at roughly 100x the cost of the cast.
            return ((Number) obj).floatValue();
        }

        return toFloat(obj.toString(), defaultValue);
    }

    /**
     * Converts a {@code BigDecimal} to a {@code float}.
     *
     * <p>If the {@code BigDecimal} {@code value} is {@code null}, then the default value {@code 0.0f} is returned.
     * A finite magnitude outside the {@code float} range saturates to {@code ±Infinity} (the IEEE-754 semantics of
     * {@link BigDecimal#floatValue()}); it does not throw.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toFloat(BigDecimal.valueOf(8.5));         // returns 8.5f
     * Numbers.toFloat(new BigDecimal("-123.456"));      // returns -123.456f
     *
     * // Edge cases
     * Numbers.toFloat((BigDecimal) null);               // returns 0.0f
     * Numbers.toFloat(new BigDecimal("1e40"));          // returns Float.POSITIVE_INFINITY (saturates, never throws)
     * }</pre>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @return the float represented by the {@code BigDecimal} or {@code 0.0f} if the {@code BigDecimal} is {@code null}.
     * @see #toFloat(BigDecimal, float)
     * @see #toDouble(BigDecimal)
     */
    public static float toFloat(final BigDecimal value) {
        return toFloat(value, 0.0f);
    }

    /**
     * Converts a {@code BigDecimal} to a {@code float}.
     *
     * <p>If the {@code BigDecimal} {@code value} is {@code null}, then the specified default value is returned.
     * A finite magnitude outside the {@code float} range saturates to {@code ±Infinity} (the IEEE-754 semantics of
     * {@link BigDecimal#floatValue()}); it does not throw.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toFloat(BigDecimal.valueOf(8.5), 1.1f);       // returns 8.5f
     * Numbers.toFloat(new BigDecimal("-123.456"), 1.1f);    // returns -123.456f
     *
     * // Edge cases
     * Numbers.toFloat((BigDecimal) null, 1.1f);             // returns 1.1f
     * Numbers.toFloat(new BigDecimal("1e40"), 1.1f);        // returns Float.POSITIVE_INFINITY (saturates, never throws)
     * }</pre>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @param defaultValue the default value to return if the {@code BigDecimal} is {@code null}.
     * @return the float represented by the {@code BigDecimal} or the default value if the {@code BigDecimal} is {@code null}.
     * @see #toFloat(BigDecimal)
     * @see #toDouble(BigDecimal, double)
     */
    public static float toFloat(final BigDecimal value, final float defaultValue) {
        return value == null ? defaultValue : value.floatValue();
    }

    /**
     * Converts the given string to a double value.
     *
     * <p>This method attempts to convert the provided string to a double. If the string is {@code null} or empty,
     * default value {@code 0.0} is returned. Otherwise, the method attempts to parse the string as a double.</p>
     *
     * <p><b>Grammar:</b> the full {@link Double#parseDouble(String)} grammar applies — leading and trailing
     * characters {@code <= U+0020} are trimmed (as by {@link String#trim()}), and hexadecimal floating-point
     * literals ({@code "0x1.8p1"}) and type suffixes ({@code "1.5f"}, {@code "2.5d"}) are accepted. The raw input,
     * including surrounding whitespace, must contain at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16
     * code units. Finite overflow yields signed infinity and underflow yields signed zero; neither is a parse
     * failure. The integer {@code to*} parsers (for example {@link #toLong(String)}) are stricter here: they
     * reject whitespace and the {@code f}/{@code d} suffixes (they accept a trailing {@code L}/{@code l} instead).
     * Both families accept ASCII digits only.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toDouble("123.45");              // returns 123.45
     * Numbers.toDouble("3.141592653589793");   // returns 3.141592653589793
     * Numbers.toDouble("-42.5");               // returns -42.5
     * Numbers.toDouble("1.23e100");            // returns 1.23E100
     * Numbers.toDouble("NaN");                 // returns NaN
     * Numbers.toDouble("Infinity");            // returns Double.POSITIVE_INFINITY
     *
     * // Edge cases
     * Numbers.toDouble((String) null);         // returns 0.0
     * Numbers.toDouble("");                    // returns 0.0
     * Numbers.toDouble("1e400");               // returns Double.POSITIVE_INFINITY (overflow saturates, never throws)
     * Numbers.toDouble("abc");                 // throws NumberFormatException
     * }</pre>
     *
     * @param str the string to convert. This can be any instance of String.
     * @return the double representation of the string, or {@code 0.0} if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is longer than the documented limit or cannot be parsed as a double.
     * @see #toDouble(Object)
     * @see #toDouble(String, double)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final String str) throws NumberFormatException {
        return toDouble(str, 0.0d);
    }

    /**
     * Converts the given object to a double value.
     *
     * <p>This method attempts to convert the provided object to a double. If the object is {@code null},
     * default value {@code 0.0} is returned. If the object is a {@code Number}, its double value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a double, so an empty or {@code null} {@code toString()} also returns {@code 0.0}.</p>
     *
     * <p><b>By design:</b> converting from {@code Float} to {@code Double} parses the float's canonical
     * {@link Float#toString(float)} with {@link Double#parseDouble(String)}, rather than IEEE-754 widening
     * via {@code doubleValue()}. This prefers the decimal spelling of the source over the binary widening of
     * the float's bits. That round trip costs roughly two orders of magnitude more than the widening it
     * replaces; call {@code doubleValue()} directly on a hot path where the widened binary value is what
     * you want.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toDouble((Object) Integer.valueOf(123));            // returns 123.0
     * Numbers.toDouble((Object) Double.valueOf(3.141592));        // returns 3.141592
     * Numbers.toDouble((Object) "456.789");                       // returns 456.789
     *
     * // Edge cases
     * Numbers.toDouble((Object) null);                            // returns 0.0
     * Numbers.toDouble((Object) "");                              // returns 0.0
     * Numbers.toDouble((Object) Double.valueOf(Double.NaN));      // returns NaN (never throws on a Number)
     * Numbers.toDouble((Object) "abc");                           // throws NumberFormatException
     * }</pre>
     *
     * <p><b>Note:</b> every {@code Number} except {@code Float} &mdash; the primitive wrappers,
     * {@code BigInteger}, {@code BigDecimal}, and any other subtype such as {@code AtomicInteger} or a custom
     * one &mdash; is converted with {@link Number#doubleValue()} semantics: a magnitude that exceeds
     * {@code Double.MAX_VALUE} yields {@code Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY}
     * without throwing, and a {@code Double} {@code NaN} stays {@code NaN} (a {@code BigInteger} or
     * {@code BigDecimal} is never {@code NaN}). Unlike the integer {@code to*} family, an unrecognized
     * {@code Number} subtype is <em>not</em> re-read from its string form here; its own {@code doubleValue()}
     * is taken at face value. {@link #convert(Number, Class)} behaves the same way for a
     * {@code float}/{@code double} target (it only throws on overflow for integer target types).</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @return the double representation of the object, or {@code 0.0} if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is longer
     *         than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units or cannot be parsed as a double.
     * @see #toDouble(String)
     * @see #toDouble(String, double)
     * @see #toDouble(Object, double)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final Object obj) throws NumberFormatException {
        return toDouble(obj, 0D);
    }

    /**
     * Converts the given string to a double value.
     *
     * <p>This method attempts to convert the provided string to a double. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a double.</p>
     *
     * <p><b>Grammar:</b> the full {@link Double#parseDouble(String)} grammar applies — leading and trailing
     * characters {@code <= U+0020} are trimmed (as by {@link String#trim()}), and hexadecimal floating-point
     * literals ({@code "0x1.8p1"}) and type suffixes ({@code "1.5f"}, {@code "2.5d"}) are accepted. The raw input,
     * including surrounding whitespace, must contain at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16
     * code units. Finite overflow yields signed infinity and underflow yields signed zero; neither is a parse
     * failure or a reason to use {@code defaultValue}. The integer {@code to*} parsers (for example
     * {@link #toLong(String, long)}) are stricter here: they reject whitespace and the {@code f}/{@code d} suffixes
     * (they accept a trailing {@code L}/{@code l} instead). Both families accept ASCII digits only.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toDouble("123.45", 0.0);              // returns 123.45
     * Numbers.toDouble("3.141592653589793", 0.0);   // returns 3.141592653589793
     * Numbers.toDouble("-42.5", 0.0);               // returns -42.5
     * Numbers.toDouble("1.23e100", 0.0);            // returns 1.23E100
     * Numbers.toDouble("NaN", 0.0);                 // returns NaN
     *
     * // Edge cases: the default covers only null and empty input
     * Numbers.toDouble((String) null, 1.0);         // returns 1.0
     * Numbers.toDouble("", 1.0);                    // returns 1.0
     * Numbers.toDouble("abc", 0.0);                 // throws NumberFormatException
     * }</pre>
     *
     * <p><b>By design:</b> {@code defaultValue} applies only to {@code null} or empty input. A malformed string
     * still throws {@code NumberFormatException}. This is a missing-input fallback, not a parse-failure fallback.</p>
     *
     * @param str the string to convert. This can be any instance of String.
     * @param defaultValue the default value to return if the string is {@code null} or empty.
     * @return the double representation of the string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException if the string is longer than the documented limit or cannot be parsed as a double.
     * @see #toDouble(String)
     * @see #toDouble(Object)
     * @see #toDouble(Object, double)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final String str, final double defaultValue) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        return parseDoubleWithinLengthLimit(str);
    }

    /**
     * Converts the given object to a double value.
     *
     * <p>This method attempts to convert the provided object to a double. If the object is {@code null},
     * the provided default value is returned. If the object is a {@code Number}, its double value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a double, so an empty or {@code null} {@code toString()} also returns the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toDouble((Object) Integer.valueOf(123), 0.0);            // returns 123.0
     * Numbers.toDouble((Object) Double.valueOf(3.141592), 0.0);        // returns 3.141592
     * Numbers.toDouble((Object) "456.789", 0.0);                       // returns 456.789
     *
     * // Edge cases: the default covers only null and an empty toString()
     * Numbers.toDouble((Object) null, 1.0);                            // returns 1.0
     * Numbers.toDouble((Object) "", 5.0);                              // returns 5.0
     * Numbers.toDouble((Object) Double.valueOf(Double.NaN), 0.0);      // returns NaN (the default is not used)
     * Numbers.toDouble((Object) "abc", 0.0);                           // throws NumberFormatException
     * }</pre>
     *
     * <p><b>Note:</b> every {@code Number} except {@code Float} &mdash; the primitive wrappers,
     * {@code BigInteger}, {@code BigDecimal}, and any other subtype such as {@code AtomicInteger} or a custom
     * one &mdash; is converted with {@link Number#doubleValue()} semantics: a magnitude that exceeds
     * {@code Double.MAX_VALUE} yields {@code Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY}
     * without throwing, and a {@code Double} {@code NaN} stays {@code NaN} (a {@code BigInteger} or
     * {@code BigDecimal} is never {@code NaN}). Unlike the integer {@code to*} family, an unrecognized
     * {@code Number} subtype is <em>not</em> re-read from its string form here; its own {@code doubleValue()}
     * is taken at face value. {@link #convert(Number, Class)} behaves the same way for a
     * {@code float}/{@code double} target (it only throws on overflow for integer target types).</p>
     *
     * <p><b>By design:</b> a {@code Float} is converted via {@link Double#parseDouble(String)} on
     * {@code toString()}, not {@code doubleValue()}. See {@link #toDouble(Object)}.</p>
     *
     * <p><b>Performance:</b> that decimal round trip costs roughly two orders of magnitude more than the
     * IEEE-754 widening it replaces, so a {@code Float} source is far more expensive here than any other
     * {@code Number}. It is the price of the documented decimal-spelling semantics. On a hot path where the
     * widened binary value is what you actually want, call {@code doubleValue()} instead.</p>
     *
     * @param obj the object to convert. This can be any instance of Object.
     * @param defaultValue the default value to return if the object is {@code null}, or if it is not a {@code Number} and {@code obj.toString()} is empty or {@code null}.
     * @return the double representation of the object, or the default value if the object is {@code null} or a non-{@code Number} whose {@code toString()} is empty or {@code null}.
     * @throws NumberFormatException if the object is not a {@code Number} and its string representation is longer
     *         than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units or cannot be parsed as a double.
     * @see #toDouble(String)
     * @see #toDouble(String, double)
     * @see #toDouble(Object)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final Object obj, final double defaultValue) throws NumberFormatException {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Number) {
            if (obj instanceof Float) {
                return floatToDoubleViaDecimal((Float) obj);
            } else {
                return ((Number) obj).doubleValue();
            }
        }

        return toDouble(obj.toString(), defaultValue);
    }

    /**
     * The documented {@code Float} to {@code double} widening of {@link #toDouble(Object)} and of the
     * {@code convert} table: the float's canonical decimal spelling re-read as a {@code double}, so
     * {@code 1.21f} becomes {@code 1.21} rather than the widened binary {@code 1.2100000381469727}.
     * {@link Float#toString(float)} renders {@code NaN}, {@code Infinity}, {@code -Infinity} and {@code -0.0}
     * as the exact tokens {@link Double#parseDouble(String)} reads back, so no special case is needed for them.
     * Stated once so the two call sites cannot drift.
     *
     * @param f the value to widen
     * @return {@code f} widened through its decimal spelling
     */
    private static double floatToDoubleViaDecimal(final float f) {
        return Double.parseDouble(Float.toString(f));
    }

    /**
     * Converts a {@code BigDecimal} to a {@code double}.
     *
     * <p>If the {@code BigDecimal} {@code value} is {@code null}, then the default value {@code 0.0} is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toDouble(BigDecimal.valueOf(8.5));        // returns 8.5
     * Numbers.toDouble(new BigDecimal("-123.456"));     // returns -123.456
     *
     * // Edge cases
     * Numbers.toDouble((BigDecimal) null);              // returns 0.0
     * Numbers.toDouble(new BigDecimal("1e400"));        // returns Double.POSITIVE_INFINITY (saturates, never throws)
     * }</pre>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @return the double represented by the {@code BigDecimal} or {@code 0.0} if the {@code BigDecimal} is {@code null}.
     * @see #toDouble(BigDecimal, double)
     * @see #toFloat(BigDecimal)
     */
    public static double toDouble(final BigDecimal value) {
        return toDouble(value, 0.0d);
    }

    /**
     * Converts a {@code BigDecimal} to a {@code double}.
     *
     * <p>If the {@code BigDecimal} {@code value} is {@code null}, then the specified default value is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.toDouble(BigDecimal.valueOf(8.5), 1.1);       // returns 8.5
     * Numbers.toDouble(new BigDecimal("-123.456"), 1.1);    // returns -123.456
     *
     * // Edge cases
     * Numbers.toDouble((BigDecimal) null, 1.1);             // returns 1.1
     * Numbers.toDouble(new BigDecimal("1e400"), 1.1);       // returns Double.POSITIVE_INFINITY (saturates, never throws)
     * }</pre>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @param defaultValue the default value to return if the {@code BigDecimal} is {@code null}.
     * @return the double represented by the {@code BigDecimal} or the default value if the {@code BigDecimal} is {@code null}.
     * @see #toDouble(BigDecimal)
     * @see #toFloat(BigDecimal, float)
     */
    public static double toDouble(final BigDecimal value, final double defaultValue) {
        return value == null ? defaultValue : value.doubleValue();
    }

    /**
     * Returns the value of the {@code long} argument as an {@code int}, throwing an exception if the value overflows an {@code int}.
     *
     * <p>This method provides overflow checking when converting a long to an int. If the long value is outside
     * the range of int values (Integer.MIN_VALUE to Integer.MAX_VALUE), an ArithmeticException is thrown.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int result = Numbers.toIntExact(123L);                       // returns 123
     * int negative = Numbers.toIntExact(-2147483648L);             // returns -2147483648
     *
     * // Edge cases
     * Numbers.toIntExact(Integer.MAX_VALUE + 1L);                  // throws ArithmeticException
     * Numbers.toIntExact(Long.MIN_VALUE);                          // throws ArithmeticException
     * }</pre>
     *
     * @param value the long value to convert to an int
     * @return the int value represented by the long argument
     * @throws ArithmeticException if the {@code value} overflows an int (outside range of Integer.MIN_VALUE to Integer.MAX_VALUE)
     * @see Math#toIntExact(long)
     * @see #saturatedCastToInt(long)
     */
    public static int toIntExact(final long value) throws ArithmeticException {
        return Math.toIntExact(value);
    }

    /**
     * Converts a {@code String} to an {@code Integer}, handling hexadecimal (0x or 0X prefix) and octal (0 prefix) notations.
     *
     * <p>This method follows the radix grammar of {@link Integer#decode(String)}, which supports:</p>
     * <ul>
     * <li>Decimal numbers: "123", "-456"</li>
     * <li>Hexadecimal numbers: "0xFF", "0x10", "#FF"</li>
     * <li>Octal numbers: "0777" (leading zero indicates octal)</li>
     * </ul>
     *
     * <p>Note: Leading zeros indicate octal notation ({@code "010"} is 8). This is <b>not</b>
     * {@link #toInt(String)}, which is decimal-first ({@code toInt("010")} is 10). A trailing {@code L}/{@code l}
     * is not accepted ({@code "0xFFL"} throws); use {@link #decodeLong(String)}. Spaces are not trimmed
     * from the input string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer decimal = Numbers.decodeInteger("123");   // returns 123
     * Integer hex = Numbers.decodeInteger("0xFF");      // returns 255
     * Integer octal = Numbers.decodeInteger("010");     // returns 8 (octal; toInt("010") is 10)
     *
     * // Edge cases
     * Integer nullValue = Numbers.decodeInteger(null);  // returns null
     * Integer emptyValue = Numbers.decodeInteger("");   // returns null
     * Numbers.decodeInteger(" ");                       // throws NumberFormatException
     * Numbers.decodeInteger("\n");                      // throws NumberFormatException
     * Numbers.decodeInteger("abc");                     // throws NumberFormatException
     * Numbers.decodeInteger("١٢٣");                     // throws NumberFormatException (non-ASCII digits)
     * }</pre>
     *
     * <p>Non-ASCII Unicode digits are rejected even though {@link Integer#decode(String)} would accept them
     * via {@code Character.digit}. This matches {@link #decodeLong(String)}, {@link #decodeBigInteger(String)},
     * {@link #toInt(String)}, and {@link #createNumber(String)}.</p>
     *
     * <p>Valid input first uses an allocation-free primitive scanner (apart from the required returned
     * {@code Integer}). Invalid or out-of-range input is rescanned only to retain detailed diagnostics. No
     * input-sized substring or JDK parse exception is created, and retained failure messages contain only a
     * bounded preview of the input.</p>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the Integer value represented by the string, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string cannot be parsed as a valid integer
     * @see #isCreatable(String)
     * @see #toInt(String)
     * @see Integer#decode(String)
     */
    @MayReturnNull
    public static Integer decodeInteger(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final long value = scanIntegerTokenValueOrInvalid(str, 0, str.length(), IntegerTokenSyntax.DECODE, Integer.MIN_VALUE, Integer.MAX_VALUE);

        if (value != INVALID_INTEGER_TOKEN) {
            return (int) value;
        }

        final IntegerTokenScan scan = scanIntegerTokenQuiet(str, str.length(), IntegerTokenSyntax.DECODE);

        // Invariant (see decodeLong(String) for the full argument): reaching here means the range-limited scan
        // rejected the token, so the quiet scan must report malformed input or a value outside the int range.
        // Checked unconditionally rather than with assert so a broken invariant cannot be reported as a
        // misleading "out of range".
        if (scan.status == SCAN_VALID && scan.value >= Integer.MIN_VALUE && scan.value <= Integer.MAX_VALUE) {
            throw new AssertionError("scanners disagree on " + previewForErrorMessage(str));
        }

        throw decodeIntegralFailure(str, "Integer", scan);
    }

    /**
     * Converts a {@code String} to a {@code Long}, handling hexadecimal (0x or 0X prefix) and octal (0 prefix) notations.
     *
     * <p>This method follows the radix grammar of {@link Long#decode(String)}, which supports:</p>
     * <ul>
     * <li>Decimal numbers: "123", "-456"</li>
     * <li>Hexadecimal numbers: "0xFF", "0x10", "#FF"</li>
     * <li>Octal numbers: "0777" (leading zero indicates octal)</li>
     * </ul>
     *
     * <p>The method also handles strings ending with 'l' or 'L' suffix, which is excluded by index before parsing.
     * That suffix rule applies to hexadecimal as well: {@code "0xFFL"} and {@code "#FFL"} are {@code 255L}.
     * {@link #createNumber(String)} does <b>not</b> accept that combination (the hex prefix is handled before
     * suffix stripping) and throws {@code NumberFormatException}.</p>
     *
     * <p>Note: Leading zeros indicate octal notation ({@code "010"} is 8). This is <b>not</b>
     * {@link #toLong(String)}, which is decimal-first ({@code toLong("010")} is 10, {@code toLong("0123L")} is 123).
     * Spaces are not trimmed from the input string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Long decimal = Numbers.decodeLong("123");       // returns 123L
     * Long hex = Numbers.decodeLong("0xFF");          // returns 255L
     * Long hexL = Numbers.decodeLong("0xFFL");        // returns 255L (createNumber("0xFFL") throws)
     * Long octal = Numbers.decodeLong("010");         // returns 8L (octal; toLong("010") is 10)
     * Long withSuffix = Numbers.decodeLong("123L");   // returns 123L
     *
     * // Edge cases
     * Long nullValue = Numbers.decodeLong(null);      // returns null
     * Long emptyValue = Numbers.decodeLong("");       // returns null
     * Numbers.decodeLong(" ");                        // throws NumberFormatException
     * Numbers.decodeLong("\n");                       // throws NumberFormatException
     * Numbers.decodeLong("abc");                      // throws NumberFormatException
     * Numbers.decodeLong("١٢٣");                      // throws NumberFormatException (non-ASCII digits)
     * }</pre>
     *
     * <p>Non-ASCII Unicode digits are rejected even though {@link Long#decode(String)} would accept them
     * via {@code Character.digit}. This matches {@link #decodeInteger(String)}, {@link #decodeBigInteger(String)},
     * {@link #toLong(String)}, and {@link #createNumber(String)}.</p>
     *
     * <p>Valid input first uses an allocation-free primitive scanner (apart from the required returned
     * {@code Long}). Invalid or out-of-range input is rescanned only to retain detailed diagnostics. No input-sized
     * suffix substring or JDK parse exception is created, and retained failure messages contain only a bounded
     * preview of the input.</p>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the Long value represented by the string, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string cannot be parsed as a valid long
     * @see #isCreatable(String)
     * @see #toLong(String)
     * @see Long#decode(String)
     */
    @MayReturnNull
    public static Long decodeLong(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final int end = integerTokenEnd(str);
        final long value = scanIntegerTokenValueOrInvalid(str, 0, end, IntegerTokenSyntax.DECODE, Long.MIN_VALUE, Long.MAX_VALUE);

        if (value != INVALID_INTEGER_TOKEN || isLongMinValueToken(str, 0, end, IntegerTokenSyntax.DECODE)) {
            return value;
        }

        final IntegerTokenScan scan = scanIntegerTokenQuiet(str, end, IntegerTokenSyntax.DECODE);

        // Invariant: the two scanners accept exactly the same tokens over the full long range, and their one
        // ambiguous result -- Long.MIN_VALUE, which collides with INVALID_INTEGER_TOKEN -- was resolved above.
        // So reaching here means the primitive scan rejected the token, and the quiet scan must agree.
        // Checked unconditionally rather than with assert, which is disabled unless -ea is passed: if the
        // invariant ever broke, decodeIntegralFailure would silently report a misleading "out of range".
        if (scan.status == SCAN_VALID) {
            throw new AssertionError("scanners disagree on " + previewForErrorMessage(str));
        }

        throw decodeIntegralFailure(str, "Long", scan);
    }

    // -----------------------------------------------------------------------

    /**
     * Converts a {@code String} to a {@code BigInteger}.
     *
     * <p>This method supports multiple number formats:</p>
     * <ul>
     * <li>Decimal numbers: "123", "-456"</li>
     * <li>Hexadecimal numbers: "0xFF", "0x10", "#FF"</li>
     * <li>Octal numbers: "0777" (leading zero indicates octal)</li>
     * </ul>
     *
     * <p>The method automatically detects the radix (base) based on the prefix and parses accordingly.
     * For hexadecimal, both "0x"/"0X" and "#" prefixes are supported. For octal, a leading zero is required.</p>
     *
     * <p>Unlike {@link #decodeLong(String)}, this method does not accept or strip a trailing {@code L}/{@code l}
     * suffix; for example, {@code decodeBigInteger("123L")} throws {@code NumberFormatException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger decimal = Numbers.decodeBigInteger("123456789012345");   // returns 123456789012345
     * BigInteger hex = Numbers.decodeBigInteger("0xFFFFFFFF");            // returns 4294967295
     * BigInteger octal = Numbers.decodeBigInteger("0777");                // returns 511
     * BigInteger negative = Numbers.decodeBigInteger("-0xFF");            // returns -255
     *
     * // Edge cases
     * BigInteger nullValue = Numbers.decodeBigInteger(null);              // returns null
     * BigInteger emptyValue = Numbers.decodeBigInteger("");               // returns null
     * Numbers.decodeBigInteger(" ");                                      // throws NumberFormatException
     * Numbers.decodeBigInteger("\n");                                     // throws NumberFormatException
     * Numbers.decodeBigInteger("abc");                                    // throws NumberFormatException
     * Numbers.decodeBigInteger("١٢٣");                                    // throws NumberFormatException (non-ASCII digits)
     * }</pre>
     *
     * <p>Non-ASCII Unicode digits are rejected even though {@link BigInteger#BigInteger(String, int)} would accept
     * them via {@code Character.digit}. This matches {@link #decodeInteger(String)}, {@link #decodeLong(String)},
     * and {@link #createNumber(String)}.</p>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the BigInteger value represented by the string, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string is not a valid BigInteger representation
     * @throws ArithmeticException if the value exceeds the JDK implementation's supported {@code BigInteger} magnitude
     * @see #isCreatable(String)
     * @see BigInteger#BigInteger(String, int)
     * @see Long#decode(String)
     */
    @MayReturnNull
    public static BigInteger decodeBigInteger(final String str) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        if (!quickCheckForNumericParsing(str)) {
            throw notAValidNumber(str, "BigInteger", null);
        }

        int pos = 0; // offset within string
        int radix = 10;
        boolean negate = false; // need to negate later?
        final char char0 = str.charAt(0);
        if (char0 == '-') {
            negate = true;
            pos = 1;
        } else if (char0 == '+') {
            pos = 1;
        }
        if (str.startsWith("0x", pos) || str.startsWith("0X", pos)) { // hex
            radix = 16;
            pos += 2;
        } else if (str.startsWith("#", pos)) { // alternative hex (allowed by Long/Integer)
            radix = 16;
            pos++;
        } else if (str.startsWith("0", pos) && str.length() > pos + 1) { // octal; so long as there are additional digits
            radix = 8;
            pos++;
        } // default is to treat as decimal

        // A sign was already consumed above; BigInteger would accept a second one ("--1" -> 1).
        if (pos > 0 && pos < str.length() && (str.charAt(pos) == '-' || str.charAt(pos) == '+')) {
            throw notAValidNumber(str, "BigInteger", null);
        }

        try {
            if (radix == 16) {
                // BigInteger(String, int) understands no 0x/# prefix; strip sign and prefix and negate afterwards.
                final BigInteger value = new BigInteger(str.substring(pos), radix);
                return negate ? value.negate() : value;
            }

            // Decimal and octal are parsed in place: BigInteger(String, int) already accepts a leading
            // sign and the leading zero of octal input, so no magnitude copy is needed.
            return new BigInteger(str, radix);
        } catch (final NumberFormatException e) {
            throw notAValidNumber(str, "BigInteger", e);
        }
    }

    /**
     * Attempts to parse a {@code String} as an {@code int} without throwing {@link NumberFormatException}
     * or {@link ArithmeticException}.
     *
     * <p>For non-empty input this method uses the same decimal-first grammar, trailing {@code L}/{@code l}
     * suffix, hexadecimal prefix, and range check as {@link #toInt(String)}. {@code "010"} is 10 (not octal);
     * {@code "0x10"} is 16. A value outside the {@code int} range, {@code null}, empty, or malformed input
     * returns {@link u.OptionalInt#empty()} instead of throwing.</p>
     *
     * <p>The result is produced by one parse; calling {@link #toInt(String)} first is unnecessary.
     * For every input {@code str}, {@code tryParseInt(str).isPresent()} if and only if {@code str} is
     * non-null and non-empty and {@link #toInt(String)} would return without throwing.
     * Invalid or out-of-range input is detected by a primitive-only scan, without constructing an exception or result holder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.tryParseInt("123").getAsInt();         // returns 123
     * Numbers.tryParseInt("010").getAsInt();         // returns 10 (decimal, not octal)
     * Numbers.tryParseInt("0x10").getAsInt();        // returns 16
     *
     * // Edge cases: every failure yields an empty optional instead of throwing
     * Numbers.tryParseInt("2147483648").isEmpty();   // returns true (overflow)
     * Numbers.tryParseInt("abc").isEmpty();          // returns true
     * Numbers.tryParseInt("").isEmpty();             // returns true
     * Numbers.tryParseInt(" ").isEmpty();            // returns true
     * Numbers.tryParseInt("\n").isEmpty();           // returns true
     * Numbers.tryParseInt(null).isEmpty();           // returns true
     * }</pre>
     *
     * @param str the string to parse; may be {@code null}
     * @return an optional containing the parsed int, or an empty optional if {@code str} is null, empty, malformed, or out of range
     * @see #toInt(String)
     * @see #tryParseLong(String)
     * @see #tryParseFloat(String)
     * @see #tryParseDouble(String)
     */
    public static u.OptionalInt tryParseInt(final String str) {
        if (Strings.isEmpty(str)) {
            return u.OptionalInt.empty();
        }

        if (str.length() < 5) {
            final Integer cached = N.stringIntCache.get(str);

            if (cached != null) {
                return u.OptionalInt.of(cached);
            }
        }

        final long value = scanIntegerTokenValueOrInvalid(str, 0, integerTokenEnd(str), IntegerTokenSyntax.DECIMAL_FIRST, Integer.MIN_VALUE, Integer.MAX_VALUE);

        return value == INVALID_INTEGER_TOKEN ? u.OptionalInt.empty() : u.OptionalInt.of((int) value);
    }

    /**
     * Attempts to parse a {@code String} as a {@code long} without throwing {@link NumberFormatException}
     * or {@link ArithmeticException}.
     *
     * <p>For non-empty input this method uses the same decimal-first grammar, trailing {@code L}/{@code l}
     * suffix, hexadecimal prefix, and range check as {@link #toLong(String)}. {@code "010"} is 10 (not octal);
     * {@code "0xFFL"} is 255. A value outside the {@code long} range, {@code null}, empty, or malformed input
     * returns {@link u.OptionalLong#empty()} instead of throwing.</p>
     *
     * <p>The result is produced by one parse; calling {@link #toLong(String)} first is unnecessary.
     * For every input {@code str}, {@code tryParseLong(str).isPresent()} if and only if {@code str} is
     * non-null and non-empty and {@link #toLong(String)} would return without throwing.
     * Invalid or out-of-range input is detected by a primitive-only scan, without constructing an exception or result holder.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.tryParseLong("123").getAsLong();                // returns 123L
     * Numbers.tryParseLong("010").getAsLong();                // returns 10L (decimal, not octal)
     * Numbers.tryParseLong("0xFFL").getAsLong();              // returns 255L
     *
     * // Edge cases: every failure yields an empty optional instead of throwing
     * Numbers.tryParseLong("9223372036854775808").isEmpty();  // returns true (overflow)
     * Numbers.tryParseLong("abc").isEmpty();                  // returns true
     * Numbers.tryParseLong("").isEmpty();                     // returns true
     * Numbers.tryParseLong(" ").isEmpty();                    // returns true
     * Numbers.tryParseLong("\n").isEmpty();                   // returns true
     * Numbers.tryParseLong(null).isEmpty();                   // returns true
     * }</pre>
     *
     * @param str the string to parse; may be {@code null}
     * @return an optional containing the parsed long, or an empty optional if {@code str} is null, empty, malformed, or out of range
     * @see #toLong(String)
     * @see #tryParseInt(String)
     * @see #tryParseFloat(String)
     * @see #tryParseDouble(String)
     */
    public static u.OptionalLong tryParseLong(final String str) {
        if (Strings.isEmpty(str)) {
            return u.OptionalLong.empty();
        }

        if (str.length() < 5) {
            final Integer cached = N.stringIntCache.get(str);

            if (cached != null) {
                return u.OptionalLong.of(cached);
            }
        }

        final int end = integerTokenEnd(str);
        final long value = scanIntegerTokenValueOrInvalid(str, 0, end, IntegerTokenSyntax.DECIMAL_FIRST, Long.MIN_VALUE, Long.MAX_VALUE);

        return value != INVALID_INTEGER_TOKEN || isLongMinValueToken(str, 0, end, IntegerTokenSyntax.DECIMAL_FIRST) ? u.OptionalLong.of(value)
                : u.OptionalLong.empty();
    }

    /**
     * Returns {@code true} if and only if {@code str} contains at most
     * {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units and {@link Double#parseDouble(String)}
     * succeeds after a lightweight ASCII/whitespace pre-filter.
     *
     * <p>{@code parseFloat} and {@code parseDouble} share that JDK floating-point grammar.
     * {@code null} and empty strings return {@code false} ({@code parseFloat}/{@code parseDouble} return
     * {@code null} for {@code null} rather than throwing; that is not treated as a successful parse here).</p>
     *
     * <p>The JDK grammar accepts scientific notation, a trailing decimal point, {@code f}/{@code F}/{@code d}/{@code D}
     * suffixes, {@code NaN}, infinity, hex floats ({@code 0x1.0p2}), and surrounding whitespace. Integer hex
     * ({@code "0xFF"}), a trailing {@code L}/{@code l}, and non-ASCII digits are rejected.
     * The length limit applies to the raw input, so surrounding whitespace counts toward it.
     * Use {@link #isCreatable(String)} as an allocation-free grammar and scale pre-check for
     * {@link #createNumber(String)}.</p>
     *
     * <p><b>Not a guard for {@link #parseBigDecimal(String)}.</b> This predicate answers only for
     * {@code parseFloat}/{@code parseDouble}; {@code parseBigDecimal} has a different grammar and no length
     * limit, so the two disagree in both directions. {@code isParsable("0x1.0p2")} is {@code true} while
     * {@code parseBigDecimal("0x1.0p2")} throws, and {@code isParsable} is {@code false} for an input longer
     * than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} code units that {@code parseBigDecimal} parses without
     * complaint. Guard {@code parseBigDecimal} by calling it, or by {@link #isCreatable(String)} for the
     * {@code createNumber} grammar.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.isParsable("123");       // returns true
     * Numbers.isParsable("-123");      // returns true
     * Numbers.isParsable("123.45");    // returns true
     * Numbers.isParsable(".5");        // returns true
     * Numbers.isParsable("123.");      // returns true   (trailing decimal point)
     * Numbers.isParsable("1.23e10");   // returns true   (scientific notation)
     * Numbers.isParsable("1.5f");      // returns true   (float suffix)
     * Numbers.isParsable("NaN");       // returns true
     * Numbers.isParsable("Infinity");  // returns true
     * Numbers.isParsable("0x1.0p2");   // returns true   (JDK hexadecimal floating-point form)
     * Numbers.isParsable(" 123 ");     // returns true   (surrounding JDK-supported whitespace)
     *
     * // Rejected forms
     * Numbers.isParsable("0xFF");      // returns false  (integer hex: Double.parseDouble rejects it)
     * Numbers.isParsable("123L");      // returns false  (long suffix)
     * Numbers.isParsable("abc");       // returns false
     * Numbers.isParsable("");          // returns false
     * Numbers.isParsable(null);        // returns false
     * }</pre>
     *
     * <p><b>Comparison of {@code isCreatable}, {@code isParsable} and {@link Strings#isNumeric(CharSequence)}:</b>
     * the predicates accept different forms of numeric string:</p>
     * <ul>
     *   <li>{@link Strings#isNumeric(CharSequence)} &mdash; <i>digit characters only</i>: every character must be a
     *       Unicode digit; no sign, decimal point, exponent, {@code 0x}/{@code #} prefix or type suffix
     *       (non-ASCII digits ARE accepted).</li>
     *   <li>{@code isParsable} &mdash; {@code true} iff the raw input is within the documented length limit and
     *       {@link Double#parseDouble(String)} succeeds after the ASCII/whitespace pre-filter. It accepts scientific notation, a trailing decimal point,
     *       {@code f}/{@code F}/{@code d}/{@code D} suffixes, {@code NaN}, infinity, hex floats, and surrounding
     *       whitespace, and rejects integer hex, a trailing {@code L}/{@code l}, and non-ASCII digits.</li>
     *   <li>{@code isCreatable} &mdash; validates the grammar and required decimal scale used by
     *       {@link #createNumber(String)} without constructing the number:
     *       hexadecimal ({@code 0x}/{@code #}), octal, scientific notation, a trailing decimal point, and
     *       type suffixes ({@code l/L}, {@code f/F}, {@code d/D}).
     *       It is <i>not</i> a strict superset of {@code isParsable}:
     *       {@code "08"}/{@code "09"} and {@code "NaN"} are accepted by {@code isParsable} but rejected by
     *       {@code isCreatable}; {@code "0xFF"} and {@code "123L"} are accepted by {@code isCreatable} but
     *       rejected by {@code isParsable}.</li>
     * </ul>
     *
     * <table border="1">
     *   <caption>{@code isCreatable} vs {@code isParsable} vs {@code Strings.isNumeric}</caption>
     *   <tr><th>Input</th><th>{@code isCreatable}</th><th>{@code isParsable}</th><th>{@code Strings.isNumeric}</th><th>Reason</th></tr>
     *   <tr><td>{@code "123"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code true}</td><td>plain non-negative integer</td></tr>
     *   <tr><td>{@code "-123"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>leading sign is not a digit</td></tr>
     *   <tr><td>{@code "123.45"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>decimal point</td></tr>
     *   <tr><td>{@code ".5"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>leading decimal point (accepted)</td></tr>
     *   <tr><td>{@code "123."}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>trailing decimal point</td></tr>
     *   <tr><td>{@code "0xFF"}</td><td>{@code true}</td><td>{@code false}</td><td>{@code false}</td><td>integer hex ({@code Double.parseDouble} rejects it)</td></tr>
     *   <tr><td>{@code "1.5e3"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>scientific notation</td></tr>
     *   <tr><td>{@code "123L"}</td><td>{@code true}</td><td>{@code false}</td><td>{@code false}</td><td>long suffix</td></tr>
     *   <tr><td>{@code "1.5f"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>float suffix</td></tr>
     *   <tr><td>{@code "NaN"}</td><td>{@code false}</td><td>{@code true}</td><td>{@code false}</td><td>JDK named float; {@code createNumber} rejects it</td></tr>
     *   <tr><td>{@code "08"}</td><td>{@code false}</td><td>{@code true}</td><td>{@code true}</td><td>leading-zero decimal vs octal</td></tr>
     *   <tr><td>Arabic-Indic {@code U+0661 U+0662 U+0663}</td><td>{@code false}</td><td>{@code false}</td><td>{@code true}</td><td>non-ASCII Unicode digits</td></tr>
     *   <tr><td>{@code "1٢11"}</td><td>{@code false}</td><td>{@code false}</td><td>{@code true}</td><td>mixed ASCII + Arabic-Indic digit</td></tr>
     *   <tr><td>{@code "abc"}, {@code ""}, {@code null}</td><td>{@code false}</td><td>{@code false}</td><td>{@code false}</td><td>not numeric</td></tr>
     * </table>
     *
     * <p>The {@link Strings} class provides the digit-class and integer-literal predicates:
     * {@link Strings#isNumeric(CharSequence)} (digit characters only) and
     * {@link Strings#isAsciiInteger(CharSequence)} (optional sign followed by digits). See
     * {@link Strings#isNumeric(CharSequence)} for a side-by-side comparison table that also includes
     * this method and {@link #isCreatable(String)}.</p>
     *
     * @param str the String to check
     * @return {@code true} if and only if {@code str} is within the documented length limit and
     *         {@link Double#parseDouble(String)} succeeds
     * @see #parseFloat(String)
     * @see #parseDouble(String)
     * @see #parseBigDecimal(String)
     * @see #tryParseFloat(String)
     * @see #tryParseDouble(String)
     * @see #isCreatable(String)
     * @see Strings#isNumeric(CharSequence)
     * @see Strings#isAsciiInteger(CharSequence)
     */
    public static boolean isParsable(final String str) {
        if ((str != null && str.length() > MAX_FLOATING_POINT_TOKEN_LENGTH) || !quickCheckForIsParsable(str)) {
            return false;
        }

        try {
            // Float.parseFloat and Double.parseDouble accept the same lexical grammar; range differences
            // produce zero or infinity rather than a parse failure.
            Double.parseDouble(str);
            return true;
        } catch (final NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Cheap, allocation-free pre-filter for {@link #isParsable(String)}. This method recognizes the outer
     * shape and character set of the JDK floating-point grammar, including trim-style surrounding whitespace,
     * signed {@code NaN}/{@code Infinity}, decimal forms and hexadecimal floating-point forms. It deliberately
     * leaves detailed syntax validation to {@link Double#parseDouble(String)}.
     *
     * <p>A {@code false} result means that parsing is guaranteed to fail. A {@code true} result only means that
     * full parsing is required; for example, {@code "1e+"} and {@code "1.2.3"} pass this pre-filter but are
     * rejected by {@code Double.parseDouble}. This helper checks lexical shape only; callers enforce the separate
     * {@value #MAX_FLOATING_POINT_TOKEN_LENGTH}-code-unit API limit.</p>
     *
     * @param str the string to pre-check; may be {@code null}
     * @return {@code false} if {@code str} is definitely not parsable, {@code true} if full parsing is needed
     */
    static boolean quickCheckForIsParsable(final String str) {
        if (Strings.isEmpty(str)) {
            return false;
        }

        int start = 0;
        int end = str.length();

        // Double.parseDouble removes leading and trailing characters as if by String.trim().
        while (start < end && str.charAt(start) <= ' ') {
            start++;
        }

        while (start < end && str.charAt(end - 1) <= ' ') {
            end--;
        }

        if (start == end) {
            return false;
        }

        char ch = str.charAt(start);
        if (ch == '+' || ch == '-') {
            start++;

            if (start == end) {
                return false;
            }
        }

        final int tokenLength = end - start;
        if ((tokenLength == 3 && str.regionMatches(start, "NaN", 0, tokenLength))
                || (tokenLength == 8 && str.regionMatches(start, "Infinity", 0, tokenLength))) {
            return true;
        }

        ch = str.charAt(start);
        if (ch != '.' && (ch < '0' || ch > '9')) {
            return false;
        }

        final boolean isHex = end - start > 2 && ch == '0' && (str.charAt(start + 1) == 'x' || str.charAt(start + 1) == 'X');
        boolean hasDigit = false;
        boolean hasBinaryExponent = !isHex;

        for (int i = isHex ? start + 2 : start; i < end; i++) {
            ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                hasDigit = true;
                continue;
            }

            if (isHex) {
                if ((ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
                    hasDigit = true;
                    continue;
                }

                if (ch == '.' || ch == '+' || ch == '-') {
                    continue;
                }

                if (ch == 'p' || ch == 'P') {
                    hasBinaryExponent = true;
                    continue;
                }
            } else if (ch == '.' || ch == '+' || ch == '-' || ch == 'e' || ch == 'E' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {
                continue;
            }

            return false;
        }

        return hasDigit && hasBinaryExponent;
    }

    /**
     * Maximum number of characters of a caller-supplied token that may appear in an exception message. A
     * {@link Number} that renders its own value is bounded by {@link #MAX_NUMBER_ERROR_TEXT_LENGTH} instead.
     */
    private static final int MAX_ERROR_MESSAGE_INPUT_LENGTH = 64;

    /**
     * Bounds the size of user input embedded in an exception message: returns {@code str} when it is at most
     * {@value #MAX_ERROR_MESSAGE_INPUT_LENGTH} characters, otherwise its first
     * {@value #MAX_ERROR_MESSAGE_INPUT_LENGTH} characters followed by an ellipsis marker and the total length.
     * The cut never splits a surrogate pair, and ISO control characters (C0/C1), U+2028, U+2029 and unpaired
     * surrogates are escaped (a newline becomes the six characters {@code &#92;u000A}), so a hostile
     * multi-megabyte token cannot produce a multi-megabyte, encoding-broken, or line-break-injecting
     * exception message.
     *
     * @param str the input to summarize; must not be {@code null}
     * @return a bounded, escaped preview of {@code str}
     */
    private static String previewForErrorMessage(final String str) {
        return previewForErrorMessage(str, 0, str.length(), MAX_ERROR_MESSAGE_INPUT_LENGTH);
    }

    /** Returns a bounded, escaped preview of {@code str[start, end)} without copying the complete slice. */
    private static String previewForErrorMessage(final String str, final int start, final int end) {
        return previewForErrorMessage(str, start, end, MAX_ERROR_MESSAGE_INPUT_LENGTH);
    }

    /**
     * As {@link #previewForErrorMessage(String, int, int)}, but with an explicit character budget. Only
     * {@link #describeNumberForError(Number, String)} passes a different one: a {@code Number} renders its own value,
     * so its budget is sized from {@link #MAX_NUMBER_ERROR_BIT_LENGTH} rather than from the length of a
     * caller-supplied token.
     */
    private static String previewForErrorMessage(final String str, final int start, final int end, final int limit) {
        final int length = end - start;

        if (length <= limit) {
            return escapeForErrorMessage(str.substring(start, end));
        }

        int cut = start + limit;

        // Do not split a surrogate pair.
        if (Character.isHighSurrogate(str.charAt(cut - 1)) && Character.isLowSurrogate(str.charAt(cut))) {
            cut--;
        }

        return escapeForErrorMessage(str.substring(start, cut)) + "...[" + length + " chars]";
    }

    /**
     * Escapes ISO control characters (C0 and C1, including U+0085), the Unicode line/paragraph
     * separators (U+2028, U+2029), and unpaired surrogates as {@code \\uXXXX} sequences so the result
     * cannot inject line breaks or invalid encodings into a log line. Well-formed surrogate pairs
     * (e.g. emoji) and all other printable input are returned unchanged.
     */
    private static String escapeForErrorMessage(final String str) {
        StringBuilder sb = null;

        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);

            // A well-formed surrogate pair (e.g. an emoji) is printable; pass it through together.
            if (Character.isHighSurrogate(ch) && i + 1 < str.length() && Character.isLowSurrogate(str.charAt(i + 1))) {
                if (sb != null) {
                    sb.append(ch).append(str.charAt(i + 1));
                }
                i++;
                continue;
            }

            if (Character.isISOControl(ch) || ch == 0x2028 || ch == 0x2029 || Character.isSurrogate(ch)) {
                if (sb == null) {
                    sb = new StringBuilder(str.length() + 16).append(str, 0, i);
                }
                sb.append("\\u");
                final String hex = Integer.toHexString(ch).toUpperCase(Locale.ROOT);
                sb.append("0".repeat(4 - hex.length())).append(hex);
            } else if (sb != null) {
                sb.append(ch);
            }
        }

        return sb == null ? str : sb.toString();
    }

    private static String escapeForErrorMessage(final char ch) {
        return escapeForErrorMessage(String.valueOf(ch));
    }

    /**
     * Builds {@code "… is not a valid …."}. Empty or whitespace-only input is shown in quotes so the
     * message is not a leading-space fragment. Long input is truncated to a bounded
     * {@linkplain #previewForErrorMessage(String) preview}. A non-null {@code cause} is attached via
     * {@link Throwable#initCause}; a cause whose own message is oversized (for example a JDK parse
     * exception embedding the whole token) is replaced by a bounded {@link NumberFormatException}
     * carrying a preview of that message, so the retained exception chain stays small and log-safe.
     *
     * @param str the offending input string; must not be {@code null}
     * @param typeName the display name of the target numeric type, canonicalized by {@link #canonicalNfeTypeName(String)}
     * @param cause the underlying parse failure to attach as the cause; may be {@code null}
     * @return the {@link NumberFormatException} to throw
     */
    private static NumberFormatException notAValidNumber(final String str, final String typeName, final NumberFormatException cause) {
        final String preview = previewForErrorMessage(str);
        final String shown = str.isBlank() ? ("\"" + preview + "\"") : preview;
        final NumberFormatException nfe = new NumberFormatException(shown + " is not a valid " + canonicalNfeTypeName(typeName) + ".");

        if (cause != null) {
            // A cause is printed by printStackTrace, so it needs the same treatment as the message: a JDK
            // parse exception embeds the whole token ("For input string: ..."), and a control character in
            // it would otherwise reach the log raw. Bounding alone is not enough -- a short message can
            // still carry a line break -- so the preview is applied whatever the length, and the original
            // exception is kept whenever nothing had to change, so the clean case allocates nothing.
            final String causeMessage = cause.getMessage();
            final String safeMessage = causeMessage == null ? null : previewForErrorMessage(causeMessage);
            nfe.initCause(safeMessage == null || safeMessage.equals(causeMessage) ? cause : new NumberFormatException(safeMessage));
        }

        return nfe;
    }

    /**
     * Maps primitive/{@code "number"} tokens to boxed names so every {@link #notAValidNumber} message
     * uses the same spelling ({@code Integer} not {@code int}, {@code Number} not {@code number}).
     *
     * @param typeName the raw type token passed to {@link #notAValidNumber} (for example {@code "int"} or {@code "number"})
     * @return the canonical boxed spelling of {@code typeName}, or {@code typeName} itself if no mapping applies
     */
    private static String canonicalNfeTypeName(final String typeName) {
        switch (typeName) {
            case "byte":
                return "Byte";
            case "short":
                return "Short";
            case "int":
                return "Integer";
            case "long":
                return "Long";
            case "number":
                return "Number";
            default:
                return typeName;
        }
    }

    /**
     * Converts a {@code String} to a {@code Float}.
     *
     * <p>For a raw input of at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units, this method
     * accepts the complete {@link Float#parseFloat(String)} lexical grammar: signed decimal and hexadecimal
     * floating-point forms, decimal or binary exponents as appropriate, {@code f}/{@code F}/{@code d}/{@code D}
     * suffixes, signed {@code NaN}/{@code Infinity}, and JDK trim-style surrounding whitespace. Non-ASCII
     * digits are rejected. The limit applies before whitespace is removed.</p>
     *
     * <p>Finite overflow produces signed infinity; underflow produces signed zero, and an explicitly signed zero
     * retains its sign. These are numeric results, not parse failures.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.parseFloat("123.45");           // returns 123.45f
     * Numbers.parseFloat("-1.23e4");          // returns -12300.0f
     * Numbers.parseFloat("NaN");              // returns Float.NaN
     * Numbers.parseFloat("Infinity");         // returns Float.POSITIVE_INFINITY
     *
     * // Edge cases
     * Numbers.parseFloat(null);               // returns null
     * Numbers.parseFloat("");                 // returns null
     * Numbers.parseFloat(" ");                // throws NumberFormatException
     * Numbers.parseFloat("\n");               // throws NumberFormatException
     * Numbers.parseFloat("abc");              // throws NumberFormatException
     * }</pre>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the Float value represented by the string, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string exceeds the documented length limit or cannot be parsed as a valid float
     * @see #isCreatable(String)
     * @see #isParsable(String)
     * @see #tryParseFloat(String)
     * @see Float#valueOf(String)
     */
    @MayReturnNull
    public static Float parseFloat(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        return parseFloatWithinLengthLimit(str);
    }

    /**
     * Converts a {@code String} to a {@code Double}.
     *
     * <p>For a raw input of at most {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units, this method
     * accepts the complete {@link Double#parseDouble(String)} lexical grammar: signed decimal and hexadecimal
     * floating-point forms, decimal or binary exponents as appropriate, {@code f}/{@code F}/{@code d}/{@code D}
     * suffixes, signed {@code NaN}/{@code Infinity}, and JDK trim-style surrounding whitespace. Non-ASCII
     * digits are rejected. The limit applies before whitespace is removed.</p>
     *
     * <p>Finite overflow produces signed infinity; underflow produces signed zero, and an explicitly signed zero
     * retains its sign. These are numeric results, not parse failures.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.parseDouble("123.45");      // returns 123.45
     * Numbers.parseDouble("-1.23e10");    // returns -1.23E10
     * Numbers.parseDouble("NaN");         // returns Double.NaN
     * Numbers.parseDouble("Infinity");    // returns Double.POSITIVE_INFINITY
     *
     * // Edge cases
     * Numbers.parseDouble(null);          // returns null
     * Numbers.parseDouble("");            // returns null
     * Numbers.parseDouble(" ");           // throws NumberFormatException
     * Numbers.parseDouble("\n");          // throws NumberFormatException
     * Numbers.parseDouble("abc");         // throws NumberFormatException
     * }</pre>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the Double value represented by the string, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string exceeds the documented length limit or cannot be parsed as a valid double
     * @see #isCreatable(String)
     * @see #isParsable(String)
     * @see #tryParseDouble(String)
     * @see Double#valueOf(String)
     */
    @MayReturnNull
    public static Double parseDouble(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        return parseDoubleWithinLengthLimit(str);
    }

    /**
     * Converts a {@code String} to a {@code BigDecimal}.
     *
     * <p>This method parses a string representation of a decimal number and returns a BigDecimal object.
     * BigDecimal provides arbitrary-precision decimal arithmetic, making it suitable for financial calculations
     * and other scenarios requiring exact decimal representation.</p>
     *
     * <p>The string can contain:</p>
     * <ul>
     * <li>Standard decimal notation: "123.45", "-0.001"</li>
     * <li>Scientific notation: "1.23E+10", "-4.56e-8"</li>
     * <li>Leading/trailing zeros: "0.100", "123.000"</li>
     * <li>A trailing decimal point: "123." (scale 0)</li>
     * </ul>
     *
     * <p>Hexadecimal, hexadecimal floating-point ({@code "0x1.0p2"}), type suffixes and surrounding
     * whitespace are <em>not</em> accepted; those belong to {@link #parseDouble(String)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.parseBigDecimal("123.45");     // returns 123.45
     * Numbers.parseBigDecimal("1.23E+10");   // returns 1.23E+10
     * Numbers.parseBigDecimal("-0.001");     // returns -0.001
     * Numbers.parseBigDecimal("123.");       // returns 123 (a trailing decimal point is accepted)
     *
     * // Edge cases
     * Numbers.parseBigDecimal(null);         // returns null
     * Numbers.parseBigDecimal("");           // returns null
     * Numbers.parseBigDecimal(" ");          // throws NumberFormatException
     * Numbers.parseBigDecimal("\n");         // throws NumberFormatException
     * Numbers.parseBigDecimal("abc");        // throws NumberFormatException
     * }</pre>
     *
     * <p>Non-ASCII Unicode digits are rejected even though {@link BigDecimal#BigDecimal(String)} would accept
     * them via {@code Character.digit}: {@code new BigDecimal("١٢٣")} is {@code 123}, while
     * {@code parseBigDecimal("١٢٣")} throws. This matches {@link #decodeInteger(String)},
     * {@link #decodeLong(String)}, {@link #decodeBigInteger(String)}, and {@link #createNumber(String)}.</p>
     *
     * <p><b>No length limit.</b> {@link #MAX_FLOATING_POINT_TOKEN_LENGTH} is an input-size policy of the
     * bounded {@code float}/{@code double} parsers only (see its documentation: the JDK binary parser is linear,
     * so the cap is a contract rather than a defence), and it does not apply here. An arbitrary-precision
     * decimal is as long as its digits, so an input of any length is accepted. {@link #isParsable(String)} is
     * therefore <em>not</em> a guard for this method.</p>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the BigDecimal value represented by the string, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string is not a valid BigDecimal representation
     * @throws ArithmeticException if the value's unscaled magnitude exceeds the one the JDK's
     *         {@code BigInteger} supports - the "no length limit" above is bounded by that implementation
     *         limit, as {@code decodeBigInteger} and {@code createNumber} already document
     * @see #isCreatable(String)
     * @see #isParsable(String)
     * @see BigDecimal#BigDecimal(String)
     */
    @MayReturnNull
    public static BigDecimal parseBigDecimal(final String str) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        if (!quickCheckForNumericParsing(str)) {
            throw notAValidNumber(str, "BigDecimal", null);
        }

        try {
            return new BigDecimal(str);
        } catch (final NumberFormatException e) {
            throw notAValidNumber(str, "BigDecimal", e);
        }
    }

    /**
     * Attempts to parse a {@code String} as a {@code float} without throwing {@link NumberFormatException}.
     *
     * <p>For non-null, non-empty input within the {@value #MAX_FLOATING_POINT_TOKEN_LENGTH}-code-unit limit,
     * this method uses the same JDK floating-point grammar and produces the same numeric result as
     * {@link #parseFloat(String)}. This includes decimal and hexadecimal floating-point forms,
     * scientific notation, {@code f}/{@code F}/{@code d}/{@code D} suffixes, {@code NaN}, infinity, and
     * JDK-supported surrounding whitespace. Overflow and underflow follow {@link Float#parseFloat(String)}:
     * they produce infinity or signed zero rather than an empty result.</p>
     *
     * <p>{@code null}, empty/blank, over-limit, and malformed input return {@link u.OptionalFloat#empty()} instead of
     * returning {@code null} or throwing {@code NumberFormatException}. The result is produced by one parse;
     * calling {@link #isParsable(String)} first is unnecessary. For every input {@code str},
     * {@code tryParseFloat(str).isPresent()} is equal to {@code isParsable(str)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.tryParseFloat("123.45").getAsFloat();   // returns 123.45f
     * Numbers.tryParseFloat("0x1.0p2").getAsFloat();  // returns 4.0f
     * Numbers.tryParseFloat("NaN").isPresent();       // returns true
     *
     * // Edge cases: every failure yields an empty optional instead of throwing
     * Numbers.tryParseFloat("abc").isEmpty();         // returns true
     * Numbers.tryParseFloat("").isEmpty();            // returns true
     * Numbers.tryParseFloat(" ").isEmpty();           // returns true
     * Numbers.tryParseFloat("\n").isEmpty();          // returns true
     * Numbers.tryParseFloat(null).isEmpty();          // returns true
     * }</pre>
     *
     * @param str the string to parse; may be {@code null}
     * @return an optional containing the parsed float, or an empty optional if {@code str} is {@code null}, empty,
     *         blank, over the documented length limit, or invalid
     * @see #parseFloat(String)
     * @see #tryParseInt(String)
     * @see #tryParseLong(String)
     * @see #tryParseDouble(String)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static u.OptionalFloat tryParseFloat(final String str) {
        if (Strings.isEmpty(str) || str.length() > MAX_FLOATING_POINT_TOKEN_LENGTH) {
            return u.OptionalFloat.empty();
        }

        if (!quickCheckForIsParsable(str)) {
            return u.OptionalFloat.empty();
        }

        try {
            return u.OptionalFloat.of(Float.parseFloat(str));
        } catch (final NumberFormatException ignored) {
            return u.OptionalFloat.empty();
        }
    }

    /**
     * Attempts to parse a {@code String} as a {@code double} without throwing {@link NumberFormatException}.
     *
     * <p>For non-null, non-empty input within the {@value #MAX_FLOATING_POINT_TOKEN_LENGTH}-code-unit limit,
     * this method uses the same JDK floating-point grammar and produces the same numeric result as
     * {@link #parseDouble(String)}. This includes decimal and hexadecimal floating-point forms,
     * scientific notation, {@code f}/{@code F}/{@code d}/{@code D} suffixes, {@code NaN}, infinity, and
     * JDK-supported surrounding whitespace. Overflow and underflow follow {@link Double#parseDouble(String)}:
     * they produce infinity or signed zero rather than an empty result.</p>
     *
     * <p>{@code null}, empty/blank, over-limit, and malformed input return {@link u.OptionalDouble#empty()} instead of
     * returning {@code null} or throwing {@code NumberFormatException}. The result is produced by one parse;
     * calling {@link #isParsable(String)} first is unnecessary. For every input {@code str},
     * {@code tryParseDouble(str).isPresent()} is equal to {@code isParsable(str)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.tryParseDouble("123.45").getAsDouble();   // returns 123.45
     * Numbers.tryParseDouble("0x1.0p2").getAsDouble();  // returns 4.0
     * Numbers.tryParseDouble("NaN").isPresent();        // returns true
     *
     * // Edge cases: every failure yields an empty optional instead of throwing
     * Numbers.tryParseDouble("abc").isEmpty();          // returns true
     * Numbers.tryParseDouble("").isEmpty();             // returns true
     * Numbers.tryParseDouble(" ").isEmpty();            // returns true
     * Numbers.tryParseDouble("\n").isEmpty();           // returns true
     * Numbers.tryParseDouble(null).isEmpty();           // returns true
     * }</pre>
     *
     * @param str the string to parse; may be {@code null}
     * @return an optional containing the parsed double, or an empty optional if {@code str} is {@code null}, empty,
     *         blank, over the documented length limit, or invalid
     * @see #parseDouble(String)
     * @see #tryParseInt(String)
     * @see #tryParseLong(String)
     * @see #tryParseFloat(String)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static u.OptionalDouble tryParseDouble(final String str) {
        if (Strings.isEmpty(str) || str.length() > MAX_FLOATING_POINT_TOKEN_LENGTH) {
            return u.OptionalDouble.empty();
        }

        if (!quickCheckForIsParsable(str)) {
            return u.OptionalDouble.empty();
        }

        try {
            return u.OptionalDouble.of(Double.parseDouble(str));
        } catch (final NumberFormatException ignored) {
            return u.OptionalDouble.empty();
        }
    }

    /**
     * Cheap pre-filter shared by {@link #decodeBigInteger(String)} and {@link #parseBigDecimal(String)}:
     * rejects a {@code null}/empty or sign-only string, a token that does not start with an ASCII digit,
     * {@code '.'}, or {@code '#'}, and any string containing a character outside the ASCII numeric-literal
     * character set (digits, {@code + - . #}, the hexadecimal letters and the {@code x}/{@code l} type markers).
     * Every character is scanned so a non-ASCII digit in the middle of an otherwise ASCII string
     * (for example {@code "1٢11"}) is rejected. A {@code true} result does not mean the string is numeric;
     * it only means the cheap check found no disqualifying character.
     *
     * @param str the string to pre-check; may be {@code null}
     * @return {@code false} if {@code str} is definitely not numeric, {@code true} if full parsing is needed
     */
    private static boolean quickCheckForNumericParsing(final String str) {
        if (Strings.isEmpty(str)) {
            return false;
        }

        final int length = str.length();
        final char first = str.charAt(0);
        final int start = first == '+' || first == '-' ? 1 : 0;

        if (start == length) {
            return false;
        }

        final char firstTokenChar = str.charAt(start);
        if (firstTokenChar != '.' && firstTokenChar != '#' && (firstTokenChar < '0' || firstTokenChar > '9')) {
            return false;
        }

        for (int i = start; i < length; i++) {
            final char ch = str.charAt(i);
            if (ch >= 128 || !alphanumerics[ch]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Allocation-free validator for {@link #isCreatable(String)} and {@link #createNumber(String)}.
     * It recognizes signed decimal, octal and hexadecimal candidates, decimal points, exponents, and supported
     * type suffixes, rejects invalid leading-zero octal digits, and checks the effective {@link BigDecimal} scale
     * range for exponent forms. It validates the complete grammar without constructing the represented number.
     *
     * <p>The result describes grammar and scale validity only. It does not guarantee that constructing an
     * arbitrary-precision result will fit the JDK implementation's supported magnitude or the available memory.</p>
     *
     * @param str the string to pre-check; may be {@code null}
     * @return {@code true} if {@code str} has the complete grammar and representable scale required by
     *         {@code createNumber}; otherwise {@code false}
     */
    static boolean quickCheckForIsCreatable(final String str) {
        return scanCreatable(str) == CREATABLE_VALID;
    }

    /** {@link #scanCreatable(String)} outcome: the whole input is a valid {@link #createNumber(String)} token. */
    private static final int CREATABLE_VALID = -1;

    /**
     * {@link #scanCreatable(String)} failure: the token is empty, or is a sign, radix prefix or exponent
     * marker with no digits after it.
     */
    private static final int CREATABLE_NO_DIGITS = -2;

    /**
     * {@link #scanCreatable(String)} failure: the exponent places the effective {@link BigDecimal} scale
     * outside the representable range.
     */
    private static final int CREATABLE_SCALE_OUT_OF_RANGE = -3;

    /**
     * Allocation-free validator for {@link #isCreatable(String)}, {@link #createNumber(String)} and
     * {@link #tryCreateNumber(String)}, reporting <em>where</em> an invalid input fails so that
     * {@code createNumber} can build a diagnostic without parsing the input a second time.
     *
     * @param str the string to validate; may be {@code null}
     * @return {@link #CREATABLE_VALID} if {@code str} has the complete grammar and representable scale
     *         required by {@code createNumber}; the index of the offending character if one is to blame;
     *         otherwise {@link #CREATABLE_NO_DIGITS} or {@link #CREATABLE_SCALE_OUT_OF_RANGE}
     */
    private static int scanCreatable(final String str) {
        if (Strings.isEmpty(str)) {
            return CREATABLE_NO_DIGITS;
        }

        final int length = str.length();
        int pos = 0;
        char ch = str.charAt(pos);

        if (ch == '+' || ch == '-') {
            pos++;

            if (pos == length) {
                return CREATABLE_NO_DIGITS;
            }
        }

        final int tokenStart = pos;
        int hexStart = -1;
        ch = str.charAt(tokenStart);

        if (ch == '#') {
            hexStart = tokenStart + 1;
        } else if (ch == '0' && tokenStart + 1 < length && (str.charAt(tokenStart + 1) == 'x' || str.charAt(tokenStart + 1) == 'X')) {
            hexStart = tokenStart + 2;
        }

        if (hexStart >= 0) {
            if (hexStart == length) {
                return CREATABLE_NO_DIGITS;
            }

            for (int i = hexStart; i < length; i++) {
                if (!isAsciiHexDigit(str.charAt(i))) {
                    return i;
                }
            }

            return CREATABLE_VALID;
        }

        boolean foundSignificandDigit = false;
        boolean hasDecimalPoint = false;
        boolean hasExponent = false;
        boolean foundExponentDigit = false;

        for (int i = tokenStart; i < length; i++) {
            ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                if (hasExponent) {
                    foundExponentDigit = true;
                } else {
                    foundSignificandDigit = true;
                }
                continue;
            }

            if (ch == '.') {
                if (hasDecimalPoint || hasExponent) {
                    return i;
                }
                hasDecimalPoint = true;
                continue;
            }

            if (ch == 'e' || ch == 'E') {
                if (hasExponent || !foundSignificandDigit) {
                    return i;
                }
                hasExponent = true;
                continue;
            }

            if (ch == '+' || ch == '-') {
                if (!hasExponent || foundExponentDigit || str.charAt(i - 1) != 'e' && str.charAt(i - 1) != 'E') {
                    return i;
                }
                continue;
            }

            if (ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {
                if (i != length - 1) {
                    return i;
                }
                if (!foundSignificandDigit) {
                    return CREATABLE_NO_DIGITS;
                }
                if (hasExponent) {
                    if (!foundExponentDigit) {
                        return CREATABLE_NO_DIGITS;
                    }
                    if (!isBigDecimalScaleInRange(str)) {
                        return CREATABLE_SCALE_OUT_OF_RANGE;
                    }
                }
                return CREATABLE_VALID;
            }

            if (ch == 'l' || ch == 'L') {
                if (i != length - 1) {
                    return i;
                }
                if (!foundSignificandDigit) {
                    return CREATABLE_NO_DIGITS;
                }
                if (hasDecimalPoint || hasExponent) {
                    // An integral suffix cannot follow a decimal point or an exponent.
                    return i;
                }
                final int badOctalDigit = firstNonOctalDigitIndex(str, tokenStart, length - 1);
                return badOctalDigit < 0 ? CREATABLE_VALID : badOctalDigit;
            }

            return i;
        }

        if (!foundSignificandDigit) {
            return CREATABLE_NO_DIGITS;
        }

        if (hasExponent) {
            if (!foundExponentDigit) {
                return CREATABLE_NO_DIGITS;
            }
            return isBigDecimalScaleInRange(str) ? CREATABLE_VALID : CREATABLE_SCALE_OUT_OF_RANGE;
        }

        if (hasDecimalPoint) {
            return CREATABLE_VALID;
        }

        final int badOctalDigit = firstNonOctalDigitIndex(str, tokenStart, length);
        return badOctalDigit < 0 ? CREATABLE_VALID : badOctalDigit;
    }

    /**
     * Rebuilds the {@link NumberFormatException} detail message for a {@link #scanCreatable(String)} failure,
     * mirroring {@link #malformedIntegerTokenCause(String, IntegerTokenScan)} for the {@code createNumber} grammar.
     *
     * @param str the offending input
     * @param failure a non-{@link #CREATABLE_VALID} result of {@link #scanCreatable(String)}
     * @return the cause to attach to the public failure
     */
    private static NumberFormatException creatableFailureCause(final String str, final int failure) {
        return switch (failure) {
            case CREATABLE_NO_DIGITS -> new NumberFormatException("no digits in number token " + previewForErrorMessage(str));
            case CREATABLE_SCALE_OUT_OF_RANGE -> new NumberFormatException("effective BigDecimal scale is out of range in " + previewForErrorMessage(str));
            default -> new NumberFormatException(
                    "invalid character '" + escapeForErrorMessage(str.charAt(failure)) + "' at index " + failure + " of " + previewForErrorMessage(str));
        };
    }

    private static boolean isAsciiHexDigit(final char ch) {
        return ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }

    /**
     * Returns the index of the first digit that disqualifies {@code str[start, end)} as a leading-zero octal
     * literal, or {@code -1} when the range is not leading-zero octal at all or is a valid one. The returned
     * index is always {@code > start}, so it never collides with the negative {@code scanCreatable} outcomes.
     */
    private static int firstNonOctalDigitIndex(final String str, final int start, final int end) {
        if (end - start < 2 || str.charAt(start) != '0') {
            return -1;
        }

        for (int i = start + 1; i < end; i++) {
            if (str.charAt(i) > '7') {
                return i;
            }
        }

        return -1;
    }

    private static boolean isBigDecimalScaleInRange(final String str) {
        int e = -1;
        int decimalPoint = -1;
        boolean zeroSignificand = true;

        for (int i = 0, n = str.length(); i < n; i++) {
            final char ch = str.charAt(i);

            if (ch == '.') {
                decimalPoint = i;
            } else if (ch == 'e' || ch == 'E') {
                e = i;
                break;
            } else if (ch >= '1' && ch <= '9') {
                zeroSignificand = false;
            }
        }

        if (e < 0 || zeroSignificand) {
            return true;
        }

        int start = e + 1;
        int end = str.length();
        final char last = str.charAt(end - 1);
        if (last == 'f' || last == 'F' || last == 'd' || last == 'D') {
            end--;
        }

        boolean negative = false;
        final char firstExponentChar = str.charAt(start);
        if (firstExponentChar == '+' || firstExponentChar == '-') {
            negative = firstExponentChar == '-';
            start++;
        }

        while (start < end && str.charAt(start) == '0') {
            start++;
        }

        // Every admissible effective-scale exponent is between roughly -2.15e9 and 4.30e9.
        if (end - start > 10) {
            return false;
        }

        long exponent = 0;
        for (int i = start; i < end; i++) {
            exponent = exponent * 10 + str.charAt(i) - '0';
        }

        if (negative) {
            exponent = -exponent;
        }

        final int fractionalDigits = decimalPoint < 0 ? 0 : e - decimalPoint - 1;
        final long minExponent = (long) fractionalDigits - Integer.MAX_VALUE;
        final long maxExponent = (long) fractionalDigits - Integer.MIN_VALUE;
        return exponent >= minExponent && exponent <= maxExponent;
    }

    /**
     * <p>Checks whether the String matches the {@linkplain Numbers Numbers-supported numeric grammar}
     * accepted by {@link #createNumber(String)}.</p>
     *
     * <p>That grammar includes hexadecimal marked with {@code 0x}/{@code 0X} or the library-specific
     * {@code #} prefix (for example {@code #FF}), octal numbers, scientific notation and
     * numbers marked with a type qualifier (e.g., 123L). A hex literal with a trailing {@code L}/{@code l}
     * ({@code "0xFFL"}) is <em>not</em> creatable: the hex check rejects {@code L}, and
     * {@link #createNumber(String)} throws for the same input. Use {@link #decodeLong(String)}.
     * It is not the full Java language numeric-literal grammar.</p>
     *
     * <p>Integral forms beginning with a leading zero are treated as octal values. Thus {@code 09} returns
     * {@code false}, while {@code 0123L} is octal 83. A decimal point, exponent, or floating-point suffix selects
     * the floating/decimal path, so {@code 0.9}, {@code 01e1}, and {@code 01f} are creatable.</p>
     *
     * <p>For exponent forms, the effective {@link BigDecimal} scale is validated only when the
     * significand contains a non-zero digit. An all-zero significand is returned by
     * {@link #createNumber(String)} as floating-point zero without constructing a {@code BigDecimal},
     * so its exponent is not constrained by the {@code BigDecimal} scale range.</p>
     *
     * <p>{@code null} and empty/blank {@code String} will return {@code false}.</p>
     *
     * <p>This predicate validates the input without constructing the represented {@link Number}. A {@code true}
     * result does not guarantee that arbitrary-precision construction will fit the JDK implementation's supported
     * magnitude or the available memory. Call {@link #tryCreateNumber(String)} instead when the parsed value is also
     * needed, to avoid validating the input twice.</p>
     *
     * <p>See the <a href="#is-creatable-matrix">class-level {@code isCreatable}/{@code createNumber}
     * relationship matrix</a> for accepted and invalid forms.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.isCreatable("123");      // returns true
     * Numbers.isCreatable("123.45");   // returns true
     * Numbers.isCreatable("0xFF");     // returns true   (hexadecimal)
     * Numbers.isCreatable("01e1");     // returns true   (createNumber returns Double 10.0)
     * Numbers.isCreatable("0123L");    // returns true   (createNumber returns Long 83, octal)
     *
     * // Rejected forms
     * Numbers.isCreatable("09");       // returns false  (invalid octal)
     * Numbers.isCreatable("1٢1");      // returns false  (non-ASCII digit)
     * Numbers.isCreatable("1٢11");     // returns false  (non-ASCII digit, regardless of position)
     * Numbers.isCreatable("abc");      // returns false
     * Numbers.isCreatable("");         // returns false
     * Numbers.isCreatable(null);       // returns false
     * }</pre>
     *
     * <p><b>Comparison of {@code isCreatable}, {@code isParsable} and {@link Strings#isNumeric(CharSequence)}:</b>
     * the predicates accept different forms of numeric string:</p>
     * <ul>
     *   <li>{@link Strings#isNumeric(CharSequence)} &mdash; <i>digit characters only</i>: every character must be a
     *       Unicode digit; no sign, decimal point, exponent, {@code 0x}/{@code #} prefix or type suffix
     *       (non-ASCII digits ARE accepted).</li>
     *   <li>{@code isParsable} &mdash; {@code true} iff the raw input is within the documented length limit and
     *       {@link Double#parseDouble(String)} succeeds after the ASCII/whitespace pre-filter. It accepts scientific notation, a trailing decimal point,
     *       {@code f}/{@code F}/{@code d}/{@code D} suffixes, {@code NaN}, infinity, hex floats, and surrounding
     *       whitespace, and rejects integer hex, a trailing {@code L}/{@code l}, and non-ASCII digits.</li>
     *   <li>{@code isCreatable} &mdash; validates the grammar and required decimal scale used by
     *       {@link #createNumber(String)} without constructing the number:
     *       hexadecimal ({@code 0x}/{@code #}), octal, scientific notation, a trailing decimal point, and
     *       type suffixes ({@code l/L}, {@code f/F}, {@code d/D}).
     *       It is <i>not</i> a strict superset of {@code isParsable}:
     *       {@code "08"}/{@code "09"} and {@code "NaN"} are accepted by {@code isParsable} but rejected by
     *       {@code isCreatable}; {@code "0xFF"} and {@code "123L"} are accepted by {@code isCreatable} but
     *       rejected by {@code isParsable}.</li>
     * </ul>
     *
     * <table border="1">
     *   <caption>{@code isCreatable} vs {@code isParsable} vs {@code Strings.isNumeric}</caption>
     *   <tr><th>Input</th><th>{@code isCreatable}</th><th>{@code isParsable}</th><th>{@code Strings.isNumeric}</th><th>Reason</th></tr>
     *   <tr><td>{@code "123"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code true}</td><td>plain non-negative integer</td></tr>
     *   <tr><td>{@code "-123"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>leading sign is not a digit</td></tr>
     *   <tr><td>{@code "123.45"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>decimal point</td></tr>
     *   <tr><td>{@code ".5"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>leading decimal point (accepted)</td></tr>
     *   <tr><td>{@code "123."}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>trailing decimal point</td></tr>
     *   <tr><td>{@code "0xFF"}</td><td>{@code true}</td><td>{@code false}</td><td>{@code false}</td><td>integer hex ({@code Double.parseDouble} rejects it)</td></tr>
     *   <tr><td>{@code "1.5e3"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>scientific notation</td></tr>
     *   <tr><td>{@code "123L"}</td><td>{@code true}</td><td>{@code false}</td><td>{@code false}</td><td>long suffix</td></tr>
     *   <tr><td>{@code "1.5f"}</td><td>{@code true}</td><td>{@code true}</td><td>{@code false}</td><td>float suffix</td></tr>
     *   <tr><td>{@code "NaN"}</td><td>{@code false}</td><td>{@code true}</td><td>{@code false}</td><td>JDK named float; {@code createNumber} rejects it</td></tr>
     *   <tr><td>{@code "08"}</td><td>{@code false}</td><td>{@code true}</td><td>{@code true}</td><td>leading-zero decimal vs octal</td></tr>
     *   <tr><td>Arabic-Indic {@code U+0661 U+0662 U+0663}</td><td>{@code false}</td><td>{@code false}</td><td>{@code true}</td><td>non-ASCII Unicode digits</td></tr>
     *   <tr><td>{@code "1٢11"}</td><td>{@code false}</td><td>{@code false}</td><td>{@code true}</td><td>mixed ASCII + Arabic-Indic digit</td></tr>
     *   <tr><td>{@code "abc"}, {@code ""}, {@code null}</td><td>{@code false}</td><td>{@code false}</td><td>{@code false}</td><td>not numeric</td></tr>
     * </table>
     *
     * <p>The {@link Strings} class provides the digit-class and integer-literal predicates:
     * {@link Strings#isNumeric(CharSequence)} (digit characters only) and
     * {@link Strings#isAsciiInteger(CharSequence)} (optional sign followed by digits). See
     * {@link Strings#isNumeric(CharSequence)} for a side-by-side comparison table that also includes
     * this method and {@link #isParsable(String)}.</p>
     *
     * @param str the string to check
     * @return {@code true} if {@code str} has the grammar and representable decimal scale required by
     *         {@link #createNumber(String)}; successful allocation of an arbitrary-precision result is not guaranteed
     * @see #isParsable(String)
     * @see #createNumber(String)
     * @see #tryCreateNumber(String)
     * @see Strings#isNumeric(CharSequence)
     * @see Strings#isAsciiInteger(CharSequence)
     */
    public static boolean isCreatable(final String str) {
        return quickCheckForIsCreatable(str);
    }

    /**
     * Converts a {@code String} to a {@code Number}.
     *
     * <p>If the string starts with an optional sign ({@code +} or {@code -}) followed by
     * {@code 0x} (lower or upper case) or {@code #}, it is parsed as a signed hexadecimal value
     * and returned as the narrowest supported type whose inclusive bounds contain that value:
     * {@code Integer}, then {@code Long}, otherwise {@code BigInteger}. Leading zeroes do not
     * affect the selected type. Thus {@code "0xFFFFFFFF"} is a {@code Long},
     * {@code "-0x80000000"} is an {@code Integer}, and {@code "-0x8000000000000000"} is a
     * {@code Long}.</p>
     *
     * <p><b>By design: a type suffix does not pin the runtime type.</b> The value is examined for a type
     * qualifier on the end, i.e., one of {@code 'f','F','d','D','l','L'}. If it is found, a larger type is
     * used when the requested type overflows to infinity or underflows to a signed zero while the
     * input still contains non-zero digits ({@code "1e-50f"} is a {@code Double}, not a {@code Float}).
     * The qualifier is applied only on the <em>non-hex</em>
     * path: hex is dispatched first, and a trailing {@code L}/{@code l} is rejected there, so a
     * Java-style hex long such as {@code "0xFFL"}, {@code "#FFL"}, or {@code "0xFFFFFFFFL"}
     * throws {@code NumberFormatException}. Use {@link #decodeLong(String)} (which excludes the suffix by index
     * before applying decode-style radix rules) or drop the suffix ({@code "0xFF"}). {@code isCreatable} is
     * also {@code false} for hex+{@code L}.</p>
     *
     * <p>If a type specifier is not found, integral forms try {@code Integer} then
     * {@code Long} then {@code BigInteger}. Decimal or exponent forms parse as
     * {@code Double} and escalate to {@code BigDecimal} on overflow to infinity or on
     * underflow to a signed zero while the literal still contains non-zero digits. It is the <em>value</em>
     * that selects the type and never the length of the literal, at any length: leading zeros, a negative
     * exponent or thousands of fraction digits do not push a long spelling of an ordinary {@code double} to
     * {@code BigDecimal}, so {@code createNumber("1" + "0".repeat(310) + "e-310")} and
     * {@code createNumber("1." + "0".repeat(4095))} both return {@code Double.valueOf(1.0)}. A finite,
     * nonzero {@code Double} is kept even when the decimal does not survive IEEE-754 rounding:
     * {@code "1.0000000000000000000000001"} returns {@code Double.valueOf(1.0)}, not a {@code BigDecimal}.</p>
     *
     * <p><b>No length limit.</b> Unlike {@link #parseDouble(String)} and the {@code to*} parsers, which reject a
     * token longer than {@value #MAX_FLOATING_POINT_TOKEN_LENGTH} UTF-16 code units, this method and
     * {@link #tryCreateNumber(String)} accept a token of any length: the JDK binary parsers are linear in the
     * token length, and the value alone decides the type. A lexical zero of any length is the correctly signed
     * {@code Double} (or {@code Float} for an {@code f}/{@code F} suffix) &mdash; which is why {@code "0e"}
     * followed by thousands of exponent digits still succeeds even though it has no {@code BigDecimal}
     * representation &mdash; and an integral token of any length yields {@code BigInteger}. See
     * {@link #MAX_FLOATING_POINT_TOKEN_LENGTH}.</p>
     *
     * <p>Integral values with a leading {@code 0} will be interpreted as octal;
     * the returned number will be Integer, Long or BigInteger as appropriate.
     * This is <b>not</b> {@link #toInt(String)}/{@link #toLong(String)}, which are decimal-first
     * ({@code toInt("010")} is 10, {@code toLong("0123L")} is 123).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.createNumber("123");                          // returns Integer
     * Numbers.createNumber("123.45");                       // returns Double
     * Numbers.createNumber("123L");                         // returns Long
     * Numbers.createNumber("123.45f");                      // returns Float
     * Numbers.createNumber("0xFF");                         // returns Integer (255)
     * Numbers.createNumber("-0x80000000");                  // returns Integer (-2147483648)
     * Numbers.createNumber("010");                          // returns Integer (8, octal; toInt("010") is 10)
     * Numbers.createNumber("1.0000000000000000000000001");  // returns Double 1.0 (IEEE-754 rounding)
     *
     * // Edge cases
     * Numbers.createNumber(null);                           // returns null
     * Numbers.createNumber("");                             // returns null
     * Numbers.createNumber("0xFFL");                        // throws NumberFormatException (use decodeLong)
     * Numbers.createNumber(" ");                            // throws NumberFormatException
     * Numbers.createNumber("\n");                           // throws NumberFormatException
     * Numbers.createNumber("abc");                          // throws NumberFormatException
     * Numbers.createNumber("1٢11");                         // throws NumberFormatException (non-ASCII digit)
     * }</pre>
     *
     * <p>See the <a href="#is-creatable-matrix">class-level {@code isCreatable}/{@code createNumber}
     * relationship matrix</a> for accepted and invalid forms.</p>
     *
     * <p>See the <a href="#create-method-matrix">class-level decode/parse/{@code createNumber} policy and result matrix</a>
     * for the grammar and return type of every typed parse/decode method and {@link #createNumber(String)}.</p>
     *
     * @param str the string to convert; {@code null} or empty returns {@code null}
     * @return the parsed Number value, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if the non-empty string cannot be converted
     * @throws ArithmeticException if an arbitrary-precision result exceeds the JDK implementation's supported magnitude
     * @see #isCreatable(String)
     * @see #tryCreateNumber(String)
     * @see #toInt(String)
     * @see #toLong(String)
     * @see #decodeInteger(String)
     * @see #decodeLong(String)
     * @see #parseFloat(String)
     * @see #parseDouble(String)
     * @see #decodeBigInteger(String)
     * @see #parseBigDecimal(String)
     * @see Long#decode(String)
     */
    @MayReturnNull
    public static Number createNumber(final String str) throws NumberFormatException, ArithmeticException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final int failure = scanCreatable(str);

        if (failure != CREATABLE_VALID) {
            throw notAValidNumber(str, "number", creatableFailureCause(str, failure));
        }

        return createNumberAfterQuickCheck(str);
    }

    /**
     * Attempts to create a {@code Number} without throwing {@link NumberFormatException}.
     *
     * <p>For non-null valid input, this method uses the same grammar, radix rules, and result-type selection
     * as {@link #createNumber(String)}. It can therefore return an {@link Integer}, {@link Long},
     * {@link BigInteger}, {@link Float}, {@link Double}, or {@link BigDecimal}, depending on the input.</p>
     *
     * <p>{@code null}, empty, whitespace-only, and malformed input return {@link u.Optional#empty()} instead of returning
     * {@code null} or throwing {@code NumberFormatException}. The result is produced by one creation attempt;
     * calling {@link #isCreatable(String)} first is unnecessary. Unlike {@code isCreatable}, this method actually
     * constructs the result, so an implementation magnitude or resource limit can still prevent creation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.tryCreateNumber("123").get();       // returns Integer 123
     * Numbers.tryCreateNumber("0xFF").get();      // returns Integer 255
     * Numbers.tryCreateNumber("123L").get();      // returns Long 123
     * Numbers.tryCreateNumber("123.45").get();    // returns Double 123.45
     *
     * // Edge cases: every failure yields an empty optional instead of throwing
     * Numbers.tryCreateNumber("abc").isEmpty();   // returns true
     * Numbers.tryCreateNumber("").isEmpty();      // returns true
     * Numbers.tryCreateNumber(" ").isEmpty();     // returns true
     * Numbers.tryCreateNumber("\n").isEmpty();    // returns true
     * Numbers.tryCreateNumber(null).isEmpty();    // returns true
     * }</pre>
     *
     * @param str the string from which to create a number; may be {@code null}
     * @return an optional containing the created number, or an empty optional if {@code str} is null or invalid
     * @throws ArithmeticException if an arbitrary-precision result exceeds the JDK implementation's supported magnitude
     * @see #createNumber(String)
     * @see #isCreatable(String)
     */
    public static u.Optional<Number> tryCreateNumber(final String str) throws ArithmeticException {
        if (Strings.isEmpty(str)) {
            return u.Optional.empty();
        }

        if (!quickCheckForIsCreatable(str)) {
            return u.Optional.empty();
        }

        try {
            return u.Optional.of(createNumberAfterQuickCheck(str));
        } catch (final NumberFormatException ignored) {
            return u.Optional.empty();
        }
    }

    private static Number createNumberAfterQuickCheck(final String str) throws NumberFormatException {

        final int len = str.length();

        // Check the accepted prefixes directly; a per-call prefix array would add an avoidable hot-path allocation.
        final int offset = str.charAt(0) == '+' || str.charAt(0) == '-' ? 1 : 0;
        final char first = str.charAt(offset);
        final boolean isHex = first == '#' || (first == '0' && offset + 1 < len && (str.charAt(offset + 1) == 'x' || str.charAt(offset + 1) == 'X'));

        if (isHex) {
            // Hex + L is not a combined form. It is rejected by scanCreatable (an 'L' is not a hex digit),
            // so both public entry points have already failed before reaching here; no guard is needed.
            return createIntegralAfterQuickCheck(str, len, true);
        }

        final char lastChar = str.charAt(len - 1);
        final String mant;
        final String dec;
        final int decPos = str.indexOf('.');
        final int ePos = str.indexOf('e');
        final int bigEPos = str.indexOf('E');
        final int expPos = (ePos >= 0 && bigEPos >= 0) ? Math.min(ePos, bigEPos) : (ePos >= 0 ? ePos : bigEPos);

        // Detect if the return type has been requested. The digit test is ASCII-explicit, like every other
        // one in this class: Character.isDigit would also admit non-ASCII Unicode digits, which this class
        // rejects everywhere. Nothing changes today -- scanCreatable has already rejected any such character
        // before this runs -- but it removes the one place a reader had to go and prove that to be sure.
        final boolean requestType = !(lastChar >= '0' && lastChar <= '9') && lastChar != '.';
        if (decPos > -1) { // there is a decimal point
            if (expPos > -1) { // there is an exponent
                // scanCreatable rejects a '.' that follows an 'e'/'E', so expPos > decPos here.
                dec = str.substring(decPos + 1, expPos);
            } else {
                // No exponent, but there may be a type character to remove
                dec = str.substring(decPos + 1, requestType ? len - 1 : len);
            }
            mant = getMantissa(str, decPos);
        } else {
            if (expPos > -1) {
                mant = getMantissa(str, expPos);
            } else {
                // No decimal, no exponent, but there may be a type character to remove
                mant = getMantissa(str, requestType ? len - 1 : len);
            }
            dec = null;
        }

        if (requestType) {
            //Requesting a specific type.
            switch (lastChar) {
                case 'l':
                case 'L':
                    // scanCreatable already proved the payload is a signed decimal/octal integer token with no
                    // decimal point and no exponent, so it is scanned in place: neither the suffix-stripping
                    // substring nor the old Strings.isNumeric re-validation is needed, and a token that fits in
                    // a long never allocates a copy of the input at all.
                    return createIntegralAfterQuickCheck(str, len - 1, false);
                case 'f':
                case 'F':
                    return createFloatingAfterTypeSuffix(str, mant, dec, true);
                case 'd':
                case 'D':
                    return createFloatingAfterTypeSuffix(str, mant, dec, false);
                default:
                    // Unreachable: quickCheckForIsCreatable only admits the suffixes l/L/f/F/d/D.
                    throw notAValidNumber(str, "number", null);
            }
        }
        //User doesn't have a preference on the return type, so let's start
        //small and go from there...
        final boolean hasExponent = expPos > -1 && expPos < len - 1;

        if (dec == null && !hasExponent) { // no decimal point and no exponent
            //Must be an Integer, Long, Biginteger
            return createIntegralAfterQuickCheck(str, len, true);
        }

        try {
            final Double d = Double.valueOf(str);

            if (d.isInfinite() || (d.doubleValue() == 0.0d && !isZero(mant, dec))) {
                return parseBigDecimal(str);
            }

            return d;
        } catch (final NumberFormatException nfe) {
            try {
                return parseBigDecimal(str);
            } catch (final NumberFormatException e) {
                throw notAValidNumber(str, "number", e);
            }
        }
    }

    /**
     * Creates an {@link Integer}, {@link Long}, or {@link BigInteger} from an already-validated, unsuffixed integral token.
     * This helper serves the unsuffixed decimal/octal and hexadecimal paths. One range-checked scan selects the
     * narrowest fitting type directly, instead of catching overflow from successively wider decode attempts;
     * only values beyond the {@code long} range reach {@code decodeBigInteger}.
     *
     * @param str the input, whose prefix {@code [0, end)} is the validated token (sign, decimal/octal/hex digits)
     * @param end the index after the token's last digit: {@code str.length()} on the unsuffixed paths, and
     *        {@code str.length() - 1} when a trailing {@code L}/{@code l} is being excluded by index
     * @param narrowToInt {@code true} to return the narrowest of {@code Integer}/{@code Long}/{@code BigInteger},
     *        {@code false} to start at {@code Long} because an explicit {@code L}/{@code l} suffix asked for it
     * @return the created Integer, Long, or BigInteger
     * @throws NumberFormatException if the token is malformed
     * @throws ArithmeticException if an arbitrary-precision value exceeds the JDK implementation's supported {@link BigInteger} magnitude
     */
    private static Number createIntegralAfterQuickCheck(final String str, final int end, final boolean narrowToInt)
            throws NumberFormatException, ArithmeticException {
        final long value = scanIntegerTokenValueOrInvalid(str, 0, end, IntegerTokenSyntax.DECODE, Long.MIN_VALUE, Long.MAX_VALUE);

        if (value != INVALID_INTEGER_TOKEN || isLongMinValueToken(str, 0, end, IntegerTokenSyntax.DECODE)) {
            // No ternary here: mixing Integer and Long branches would unbox to long and re-box as Long.
            if (narrowToInt && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return Integer.valueOf((int) value);
            }
            return Long.valueOf(value);
        }

        // Only a token beyond the long range needs the suffix-stripping copy that BigInteger's String
        // constructor requires; the common case above never allocates one.
        final String token = end == str.length() ? str : str.substring(0, end);

        try {
            return decodeBigInteger(token);
        } catch (final NumberFormatException e) {
            throw notAValidNumber(str, "number", e);
        }
    }

    /**
     * Shared {@code f}/{@code F} and {@code d}/{@code D} suffix handling for {@link #createNumber(String)}.
     * The last {@link NumberFormatException} from the JDK parsers or {@code parseBigDecimal} is attached as the
     * cause of the unified fallback.
     *
     * <p>The JDK parsers are called directly rather than through {@link #parseFloat(String)}/
     * {@link #parseDouble(String)}: the {@code create} family applies no length limit (the value alone selects
     * the type), whereas those two reject a token longer than {@link #MAX_FLOATING_POINT_TOKEN_LENGTH}.</p>
     *
     * @param str the original input string, including the trailing type suffix
     * @param mant the mantissa digits before any decimal point or exponent
     * @param dec the digits between the decimal point and any exponent; {@code null} when there is no decimal point
     * @param tryFloat {@code true} to try {@code Float} first (for {@code f}/{@code F}),
     *        {@code false} to start at {@code Double} (for {@code d}/{@code D})
     * @return the created {@link Float}, {@link Double}, or {@link BigDecimal}
     * @throws NumberFormatException if the value cannot be converted to any floating/decimal type
     */
    private static Number createFloatingAfterTypeSuffix(final String str, final String mant, final String dec, final boolean tryFloat)
            throws NumberFormatException {
        final int payloadEnd = str.length() - 1;
        NumberFormatException lastFailure = null;

        if (tryFloat) {
            try {
                final float f = Float.parseFloat(str);
                // Primitive comparison on purpose: -0.0f must count as "underflowed to zero".
                if (!(Float.isInfinite(f) || f == 0.0F && !isZero(mant, dec))) {
                    return f;
                }
            } catch (final NumberFormatException e) {
                lastFailure = notAValidNumber(str, "Float", e);
            }
        }

        try {
            final double d = Double.parseDouble(str);
            // Primitive comparison on purpose: -0.0d must count as "underflowed to zero".
            if (!(Double.isInfinite(d) || d == 0.0d && !isZero(mant, dec))) {
                return d;
            }
        } catch (final NumberFormatException e) {
            lastFailure = notAValidNumber(str, "Double", e);
        }

        try {
            return parseBigDecimal(str.substring(0, payloadEnd));
        } catch (final NumberFormatException e) {
            lastFailure = e;
        }

        throw notAValidNumber(str, "number", lastFailure);
    }

    /**
     * <p>Utility method for {@link Numbers#createNumber(java.lang.String)}.</p>
     *
     * <p>Returns mantissa of the given number.</p>
     *
     * @param str the string representation of the number
     * @param stopPos the position of the exponent or decimal point
     * @return mantissa of the given number
     */
    private static String getMantissa(final String str, final int stopPos) {
        final char firstChar = str.charAt(0);
        final boolean hasSign = firstChar == '-' || firstChar == '+';

        // A sign-only token never reaches here: scanCreatable reports it as CREATABLE_NO_DIGITS.
        return hasSign ? str.substring(1, stopPos) : str.substring(0, stopPos);
    }

    /**
     * Utility method for {@link #createNumber(java.lang.String)}.
     *
     * <p>This will check if the magnitude of the number is zero by checking if there
     * are only zeros before and after the decimal place.</p>
     *
     * <p>Note: It is <strong>assumed</strong> that the input string has been converted
     * to either a Float or Double with a value of zero when this method is called.
     * This eliminates invalid input for example {@code ".", ".D", ".e0"}.</p>
     *
     * <p>Thus the method only requires checking if both arguments are {@code null}, empty or contain only zeros.</p>
     *
     * <p>Given {@code s = mant + "." + dec}:</p>
     * <ul>
     * <li>{@code true} if s is {@code "0.0"}
     * <li>{@code true} if s is {@code "0."}
     * <li>{@code true} if s is {@code ".0"}
     * <li>{@code false} otherwise (this assumes {@code "."} is not possible)
     * </ul>
     *
     * @param mant the mantissa decimal digits before the decimal point (sign must be removed; never null)
     * @param dec the decimal digits after the decimal point (exponent and type specifier removed; can be null)
     * @return {@code true} if the magnitude is zero
     */
    private static boolean isZero(final String mant, final String dec) {
        return isAllZeros(mant) && isAllZeros(dec);
    }

    /**
     * Utility method for {@link #createNumber(java.lang.String)}.
     *
     * <p>Returns {@code true} if {@code str} is {@code null}, empty, or contains only {@code '0'} characters.</p>
     *
     * @param str the String to check
     * @return {@code true} if {@code str} is {@code null}, empty, or contains only '0' characters; {@code false} otherwise
     */
    private static boolean isAllZeros(final String str) {
        if (str == null) {
            return true;
        }

        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if {@code n} is a <a href="https://mathworld.wolfram.com/PrimeNumber.html">prime number</a>.
     *
     * <p>Convenience {@code int} overload that widens to {@link #isPrime(long)}; provided to complete the
     * {@code int}/{@code long} family (as with {@link #isPerfectSquare(int)} and {@link #isPowerOfTwo(int)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.isPrime(2);     // returns true  (smallest prime)
     * Numbers.isPrime(17);    // returns true
     * Numbers.isPrime(100);   // returns false (composite)
     *
     * // Edge cases
     * Numbers.isPrime(1);     // returns false (not prime by definition)
     * Numbers.isPrime(0);     // returns false
     * Numbers.isPrime(-7);    // throws IllegalArgumentException (negative)
     * }</pre>
     *
     * @param n the number to test for primality; must be &gt;= 0
     * @return {@code true} if n is prime, {@code false} otherwise (0 and 1 return {@code false})
     * @throws IllegalArgumentException if {@code n} is negative.
     * @see #isPrime(long)
     */
    public static boolean isPrime(final int n) throws IllegalArgumentException {
        return isPrime((long) n);
    }

    /**
     * Returns {@code true} if {@code n} is a <a href="https://mathworld.wolfram.com/PrimeNumber.html">prime number</a>:
     * an integer <i>greater than one</i> that cannot be factored into a product of <i>smaller</i> positive integers.
     *
     * <p>This method uses the Miller-Rabin primality test with deterministic bases for numbers up to {@code Long.MAX_VALUE}.
     * It provides a fast and accurate primality test for 64-bit integers.</p>
     *
     * <p>Returns {@code false} if {@code n} is zero, one, or a composite number (one which <i>can</i> be factored into smaller positive integers).</p>
     *
     * <p>Note: To test larger numbers (beyond long range), use {@link BigInteger#isProbablePrime(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean b1 = Numbers.isPrime(2L);                     // returns true (smallest prime)
     * boolean b2 = Numbers.isPrime(17L);                    // returns true
     * boolean b3 = Numbers.isPrime(100L);                   // returns false (composite)
     * boolean b4 = Numbers.isPrime(9223372036854775783L);   // returns true (largest long prime)
     *
     * // Edge cases
     * boolean b5 = Numbers.isPrime(1L);                     // returns false (not prime by definition)
     * boolean b6 = Numbers.isPrime(0L);                     // returns false
     * Numbers.isPrime(-7L);                                 // throws IllegalArgumentException (negative)
     * }</pre>
     *
     * @param n the number to test for primality; must be &gt;= 0
     * @return {@code true} if n is prime, {@code false} otherwise (0 and 1 return {@code false})
     * @throws IllegalArgumentException if {@code n} is negative.
     * @see #isPrime(int)
     * @see BigInteger#isProbablePrime(int)
     */
    public static boolean isPrime(final long n) throws IllegalArgumentException {
        if (n < 2) {
            checkNonNegative("n", n);
            return false;
        }
        if (n == 2 || n == 3 || n == 5 || n == 7 || n == 11 || n == 13) {
            return true;
        }

        if (((SIEVE_30 & (1 << (n % 30))) != 0) || n % 7 == 0 || n % 11 == 0 || n % 13 == 0) {
            return false;
        }
        if (n < 17 * 17) {
            return true;
        }

        for (final long[] baseSet : millerRabinBaseSets) {
            if (n <= baseSet[0]) {
                for (int i = 1; i < baseSet.length; i++) {
                    if (!MillerRabinTester.test(baseSet[i], n)) {
                        return false;
                    }
                }
                return true;
            }
        }
        throw new AssertionError();
    }

    /**
     * Checks if the given integer is a perfect square (i.e., the square of an integer).
     *
     * <p>A perfect square is a non-negative integer that can be expressed as n = k * k for some integer k.
     * This method uses an optimized algorithm that first checks the lower 4 bits as a fast filter,
     * then performs a square root check for potential candidates.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean b1 = Numbers.isPerfectSquare(16);                // returns true (4*4)
     * boolean b2 = Numbers.isPerfectSquare(25);                // returns true (5*5)
     * boolean b3 = Numbers.isPerfectSquare(26);                // returns false
     *
     * // Edge cases
     * boolean b4 = Numbers.isPerfectSquare(0);                 // returns true (0*0)
     * boolean b5 = Numbers.isPerfectSquare(1);                 // returns true (1*1)
     * boolean b6 = Numbers.isPerfectSquare(-4);                // returns false (never throws; negatives are not squares)
     * boolean b7 = Numbers.isPerfectSquare(Integer.MAX_VALUE); // returns false
     * }</pre>
     *
     * @param n the integer to check
     * @return {@code true} if n is a perfect square, {@code false} otherwise (including for negative numbers)
     * @see #isPerfectSquare(long)
     * @see #isPerfectSquare(BigInteger)
     */
    public static boolean isPerfectSquare(final int n) {
        if (n < 0) {
            return false;
        }

        switch (n & 0xF) {
            case 0:
            case 1:
            case 4:
            case 9:
                final long tst = (long) Math.sqrt(n);
                return tst * tst == n;

            default:
                return false;
        }
    }

    /**
     * Checks if the given long value is a perfect square (i.e., the square of an integer).
     *
     * <p>A perfect square is a non-negative integer that can be expressed as n = k * k for some integer k.
     * This method uses an optimized algorithm that first checks the lower 4 bits as a fast filter,
     * then performs a square root check for potential candidates.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean b1 = Numbers.isPerfectSquare(16L);            // returns true (4*4)
     * boolean b2 = Numbers.isPerfectSquare(100L);           // returns true (10*10)
     * boolean b3 = Numbers.isPerfectSquare(1000000L);       // returns true (1000*1000)
     * boolean b4 = Numbers.isPerfectSquare(1000001L);       // returns false
     *
     * // Edge cases
     * boolean b5 = Numbers.isPerfectSquare(0L);             // returns true (0*0)
     * boolean b6 = Numbers.isPerfectSquare(-100L);          // returns false (never throws; negatives are not squares)
     * boolean b7 = Numbers.isPerfectSquare(Long.MAX_VALUE); // returns false
     * }</pre>
     *
     * @param n the long value to check
     * @return {@code true} if n is a perfect square, {@code false} otherwise (including for negative numbers)
     * @see #isPerfectSquare(int)
     * @see #isPerfectSquare(BigInteger)
     */
    public static boolean isPerfectSquare(final long n) {
        if (n < 0) {
            return false;
        }

        switch ((int) (n & 0xF)) {
            case 0:
            case 1:
            case 4:
            case 9:
                final long tst = (long) Math.sqrt(n);
                return tst * tst == n || (tst + 1) * (tst + 1) == n;

            default:
                return false;
        }
    }

    /**
     * Checks if the given {@code BigInteger} is a perfect square (i.e., the square of an integer).
     *
     * <p>A perfect square is a non-negative integer that can be expressed as {@code n = k * k} for some integer
     * {@code k}. This overload completes the {@code int}/{@code long}/{@code BigInteger} family and supports
     * arbitrary-precision values; it takes the exact integer square root and squares it back, so the answer is
     * exact at every magnitude (no floating-point estimate is involved).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.isPerfectSquare(BigInteger.valueOf(16));                    // returns true (4*4)
     * Numbers.isPerfectSquare(BigInteger.valueOf(1000000));               // returns true (1000*1000)
     * Numbers.isPerfectSquare(BigInteger.valueOf(1000001));               // returns false
     * Numbers.isPerfectSquare(BigInteger.TEN.pow(100));                   // returns true (10^50 squared)
     * Numbers.isPerfectSquare(BigInteger.TEN.pow(101));                   // returns false (an odd power of ten)
     *
     * // Edge cases
     * Numbers.isPerfectSquare(BigInteger.ZERO);                           // returns true (0*0)
     * Numbers.isPerfectSquare(BigInteger.ONE);                            // returns true (1*1)
     * Numbers.isPerfectSquare(BigInteger.valueOf(-4));                    // returns false (negatives are not squares)
     * Numbers.isPerfectSquare((BigInteger) null);                         // throws IllegalArgumentException
     * }</pre>
     *
     * @param n the value to check; must not be {@code null}
     * @return {@code true} if {@code n} is a perfect square, {@code false} otherwise (including for negative values)
     * @throws IllegalArgumentException if {@code n} is {@code null}.
     * @see #isPerfectSquare(int)
     * @see #isPerfectSquare(long)
     * @see #sqrt(BigInteger, RoundingMode)
     */
    public static boolean isPerfectSquare(final BigInteger n) throws IllegalArgumentException {
        N.checkArgNotNull(n, cs.n);

        if (n.signum() < 0) {
            return false;
        }

        final BigInteger root = n.sqrt();

        return root.multiply(root).equals(n);
    }

    /**
     * Checks if the given integer is a power of two (i.e., 2^k for some non-negative integer k).
     *
     * <p>A power of two is a positive integer that can be expressed as 2^k where k is a non-negative integer.
     * This method uses a bitwise trick: a power of two has exactly one bit set in its binary representation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean b1 = Numbers.isPowerOfTwo(16);                // returns true (2^4)
     * boolean b2 = Numbers.isPowerOfTwo(1024);              // returns true (2^10)
     * boolean b3 = Numbers.isPowerOfTwo(100);               // returns false
     *
     * // Edge cases
     * boolean b4 = Numbers.isPowerOfTwo(1);                 // returns true (2^0)
     * boolean b5 = Numbers.isPowerOfTwo(0);                 // returns false
     * boolean b6 = Numbers.isPowerOfTwo(-8);                // returns false (never throws; negatives are not powers of two)
     * boolean b7 = Numbers.isPowerOfTwo(Integer.MIN_VALUE); // returns false (the sign bit is not a positive power)
     * }</pre>
     *
     * @param x the integer to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     * @see #isPowerOfTwo(long)
     * @see #isPowerOfTwo(double)
     * @see #isPowerOfTwo(BigInteger)
     */
    public static boolean isPowerOfTwo(final int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    /**
     * Checks if the given long value is a power of two (i.e., 2^k for some non-negative integer k).
     *
     * <p>A power of two is a positive integer that can be expressed as 2^k where k is a non-negative integer.
     * This method uses a bitwise trick: a power of two has exactly one bit set in its binary representation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean b1 = Numbers.isPowerOfTwo(1024L);            // returns true (2^10)
     * boolean b2 = Numbers.isPowerOfTwo(1099511627776L);   // returns true (2^40)
     * boolean b3 = Numbers.isPowerOfTwo(1000L);            // returns false
     *
     * // Edge cases
     * boolean b4 = Numbers.isPowerOfTwo(1L);               // returns true (2^0)
     * boolean b5 = Numbers.isPowerOfTwo(0L);               // returns false
     * boolean b6 = Numbers.isPowerOfTwo(-8L);              // returns false (never throws; negatives are not powers of two)
     * boolean b7 = Numbers.isPowerOfTwo(Long.MIN_VALUE);   // returns false (the sign bit is not a positive power)
     * }</pre>
     *
     * @param x the long value to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     * @see #isPowerOfTwo(int)
     * @see #isPowerOfTwo(double)
     * @see #isPowerOfTwo(BigInteger)
     */
    public static boolean isPowerOfTwo(final long x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    /**
     * Checks if the given double value is a power of two (i.e., 2^k for some integer k).
     *
     * <p>Unlike the integer versions, this method can handle fractional powers of two (negative exponents)
     * such as 0.5 (2^-1), 0.25 (2^-2), etc., as well as large powers beyond the long range.</p>
     *
     * <p>The method checks that the value is positive, finite (not infinity or NaN), and has a significand
     * (mantissa) that is itself a power of two.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean b1 = Numbers.isPowerOfTwo(16.0);                       // returns true (2^4)
     * boolean b2 = Numbers.isPowerOfTwo(0.5);                        // returns true (2^-1)
     * boolean b3 = Numbers.isPowerOfTwo(0.25);                       // returns true (2^-2)
     * boolean b4 = Numbers.isPowerOfTwo(3.0);                        // returns false
     *
     * // Edge cases
     * boolean b5 = Numbers.isPowerOfTwo(0.0);                        // returns false
     * boolean b6 = Numbers.isPowerOfTwo(-16.0);                      // returns false (must be positive)
     * boolean b7 = Numbers.isPowerOfTwo(Double.NaN);                 // returns false
     * boolean b8 = Numbers.isPowerOfTwo(Double.POSITIVE_INFINITY);   // returns false
     * boolean b9 = Numbers.isPowerOfTwo(Double.MIN_VALUE);           // returns true (2^-1074, a subnormal power)
     * }</pre>
     *
     * @param x the double value to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     * @see #isPowerOfTwo(int)
     * @see #isPowerOfTwo(long)
     * @see #isPowerOfTwo(BigInteger)
     */
    public static boolean isPowerOfTwo(final double x) {
        return x > 0.0 && isFinite(x) && isPowerOfTwo(getSignificand(x));
    }

    /**
     * Checks if the given BigInteger value is a power of two (i.e., 2^k for some non-negative integer k).
     *
     * <p>A power of two is a positive integer that can be expressed as 2^k where k is a non-negative integer.
     * This method efficiently determines this by checking if exactly one bit is set in the binary representation
     * of the number. Specifically, it verifies that the lowest set bit position equals the bit length minus one.</p>
     *
     * <p>This method supports arbitrary-precision arithmetic, making it suitable for very large values beyond
     * the range of primitive types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger bi1 = BigInteger.valueOf(8);
     * boolean b1 = Numbers.isPowerOfTwo(bi1);                 // returns true (2^3)
     *
     * BigInteger bi2 = BigInteger.valueOf(1024);
     * boolean b2 = Numbers.isPowerOfTwo(bi2);                 // returns true (2^10)
     *
     * BigInteger bi3 = BigInteger.valueOf(1000);
     * boolean b3 = Numbers.isPowerOfTwo(bi3);                 // returns false
     *
     * BigInteger large = new BigInteger("2").pow(1000);
     * boolean b4 = Numbers.isPowerOfTwo(large);               // returns true (2^1000)
     *
     * BigInteger large2 = new BigInteger("2").pow(1000).add(BigInteger.ONE);
     * boolean b5 = Numbers.isPowerOfTwo(large2);              // returns false
     *
     * // Edge cases
     * boolean b6 = Numbers.isPowerOfTwo(BigInteger.ZERO);     // returns false (must be positive)
     * boolean b7 = Numbers.isPowerOfTwo(BigInteger.ONE);      // returns true (2^0)
     * Numbers.isPowerOfTwo((BigInteger) null);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     * @throws IllegalArgumentException if {@code x} is {@code null}.
     * @see #isPowerOfTwo(int)
     * @see #isPowerOfTwo(long)
     * @see #isPowerOfTwo(double)
     */
    public static boolean isPowerOfTwo(final BigInteger x) throws IllegalArgumentException {
        N.checkArgNotNull(x, cs.x);
        return x.signum() > 0 && x.getLowestSetBit() == x.bitLength() - 1;
    }

    /**
     * Returns the natural logarithm (base e) of a double value.
     *
     * <p>This is a convenience wrapper around {@link Math#log(double)}. The natural logarithm
     * is the inverse of the exponential function: if y = log(x), then e^y = x.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double log1 = Numbers.log(Math.E);   // returns 1.0 (ln(e) = 1)
     * double log2 = Numbers.log(1.0);      // returns 0.0 (ln(1) = 0)
     * double log3 = Numbers.log(10.0);     // returns ~2.302585 (ln(10))
     * double log4 = Numbers.log(100.0);    // returns ~4.605170 (ln(100))
     *
     * // Edge cases: this method never throws
     * double log5 = Numbers.log(0.0);        // returns -Infinity
     * double log6 = Numbers.log(-1.0);       // returns NaN (outside the domain)
     * double log7 = Numbers.log(Double.NaN); // returns NaN
     * }</pre>
     *
     * @param a the value to compute the logarithm of
     * @return the natural logarithm of the specified value
     * @see Math#log(double)
     * @see #log2(double)
     * @see #log10(double)
     */
    public static double log(final double a) {
        return Math.log(a);
    }

    /**
     * Returns the base-2 logarithm of an integer value, rounded according to the specified rounding mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.log2(8, RoundingMode.DOWN);           // returns 3    (2^3 = 8)
     * Numbers.log2(16, RoundingMode.DOWN);          // returns 4    (2^4 = 16)
     * Numbers.log2(10, RoundingMode.DOWN);          // returns 3    (floor(log2(10)))
     * Numbers.log2(10, RoundingMode.UP);            // returns 4    (ceiling(log2(10)))
     * Numbers.log2(10, RoundingMode.CEILING);       // returns 4
     * Numbers.log2(10, RoundingMode.FLOOR);         // returns 3
     * Numbers.log2(10, RoundingMode.HALF_UP);       // returns 3    (10 < 2^3.5 (~11.3), so rounds down)
     * Numbers.log2(16, RoundingMode.UNNECESSARY);   // returns 4    (exact power of 2)
     *
     * // Edge cases
     * Numbers.log2(1, RoundingMode.DOWN);           // returns 0    (2^0 = 1)
     * Numbers.log2(10, RoundingMode.UNNECESSARY);   // throws ArithmeticException (not a power of 2)
     * Numbers.log2(0, RoundingMode.DOWN);           // throws IllegalArgumentException (x must be positive)
     * Numbers.log2(-8, RoundingMode.DOWN);          // throws IllegalArgumentException (x must be positive)
     * }</pre>
     *
     * @param x the integer value to compute the logarithm of, must be positive
     * @param mode the rounding mode to apply
     * @return the base-2 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if {@code x <= 0}, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     * @see #log2(long, RoundingMode)
     * @see #log2(double)
     * @see #log2(double, RoundingMode)
     * @see #log2(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log2(final int x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkPositive("x", x);

        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                //$FALL-THROUGH$
            case DOWN:
            case FLOOR:
                return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Integer.SIZE - Integer.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                final int leadingZeros = Integer.numberOfLeadingZeros(x);
                final int cmp = INT_MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                final int logFloor = (Integer.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.log2(8L, RoundingMode.DOWN);                // returns 3    (2^3 = 8)
     * Numbers.log2(1024L, RoundingMode.DOWN);             // returns 10   (2^10 = 1024)
     * Numbers.log2(1000L, RoundingMode.DOWN);             // returns 9    (floor(log2(1000)))
     * Numbers.log2(1000L, RoundingMode.UP);               // returns 10   (ceiling(log2(1000)))
     * Numbers.log2(1000L, RoundingMode.HALF_UP);          // returns 10   (1000 > 2^9.5 (~724), so rounds up)
     * Numbers.log2(1048576L, RoundingMode.UNNECESSARY);   // returns 20   (exact power of 2 (2^20))
     *
     * // Edge cases
     * Numbers.log2(1L, RoundingMode.DOWN);                // returns 0    (2^0 = 1)
     * Numbers.log2(1000L, RoundingMode.UNNECESSARY);      // throws ArithmeticException (not a power of 2)
     * Numbers.log2(0L, RoundingMode.DOWN);                // throws IllegalArgumentException (x must be positive)
     * }</pre>
     *
     * @param x the value to compute the logarithm of, must be positive
     * @param mode the rounding mode to apply
     * @return the base-2 logarithm of x, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x <= 0}, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     * @see #log2(int, RoundingMode)
     * @see #log2(double)
     * @see #log2(double, RoundingMode)
     * @see #log2(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log2(final long x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkPositive("x", x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                //$FALL-THROUGH$
            case DOWN:
            case FLOOR:
                return (Long.SIZE - 1) - Long.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Long.SIZE - Long.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                final int leadingZeros = Long.numberOfLeadingZeros(x);
                final long cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                final int logFloor = (Long.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError("impossible");
        }
    }

    /**
     * Returns the base 2 logarithm of a double value.
     *
     * <p>Special cases:
     * <ul>
     * <li>If {@code x} is NaN or less than zero, the result is NaN.
     * <li>If {@code x} is positive infinity, the result is positive infinity.
     * <li>If {@code x} is positive or negative zero, the result is negative infinity.
     * </ul>
     *
     * <p>The computed result is within 1 ulp of the exact result. That ulp is real even at an exact power
     * of two: about one in five of them comes back a hair off the integer (for example {@code log2(0x1p-1066)}
     * is {@code -1066.0000000000002}), so never truncate this result to an {@code int}.
     *
     * <p>If the result of this method will be immediately rounded to an {@code int},
     * {@link #log2(double, RoundingMode)} is both exact and faster.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double log1 = Numbers.log2(8.0);      // returns 3.0 (2^3 = 8)
     * double log2 = Numbers.log2(16.0);     // returns 4.0 (2^4 = 16)
     * double log3 = Numbers.log2(10.0);     // returns ~3.321928 (exact log2(10))
     * double log4 = Numbers.log2(1024.0);   // returns 10.0 (2^10 = 1024)
     * double log5 = Numbers.log2(0.5);      // returns -1.0 (2^-1 = 0.5)
     *
     * // Edge cases: this method never throws
     * double log6 = Numbers.log2(0.0);      // returns -Infinity
     * double log7 = Numbers.log2(-1.0);     // returns NaN (outside the domain)
     * }</pre>
     *
     * @param x the value to compute the logarithm of
     * @return the base-2 logarithm of the specified value
     * @see #log2(int, RoundingMode)
     * @see #log2(long, RoundingMode)
     * @see #log2(double, RoundingMode)
     * @see #log2(BigInteger, RoundingMode)
     * @see #log10(double)
     * @see #log(double)
     */
    public static double log2(final double x) {
        return Math.log(x) / LN_2; // surprisingly within 1 ulp according to tests
    }

    /**
     * Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an
     * {@code int}.
     *
     * <p>Regardless of the rounding mode, this is faster than {@code (int) Numbers.log2(x)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.log2(8.0, RoundingMode.DOWN);           // returns 3    (2^3 = 8)
     * Numbers.log2(10.0, RoundingMode.DOWN);          // returns 3    (floor(log2(10)))
     * Numbers.log2(10.0, RoundingMode.UP);            // returns 4    (ceiling(log2(10)))
     * Numbers.log2(10.0, RoundingMode.FLOOR);         // returns 3
     * Numbers.log2(10.0, RoundingMode.CEILING);       // returns 4
     * Numbers.log2(10.0, RoundingMode.HALF_UP);       // returns 3
     * Numbers.log2(16.0, RoundingMode.UNNECESSARY);   // returns 4    (exact power of 2)
     *
     * // Edge cases
     * Numbers.log2(0.5, RoundingMode.DOWN);           // returns -1   (fractional powers are supported)
     * Numbers.log2(10.0, RoundingMode.UNNECESSARY);   // throws ArithmeticException (not a power of 2)
     * Numbers.log2(0.0, RoundingMode.DOWN);           // throws IllegalArgumentException (must be positive and finite)
     * Numbers.log2(Double.NaN, RoundingMode.DOWN);    // throws IllegalArgumentException (must be positive and finite)
     * }</pre>
     *
     * @param x the value to compute the logarithm of, must be positive and finite
     * @param mode the rounding mode to apply
     * @return the base-2 logarithm of the specified value, rounded to an int
     * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN, or {@code x} is infinite, or if
     *         {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     * @see #log2(int, RoundingMode)
     * @see #log2(long, RoundingMode)
     * @see #log2(double)
     * @see #log2(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log2(final double x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        N.checkArgument(x > 0.0 && isFinite(x), "x must be positive and finite");
        final int exponent = getExponent(x);
        if (!isNormal(x)) {
            return log2(x * IMPLICIT_BIT, mode) - SIGNIFICAND_BITS;
            // Do the calculation on a normal value.
        }
        // x is positive, finite, and normal
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                //$FALL-THROUGH$
            case FLOOR:
                increment = false;
                break;
            case CEILING:
                increment = !isPowerOfTwo(x);
                break;
            case DOWN:
                increment = exponent < 0 & !isPowerOfTwo(x); //NOSONAR
                break;
            case UP:
                increment = exponent >= 0 & !isPowerOfTwo(x); //NOSONAR
                break;
            case HALF_DOWN:
            case HALF_EVEN:
            case HALF_UP:
                final double xScaled = scaleNormalize(x);
                // sqrt(2) is irrational, and the spec is relative to the "exact numerical result,"
                // so log2(x) is never exactly exponent + 0.5.
                increment = (xScaled * xScaled) > 2.0;
                break;
            default:
                throw new AssertionError();
        }
        return increment ? exponent + 1 : exponent;
    }

    /**
     * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger bi1 = BigInteger.valueOf(8);
     * Numbers.log2(bi1, RoundingMode.DOWN);        // returns 3    (2^3 = 8)
     *
     * BigInteger bi2 = BigInteger.valueOf(1024);
     * Numbers.log2(bi2, RoundingMode.DOWN);        // returns 10   (2^10 = 1024)
     *
     * BigInteger bi3 = BigInteger.valueOf(1000);
     * Numbers.log2(bi3, RoundingMode.DOWN);        // returns 9    (floor(log2(1000)))
     * Numbers.log2(bi3, RoundingMode.UP);          // returns 10   (ceiling(log2(1000)))
     *
     * BigInteger large = new BigInteger("2").pow(100);
     * Numbers.log2(large, RoundingMode.DOWN);      // returns 100  (exact power of 2)
     *
     * // Edge cases
     * Numbers.log2(BigInteger.ONE, RoundingMode.DOWN);          // returns 0    (2^0 = 1)
     * Numbers.log2(bi3, RoundingMode.UNNECESSARY);              // throws ArithmeticException (not a power of 2)
     * Numbers.log2(BigInteger.ZERO, RoundingMode.DOWN);         // throws IllegalArgumentException (x must be positive)
     * Numbers.log2((BigInteger) null, RoundingMode.DOWN);       // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to compute the logarithm of, must be {@code non-null} and positive
     * @param mode the rounding mode to apply
     * @return the base-2 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if {@code x} is {@code null} or {@code <= 0}, or if {@code mode} is
     *         {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     * @see #log2(int, RoundingMode)
     * @see #log2(long, RoundingMode)
     * @see #log2(double)
     * @see #log2(double, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    public static int log2(final BigInteger x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkPositive("x", N.checkArgNotNull(x, cs.x));
        final int logFloor = x.bitLength() - 1;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x)); // fall through
            case DOWN, FLOOR:
                return logFloor;

            case UP:
            case CEILING:
                return isPowerOfTwo(x) ? logFloor : logFloor + 1;

            case HALF_DOWN, HALF_UP, HALF_EVEN:
                if (logFloor < SQRT2_PRECOMPUTE_THRESHOLD) {
                    final BigInteger halfPower = SQRT2_PRECOMPUTED_BITS.shiftRight(SQRT2_PRECOMPUTE_THRESHOLD - logFloor);
                    if (x.compareTo(halfPower) <= 0) {
                        return logFloor;
                    } else {
                        return logFloor + 1;
                    }
                }
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                //
                // To determine which side of logFloor.5 the logarithm is,
                // we compare x^2 to 2^(2 * logFloor + 1).
                final BigInteger x2 = x.pow(2);
                final int logX2Floor = x2.bitLength() - 1;
                return (logX2Floor < 2 * logFloor + 1) ? logFloor : logFloor + 1;

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-10 logarithm of an integer value, rounded according to the specified rounding mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.log10(100, RoundingMode.DOWN);           // returns 2    (10^2 = 100)
     * Numbers.log10(1000, RoundingMode.DOWN);          // returns 3    (10^3 = 1000)
     * Numbers.log10(999, RoundingMode.DOWN);           // returns 2    (floor(log10(999)))
     * Numbers.log10(999, RoundingMode.UP);             // returns 3    (ceiling(log10(999)))
     * Numbers.log10(500, RoundingMode.HALF_UP);        // returns 3    (500 > 10^2.5 (~316), so rounds up)
     * Numbers.log10(1000, RoundingMode.UNNECESSARY);   // returns 3    (exact power of 10)
     *
     * // Edge cases
     * Numbers.log10(1, RoundingMode.DOWN);             // returns 0    (10^0 = 1)
     * Numbers.log10(999, RoundingMode.UNNECESSARY);    // throws ArithmeticException (not a power of 10)
     * Numbers.log10(0, RoundingMode.DOWN);             // throws IllegalArgumentException (x must be positive)
     * }</pre>
     *
     * @param x the integer value to compute the logarithm of, must be positive
     * @param mode the rounding mode to apply
     * @return the base-10 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if {@code x <= 0}, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     * @see #log10(long, RoundingMode)
     * @see #log10(double)
     * @see #log10(double, RoundingMode)
     * @see #log10(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log10(final int x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkPositive("x", x);

        final int logFloor = log10Floor(x);
        final int floorPow = int_powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(x == floorPow);
                //$FALL-THROUGH$
            case FLOOR:
            case DOWN:
                return logFloor;
            case CEILING:
            case UP:
                return logFloor + lessThanBranchFree(floorPow, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // sqrt(10) is irrational, so log10(x) - logFloor is never exactly 0.5
                return logFloor + lessThanBranchFree(int_halfPowersOf10[logFloor], x);
            default:
                throw new AssertionError();
        }
    }

    private static int log10Floor(final int x) {
        /*
         * Based on Hacker's Delight Fig. 11-5, the two-table-lookup, branch-free implementation.
         *
         * The key idea is that based on the number of leading zeros (equivalently, floor(log2(x))),
         * we can narrow the possible floor(log10(x)) values to two.  For example, if floor(log2(x))
         * is 6, then 64 <= x < 128, so floor(log10(x)) is either 1 or 2.
         */
        final int y = int_maxLog10ForLeadingZeros[Integer.numberOfLeadingZeros(x)];
        /*
         * y is the higher of the two possible values of floor(log10(x)). If x < 10^y, then we want the
         * lower of the two possible values, or y - 1, otherwise, we want y.
         */
        return y - lessThanBranchFree(x, int_powersOf10[y]);
    }

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.log10(100L, RoundingMode.DOWN);                 // returns 2    (10^2 = 100)
     * Numbers.log10(1000000L, RoundingMode.DOWN);             // returns 6    (10^6 = 1000000)
     * Numbers.log10(999999L, RoundingMode.DOWN);              // returns 5    (floor(log10(999999)))
     * Numbers.log10(999999L, RoundingMode.UP);                // returns 6    (ceiling(log10(999999)))
     * Numbers.log10(1000000000L, RoundingMode.UNNECESSARY);   // returns 9    (exact power of 10 (10^9))
     *
     * // Edge cases
     * Numbers.log10(1L, RoundingMode.DOWN);                   // returns 0    (10^0 = 1)
     * Numbers.log10(999L, RoundingMode.UNNECESSARY);          // throws ArithmeticException (not a power of 10)
     * Numbers.log10(0L, RoundingMode.DOWN);                   // throws IllegalArgumentException (x must be positive)
     * }</pre>
     *
     * @param x the value to compute the logarithm of, must be positive
     * @param mode the rounding mode to apply
     * @return the base-10 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if {@code x <= 0}, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     * @see #log10(int, RoundingMode)
     * @see #log10(double)
     * @see #log10(double, RoundingMode)
     * @see #log10(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log10(final long x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkPositive("x", x);

        final int logFloor = log10Floor(x);
        final long floorPow = powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(x == floorPow);
                //$FALL-THROUGH$
            case FLOOR:
            case DOWN:
                return logFloor;
            case CEILING:
            case UP:
                return logFloor + lessThanBranchFree(floorPow, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // sqrt(10) is irrational, so log10(x)-logFloor is never exactly 0.5
                return logFloor + lessThanBranchFree(halfPowersOf10[logFloor], x);
            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-10 logarithm of the given double value.
     *
     * <p>This method is a convenience wrapper around {@link Math#log10(double)}, providing the base-10
     * logarithm calculation. The result is the power to which 10 must be raised to obtain the value x.</p>
     *
     * <p>Special cases:</p>
     * <ul>
     * <li>If the argument is NaN or less than zero, the result is NaN.</li>
     * <li>If the argument is positive infinity, the result is positive infinity.</li>
     * <li>If the argument is positive zero or negative zero, the result is negative infinity.</li>
     * <li>If the argument is 10^n for integer n, the result is n.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double log1 = Numbers.log10(100.0);    // returns 2.0 (10^2 = 100)
     * double log2 = Numbers.log10(1000.0);   // returns 3.0 (10^3 = 1000)
     * double log3 = Numbers.log10(1.0);      // returns 0.0 (10^0 = 1)
     * double log4 = Numbers.log10(0.1);      // returns -1.0 (10^-1 = 0.1)
     *
     * // Edge cases: this method never throws
     * double log5 = Numbers.log10(0.0);        // returns -Infinity
     * double log6 = Numbers.log10(-1.0);       // returns NaN (outside the domain)
     * double log7 = Numbers.log10(Double.NaN); // returns NaN
     * }</pre>
     *
     * @param x the value to compute the logarithm of
     * @return the base-10 logarithm of x
     * @see Math#log10(double)
     * @see #log10(int, RoundingMode)
     * @see #log10(long, RoundingMode)
     * @see #log10(double, RoundingMode)
     * @see #log10(BigInteger, RoundingMode)
     * @see #log2(double)
     * @see #log(double)
     */
    public static double log10(final double x) {
        return Math.log10(x);
    }

    /**
     * Returns the base-10 logarithm of a positive finite {@code double}, rounded to an {@code int}
     * with the specified rounding mode.
     *
     * <p>Rounding is based on the exact numerical value represented by {@code x}, not on the rounded
     * result returned by {@link Math#log10(double)}. This matters at decimal boundaries that are not
     * exactly representable in binary floating point. For example, the {@code double} value {@code 0.1}
     * is slightly greater than the exact value 10<sup>-1</sup>, so {@code FLOOR} returns {@code -1},
     * {@code CEILING} returns {@code 0}, and {@code UNNECESSARY} throws.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.log10(100.0, RoundingMode.UNNECESSARY);  // returns 2
     * Numbers.log10(200.0, RoundingMode.FLOOR);        // returns 2
     * Numbers.log10(200.0, RoundingMode.CEILING);      // returns 3
     * Numbers.log10(300.0, RoundingMode.HALF_UP);      // returns 2
     * Numbers.log10(400.0, RoundingMode.HALF_UP);      // returns 3
     * Numbers.log10(0.2, RoundingMode.DOWN);           // returns 0 (toward zero)
     * Numbers.log10(0.2, RoundingMode.UP);             // returns -1 (away from zero)
     *
     * // Edge cases
     * Numbers.log10(0.1, RoundingMode.UNNECESSARY);    // throws ArithmeticException
     * Numbers.log10(0.0, RoundingMode.FLOOR);          // throws IllegalArgumentException
     * Numbers.log10(Double.NaN, RoundingMode.FLOOR);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to compute the logarithm of; must be positive and finite
     * @param mode the rounding mode to apply; must not be {@code null}
     * @return the base-10 logarithm of the exact value represented by {@code x}, rounded to an int
     * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN or infinite, or {@code mode} is {@code null}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x} is not exactly a power of ten
     * @see #log10(double)
     * @see #log10(int, RoundingMode)
     * @see #log10(long, RoundingMode)
     * @see #log10(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    public static int log10(final double x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        N.checkArgument(x > 0.0 && isFinite(x), "x must be positive and finite");

        // BigDecimal(double) captures the exact binary floating-point value. Its decimal value lies in
        // [10^(precision-scale-1), 10^(precision-scale)), so floor(log10(x)) follows directly — no
        // estimate-and-correct loop and no per-comparison power construction.
        final BigDecimal exactX = new BigDecimal(x);
        final int logFloor = exactX.precision() - exactX.scale() - 1;

        // Only 10^0 … 10^22 are exactly representable as doubles (5^23 exceeds 2^53), and no negative
        // power of ten is dyadic, so an exact power of ten is recognized by a primitive table compare.
        final boolean isPowerOfTen = logFloor >= 0 && logFloor < EXACT_DOUBLE_POWERS_OF_TEN.length && x == EXACT_DOUBLE_POWERS_OF_TEN[logFloor];

        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTen);
                return logFloor;

            case FLOOR:
                return logFloor;

            case CEILING:
                return isPowerOfTen ? logFloor : logFloor + 1;

            case DOWN:
                return logFloor < 0 && !isPowerOfTen ? logFloor + 1 : logFloor;

            case UP:
                return logFloor >= 0 && !isPowerOfTen ? logFloor + 1 : logFloor;

            case HALF_DOWN:
            case HALF_EVEN:
            case HALF_UP:
                // sqrt(10) is irrational, so an exact double can never be exactly halfway.
                final BigDecimal squaredX = exactX.multiply(exactX);
                final BigDecimal squaredHalfPower = BigDecimal.ONE.scaleByPowerOfTen(2 * logFloor + 1);
                return squaredX.compareTo(squaredHalfPower) < 0 ? logFloor : logFloor + 1;

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger bi1 = BigInteger.valueOf(100);
     * Numbers.log10(bi1, RoundingMode.DOWN);       // returns 2    (10^2 = 100)
     *
     * BigInteger bi2 = BigInteger.valueOf(1000000);
     * Numbers.log10(bi2, RoundingMode.DOWN);       // returns 6    (10^6 = 1000000)
     *
     * BigInteger bi3 = new BigInteger("1000000000000");
     * Numbers.log10(bi3, RoundingMode.DOWN);       // returns 12   (10^12)
     *
     * BigInteger large = BigInteger.TEN.pow(100);
     * Numbers.log10(large, RoundingMode.DOWN);     // returns 100  (exact power of 10)
     *
     * // Edge cases
     * Numbers.log10(BigInteger.ONE, RoundingMode.DOWN);                  // returns 0    (10^0 = 1)
     * Numbers.log10(BigInteger.valueOf(999), RoundingMode.UNNECESSARY);  // throws ArithmeticException (not a power of 10)
     * Numbers.log10(BigInteger.ZERO, RoundingMode.DOWN);                 // throws IllegalArgumentException (x must be positive)
     * Numbers.log10((BigInteger) null, RoundingMode.DOWN);               // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to compute the logarithm of, must be {@code non-null} and positive
     * @param mode the rounding mode to apply
     * @return the base-10 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if {@code x} is {@code null} or {@code <= 0}, or if {@code mode} is
     *         {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     * @see #log10(int, RoundingMode)
     * @see #log10(long, RoundingMode)
     * @see #log10(double)
     * @see #log10(double, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log10(final BigInteger x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkPositive("x", N.checkArgNotNull(x, cs.x));
        if (fitsInLong(x)) {
            return log10(x.longValue(), mode);
        }

        int approxLog10 = (int) (log2(x, FLOOR) * LN_2 / LN_10);
        BigInteger approxPow = BigInteger.TEN.pow(approxLog10);
        int approxCmp = approxPow.compareTo(x);

        /*
         * We adjust approxLog10 and approxPow until they're equal to floor(log10(x)) and
         * 10^floor(log10(x)).
         */

        if (approxCmp > 0) {
            /*
             * The code is written so that even completely incorrect approximations will still yield the
             * correct answer eventually, but in practice this branch should almost never be entered, and
             * even then the loop should not run more than once.
             */
            do {
                approxLog10--;
                approxPow = approxPow.divide(BigInteger.TEN);
                approxCmp = approxPow.compareTo(x);
            } while (approxCmp > 0);
        } else {
            BigInteger nextPow = BigInteger.TEN.multiply(approxPow);
            int nextCmp = nextPow.compareTo(x);
            while (nextCmp <= 0) {
                approxLog10++;
                approxPow = nextPow;
                approxCmp = nextCmp;
                nextPow = BigInteger.TEN.multiply(approxPow);
                nextCmp = nextPow.compareTo(x);
            }
        }

        final int floorLog = approxLog10;
        final BigInteger floorPow = approxPow;
        final int floorCmp = approxCmp;

        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(floorCmp == 0);
                //$FALL-THROUGH$
            case FLOOR:
            case DOWN:
                return floorLog;

            case CEILING:
            case UP:
                return floorPow.equals(x) ? floorLog : floorLog + 1;

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(10) is irrational, log10(x) - floorLog can never be exactly 0.5
                final BigInteger x2 = x.pow(2);
                final BigInteger halfPowerSquared = floorPow.pow(2).multiply(BigInteger.TEN);
                return (x2.compareTo(halfPowerSquared) <= 0) ? floorLog : floorLog + 1;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns {@code true} if {@code x} is exactly representable as a {@code long}, i.e. if
     * {@code x.longValue()} loses no information.
     *
     * @param x the value to test; must not be {@code null}
     * @return {@code true} if {@code x} fits in a {@code long}, {@code false} otherwise
     */
    static boolean fitsInLong(final BigInteger x) {
        return x.bitLength() <= Long.SIZE - 1;
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code x}. This is equivalent to
     * {@code Numbers.powExact(2, Numbers.log2(x, CEILING))}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.ceilingPowerOfTwo(7);                    // returns 8     (next power of 2 after 7)
     * Numbers.ceilingPowerOfTwo(8);                    // returns 8     (8 is already a power of 2)
     * Numbers.ceilingPowerOfTwo(9);                    // returns 16    (next power of 2 after 9)
     * Numbers.ceilingPowerOfTwo(100);                  // returns 128   (2^7 = 128)
     * Numbers.ceilingPowerOfTwo(1000);                 // returns 1024  (2^10 = 1024)
     *
     * // Edge cases
     * Numbers.ceilingPowerOfTwo(1);                    // returns 1     (2^0)
     * Numbers.ceilingPowerOfTwo(1 << 30);              // returns 1073741824 (the largest representable power)
     * Numbers.ceilingPowerOfTwo(0);                    // throws IllegalArgumentException (x must be positive)
     * Numbers.ceilingPowerOfTwo(Integer.MAX_VALUE);    // throws ArithmeticException (2^31 does not fit an int)
     * }</pre>
     *
     * @param x the value to compute the ceiling power of two for, must be positive
     * @return the smallest power of two greater than or equal to x
     * @throws IllegalArgumentException if {@code x <= 0}.
     * @throws ArithmeticException if the next-higher power of two is not representable as an
     *         {@code int}, i.e., when {@code x > 2^30}
     * @see #ceilingPowerOfTwo(long)
     * @see #ceilingPowerOfTwo(BigInteger)
     * @see #floorPowerOfTwo(int)
     */
    public static int ceilingPowerOfTwo(final int x) throws IllegalArgumentException, ArithmeticException {
        checkPositive("x", x);
        if (x > (1 << (Integer.SIZE - 2))) {
            throw new ArithmeticException("ceilingPowerOfTwo(" + x + ") is not representable as an int");
        }
        return 1 << -Integer.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code x}.  This is equivalent to
     * {@code Numbers.powExact(2, Numbers.log2(x, CEILING))}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.ceilingPowerOfTwo(7L);                // returns 8     (next power of 2 after 7)
     * Numbers.ceilingPowerOfTwo(8L);                // returns 8     (8 is already a power of 2)
     * Numbers.ceilingPowerOfTwo(9L);                // returns 16    (next power of 2 after 9)
     * Numbers.ceilingPowerOfTwo(100L);              // returns 128   (2^7 = 128)
     * Numbers.ceilingPowerOfTwo(1000L);             // returns 1024  (2^10 = 1024)
     *
     * // Edge cases
     * Numbers.ceilingPowerOfTwo(1L);                // returns 1     (2^0)
     * Numbers.ceilingPowerOfTwo(0L);                // throws IllegalArgumentException (x must be positive)
     * Numbers.ceilingPowerOfTwo(Long.MAX_VALUE);    // throws ArithmeticException (2^63 does not fit a long)
     * }</pre>
     *
     * @param x the value to compute the ceiling power of two for, must be positive
     * @return the smallest power of two greater than or equal to x
     * @throws IllegalArgumentException if {@code x <= 0}.
     * @throws ArithmeticException if the next-higher power of two is not representable as a
     *         {@code long}, i.e., when {@code x > 2^62}
     * @see #ceilingPowerOfTwo(int)
     * @see #ceilingPowerOfTwo(BigInteger)
     * @see #floorPowerOfTwo(long)
     */
    public static long ceilingPowerOfTwo(final long x) throws IllegalArgumentException, ArithmeticException {
        checkPositive("x", x);
        if (x > MAX_SIGNED_POWER_OF_TWO) {
            throw new ArithmeticException("ceilingPowerOfTwo(" + x + ") is not representable as a long");
        }
        return 1L << -Long.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to the given BigInteger value.
     *
     * <p>This method rounds x up to the nearest power of two. For example, if x is 100,
     * the result will be 128 (2^7). This is equivalent to {@code 2^log2(x, RoundingMode.CEILING)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger result1 = Numbers.ceilingPowerOfTwo(BigInteger.valueOf(100));   // returns 128
     * BigInteger result2 = Numbers.ceilingPowerOfTwo(BigInteger.valueOf(128));   // returns 128
     * BigInteger result3 = Numbers.ceilingPowerOfTwo(BigInteger.valueOf(129));   // returns 256
     * BigInteger result4 = Numbers.ceilingPowerOfTwo(BigInteger.ONE);            // returns 1
     *
     * // Edge cases: unlike the primitive overloads there is no overflow case
     * Numbers.ceilingPowerOfTwo(BigInteger.ZERO);                                // throws IllegalArgumentException (must be positive)
     * Numbers.ceilingPowerOfTwo((BigInteger) null);                              // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the BigInteger value (must be positive)
     * @return the smallest power of two greater than or equal to x
     * @throws IllegalArgumentException if {@code x} is not positive, or if {@code x} is {@code null}.
     * @see #ceilingPowerOfTwo(int)
     * @see #ceilingPowerOfTwo(long)
     * @see #floorPowerOfTwo(BigInteger)
     */
    public static BigInteger ceilingPowerOfTwo(final BigInteger x) throws IllegalArgumentException {
        return BigInteger.ZERO.setBit(log2(x, RoundingMode.CEILING));
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}. This is equivalent to
     * {@code Numbers.powExact(2, Numbers.log2(x, FLOOR))}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.floorPowerOfTwo(100);                 // returns 64    (2^6)
     * Numbers.floorPowerOfTwo(128);                 // returns 128   (already power of 2)
     * Numbers.floorPowerOfTwo(129);                 // returns 128
     *
     * // Edge cases
     * Numbers.floorPowerOfTwo(1);                   // returns 1     (2^0)
     * Numbers.floorPowerOfTwo(Integer.MAX_VALUE);   // returns 1073741824 (2^30; never overflows)
     * Numbers.floorPowerOfTwo(0);                   // throws IllegalArgumentException (x must be positive)
     * }</pre>
     *
     * @param x the value to compute the floor power of two for, must be positive
     * @return the largest power of two less than or equal to x
     * @throws IllegalArgumentException if {@code x <= 0}.
     * @see #floorPowerOfTwo(long)
     * @see #floorPowerOfTwo(BigInteger)
     * @see #ceilingPowerOfTwo(int)
     */
    public static int floorPowerOfTwo(final int x) throws IllegalArgumentException {
        checkPositive("x", x);

        return 1 << ((Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x));
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}.  This is equivalent to
     * {@code Numbers.powExact(2, Numbers.log2(x, FLOOR))}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.floorPowerOfTwo(100L);             // returns 64    (2^6)
     * Numbers.floorPowerOfTwo(128L);             // returns 128   (already power of 2)
     * Numbers.floorPowerOfTwo(129L);             // returns 128
     *
     * // Edge cases
     * Numbers.floorPowerOfTwo(1L);               // returns 1     (2^0)
     * Numbers.floorPowerOfTwo(Long.MAX_VALUE);   // returns 4611686018427387904L (2^62; never overflows)
     * Numbers.floorPowerOfTwo(0L);               // throws IllegalArgumentException (x must be positive)
     * }</pre>
     *
     * @param x the value to compute the floor power of two for, must be positive
     * @return the largest power of two less than or equal to x
     * @throws IllegalArgumentException if {@code x <= 0}.
     * @see #floorPowerOfTwo(int)
     * @see #floorPowerOfTwo(BigInteger)
     * @see #ceilingPowerOfTwo(long)
     */
    public static long floorPowerOfTwo(final long x) throws IllegalArgumentException {
        checkPositive("x", x);

        return 1L << ((Long.SIZE - 1) - Long.numberOfLeadingZeros(x));
    }

    /**
     * Returns the largest power of two less than or equal to the given BigInteger value.
     *
     * <p>This method rounds x down to the nearest power of two. For example, if x is 100,
     * the result will be 64 (2^6). This is equivalent to {@code 2^log2(x, RoundingMode.FLOOR)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger result1 = Numbers.floorPowerOfTwo(BigInteger.valueOf(100));   // returns 64
     * BigInteger result2 = Numbers.floorPowerOfTwo(BigInteger.valueOf(128));   // returns 128
     * BigInteger result3 = Numbers.floorPowerOfTwo(BigInteger.valueOf(129));   // returns 128
     * BigInteger result4 = Numbers.floorPowerOfTwo(BigInteger.ONE);            // returns 1
     *
     * // Edge cases
     * Numbers.floorPowerOfTwo(BigInteger.ZERO);                                // throws IllegalArgumentException (must be positive)
     * Numbers.floorPowerOfTwo((BigInteger) null);                              // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the BigInteger value (must be positive)
     * @return the largest power of two less than or equal to x
     * @throws IllegalArgumentException if {@code x} is not positive, or if {@code x} is {@code null}.
     * @see #floorPowerOfTwo(int)
     * @see #floorPowerOfTwo(long)
     * @see #ceilingPowerOfTwo(BigInteger)
     */
    public static BigInteger floorPowerOfTwo(final BigInteger x) throws IllegalArgumentException {
        return BigInteger.ZERO.setBit(log2(x, RoundingMode.FLOOR));
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * <p>This method computes the integer square root of a non-negative integer value,
     * applying the specified rounding mode to handle non-perfect squares.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.sqrt(9, RoundingMode.DOWN);           // returns 3    (perfect square)
     * Numbers.sqrt(10, RoundingMode.DOWN);          // returns 3    (rounds toward zero)
     * Numbers.sqrt(10, RoundingMode.UP);            // returns 4    (rounds away from zero)
     * Numbers.sqrt(10, RoundingMode.FLOOR);         // returns 3    (rounds toward negative infinity)
     * Numbers.sqrt(10, RoundingMode.CEILING);       // returns 4    (rounds toward positive infinity)
     * Numbers.sqrt(10, RoundingMode.HALF_UP);       // returns 3    (rounds to nearest, ties away from zero)
     * Numbers.sqrt(11, RoundingMode.HALF_UP);       // returns 3    (11 is closer to 9 than 16)
     * Numbers.sqrt(16, RoundingMode.UNNECESSARY);   // returns 4    (exact square root required)
     *
     * // Edge cases
     * Numbers.sqrt(0, RoundingMode.DOWN);           // returns 0
     * Numbers.sqrt(10, RoundingMode.UNNECESSARY);   // throws ArithmeticException (not a perfect square)
     * Numbers.sqrt(-1, RoundingMode.DOWN);          // throws IllegalArgumentException (x must be non-negative)
     * }</pre>
     *
     * @param x the value to compute the square root of; must be non-negative
     * @param mode the rounding mode to apply
     * @return the integer square root of {@code x}, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x < 0}, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code x} is not a perfect square
     * @see #sqrt(long, RoundingMode)
     * @see #sqrt(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    public static int sqrt(final int x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkNonNegative("x", x);
        final int sqrtFloor = sqrtFloor(x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(sqrtFloor * sqrtFloor == x); // fall through
            case FLOOR:
            case DOWN:
                return sqrtFloor;
            case CEILING:
            case UP:
                return sqrtFloor + lessThanBranchFree(sqrtFloor * sqrtFloor, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                final int halfSquare = sqrtFloor * sqrtFloor + sqrtFloor;
                /*
                 * We wish to test whether x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both
                 * x and halfSquare are integers, this is equivalent to testing whether x <=
                 * halfSquare. (We have to deal with overflow, though.)
                 *
                 * If we treat halfSquare as an unsigned int, we know that
                 *            sqrtFloor^2 <= x < (sqrtFloor + 1)^2
                 * halfSquare - sqrtFloor <= x < halfSquare + sqrtFloor + 1
                 * so |x - halfSquare| <= sqrtFloor.  Therefore, it's safe to treat x - halfSquare as a
                 * signed int, so lessThanBranchFree is safe for use.
                 */
                return sqrtFloor + lessThanBranchFree(halfSquare, x);
            default:
                throw new AssertionError();
        }
    }

    private static int sqrtFloor(final int x) {
        // Every int is exactly representable as a double, whose significand has 53 bits of precision.
        return (int) Math.sqrt(x);
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * <p>This method computes the integer square root of a non-negative long value,
     * applying the specified rounding mode to handle non-perfect squares.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.sqrt(9L, RoundingMode.DOWN);                    // returns 3L    (perfect square)
     * Numbers.sqrt(10L, RoundingMode.DOWN);                   // returns 3L    (rounds toward zero)
     * Numbers.sqrt(10L, RoundingMode.UP);                     // returns 4L    (rounds away from zero)
     * Numbers.sqrt(10L, RoundingMode.FLOOR);                  // returns 3L    (rounds toward negative infinity)
     * Numbers.sqrt(10L, RoundingMode.CEILING);                // returns 4L    (rounds toward positive infinity)
     * Numbers.sqrt(10L, RoundingMode.HALF_UP);                // returns 3L    (rounds to nearest, ties away from zero)
     * Numbers.sqrt(100000000000L, RoundingMode.DOWN);         // returns 316227L
     * Numbers.sqrt(100000000000L, RoundingMode.UP);           // returns 316228L
     * Numbers.sqrt(10000000000L, RoundingMode.UNNECESSARY);   // returns 100000L  (exact square root)
     *
     * // Edge cases
     * Numbers.sqrt(0L, RoundingMode.DOWN);                    // returns 0L
     * Numbers.sqrt(10L, RoundingMode.UNNECESSARY);            // throws ArithmeticException (not a perfect square)
     * Numbers.sqrt(-1L, RoundingMode.DOWN);                   // throws IllegalArgumentException (x must be non-negative)
     * }</pre>
     *
     * @param x the value to compute the square root of; must be non-negative
     * @param mode the rounding mode to apply
     * @return the integer square root of {@code x}, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x < 0}, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code x} is not a perfect square
     * @see #sqrt(int, RoundingMode)
     * @see #sqrt(BigInteger, RoundingMode)
     * @see RoundingMode
     */
    public static long sqrt(final long x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkNonNegative("x", x);

        if (fitsInInt(x)) {
            return sqrt((int) x, mode);
        }
        /*
         * Let k be the {@code true} value of floor(sqrt(x)), so that
         *
         *            k * k <= x          <  (k + 1) * (k + 1)
         * (double) (k * k) <= (double) x <= (double) ((k + 1) * (k + 1))
         *          since casting to double is nondecreasing.
         *          Note that the right-hand inequality is no longer strict.
         * Math.sqrt(k * k) <= Math.sqrt(x) <= Math.sqrt((k + 1) * (k + 1))
         *          since Math.sqrt is monotonic.
         * (long) Math.sqrt(k * k) <= (long) Math.sqrt(x) <= (long) Math.sqrt((k + 1) * (k + 1))
         *          since casting to long is monotonic
         * k <= (long) Math.sqrt(x)              = k + 1
         *          since (long) Math.sqrt(k * k)   == k, as checked exhaustively in
         *          Guava's LongMathTest testSqrtOfPerfectSquareAsDoubleIsPerfect
         */
        final long guess = (long) Math.sqrt(x);
        // Note: guess is always <= FLOOR_SQRT_MAX_LONG.
        final long guessSquared = guess * guess;
        // Note (2013-2-26): benchmarks indicate that, inscrutably enough, using if statements are faster here than using lessThanBranchFree.
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(guessSquared == x);
                return guess;
            case FLOOR:
            case DOWN:
                if (x < guessSquared) {
                    return guess - 1;
                }
                return guess;
            case CEILING:
            case UP:
                if (x > guessSquared) {
                    return guess + 1;
                }
                return guess;
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                final long sqrtFloor = guess - ((x < guessSquared) ? 1 : 0);
                final long halfSquare = sqrtFloor * sqrtFloor + sqrtFloor;
                /*
                 * We wish to test whether x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both x
                 * and halfSquare are integers, this is equivalent to testing whether x <=
                 * halfSquare. (We have to deal with overflow, though.)
                 *
                 * If we treat halfSquare as an unsigned long, we know that
                 *            sqrtFloor^2 <= x < (sqrtFloor + 1)^2
                 * halfSquare - sqrtFloor <= x < halfSquare + sqrtFloor + 1
                 * so |x - halfSquare| <= sqrtFloor.  Therefore, it's safe to treat x - halfSquare as a
                 * signed long, so lessThanBranchFree is safe for use.
                 */
                return sqrtFloor + lessThanBranchFree(halfSquare, x);
            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * <p>This method computes the integer square root of a non-negative BigInteger value,
     * applying the specified rounding mode to handle non-perfect squares. This method supports
     * arbitrary-precision arithmetic for very large values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger nine = BigInteger.valueOf(9);
     * BigInteger ten = BigInteger.valueOf(10);
     * Numbers.sqrt(nine, RoundingMode.DOWN);     // returns 3      (perfect square)
     * Numbers.sqrt(ten, RoundingMode.DOWN);      // returns 3      (rounds toward zero)
     * Numbers.sqrt(ten, RoundingMode.UP);        // returns 4      (rounds away from zero)
     * Numbers.sqrt(ten, RoundingMode.FLOOR);     // returns 3      (rounds toward negative infinity)
     * Numbers.sqrt(ten, RoundingMode.CEILING);   // returns 4      (rounds toward positive infinity)
     * Numbers.sqrt(ten, RoundingMode.HALF_UP);   // returns 3      (rounds to nearest, ties away from zero)
     *
     * // Large value example
     * BigInteger large = new BigInteger("123456789012345678901234567890");
     * Numbers.sqrt(large, RoundingMode.DOWN);                  // returns 351364182882014
     *
     * // Edge cases
     * Numbers.sqrt(BigInteger.ZERO, RoundingMode.DOWN);        // returns 0
     * Numbers.sqrt(ten, RoundingMode.UNNECESSARY);             // throws ArithmeticException (not a perfect square)
     * Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.DOWN); // throws IllegalArgumentException (x must be non-negative)
     * Numbers.sqrt((BigInteger) null, RoundingMode.DOWN);      // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to compute the square root of; must be {@code non-null} and non-negative
     * @param mode the rounding mode to apply
     * @return the integer square root of {@code x}, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x} is {@code null} or negative, or if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code x} is not a perfect square
     * @see RoundingMode
     * @see #sqrt(int, RoundingMode)
     * @see #sqrt(long, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    public static BigInteger sqrt(final BigInteger x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        checkNonNegative("x", N.checkArgNotNull(x, cs.x));
        if (fitsInLong(x)) {
            return BigInteger.valueOf(sqrt(x.longValue(), mode));
        }
        final BigInteger sqrtFloor = x.sqrt();
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(sqrtFloor.pow(2).equals(x)); // fall through
            case FLOOR:
            case DOWN:
                return sqrtFloor;
            case CEILING:
            case UP:
                final int sqrtFloorInt = sqrtFloor.intValue();
                final boolean sqrtFloorIsExact = (sqrtFloorInt * sqrtFloorInt == x.intValue()) // fast check mod 2^32
                        && sqrtFloor.pow(2).equals(x); // slow exact check
                return sqrtFloorIsExact ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                final BigInteger halfSquare = sqrtFloor.pow(2).add(sqrtFloor);
                /*
                 * We wish to test whether x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both x
                 * and halfSquare are integers, this is equivalent to testing whether x <=
                 * halfSquare.
                 */
                return (halfSquare.compareTo(x) >= 0) ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
     *
     * <p>This method provides precise control over rounding behavior for integer division, supporting
     * all standard rounding modes defined in {@link RoundingMode}.
     *
     * <p><b>Guava difference:</b> {@code Integer.MIN_VALUE / -1} throws {@code ArithmeticException} under
     * every rounding mode (the quotient 2^31 overflows an int). Guava {@code IntMath.divide} silently
     * wraps to {@code Integer.MIN_VALUE} in that case. The throw is deliberate: a wrapped quotient is
     * never the correctly rounded result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.divide(7, 3, RoundingMode.DOWN);          // returns 2   (rounds toward zero)
     * Numbers.divide(7, 3, RoundingMode.UP);            // returns 3   (rounds away from zero)
     * Numbers.divide(7, 3, RoundingMode.FLOOR);         // returns 2   (rounds toward negative infinity)
     * Numbers.divide(-7, 3, RoundingMode.FLOOR);        // returns -3  (rounds toward negative infinity)
     * Numbers.divide(7, 3, RoundingMode.CEILING);       // returns 3   (rounds toward positive infinity)
     * Numbers.divide(7, 2, RoundingMode.HALF_UP);       // returns 4   (rounds to nearest, ties away from zero)
     * Numbers.divide(9, 2, RoundingMode.HALF_EVEN);     // returns 4   (rounds to nearest, ties to even)
     * Numbers.divide(9, 3, RoundingMode.UNNECESSARY);   // returns 3   (exact division required)
     *
     * // Edge cases
     * Numbers.divide(7, 3, RoundingMode.UNNECESSARY);            // throws ArithmeticException (not an exact multiple)
     * Numbers.divide(1, 0, RoundingMode.DOWN);                   // throws ArithmeticException (/ by zero)
     * Numbers.divide(Integer.MIN_VALUE, -1, RoundingMode.DOWN);  // throws ArithmeticException (quotient 2^31 overflows an int)
     * }</pre>
     *
     * @param p the dividend
     * @param q the divisor
     * @param mode the rounding mode to apply
     * @return the result of {@code p / q} rounded according to the specified mode
     * @throws IllegalArgumentException if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code q == 0}; if {@code p == Integer.MIN_VALUE && q == -1}
     *         (the quotient 2^31 overflows an int); or if {@code mode == UNNECESSARY} and {@code p}
     *         is not an integer multiple of {@code q}
     * @see RoundingMode
     * @see #divide(long, long, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int divide(final int p, final int q, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        if (q == 0) {
            throw new ArithmeticException("/ by zero");
        }
        // Integer.MIN_VALUE / -1 silently overflows back to Integer.MIN_VALUE in Java.
        if (p == Integer.MIN_VALUE && q == -1) {
            throw new ArithmeticException("integer overflow");
        }
        final int div = p / q;
        final int rem = p - q * div; // equal to p % q

        if (rem == 0) {
            return div;
        }

        /*
         * Normal Java division rounds towards 0, consistently with RoundingMode.DOWN. We just have to
         * deal with the cases where rounding towards 0 is wrong, which typically depends on the sign of
         * p / q.
         *
         * signum is 1 if p and q are both nonnegative or both negative, and -1 otherwise.
         */
        final int signum = 1 | ((p ^ q) >> (Integer.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                //noinspection ConstantValue,DataFlowIssue
                checkRoundingUnnecessary(rem == 0);
                //$FALL-THROUGH$
            case DOWN:
                increment = false;
                break;
            case UP:
                increment = true;
                break;
            case CEILING:
                increment = signum > 0;
                break;
            case FLOOR:
                increment = signum < 0;
                break;
            case HALF_EVEN:
            case HALF_DOWN:
            case HALF_UP:
                final int absRem = abs(rem);
                final int cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative ints can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP || (mode == HALF_EVEN && (div & 1) != 0));
                } else {
                    increment = cmpRemToHalfDivisor > 0; // closer to the UP value
                }
                break;
            default:
                throw new AssertionError();
        }
        return increment ? div + signum : div;
    }

    /**
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
     *
     * <p>This method provides precise control over rounding behavior for long integer division, supporting
     * all standard rounding modes defined in {@link RoundingMode}.
     *
     * <p><b>Guava difference:</b> {@code Long.MIN_VALUE / -1} throws {@code ArithmeticException} under
     * every rounding mode (the quotient 2^63 overflows a long). Guava {@code LongMath.divide} silently
     * wraps to {@code Long.MIN_VALUE} in that case. The throw is deliberate: a wrapped quotient is
     * never the correctly rounded result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.divide(7L, 3L, RoundingMode.DOWN);          // returns 2L   (rounds toward zero)
     * Numbers.divide(7L, 3L, RoundingMode.UP);            // returns 3L   (rounds away from zero)
     * Numbers.divide(7L, 3L, RoundingMode.FLOOR);         // returns 2L   (rounds toward negative infinity)
     * Numbers.divide(-7L, 3L, RoundingMode.FLOOR);        // returns -3L  (rounds toward negative infinity)
     * Numbers.divide(7L, 3L, RoundingMode.CEILING);       // returns 3L   (rounds toward positive infinity)
     * Numbers.divide(7L, 2L, RoundingMode.HALF_UP);       // returns 4L   (rounds to nearest, ties away from zero)
     * Numbers.divide(9L, 2L, RoundingMode.HALF_EVEN);     // returns 4L   (rounds to nearest, ties to even)
     * Numbers.divide(9L, 3L, RoundingMode.UNNECESSARY);   // returns 3L   (exact division required)
     *
     * // Edge cases
     * Numbers.divide(7L, 3L, RoundingMode.UNNECESSARY);          // throws ArithmeticException (not an exact multiple)
     * Numbers.divide(1L, 0L, RoundingMode.DOWN);                 // throws ArithmeticException (/ by zero)
     * Numbers.divide(Long.MIN_VALUE, -1L, RoundingMode.DOWN);    // throws ArithmeticException (quotient 2^63 overflows a long)
     * }</pre>
     *
     * @param p the dividend
     * @param q the divisor
     * @param mode the rounding mode to apply
     * @return the result of {@code p / q} rounded according to the specified mode
     * @throws IllegalArgumentException if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code q == 0}; if {@code p == Long.MIN_VALUE && q == -1}
     *         (the quotient 2^63 overflows a long); or if {@code mode == UNNECESSARY} and {@code p}
     *         is not an integer multiple of {@code q}
     * @see RoundingMode
     * @see #divide(int, int, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static long divide(final long p, final long q, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        // Stated explicitly rather than left to `p / q` below, so this overload reads the same as its int
        // sibling. The message is identical either way -- the JVM raises ArithmeticException("/ by zero")
        // for long division too -- so this is a readability change, not a behavioural one.
        if (q == 0) {
            throw new ArithmeticException("/ by zero");
        }
        // Long.MIN_VALUE / -1 silently overflows back to Long.MIN_VALUE in Java.
        if (p == Long.MIN_VALUE && q == -1L) {
            throw new ArithmeticException("long overflow");
        }
        final long div = p / q;
        final long rem = p - q * div; // equals p % q

        if (rem == 0) {
            return div;
        }

        /*
         * Normal Java division rounds towards 0, consistently with RoundingMode.DOWN. We just have to
         * deal with the cases where rounding towards 0 is wrong, which typically depends on the sign of
         * p / q.
         *
         * signum is 1 if p and q are both nonnegative or both negative, and -1 otherwise.
         */
        final int signum = 1 | (int) ((p ^ q) >> (Long.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                //noinspection ConstantValue,DataFlowIssue
                checkRoundingUnnecessary(rem == 0);
                //$FALL-THROUGH$
            case DOWN:
                increment = false;
                break;
            case UP:
                increment = true;
                break;
            case CEILING:
                increment = signum > 0;
                break;
            case FLOOR:
                increment = signum < 0;
                break;
            case HALF_EVEN:
            case HALF_DOWN:
            case HALF_UP:
                final long absRem = abs(rem);
                final long cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative longs can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP || (mode == HALF_EVEN && (div & 1) != 0)); //NOSONAR
                } else {
                    increment = cmpRemToHalfDivisor > 0; // closer to the UP value
                }
                break;
            default:
                throw new AssertionError();
        }
        return increment ? div + signum : div;
    }

    /**
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
     *
     * <p>This method provides precise control over rounding behavior for arbitrary-precision integer division,
     * supporting all standard rounding modes defined in {@link RoundingMode}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BigInteger seven = BigInteger.valueOf(7);
     * BigInteger three = BigInteger.valueOf(3);
     * Numbers.divide(seven, three, RoundingMode.DOWN);                          // returns 2   (rounds toward zero)
     * Numbers.divide(seven, three, RoundingMode.UP);                            // returns 3   (rounds away from zero)
     * Numbers.divide(seven, three, RoundingMode.FLOOR);                         // returns 2   (rounds toward negative infinity)
     * Numbers.divide(seven, three, RoundingMode.CEILING);                       // returns 3   (rounds toward positive infinity)
     * Numbers.divide(seven, BigInteger.valueOf(2), RoundingMode.HALF_UP);       // returns 4   (rounds to nearest)
     * Numbers.divide(BigInteger.valueOf(9), three, RoundingMode.UNNECESSARY);   // returns 3   (exact division)
     *
     * // Edge cases
     * Numbers.divide(seven, three, RoundingMode.UNNECESSARY);      // throws ArithmeticException (not an exact multiple)
     * Numbers.divide(seven, BigInteger.ZERO, RoundingMode.DOWN);   // throws ArithmeticException (/ by zero)
     * Numbers.divide(null, three, RoundingMode.DOWN);              // throws IllegalArgumentException
     * }</pre>
     *
     * @param p the dividend
     * @param q the divisor
     * @param mode the rounding mode to apply
     * @return the result of {@code p / q} rounded according to the specified mode as a BigInteger
     * @throws IllegalArgumentException if {@code p}, {@code q}, or {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code q} is zero, or if {@code mode == UNNECESSARY} and {@code p}
     *         is not an integer multiple of {@code q}
     * @see RoundingMode
     * @see #divide(int, int, RoundingMode)
     * @see #divide(long, long, RoundingMode)
     */
    public static BigInteger divide(final BigInteger p, final BigInteger q, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);
        N.checkArgNotNull(p, cs.p);
        N.checkArgNotNull(q, cs.q);

        // Deliberate: scale-0 BigDecimal division, the same implementation as Guava's BigIntegerMath.divide.
        // Hand-rolling this over p.divideAndRemainder(q) -- as the primitive divide(int/long) overloads above
        // do -- looks leaner but measures slower: new BigDecimal(BigInteger) is a wrapper with no magnitude
        // copy, and scale-0 divide bottoms out in the same BigInteger division, while the manual form adds a
        // BigInteger[], abs(), shiftLeft(1), compareTo and valueOf()+add(). Measured ~2x slower for 32-bit
        // operands and no better until roughly 16k-bit ones. Do not "optimize" this into the manual form.
        final BigDecimal pDec = new BigDecimal(p);
        final BigDecimal qDec = new BigDecimal(q);
        return pDec.divide(qDec, 0, mode).toBigIntegerExact();
    }

    /**
     * Returns {@code x mod m}, a non-negative value less than {@code m}.
     * This differs from {@code x % m}, which might be negative.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.mod(7, 4);    // returns 3
     * Numbers.mod(-7, 4);   // returns 1  (unlike -7 % 4, which is -3)
     * Numbers.mod(-1, 4);   // returns 3
     * Numbers.mod(-8, 4);   // returns 0
     * Numbers.mod(8, 4);    // returns 0
     *
     * // Edge cases
     * Numbers.mod(7, 1);    // returns 0
     * Numbers.mod(7, 0);    // throws ArithmeticException (modulus must be > 0)
     * Numbers.mod(7, -4);   // throws ArithmeticException (modulus must be > 0)
     * }</pre>
     *
     * @param x the dividend
     * @param m the modulus, must be positive
     * @return x mod m, a non-negative value less than m
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.17.3">Remainder Operator</a>
     */
    public static int mod(final int x, final int m) throws ArithmeticException {
        if (m <= 0) {
            throw new ArithmeticException("Modulus " + m + " must be > 0");
        }
        final int result = x % m;
        return (result >= 0) ? result : result + m;
    }

    /**
     * Returns {@code x mod m}, a non-negative value less than {@code m}. This differs from
     * {@code x % m}, which might be negative.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.mod(7L, 4);    // returns 3
     * Numbers.mod(-7L, 4);   // returns 1  (unlike -7L % 4, which is -3)
     * Numbers.mod(-1L, 4);   // returns 3
     * Numbers.mod(-8L, 4);   // returns 0
     * Numbers.mod(8L, 4);    // returns 0
     *
     * // Edge cases
     * Numbers.mod(7L, 1);    // returns 0
     * Numbers.mod(7L, 0);    // throws ArithmeticException (modulus must be > 0)
     * Numbers.mod(7L, -4);   // throws ArithmeticException (modulus must be > 0)
     * }</pre>
     *
     * @param x the dividend
     * @param m the modulus, must be positive
     * @return x mod m, a non-negative value less than m
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.17.3">Remainder Operator</a>
     */
    public static int mod(final long x, final int m) throws ArithmeticException {
        // Cast is safe because the result is guaranteed in the range [0, m)
        return (int) mod(x, (long) m);
    }

    /**
     * Returns {@code x mod m}, a non-negative value less than {@code m}. This differs from
     * {@code x % m}, which might be negative.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.mod(7L, 4L);    // returns 3L
     * Numbers.mod(-7L, 4L);   // returns 1L  (unlike -7L % 4L, which is -3L)
     * Numbers.mod(-1L, 4L);   // returns 3L
     * Numbers.mod(-8L, 4L);   // returns 0L
     * Numbers.mod(8L, 4L);    // returns 0L
     *
     * // Edge cases
     * Numbers.mod(7L, 1L);    // returns 0L
     * Numbers.mod(7L, 0L);    // throws ArithmeticException (modulus must be > 0)
     * Numbers.mod(7L, -4L);   // throws ArithmeticException (modulus must be > 0)
     * }</pre>
     *
     * @param x the dividend
     * @param m the modulus, must be positive
     * @return x mod m, a non-negative value less than m
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.17.3">Remainder Operator</a>
     */
    public static long mod(final long x, final long m) throws ArithmeticException {
        if (m <= 0) {
            throw new ArithmeticException("Modulus " + m + " must be > 0");
        }
        final long result = x % m;
        return (result >= 0) ? result : result + m;
    }

    /**
     * Returns the greatest common divisor (GCD) of two integers using the binary GCD algorithm.
     * The GCD is the largest positive integer that divides both numbers without a remainder.
     *
     * <p>This implementation uses the binary GCD algorithm (also known as Stein's algorithm). The method handles
     * negative numbers by taking their absolute values before computing the GCD.</p>
     *
     * <p><b>Guava difference:</b> this method accepts negatives ({@code gcd(-4, 6)} is {@code 2}).
     * Guava {@code IntMath.gcd} throws {@code IllegalArgumentException} if either argument is negative.
     * The extension is deliberate: a GCD is defined on absolute values. A port from Guava that relied
     * on that throw should add its own non-negativity check.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.gcd(12, 8);                            // returns 4
     * Numbers.gcd(17, 19);                           // returns 1  (coprime numbers)
     * Numbers.gcd(-12, 8);                           // returns 4  (Guava IntMath.gcd throws)
     *
     * // Edge cases
     * Numbers.gcd(0, 5);                                   // returns 5
     * Numbers.gcd(0, 0);                                   // returns 0
     * Numbers.gcd(Integer.MIN_VALUE, 6);                   // returns 2  (|MIN_VALUE| is halved internally)
     * Numbers.gcd(0, Integer.MIN_VALUE);                   // throws ArithmeticException (gcd would be 2^31)
     * Numbers.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);   // throws ArithmeticException (gcd would be 2^31)
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the greatest common divisor of {@code a} and {@code b}; returns {@code 0} if both are zero
     * @throws ArithmeticException if {@code (a == 0 && b == Integer.MIN_VALUE)}, {@code (b == 0 && a == Integer.MIN_VALUE)},
     *         or {@code (a == Integer.MIN_VALUE && b == Integer.MIN_VALUE)}, because the GCD would be 2^31 which
     *         cannot be represented as a positive int
     * @see #gcd(long, long)
     * @see #lcm(int, int)
     */
    public static int gcd(int a, int b) throws ArithmeticException {

        if ((a == 0 && b == Integer.MIN_VALUE) || (b == 0 && a == Integer.MIN_VALUE) || (a == Integer.MIN_VALUE && b == Integer.MIN_VALUE)) {
            throw new ArithmeticException("gcd would be 2^31, not representable as int");
        }

        if (a == Integer.MIN_VALUE) {
            a = Math.abs(a / 2);
        }

        if (b == Integer.MIN_VALUE) {
            b = Math.abs(b / 2);
        }

        a = abs(a);
        b = abs(b);

        if (a == 0) {
            // 0 % b == 0, so b divides a, but the converse doesn't hold.
            // BigInteger.gcd is consistent with this decision.
            return b;
        } else if (b == 0) {
            return a; // similar logic
        }
        // Uses the binary GCD algorithm; see https://en.wikipedia.org/wiki/Binary_GCD_algorithm.
        final int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        final int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd.  Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // https://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            final int delta = a - b; // can't overflow, since a and b are nonnegative

            final int minDeltaOrZero = delta & (delta >> (Integer.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Integer.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    /**
     * Returns the greatest common divisor (GCD) of two long integers using the binary GCD algorithm.
     * The GCD is the largest positive integer that divides both numbers without a remainder.
     *
     * <p>This implementation uses the binary GCD algorithm (also known as Stein's algorithm). The method handles
     * negative numbers by taking their absolute values before computing the GCD.</p>
     *
     * <p><b>Guava difference:</b> this method accepts negatives ({@code gcd(-4L, 6L)} is {@code 2L}).
     * Guava {@code LongMath.gcd} throws {@code IllegalArgumentException} if either argument is negative.
     * The extension is deliberate: a GCD is defined on absolute values. A port from Guava that relied
     * on that throw should add its own non-negativity check.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.gcd(12L, 8L);                         // returns 4L
     * Numbers.gcd(17L, 19L);                        // returns 1L  (coprime numbers)
     * Numbers.gcd(-12L, 8L);                        // returns 4L  (Guava LongMath.gcd throws)
     * Numbers.gcd(1000000000000L, 500000000000L);   // returns 500000000000L
     *
     * // Edge cases
     * Numbers.gcd(0L, 5L);                          // returns 5L
     * Numbers.gcd(0L, 0L);                          // returns 0L
     * Numbers.gcd(Long.MIN_VALUE, 6L);              // returns 2L  (|MIN_VALUE| is halved internally)
     * Numbers.gcd(0L, Long.MIN_VALUE);              // throws ArithmeticException (gcd would be 2^63)
     * }</pre>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the greatest common divisor of {@code a} and {@code b}; returns {@code 0} if both are zero
     * @throws ArithmeticException if {@code (a == 0 && b == Long.MIN_VALUE)}, {@code (b == 0 && a == Long.MIN_VALUE)},
     *         or {@code (a == Long.MIN_VALUE && b == Long.MIN_VALUE)}, because the GCD would be 2^63 which
     *         cannot be represented as a positive long
     * @see #gcd(int, int)
     * @see #lcm(long, long)
     */
    public static long gcd(long a, long b) throws ArithmeticException {

        if ((a == 0 && b == Long.MIN_VALUE) || (b == 0 && a == Long.MIN_VALUE) || (a == Long.MIN_VALUE && b == Long.MIN_VALUE)) {
            throw new ArithmeticException("gcd would be 2^63, not representable as long");
        }

        if (a == Long.MIN_VALUE) {
            a = Math.abs(a / 2);
        }

        if (b == Long.MIN_VALUE) {
            b = Math.abs(b / 2);
        }

        a = abs(a);
        b = abs(b);

        if (a == 0) {
            // 0 % b == 0, so b divides a, but the converse doesn't hold.
            // BigInteger.gcd is consistent with this decision.
            return b;
        } else if (b == 0) {
            return a; // similar logic
        }
        // Uses the binary GCD algorithm; see https://en.wikipedia.org/wiki/Binary_GCD_algorithm.
        final int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        final int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd. Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // https://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            final long delta = a - b; // can't overflow, since a and b are nonnegative

            final long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Long.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    /**
     * Returns the least common multiple (LCM) of two integers.
     * The LCM is the smallest positive integer that is divisible by both numbers.
     *
     * <p>This method uses the formula {@code Numbers.lcm(a,b) = (a / Numbers.gcd(a,b)) * b}, computing
     * the LCM from the absolute values of the inputs. The method handles negative numbers
     * by taking their absolute values before computing the LCM.</p>
     *
     * <p>Negative inputs are accepted ({@code lcm(-4, 6)} is {@code 12}), and the result is
     * sign-independent, consistently with {@link #gcd(int, int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.lcm(4, 6);                    // returns 12
     * Numbers.lcm(3, 7);                    // returns 21  (coprime numbers: lcm equals product)
     * Numbers.lcm(12, 18);                  // returns 36
     * Numbers.lcm(-4, 6);                   // returns 12  (sign-independent)
     *
     * // Edge cases
     * Numbers.lcm(65536, 65536 * 2);        // returns 131072
     * Numbers.lcm(0, 5);                    // returns 0
     * Numbers.lcm(0, 0);                    // returns 0
     * Numbers.lcm(Integer.MIN_VALUE, 2);    // throws ArithmeticException (the true lcm is 2^31)
     * Numbers.lcm(46341, 46341 * 2 - 1);    // throws ArithmeticException (the product exceeds int range)
     * }</pre>
     *
     * <p>Special cases:
     * <ul>
     * <li>The invocations {@code Numbers.lcm(Integer.MIN_VALUE, n)} and {@code Numbers.lcm(n, Integer.MIN_VALUE)} throw
     * an {@code ArithmeticException} for every non-zero {@code n}, because once an operand is {@code -2^31} the true LCM
     * is at least 2^31, which is too large for an int value (the smallest such case is when {@code Math.abs(n)} is a
     * power of 2, where the result would be exactly 2^31).</li>
     * <li>The result of {@code Numbers.lcm(0, x)} and {@code Numbers.lcm(x, 0)} is {@code 0} for any {@code x}.</li>
     * </ul>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the least common multiple of the absolute values of {@code a} and {@code b};
     *         returns {@code 0} if either is zero
     * @throws ArithmeticException if the result cannot be represented as a non-negative {@code int} value,
     *         or if the computation would overflow
     * @see #lcm(long, long)
     * @see #gcd(int, int)
     */
    public static int lcm(final int a, final int b) throws ArithmeticException {
        if (a == 0 || b == 0) {
            return 0;
        }

        try {
            final int divisor = gcd(a, b);
            // Compute in long so the product cannot overflow (|a / gcd| and |b| are each <= 2^31), then range-check once.
            final long lcm = abs((long) (a / divisor) * b);

            if (lcm <= Integer.MAX_VALUE) {
                return (int) lcm;
            }
        } catch (final ArithmeticException e) {
            // gcd itself is not representable for (MIN_VALUE, MIN_VALUE) -> use the lcm-specific error below.
        }

        throw new ArithmeticException("overflow: lcm(" + a + ", " + b + ") is not representable as a non-negative int");
    }

    /**
     * Returns the least common multiple (LCM) of two long integers.
     * The LCM is the smallest positive integer that is divisible by both numbers.
     *
     * <p>This method uses the formula {@code Numbers.lcm(a,b) = (a / Numbers.gcd(a,b)) * b}, computing
     * the LCM from the absolute values of the inputs. The method handles negative numbers
     * by taking their absolute values before computing the LCM.</p>
     *
     * <p>Negative inputs are accepted ({@code lcm(-4L, 6L)} is {@code 12L}), and the result is
     * sign-independent, consistently with {@link #gcd(long, long)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.lcm(4L, 6L);                         // returns 12L
     * Numbers.lcm(3L, 7L);                         // returns 21L  (coprime numbers: lcm equals product)
     * Numbers.lcm(12L, 18L);                       // returns 36L
     * Numbers.lcm(-4L, 6L);                        // returns 12L  (sign-independent)
     * Numbers.lcm(100000000000L, 150000000000L);   // returns 300000000000L
     *
     * // Edge cases
     * Numbers.lcm(0L, 5L);                         // returns 0L
     * Numbers.lcm(0L, 0L);                         // returns 0L
     * Numbers.lcm(Long.MIN_VALUE, 2L);             // throws ArithmeticException (the true lcm is 2^63)
     * }</pre>
     *
     * <p>Special cases:
     * <ul>
     * <li>The invocations {@code Numbers.lcm(Long.MIN_VALUE, n)} and {@code Numbers.lcm(n, Long.MIN_VALUE)} throw an
     * {@code ArithmeticException} for every non-zero {@code n}, because once an operand is {@code -2^63} the true LCM is
     * at least 2^63, which is too large for a long value (the smallest such case is when {@code Math.abs(n)} is a power
     * of 2, where the result would be exactly 2^63).</li>
     * <li>The result of {@code Numbers.lcm(0L, x)} and {@code Numbers.lcm(x, 0L)} is {@code 0L} for any {@code x}.</li>
     * </ul>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the least common multiple of the absolute values of {@code a} and {@code b};
     *         returns {@code 0} if either is zero
     * @throws ArithmeticException if the result cannot be represented as a non-negative {@code long} value,
     *         or if the computation would overflow
     * @see #lcm(int, int)
     * @see #gcd(long, long)
     */
    public static long lcm(final long a, final long b) throws ArithmeticException {
        if (a == 0 || b == 0) {
            return 0;
        }

        try {
            final long quotient = a / gcd(a, b);
            final long lcm = abs(multiplyExact(quotient, b));

            if (lcm != Long.MIN_VALUE) { // MIN_VALUE means the true lcm is 2^63, which is not a non-negative long
                return lcm;
            }
        } catch (final ArithmeticException e) {
            // the product overflowed long -> fall through to the single descriptive throw below
        }

        throw new ArithmeticException("overflow: lcm(" + a + ", " + b + ") is not representable as a non-negative long");
    }

    /**
     * Returns the greatest common divisor (GCD) of two {@code BigInteger} values.
     *
     * <p>This convenience wrapper around {@link BigInteger#gcd(BigInteger)} completes the
     * {@code int}/{@code long}/{@code BigInteger} family. As with the primitive overloads the result is
     * non-negative, the computation is sign-independent, and {@code gcd(0, 0)} returns {@code 0}. Unlike the
     * primitive overloads there is no overflow case. This matches {@code BigInteger.gcd} and the primitive
     * {@link #gcd(int, int)}/{@link #gcd(long, long)} overloads (negatives accepted); Guava
     * {@code IntMath}/{@code LongMath.gcd} reject negatives.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.gcd(BigInteger.valueOf(12), BigInteger.valueOf(8));    // returns 4
     * Numbers.gcd(BigInteger.valueOf(-12), BigInteger.valueOf(8));   // returns 4  (sign-independent)
     *
     * // Edge cases: unlike the primitive overloads there is no overflow case
     * Numbers.gcd(BigInteger.ZERO, BigInteger.TEN);                  // returns 10
     * Numbers.gcd(BigInteger.ZERO, BigInteger.ZERO);                 // returns 0
     * Numbers.gcd((BigInteger) null, BigInteger.TEN);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param a the first value; must not be {@code null}
     * @param b the second value; must not be {@code null}
     * @return the greatest common divisor of the absolute values of {@code a} and {@code b}
     * @throws IllegalArgumentException if {@code a} or {@code b} is {@code null}.
     * @see #gcd(int, int)
     * @see #gcd(long, long)
     * @see #lcm(BigInteger, BigInteger)
     * @see BigInteger#gcd(BigInteger)
     */
    public static BigInteger gcd(final BigInteger a, final BigInteger b) throws IllegalArgumentException {
        N.checkArgNotNull(a, cs.a);
        N.checkArgNotNull(b, cs.b);

        return a.gcd(b);
    }

    /**
     * Returns the least common multiple (LCM) of two {@code BigInteger} values.
     *
     * <p>This wrapper completes the {@code int}/{@code long}/{@code BigInteger} family (there is no
     * {@code BigInteger.lcm}). It is computed as {@code |a / gcd(a, b) * b|}. As with the primitive overloads
     * the result is non-negative and {@code lcm(0, x) == lcm(x, 0) == 0}. Unlike the primitive overloads there
     * is no primitive range limit. Negatives are accepted, consistently with {@link #lcm(int, int)}
     * and {@link #lcm(long, long)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.lcm(BigInteger.valueOf(4), BigInteger.valueOf(6));     // returns 12
     * Numbers.lcm(BigInteger.valueOf(12), BigInteger.valueOf(18));   // returns 36
     * Numbers.lcm(BigInteger.valueOf(-4), BigInteger.valueOf(6));    // returns 12  (sign-independent)
     *
     * // Edge cases: unlike the primitive overloads there is no overflow case
     * Numbers.lcm(BigInteger.ZERO, BigInteger.TEN);                  // returns 0
     * Numbers.lcm((BigInteger) null, BigInteger.TEN);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param a the first value; must not be {@code null}
     * @param b the second value; must not be {@code null}
     * @return the least common multiple of the absolute values of {@code a} and {@code b};
     *         returns {@code 0} if either is zero
     * @throws IllegalArgumentException if {@code a} or {@code b} is {@code null}.
     * @see #lcm(int, int)
     * @see #lcm(long, long)
     * @see #gcd(BigInteger, BigInteger)
     */
    public static BigInteger lcm(final BigInteger a, final BigInteger b) throws IllegalArgumentException {
        N.checkArgNotNull(a, cs.a);
        N.checkArgNotNull(b, cs.b);

        if (a.signum() == 0 || b.signum() == 0) {
            return BigInteger.ZERO;
        }

        return a.divide(a.gcd(b)).multiply(b).abs();
    }

    /**
     * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs addition with overflow checking. If the result would exceed the range
     * of int values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.addExact(100, 200);                // returns 300
     * Numbers.addExact(-5, 3);                   // returns -2
     * Numbers.addExact(Integer.MAX_VALUE, 0);    // returns Integer.MAX_VALUE (the bound itself is representable)
     *
     * // Edge cases: overflow throws instead of wrapping
     * Numbers.addExact(Integer.MAX_VALUE, 1);    // throws ArithmeticException
     * Numbers.addExact(Integer.MIN_VALUE, -1);   // throws ArithmeticException
     * }</pre>
     *
     * @param a the first int value to add
     * @param b the second int value to add
     * @return the sum of {@code a} and {@code b}
     * @throws ArithmeticException if {@code a + b} overflows in signed {@code int} arithmetic
     * @see Math#addExact(int, int)
     * @see #addExact(long, long)
     * @see #saturatedAdd(int, int)
     */
    public static int addExact(final int a, final int b) throws ArithmeticException {
        return Math.addExact(a, b);
    }

    /**
     * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs addition with overflow checking. If the result would exceed the range
     * of long values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.addExact(100L, 200L);            // returns 300L
     * Numbers.addExact(-5L, 3L);               // returns -2L
     * Numbers.addExact(Long.MAX_VALUE, 0L);    // returns Long.MAX_VALUE (the bound itself is representable)
     *
     * // Edge cases: overflow throws instead of wrapping
     * Numbers.addExact(Long.MAX_VALUE, 1L);    // throws ArithmeticException
     * Numbers.addExact(Long.MIN_VALUE, -1L);   // throws ArithmeticException
     * }</pre>
     *
     * @param a the first long value to add
     * @param b the second long value to add
     * @return the sum of {@code a} and {@code b}
     * @throws ArithmeticException if {@code a + b} overflows in signed {@code long} arithmetic
     * @see Math#addExact(long, long)
     * @see #addExact(int, int)
     * @see #saturatedAdd(long, long)
     */
    public static long addExact(final long a, final long b) throws ArithmeticException {
        return Math.addExact(a, b);
    }

    /**
     * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs subtraction with overflow checking. If the result would exceed the range
     * of int values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.subtractExact(200, 100);                // returns 100
     * Numbers.subtractExact(-5, 3);                   // returns -8
     * Numbers.subtractExact(Integer.MIN_VALUE, 0);    // returns Integer.MIN_VALUE (the bound itself is representable)
     *
     * // Edge cases: overflow throws instead of wrapping
     * Numbers.subtractExact(Integer.MIN_VALUE, 1);    // throws ArithmeticException
     * Numbers.subtractExact(Integer.MAX_VALUE, -1);   // throws ArithmeticException
     * }</pre>
     *
     * @param a the value to subtract from
     * @param b the value to subtract
     * @return the difference of {@code a} and {@code b}
     * @throws ArithmeticException if {@code a - b} overflows in signed {@code int} arithmetic
     * @see Math#subtractExact(int, int)
     * @see #subtractExact(long, long)
     * @see #saturatedSubtract(int, int)
     */
    public static int subtractExact(final int a, final int b) throws ArithmeticException {
        return Math.subtractExact(a, b);
    }

    /**
     * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs subtraction with overflow checking. If the result would exceed the range
     * of long values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.subtractExact(200L, 100L);            // returns 100L
     * Numbers.subtractExact(-5L, 3L);               // returns -8L
     * Numbers.subtractExact(Long.MIN_VALUE, 0L);    // returns Long.MIN_VALUE (the bound itself is representable)
     *
     * // Edge cases: overflow throws instead of wrapping
     * Numbers.subtractExact(Long.MIN_VALUE, 1L);    // throws ArithmeticException
     * Numbers.subtractExact(Long.MAX_VALUE, -1L);   // throws ArithmeticException
     * }</pre>
     *
     * @param a the value to subtract from
     * @param b the value to subtract
     * @return the difference of {@code a} and {@code b}
     * @throws ArithmeticException if {@code a - b} overflows in signed {@code long} arithmetic
     * @see Math#subtractExact(long, long)
     * @see #subtractExact(int, int)
     * @see #saturatedSubtract(long, long)
     */
    public static long subtractExact(final long a, final long b) throws ArithmeticException {
        return Math.subtractExact(a, b);
    }

    /**
     * Returns the product of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs multiplication with overflow checking. If the result would exceed the range
     * of int values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.multiplyExact(100, 200);               // returns 20000
     * Numbers.multiplyExact(-100, 200);              // returns -20000
     * Numbers.multiplyExact(0, Integer.MAX_VALUE);   // returns 0
     *
     * // Edge cases: overflow throws instead of wrapping
     * Numbers.multiplyExact(Integer.MAX_VALUE, 2);   // throws ArithmeticException
     * Numbers.multiplyExact(100000, 100000);         // throws ArithmeticException
     * }</pre>
     *
     * @param a the first int value to multiply
     * @param b the second int value to multiply
     * @return the product of {@code a} and {@code b}
     * @throws ArithmeticException if {@code a * b} overflows in signed {@code int} arithmetic
     * @see Math#multiplyExact(int, int)
     * @see #multiplyExact(long, long)
     * @see #saturatedMultiply(int, int)
     */
    public static int multiplyExact(final int a, final int b) throws ArithmeticException {
        return Math.multiplyExact(a, b);
    }

    /**
     * Returns the product of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs multiplication with overflow checking. If the result would exceed the range
     * of long values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.multiplyExact(100L, 200L);                   // returns 20000L
     * Numbers.multiplyExact(-100L, 200L);                  // returns -20000L
     * Numbers.multiplyExact(0L, Long.MAX_VALUE);           // returns 0L
     *
     * // Edge cases: overflow throws instead of wrapping
     * Numbers.multiplyExact(Long.MAX_VALUE, 2L);           // throws ArithmeticException
     * Numbers.multiplyExact(10000000000L, 10000000000L);   // throws ArithmeticException
     * }</pre>
     *
     * @param a the first long value to multiply
     * @param b the second long value to multiply
     * @return the product of {@code a} and {@code b}
     * @throws ArithmeticException if {@code a * b} overflows in signed {@code long} arithmetic
     * @see Math#multiplyExact(long, long)
     * @see #multiplyExact(int, int)
     * @see #saturatedMultiply(long, long)
     */
    public static long multiplyExact(final long a, final long b) throws ArithmeticException {
        return Math.multiplyExact(a, b);
    }

    /**
     * Returns {@code b} to the {@code k}th power, throwing an exception if overflow occurs.
     *
     * <p>This method computes integer exponentiation with overflow checking. It throws an
     * {@code ArithmeticException} if the result cannot be represented as an {@code int}.
     * Use {@link #saturatedPow(int, int)} to clamp at the integer bounds instead of throwing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.powExact(2, 3);     // returns 8
     * Numbers.powExact(3, 4);     // returns 81
     * Numbers.powExact(10, 9);    // returns 1000000000
     * Numbers.powExact(-2, 30);   // returns 1073741824
     *
     * // Edge cases
     * Numbers.powExact(5, 0);     // returns 1     (any base to the power 0)
     * Numbers.powExact(0, 0);     // returns 1     (0^0 is 1 by convention)
     * Numbers.powExact(-2, 31);   // returns Integer.MIN_VALUE   (exactly representable)
     * Numbers.powExact(10, 10);   // throws ArithmeticException (overflow)
     * Numbers.powExact(2, 31);    // throws ArithmeticException (overflow)
     * Numbers.powExact(-2, 32);   // throws ArithmeticException (overflow)
     * Numbers.powExact(2, -1);    // throws IllegalArgumentException (negative exponent)
     * }</pre>
     *
     * @param b the base integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power
     * @throws IllegalArgumentException if {@code k < 0}.
     * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed {@code int} arithmetic
     * @see #saturatedPow(int, int)
     * @see #powExact(long, int)
     */
    public static int powExact(int b, int k) throws IllegalArgumentException, ArithmeticException {
        checkNonNegative("exponent", k);
        final int origB = b;
        final int origK = k;
        switch (b) {
            case 0:
                return (k == 0) ? 1 : 0;
            case 1:
                return 1;
            case (-1):
                return ((k & 1) == 0) ? 1 : -1;
            case 2:
                checkNoOverflow(k < Integer.SIZE - 1, b, k);
                return 1 << k;
            case (-2):
                checkNoOverflow(k < Integer.SIZE, b, k);
                return ((k & 1) == 0) ? 1 << k : -1 << k;
            default:
                // continue below to handle the general case
        }
        int accum = 1;
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return multiplyExact(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = multiplyExact(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        checkNoOverflow(-FLOOR_SQRT_MAX_INT <= b && b <= FLOOR_SQRT_MAX_INT, origB, origK);
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns {@code b} to the {@code k}th power, throwing an exception if overflow occurs.
     *
     * <p>This method computes long integer exponentiation with overflow checking. It throws an
     * {@code ArithmeticException} if the result cannot be represented as a {@code long}.
     * Use {@link #saturatedPow(long, int)} to clamp at the long bounds instead of throwing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.powExact(2L, 3);     // returns 8L
     * Numbers.powExact(3L, 4);     // returns 81L
     * Numbers.powExact(10L, 18);   // returns 1000000000000000000L
     * Numbers.powExact(-2L, 62);   // returns 4611686018427387904L
     *
     * // Edge cases
     * Numbers.powExact(5L, 0);     // returns 1L    (any base to the power 0)
     * Numbers.powExact(0L, 0);     // returns 1L    (0^0 is 1 by convention)
     * Numbers.powExact(-2L, 63);   // returns Long.MIN_VALUE   (exactly representable)
     * Numbers.powExact(10L, 19);   // throws ArithmeticException (overflow)
     * Numbers.powExact(2L, 63);    // throws ArithmeticException (overflow)
     * Numbers.powExact(-2L, 64);   // throws ArithmeticException (overflow)
     * Numbers.powExact(2L, -1);    // throws IllegalArgumentException (negative exponent)
     * }</pre>
     *
     * @param b the base long integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power
     * @throws IllegalArgumentException if {@code k < 0}.
     * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed {@code long} arithmetic
     * @see #saturatedPow(long, int)
     * @see #powExact(int, int)
     */
    public static long powExact(long b, int k) throws IllegalArgumentException, ArithmeticException {
        checkNonNegative("exponent", k);
        final long origB = b;
        final int origK = k;
        if (b >= -2 && b <= 2) {
            switch ((int) b) {
                case 0:
                    return (k == 0) ? 1 : 0;
                case 1:
                    return 1;
                case (-1):
                    return ((k & 1) == 0) ? 1 : -1;
                case 2:
                    checkNoOverflow(k < Long.SIZE - 1, b, k);
                    return 1L << k;
                case (-2):
                    checkNoOverflow(k < Long.SIZE, b, k);
                    return ((k & 1) == 0) ? (1L << k) : (-1L << k);
                default:
                    throw new AssertionError();
            }
        }
        long accum = 1;
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return multiplyExact(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = multiplyExact(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        checkNoOverflow(-FLOOR_SQRT_MAX_LONG <= b && b <= FLOOR_SQRT_MAX_LONG, origB, origK);
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns the sum of {@code a} and {@code b}, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method performs addition with saturation arithmetic. If the {@code true} sum would exceed
     * {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the {@code true} sum
     * would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedAdd(100, 200);                  // returns 300
     * Numbers.saturatedAdd(1000000000, 1000000000);    // returns 2000000000
     *
     * // Edge cases: the result clamps instead of wrapping
     * Numbers.saturatedAdd(Integer.MAX_VALUE, 1);      // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedAdd(Integer.MAX_VALUE, 100);    // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedAdd(2000000000, 2000000000);    // returns Integer.MAX_VALUE  (saturates)
     * Numbers.saturatedAdd(Integer.MIN_VALUE, -1);     // returns Integer.MIN_VALUE  (saturates at min)
     * Numbers.saturatedAdd(Integer.MIN_VALUE, -100);   // returns Integer.MIN_VALUE  (saturates at min)
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the sum of {@code a} and {@code b}, or the appropriate bound if overflow would occur
     * @see #saturatedAdd(long, long)
     * @see #addExact(int, int)
     */
    public static int saturatedAdd(final int a, final int b) {
        return saturatedCastToInt((long) a + b);
    }

    /**
     * Returns the sum of {@code a} and {@code b}, saturating at the long bounds instead of overflowing.
     *
     * <p>This method performs addition with saturation arithmetic. If the {@code true} sum would exceed
     * {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the {@code true} sum
     * would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedAdd(100L, 200L);               // returns 300L
     * Numbers.saturatedAdd(1000000000L, 1000000000L); // returns 2000000000L
     *
     * // Edge cases: the result clamps instead of wrapping
     * Numbers.saturatedAdd(Long.MAX_VALUE, 1L);      // returns Long.MAX_VALUE  (saturates at max)
     * Numbers.saturatedAdd(Long.MAX_VALUE, 100L);    // returns Long.MAX_VALUE  (saturates at max)
     * Numbers.saturatedAdd(Long.MIN_VALUE, -1L);     // returns Long.MIN_VALUE  (saturates at min)
     * Numbers.saturatedAdd(Long.MIN_VALUE, -100L);   // returns Long.MIN_VALUE  (saturates at min)
     * }</pre>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the sum of {@code a} and {@code b}, or the appropriate bound if overflow would occur
     * @see #saturatedAdd(int, int)
     * @see #addExact(long, long)
     */
    public static long saturatedAdd(final long a, final long b) {
        final long naiveSum = a + b;
        if ((a ^ b) < 0 || (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    /**
     * Returns the difference of {@code a} and {@code b}, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method performs subtraction with saturation arithmetic. If the {@code true} difference would exceed
     * {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the {@code true} difference
     * would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedSubtract(200, 100);                  // returns 100
     * Numbers.saturatedSubtract(100, 200);                  // returns -100
     *
     * // Edge cases: the result clamps instead of wrapping
     * Numbers.saturatedSubtract(Integer.MAX_VALUE, -1);     // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedSubtract(Integer.MAX_VALUE, -100);   // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedSubtract(Integer.MIN_VALUE, 1);      // returns Integer.MIN_VALUE  (saturates at min)
     * Numbers.saturatedSubtract(Integer.MIN_VALUE, 100);    // returns Integer.MIN_VALUE  (saturates at min)
     * Numbers.saturatedSubtract(-1000000000, 1500000000);   // returns Integer.MIN_VALUE  (saturates)
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the difference {@code a - b}, or the appropriate bound if overflow would occur
     * @see #saturatedSubtract(long, long)
     * @see #subtractExact(int, int)
     */
    public static int saturatedSubtract(final int a, final int b) {
        return saturatedCastToInt((long) a - b);
    }

    /**
     * Returns the difference of {@code a} and {@code b}, saturating at the long bounds instead of overflowing.
     *
     * <p>This method performs subtraction with saturation arithmetic. If the {@code true} difference would exceed
     * {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the {@code true} difference
     * would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedSubtract(200L, 100L);              // returns 100L
     * Numbers.saturatedSubtract(100L, 200L);              // returns -100L
     *
     * // Edge cases: the result clamps instead of wrapping
     * Numbers.saturatedSubtract(Long.MAX_VALUE, -1L);     // returns Long.MAX_VALUE  (saturates at max)
     * Numbers.saturatedSubtract(Long.MAX_VALUE, -100L);   // returns Long.MAX_VALUE  (saturates at max)
     * Numbers.saturatedSubtract(Long.MIN_VALUE, 1L);      // returns Long.MIN_VALUE  (saturates at min)
     * Numbers.saturatedSubtract(Long.MIN_VALUE, 100L);    // returns Long.MIN_VALUE  (saturates at min)
     * }</pre>
     *
     * @param a the first long value
     * @param b the second long value
     * @return the difference {@code a - b}, or the appropriate bound if overflow would occur
     * @see #saturatedSubtract(int, int)
     * @see #subtractExact(long, long)
     */
    public static long saturatedSubtract(final long a, final long b) {
        final long naiveDifference = a - b;
        if ((a ^ b) >= 0 || (a ^ naiveDifference) >= 0) {
            // If a and b have the same signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveDifference;
        }
        // we did over/under flow
        return Long.MAX_VALUE + ((naiveDifference >>> (Long.SIZE - 1)) ^ 1);
    }

    /**
     * Returns the product of {@code a} and {@code b}, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method performs multiplication with saturation arithmetic. If the {@code true} product would exceed
     * {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the {@code true} product
     * would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedMultiply(100, 200);                // returns 20000
     * Numbers.saturatedMultiply(-100, 200);               // returns -20000
     *
     * // Edge cases: the result clamps instead of wrapping
     * Numbers.saturatedMultiply(Integer.MAX_VALUE, 2);    // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedMultiply(100000, 100000);          // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedMultiply(Integer.MIN_VALUE, 2);    // returns Integer.MIN_VALUE  (saturates at min)
     * Numbers.saturatedMultiply(-100000, 100000);         // returns Integer.MIN_VALUE  (saturates at min)
     * Numbers.saturatedMultiply(Integer.MAX_VALUE, -1);   // returns -Integer.MAX_VALUE
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the product {@code a * b}, or the appropriate bound if overflow would occur
     * @see #saturatedMultiply(long, long)
     * @see #multiplyExact(int, int)
     */
    public static int saturatedMultiply(final int a, final int b) {
        return saturatedCastToInt((long) a * b);
    }

    /**
     * Returns the product of {@code a} and {@code b}, saturating at the long bounds instead of overflowing.
     *
     * <p>This method performs multiplication with saturation arithmetic. If the {@code true} product would exceed
     * {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the {@code true} product
     * would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedMultiply(100L, 200L);                    // returns 20000L
     * Numbers.saturatedMultiply(-100L, 200L);                   // returns -20000L
     *
     * // Edge cases: the result clamps instead of wrapping
     * Numbers.saturatedMultiply(0L, Long.MAX_VALUE);            // returns 0L
     * Numbers.saturatedMultiply(Long.MAX_VALUE, 2L);            // returns Long.MAX_VALUE  (saturates at max)
     * Numbers.saturatedMultiply(10000000000L, 10000000000L);    // returns Long.MAX_VALUE  (saturates at max)
     * Numbers.saturatedMultiply(Long.MIN_VALUE, 2L);            // returns Long.MIN_VALUE  (saturates at min)
     * Numbers.saturatedMultiply(-10000000000L, 10000000000L);   // returns Long.MIN_VALUE  (saturates at min)
     * Numbers.saturatedMultiply(Long.MIN_VALUE, -1L);           // returns Long.MAX_VALUE  (2^63 is not representable)
     * }</pre>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the product {@code a * b}, or the appropriate bound if overflow would occur
     * @see #saturatedMultiply(int, int)
     * @see #multiplyExact(long, long)
     */
    public static long saturatedMultiply(final long a, final long b) {
        // see multiplyExact for explanation
        final int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a) + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        // the return value if we will overflow (which we calculate by overflowing a long :) )
        final long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
        if (leadingZeros < Long.SIZE || (a < 0 && b == Long.MIN_VALUE)) { //NOSONAR
            // overflow
            return limit;
        }
        final long result = a * b;
        if (a == 0 || result / a == b) {
            return result;
        }
        return limit;
    }

    /**
     * Returns {@code b} to the {@code k}th power, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method computes integer exponentiation with saturation arithmetic. If the {@code true} result
     * would exceed {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the
     * {@code true} result would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p>This is useful when you want to avoid overflow but don't want to throw exceptions or use
     * larger data types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedPow(2, 3);     // returns 8
     * Numbers.saturatedPow(3, 4);     // returns 81
     * Numbers.saturatedPow(10, 9);    // returns 1000000000
     *
     * // Edge cases: the result clamps instead of overflowing
     * Numbers.saturatedPow(0, 0);     // returns 1                    (0^0 is 1 by convention)
     * Numbers.saturatedPow(10, 10);   // returns Integer.MAX_VALUE    (saturates instead of overflowing)
     * Numbers.saturatedPow(2, 31);    // returns Integer.MAX_VALUE    (saturates at max value)
     * Numbers.saturatedPow(2, 100);   // returns Integer.MAX_VALUE    (saturates at max value)
     * Numbers.saturatedPow(-2, 31);   // returns Integer.MIN_VALUE    (exactly representable)
     * Numbers.saturatedPow(-2, 32);   // returns Integer.MAX_VALUE    (saturates at max value, even exponent)
     * Numbers.saturatedPow(2, -1);    // throws IllegalArgumentException (negative exponent)
     * }</pre>
     *
     * @param b the base integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power, or the appropriate bound if overflow would occur
     * @throws IllegalArgumentException if {@code k < 0}.
     * @see #powExact(int, int)
     * @see #saturatedPow(long, int)
     */
    public static int saturatedPow(int b, int k) throws IllegalArgumentException {
        checkNonNegative("exponent", k);
        switch (b) {
            case 0:
                return (k == 0) ? 1 : 0;
            case 1:
                return 1;
            case (-1):
                return ((k & 1) == 0) ? 1 : -1;
            case 2:
                if (k >= Integer.SIZE - 1) {
                    return Integer.MAX_VALUE;
                }
                return 1 << k;
            case (-2):
                if (k >= Integer.SIZE) {
                    return Integer.MAX_VALUE + (k & 1);
                }
                return ((k & 1) == 0) ? 1 << k : -1 << k;
            default:
                // continue below to handle the general case
        }
        int accum = 1;
        // if b is negative and k is odd then the limit is MIN otherwise the limit is MAX
        final int limit = Integer.MAX_VALUE + ((b >>> (Integer.SIZE - 1)) & (k & 1));
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return saturatedMultiply(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = saturatedMultiply(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        if (-FLOOR_SQRT_MAX_INT > b || b > FLOOR_SQRT_MAX_INT) {
                            return limit;
                        }
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns {@code b} to the {@code k}th power, saturating at the long bounds instead of overflowing.
     *
     * <p>This method computes long integer exponentiation with saturation arithmetic. If the {@code true} result
     * would exceed {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the
     * {@code true} result would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p>This is useful when you want to avoid overflow but don't want to throw exceptions or use
     * larger data types like BigInteger.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedPow(2L, 3);     // returns 8L
     * Numbers.saturatedPow(3L, 4);     // returns 81L
     * Numbers.saturatedPow(10L, 18);   // returns 1000000000000000000L
     *
     * // Edge cases: the result clamps instead of overflowing
     * Numbers.saturatedPow(0L, 0);     // returns 1L                  (0^0 is 1 by convention)
     * Numbers.saturatedPow(10L, 19);   // returns Long.MAX_VALUE      (saturates instead of overflowing)
     * Numbers.saturatedPow(2L, 63);    // returns Long.MAX_VALUE      (saturates at max value)
     * Numbers.saturatedPow(2L, 100);   // returns Long.MAX_VALUE      (saturates at max value)
     * Numbers.saturatedPow(-2L, 63);   // returns Long.MIN_VALUE      (exactly representable)
     * Numbers.saturatedPow(-2L, 64);   // returns Long.MAX_VALUE      (saturates at max value, even exponent)
     * Numbers.saturatedPow(2L, -1);    // throws IllegalArgumentException (negative exponent)
     * }</pre>
     *
     * @param b the base long integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power, or the appropriate bound if overflow would occur
     * @throws IllegalArgumentException if {@code k < 0}.
     * @see #powExact(long, int)
     * @see #saturatedPow(int, int)
     */
    public static long saturatedPow(long b, int k) throws IllegalArgumentException {
        checkNonNegative("exponent", k);
        if (b >= -2 && b <= 2) {
            switch ((int) b) {
                case 0:
                    return (k == 0) ? 1 : 0;
                case 1:
                    return 1;
                case (-1):
                    return ((k & 1) == 0) ? 1 : -1;
                case 2:
                    if (k >= Long.SIZE - 1) {
                        return Long.MAX_VALUE;
                    }
                    return 1L << k;
                case (-2):
                    if (k >= Long.SIZE) {
                        return Long.MAX_VALUE + (k & 1);
                    }
                    return ((k & 1) == 0) ? (1L << k) : (-1L << k);
                default:
                    throw new AssertionError();
            }
        }
        long accum = 1;
        // if b is negative and k is odd then the limit is MIN otherwise the limit is MAX
        final long limit = Long.MAX_VALUE + ((b >>> Long.SIZE - 1) & (k & 1));
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return saturatedMultiply(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = saturatedMultiply(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        if (-FLOOR_SQRT_MAX_LONG > b || b > FLOOR_SQRT_MAX_LONG) {
                            return limit;
                        }
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns {@code value} clamped to the inclusive {@code int} range.
     *
     * <p>This method casts a {@code long} value to an {@code int} with saturation. If the value
     * exceeds {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the value
     * is less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     * Otherwise, the method returns the {@code long} value cast to an {@code int}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedCastToInt(100L);             // returns 100
     * Numbers.saturatedCastToInt(0L);               // returns 0
     * Numbers.saturatedCastToInt(10000000000L);     // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedCastToInt(-10000000000L);    // returns Integer.MIN_VALUE  (saturates at min)
     *
     * // Edge cases: the exact bounds fit; anything past them clamps
     * Numbers.saturatedCastToInt(2147483647L);      // returns Integer.MAX_VALUE  (exact fit)
     * Numbers.saturatedCastToInt(2147483648L);      // returns Integer.MAX_VALUE  (saturates at max)
     * Numbers.saturatedCastToInt(-2147483648L);     // returns Integer.MIN_VALUE  (exact fit)
     * Numbers.saturatedCastToInt(-2147483649L);     // returns Integer.MIN_VALUE  (saturates at min)
     * Numbers.saturatedCastToInt(Long.MAX_VALUE);   // returns Integer.MAX_VALUE  (saturates at max)
     * }</pre>
     *
     * <p>The throwing counterpart of this {@code long}-to-{@code int} narrowing is
     * {@link #toIntExact(long)}, which throws an {@code ArithmeticException} instead of saturating.</p>
     *
     * <p>There is deliberately no {@code saturatedCastToByte} or {@code saturatedCastToShort}: for a narrower
     * target use {@link #clamp(long, long, long)} with the target's bounds &mdash; for example
     * {@code (byte) Numbers.clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE)} &mdash; or
     * {@link #toByte(Object)}/{@link #toShort(Object)} for the throwing form.</p>
     *
     * @param value any {@code long} value
     * @return {@code value} as an {@code int}, clamped to {@code Integer.MAX_VALUE} if it is too large or
     *         {@code Integer.MIN_VALUE} if it is too small
     * @see #toIntExact(long)
     */
    public static int saturatedCastToInt(final long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /**
     * Clamps the given {@code int} value to the inclusive range {@code [min, max]}.
     *
     * <p>Returns {@code min} if {@code value < min}, {@code max} if {@code value > max}, otherwise {@code value}.
     * Equivalent to {@code Math.clamp(value, min, max)} (Java 21+), provided here for the Java 17 baseline.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.clamp(5, 1, 10);    // returns 5
     * Numbers.clamp(-3, 1, 10);   // returns 1   (below min)
     * Numbers.clamp(42, 1, 10);   // returns 10  (above max)
     *
     * // Edge cases
     * Numbers.clamp(1, 1, 1);     // returns 1   (a single-point range is allowed)
     * Numbers.clamp(5, 10, 1);    // throws IllegalArgumentException (min > max)
     * }</pre>
     *
     * @param value the value to clamp
     * @param min the lower bound (inclusive)
     * @param max the upper bound (inclusive)
     * @return {@code value} constrained to {@code [min, max]}
     * @throws IllegalArgumentException if {@code min > max}.
     * @see #clamp(long, long, long)
     * @see #clamp(float, float, float)
     * @see #clamp(double, double, double)
     */
    public static int clamp(final int value, final int min, final int max) throws IllegalArgumentException {
        N.checkArgument(min <= max, "min (%s) must not be greater than max (%s)", min, max);

        return Math.min(max, Math.max(value, min));
    }

    /**
     * Clamps the given {@code long} value to the inclusive range {@code [min, max]}.
     *
     * <p>Returns {@code min} if {@code value < min}, {@code max} if {@code value > max}, otherwise {@code value}.
     * Equivalent to {@code Math.clamp(value, min, max)} (Java 21+), provided here for the Java 17 baseline.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.clamp(5L, 1L, 10L);    // returns 5L
     * Numbers.clamp(-3L, 1L, 10L);   // returns 1L   (below min)
     * Numbers.clamp(42L, 1L, 10L);   // returns 10L  (above max)
     *
     * // Edge cases
     * Numbers.clamp(1L, 1L, 1L);     // returns 1L   (a single-point range is allowed)
     * Numbers.clamp(5L, 10L, 1L);    // throws IllegalArgumentException (min > max)
     * }</pre>
     *
     * @param value the value to clamp
     * @param min the lower bound (inclusive)
     * @param max the upper bound (inclusive)
     * @return {@code value} constrained to {@code [min, max]}
     * @throws IllegalArgumentException if {@code min > max}.
     * @see #clamp(int, int, int)
     * @see #clamp(float, float, float)
     * @see #clamp(double, double, double)
     */
    public static long clamp(final long value, final long min, final long max) throws IllegalArgumentException {
        N.checkArgument(min <= max, "min (%s) must not be greater than max (%s)", min, max);

        return Math.min(max, Math.max(value, min));
    }

    /**
     * Clamps the given {@code float} value to the inclusive range {@code [min, max]}.
     *
     * <p>Returns {@code min} if {@code value < min}, {@code max} if {@code value > max}, otherwise {@code value}.
     * A {@code NaN} {@code value} is returned unchanged. Equivalent to {@code Math.clamp(value, min, max)} (Java 21+),
     * provided here for the Java 17 baseline; implemented as {@code Math.min(max, Math.max(value, min))}, so it
     * matches {@code Math.clamp} exactly, including signed-zero ordering: {@code -0.0f} is treated as strictly less
     * than {@code +0.0f} (e.g. {@code clamp(-0.0f, 0.0f, 1.0f)} returns {@code +0.0f}). Bounds are compared with
     * {@link Float#compare(float, float)}, so {@code min = +0.0f} and {@code max = -0.0f} is an invalid interval
     * and throws (as in {@code Math.clamp}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.clamp(5.0f, 1.0f, 10.0f);        // returns 5.0f
     * Numbers.clamp(-3.0f, 1.0f, 10.0f);       // returns 1.0f   (below min)
     * Numbers.clamp(42.0f, 1.0f, 10.0f);       // returns 10.0f  (above max)
     *
     * // Edge cases
     * Numbers.clamp(Float.NaN, 1.0f, 10.0f);   // returns NaN
     * Numbers.clamp(-0.0f, 0.0f, 1.0f);        // returns +0.0f  (-0.0f ordered below +0.0f, as in Math.clamp)
     * Numbers.clamp(5.0f, 10.0f, 1.0f);        // throws IllegalArgumentException (min > max)
     * Numbers.clamp(5.0f, Float.NaN, 10.0f);   // throws IllegalArgumentException (a bound must not be NaN)
     * }</pre>
     *
     * @param value the value to clamp
     * @param min the lower bound (inclusive); must not be {@code NaN}
     * @param max the upper bound (inclusive); must not be {@code NaN}
     * @return {@code value} constrained to {@code [min, max]}, or {@code NaN} if {@code value} is {@code NaN}
     * @throws IllegalArgumentException if {@code min > max} or either bound is {@code NaN}.
     * @see #clamp(int, int, int)
     * @see #clamp(long, long, long)
     * @see #clamp(double, double, double)
     */
    public static float clamp(final float value, final float min, final float max) throws IllegalArgumentException {
        if (Float.isNaN(min) || Float.isNaN(max)) {
            throw new IllegalArgumentException("min (" + min + ") and max (" + max + ") must not be NaN");
        }
        N.checkArgument(Float.compare(min, max) <= 0, "min (%s) must not be greater than max (%s)", min, max);

        return Math.min(max, Math.max(value, min));
    }

    /**
     * Clamps the given {@code double} value to the inclusive range {@code [min, max]}.
     *
     * <p>Returns {@code min} if {@code value < min}, {@code max} if {@code value > max}, otherwise {@code value}.
     * A {@code NaN} {@code value} is returned unchanged. Equivalent to {@code Math.clamp(value, min, max)} (Java 21+),
     * provided here for the Java 17 baseline; implemented as {@code Math.min(max, Math.max(value, min))}, so it
     * matches {@code Math.clamp} exactly, including signed-zero ordering: {@code -0.0} is treated as strictly less
     * than {@code +0.0} (e.g. {@code clamp(-0.0, 0.0, 1.0)} returns {@code +0.0}). Bounds are compared with
     * {@link Double#compare(double, double)}, so {@code min = +0.0} and {@code max = -0.0} is an invalid interval
     * and throws (as in {@code Math.clamp}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.clamp(5.0, 1.0, 10.0);          // returns 5.0
     * Numbers.clamp(-3.0, 1.0, 10.0);         // returns 1.0   (below min)
     * Numbers.clamp(42.0, 1.0, 10.0);         // returns 10.0  (above max)
     *
     * // Edge cases
     * Numbers.clamp(Double.NaN, 1.0, 10.0);   // returns NaN
     * Numbers.clamp(-0.0, 0.0, 1.0);          // returns +0.0  (-0.0 ordered below +0.0, as in Math.clamp)
     * Numbers.clamp(5.0, 10.0, 1.0);          // throws IllegalArgumentException (min > max)
     * Numbers.clamp(5.0, Double.NaN, 10.0);   // throws IllegalArgumentException (a bound must not be NaN)
     * }</pre>
     *
     * @param value the value to clamp
     * @param min the lower bound (inclusive); must not be {@code NaN}
     * @param max the upper bound (inclusive); must not be {@code NaN}
     * @return {@code value} constrained to {@code [min, max]}, or {@code NaN} if {@code value} is {@code NaN}
     * @throws IllegalArgumentException if {@code min > max} or either bound is {@code NaN}.
     * @see #clamp(int, int, int)
     * @see #clamp(long, long, long)
     * @see #clamp(float, float, float)
     */
    public static double clamp(final double value, final double min, final double max) throws IllegalArgumentException {
        if (Double.isNaN(min) || Double.isNaN(max)) {
            throw new IllegalArgumentException("min (" + min + ") and max (" + max + ") must not be NaN");
        }
        N.checkArgument(Double.compare(min, max) <= 0, "min (%s) must not be greater than max (%s)", min, max);

        return Math.min(max, Math.max(value, min));
    }

    /**
     * Returns {@code n!} (n factorial), the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. If the {@code true} result would exceed {@code Integer.MAX_VALUE}, this method
     * returns {@code Integer.MAX_VALUE} instead.
     *
     * <p>The largest value of {@code n} for which {@code n!} fits in an {@code int} is 12.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedFactorial(5);     // returns 120
     * Numbers.saturatedFactorial(10);    // returns 3628800
     * Numbers.saturatedFactorial(12);    // returns 479001600 (the largest n! that fits an int)
     *
     * // Edge cases
     * Numbers.saturatedFactorial(0);     // returns 1 (0! is 1 by convention)
     * Numbers.saturatedFactorial(1);     // returns 1
     * Numbers.saturatedFactorial(13);    // returns Integer.MAX_VALUE (overflow, saturates)
     * Numbers.saturatedFactorial(100);   // returns Integer.MAX_VALUE (overflow, saturates)
     * Numbers.saturatedFactorial(-1);    // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} if it fits in an {@code int}, otherwise {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}.
     * @see #factorialExact(int)
     * @see #factorialExactToLong(int)
     * @see #saturatedFactorialToLong(int)
     * @see #factorialToDouble(int)
     * @see #factorialToBigInteger(int)
     */
    public static int saturatedFactorial(final int n) throws IllegalArgumentException {
        checkNonNegative("n", n);
        return (n < int_factorials.length) ? int_factorials[n] : Integer.MAX_VALUE;
    }

    /**
     * Returns {@code n!} (n factorial) as an {@code int}, throwing if the result does not fit.
     *
     * <p>The largest value of {@code n} for which {@code n!} fits in an {@code int} is 12.
     * Use {@link #saturatedFactorial(int)} to clamp at {@code Integer.MAX_VALUE} instead of throwing,
     * or {@link #factorialExactToLong(int)}/{@link #factorialToBigInteger(int)} for a wider result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.factorialExact(5);     // returns 120
     * Numbers.factorialExact(12);    // returns 479001600
     *
     * // Edge cases
     * Numbers.factorialExact(0);     // returns 1  (0! is 1 by convention)
     * Numbers.factorialExact(13);    // throws ArithmeticException (does not fit an int)
     * Numbers.factorialExact(-1);    // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} as an {@code int}
     * @throws IllegalArgumentException if {@code n < 0}.
     * @throws ArithmeticException if {@code n!} does not fit in an {@code int} ({@code n > 12})
     * @see #factorialToDouble(int)
     * @see #saturatedFactorial(int)
     * @see #factorialExactToLong(int)
     * @see #saturatedFactorialToLong(int)
     * @see #factorialToBigInteger(int)
     */
    public static int factorialExact(final int n) throws IllegalArgumentException, ArithmeticException {
        checkNonNegative("n", n);
        if (n >= int_factorials.length) {
            throw new ArithmeticException("factorialExact(" + n + ") overflow");
        }
        return int_factorials[n];
    }

    /**
     * Returns {@code n!} (n factorial) as a {@code long}, the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. If the {@code true} result would exceed {@code Long.MAX_VALUE}, this method
     * returns {@code Long.MAX_VALUE} instead.
     *
     * <p>The largest value of {@code n} for which {@code n!} fits in a {@code long} is 20.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedFactorialToLong(5);     // returns 120L
     * Numbers.saturatedFactorialToLong(10);    // returns 3628800L
     * Numbers.saturatedFactorialToLong(15);    // returns 1307674368000L
     * Numbers.saturatedFactorialToLong(20);    // returns 2432902008176640000L (the largest n! that fits a long)
     *
     * // Edge cases
     * Numbers.saturatedFactorialToLong(0);     // returns 1L (0! is 1 by convention)
     * Numbers.saturatedFactorialToLong(1);     // returns 1L
     * Numbers.saturatedFactorialToLong(21);    // returns Long.MAX_VALUE (overflow, saturates)
     * Numbers.saturatedFactorialToLong(100);   // returns Long.MAX_VALUE (overflow, saturates)
     * Numbers.saturatedFactorialToLong(-1);    // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} if it fits in a {@code long}, otherwise {@code Long.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}.
     * @see #saturatedFactorial(int)
     * @see #factorialExact(int)
     * @see #factorialExactToLong(int)
     * @see #factorialToDouble(int)
     * @see #factorialToBigInteger(int)
     */
    public static long saturatedFactorialToLong(final int n) throws IllegalArgumentException {
        checkNonNegative("n", n);
        return (n < long_factorials.length) ? long_factorials[n] : Long.MAX_VALUE;
    }

    /**
     * Returns {@code n!} (n factorial) as a {@code long}, throwing if the result does not fit.
     *
     * <p>The largest value of {@code n} for which {@code n!} fits in a {@code long} is 20.
     * Use {@link #saturatedFactorialToLong(int)} to clamp at {@code Long.MAX_VALUE} instead of throwing,
     * or {@link #factorialToBigInteger(int)} for a wider result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.factorialExactToLong(5);     // returns 120L
     * Numbers.factorialExactToLong(20);    // returns 2432902008176640000L
     *
     * // Edge cases
     * Numbers.factorialExactToLong(0);     // returns 1L  (0! is 1 by convention)
     * Numbers.factorialExactToLong(21);    // throws ArithmeticException (does not fit a long)
     * Numbers.factorialExactToLong(-1);    // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} as a {@code long}
     * @throws IllegalArgumentException if {@code n < 0}.
     * @throws ArithmeticException if {@code n!} does not fit in a {@code long} ({@code n > 20})
     * @see #factorialToDouble(int)
     * @see #saturatedFactorial(int)
     * @see #saturatedFactorialToLong(int)
     * @see #factorialExact(int)
     * @see #factorialToBigInteger(int)
     */
    public static long factorialExactToLong(final int n) throws IllegalArgumentException, ArithmeticException {
        checkNonNegative("n", n);
        if (n >= long_factorials.length) {
            throw new ArithmeticException("factorialExactToLong(" + n + ") overflow");
        }
        return long_factorials[n];
    }

    /**
     * Returns {@code n!} (n factorial) as a {@code double}, the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. If the {@code true} result would exceed {@code Double.MAX_VALUE}, this method
     * returns {@code Double.POSITIVE_INFINITY}.
     *
     * <p>The result is within 1 ulp of the {@code true} value, providing accurate floating-point
     * approximations for factorial values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.factorialToDouble(5);      // returns 120.0
     * Numbers.factorialToDouble(10);     // returns 3628800.0
     * Numbers.factorialToDouble(20);     // returns 2.43290200817664E18
     *
     * // Edge cases
     * Numbers.factorialToDouble(0);      // returns 1.0 (0! is 1 by convention)
     * Numbers.factorialToDouble(1);      // returns 1.0
     * Numbers.factorialToDouble(170);    // returns 7.257415615308E306 (the largest finite n!)
     * Numbers.factorialToDouble(171);    // returns Double.POSITIVE_INFINITY  (exceeds Double.MAX_VALUE)
     * Numbers.factorialToDouble(1000);   // returns Double.POSITIVE_INFINITY  (exceeds Double.MAX_VALUE)
     * Numbers.factorialToDouble(-1);     // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} as a {@code double}, or {@code Double.POSITIVE_INFINITY} if the result exceeds {@code Double.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}.
     * @see #saturatedFactorial(int)
     * @see #factorialExact(int)
     * @see #factorialExactToLong(int)
     * @see #saturatedFactorialToLong(int)
     * @see #factorialToBigInteger(int)
     */
    public static double factorialToDouble(final int n) throws IllegalArgumentException {
        checkNonNegative("n", n);
        if (n > MAX_FACTORIAL) {
            return Double.POSITIVE_INFINITY;
        } else {
            // Multiplying the last (n & 0xf) values into their own accumulator gives a more accurate
            // result than multiplying by everySixteenthFactorial[n >> 4] directly.
            double accum = 1.0;
            for (int i = 1 + (n & ~0xf); i <= n; i++) {
                accum *= i;
            }
            return accum * everySixteenthFactorial[n >> 4];
        }
    }

    /**
     * Returns {@code n!} (n factorial) as a {@code BigInteger}, the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. This method supports arbitrary-precision computation and can handle
     * very large values of {@code n}.
     *
     * <p><b>Performance Note:</b> This method uses an efficient binary recursive algorithm with
     * balanced multiplies. It removes all factors of 2 from intermediate products and shifts them
     * back in at the end. The result takes <i>O(n log n)</i> space, so use cautiously for very
     * large values of {@code n}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.factorialToBigInteger(5);      // returns 120
     * Numbers.factorialToBigInteger(20);     // returns 2432902008176640000
     * Numbers.factorialToBigInteger(100);    // returns a 158-digit number
     *
     * // Edge cases
     * Numbers.factorialToBigInteger(0);      // returns 1 (0! is 1 by convention)
     * Numbers.factorialToBigInteger(1000);   // returns a 2568-digit number (use cautiously)
     * Numbers.factorialToBigInteger(-1);     // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * <p><b>Unbounded result.</b> This is the only factorial in the class with no ceiling: {@code n!} has
     * roughly {@code n * log10(n)} decimal digits, so both the cost and the size of the result grow without
     * limit, and a large {@code n} is bounded only by time and memory &mdash;
     * {@code factorialToBigInteger(Integer.MAX_VALUE)} exhausts the heap rather than returning. Use
     * {@link #factorialToDouble(int)} when an approximation is enough, or
     * {@link #saturatedFactorialToLong(int)} for a bounded result.</p>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} as a {@code BigInteger}
     * @throws IllegalArgumentException if {@code n < 0}.
     * @see #saturatedFactorial(int)
     * @see #factorialExact(int)
     * @see #factorialExactToLong(int)
     * @see #saturatedFactorialToLong(int)
     * @see #factorialToDouble(int)
     */
    public static BigInteger factorialToBigInteger(final int n) throws IllegalArgumentException {
        checkNonNegative("n", n);

        // If the factorial is small enough, just use LongMath to do it.
        if (n < long_factorials.length) {
            return BigInteger.valueOf(long_factorials[n]);
        }

        // Use long multiplication: n * log2(n) overflows int for large n. The quotient fits in an
        // int for every non-negative int n because log2(n) is at most 31 and the divisor is 64.
        final int approxSize = (int) divide((long) n * log2(n, CEILING), Long.SIZE, CEILING);
        final ArrayList<BigInteger> bignums = new ArrayList<>(approxSize);

        // Start from the pre-computed maximum long factorial.
        final int startingNumber = long_factorials.length;
        long product = long_factorials[startingNumber - 1];
        // Strip off 2s from this value.
        int shift = Long.numberOfTrailingZeros(product);
        product >>= shift;

        // Use floor(log2(num)) + 1 to prevent overflow of multiplication.
        int productBits = log2(product, FLOOR) + 1;
        int bits = log2(startingNumber, FLOOR) + 1;
        // Check for the next power of two boundary, to save us a CLZ operation.
        int nextPowerOfTwo = 1 << (bits - 1);

        // Iteratively multiply the longs as big as they can go.
        for (long num = startingNumber; num <= n; num++) {
            // Check to see if the floor(log2(num)) + 1 has changed.
            if ((num & nextPowerOfTwo) != 0) {
                nextPowerOfTwo <<= 1;
                bits++;
            }
            // Get rid of the 2s in num.
            final int tz = Long.numberOfTrailingZeros(num);
            final long normalizedNum = num >> tz;
            shift += tz;
            // Adjust floor(log2(num)) + 1.
            final int normalizedBits = bits - tz;
            // If it doesn't fit in a long, then we store off the intermediate product.
            if (normalizedBits + productBits >= Long.SIZE) {
                bignums.add(BigInteger.valueOf(product));
                product = 1;
                productBits = 0; //NOSONAR
            }
            product *= normalizedNum;
            productBits = log2(product, FLOOR) + 1;
        }
        // Check for leftovers.
        if (product > 1) {
            bignums.add(BigInteger.valueOf(product));
        }
        // Efficiently multiply all the intermediate products together.
        return listProduct(bignums).shiftLeft(shift);
    }

    /**
     * Returns the product of all elements of {@code nums}, or {@link BigInteger#ONE} if the list is empty.
     *
     * @param nums the values to multiply; must not be {@code null}
     * @return the product of every element of {@code nums}
     */
    static BigInteger listProduct(final List<BigInteger> nums) {
        return listProduct(nums, 0, nums.size());
    }

    /**
     * Returns the product of {@code nums[start..end)}, or {@link BigInteger#ONE} if the range is empty.
     * The range is multiplied by recursive halving (balanced multiplies), which is substantially faster than
     * a left-to-right fold once the intermediate values grow large.
     *
     * @param nums the values to multiply; must not be {@code null}
     * @param start the index of the first element to include (inclusive)
     * @param end the index after the last element to include (exclusive)
     * @return the product of the elements in {@code [start, end)}
     */
    static BigInteger listProduct(final List<BigInteger> nums, final int start, final int end) {
        switch (end - start) {
            case 0:
                return BigInteger.ONE;
            case 1:
                return nums.get(start);
            case 2:
                return nums.get(start).multiply(nums.get(start + 1));
            case 3:
                return nums.get(start).multiply(nums.get(start + 1)).multiply(nums.get(start + 2));
            default:
                // Otherwise, split the list in half and recursively do this.
                final int m = (end + start) >>> 1;
                return listProduct(nums, start, m).multiply(listProduct(nums, m, end));
        }
    }

    /**
     * Returns the binomial coefficient "n choose k", denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from
     * {@code n} items without regard to order. It is calculated as {@code n! / (k! * (n-k)!)}.
     * If the result would exceed {@code Integer.MAX_VALUE}, this method returns {@code Integer.MAX_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedBinomial(5, 0);      // returns 1      (only one way to choose nothing)
     * Numbers.saturatedBinomial(5, 1);      // returns 5      (five ways to choose one item)
     * Numbers.saturatedBinomial(5, 2);      // returns 10
     * Numbers.saturatedBinomial(5, 3);      // returns 10
     * Numbers.saturatedBinomial(10, 5);     // returns 252
     * Numbers.saturatedBinomial(52, 5);     // returns 2598960  (poker hands from a deck)
     * Numbers.saturatedBinomial(100, 50);   // returns Integer.MAX_VALUE  (overflow, saturates)
     *
     * // Edge cases
     * Numbers.saturatedBinomial(5, 5);      // returns 1
     * Numbers.saturatedBinomial(0, 0);      // returns 1
     * Numbers.saturatedBinomial(5, 6);      // throws IllegalArgumentException (k > n)
     * Numbers.saturatedBinomial(5, -1);     // throws IllegalArgumentException (k must be non-negative)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) if it fits in an {@code int}, otherwise {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @see #binomialExact(int, int)
     * @see #binomialExactToLong(int, int)
     * @see #saturatedBinomialToLong(int, int)
     * @see #binomialToDouble(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static int saturatedBinomial(final int n, final int k) throws IllegalArgumentException {
        return binomial0(n, k, false);
    }

    /**
     * Shared implementation of {@link #saturatedBinomial(int, int)} and {@link #binomialExact(int, int)}, so the
     * overflow predicate is stated once instead of being re-derived by the throwing variant.
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @param throwOnOverflow {@code true} to throw when {@code C(n, k)} does not fit an {@code int},
     *        {@code false} to return {@link Integer#MAX_VALUE}
     * @return the binomial coefficient {@code C(n, k)}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @throws ArithmeticException if {@code throwOnOverflow} and {@code C(n, k)} does not fit an {@code int}
     */
    private static int binomial0(final int n, final int k, final boolean throwOnOverflow) throws IllegalArgumentException, ArithmeticException {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        N.checkArgument(k <= n, "k (%s) > n (%s)", k, n);

        // C(n, k) == C(n, n - k); the tables below are indexed by the smaller one.
        final int kk = (k > (n >> 1)) ? n - k : k;

        if (kk >= int_biggestBinomials.length || n > int_biggestBinomials[kk]) {
            if (throwOnOverflow) {
                throw new ArithmeticException("binomialExact(" + n + ", " + k + ") overflow");
            }

            return Integer.MAX_VALUE;
        }

        switch (kk) {
            case 0:
                return 1;
            case 1:
                return n;
            default:
                long result = 1;
                for (int i = 0; i < kk; i++) {
                    result *= n - i;
                    result /= i + 1;
                }
                return (int) result;
        }
    }

    /**
     * Returns the binomial coefficient C(n, k) as an {@code int}, throwing if the result does not fit.
     *
     * <p>Use {@link #saturatedBinomial(int, int)} to clamp at {@code Integer.MAX_VALUE} instead of throwing,
     * or {@link #binomialExactToLong(int, int)}/{@link #binomialToBigInteger(int, int)} for a wider result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.binomialExact(5, 2);     // returns 10
     * Numbers.binomialExact(10, 5);    // returns 252
     *
     * // Edge cases
     * Numbers.binomialExact(5, 0);     // returns 1
     * Numbers.binomialExact(100, 50);  // throws ArithmeticException (does not fit an int)
     * Numbers.binomialExact(5, 6);     // throws IllegalArgumentException (k > n)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) as an {@code int}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @throws ArithmeticException if C(n, k) does not fit in an {@code int}
     * @see #binomialToDouble(int, int)
     * @see #saturatedBinomial(int, int)
     * @see #binomialExactToLong(int, int)
     * @see #saturatedBinomialToLong(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static int binomialExact(final int n, final int k) throws IllegalArgumentException, ArithmeticException {
        return binomial0(n, k, true);
    }

    /**
     * Returns the binomial coefficient "n choose k" as a {@code long}, denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from
     * {@code n} items without regard to order. It is calculated as {@code n! / (k! * (n-k)!)}.
     * If the result would exceed {@code Long.MAX_VALUE}, this method returns {@code Long.MAX_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.saturatedBinomialToLong(5, 0);      // returns 1L        (only one way to choose nothing)
     * Numbers.saturatedBinomialToLong(5, 1);      // returns 5L        (five ways to choose one item)
     * Numbers.saturatedBinomialToLong(5, 2);      // returns 10L
     * Numbers.saturatedBinomialToLong(10, 5);     // returns 252L
     * Numbers.saturatedBinomialToLong(52, 5);     // returns 2598960L  (poker hands from a deck)
     * Numbers.saturatedBinomialToLong(60, 30);    // returns 118264581564861424L
     * Numbers.saturatedBinomialToLong(100, 50);   // returns Long.MAX_VALUE  (overflow, saturates)
     *
     * // Edge cases
     * Numbers.saturatedBinomialToLong(5, 5);      // returns 1L
     * Numbers.saturatedBinomialToLong(0, 0);      // returns 1L
     * Numbers.saturatedBinomialToLong(5, 6);      // throws IllegalArgumentException (k > n)
     * Numbers.saturatedBinomialToLong(-1, 0);     // throws IllegalArgumentException (n must be non-negative)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) if it fits in a {@code long}, otherwise {@code Long.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @see #saturatedBinomial(int, int)
     * @see #binomialExact(int, int)
     * @see #binomialExactToLong(int, int)
     * @see #binomialToDouble(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static long saturatedBinomialToLong(final int n, final int k) throws IllegalArgumentException {
        return binomialToLong0(n, k, false);
    }

    /**
     * Shared implementation of {@link #saturatedBinomialToLong(int, int)} and
     * {@link #binomialExactToLong(int, int)}, so the overflow predicate is stated once instead of being
     * re-derived by the throwing variant.
     *
     * @param originalN the total number of items as supplied by the caller; must be non-negative
     * @param originalK the number of items to choose as supplied by the caller; must be non-negative and at most {@code originalN}
     * @param throwOnOverflow {@code true} to throw when {@code C(n, k)} does not fit a {@code long},
     *        {@code false} to return {@link Long#MAX_VALUE}
     * @return the binomial coefficient {@code C(n, k)}
     * @throws IllegalArgumentException if {@code originalN < 0}, {@code originalK < 0}, or {@code originalK > originalN}.
     * @throws ArithmeticException if {@code throwOnOverflow} and {@code C(n, k)} does not fit a {@code long}
     */
    private static long binomialToLong0(final int originalN, final int originalK, final boolean throwOnOverflow)
            throws IllegalArgumentException, ArithmeticException {
        checkNonNegative("n", originalN);
        checkNonNegative("k", originalK);
        N.checkArgument(originalK <= originalN, "k (%s) > n (%s)", originalK, originalN);

        int n = originalN;
        // C(n, k) == C(n, n - k); the tables below are indexed by the smaller one.
        final int k = (originalK > (originalN >> 1)) ? originalN - originalK : originalK;

        switch (k) {
            case 0:
                return 1;
            case 1:
                return n;
            default:
                if (n < long_factorials.length) {
                    return long_factorials[n] / (long_factorials[k] * long_factorials[n - k]);
                } else if (k >= biggestBinomials.length || n > biggestBinomials[k]) {
                    if (throwOnOverflow) {
                        throw new ArithmeticException("binomialExactToLong(" + originalN + ", " + originalK + ") overflow");
                    }

                    return Long.MAX_VALUE;
                } else if (k < biggestSimpleBinomials.length && n <= biggestSimpleBinomials[k]) {
                    // guaranteed not to overflow
                    long result = n--;
                    for (int i = 2; i <= k; n--, i++) {
                        result *= n;
                        result /= i;
                    }
                    return result;
                } else {
                    final int nBits = log2(n, RoundingMode.CEILING);

                    long result = 1;
                    long numerator = n--;
                    long denominator = 1;

                    int numeratorBits = nBits;
                    // This is an upper bound on log2(numerator, ceiling).

                    /*
                     * We want to do this in long math for speed, but want to avoid overflow. We adapt the
                     * technique previously used by BigIntegerMath: maintain separate numerator and
                     * denominator accumulators, multiplying the fraction into the result when near overflow.
                     */
                    for (int i = 2; i <= k; i++, n--) {
                        if (numeratorBits + nBits < Long.SIZE - 1) {
                            // It's definitely safe to multiply into numerator and denominator.
                            numerator *= n;
                            denominator *= i;
                            numeratorBits += nBits;
                        } else {
                            // It might not be safe to multiply into numerator and denominator,
                            // so multiply (numerator / denominator) into the result.
                            result = multiplyFraction(result, numerator, denominator);
                            numerator = n;
                            denominator = i;
                            numeratorBits = nBits;
                        }
                    }
                    return multiplyFraction(result, numerator, denominator);
                }
        }
    }

    /**
     * Returns the binomial coefficient C(n, k) as a {@code long}, throwing if the result does not fit.
     *
     * <p>Use {@link #saturatedBinomialToLong(int, int)} to clamp at {@code Long.MAX_VALUE} instead of throwing,
     * or {@link #binomialToBigInteger(int, int)} for a wider result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.binomialExactToLong(5, 2);     // returns 10L
     * Numbers.binomialExactToLong(60, 30);   // returns 118264581564861424L
     *
     * // Edge cases
     * Numbers.binomialExactToLong(5, 0);     // returns 1L
     * Numbers.binomialExactToLong(100, 50);  // throws ArithmeticException (does not fit a long)
     * Numbers.binomialExactToLong(5, 6);     // throws IllegalArgumentException (k > n)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) as a {@code long}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @throws ArithmeticException if C(n, k) does not fit in a {@code long}
     * @see #binomialToDouble(int, int)
     * @see #saturatedBinomial(int, int)
     * @see #saturatedBinomialToLong(int, int)
     * @see #binomialExact(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static long binomialExactToLong(final int n, final int k) throws IllegalArgumentException, ArithmeticException {
        return binomialToLong0(n, k, true);
    }

    /**
     * Returns the binomial coefficient "n choose k" as a {@code double}, denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from {@code n} items
     * without regard to order. Results that fit in a {@code long} use the existing exact long-arithmetic path;
     * larger results batch numerator and denominator factors in {@code long} values before flushing them to exact
     * {@link BigInteger} intermediates. Once a flushed coefficient has more than 1024 bits, all remaining
     * coefficients up to the symmetry-adjusted {@code k} are at least as large, so this method returns
     * {@code Double.POSITIVE_INFINITY} without materializing an arbitrarily large exact result. This is the
     * {@code double}-valued rung of the binomial family, mirroring {@link #factorialToDouble(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.binomialToDouble(5, 2);         // returns 10.0
     * Numbers.binomialToDouble(52, 5);        // returns 2598960.0
     * Numbers.binomialToDouble(100, 50);      // returns about 1.0089E29 (exceeds long, fits double)
     * Numbers.binomialToDouble(2000, 1000);   // returns Double.POSITIVE_INFINITY (exceeds Double.MAX_VALUE)
     *
     * // Edge cases
     * Numbers.binomialToDouble(5, 0);         // returns 1.0
     * Numbers.binomialToDouble(5, 6);         // throws IllegalArgumentException (k > n)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) as a {@code double}, or {@code Double.POSITIVE_INFINITY}
     *         if the true value exceeds {@code Double.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @see #saturatedBinomial(int, int)
     * @see #binomialExact(int, int)
     * @see #binomialExactToLong(int, int)
     * @see #saturatedBinomialToLong(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static double binomialToDouble(final int n, final int k) throws IllegalArgumentException {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        N.checkArgument(k <= n, "k (%s) > n (%s)", k, n);

        final int kk = k > (n >> 1) ? n - k : k;

        if (kk < biggestBinomials.length && n <= biggestBinomials[kk]) {
            return saturatedBinomialToLong(n, kk);
        }

        final BigInteger result = binomialToBigIntegerBatched(n, kk, Double.MAX_EXPONENT + 1);

        // A positive integer with more than 1024 bits cannot round to a finite double.
        if (result.bitLength() > Double.MAX_EXPONENT + 1) {
            return Double.POSITIVE_INFINITY;
        }

        return result.doubleValue();
    }

    /**
     * Returns the binomial coefficient "n choose k" as a {@code BigInteger}, denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from
     * {@code n} items without regard to order. It is calculated as {@code n! / (k! * (n-k)!)}.
     * This method supports arbitrary-precision computation and can handle very large values.
     *
     * <p><b>Performance Note:</b> The result can take as much as <i>O(k log n)</i> space. Use
     * cautiously for very large values of {@code k} and {@code n}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.binomialToBigInteger(5, 2);        // returns 10
     * Numbers.binomialToBigInteger(10, 5);       // returns 252
     * Numbers.binomialToBigInteger(52, 5);       // returns 2598960
     * Numbers.binomialToBigInteger(100, 50);     // returns 100891344545564193334812497256
     * Numbers.binomialToBigInteger(1000, 500);   // returns a 300-digit number
     *
     * // Edge cases
     * Numbers.binomialToBigInteger(5, 0);        // returns 1
     * Numbers.binomialToBigInteger(0, 0);        // returns 1
     * Numbers.binomialToBigInteger(5, 6);        // throws IllegalArgumentException (k > n)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) as a {@code BigInteger}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}.
     * @see #saturatedBinomial(int, int)
     * @see #binomialExact(int, int)
     * @see #binomialExactToLong(int, int)
     * @see #saturatedBinomialToLong(int, int)
     * @see #binomialToDouble(int, int)
     */
    public static BigInteger binomialToBigInteger(final int n, int k) throws IllegalArgumentException {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        N.checkArgument(k <= n, "k (%s) > n (%s)", k, n);
        if (k > (n >> 1)) {
            k = n - k;
        }
        if (k < biggestBinomials.length && n <= biggestBinomials[k]) {
            return BigInteger.valueOf(saturatedBinomialToLong(n, k));
        }

        return binomialToBigIntegerBatched(n, k, 0);
    }

    /**
     * Computes {@code C(n, k)} by batching numerator and denominator factors in {@code long} values before
     * applying them to an exact {@link BigInteger} accumulator. If {@code stopAboveBitLength} is positive,
     * the method may return an exact intermediate {@code C(n, i)} as soon as its bit length exceeds that limit.
     * This is safe for overflow detection because callers pass a symmetry-adjusted {@code k <= n / 2}, over
     * which the coefficients are non-decreasing. A zero limit computes the complete coefficient.
     */
    private static BigInteger binomialToBigIntegerBatched(final int n, final int k, final int stopAboveBitLength) {
        if (k == 0) {
            return BigInteger.ONE;
        }

        BigInteger accum = BigInteger.ONE;

        long numeratorAccum = n;
        long denominatorAccum = 1;

        final int bits = log2(n, RoundingMode.CEILING);

        int numeratorBits = bits;

        for (int i = 1; i < k; i++) {
            final int p = n - i;
            final int q = i + 1;

            // log2(p) >= bits - 1, because p >= n/2

            if (numeratorBits + bits >= Long.SIZE - 1) {
                // The numerator is as big as it can get without risking overflow.
                // Multiply numeratorAccum / denominatorAccum into accum.
                accum = accum.multiply(BigInteger.valueOf(numeratorAccum)).divide(BigInteger.valueOf(denominatorAccum));

                if (stopAboveBitLength > 0 && accum.bitLength() > stopAboveBitLength) {
                    return accum;
                }

                numeratorAccum = p;
                denominatorAccum = q;
                numeratorBits = bits;
            } else {
                // We can definitely multiply into the long accumulators without overflowing them.
                numeratorAccum *= p;
                denominatorAccum *= q;
                numeratorBits += bits;
            }
        }

        // This final flush either completes C(n, k), or gives the caller the first over-limit result
        // when the limit was crossed within the last batch.
        return accum.multiply(BigInteger.valueOf(numeratorAccum)).divide(BigInteger.valueOf(denominatorAccum));
    }

    /**
     * Returns the <a href="https://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
     * {@code values}.
     *
     * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
     * the arithmetic mean of the population.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.mean(1, 2, 3, 4, 5);        // returns 3.0
     * Numbers.mean(new int[] {10, 20});   // returns 15.0
     * Numbers.mean(-10, 0, 10);           // returns 0.0
     *
     * // Edge cases
     * Numbers.mean(5);                    // returns 5.0 (a single value)
     * Numbers.mean(new int[0]);           // throws IllegalArgumentException (no values)
     * Numbers.mean((int[]) null);         // throws NullPointerException
     * }</pre>
     *
     * <p><b>Note:</b> this method throws an {@code IllegalArgumentException} for an empty array,
     * whereas {@link N#average(int...)} returns {@code 0d} for a {@code null} or empty array.</p>
     *
     * @param values a nonempty series of values
     * @return the arithmetic mean of the values
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} is empty.
     * @see #mean(long...)
     * @see #mean(double...)
     * @see N#average(int...)
     */
    public static double mean(final int... values) throws NullPointerException, IllegalArgumentException {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");
        // The upper bound on the length of an array and the bounds on the int values mean that, in
        // this case only, we can compute the sum as a long without risking overflow or loss of
        // precision. So we do that, as it's slightly quicker than the Knuth algorithm.
        long sum = 0;
        for (final int value : values) {
            sum += value;
        }
        return (double) sum / values.length;
    }

    /**
     * Returns the <a href="https://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
     * {@code values}.
     *
     * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
     * the arithmetic mean of the population.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.mean(1L, 2L, 3L, 4L, 5L);      // returns 3.0
     * Numbers.mean(new long[] {10L, 20L});   // returns 15.0
     * Numbers.mean(-10L, 0L, 10L);           // returns 0.0
     *
     * // Edge cases
     * Numbers.mean(5L);                               // returns 5.0 (a single value)
     * Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE);   // returns 9.223372036854776E18 (the sum does not overflow)
     * Numbers.mean(Long.MIN_VALUE, Long.MIN_VALUE);   // returns -9.223372036854776E18
     * Numbers.mean(new long[0]);                      // throws IllegalArgumentException (no values)
     * Numbers.mean((long[]) null);                    // throws NullPointerException
     * }</pre>
     *
     * <p><b>Note:</b> this method throws an {@code IllegalArgumentException} for an empty array,
     * whereas {@link N#average(long...)} returns {@code 0d} for a {@code null} or empty array.</p>
     *
     * <p><b>Guarantee:</b> no intermediate sum overflows, whatever the magnitudes involved, so a series whose
     * total exceeds the {@code long} range still averages correctly rather than wrapping &mdash;
     * {@code mean(Long.MAX_VALUE, Long.MAX_VALUE)} is {@code 9.223372036854776E18}, not a negative value.
     * The returned {@code double} is still subject to ordinary binary floating-point rounding when the exact
     * mean is not representable.</p>
     *
     * @param values a nonempty series of values
     * @return the arithmetic mean of the values
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} is empty.
     * @see #mean(int...)
     * @see #mean(double...)
     * @see N#average(long...)
     */
    public static double mean(final long... values) throws NullPointerException, IllegalArgumentException {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");

        return N.average(values);
    }

    /**
     * Returns the <a href="https://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
     * {@code values}.
     *
     * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
     * the arithmetic mean of the population.
     *
     * <p><b>Guarantee:</b> every input is finite (a non-finite one is rejected), and a running sum that leaves
     * the {@code double} range does not by itself make the result infinite: values of opposite sign still
     * cancel, so {@code mean(MAX_VALUE, MAX_VALUE, -MAX_VALUE)} is about {@code 5.99E307} where the naive
     * {@code (a + b + c) / 3} is {@code Infinity}. Because the exact mean of finite values always lies between
     * the smallest and the largest of them, the result is itself always finite. Compensated summation also
     * keeps the accumulated rounding error far below that of a naive running total. The returned
     * {@code double} is nonetheless subject to ordinary binary floating-point rounding whenever the exact mean
     * is not representable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.mean(1.0, 2.0, 3.0);      // returns 2.0
     * Numbers.mean(10.5, 20.5, 30.0);   // returns 20.333... (20.333333333333332)
     *
     * // Edge cases
     * Numbers.mean(5.0);                // returns 5.0 (a single value)
     * Numbers.mean(new double[0]);      // throws IllegalArgumentException (no values)
     * Numbers.mean(1.0, Double.NaN);    // throws IllegalArgumentException (non-finite value)
     * Numbers.mean((double[]) null);    // throws NullPointerException
     * }</pre>
     *
     * <p><b>Note:</b> this method throws an {@code IllegalArgumentException} for an empty array or
     * any non-finite value, whereas {@link N#average(double...)} returns {@code 0d} for a
     * {@code null} or empty array and does not reject non-finite values.</p>
     *
     * @param values a nonempty series of finite double values
     * @return the arithmetic mean of the values
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} is empty or contains any non-finite values (NaN or
     *         infinite).
     * @see #mean(int...)
     * @see #mean(long...)
     * @see N#average(double...)
     */
    public static double mean(final double... values) throws NullPointerException, IllegalArgumentException {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");

        final KahanSummation summation = new KahanSummation();

        for (final double value : values) {
            summation.add(checkFinite(value));
        }

        return summation.average().get();
    }

    private static double checkFinite(final double argument) {
        N.checkArgument(isFinite(argument), "%s is not a finite double value", argument);
        return argument;
    }

    /**
     * Rounds {@code x} to a mathematical integer, still returned as a {@code double}, using the given mode.
     * This is the shared first step of {@link #roundToInt(double, RoundingMode)},
     * {@link #roundToLong(double, RoundingMode)} and {@link #roundToBigInteger(double, RoundingMode)}; those
     * methods add the range check for their own target type.
     *
     * @param x the value to round
     * @param mode the rounding mode to apply
     * @return {@code x} rounded to a mathematical integer, except in the truncation-safe cases where
     *         {@code x} is returned unchanged and the final truncation is left to the callers' narrowing
     *         casts: {@link RoundingMode#DOWN} (always), {@link RoundingMode#FLOOR} when {@code x >= 0},
     *         {@link RoundingMode#CEILING} when {@code x <= 0}, and {@link RoundingMode#HALF_DOWN} at an
     *         exact {@code 0.5} tie
     * @throws IllegalArgumentException if {@code mode} is {@code null}.
     * @throws ArithmeticException if {@code x} is infinite or {@code NaN}, or if {@code mode} is
     *         {@link RoundingMode#UNNECESSARY} and {@code x} is not already a mathematical integer
     */
    static double roundIntermediate(final double x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(mode, cs.mode);

        if (!isFinite(x)) {
            throw new ArithmeticException("input is infinite or NaN");
        }
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isMathematicalInteger(x));
                return x;

            case FLOOR:
                if (x >= 0.0 || isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x - 1; //NOSONAR
                }

            case CEILING:
                if (x <= 0.0 || isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x + 1; //NOSONAR
                }

            case DOWN:
                return x;

            case UP:
                if (isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x + (x > 0 ? 1 : -1); //NOSONAR
                }

            case HALF_EVEN:
                return Math.rint(x);

            case HALF_UP: {
                final double z = Math.rint(x);
                if (N.equals(abs(x - z), 0.5)) {
                    return x + Math.copySign(0.5, x);
                } else {
                    return z;
                }
            }

            case HALF_DOWN: {
                final double z = Math.rint(x);
                if (N.equals(abs(x - z), 0.5)) {
                    return x;
                } else {
                    return z;
                }
            }

            default:
                throw new AssertionError();
        }
    }

    /**
     * The scale at or below which every finite {@code double} and {@code float} rounds the same way, so the
     * {@code round} family clamps there rather than building a larger power of ten. One unit at this scale is
     * 10<sup>400</sup>, which exceeds twice {@link Double#MAX_VALUE}: every finite value is therefore strictly
     * inside the first step, and rounds either to zero (the sign restored from the input) or to
     * &plusmn;10<sup>400</sup>, which is &plusmn;{@code Infinity} as a {@code double} or {@code float} &mdash;
     * the same answer for this scale as for any deeper one. It also sits below the most negative canonical
     * decimal scale any value has ({@code -307} for a {@code double}, {@code -37} for a {@code float}), so it
     * can never pre-empt the "raising the scale is a no-op" guard above it.
     */
    private static final int MIN_EFFECTIVE_ROUNDING_SCALE = -400;

    /**
     * Rounds the given float value to the specified number of decimal places using
     * {@link RoundingMode#HALF_UP} (halfway values round away from zero), at every scale.
     *
     * <p>For finite input this is exactly equivalent to
     * {@link #round(float, int, RoundingMode) round(x, scale, RoundingMode.HALF_UP)}: the value is converted
     * via {@code new BigDecimal(Float.toString(x))}, so rounding follows the float's decimal string form
     * (e.g. {@code round(1.005f, 2)} returns {@code 1.01f}, not {@code 1.0f}). A negative scale rounds to the
     * corresponding power of ten (e.g. {@code scale == -2} rounds to the nearest hundred), as in
     * {@link BigDecimal#setScale(int, RoundingMode)}.</p>
     *
     * <p><b>Not the same as {@link #format(float, String)}:</b> {@code format} rounds the {@code float}
     * widened to {@code double} with {@code HALF_EVEN}, so {@code round(12.105f, 2)} is {@code 12.11f} while
     * {@code format(12.105f, "0.00")} is {@code "12.10"}. See the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * <p>Non-finite input never throws, at every scale: {@code NaN} and infinite values are returned
     * unchanged.</p>
     *
     * <p>A finite value can still round to {@code Infinity} when rounding up pushes the magnitude past the
     * largest finite float (e.g. {@code round(Float.MAX_VALUE, -35)} returns {@code Infinity}), consistent with
     * IEEE 754 overflow.</p>
     *
     * <p><b>This overload never throws.</b> Every {@code scale} in the {@code int} range has an answer:
     * raising the scale beyond the value's own leaves it unchanged, and lowering it far enough yields a signed
     * zero or {@code ±Infinity}. {@code HALF_UP} always has an answer, so unlike the three-argument overload
     * this one cannot throw for {@link RoundingMode#UNNECESSARY} either.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.round(3.14159f, 2);                  // returns 3.14f
     * Numbers.round(123.456f, 1);                  // returns 123.5f
     * Numbers.round(2.5f, 0);                      // returns 3.0f     (half rounds away from zero)
     * Numbers.round(-2.5f, 0);                     // returns -3.0f    (half rounds away from zero)
     * Numbers.round(1.005f, 2);                    // returns 1.01f    (decimal HALF_UP, same as the 3-arg overload)
     * Numbers.round(12345.0f, -2);                 // returns 12300.0f (negative scale rounds to a power of ten)
     *
     * // Edge cases: non-finite input is returned unchanged
     * Numbers.round(-0.004f, 2);                   // returns -0.0f    (a zero result keeps the sign of x)
     * Numbers.round(Float.NaN, 2);                 // returns NaN      (non-finite input is returned unchanged)
     * Numbers.round(Float.POSITIVE_INFINITY, 2);   // returns Infinity
     * }</pre>
     *
     * @param x the float value to be rounded
     * @param scale the number of decimal places to round to; a negative scale rounds to the
     *     corresponding power of ten (as in {@link BigDecimal#setScale(int, RoundingMode)})
     * @return the rounded float value; a finite zero result has the same sign as {@code x}, and {@code NaN} and
     *         infinite values are returned unchanged
     * @see #round(float, int, RoundingMode)
     * @see #round(double, int)
     * @see Math#round(float)
     * @see #format(float, String)
     */
    public static float round(final float x, final int scale) {
        // The non-finite passthrough lives in the 3-arg overload; repeating it here would be dead code.
        return round(x, scale, RoundingMode.HALF_UP);
    }

    /**
     * Rounds the given double value to the specified number of decimal places using
     * {@link RoundingMode#HALF_UP} (halfway values round away from zero), at every scale.
     *
     * <p>For finite input this is exactly equivalent to
     * {@link #round(double, int, RoundingMode) round(x, scale, RoundingMode.HALF_UP)}: the value is converted
     * via {@code BigDecimal.valueOf(x)}, so rounding follows the double's canonical decimal string form
     * (e.g. {@code round(1.005, 2)} returns {@code 1.01}, not {@code 1.0}). A negative scale rounds to the
     * corresponding power of ten (e.g. {@code scale == -2} rounds to the nearest hundred), as in
     * {@link BigDecimal#setScale(int, RoundingMode)}.</p>
     *
     * <p><b>Not the same as {@link #format(double, String)}:</b> {@code format} rounds the binary
     * {@code double} with {@code HALF_EVEN}, so {@code round(2.5, 0)} is {@code 3.0} while
     * {@code format(2.5, "0")} is {@code "2"}. See the
     * <a href="#decimal-format-policy">class-level {@code DecimalFormat} pattern policy</a>.</p>
     *
     * <p>Non-finite input never throws, at every scale: {@code NaN} and infinite values are returned
     * unchanged.</p>
     *
     * <p>A finite value can still round to {@code Infinity} when rounding up pushes the magnitude past the
     * largest finite double (e.g. {@code round(Double.MAX_VALUE, -308)} returns {@code Infinity}), consistent
     * with IEEE 754 overflow.</p>
     *
     * <p><b>This overload never throws.</b> Every {@code scale} in the {@code int} range has an answer:
     * raising the scale beyond the value's own leaves it unchanged, and lowering it far enough yields a signed
     * zero or {@code ±Infinity}. {@code HALF_UP} always has an answer, so unlike the three-argument overload
     * this one cannot throw for {@link RoundingMode#UNNECESSARY} either.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.round(3.14159, 2);                    // returns 3.14
     * Numbers.round(123.456, 1);                    // returns 123.5
     * Numbers.round(2.5, 0);                        // returns 3.0     (half rounds away from zero)
     * Numbers.round(-2.5, 0);                       // returns -3.0    (half rounds away from zero)
     * Numbers.round(1.005, 2);                      // returns 1.01    (decimal HALF_UP, same as the 3-arg overload)
     * Numbers.round(12345.0, -2);                   // returns 12300.0 (negative scale rounds to a power of ten)
     *
     * // Edge cases: non-finite input is returned unchanged
     * Numbers.round(-0.004, 2);                     // returns -0.0    (a zero result keeps the sign of x)
     * Numbers.round(Double.NaN, 2);                 // returns NaN     (non-finite input is returned unchanged)
     * Numbers.round(Double.NEGATIVE_INFINITY, 2);   // returns -Infinity
     * }</pre>
     *
     * @param x the double value to be rounded
     * @param scale the number of decimal places to round to; a negative scale rounds to the
     *     corresponding power of ten (as in {@link BigDecimal#setScale(int, RoundingMode)})
     * @return the rounded double value; a finite zero result has the same sign as {@code x}, and {@code NaN} and
     *         infinite values are returned unchanged
     * @see #round(double, int, RoundingMode)
     * @see #round(float, int)
     * @see Math#round(double)
     * @see #format(double, String)
     */
    public static double round(final double x, final int scale) {
        // The non-finite passthrough lives in the 3-arg overload; repeating it here would be dead code.
        return round(x, scale, RoundingMode.HALF_UP);
    }

    /**
     * Rounds the given float value to the specified number of decimal places.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.round(3.14159f, 2, RoundingMode.HALF_UP);      // returns 3.14f
     * Numbers.round(2.5f, 0, RoundingMode.HALF_UP);          // returns 3.0f
     * Numbers.round(2.5f, 0, RoundingMode.HALF_DOWN);        // returns 2.0f
     * Numbers.round(2.5f, 0, RoundingMode.CEILING);          // returns 3.0f
     * Numbers.round(-2.5f, 0, RoundingMode.FLOOR);           // returns -3.0f
     * Numbers.round(12345.0f, -2, RoundingMode.HALF_UP);     // returns 12300.0f (a negative scale is allowed)
     *
     * // Edge cases
     * Numbers.round(Float.NaN, 2, RoundingMode.HALF_UP);     // returns NaN (non-finite input is returned unchanged)
     * Numbers.round(3.14159f, 2, RoundingMode.UNNECESSARY);  // throws ArithmeticException (not exact at scale 2)
     * Numbers.round(3.14159f, 2, null);                      // throws IllegalArgumentException
     * }</pre>
     *
     * <p>The value is converted via {@code new BigDecimal(Float.toString(x))}, which preserves the float's
     * decimal string form rather than its widened binary value, so {@code round(1.005f, 2, HALF_UP)} is
     * {@code 1.01f}. This method then converts the result back to {@code float} and restores the sign of
     * {@code x} if the result is zero; {@code BigDecimal} itself has no signed zero. To keep the exact decimal
     * result instead, call {@link BigDecimal#setScale(int, RoundingMode)} on
     * {@code new BigDecimal(Float.toString(x))} directly.</p>
     *
     * <p>A finite value can round to {@code Infinity} when rounding away from zero pushes the magnitude past
     * the largest finite float (e.g. {@code round(Float.MAX_VALUE, -38, RoundingMode.UP)} returns
     * {@code Infinity}), per {@link BigDecimal#floatValue()} and IEEE 754 overflow.</p>
     *
     * @param x the float value to be rounded
     * @param scale the number of decimal places to round to; a negative scale rounds to the
     *     corresponding power of ten (as in {@link BigDecimal#setScale(int, RoundingMode)})
     * @param roundingMode the rounding mode to use; must not be {@code null}
     * @return the rounded float value; a finite zero result has the same sign as {@code x}, and {@code NaN} and
     *         infinite values are returned unchanged
     * @throws IllegalArgumentException if {@code roundingMode} is {@code null}.
     * @throws ArithmeticException if {@code roundingMode} is {@link RoundingMode#UNNECESSARY} but the value
     *         cannot be represented exactly at the requested {@code scale}. This is the only reason this method
     *         throws: every {@code scale} in the {@code int} range is answerable
     * @see #round(float, int)
     * @see #round(double, int, RoundingMode)
     * @see BigDecimal#setScale(int, RoundingMode)
     * @see BigDecimal#floatValue()
     */
    public static float round(final float x, final int scale, final RoundingMode roundingMode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(roundingMode, cs.roundingMode);

        if (!Float.isFinite(x)) {
            return x;
        }

        final BigDecimal bd = new BigDecimal(Float.toString(x));

        // See round(double, int, RoundingMode) for why these two guards are exact. A float's canonical
        // decimal scale lies in [-37, 46] (verified over all 2^32 bit patterns), so both bounds apply here
        // with even more margin than they do for a double.
        if (scale >= bd.scale()) {
            return x;
        }

        final float rounded = bd.setScale(Math.max(scale, MIN_EFFECTIVE_ROUNDING_SCALE), roundingMode).floatValue();
        return N.equals(rounded, FLOAT_POSITIVE_ZERO) ? Math.copySign(FLOAT_POSITIVE_ZERO, x) : rounded;
    }

    /**
     * Rounds the given double value to the specified number of decimal places.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.round(3.14159, 2, RoundingMode.HALF_UP);      // returns 3.14
     * Numbers.round(2.5, 0, RoundingMode.HALF_UP);          // returns 3.0
     * Numbers.round(2.5, 0, RoundingMode.HALF_DOWN);        // returns 2.0
     * Numbers.round(2.5, 0, RoundingMode.CEILING);          // returns 3.0
     * Numbers.round(-2.5, 0, RoundingMode.FLOOR);           // returns -3.0
     * Numbers.round(12345.0, -2, RoundingMode.HALF_UP);     // returns 12300.0 (a negative scale is allowed)
     *
     * // Edge cases
     * Numbers.round(Double.NaN, 2, RoundingMode.HALF_UP);   // returns NaN (non-finite input is returned unchanged)
     * Numbers.round(3.14159, 2, RoundingMode.UNNECESSARY);  // throws ArithmeticException (not exact at scale 2)
     * Numbers.round(3.14159, 2, null);                      // throws IllegalArgumentException
     * }</pre>
     *
     * <p>A finite value can round to {@code Infinity} when rounding away from zero pushes the magnitude past
     * the largest finite double (e.g. {@code round(Double.MAX_VALUE, -308, RoundingMode.UP)} returns
     * {@code Infinity}), per {@link BigDecimal#doubleValue()} and IEEE 754 overflow.</p>
     *
     * @param x the double value to be rounded
     * @param scale the number of decimal places to round to; a negative scale rounds to the
     *     corresponding power of ten (as in {@link BigDecimal#setScale(int, RoundingMode)})
     * @param roundingMode the rounding mode to use; must not be {@code null}
     * @return the rounded double value; a finite zero result has the same sign as {@code x}, and {@code NaN} and
     *         infinite values are returned unchanged
     * @throws IllegalArgumentException if {@code roundingMode} is {@code null}.
     * @throws ArithmeticException if {@code roundingMode} is {@link RoundingMode#UNNECESSARY} but the value
     *         cannot be represented exactly at the requested {@code scale}. This is the only reason this method
     *         throws: every {@code scale} in the {@code int} range is answerable
     * @see #round(double, int)
     * @see #round(float, int, RoundingMode)
     * @see BigDecimal#setScale(int, RoundingMode)
     * @see BigDecimal#doubleValue()
     */
    public static double round(final double x, final int scale, final RoundingMode roundingMode) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(roundingMode, cs.roundingMode);

        if (!Double.isFinite(x)) {
            return x;
        }

        final BigDecimal bd = BigDecimal.valueOf(x);

        // Two guards, so that a scale the value cannot use costs nothing instead of materializing
        // 10^|scale - bd.scale()|. Without them setScale did work proportional to that power before it could
        // answer: round(1.5, 10_000_000) took 2.2 s, round(1.5, 700_000_000) ran about ten MINUTES and then
        // threw, and round(1.5, Integer.MAX_VALUE) threw outright -- all for the answer 1.5. Same shape as the
        // bigDecimalToBigInteger fix: the most trivial case was the most expensive one.
        //
        // (1) Raising the scale only appends zeros. BigDecimal.valueOf(x) is exactly x's canonical decimal
        // (Double.toString round-trips), so at any scale >= its own the value is unchanged, no rounding
        // decision is taken -- UNNECESSARY therefore cannot throw -- and doubleValue() returns x's own bits,
        // -0.0 included. This holds when bd.scale() is NEGATIVE too (1e300 has scale -299): such a value is
        // already a multiple of that power of ten.
        if (scale >= bd.scale()) {
            return x;
        }

        // (2) Below MIN_EFFECTIVE_ROUNDING_SCALE every finite value behaves identically, so clamping there
        // changes no answer while bounding the divisor to 10^725.
        final double rounded = bd.setScale(Math.max(scale, MIN_EFFECTIVE_ROUNDING_SCALE), roundingMode).doubleValue();
        return N.equals(rounded, DOUBLE_POSITIVE_ZERO) ? Math.copySign(DOUBLE_POSITIVE_ZERO, x) : rounded;
    }

    /**
     * Returns a {@link DecimalFormat} for the given pattern and the current FORMAT locale, exclusive to the
     * calling thread. A change of FORMAT locale discards the thread's cache.
     *
     * @param decimalFormat the format pattern
     * @return a DecimalFormat exclusive to the current thread
     * @throws IllegalArgumentException if {@code decimalFormat} is not a valid {@link DecimalFormat} pattern.
     */
    private static DecimalFormat getThreadLocalDecimalFormat(final String decimalFormat) throws IllegalArgumentException {
        final Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        final Object[] cache = THREAD_LOCAL_DECIMAL_FORMATS.get();
        @SuppressWarnings("unchecked")
        final LinkedHashMap<String, DecimalFormat> byPattern = (LinkedHashMap<String, DecimalFormat>) cache[CACHE_FORMATS];

        if (!locale.equals(cache[CACHE_LOCALE])) {
            cache[CACHE_LOCALE] = locale;
            byPattern.clear();
        }

        DecimalFormat df = byPattern.get(decimalFormat);

        if (df == null) {
            // An invalid pattern throws here, before anything is cached, so a bad pattern is never retained.
            df = newDecimalFormat(decimalFormat, locale);
            df.setRoundingMode(RoundingMode.HALF_EVEN);
            byPattern.put(decimalFormat, df);

            if (byPattern.size() > DECIMAL_FORMAT_CACHE_CAPACITY) {
                // Access order: the first key is the least recently used one.
                final Iterator<String> eldest = byPattern.keySet().iterator();
                eldest.next();
                eldest.remove();
            }
        }

        return df;
    }

    /**
     * Builds a {@link DecimalFormat}, replacing the JDK's rejection with one whose message embeds only a
     * bounded, escaped preview of the pattern.
     *
     * <p>{@code DecimalFormat} repeats the <em>entire</em> pattern in its own message
     * ({@code Malformed pattern "..."}), unescaped, so a 100 KB pattern produced a 100 KB message and a
     * pattern containing a line break injected that break into the log line. A format pattern is
     * caller-supplied text like any parsed token, so it gets the same treatment; the cause is previewed too,
     * because {@link Throwable#printStackTrace()} prints causes.</p>
     *
     * @param pattern the format pattern
     * @param locale the FORMAT locale whose symbols to use
     * @return the new {@code DecimalFormat}
     * @throws IllegalArgumentException if {@code pattern} is not a valid {@link DecimalFormat} pattern
     */
    private static DecimalFormat newDecimalFormat(final String pattern, final Locale locale) throws IllegalArgumentException {
        try {
            return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale));
        } catch (final IllegalArgumentException e) {
            final IllegalArgumentException iae = new IllegalArgumentException("Malformed DecimalFormat pattern: " + previewForErrorMessage(pattern));
            final String causeMessage = e.getMessage();
            final String safeMessage = causeMessage == null ? null : previewForErrorMessage(causeMessage);
            iae.initCause(safeMessage == null || safeMessage.equals(causeMessage) ? e : new IllegalArgumentException(safeMessage));
            throw iae;
        }
    }

    /**
     * Returns the {@code int} value that is equal to {@code x} rounded with the specified rounding mode, if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.roundToInt(5.5, RoundingMode.UP);             // returns 6
     * Numbers.roundToInt(5.5, RoundingMode.DOWN);           // returns 5
     * Numbers.roundToInt(5.5, RoundingMode.HALF_UP);        // returns 6
     * Numbers.roundToInt(-5.5, RoundingMode.UP);            // returns -6
     * Numbers.roundToInt(-5.5, RoundingMode.DOWN);          // returns -5
     * Numbers.roundToInt(5.0, RoundingMode.UNNECESSARY);    // returns 5  (exact value)
     *
     * // Edge cases
     * Numbers.roundToInt(5.5, RoundingMode.UNNECESSARY);    // throws ArithmeticException (not a mathematical integer)
     * Numbers.roundToInt(1e20, RoundingMode.DOWN);          // throws ArithmeticException (outside int range)
     * Numbers.roundToInt(Double.NaN, RoundingMode.DOWN);    // throws ArithmeticException (infinite or NaN)
     * Numbers.roundToInt(5.0, null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to round
     * @param mode the rounding mode to apply
     * @return the rounded int value
     * @throws IllegalArgumentException if {@code mode} is {@code null}.
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
     *         mode, is either less than {@code Integer.MIN_VALUE} or greater than {@code
     *         Integer.MAX_VALUE}
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     * @see #roundToLong(double, RoundingMode)
     * @see #roundToBigInteger(double, RoundingMode)
     * @see RoundingMode
     */
    public static int roundToInt(final double x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        final double z = roundIntermediate(x, mode);
        checkRoundedInRange(z > MIN_INT_AS_DOUBLE - 1.0 && z < MAX_INT_AS_DOUBLE + 1.0, x, z, mode);
        return (int) z;
    }

    /**
     * Returns the {@code long} value that is equal to {@code x} rounded with the specified rounding mode, if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.roundToLong(5.5, RoundingMode.UP);             // returns 6L
     * Numbers.roundToLong(5.5, RoundingMode.DOWN);           // returns 5L
     * Numbers.roundToLong(5.5, RoundingMode.HALF_UP);        // returns 6L
     * Numbers.roundToLong(-5.5, RoundingMode.UP);            // returns -6L
     * Numbers.roundToLong(-5.5, RoundingMode.DOWN);          // returns -5L
     * Numbers.roundToLong(5.0, RoundingMode.UNNECESSARY);    // returns 5L  (exact value)
     *
     * // Edge cases
     * Numbers.roundToLong(5.5, RoundingMode.UNNECESSARY);    // throws ArithmeticException (not a mathematical integer)
     * Numbers.roundToLong(1e30, RoundingMode.DOWN);          // throws ArithmeticException (outside long range)
     * Numbers.roundToLong(Double.NaN, RoundingMode.DOWN);    // throws ArithmeticException (infinite or NaN)
     * Numbers.roundToLong(5.0, null);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to round
     * @param mode the rounding mode to apply
     * @return the rounded long value
     * @throws IllegalArgumentException if {@code mode} is {@code null}.
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
     *         mode, is either less than {@code Long.MIN_VALUE} or greater than {@code
     *         Long.MAX_VALUE}
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     * @see #roundToInt(double, RoundingMode)
     * @see #roundToBigInteger(double, RoundingMode)
     * @see RoundingMode
     */
    public static long roundToLong(final double x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        final double z = roundIntermediate(x, mode);
        checkRoundedInRange(MIN_LONG_AS_DOUBLE - z < 1.0 && z < MAX_LONG_AS_DOUBLE_PLUS_ONE, x, z, mode);
        return (long) z;
    }

    /**
     * Returns the {@code BigInteger} value that is equal to {@code x} rounded with the specified rounding mode, if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.roundToBigInteger(5.5, RoundingMode.UP);            // returns BigInteger.valueOf(6)
     * Numbers.roundToBigInteger(5.5, RoundingMode.DOWN);          // returns BigInteger.valueOf(5)
     * Numbers.roundToBigInteger(5.5, RoundingMode.HALF_UP);       // returns BigInteger.valueOf(6)
     * Numbers.roundToBigInteger(-5.5, RoundingMode.UP);           // returns BigInteger.valueOf(-6)
     * Numbers.roundToBigInteger(5.0, RoundingMode.UNNECESSARY);   // returns BigInteger.valueOf(5)
     * Numbers.roundToBigInteger(1e20, RoundingMode.DOWN);         // returns 100000000000000000000 (no range limit)
     *
     * // Edge cases
     * Numbers.roundToBigInteger(5.5, RoundingMode.UNNECESSARY);   // throws ArithmeticException (not a mathematical integer)
     * Numbers.roundToBigInteger(Double.NaN, RoundingMode.DOWN);   // throws ArithmeticException (infinite or NaN)
     * Numbers.roundToBigInteger(5.0, null);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the value to round
     * @param mode the rounding mode to apply
     * @return the rounded BigInteger value
     * @throws IllegalArgumentException if {@code mode} is {@code null}.
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     * @see #roundToInt(double, RoundingMode)
     * @see #roundToLong(double, RoundingMode)
     * @see RoundingMode
     */
    public static BigInteger roundToBigInteger(double x, final RoundingMode mode) throws IllegalArgumentException, ArithmeticException {
        // #roundIntermediate, java.lang.Math.getExponent, com.google.common.math.DoubleUtils
        x = roundIntermediate(x, mode);
        if (MIN_LONG_AS_DOUBLE - x < 1.0 && x < MAX_LONG_AS_DOUBLE_PLUS_ONE) {
            return BigInteger.valueOf((long) x);
        }
        final int exponent = getExponent(x);
        final long significand = getSignificand(x);
        final BigInteger result = BigInteger.valueOf(significand).shiftLeft(exponent - SIGNIFICAND_BITS);
        return (x < 0) ? result.negate() : result;
    }

    /**
     * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
     *
     * <p>Technically speaking, this is equivalent to
     * {@code Math.abs(a - b) <= tolerance || Float.valueOf(a).equals(Float.valueOf(b))}.
     *
     * <p>Notable special cases include:
     * <ul>
     * <li>All NaNs are fuzzily equal.
     * <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
     * <li>Positive and negative zero are always fuzzily equal.
     * <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then {@code a}
     *     and {@code b} are fuzzily equal if and only if {@code a == b}.
     * <li>With {@link Float#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
     * <li>With finite tolerance, {@code Float.POSITIVE_INFINITY} and {@code
     *     Float.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
     * </ul>
     *
     * <p>This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
     * equivalence relation and <em>not</em> suitable for use in {@link Object#equals}
     * implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.fuzzyEquals(1.0001f, 1.0002f, 0.001f);             // returns true  (within tolerance)
     * Numbers.fuzzyEquals(1.0f, 1.1f, 0.01f);                    // returns false (exceeds tolerance)
     *
     * // Edge cases
     * Numbers.fuzzyEquals(0.0f, -0.0f, 0.0f);                    // returns true  (positive and negative zero are equal)
     * Numbers.fuzzyEquals(Float.NaN, Float.NaN, 0.1f);           // returns true  (all NaNs are fuzzily equal)
     * Numbers.fuzzyEquals(Float.NaN, 1.0f, 0.1f);                // returns false (NaN equals only NaN)
     * Numbers.fuzzyEquals(1.0f, 1e20f, Float.POSITIVE_INFINITY); // returns true  (infinite tolerance)
     * Numbers.fuzzyEquals(1.0f, 1.0f, -0.1f);                    // throws IllegalArgumentException (negative tolerance)
     * }</pre>
     *
     * @param a the first float value to compare
     * @param b the second float value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal; must be non-negative
     * @return {@code true} if the absolute difference between {@code a} and {@code b} is less than or equal to {@code tolerance},
     *         or if both are NaN; {@code false} otherwise
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN.
     * @see #fuzzyEquals(double, double, double)
     * @see #fuzzyCompare(float, float, float)
     * @see Float#compare(float, float)
     */
    public static boolean fuzzyEquals(final float a, final float b, final float tolerance) throws IllegalArgumentException {
        // Check that tolerance is valid (non-negative and not NaN)
        if (tolerance < 0.0 || Float.isNaN(tolerance)) {
            throw new IllegalArgumentException("tolerance must be non-negative and not NaN");
        }

        return Math.copySign(a - b, 1.0f) <= tolerance // branch-free abs; NaN of (a-b) is not <= tolerance
                || N.equals(a, b); // infinities and NaNs (N.equals uses compare, unlike ==)
    }

    /**
     * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
     *
     * <p>Technically speaking, this is equivalent to
     * {@code Math.abs(a - b) <= tolerance || Double.valueOf(a).equals(Double.valueOf(b))}.
     *
     * <p>Notable special cases include:
     * <ul>
     * <li>All NaNs are fuzzily equal.
     * <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
     * <li>Positive and negative zero are always fuzzily equal.
     * <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then {@code a}
     *     and {@code b} are fuzzily equal if and only if {@code a == b}.
     * <li>With {@link Double#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
     * <li>With finite tolerance, {@code Double.POSITIVE_INFINITY} and {@code
     *     Double.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
     * </ul>
     *
     * <p>This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
     * equivalence relation and <em>not</em> suitable for use in {@link Object#equals}
     * implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.fuzzyEquals(1.0001, 1.0002, 0.001);                   // returns true  (within tolerance)
     * Numbers.fuzzyEquals(1.0, 1.1, 0.01);                          // returns false (exceeds tolerance)
     *
     * // Edge cases
     * Numbers.fuzzyEquals(0.0, -0.0, 0.0);                          // returns true  (positive and negative zero are equal)
     * Numbers.fuzzyEquals(Double.NaN, Double.NaN, 0.1);             // returns true  (all NaNs are fuzzily equal)
     * Numbers.fuzzyEquals(Double.NaN, 1.0, 0.1);                    // returns false (NaN equals only NaN)
     * Numbers.fuzzyEquals(1.0, 1e300, Double.POSITIVE_INFINITY);    // returns true  (infinite tolerance)
     * Numbers.fuzzyEquals(1.0, 1.0, -0.1);                          // throws IllegalArgumentException (negative tolerance)
     * }</pre>
     *
     * @param a the first double value to compare
     * @param b the second double value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal; must be non-negative
     * @return {@code true} if the absolute difference between {@code a} and {@code b} is less than or equal to {@code tolerance},
     *         or if both are NaN; {@code false} otherwise
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN.
     * @see #fuzzyEquals(float, float, float)
     * @see #fuzzyCompare(double, double, double)
     * @see Double#compare(double, double)
     */
    public static boolean fuzzyEquals(final double a, final double b, final double tolerance) throws IllegalArgumentException {
        // Check that tolerance is valid (non-negative and not NaN)
        if (tolerance < 0.0 || Double.isNaN(tolerance)) {
            throw new IllegalArgumentException("tolerance must be non-negative and not NaN");
        }

        return Math.copySign(a - b, 1.0) <= tolerance // branch-free abs; NaN of (a-b) is not <= tolerance
                || N.equals(a, b); // infinities and NaNs (N.equals uses compare, unlike ==)
    }

    /**
     * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly equal values.
     *
     * <p>This method is equivalent to
     * {@code Numbers.fuzzyEquals(a, b, tolerance) ? 0 : Float.compare(a, b)}. In particular, like
     * {@link Float#compare(float, float)}, it treats all NaN values as equal and greater than all
     * other values (including {@link Float#POSITIVE_INFINITY}).
     *
     * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
     * {@link Comparable#compareTo} implementations. In particular, it is not transitive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.fuzzyCompare(1.0f, 1.0001f, 0.001f);        // returns 0   (fuzzily equal)
     * Numbers.fuzzyCompare(1.0f, 2.0f, 0.1f);             // returns -1  (1.0 is less than 2.0)
     * Numbers.fuzzyCompare(2.0f, 1.0f, 0.1f);             // returns 1   (2.0 is greater than 1.0)
     *
     * // Edge cases
     * Numbers.fuzzyCompare(Float.NaN, Float.NaN, 0.1f);   // returns 0   (NaN values are equal)
     * Numbers.fuzzyCompare(Float.NaN, 1.0f, 0.1f);        // returns 1   (NaN is greater than all other values)
     * Numbers.fuzzyCompare(1.0f, 2.0f, -0.1f);            // throws IllegalArgumentException (negative tolerance)
     * }</pre>
     *
     * @param a the first float value to compare
     * @param b the second float value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal
     * @return {@code 0} if {@code a} and {@code b} are fuzzily equal, a negative integer if {@code a} is less than {@code b},
     *         or a positive integer if {@code a} is greater than {@code b}
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN.
     * @see #fuzzyEquals(float, float, float)
     * @see #fuzzyCompare(double, double, double)
     */
    public static int fuzzyCompare(final float a, final float b, final float tolerance) throws IllegalArgumentException {
        if (fuzzyEquals(a, b, tolerance)) {
            return 0;
        } else if (a < b) {
            return -1;
        } else if (a > b) {
            return 1;
        } else {
            return Boolean.compare(Float.isNaN(a), Float.isNaN(b));
        }
    }

    /**
     * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly equal values.
     *
     * <p>This method is equivalent to
     * {@code Numbers.fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a, b)}. In particular, like
     * {@link Double#compare(double, double)}, it treats all NaN values as equal and greater than all
     * other values (including {@link Double#POSITIVE_INFINITY}).
     *
     * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
     * {@link Comparable#compareTo} implementations. In particular, it is not transitive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.fuzzyCompare(1.0, 1.0001, 0.001);            // returns 0   (fuzzily equal)
     * Numbers.fuzzyCompare(1.0, 2.0, 0.1);                 // returns -1  (1.0 is less than 2.0)
     * Numbers.fuzzyCompare(2.0, 1.0, 0.1);                 // returns 1   (2.0 is greater than 1.0)
     *
     * // Edge cases
     * Numbers.fuzzyCompare(Double.NaN, Double.NaN, 0.1);   // returns 0   (NaN values are equal)
     * Numbers.fuzzyCompare(Double.NaN, 1.0, 0.1);          // returns 1   (NaN is greater than all other values)
     * Numbers.fuzzyCompare(1.0, 2.0, -0.1);                // throws IllegalArgumentException (negative tolerance)
     * }</pre>
     *
     * @param a the first double value to compare
     * @param b the second double value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal
     * @return {@code 0} if {@code a} and {@code b} are fuzzily equal, a negative integer if {@code a} is less than {@code b},
     *         or a positive integer if {@code a} is greater than {@code b}
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN.
     * @see #fuzzyEquals(double, double, double)
     * @see #fuzzyCompare(float, float, float)
     */
    public static int fuzzyCompare(final double a, final double b, final double tolerance) throws IllegalArgumentException {
        if (fuzzyEquals(a, b, tolerance)) {
            return 0;
        } else if (a < b) {
            return -1;
        } else if (a > b) {
            return 1;
        } else {
            return Boolean.compare(Double.isNaN(a), Double.isNaN(b));
        }
    }

    /**
     * Returns {@code true} if {@code x} represents a mathematical integer.
     *
     * <p>Equivalent to {@code !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x)}, which is also
     * how it is implemented.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.isMathematicalInteger(5.0);                        // returns true
     * Numbers.isMathematicalInteger(-3.0);                       // returns true
     * Numbers.isMathematicalInteger(5.5);                        // returns false
     * Numbers.isMathematicalInteger(1e300);                      // returns true  (every large double is an integer)
     *
     * // Edge cases
     * Numbers.isMathematicalInteger(0.0);                        // returns true
     * Numbers.isMathematicalInteger(-0.0);                       // returns true  (signed zero is an integer)
     * Numbers.isMathematicalInteger(Double.NaN);                 // returns false
     * Numbers.isMathematicalInteger(Double.POSITIVE_INFINITY);   // returns false
     * }</pre>
     *
     * @param x the value to check
     * @return {@code true} if {@code x} is finite and represents an integer value, {@code false} otherwise
     * @see Math#rint(double)
     */
    public static boolean isMathematicalInteger(final double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x);
    }

    /**
     * Returns 1 if {@code x < y} as unsigned longs, and 0 otherwise. Assumes that x - y fits into a
     * signed long. The implementation is branch-free, and benchmarks suggest it is measurably faster
     * than the straightforward ternary expression.
     *
     * @param x the first value
     * @param y the second value
     * @return 1 if x is less than y as unsigned longs, 0 otherwise
     */
    private static int lessThanBranchFree(final long x, final long y) {
        // Returns the sign bit of x - y.
        return (int) ((x - y) >>> (Long.SIZE - 1));
    }

    /**
     * Returns {@code floor(log10(x))} for a positive {@code x}. The caller is responsible for checking that
     * {@code x > 0}; the result is unspecified otherwise.
     *
     * @param x the value to take the base-10 logarithm of; must be positive
     * @return the largest {@code n} such that {@code 10^n <= x}
     */
    private static int log10Floor(final long x) {
        /*
         * Based on Hacker's Delight Fig. 11-5, the two-table-lookup, branch-free implementation.
         *
         * The key idea is that based on the number of leading zeros (equivalently, floor(log2(x))), we
         * can narrow the possible floor(log10(x)) values to two. For example, if floor(log2(x)) is 6,
         * then 64 <= x < 128, so floor(log10(x)) is either 1 or 2.
         */
        final int y = maxLog10ForLeadingZeros[Long.numberOfLeadingZeros(x)];
        /*
         * y is the higher of the two possible values of floor(log10(x)). If x < 10^y, then we want the
         * lower of the two possible values, or y - 1, otherwise, we want y.
         */
        return y - lessThanBranchFree(x, powersOf10[y]);
    }

    /**
     * Returns (x * numerator / denominator), which is assumed to come out to an integral value.
     *
     * @param x the multiplicand
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction
     * @return the result of x * numerator / denominator
     */
    private static long multiplyFraction(long x, final long numerator, long denominator) {
        if (x == 1) {
            return numerator / denominator;
        }
        final long commondivisor = gcd(x, denominator);
        x /= commondivisor;
        denominator /= commondivisor; //NOSONAR
        // We know gcd(x, denominator) = 1, and x * numerator / denominator is exact,
        // so denominator must be a divisor of numerator.
        return x * (numerator / denominator); //NOSONAR
    }

    /**
     * Returns the significand (mantissa) of {@code d} as a {@code long}, with the implicit leading bit
     * restored for a normal value and the bits shifted left by one for a subnormal value.
     *
     * @param d the value to take the significand of; must be finite
     * @return the significand of {@code d}
     * @throws IllegalArgumentException if {@code d} is infinite or {@code NaN}.
     */
    private static long getSignificand(final double d) throws IllegalArgumentException {
        N.checkArgument(isFinite(d), "not a finite value");
        final int exponent = getExponent(d);
        long bits = doubleToRawLongBits(d);
        bits &= SIGNIFICAND_MASK;
        return (exponent == MIN_EXPONENT - 1) ? bits << 1 : bits | IMPLICIT_BIT;
    }

    /**
     * Returns {@code true} if {@code d} is neither infinite nor {@code NaN}.
     *
     * @param d the value to test
     * @return {@code true} if {@code d} is a finite value
     */
    private static boolean isFinite(final double d) {
        return Double.isFinite(d);
    }

    /**
     * Returns {@code true} if {@code d} is a normal (i.e. non-zero, non-subnormal, finite) value.
     *
     * @param d the value to test
     * @return {@code true} if {@code d} is normal
     */
    static boolean isNormal(final double d) {
        return isFinite(d) && getExponent(d) >= MIN_EXPONENT;
    }

    /**
     * Returns {@code x} scaled by a power of two so that the result lies in {@code [1, 2)}. Assumes
     * {@code x} is positive, normal and finite; the result is unspecified otherwise.
     *
     * @param x the value to scale
     * @return {@code x} scaled into {@code [1, 2)}
     */
    private static double scaleNormalize(final double x) {
        final long significand = doubleToRawLongBits(x) & SIGNIFICAND_MASK;
        return longBitsToDouble(significand | ONE_BITS);
    }

    /**
     * Returns 1 if {@code x < y} as signed ints, and 0 otherwise. Assumes that {@code x - y} does not
     * overflow. The implementation is branch-free.
     *
     * @param x the first value
     * @param y the second value
     * @return 1 if {@code x} is less than {@code y}, 0 otherwise
     */
    private static int lessThanBranchFree(final int x, final int y) {
        // Returns the sign bit of x - y.
        return (x - y) >>> (Integer.SIZE - 1);
    }

    /**
     * Returns {@code true} if {@code x} is exactly representable as an {@code int}, i.e. if casting it to
     * {@code int} loses no information.
     *
     * @param x the value to test
     * @return {@code true} if {@code x} fits in an {@code int}, {@code false} otherwise
     */
    private static boolean fitsInInt(final long x) {
        return (int) x == x;
    }

    /**
     * Returns {@code x} if it is positive; otherwise throws.
     *
     * @param role the parameter name to report in the exception message
     * @param x the value to check
     * @return {@code x}
     * @throws IllegalArgumentException if {@code x <= 0}.
     */
    private static int checkPositive(final String role, final int x) throws IllegalArgumentException {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Returns {@code x} if it is positive; otherwise throws.
     *
     * @param role the parameter name to report in the exception message
     * @param x the value to check
     * @return {@code x}
     * @throws IllegalArgumentException if {@code x <= 0}.
     */
    private static long checkPositive(final String role, final long x) throws IllegalArgumentException {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Returns {@code x} if it is positive; otherwise throws.
     *
     * @param role the parameter name to report in the exception message
     * @param x the value to check; must not be {@code null}
     * @return {@code x}
     * @throws IllegalArgumentException if {@code x <= 0}.
     */
    private static BigInteger checkPositive(final String role, final BigInteger x) throws IllegalArgumentException {
        if (x.signum() <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Returns {@code x} if it is non-negative; otherwise throws.
     *
     * @param role the parameter name to report in the exception message
     * @param x the value to check
     * @return {@code x}
     * @throws IllegalArgumentException if {@code x < 0}.
     */
    private static int checkNonNegative(final String role, final int x) throws IllegalArgumentException {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Returns {@code x} if it is non-negative; otherwise throws.
     *
     * @param role the parameter name to report in the exception message
     * @param x the value to check
     * @return {@code x}
     * @throws IllegalArgumentException if {@code x < 0}.
     */
    private static long checkNonNegative(final String role, final long x) throws IllegalArgumentException {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Returns {@code x} if it is non-negative; otherwise throws.
     *
     * @param role the parameter name to report in the exception message
     * @param x the value to check; must not be {@code null}
     * @return {@code x}
     * @throws IllegalArgumentException if {@code x < 0}.
     */
    private static BigInteger checkNonNegative(final String role, final BigInteger x) throws IllegalArgumentException {
        if (x.signum() < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Throws if {@code condition} is {@code false}, reporting that {@link RoundingMode#UNNECESSARY} was
     * requested for a value that cannot be represented exactly.
     *
     * @param condition {@code true} if no rounding is needed
     * @throws ArithmeticException if {@code condition} is {@code false}
     */
    private static void checkRoundingUnnecessary(final boolean condition) throws ArithmeticException {
        if (!condition) {
            throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        }
    }

    /**
     * Throws if {@code condition} is {@code false}, reporting the value the caller supplied, the rounded
     * intermediate that was range-checked and the rounding mode that produced it. All three are needed because
     * an in-range {@code x} can round out of range under one mode and not another.
     *
     * @param condition {@code true} if the rounded value is in range
     * @param x the value the caller passed in
     * @param z {@code x} rounded with {@code mode} - the value that was range-checked
     * @param mode the rounding mode applied to {@code x}
     * @throws ArithmeticException if {@code condition} is {@code false}
     */
    private static void checkRoundedInRange(final boolean condition, final double x, final double z, final RoundingMode mode) throws ArithmeticException {
        if (!condition) {
            throw new ArithmeticException("not in range: " + x + " rounded to " + z + " with rounding mode " + mode);
        }
    }

    /**
     * Throws if {@code condition} is {@code false}, lazily constructing a
     * {@link #powExact(int, int)}/{@link #powExact(long, int)} overflow message. Primitive operands keep successful
     * exponentiation paths allocation-free.
     *
     * @param condition {@code true} if no overflow occurred
     * @param base the base of the overflowing exponentiation
     * @param exponent the exponent of the overflowing exponentiation
     * @throws ArithmeticException if {@code condition} is {@code false}
     */
    private static void checkNoOverflow(final boolean condition, final long base, final int exponent) throws ArithmeticException {
        if (!condition) {
            throw new ArithmeticException("powExact(" + base + ", " + exponent + ") overflow");
        }
    }

    /**
     * Computes the inverse hyperbolic sine (arcsinh) of a number.
     *
     * <p>The inverse hyperbolic sine is defined as: {@code asinh(x) = ln(x + sqrt(x² + 1))}.
     * This function is the inverse of the hyperbolic sine function {@code sinh}: mathematically
     * {@code sinh(asinh(x)) = x} for all real {@code x}; in floating-point the identity holds only
     * approximately.
     *
     * <p><b>Accuracy:</b> the result is within 2 ulp of the exact value over the whole domain (measured
     * against a 60-digit reference on 70,000 points from 2<sup>-20</sup> to 2<sup>300</sup>; typical error
     * is a third of an ulp). It is computed as {@code log1p(|a| + a^2 / (1 + sqrt(1 + a^2)))}, which is exact
     * in the limit at both ends: for a tiny {@code a} it reduces to {@code log1p(a) = a}, and for a huge one to
     * {@code log1p(2|a|)}.</p>
     *
     * <p>Mathematical properties:
     * <ul>
     *   <li>{@code Numbers.asinh(-x) = -Numbers.asinh(x)} (odd function; signed zero is preserved)</li>
     *   <li>{@code Numbers.asinh(0) = 0}; {@code Numbers.asinh(-0.0) = -0.0}</li>
     *   <li>Domain: all real numbers (-∞, +∞)</li>
     *   <li>Range: all real numbers (-∞, +∞)</li>
     *   <li>{@code Numbers.asinh(Double.POSITIVE_INFINITY)} returns {@link Double#POSITIVE_INFINITY};
     *       {@code Numbers.asinh(Double.NEGATIVE_INFINITY)} returns {@link Double#NEGATIVE_INFINITY}</li>
     *   <li>a {@code NaN} input returns {@code NaN}</li>
     * </ul>
     *
     * <p>For very large finite magnitudes, this implementation uses the asymptotically equivalent
     * {@code log(|a|) + log(2)} form so that squaring {@code a} cannot overflow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.asinh(1.0);    // returns ~0.88137
     * Numbers.asinh(-1.0);   // returns ~-0.88137
     * Numbers.asinh(10.0);   // returns ~2.99822
     *
     * // Edge cases: the domain is all of the reals, so this method never throws
     * Numbers.asinh(0.0);                         // returns 0.0
     * Numbers.asinh(-0.0);                        // returns -0.0 (signed zero is preserved)
     * Numbers.asinh(Double.POSITIVE_INFINITY);    // returns Infinity
     * Numbers.asinh(Double.NaN);                  // returns NaN
     * }</pre>
     *
     * @param a the number on which to compute the inverse hyperbolic sine
     * @return the inverse hyperbolic sine of {@code a}
     * @see #acosh(double)
     * @see #atanh(double)
     */
    public static double asinh(final double a) {
        final double abs = Math.abs(a);
        final double absAsinh;

        if (abs >= SQRT_MAX_DOUBLE) {
            absAsinh = Math.log(abs) + LN_2;
        } else {
            // One log1p formulation for the whole finite range. This replaced a four-branch Taylor series
            // below 0.167 and log(sqrt(a*a + 1) + a) above it: that log form loses the leading bits when
            // sqrt(a*a + 1) + a is close to 1 and measured up to 8 ulp off (at a = 0.2478, against a
            // 60-digit reference), where log1p of the same quantity minus one is within 1.5 ulp. The
            // rewritten argument a + a^2 / (1 + sqrt(1 + a^2)) is sqrt(1 + a^2) + a - 1 computed without
            // cancellation, so the series branches are no longer needed for accuracy at small a either:
            // for a below 2^-27 the second term vanishes and log1p(a) is a, and a subnormal a squares to
            // zero, so asinh(a) == a exactly there, as it should. NaN propagates (NaN >= x is false, and
            // every operation below preserves it).
            absAsinh = Math.log1p(abs + abs * abs / (1 + Math.sqrt(1 + abs * abs)));
        }

        return Math.copySign(absAsinh, a);
    }

    /**
     * Computes the inverse hyperbolic cosine (arccosh) of a number.
     *
     * <p>The inverse hyperbolic cosine is defined as: {@code acosh(x) = ln(x + sqrt(x² - 1))}.
     * This function is the inverse of the hyperbolic cosine function {@code cosh}: mathematically
     * {@code cosh(acosh(x)) = x} for all {@code x >= 1}; in floating-point the identity holds only
     * approximately.
     *
     * <p>Mathematical properties:
     * <ul>
     *   <li>{@code Numbers.acosh(1) = 0}</li>
     *   <li>Domain: [1, +∞) (requires {@code x >= 1})</li>
     *   <li>Range: [0, +∞)</li>
     *   <li>For {@code x < 1}, the result is NaN</li>
     *   <li>{@code Double.POSITIVE_INFINITY} input returns {@link Double#POSITIVE_INFINITY}</li>
     *   <li>a {@code NaN} input returns {@code NaN}</li>
     * </ul>
     *
     * <p>For very large finite inputs, this implementation uses the asymptotically equivalent
     * {@code log(a) + log(2)} form so that squaring {@code a} cannot overflow. For other inputs it
     * uses {@code log1p((a - 1) + sqrt((a - 1) * (a + 1)))} to retain accuracy close to {@code 1}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.acosh(2.0);    // returns ~1.31696
     * Numbers.acosh(10.0);   // returns ~2.99322
     *
     * // Edge cases: outside the domain the result is NaN; this method never throws
     * Numbers.acosh(1.0);                         // returns 0.0 (the domain starts at 1)
     * Numbers.acosh(0.5);                         // returns NaN (outside the domain)
     * Numbers.acosh(Double.POSITIVE_INFINITY);    // returns Infinity
     * Numbers.acosh(Double.NaN);                  // returns NaN
     * }</pre>
     *
     * @param a the number on which to compute the inverse hyperbolic cosine; values &lt; 1 return {@code NaN}
     * @return the inverse hyperbolic cosine of {@code a}, or NaN if {@code a < 1} or {@code a} is NaN
     * @see #asinh(double)
     * @see #atanh(double)
     */
    public static double acosh(final double a) {
        if (Double.isNaN(a) || a < 1.0d) {
            return Double.NaN;
        }

        if (a >= SQRT_MAX_DOUBLE) {
            return Math.log(a) + LN_2;
        }

        return Math.log1p((a - 1) + Math.sqrt((a - 1) * (a + 1)));
    }

    /**
     * Computes the inverse hyperbolic tangent (arctanh) of a number.
     *
     * <p>The inverse hyperbolic tangent is defined as: {@code atanh(x) = 0.5 * ln((1 + x) / (1 - x))}.
     * This function is the inverse of the hyperbolic tangent function {@code tanh}: mathematically
     * {@code tanh(atanh(x)) = x} for {@code -1 < x < 1}; in floating-point the identity holds only
     * approximately.
     *
     * <p><b>Accuracy:</b> the result is within 2 ulp of the exact value over the whole open domain
     * (measured against a 60-digit reference on 70,000 points, including arguments within 2<sup>-40</sup>
     * of {@code ±1}; typical error is a third of an ulp). It is computed as
     * {@code 0.5 * log1p(2|a| / (1 - |a|))}, which is exact in the limit at both ends: for a tiny {@code a}
     * it reduces to {@code a}, and at {@code |a| = 1} the argument is infinite.</p>
     *
     * <p>Mathematical properties and edge cases:
     * <ul>
     *   <li>{@code Numbers.atanh(-x) = -Numbers.atanh(x)} (odd function; signed zero is preserved)</li>
     *   <li>{@code Numbers.atanh(0) = 0}; {@code Numbers.atanh(-0.0) = -0.0}</li>
     *   <li>Open domain {@code (-1, 1)}; range {@code (-∞, +∞)}</li>
     *   <li>{@code atanh(1.0)} returns {@link Double#POSITIVE_INFINITY}; {@code atanh(-1.0)} returns
     *       {@link Double#NEGATIVE_INFINITY}</li>
     *   <li>{@code |x| > 1} returns {@code NaN}</li>
     *   <li>a {@code NaN} input returns {@code NaN}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Numbers.atanh(0.5);    // returns ~0.54931
     * Numbers.atanh(-0.5);   // returns ~-0.54931
     * Numbers.atanh(0.9);    // returns ~1.47222
     *
     * // Edge cases: outside the domain the result is NaN; this method never throws
     * Numbers.atanh(0.0);                    // returns 0.0
     * Numbers.atanh(-0.0);                   // returns -0.0 (signed zero is preserved)
     * Numbers.atanh(1.0);                    // returns Double.POSITIVE_INFINITY
     * Numbers.atanh(-1.0);                   // returns Double.NEGATIVE_INFINITY
     * Numbers.atanh(1.5);                    // returns NaN (outside the domain)
     * Numbers.atanh(Double.NaN);             // returns NaN
     * }</pre>
     *
     * @param a the number on which to compute the inverse hyperbolic tangent
     * @return the inverse hyperbolic tangent of {@code a}; {@code ±Infinity} at {@code ±1},
     *         {@code NaN} when {@code |a| > 1} or {@code a} is {@code NaN}
     * @see #asinh(double)
     * @see #acosh(double)
     */
    public static double atanh(final double a) {
        final double abs = Math.abs(a);

        // One log1p formulation for the whole domain. This replaced a four-branch Taylor series below 0.15
        // and 0.5 * log((1 + a) / (1 - a)) above it: that quotient form measured up to 3.8 ulp off (at
        // a = 0.16, against a 60-digit reference) because (1 + a) / (1 - a) is rounded before the log,
        // whereas log1p(2a / (1 - a)) -- the same quantity minus one -- stays within 1.8 ulp, and is at
        // least as accurate as the series on the small arguments it covered. The special values fall out of
        // the arithmetic: |a| = 1 makes the argument +Infinity, so the result is +-Infinity; |a| > 1 makes it
        // less than -1, so log1p returns NaN; a NaN or infinite a propagates NaN; and a signed zero is
        // restored by copySign.
        return Math.copySign(0.5 * Math.log1p(2 * abs / (1 - abs)), a);
    }

    /**
     * Helpers that reinterpret a {@code long} as an unsigned 64-bit quantity, used by the Miller-Rabin
     * primality test in {@link Numbers#isPrime(long)}.
     */
    private static final class UnsignedLongs {

        private UnsignedLongs() {
        }

        /**
         * Compares the two specified {@code long} values, treating them as unsigned values between
         * {@code 0} and {@code 2^64 - 1} inclusive.
         *
         * @param a the first unsigned {@code long} to compare
         * @param b the second unsigned {@code long} to compare
         * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
         *     greater than {@code b}; or zero if they are equal
         */
        static int compare(final long a, final long b) {
            return Long.compareUnsigned(a, b);
        }

        /**
         * Returns {@code dividend % divisor}, where the dividend and divisor are treated as unsigned
         * 64-bit quantities.
         *
         * @param dividend the dividend (numerator)
         * @param divisor the divisor (denominator)
         * @return the remainder of {@code dividend} divided by {@code divisor}
         * @throws ArithmeticException if {@code divisor} is 0
         */
        static long remainder(final long dividend, final long divisor) throws ArithmeticException {
            if (divisor < 0) { // i.e., divisor >= 2^63:
                if (compare(dividend, divisor) < 0) {
                    return dividend; // dividend < divisor
                } else {
                    return dividend - divisor; // dividend >= divisor
                }
            }

            // Optimization - use signed modulus if dividend < 2^63
            if (dividend >= 0) {
                return dividend % divisor;
            }

            /*
             * Otherwise, approximate the quotient, check, and correct if necessary. Our approximation is
             * guaranteed to be either exact or one less than the correct value. This follows from the fact
             * that floor(floor(x)/i) == floor(x/i) for any real x and integer i != 0. The proof is not
             * quite trivial.
             */
            final long quotient = ((dividend >>> 1) / divisor) << 1;
            final long rem = dividend - quotient * divisor;
            return rem - (compare(rem, divisor) >= 0 ? divisor : 0);
        }

    }

    /**
     * Miller-Rabin strong-probable-prime testers used by {@link Numbers#isPrime(long)}. {@link #SMALL}
     * uses plain {@code long} multiplication when {@code n <= FLOOR_SQRT_MAX_LONG}; {@link #LARGE}
     * performs the modular arithmetic over unsigned 64-bit values (via {@link UnsignedLongs}) for
     * larger {@code n}.
     */
    private enum MillerRabinTester {
        SMALL {
            @Override
            long mulMod(final long a, final long b, final long m) {
                /*
                 * NOTE(lowasser, 2015-Feb-12): Benchmarks suggest that changing this to
                 * UnsignedLongs.remainder and increasing the threshold to 2^32 doesn't pay for itself, and
                 * adding another enum constant hurts performance further -- I suspect because bimorphic
                 * implementation is a sweet spot for the JVM.
                 */
                return (a * b) % m;
            }

            @Override
            long squareMod(final long a, final long m) {
                return (a * a) % m;
            }
        },
        LARGE {
            /**
             * Returns (a + b) mod m. Precondition: 0 &lt;= a, b &lt; m &lt; 2^63.
             */
            private long plusMod(final long a, final long b, final long m) {
                return (a >= m - b) ? (a + b - m) : (a + b);
            }

            /**
             * Returns (a * 2^32) mod m. a may be any unsigned long.
             */
            private long times2ToThe32Mod(long a, final long m) {
                int remainingPowersOf2 = 32;
                do {
                    final int shift = Math.min(remainingPowersOf2, Long.numberOfLeadingZeros(a));
                    // shift is either the number of powers of 2 left to multiply a by, or the biggest shift
                    // possible while keeping a in an unsigned long.
                    a = UnsignedLongs.remainder(a << shift, m);
                    remainingPowersOf2 -= shift;
                } while (remainingPowersOf2 > 0);
                return a;
            }

            @Override
            long mulMod(final long a, final long b, final long m) {
                final long aHi = a >>> 32; // < 2^31
                final long bHi = b >>> 32; // < 2^31
                final long aLo = a & 0xFFFFFFFFL; // < 2^32
                final long bLo = b & 0xFFFFFFFFL; // < 2^32

                /*
                 * a * b == aHi * bHi * 2^64 + (aHi * bLo + aLo * bHi) * 2^32 + aLo * bLo.
                 *       == (aHi * bHi * 2^32 + aHi * bLo + aLo * bHi) * 2^32 + aLo * bLo
                 *
                 * We carry out this computation in modular arithmetic. Since times2ToThe32Mod accepts any
                 * unsigned long, we don't have to do a mod on every operation, only when intermediate
                 * results can exceed 2^63.
                 */
                long result = times2ToThe32Mod(aHi * bHi /* < 2^62 */, m); // < m < 2^63
                result += aHi * bLo; // aHi * bLo < 2^63, result < 2^64
                if (result < 0) {
                    result = UnsignedLongs.remainder(result, m);
                }
                // result < 2^63 again
                result += aLo * bHi; // aLo * bHi < 2^63, result < 2^64
                result = times2ToThe32Mod(result, m); // result < m < 2^63
                return plusMod(result, UnsignedLongs.remainder(aLo * bLo /* < 2^64 */, m), m);
            }

            @Override
            long squareMod(final long a, final long m) {
                final long aHi = a >>> 32; // < 2^31
                final long aLo = a & 0xFFFFFFFFL; // < 2^32

                /*
                 * a^2 == aHi^2 * 2^64 + aHi * aLo * 2^33 + aLo^2
                 *     == (aHi^2 * 2^32 + aHi * aLo * 2) * 2^32 + aLo^2
                 * We carry out this computation in modular arithmetic.  Since times2ToThe32Mod accepts any
                 * unsigned long, we don't have to do a mod on every operation, only when intermediate
                 * results can exceed 2^63.
                 */
                long result = times2ToThe32Mod(aHi * aHi /* < 2^62 */, m); // < m < 2^63
                long hiLo = aHi * aLo * 2;
                if (hiLo < 0) {
                    hiLo = UnsignedLongs.remainder(hiLo, m);
                }
                // hiLo < 2^63
                result += hiLo; // result < 2^64
                result = times2ToThe32Mod(result, m); // result < m < 2^63
                return plusMod(result, UnsignedLongs.remainder(aLo * aLo /* < 2^64 */, m), m);
            }
        };

        static boolean test(final long base, final long n) {
            // Since base will be considered % n, it's okay if base > FLOOR_SQRT_MAX_LONG,
            // so long as n <= FLOOR_SQRT_MAX_LONG.
            return ((n <= FLOOR_SQRT_MAX_LONG) ? SMALL : LARGE).testWitness(base, n);
        }

        /**
         * Returns a * b mod m.
         *
         * @param a the first value
         * @param b the second value
         * @param m the modulus
         * @return the result of (a * b) mod m
         */
        abstract long mulMod(long a, long b, long m);

        /**
         * Returns a^2 mod m.
         *
         * @param a the value to square
         * @param m the modulus
         * @return the result of (a * a) mod m
         */
        abstract long squareMod(long a, long m);

        private long powMod(long a, long p, final long m) {
            long res = 1;
            while (true) {
                if ((p & 1) != 0) {
                    res = mulMod(res, a, m);
                }
                p >>= 1;
                if (p == 0) {
                    // Exit before squaring: the square past the highest set exponent bit is never used.
                    return res;
                }
                a = squareMod(a, m);
            }
        }

        /**
         * Returns {@code true} if n is a strong probable prime relative to the specified base.
         *
         * @param base the base value
         * @param n the value to test
         * @return {@code true} if n is a strong probable prime relative to the specified base
         */
        private boolean testWitness(long base, final long n) {
            final int r = Long.numberOfTrailingZeros(n - 1);
            final long d = (n - 1) >> r;
            base %= n;
            if (base == 0) {
                return true;
            }
            // Calculate a := base^d mod n.
            long a = powMod(base, d, n);
            // n passes this test if
            //    base^d = 1 (mod n)
            // or base^(2^j * d) = -1 (mod n) for some 0 <= j < r.
            if (a == 1) {
                return true;
            }
            int j = 0;
            while (a != n - 1) {
                if (++j == r) {
                    return false;
                }
                a = squareMod(a, n);
            }
            return true;
        }
    }
}
