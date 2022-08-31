package fr.sparkit.accounting.auditing;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime locDateTime) {
        if (locDateTime == null) {
            return null;
        } else {
            return Timestamp.valueOf(locDateTime);
        }

    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp sqlTimestamp) {
        if (sqlTimestamp == null) {
            return null;
        } else {
            return sqlTimestamp.toLocalDateTime();
        }
    }
}
