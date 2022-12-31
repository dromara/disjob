package cn.ponfee.scheduler.common.base.exception;

/**
 * Json exception description
 * 
 * @author Ponfee
 */
public class JsonException extends RuntimeException {
    private static final long serialVersionUID = 8109219010796537426L;

    public JsonException() {
        super();
    }

    public JsonException(String message) {
        super(message);
    }

    public JsonException(Throwable cause) {
        super(cause);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }

    protected JsonException(String message,
                            Throwable cause,
                            boolean enableSuppression,
                            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
