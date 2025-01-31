/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.objgen;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A generator that uses a specified parameter list to create instances
 * of a specified class.
 *
 * @param <T> The type of the generated objects.
 * @author Daniel M. Zimmerman
 * @version January 2012
 */
public class Instantiator<T> implements ObjectGenerator<T> {
    /**
     * The lookup map from primitive classes to their wrappers.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS;

    // static initializer for the primitive class wrapper lookup map
    static {
        final Map<Class<?>, Class<?>> wrappers = new HashMap<Class<?>, Class<?>>();
        wrappers.put(Boolean.TYPE, Boolean.class);
        wrappers.put(Byte.TYPE, Byte.class);
        wrappers.put(Character.TYPE, Character.class);
        wrappers.put(Double.TYPE, Double.class);
        wrappers.put(Float.TYPE, Float.class);
        wrappers.put(Integer.TYPE, Integer.class);
        wrappers.put(Long.TYPE, Long.class);
        wrappers.put(Short.TYPE, Short.class);
        wrappers.put(Void.TYPE, Void.class);

        PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(wrappers);
    }

    /**
     * The class of the objects being instantiated.
     */
    private final /*@ non_null @*/ Class<?> my_class;

    /**
     * The constructor to use for instantiating the objects.
     */
    private final /*@ non_null @*/ Constructor<T> my_constructor;

    /**
     * The parameter list to use for instantiating the objects.
     */
    private final /*@ non_null @*/ Object[] my_params;

    /**
     * Constructs a new Instantiator to instantiate objects of the
     * specified class with the specified parameters. If any of the
     * parameters are ObjectGenerators, the objects generated by them
     * will be used. The parameter list is not cloned, so subsequent
     * modifications to it (if supplied as an array) will be reflected
     * in the instantiated objects.
     *
     * @param the_class  The class to instantiate, must be assignable to T.
     * @param the_params The parameters to instantiate the class with.
     * @throws IllegalArgumentException if no constructor for the
     *                                  specified class can be found that can take the specified parameters,
     *                                  multiple such constructors are found, or the constructor found
     *                                  cannot successfully be used with the specified parameters.
     */
    public Instantiator(final /*@ non_null @*/ Class<?> the_class,
                        final Object... the_params)
            throws IllegalArgumentException {
        my_class = the_class;
        my_params = the_params;
        my_constructor = findConstructor();
    }

    /**
     * Constructs a new Instantiator to instantiate objects assignable to the
     * specified class with the specified parameters using the specified
     * constructor. If any of the parameters are ObjectGenerators, the
     * objects generated by them will be used. The parameter list is not
     * cloned, so subsequent modifications to it (if supplied as an array)
     * will be reflected in the instantiated objects.
     *
     * @param the_class       The class to instantiate, must be assignable to T.
     * @param the_constructor The constructor to use, must be a constructor
     *                        of a class assignable to the_class.
     * @param the_params      The parameters to instantiate the class with.
     * @throws ClassCastException       if the specified constructor is not for
     *                                  a class assignable to the_class.
     * @throws IllegalArgumentException if the specified constructor
     *                                  cannot successfully be used with the supplied parameters.
     */
    public Instantiator(final /*@ non_null @*/ Class<?> the_class,
                        final /*@ non_null @*/ Constructor<T> the_constructor,
                        final Object... the_params)
            throws ClassCastException, IllegalArgumentException {
        my_class = the_class;
        my_constructor = the_constructor;
        my_params = the_params;
        if (!my_class.isAssignableFrom(my_constructor.getDeclaringClass())) {
            throw new ClassCastException(the_constructor +
                    " is not a constructor for " + the_class);
        }
        try {
            my_constructor.newInstance(actualParams());
        } catch (final Exception e) {
            // normally we wouldn't catch "Exception", but the end result here is
            // the same for any exception
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @return an array of actual parameters (after all recursive object
     * generation) to use for the constructor call.
     */
    private Object[] actualParams() {
        final Object[] result = new Object[my_params.length];
        for (int i = 0; i < my_params.length; i++) {
            final Object o = my_params[i];
            if (o instanceof ObjectGenerator) {
                result[i] = ((ObjectGenerator<?>) o).generate();
            } else {
                result[i] = o;
            }
        }
        return result;
    }

    /**
     * @return an appropriate constructor to use for this instantiator.
     * @throws IllegalArgumentException if there is no appropriate constructor,
     *                                  or if an error occurs while trying to use it.
     */
    private Constructor<T> findConstructor() throws IllegalArgumentException {
        final Class<?>[] param_classes = new Class[my_params.length];

        for (int i = 0; i < my_params.length; i++) {
            final Object o = my_params[i];
            if (o == null) {
                param_classes[i] = null;
                // the parameter class at index i stays null
            } else if (ObjectGenerator.class.isAssignableFrom(o.getClass())) {
                param_classes[i] = ((ObjectGenerator<?>) o).generatedClass();
            } else {
                param_classes[i] = o.getClass();
            }
        }

        Constructor<T> result = findMatchingConstructors(param_classes);

        if (result != null) {
            try {
                result.newInstance(actualParams());
            } catch (final Exception e) {
                // normally we wouldn't catch "Exception", but the end result here is
                // the same for any exception
                e.printStackTrace();
                result = null;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("could not find an appropriate " + my_class +
                    "constructor for supplied parameters");
        }

        return result;
    }

    /**
     * Finds matching constructors for the specified parameter classes, some of
     * which are null.
     *
     * @param the_classes The parameter classes.
     * @return the matching constructor, or null if no such constructor exists.
     */
    @SuppressWarnings("unchecked")
    private Constructor<T> findMatchingConstructors(final Class<?>[] the_classes) {
        Constructor<T> result = null;

        for (Constructor<?> c : my_class.getConstructors()) {
            final Class<?>[] param_classes = c.getParameterTypes();
            if (param_classes.length == the_classes.length) {
                int i = 0;
                boolean match = true;
                while (i < param_classes.length) {
                    if (the_classes[i] == null) {
                        match = match && !param_classes[i].isPrimitive();
                    } else if (param_classes[i].isPrimitive()) {
                        // we can't match primitives with objects, so we need to convert
                        // the primitive
                        match = match &&
                                PRIMITIVE_WRAPPERS.get(param_classes[i]).isAssignableFrom(the_classes[i]);

                    } else {
                        match = match && param_classes[i].isAssignableFrom(the_classes[i]);
                    }
                    i = i + 1;
                }
                if (match && result != null) {
                    // we found more than one matching constructor
                    result = null;
                    break;
                } else if (match) {
                    result = (Constructor<T>) c;
                }
            }
        }

        return result;
    }

    @Override
    public T generate() {
        T result = null;
        try {
            result = my_constructor.newInstance(actualParams());
        } catch (final Exception e) {
            // normally we wouldn't catch "Exception", but the end result here is
            // the same for any exception
            result = null;
        }
        return result;
    }

    @Override
    public Class<?> generatedClass() {
        return my_class;
    }
}
