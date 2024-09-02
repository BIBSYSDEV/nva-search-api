package no.unit.nva.search.common.csv;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply column ordering to Csv beans. Applies to Bean class.
 *
 * <p>Takes argument of form {"column name 1", "column name 2", etc…}".
 *
 * @author Rurik Greenall
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CsvBindByNameOrder {
    String[] value() default {};
}
