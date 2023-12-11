package cn.ponfee.disjob.common.date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.JSR310DateTimeDeserializerBase;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Deserializer for Java 8 temporal {@link LocalDateTime}s.
 *
 * @author Nick Williams
 * @author Ponfee
 * @see com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
 * @since 2.2.0
 */
public class CustomLocalDateTimeDeserializer extends JSR310DateTimeDeserializerBase<LocalDateTime> {
    private static final long serialVersionUID = 1L;

    public static final CustomLocalDateTimeDeserializer INSTANCE = new CustomLocalDateTimeDeserializer();

    private final transient LocalDateTimeFormat wrappedFormatter;

    protected CustomLocalDateTimeDeserializer() {
        this(Dates.DATETIME_PATTERN);
    }

    public CustomLocalDateTimeDeserializer(String pattern) {
        this(DateTimeFormatter.ofPattern(pattern));
    }

    public CustomLocalDateTimeDeserializer(DateTimeFormatter formatter) {
        super(LocalDateTime.class, formatter);
        this.wrappedFormatter = new LocalDateTimeFormat(formatter);
    }

    @Override
    protected JSR310DateTimeDeserializerBase<LocalDateTime> withShape(JsonFormat.Shape shape) {
        return this;
    }

    @Override
    protected CustomLocalDateTimeDeserializer withDateFormat(DateTimeFormatter formatter) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected CustomLocalDateTimeDeserializer withLeniency(Boolean leniency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasTokenId(JsonTokenId.ID_STRING)) {
            return _fromString(parser, context, parser.getText());
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (parser.isExpectedStartObjectToken()) {
            return _fromString(parser, context, context.extractScalarFromObject(parser, this, handledType()));
        }
        if (parser.isExpectedStartArrayToken()) {
            JsonToken t = parser.nextToken();
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            if (   (t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)
                && context.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            ) {
                final LocalDateTime parsed = deserialize(parser, context);
                if (parser.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(parser, context);
                }
                return parsed;
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                LocalDateTime result;

                int year = parser.getIntValue();
                int month = parser.nextIntValue(-1);
                int day = parser.nextIntValue(-1);
                int hour = parser.nextIntValue(-1);
                int minute = parser.nextIntValue(-1);

                t = parser.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    result = LocalDateTime.of(year, month, day, hour, minute);
                } else {
                    int second = parser.getIntValue();
                    t = parser.nextToken();
                    if (t == JsonToken.END_ARRAY) {
                        result = LocalDateTime.of(year, month, day, hour, minute, second);
                    } else {
                        int partialSecond = parser.getIntValue();
                        if (partialSecond < 1_000 && !context.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)) {
                            // value is milliseconds, convert it to nanoseconds
                            partialSecond *= 1_000_000;
                        }
                        if (parser.nextToken() != JsonToken.END_ARRAY) {
                            throw context.wrongTokenException(parser, handledType(), JsonToken.END_ARRAY,
                                "Expected array to end");
                        }
                        result = LocalDateTime.of(year, month, day, hour, minute, second, partialSecond);
                    }
                }
                return result;
            }
            context.reportInputMismatch(handledType(), "Unexpected token (%s) within Array, expected VALUE_NUMBER_INT", t);
        }
        if (parser.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (LocalDateTime) parser.getEmbeddedObject();
        }
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            _throwNoNumericTimestampNeedTimeZone(parser, context);
        }
        return _handleUnexpectedToken(context, parser, "Expected array or string.");
    }

    protected LocalDateTime _fromString(JsonParser p, DeserializationContext ctxt, String string0) throws IOException {
        String string = string0.trim();
        if (string.length() == 0) {
            return _fromEmptyString(p, ctxt, string);
        }
        try {
            return wrappedFormatter.parse(string);
        } catch (DateTimeException e) {
            return _handleDateTimeException(ctxt, e, string);
        }
    }
}
