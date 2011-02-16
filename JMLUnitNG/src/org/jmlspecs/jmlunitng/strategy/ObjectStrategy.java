/*
 * JMLUnitNG 
 * Copyright (C) 2010-11
 */

package org.jmlspecs.jmlunitng.strategy;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jmlspecs.jmlunitng.iterator.InstantiationIterator;
import org.jmlspecs.jmlunitng.iterator.MultiIterator;
import org.jmlspecs.jmlunitng.iterator.NonNullMultiIterator;
import org.jmlspecs.jmlunitng.iterator.ObjectArrayIterator;
import org.jmlspecs.jmlunitng.iterator.RepeatedAccessIterator;

/**
 * The strategy for all non-primitive, non-array types. For Enums, 
 * it always provides all values of the enum unless the default 
 * values are overridden. For other types of object, it attempts
 * to find test data generators for the default values.
 * 
 * @author Jonathan Hogins
 * @author Daniel M. Zimmerman
 * @version February 2011
 */
public abstract class ObjectStrategy extends NonPrimitiveStrategy {  
  /**
   * The ThreadLocal used for cycle detection.
   */
  private static final ThreadLocal<Set<Class<?>>> TRAVERSED_CLASSES = 
    new ThreadLocal<Set<Class<?>>>();
  
  /**
   * The enum constants for the given class, if it is an enum type.
   */
  private final Object[] my_enum_constants; 

  /**
   * Creates a new ObjectStrategy for the given class. If the class is an
   * enum, all enum constants will be used. By default, reflection will not
   * be used; this behavior can be subsequently changed with control methods.
   * 
   * @param the_class The class for which to generate test data.
   */
  public ObjectStrategy(final /*@ non_null @*/ Class<?> the_class) {
    super(the_class, (the_class.getEnumConstants() == null) ? the_class : null);
    my_enum_constants = the_class.getEnumConstants();
  }
  
  /**
   * A default empty iterator, may be overridden by child classes.
   * 
   * @return An empty iterator.
   */
  public RepeatedAccessIterator<?> localValues() {
    return new ObjectArrayIterator<Object>
    ((Object[]) Array.newInstance(my_class, 0));
  }
  
  /**
   * A default empty iterator, may be overridden by child classes.
   * 
   * @return An empty iterator.
   */
  public RepeatedAccessIterator<?> classValues() {
    return new ObjectArrayIterator<Object>
    ((Object[]) Array.newInstance(my_class, 0));
  }

  /**
   * A default empty iterator, may be overridden by child classes.
   * 
   * @return An empty iterator.
   */
  public RepeatedAccessIterator<?> packageValues() {
    return new ObjectArrayIterator<Object>
    ((Object[]) Array.newInstance(my_class, 0));
  }
  
  /**
   * Returns an iterator over objects of the various data classes, 
   * to the extent that they can be found.
   * 
   * @return An iterator over default values.
   */
  public RepeatedAccessIterator<?> defaultValues() {
    RepeatedAccessIterator<?> result;
    final boolean orig_reflective = isReflective();
    boolean already_traversed = wasTraversed();
    
    if (already_traversed) {
      // we don't do reflection if we've already been traversed
      setReflective(false);
    } else {
      traverse();
    }
    
    if (my_enum_constants == null) {
      result = nonEnumDefaultValues();
    } else {
      result = new ObjectArrayIterator<Object>(my_enum_constants);
    }
    
    setReflective(orig_reflective);
    if (!already_traversed) {
      untraverse();
    }
    return result;
  }
  
  /**
   * @return an iterator of default values for non-enum types.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private RepeatedAccessIterator<?> nonEnumDefaultValues() {
    final List<RepeatedAccessIterator<?>> result = 
      new LinkedList<RepeatedAccessIterator<?>>();
    result.add(new ObjectArrayIterator<Object>(new Object[] { null }));
    if (isReflective() && !my_generators.isEmpty()) {
      // try to return data generated using reflection
      final List<RepeatedAccessIterator<?>> iterators = 
        new LinkedList<RepeatedAccessIterator<?>>();
      for (Class<? extends Strategy> c : my_generators) {
        try {
          iterators.add(c.newInstance().iterator());
        } catch (InstantiationException e) {
          // should never happen because we checked it earlier
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          // should never happen because we checked it earlier
          e.printStackTrace();
        }
      }
      for (Class<?> c : my_non_generator_classes) {
        // add the default constructor for each non-generator class
        iterators.add
          (new InstantiationIterator(c, new Class<?>[0], 
                                     new ObjectArrayIterator<Object[]>(new Object[][]{{}})));
      }
      result.add(new NonNullMultiIterator(iterators));
    } else if (!isReflective()) {
      // no reflection, but we can still use default constructors
      final List<RepeatedAccessIterator<?>> iterators = 
        new LinkedList<RepeatedAccessIterator<?>>();
      for (Class<?> c : my_generator_classes) {
        // add the default constructor for each generator class
        iterators.add
          (new InstantiationIterator(c, new Class<?>[0], 
                                     new ObjectArrayIterator<Object[]>(new Object[][]{{}})));        
      }
      for (Class<?> c : my_non_generator_classes) {
        // add the default constructor for each non-generator class
        iterators.add
          (new InstantiationIterator(c, new Class<?>[0], 
                                     new ObjectArrayIterator<Object[]>(new Object[][]{{}})));        
      }
      result.add(new NonNullMultiIterator(iterators));
    }
    
    return new MultiIterator(result);
  }
  
  /**
   * Detects cycles in reflective object instantiation.
   * 
   * @return true if this strategy's data class has already been traversed
   * the currently-ongoing reflective object generation, false otherwise.
   */
  private boolean wasTraversed() {
    boolean result = false;
    
    if (TRAVERSED_CLASSES.get() != null) {
      result = TRAVERSED_CLASSES.get().contains(my_class);
    }
    
    return result;
  }
    
  /**
   * Adds this strategy's data class to the set of traversed classes.
   */
  private void traverse() {
    if (TRAVERSED_CLASSES.get() == null) {
      TRAVERSED_CLASSES.set(new HashSet<Class<?>>());
    }
    TRAVERSED_CLASSES.get().add(my_class);
  }
  
  /**
   * Removes this strategy's data class from the set of traversed classes.
   */
  private void untraverse() {
    if (TRAVERSED_CLASSES.get() != null) {
      TRAVERSED_CLASSES.get().remove(my_class);
    }
  }
}
