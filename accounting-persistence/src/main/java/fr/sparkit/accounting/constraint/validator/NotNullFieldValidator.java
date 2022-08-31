package fr.sparkit.accounting.constraint.validator;

import lombok.NoArgsConstructor;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@NoArgsConstructor
class NotNullFieldValidator implements ConstraintValidator<NotNullField, Object> {

    @Override
    public boolean isValid(Object field, ConstraintValidatorContext context) {
        return field != null;
    }

}
