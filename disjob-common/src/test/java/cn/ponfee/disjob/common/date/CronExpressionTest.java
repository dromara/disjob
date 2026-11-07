/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;

/**
 * CronExpression Test
 *
 * @author Ponfee
 */
public class CronExpressionTest {

    /**
     * Regression test for the fixed boolean-logic bug (De Morgan's law):
     * the loop condition was {@code c != ' ' || c != '\t'} (always true),
     * which made the scan run to the end of the string instead of stopping
     * at the first whitespace.
     */
    @Test
    public void testFindNextWhiteSpace() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 ? * *");

        // no whitespace: returns string length
        Assertions.assertEquals(1, cron.findNextWhiteSpace(0, "5"));
        Assertions.assertEquals(4, cron.findNextWhiteSpace(0, "1970"));
        // stops at the first space
        Assertions.assertEquals(1, cron.findNextWhiteSpace(0, "5 "));
        Assertions.assertEquals(1, cron.findNextWhiteSpace(0, "5 \t"));
        // stops at the first tab (the bug made '\t' never terminate the loop)
        Assertions.assertEquals(1, cron.findNextWhiteSpace(0, "5\t"));
        Assertions.assertEquals(1, cron.findNextWhiteSpace(0, "5\t "));
        // starts from the given index
        Assertions.assertEquals(2, cron.findNextWhiteSpace(1, " 5 40"));
        Assertions.assertEquals(5, cron.findNextWhiteSpace(3, " 5 40"));
        // scans past digits and stops at the following space
        Assertions.assertEquals(4, cron.findNextWhiteSpace(1, "5540 5"));
        // scans past digits and stops at a tab
        Assertions.assertEquals(4, cron.findNextWhiteSpace(0, "5540\t5"));
        // empty string / index at end
        Assertions.assertEquals(0, cron.findNextWhiteSpace(0, ""));
        Assertions.assertEquals(2, cron.findNextWhiteSpace(2, "5 "));
    }

    @Test
    public void testSkipWhiteSpace() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 ? * *");

        // no whitespace: nothing to skip
        Assertions.assertEquals(0, cron.skipWhiteSpace(0, "5 "));
        // skips leading spaces and tabs
        Assertions.assertEquals(1, cron.skipWhiteSpace(0, " 5"));
        Assertions.assertEquals(1, cron.skipWhiteSpace(0, "\t5"));
        Assertions.assertEquals(4, cron.skipWhiteSpace(0, " \t \t5"));
        // starts from the given index
        Assertions.assertEquals(4, cron.skipWhiteSpace(3, "5   40"));

        Date date = Dates.parse("2026-01-01 00:00:00", Dates.DATETIME_PATTERN);
        Assertions.assertEquals("2026-01-01 00:00:10", Dates.format(new CronExpression("*/10 * * * * ?").getNextValidTimeAfter(date)));
        Assertions.assertEquals("2026-01-01 00:05:00", Dates.format(new CronExpression("0 */5 * * * ?").getNextValidTimeAfter(date)));
        Assertions.assertEquals("2026-01-03 00:00:00", Dates.format(new CronExpression("0 0 0 ? * */2").getNextValidTimeAfter(date)));
    }

    /**
     * {@link CronExpression#getNumericValue(String, int)} relies on
     * findNextWhiteSpace to locate the token end, verify its behavior.
     */
    @Test
    public void testGetNumericValue() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 ? * *");

        Assertions.assertEquals(5, cron.getNumericValue("5", 0));
        Assertions.assertEquals(5, cron.getNumericValue("5 40", 0));
        Assertions.assertEquals(40, cron.getNumericValue("5 40", 2));
        Assertions.assertEquals(5, cron.getNumericValue("5\t40", 0));
        Assertions.assertEquals(1970, cron.getNumericValue("1970\t", 0));
    }

}
