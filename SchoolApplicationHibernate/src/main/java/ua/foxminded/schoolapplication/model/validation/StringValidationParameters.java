package ua.foxminded.schoolapplication.model.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StringValidationParameters {
	boolean isNullPossible() default false;

	int minLength() default 0;

	int maxLength() default Integer.MAX_VALUE;

	String pattern() default ".*";
}
