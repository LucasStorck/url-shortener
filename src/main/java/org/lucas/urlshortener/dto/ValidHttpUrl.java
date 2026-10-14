package org.lucas.urlshortener.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidHttpUrl {
  String message() default "must be a valid http/https URL";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
