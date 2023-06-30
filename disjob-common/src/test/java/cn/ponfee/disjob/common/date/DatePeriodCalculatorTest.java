/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.date;

import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * 周期计算
 *
 * @author Ponfee
 */
public class DatePeriodCalculatorTest {

    private static final FastDateFormat FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");

    @Test
    public void test1()  {
        test(Dates.toDate("2020-02-26 00:00:00", "yyyy-MM-dd HH:mm:ss"));
        System.out.println("------------------------\n");
        test(Dates.toDate("2021-02-26 00:00:00", "yyyy-MM-dd HH:mm:ss"));
    }

    private static void test(Date startDate )  {
        int step = 2;
        int next = 1;
        Date target = startDate;
        String except, actual;

        except = calc(DatePeriodCalculator.Periods.DAILY, startDate, target, step, next);
        actual = DatePeriods.DAILY.next(startDate, target, step, next).toString();
        System.out.println(actual);
        Assertions.assertEquals(except, actual);

        except = calc(DatePeriodCalculator.Periods.WEEKLY, startDate, target, step, next);
        actual = DatePeriods.WEEKLY.next(startDate, target, step, next).toString();
        System.out.println(actual);
        Assertions.assertEquals(except, actual);

        except = calc(DatePeriodCalculator.Periods.MONTHLY, startDate, target, step, next);
        actual = DatePeriods.MONTHLY.next(startDate, target, step, next).toString();
        System.out.println(actual);
        Assertions.assertEquals(except, actual);

        except = calc(DatePeriodCalculator.Periods.QUARTERLY, startDate, target, step, next);
        actual = DatePeriods.QUARTERLY.next(startDate, target, step, next).toString();
        System.out.println(actual);
        Assertions.assertEquals(except, actual);

        except = calc(DatePeriodCalculator.Periods.HALF_YEARLY, startDate, target, step, next);
        actual = DatePeriods.SEMIANNUAL.next(startDate, target, step, next).toString();
        System.out.println(actual);
        Assertions.assertEquals(except, actual);

        except = calc(DatePeriodCalculator.Periods.YEARLY, startDate, target, step, next);
        actual = DatePeriods.ANNUAL.next(startDate, target, step, next).toString();
        System.out.println(actual);
        Assertions.assertEquals(except, actual);
    }

    private static class DatePeriodCalculator {
        private final Date starting; // 最开始的周期（起点）时间
        private final Date target; // 待计算时间
        private final Periods period; // 周期类型

        public DatePeriodCalculator(Date starting, Date target, Periods period) {
            this.starting = starting;
            this.target = target;
            this.period = period;
        }

        /**
         * @param quantity 周期数量
         * @param next     目标周期的下next个周期
         * @return
         */
        public Date[] calculate(int quantity, int next) {
            if (quantity < 1) {
                throw new IllegalArgumentException("quantity must be positive number");
            }
            if (starting.after(target)) {
                throw new IllegalArgumentException("starting cannot after target date");
            }

            Calendar c1 = Calendar.getInstance();
            Calendar c2 = Calendar.getInstance();
            c1.setTime(starting);
            c2.setTime(target);
            Date startDate = null;
            int cycleNum, year;
            Calendar tmp;
            float days;
            switch (period) {
                case WEEKLY:
                    quantity *= 7;
                case DAILY:
                    days = c2.get(Calendar.DAY_OF_YEAR) - c1.get(Calendar.DAY_OF_YEAR); // 间隔天数
                    year = c2.get(Calendar.YEAR);
                    tmp = (Calendar) c1.clone();
                    while (tmp.get(Calendar.YEAR) != year) {
                        days += tmp.getActualMaximum(Calendar.DAY_OF_YEAR);// 得到当年的实际天数
                        tmp.add(Calendar.YEAR, 1);
                    }
                    cycleNum = (int) Math.floor(days / quantity) + next; // 上一个周期
                    c1.add(Calendar.DAY_OF_YEAR, cycleNum * quantity);
                    startDate = c1.getTime();
                    c1.add(Calendar.DAY_OF_YEAR, quantity);
                    break;
                case QUARTERLY: // 季度
                case HALF_YEARLY: // 半年度
                case YEARLY: // 年度
                case MONTHLY: // 月
                    switch (period) {
                        case QUARTERLY: // 季度
                            quantity *= 3;
                            break;
                        case HALF_YEARLY: // 半年度
                            quantity *= 6;
                            break;
                        case YEARLY:
                            quantity *= 12; // 年度
                            break;
                        default: // MONTHLY
                    }
                    // 间隔月数
                    int intervalMonth = (c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR)) * 12 + c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
                    cycleNum = (int) Math.floor(intervalMonth / quantity);
                    tmp = (Calendar) c1.clone();
                    // 跨月问题，当前时间仍属于该周期内，则应减一个周期数，如：(2012-01-15 ~ 2012-02-14，当前时间为2012-02-14，则当前时间属于该周期，而不是下一周期)
                    tmp.add(Calendar.MONTH, cycleNum * quantity);
                    if (tmp.after(c2)) {
                        cycleNum -= 1;
                    }
                    cycleNum += next; // 上一个周期
                    c1.add(Calendar.MONTH, cycleNum * quantity);
                    startDate = c1.getTime(); // 本周期开始时间
                    c1.add(Calendar.MONTH, quantity); // 本周期结束时间
                    break;
                default:
                    throw new IllegalArgumentException("invalid period type");
            }
            c1.add(Calendar.MILLISECOND, -1);
            return new Date[]{startDate, c1.getTime()};
        }

        public Date[] calculate(int next) {
            return calculate(1, next);
        }

        private enum Periods {
            DAILY("每日"), //
            WEEKLY("每周"), //
            MONTHLY("每月"), //
            QUARTERLY("每季度"), //
            HALF_YEARLY("每半年"), //
            YEARLY("每年");

            private final String desc;

            Periods(String desc) {
                this.desc = desc;
            }
        }
    }

    private static String calc(DatePeriodCalculator.Periods p, Date start, Date target, int step, int next) {
        Date[] dates = new DatePeriodCalculator(start, target, p).calculate(step, next);
        return FORMAT.format(dates[0]) + " ~ " + FORMAT.format(dates[1]);
    }

}
