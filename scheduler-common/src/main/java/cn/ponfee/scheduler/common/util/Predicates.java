package cn.ponfee.scheduler.common.util;

/**
 * Representing a boolean status
 *
 * @author Ponfee
 */
public enum Predicates {

    Y(1, "是"), //

    N(0, "否"), //

    ;

    private final int value;
    private final char code;
    private final String desc;

    Predicates(int value, String desc) {
        this.value = value;
        this.code = name().charAt(0); // 'Y' or 'N'
        this.desc = desc;
    }

    public int value() {
        return value;
    }

    public char code() {
        return code;
    }

    public boolean state() {
        return this == Y;
    }

    public String desc() {
        return this.desc;
    }

    // ------------------------------------------------ equals methods
    public boolean equals(Integer value) {
        return equals(value == null ? N.value : value);
    }

    public boolean equals(int value) {
        if (value != Y.value && value != N.value) {
            throw new IllegalArgumentException("Invalid int value '" + value + "'");
        }
        return this.value == value;
    }

    public boolean equals(String code) {
        char c;
        if (code == null) {
            c = N.code;
        } else if (code.length() == 1) {
            c = code.charAt(0);
        } else {
            throw new IllegalArgumentException("Invalid string code '" + code + "'");
        }
        return equals(c);
    }

    public boolean equals(Character code) {
        return equals(code == null ? N.code : code);
    }

    public boolean equals(char code) {
        code = Character.toUpperCase(code);
        if (code != Y.code && code != N.code) {
            throw new IllegalArgumentException("Invalid char code '" + code + "'");
        }
        return this.code == code;
    }

    public boolean equals(Boolean state) {
        return equals(state == null ? N.state() : state);
    }

    public boolean equals(boolean state) {
        return state() == state;
    }

    public boolean equals(Predicates other) {
        return this == (other == null ? N : other);
    }

    // ------------------------------------------------ check whether the value is yes
    public static boolean yes(Integer value) {
        return Y.equals(value);
    }

    public static boolean yes(int value) {
        return Y.equals(value);
    }

    public static boolean yes(String code) {
        return Y.equals(code);
    }

    public static boolean yes(Character code) {
        return Y.equals(code);
    }

    public static boolean yes(char code) {
        return Y.equals(code);
    }

    public static boolean yes(Boolean state) {
        return Y.equals(state);
    }

    public static boolean yes(boolean state) {
        return Y.equals(state);
    }

    public static boolean yes(Predicates other) {
        return Y.equals(other);
    }

    // ------------------------------------------------ check whether the value is no
    public static boolean no(Integer value) {
        return N.equals(value);
    }

    public static boolean no(int value) {
        return N.equals(value);
    }

    public static boolean no(String code) {
        return N.equals(code);
    }

    public static boolean no(Character code) {
        return N.equals(code);
    }

    public static boolean no(char code) {
        return N.equals(code);
    }

    public static boolean no(Boolean state) {
        return N.equals(state);
    }

    public static boolean no(boolean state) {
        return N.equals(state);
    }

    public static boolean no(Predicates other) {
        return N.equals(other);
    }

    // ------------------------------------------------ of methods
    public static Predicates of(Integer value) {
        return Y.equals(value) ? Y : N;
    }

    public static Predicates of(int value) {
        return Y.equals(value) ? Y : N;
    }

    public static Predicates of(String code) {
        return Y.equals(code) ? Y : N;
    }

    public static Predicates of(Character code) {
        return Y.equals(code) ? Y : N;
    }

    public static Predicates of(char code) {
        return Y.equals(code) ? Y : N;
    }

    public static Predicates of(Boolean state) {
        return Y.equals(state) ? Y : N;
    }

    public static Predicates of(boolean state) {
        return Y.equals(state) ? Y : N;
    }

}
