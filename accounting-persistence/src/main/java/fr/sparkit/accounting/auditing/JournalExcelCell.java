package fr.sparkit.accounting.auditing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.microsoft.sqlserver.jdbc.StringUtils;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JournalExcelCell {
    String headerName();

    boolean isDarker() default false;

    String tooltipMessage() default StringUtils.EMPTY;
}
