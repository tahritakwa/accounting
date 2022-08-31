package fr.sparkit.accounting.auditing;

import com.microsoft.sqlserver.jdbc.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DocumentAccountExcelCell {
    String headerName();

    boolean isDocumentAccountField() default false;

    boolean isDarker() default false;

    String tooltipMessage() default StringUtils.EMPTY;
}
