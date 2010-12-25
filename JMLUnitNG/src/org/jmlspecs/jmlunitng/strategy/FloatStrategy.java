/*
 * JMLUnitNG 
 * Copyright (C) 2010
 */

package org.jmlspecs.jmlunitng.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmlspecs.jmlunitng.iterator.IteratorAdapter;
import org.jmlspecs.jmlunitng.iterator.RepeatedAccessIterator;
/**
 * The default strategy for the <code>float</code> type. The default values are 
 * -1.0, 0.0 and 1.0.
 *
 * @author Jonathan Hogins
 * @author Daniel M. Zimmerman
 * @version December 2010
 */
public abstract class FloatStrategy extends PrimitiveStrategy {
  /**
   * The default values for this strategy.
   */
  private static final List<Float> DEFAULT_VALUES;
  static {
    final List<Float> defs = new ArrayList<Float>(3);
    defs.add(-1.f);
    defs.add(0.f);
    defs.add(1.f);
    DEFAULT_VALUES = Collections.unmodifiableList(defs);
  }
  /**
   * To be implemented by subclasses. Returns the iterator over default values.
   * @return An Iterator over default values.
   */
  public RepeatedAccessIterator<?> getDefaultValues() {
    return new IteratorAdapter<Float>(DEFAULT_VALUES.iterator());
  }
}
