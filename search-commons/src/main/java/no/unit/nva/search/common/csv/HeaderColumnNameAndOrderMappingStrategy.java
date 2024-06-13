package no.unit.nva.search.common.csv;

import com.opencsv.bean.BeanField;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.comparators.ComparableComparator;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.comparators.FixedOrderComparator;
import org.apache.commons.collections4.comparators.NullComparator;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Rurik Greenall
 */
public class HeaderColumnNameAndOrderMappingStrategy<T> extends HeaderColumnNameMappingStrategy<T> {
    public HeaderColumnNameAndOrderMappingStrategy(Class<T> type) {
        super();
        setType(type);
    }

    /**
     * This maintains case of header strings.
     *
     * @param bean One fully populated bean from which the header can be derived.
     *             This is important in the face of joining and splitting. If we have a
     *             MultiValuedMap as a field that is the target for a join on reading, that
     *             same field must be split into multiple columns on writing. Since the
     *             joining is done via regular expressions, it is impossible for opencsv
     *             to know what the column names are supposed to be on writing unless this
     *             bean includes a fully populated map.
     * @return Array of header Strings maintaining case.
     * @throws CsvRequiredFieldEmptyException in case any field that is marked "required" is empty.
     */
    @Override
    public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
        String[] header = super.generateHeader(bean);
        final int numColumns = headerIndex.findMaxIndex();
        if (numColumns == -1) {
            return header;
        }

        header = new String[numColumns + 1];

        BeanField<T, String> beanField;
        for (int i = 0; i <= numColumns; i++) {
            beanField = findField(i);
            String columnHeaderName = extractHeaderName(beanField);
            header[i] = columnHeaderName;
        }
        return header;
    }


    /**
     * This override allows setting of the column order based on annotation {@link CsvBindByNameOrder} .
     */
    @Override
    protected void loadFieldMap() {
        if (writeOrder == null && type.isAnnotationPresent(CsvBindByNameOrder.class)) {
            var predefinedList = Arrays.stream(type.getAnnotation(CsvBindByNameOrder.class).value())
                    .map(String::toUpperCase).collect(Collectors.toList());
            var fixedComparator = new FixedOrderComparator<>(predefinedList);
            fixedComparator.setUnknownObjectBehavior(FixedOrderComparator.UnknownObjectBehavior.AFTER);
            var comparator = new ComparatorChain<>(Arrays.asList(
                    fixedComparator,
                    new NullComparator<>(false),
                    new ComparableComparator<>()));
            setColumnOrderOnWrite(comparator);
        }
        super.loadFieldMap();
    }

    private String extractHeaderName(final BeanField<T, String> beanField) {
        if (beanField == null || beanField.getField() == null
                || beanField.getField().getDeclaredAnnotationsByType(CsvBindByName.class).length == 0) {
            return StringUtils.EMPTY;
        }

        if (beanField.getField().isAnnotationPresent(CsvBindByName.class)) {
            return beanField.getField().getDeclaredAnnotationsByType(CsvBindByName.class)[0].column();
        } else if (beanField.getField().isAnnotationPresent(CsvCustomBindByName.class)) {
            return beanField.getField().getDeclaredAnnotationsByType(CsvCustomBindByName.class)[0].column();
        }
        return StringUtils.EMPTY;

    }
}
