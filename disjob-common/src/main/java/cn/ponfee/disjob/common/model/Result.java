/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.model;

import cn.ponfee.disjob.common.base.ToJsonString;

import java.beans.Transient;
import java.util.function.Function;

/**
 * Representing the result-data structure
 *
 * @param <T> the data type
 * @author Ponfee
 */
public class Result<T> extends ToJsonString implements CodeMsg, java.io.Serializable {
    private static final long serialVersionUID = -7637967889181161801L;

    /**
     * the code
     */
    private Integer code;

    /**
     * the message
     */
    private String msg;

    /**
     * the data
     */
    private T data;

    // -----------------------------------------------constructor methods

    public Result() {
        // No operation: retain no-arg constructor for help deserialization
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // -----------------------------------------------other methods

    public <E> Result<E> cast() {
        return (Result<E>) this;
    }

    public <E> Result<E> from(E data) {
        return new Result<>(code, msg, data);
    }

    public <E> Result<E> map(Function<T, E> mapper) {
        return new Result<>(code, msg, mapper.apply(data));
    }

    // -----------------------------------------------static success methods

    public static Result<Void> success() {
        return SuccessResult.INSTANCE;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SuccessResult.CODE, SuccessResult.MSG, data);
    }

    // -----------------------------------------------static failure methods

    public static <T> Result<T> failure(CodeMsg cm) {
        return failure(cm.getCode(), cm.getMsg());
    }

    public static <T> Result<T> failure(int code) {
        return failure(code, null);
    }

    public static <T> Result<T> failure(int code, String msg) {
        if (code == SuccessResult.CODE) {
            throw new IllegalStateException("Result failure code '" + code + "' cannot be '" + SuccessResult.CODE + "'.");
        }
        return new Result<>(code, msg, null);
    }

    // -----------------------------------------------of operations

    public static <T> Result<T> of(CodeMsg cm) {
        return new Result<>(cm.getCode(), cm.getMsg(), null);
    }

    public static <T> Result<T> of(CodeMsg cm, T data) {
        return new Result<>(cm.getCode(), cm.getMsg(), data);
    }

    public static <T> Result<T> of(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> of(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    // -----------------------------------------------getter/setter

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Transient
    public boolean isSuccess() {
        return code != null && code == SuccessResult.CODE;
    }

    @Transient
    public boolean isFailure() {
        return !isSuccess();
    }

    // -----------------------------------------------static class

    /**
     * Immutable result
     *
     * @param <T> the data type
     */
    public static abstract class ImmutableResult<T> extends Result<T> {
        private static final long serialVersionUID = -8356385235924100622L;

        public ImmutableResult(int code, String msg, T data) {
            super(code, msg, data);
        }

        @Override
        public void setCode(int code) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMsg(String msg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setData(T data) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Success result
     */
    private static final class SuccessResult extends ImmutableResult<Void> {
        private static final long serialVersionUID = -8356385235924100622L;

        private static final int   CODE = 0;
        private static final String MSG = "OK";

        public static final Result<Void> INSTANCE = new SuccessResult();

        private SuccessResult() {
            super(CODE, MSG, null);
        }

        /**
         * Singleton support
         *
         * @return singleton instance
         */
        private Object readResolve() {
            return INSTANCE;
        }
    }

}
