/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.iterator;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A repeated access iterator that combines one or more other iterators.
 *
 * @param <T> The type of the returned elements.
 * @author Daniel M. Zimmerman
 * @version January 2012
 */
public class MultiIterator<T> implements RepeatedAccessIterator<T> {
    /**
     * The Iterator over concatenated iterators.
     */
    private final RepeatedAccessIterator<RepeatedAccessIterator<T>> my_iterators;

    /**
     * Creates a new MultiIterator that iterates over all given iterators in
     * sequence.
     *
     * @param the_iterators The list of iterators to iterate over.
     */
    @SuppressWarnings("unchecked")
    public MultiIterator(final List<RepeatedAccessIterator<T>> the_iterators) {
        // only keep non-empty iterators
        final List<RepeatedAccessIterator<T>> non_empties =
                new LinkedList<RepeatedAccessIterator<T>>();
        for (RepeatedAccessIterator<T> i : the_iterators) {
            if (i.hasElement()) {
                non_empties.add(i);
            }
        }
        final RepeatedAccessIterator<T>[] non_empty_array =
                non_empties.toArray(new RepeatedAccessIterator[non_empties.size()]);
        my_iterators =
                new ObjectArrayIterator<RepeatedAccessIterator<T>>(non_empty_array);
        // at this point, the iterator either has an element or is completely empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void advance() {
        if (my_iterators.hasElement() && my_iterators.element().hasElement()) {
            my_iterators.element().advance();
        }
        while (my_iterators.hasElement() &&
                !my_iterators.element().hasElement()) {
            my_iterators.advance();
        }
        // at this point, the iterator either has an element or is completely empty,
        // as all the individual iterators are non-empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public /*@ pure */ T element() throws NoSuchElementException {
        return my_iterators.element().element();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public /*@ pure */ boolean hasElement() {
        return my_iterators.hasElement() &&
                my_iterators.element().hasElement();
    }
}
