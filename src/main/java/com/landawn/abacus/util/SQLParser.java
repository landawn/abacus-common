/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class SQLParser {
    private static final char TAB = '\t';
    private static final char ENTER = '\n';
    private static final Set<Object> seperators = new HashSet<>();

    static {
        seperators.add(TAB);
        seperators.add(ENTER);
        seperators.add(' ');
        seperators.add('?');
        seperators.add(',');
        seperators.add('~');
        seperators.add('!');
        seperators.add('@');
        seperators.add('^');
        seperators.add('#');
        seperators.add("!!");
        seperators.add(';');
        seperators.add('(');
        seperators.add(')');
        seperators.add('=');
        seperators.add("==");
        seperators.add(":=");
        seperators.add("^=");
        seperators.add("~=");
        seperators.add("+=");
        seperators.add("-=");
        seperators.add("*=");
        seperators.add("/=");
        seperators.add("%=");
        seperators.add("&=");
        seperators.add("|=");
        seperators.add("!=");
        seperators.add("!<");
        seperators.add("!>");
        seperators.add('>');
        seperators.add(">>");
        seperators.add(">=");
        seperators.add("@>");
        seperators.add("&>");
        seperators.add(">^");
        seperators.add('<');
        seperators.add("<<");
        seperators.add("<=");
        seperators.add("<@");
        seperators.add("&<");
        seperators.add("<^");
        seperators.add('+');
        seperators.add('-');
        seperators.add('%');
        seperators.add('/');
        seperators.add('*');
        seperators.add('&');
        seperators.add("&&");
        seperators.add('|');
        seperators.add("||");
        seperators.add("|/");
        seperators.add("||/");
        seperators.add('^');
        seperators.add('~');
        seperators.add('!');
        seperators.add("->");
        seperators.add('#');
        seperators.add("##");
        seperators.add("@@");
        seperators.add("@-@");
        seperators.add("@@@");
        seperators.add("->>");
        seperators.add("<->");
        seperators.add("<=>");
        seperators.add(">>=");
        seperators.add("<<=");
        seperators.add("<<|");
        seperators.add("|>>");
        seperators.add("&<|");
        seperators.add("|&>");
        seperators.add("|>>");
        seperators.add("(+)");
        seperators.add("?#");
        seperators.add("?-");
        seperators.add("?-");
        seperators.add("?|");
        seperators.add("?-|");
        seperators.add("?||");
        seperators.add("~*");
        seperators.add("!~");
        seperators.add("!~*");
        seperators.add("^-=");
        seperators.add("|*=");
    }

    private static final Map<String, String[]> compositeWords = new ObjectPool<String, String[]>(64);

    static {
        compositeWords.put(WD.LEFT_JOIN, new String[] { "LEFT", "JOIN" });
        compositeWords.put(WD.RIGHT_JOIN, new String[] { "RIGHT", "JOIN" });
        compositeWords.put(WD.FULL_JOIN, new String[] { "FULL", "JOIN" });
        compositeWords.put(WD.CROSS_JOIN, new String[] { "CROSS", "JOIN" });
        compositeWords.put(WD.INNER_JOIN, new String[] { "INNER", "JOIN" });
        compositeWords.put(WD.NATURAL_JOIN, new String[] { "NATURAL", "JOIN" });
        compositeWords.put(WD.INNER_JOIN, new String[] { "INNER", "JOIN" });
        compositeWords.put(WD.GROUP_BY, new String[] { "GROUP", "BY" });
        compositeWords.put(WD.ORDER_BY, new String[] { "ORDER", "BY" });
        compositeWords.put(WD.FOR_UPDATE, new String[] { "FOR", "UPDATE" });
        compositeWords.put(WD.FETCH_FIRST, new String[] { "FETCH", "FIRST" });
        compositeWords.put(WD.FETCH_NEXT, new String[] { "FETCH", "NEXT" });
        compositeWords.put(WD.ROWS_ONLY, new String[] { "ROWS", "ONLY" });
        compositeWords.put(WD.UNION_ALL, new String[] { "UNION", "ALL" });
        compositeWords.put(WD.IS_NOT, new String[] { "IS", "NOT" });
        compositeWords.put(WD.IS_NULL, new String[] { "IS", "NULL" });
        compositeWords.put(WD.IS_NOT_NULL, new String[] { "IS", "NOT", "NULL" });
        compositeWords.put(WD.IS_EMPTY, new String[] { "IS", "EMPTY" });
        compositeWords.put(WD.IS_NOT_EMPTY, new String[] { "IS", "NOT", "EMPTY" });
        compositeWords.put(WD.IS_BLANK, new String[] { "IS", "BLANK" });
        compositeWords.put(WD.IS_NOT_BLANK, new String[] { "IS", "NOT", "BLANK" });
        compositeWords.put(WD.NOT_IN, new String[] { "NOT", "IN" });
        compositeWords.put(WD.NOT_EXISTS, new String[] { "NOT", "EXISTS" });

        List<String> list = new ArrayList<>(compositeWords.keySet());

        for (String e : list) {
            e = e.toLowerCase();

            if (!compositeWords.containsKey(e)) {
                compositeWords.put(e, Splitter.with(WD.SPACE).trim(true).splitToArray(e));
            }

            e = e.toUpperCase();

            if (!compositeWords.containsKey(e)) {
                compositeWords.put(e, Splitter.with(WD.SPACE).trim(true).splitToArray(e));
            }
        }
    }

    private SQLParser() {
    }

    public static List<String> parse(String sql) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();
        final List<String> words = new ArrayList<>();

        String temp = "";
        char quoteChar = 0;

        for (int index = 0; index < sqlLength; index++) {
            // TODO [performance improvement]. will it improve performance if
            // change to char array?
            // char c = sqlCharArray[charIndex];
            char c = sql.charAt(index);

            // is it in a quoted identifier?
            if (quoteChar != 0) {
                sb.append(c);

                // end in quote.
                if (c == quoteChar) {
                    words.add(sb.toString());
                    sb.setLength(0);

                    quoteChar = 0;
                }
            } else if (isSeperator(sql, sqlLength, index, c)) {
                if (sb.length() > 0) {
                    words.add(sb.toString());
                    sb.setLength(0);
                }

                if ((index < (sqlLength - 2)) && seperators.contains(temp = sql.substring(index, index + 3))) {
                    words.add(temp);
                    index += 2;
                } else if ((index < (sqlLength - 1)) && seperators.contains(temp = sql.substring(index, index + 2))) {
                    words.add(temp);
                    index += 1;
                } else if (c == WD._SPACE || c == TAB || c == ENTER) {
                    if ((words.size() > 0) && !words.get(words.size() - 1).equals(WD.SPACE)) {
                        words.add(WD.SPACE);
                    }
                } else {
                    words.add(String.valueOf(c));
                }
            } else {
                sb.append(c);

                if ((c == WD._QUOTATION_S) || (c == WD._QUOTATION_D)) {
                    quoteChar = c;
                }
            }
        }

        if (sb.length() > 0) {
            words.add(sb.toString());
            sb.setLength(0);
        }

        Objectory.recycle(sb);

        return words;
    }

    public static int indexWord(String sql, String word, int fromIndex, boolean caseSensitive) {
        String[] subWords = compositeWords.get(word);

        if (subWords == null) {
            subWords = Splitter.with(WD.SPACE).trim(true).splitToArray(word);
            compositeWords.put(word, subWords);
        }

        if ((subWords == null) || (subWords.length <= 1)) {
            int result = N.INDEX_NOT_FOUND;

            final StringBuilder sb = Objectory.createStringBuilder();
            final int sqlLength = sql.length();
            String temp = "";
            char quoteChar = 0;

            for (int index = fromIndex; index < sqlLength; index++) {
                char c = sql.charAt(index);

                // is it in a quoted identifier?
                if (quoteChar != 0) {
                    sb.append(c);

                    // end in quote.
                    if (c == quoteChar) {
                        temp = sb.toString();

                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index - word.length() + 1;

                            break;
                        }

                        sb.setLength(0);
                        quoteChar = 0;
                    }
                } else if (isSeperator(sql, sqlLength, index, c)) {
                    if (sb.length() > 0) {
                        temp = sb.toString();

                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index - word.length();

                            break;
                        }

                        sb.setLength(0);
                    } else if (c == WD._SPACE || c == TAB || c == ENTER) {
                        // skip white char
                        continue;
                    }

                    if ((index < (sqlLength - 2)) && seperators.contains(temp = sql.substring(index, index + 3))) {
                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index;

                            break;
                        }

                        index += 2;
                    } else if ((index < (sqlLength - 1)) && seperators.contains(temp = sql.substring(index, index + 2))) {
                        if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                            result = index;

                            break;
                        }

                        index += 1;
                    } else if (word.equals(String.valueOf(c)) || (!caseSensitive && word.equalsIgnoreCase(String.valueOf(c)))) {
                        result = index;

                        break;
                    }
                } else {
                    sb.append(c);

                    if ((c == WD._QUOTATION_S) || (c == WD._QUOTATION_D)) {
                        quoteChar = c;
                    }
                }
            }

            if (result < 0 && sb.length() > 0) {
                temp = sb.toString();

                if (word.equals(temp) || (!caseSensitive && word.equalsIgnoreCase(temp))) {
                    result = sqlLength - word.length();
                }
            }

            Objectory.recycle(sb);

            return result;
        } else {
            int result = indexWord(sql, subWords[0], fromIndex, caseSensitive);

            if (result >= 0) {
                int tmpIndex = result + subWords[0].length();
                String nextWord = null;

                for (int i = 1; i < subWords.length; i++) {
                    nextWord = nextWord(sql, tmpIndex);

                    if (N.notNullOrEmpty(nextWord) && (nextWord.equals(subWords[i]) || (!caseSensitive && nextWord.equalsIgnoreCase(subWords[i])))) {
                        tmpIndex += (subWords[i].length() + 1);
                    } else {
                        result = -1;

                        break;
                    }
                }
            }

            return result;
        }
    }

    public static String nextWord(String sql, int fromIndex) {
        final int sqlLength = sql.length();
        final StringBuilder sb = Objectory.createStringBuilder();

        String temp = "";
        char quoteChar = 0;

        for (int index = fromIndex; index < sqlLength; index++) {
            char c = sql.charAt(index);

            // is it in a quoted identifier?
            if (quoteChar != 0) {
                sb.append(c);

                // end in quote.
                if (c == quoteChar) {
                    break;
                }
            } else if (isSeperator(sql, sqlLength, index, c)) {
                if (sb.length() > 0) {
                    break;
                } else if (c == WD._SPACE || c == TAB || c == ENTER) {
                    // skip white char
                    continue;
                }

                if (((index < (sqlLength - 2)) && seperators.contains(temp = sql.substring(index, index + 3)))
                        || ((index < (sqlLength - 1)) && seperators.contains(temp = sql.substring(index, index + 2)))) {
                    sb.append(temp);
                } else {
                    sb.append(c);
                }

                break;
            } else {
                sb.append(c);

                if ((c == WD._QUOTATION_S) || (c == WD._QUOTATION_D)) {
                    quoteChar = c;
                }
            }
        }

        String st = (sb.length() == 0) ? "" : sb.toString();
        Objectory.recycle(sb);

        return st;
    }

    public static void registerSeperator(char seperator) {
        N.checkArgPositive(seperator, "seperator");

        seperators.add(seperator);
    }

    public static void registerSeperator(String seperator) {
        N.checkArgNotNull(seperator, "seperator");

        seperators.add(seperator);

        if (seperator.length() == 1) {
            seperators.add(seperator.charAt(0));
        }
    }

    public static boolean isSeperator(String str, int len, int index, char ch) {
        // for Ibatis/Mybatis
        if (ch == '#' && index < len - 1 && str.charAt(index + 1) == '{') {
            return false;
        }

        return seperators.contains(ch);
    }

    public static boolean isFunctionName(final List<String> words, int len, int index) {
        //    return (i < len - 1 && words.get(i + 1).charAt(0) == WD._PARENTHESES_L)
        //            || (i < len - 2 && WD.SPACE.equals(words.get(i + 1)) && words.get(i + 2).charAt(0) == WD._PARENTHESES_L);

        return (index < len - 1 && words.get(index + 1).charAt(0) == WD._PARENTHESES_L);
    }
}
