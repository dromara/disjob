package cn.ponfee.scheduler.common.base.model;

import cn.ponfee.scheduler.common.base.ToJsonString;

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

    public static final Result<Void> SUCCESS = new Success();

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

    public <E> Result<E> convert(Function<T, E> mapper) {
        return new Result<>(code, msg, mapper.apply(data));
    }

    // -----------------------------------------------static success methods

    public static Result<Void> success() {
        return SUCCESS;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Success.CODE, Success.MSG, data);
    }

    // -----------------------------------------------static failure methods

    public static <T> Result<T> failure(CodeMsg cm) {
        return failure(cm.getCode(), cm.getMsg());
    }

    public static <T> Result<T> failure(int code) {
        return failure(code, null);
    }

    public static <T> Result<T> failure(int code, String msg) {
        if (code == Success.CODE) {
            throw new IllegalStateException("Failure code '" + code + "' cannot be '" + Success.CODE + "'.");
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

    public static <T> Result<T> of(int code,String msg, T data) {
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
        return code != null && code == Success.CODE;
    }

    @Transient
    public boolean isFailure() {
        return !isSuccess();
    }

    // -----------------------------------------------static class

    /**
     * Success Result
     */
    private static final class Success extends Result<Void> {
        private static final long serialVersionUID = -8356385235924100622L;
        private static final int CODE = 0;
        private static final String MSG = "OK";

        private Success() {
            super(CODE, MSG, null);
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
        public void setData(Void data) {
            throw new UnsupportedOperationException();
        }

        private Object readResolve() {
            return SUCCESS;
        }
    }

}
