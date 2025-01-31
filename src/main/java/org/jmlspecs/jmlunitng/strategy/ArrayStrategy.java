/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.strategy;

import org.jmlspecs.jmlunitng.iterator.DynamicArrayIterator;
import org.jmlspecs.jmlunitng.iterator.MultiIterator;
import org.jmlspecs.jmlunitng.iterator.ObjectArrayIterator;
import org.jmlspecs.jmlunitng.iterator.RepeatedAccessIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * The strategy for all array types. It attempts to generate arrays
 * of lengths up to a specified maximum, using strategies for
 * the array component type.
 *
 * @author Daniel M. Zimmerman
 * @version July 2011
 */
public abstract class ArrayStrategy extends NonPrimitiveStrategy {
    /**
     * The maximum array size to generate.
     */
    private int my_max_length;

    /**
     * Creates a new ArrayStrategy for the given class. Default values will
     * be generated by using the strategy for the array's component type
     * (and any subtypes specified post-construction). By default, the maximum
     * size array to be generated is 1.
     *
     * @param the_class The array class for which to generate test data.
     * @throws IllegalArgumentException if the_class is not an array type.
     */
    public ArrayStrategy(final /*@ non_null @*/ Class<?> the_class)
            throws IllegalArgumentException {
        super(the_class, the_class.getComponentType());
        if (!the_class.isArray()) {
            throw new IllegalArgumentException
                    ("Cannot create ArrayStrategy for type " + the_class);
        }
        my_max_length = 1;
    }

    /**
     * Returns an iterator of arrays. If reflection is turned on,
     * all possible arrays up to the maximum array length will be
     * generated using elements taken from the generators
     * found for the specified data classes; otherwise, only the
     * null array and the empty array will be generated.
     *
     * @return An Iterator over default values.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RepeatedAccessIterator<?> defaultValues() {
        int max = 0;

        if (isReflective()) {
            max = my_max_length;
        }

        final List<RepeatedAccessIterator<?>> list =
                new ArrayList<RepeatedAccessIterator<?>>();
        list.add(new ObjectArrayIterator(new Object[]{null}));
        list.add(new DynamicArrayIterator(my_class.getComponentType(),
                my_generators, max));
        return new MultiIterator(list);
    }

    /**
     * Sets the maximum array length to generate. Increasing this value
     * can result in <i>many</i> additional data values being generated,
     * so use caution.
     *
     * @param the_max_length The maximum array length to generate.
     */
    //@ requires the_max_size >= 0;
    //@ ensures getMaxLength() == the_max_length;
    public final void setMaxLength(final int the_max_length) {
        my_max_length = the_max_length;
    }

    /**
     * @return the maximum array size to generate.
     */
    public final int maxLength() {
        return my_max_length;
    }
}
