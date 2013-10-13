/*
 * JMLUnitNG 
 * Copyright (C) 2010-12
 */

package org.jmlspecs.jmlunitng.strategy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.jmlspecs.jmlunitng.iterator.IteratorSampler;
import org.jmlspecs.jmlunitng.iterator.MultiIterator;
import org.jmlspecs.jmlunitng.iterator.ObjectArrayIterator;
import org.jmlspecs.jmlunitng.iterator.RepeatedAccessIterator;

/**
 * The parent strategy for all non-primitive types.
 * 
 * @author Daniel M. Zimmerman
 * @version July 2011
 */
public abstract class NonPrimitiveStrategy extends AbstractStrategy {
  /**
   * The class for which this strategy was made.
   */
  protected final Class<?> my_class;

  /**
   * The default data class for this strategy. For 
   * regular object strategies this is the same as the class
   * for which the strategy was made; for array strategies it
   * is the component type.
   */
  protected final Class<?> my_default_data_class;
  
  /**
   * The test data generators found for this strategy to use.
   */
  protected final List<Class<? extends Strategy>> my_generators;
  
  /**
   * The classes that are generated by the test data generators.
   */
  protected final List<Class<?>> my_generator_classes;
  
  /**
   * The classes for which we should use the default constructors
   * since no test data generators were found.
   */
  protected final List<Class<?>> my_non_generator_classes;
  
  /**
   * Should we use reflective data generation?
   */
  private boolean my_reflective;
  
  /**
   * Constructs a NonPrimitiveStrategy for the specified class
   * and default data class. By default, reflection will not be used.
   * 
   * @param the_class The class.
   * @param the_default_data_class The default data class.
   */
  public NonPrimitiveStrategy(final Class<?> the_class, 
                              final Class<?> the_default_data_class) {
    super();
    my_class = the_class;
    my_default_data_class = the_default_data_class;
    my_reflective = false;
    my_generators = new ArrayList<Class<? extends Strategy>>();
    my_generator_classes = new ArrayList<Class<?>>();
    my_non_generator_classes = new ArrayList<Class<?>>();
    if (the_default_data_class != null) {
      addDataClass(the_default_data_class);
    }
  }

  /**
   * A default empty iterator, may be overridden by child classes.
   * 
   * @return An empty iterator.
   */
  public RepeatedAccessIterator<?> localValues() {
    return emptyIterator();
  }

  /**
   * A default empty iterator, may be overridden by child classes.
   * 
   * @return An empty iterator.
   */
  public RepeatedAccessIterator<?> classValues() {
    return emptyIterator();
  }

  /**
   * A default empty iterator, may be overridden by child classes.
   * 
   * @return An empty iterator.
   */
  public RepeatedAccessIterator<?> packageValues() {
    return emptyIterator();
  }
  
  /**
   * Returns a RepeatedAccessIterator over all values in the order: local-scope
   * values, class-scope values, package-scope values, default values.
   * 
   * @return What are all your values?
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public RepeatedAccessIterator<?> iterator() {
    final List<RepeatedAccessIterator<?>> iterators = 
      new ArrayList<RepeatedAccessIterator<?>>(4);
    iterators.add(localValues());
    iterators.add(classValues());
    iterators.add(packageValues());
    iterators.add(defaultValues());
    RepeatedAccessIterator<?> result = new MultiIterator(iterators);
    if (fraction() < 1.0) {
      result = new IteratorSampler(result, fraction(), seed());
    }
    return result;
  } 
  
  /**
   * Controls the use of reflection by this strategy.
   * 
   * @param the_reflective true to enable the use of reflection to
   * generate objects, false otherwise.
   */
  public final void setReflective(final boolean the_reflective) {
    my_reflective = the_reflective;
  }
  
  /**
   * @return true if this strategy is using reflection, false otherwise.
   */
  public final boolean isReflective() {
    return my_reflective;
  }
  
  /**
   * @return an empty iterator of this strategy's data type.
   */
  public final ObjectArrayIterator<Object> emptyIterator() {
    return new ObjectArrayIterator<Object>
    ((Object[]) Array.newInstance(my_class, 0));
  }
  
  /**
   * Adds a data class to be used by this strategy.
   * 
   * @param the_class The new data class.
   * @exception ClassCastException if the new data class cannot
   *  be cast to the default data class of this strategy.
   */
  @SuppressWarnings("unchecked")
  protected final void addDataClass(final Class<?> the_class) 
    throws ClassCastException {
    if (my_default_data_class == null) {
      throw new ClassCastException
      ("Cannot add a data class to strategy for " + my_class);
    } else if (!my_default_data_class.isAssignableFrom(the_class)) {
      throw new ClassCastException
      ("Cannot add " + the_class + " as a data class to strategy for " + my_class);
    } else if (!my_generator_classes.contains(the_class) &&
               !my_non_generator_classes.contains(the_class)) { 
      // it's not already added, so we can add it
      final Class<?> generator_class = findStrategyClass(the_class); 
      if (generator_class != null &&
          Strategy.class.isAssignableFrom(generator_class)) {
        my_generators.add((Class<? extends Strategy>) generator_class);
        my_generator_classes.add(the_class);
      } else {
        my_non_generator_classes.add(the_class);
      }
    }
  }
  
  /**
   * Clears the list of data classes to be used by this strategy; note that
   * it always uses the data class for its own component type, so that data
   * class is never cleared.
   */
  protected final void clearDataClasses() {
    my_generators.clear();
    my_generator_classes.clear();
    my_non_generator_classes.clear();
    
    addDataClass(my_class.getComponentType());
  }

  /**
   * Finds the appropriate strategy class to use for the specified class.
   * 
   * @param the_class The class to find a strategy class for.
   * @return the strategy class, or null if no strategy class can be loaded.
   */
  private Class<?> findStrategyClass(final Class<?> the_class) {
    Class<?> result = null;
    final String class_name = the_class.getCanonicalName();
    result = loadClass(class_name + "_InstanceStrategy");
    
    if (result == null) {
      final String formatted_name = formatClassName(class_name);
      // no instance strategy, try to find a class strategy in our "package"
      if (getClass().getPackage() == null) {
        // the first bit of our name, before the first underscore, is our prefix
        final String prefix = 
          getClass().getName().substring(0, getClass().getName().indexOf('_'));
        // a class strategy name looks like "prefix_ClassStrategy_formattedclassname"
        result = 
          loadClass(prefix + "_ClassStrategy_" + formatted_name);
      } else {
        // a class strategy name, in our package, looks like 
        // "ClassStrategy_formattedclassname"
        final String pkg_name = 
          getClass().getName().substring(0, getClass().getName().lastIndexOf('.'));
        result = loadClass(pkg_name + ".ClassStrategy_" + formatted_name);
      }
    }
  
    if (result == null) {
      final String formatted_name = formatClassName(class_name);
      // no instance or class strategy, try to find a package strategy
      if (my_class.getPackage() == null) {
        // a package strategy name looks like "PackageStrategy_formattedclassname"
        result = loadClass("PackageStrategy_" + formatted_name);
      } else {
        // we need to look in our parent package
        final String pkg_name = 
          my_class.getName().substring(0, my_class.getName().lastIndexOf('.'));
        final String parent_pkg_name = 
          pkg_name.substring(0, pkg_name.lastIndexOf('.'));
        result = loadClass(parent_pkg_name + ".PackageStrategy_" + formatted_name);
      }
    }
    
    // try to instantiate the strategy we found to make sure it works, unless
    // it's this class, in which case we already know it works
    
    if (result != getClass()) {
      try {
        result.newInstance();
      } catch (final InstantiationException e) {
        result = null;
      } catch (final IllegalAccessException e) {
        result = null;
      } catch (final NullPointerException e) {
        result = null;
      }
    }
    
    // and that's it; if we didn't find anything, that's too bad, we return null
    return result;
  }

  /**
   * Attempts to load the specified class.
   * 
   * @param the_name The name of the class.
   * @return the class, or null if it does not exist.
   */
  private Class<?> loadClass(final String the_name) {
    try {
      return Class.forName(the_name);
    } catch (final ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Formats the name of a class, for use in locating strategies.
   * 
   * @param the_name The name to format.
   * @return The formatted name.
   */
  private String formatClassName(final String the_name) {
    final StringBuilder formatted = new StringBuilder(the_name.replace('.', '_'));
    if (the_name.contains("[]")) {
      final int array_dimension = 
        the_name.substring(the_name.indexOf("[]"), the_name.length()).length() / 2;
      formatted.delete(formatted.indexOf("[]"), formatted.length());
      formatted.append(array_dimension + "DArray");
    } 
    return formatted.toString();
  }

}
