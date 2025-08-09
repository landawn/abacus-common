/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

public class SwitchTest {
    private static final String jsonString = "{\"person\":[{\"id\":-2147483648,\"firstName\":\"4809981875dc43ddb6d1add0b1eec75c><\\\"<//> ' \\\"\",\"lastName\":\"4809981875dc43ddb6d1add0b1eec75c\",\"address1\":\"4809981875dc43ddb6d1add0b1eec75c\",\"postCode\":\"4809981875dc43ddb6d1add0b1eec75c\",\"city\":\"4809981875dc43ddb6d1add0b1eec75c\",\"country\":\"4809981875dc43ddb6d1add0b1eec75c\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"3b21ba46239b484ea1d1b7d9dbee68c0><\\\"<//> ' \\\"\",\"lastName\":\"3b21ba46239b484ea1d1b7d9dbee68c0\",\"address1\":\"3b21ba46239b484ea1d1b7d9dbee68c0\",\"postCode\":\"3b21ba46239b484ea1d1b7d9dbee68c0\",\"city\":\"3b21ba46239b484ea1d1b7d9dbee68c0\",\"country\":\"3b21ba46239b484ea1d1b7d9dbee68c0\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"c953f8ebaaae4424a656c819b8149376><\\\"<//> ' \\\"\",\"lastName\":\"c953f8ebaaae4424a656c819b8149376\",\"address1\":\"c953f8ebaaae4424a656c819b8149376\",\"postCode\":\"c953f8ebaaae4424a656c819b8149376\",\"city\":\"c953f8ebaaae4424a656c819b8149376\",\"country\":\"c953f8ebaaae4424a656c819b8149376\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"d75587f037324efaafc88de095416221><\\\"<//> ' \\\"\",\"lastName\":\"d75587f037324efaafc88de095416221\",\"address1\":\"d75587f037324efaafc88de095416221\",\"postCode\":\"d75587f037324efaafc88de095416221\",\"city\":\"d75587f037324efaafc88de095416221\",\"country\":\"d75587f037324efaafc88de095416221\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"e8291a6104094e94b81daf4cf1852efe><\\\"<//> ' \\\"\",\"lastName\":\"e8291a6104094e94b81daf4cf1852efe\",\"address1\":\"e8291a6104094e94b81daf4cf1852efe\",\"postCode\":\"e8291a6104094e94b81daf4cf1852efe\",\"city\":\"e8291a6104094e94b81daf4cf1852efe\",\"country\":\"e8291a6104094e94b81daf4cf1852efe\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"63f0816b82eb42a8be9181e4a733285a><\\\"<//> ' \\\"\",\"lastName\":\"63f0816b82eb42a8be9181e4a733285a\",\"address1\":\"63f0816b82eb42a8be9181e4a733285a\",\"postCode\":\"63f0816b82eb42a8be9181e4a733285a\",\"city\":\"63f0816b82eb42a8be9181e4a733285a\",\"country\":\"63f0816b82eb42a8be9181e4a733285a\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"26019c0c46ef487dae175c1bae381417><\\\"<//> ' \\\"\",\"lastName\":\"26019c0c46ef487dae175c1bae381417\",\"address1\":\"26019c0c46ef487dae175c1bae381417\",\"postCode\":\"26019c0c46ef487dae175c1bae381417\",\"city\":\"26019c0c46ef487dae175c1bae381417\",\"country\":\"26019c0c46ef487dae175c1bae381417\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"a917a9d6628d4afea75ff6212df3bd32><\\\"<//> ' \\\"\",\"lastName\":\"a917a9d6628d4afea75ff6212df3bd32\",\"address1\":\"a917a9d6628d4afea75ff6212df3bd32\",\"postCode\":\"a917a9d6628d4afea75ff6212df3bd32\",\"city\":\"a917a9d6628d4afea75ff6212df3bd32\",\"country\":\"a917a9d6628d4afea75ff6212df3bd32\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"ef7663f41de648bf8d4a53e32ab0c386><\\\"<//> ' \\\"\",\"lastName\":\"ef7663f41de648bf8d4a53e32ab0c386\",\"address1\":\"ef7663f41de648bf8d4a53e32ab0c386\",\"postCode\":\"ef7663f41de648bf8d4a53e32ab0c386\",\"city\":\"ef7663f41de648bf8d4a53e32ab0c386\",\"country\":\"ef7663f41de648bf8d4a53e32ab0c386\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true},{\"id\":-2147483648,\"firstName\":\"c56ae519a31a4f478da29b9eac61a426><\\\"<//> ' \\\"\",\"lastName\":\"c56ae519a31a4f478da29b9eac61a426\",\"address1\":\"c56ae519a31a4f478da29b9eac61a426\",\"postCode\":\"c56ae519a31a4f478da29b9eac61a426\",\"city\":\"c56ae519a31a4f478da29b9eac61a426\",\"country\":\"c56ae519a31a4f478da29b9eac61a426\",\"birthday\":\"2014-03-04T00:12:06Z\",\"active\":true}]}";
    private static int[] caseArray = new int[128];

    static {
        caseArray[','] = 1;
        caseArray[':'] = 1;
        caseArray['"'] = 1;
        caseArray['{'] = 1;
        caseArray['}'] = 1;
        caseArray['['] = 1;
        caseArray[']'] = 1;
    }

    @Test
    public void testSwitchPerformance() {
        long startTime = System.currentTimeMillis();

        int loopNum = 100000;

        for (int i = 0; i < loopNum; i++) {
            switch_1();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("switch_1 took: " + (endTime - startTime));

        startTime = System.currentTimeMillis();

        for (int i = 0; i < loopNum; i++) {
            switch_2();
        }

        endTime = System.currentTimeMillis();

        System.out.println("switch_2 took: " + (endTime - startTime));

        startTime = System.currentTimeMillis();

        for (int i = 0; i < loopNum; i++) {
            switch_3();
        }

        endTime = System.currentTimeMillis();

        System.out.println("switch_3 took: " + (endTime - startTime));

        startTime = System.currentTimeMillis();

        for (int i = 0; i < loopNum; i++) {
            switch_4();
        }

        endTime = System.currentTimeMillis();

        System.out.println("switch_4 took: " + (endTime - startTime));
    }

    @Test
    public void testSwitchPerformance_2() {
        for (int i = 0; i < 3; i++) {
            Profiler.run(this, "switch_1", 1, 100000, 1).printResult();
            Profiler.run(this, "switch_2", 1, 100000, 1).printResult();
            Profiler.run(this, "switch_3", 1, 100000, 1).printResult();
            Profiler.run(this, "switch_4", 1, 100000, 1).printResult();
        }
    }

    @Test
    public void switch_1() {
        char ch = 0;

        for (int i = 0, len = jsonString.length(); i < len; i++) {
            ch = jsonString.charAt(i);

            switch (ch) {
                case '"':
                    break;

                case ',':
                    break;

                case ':':
                    break;

                case '[':
                    break;

                case ']':
                    break;

                case '{':
                    break;

                case '}':
                    break;

                default:
            }
        }
    }

    @Test
    public void switch_2() {
        int ch = 0;

        for (int i = 0, len = jsonString.length(); i < len; i++) {
            ch = jsonString.charAt(i);

            switch (ch) {
                case 1: // no char equals 1 in the json string.
                    break;

                case '"':
                    break;

                case ',':
                    break;

                case ':':
                    break;

                case '[':
                    break;

                case ']':
                    break;

                case '{':
                    break;

                case '}':
                    break;

                default:
            }
        }
    }

    @Test
    public void switch_3() {
        int ch = 0;

        for (int i = 0, len = jsonString.length(); i < len; i++) {
            ch = jsonString.charAt(i);

            if ((ch == ',') || (ch == ':') || (ch == '"') || (ch == '{')) {
                continue;
            }

            if (ch == '}') {
                continue;
            }

            if (ch == ']') {
                continue;
            }

            if (ch == '[') {
            }
        }
    }

    @Test
    public void switch_4() {
        int ch = 0;

        for (int i = 0, len = jsonString.length(); i < len; i++) {
            ch = jsonString.charAt(i);

            if (caseArray[ch] > 0) {
            }
        }
    }
}
