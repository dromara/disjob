/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.common.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * Representing a boolean status
 *
 * @author Ponfee
 */
public enum Predicates {

    /**
     * Yes
     */
    Y(1, "是"),

    /**
     * No
     */
    N(0, "否"),

    ;

    private final int value;
    private final char code;
    private final String desc;

    Predicates(int value, String desc) {
        this.value = value;
        this.code = name().charAt(0);
        this.desc = desc;
    }

    public int value() {
        return value;
    }

    public char code() {
        return code;
    }

    public String desc() {
        return desc;
    }

    public boolean state() {
        return this == Y;
    }

    // ------------------------------------------------ equals methods

    public boolean equals(int val) {
        Assert.isTrue(Y.value == val || N.value == val, () -> "Invalid predicate int value: " + val);
        return this.value == val;
    }

    public boolean equals(String str) {
        Assert.isTrue(StringUtils.length(str) == 1, () -> "Invalid predicate string code: " + str);
        return equals(str.charAt(0));
    }

    public boolean equals(char val) {
        char chr = Character.toUpperCase(val);
        Assert.isTrue(Y.code == chr || N.code == chr, () -> "Invalid predicate char code: " + val);
        return this.code == chr;
    }

    public boolean equals(boolean state) {
        return state() == state;
    }

    // ------------------------------------------------ check whether the value is yes

    public static boolean yes(int value) {
        return Y.equals(value);
    }

    public static boolean yes(String code) {
        return Y.equals(code);
    }

    public static boolean yes(char code) {
        return Y.equals(code);
    }

    public static boolean yes(boolean state) {
        return Y.equals(state);
    }

    // ------------------------------------------------ check whether the value is no

    public static boolean no(int value) {
        return N.equals(value);
    }

    public static boolean no(String code) {
        return N.equals(code);
    }

    public static boolean no(char code) {
        return N.equals(code);
    }

    public static boolean no(boolean state) {
        return N.equals(state);
    }

    // ------------------------------------------------ of methods

    public static Predicates of(int value) {
        return Y.equals(value) ? Y : N;
    }

    public static Predicates of(String code) {
        return Y.equals(code) ? Y : N;
    }

    public static Predicates of(char code) {
        return Y.equals(code) ? Y : N;
    }

    public static Predicates of(boolean state) {
        return Y.equals(state) ? Y : N;
    }

}
