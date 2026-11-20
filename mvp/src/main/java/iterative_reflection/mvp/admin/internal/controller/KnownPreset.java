package iterative_reflection.mvp.admin.internal.controller;

import iterative_reflection.mvp.settings.ThemeSettings;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value is one of {@link ThemeSettings#PRESET_IDS}; null passes (pair with {@code @NotNull}). */
@Documented
@Constraint(validatedBy = KnownPreset.Validator.class)
@Target({ ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@interface KnownPreset {

	String message() default "must be a known theme preset";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<KnownPreset, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			if (value == null || ThemeSettings.isKnownPreset(value)) {
				return true;
			}
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("must be one of: " + String.join(", ", ThemeSettings.PRESET_IDS))
					.addConstraintViolation();
			return false;
		}

	}

}
