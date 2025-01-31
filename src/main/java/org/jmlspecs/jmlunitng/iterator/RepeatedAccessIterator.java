/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.iterator;

import java.util.NoSuchElementException;

/**
 * An iterator that supports accessing the current value multiple times.
 *
 * @param <T> The type of the returned elements.
 * @author Daniel M. Zimmerman
 * @author Jonathan Hogins
 * @version March 2010
 */
public interface RepeatedAccessIterator<T> {
    /**
     * @return Does the iterator have a current element?
     */
    boolean hasElement();

    /**
     * @return What is the iterator's current element?
     * @throws NoSuchElementException if there is no current element.
     */
    T element() throws NoSuchElementException;

    /**
     * Advance the iterator to the next element!
     */
    void advance();
}
