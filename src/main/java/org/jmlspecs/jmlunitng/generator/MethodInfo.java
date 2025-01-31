/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.generator;

import org.jmlspecs.jmlunitng.util.ProtectionLevel;

import java.util.*;

/**
 * Information about a method under test.
 *
 * @author Daniel M. Zimmerman
 * @author Jonathan Hogins
 * @version July 2011
 */
public class MethodInfo implements Comparable<MethodInfo> {
    /**
     * The static set of untestable method names.
     */
    private static final /*@ non_null @*/ Set<String> UNTESTABLE_METHOD_NAMES;

    static {
        final Set<String> untestable_methods = new HashSet<String>();
        untestable_methods.add("finalize");
        untestable_methods.add("getClass");
        untestable_methods.add("notify");
        untestable_methods.add("notifyAll");
        untestable_methods.add("wait");
        UNTESTABLE_METHOD_NAMES = Collections.unmodifiableSet(untestable_methods);
    }

    /**
     * The name of the method.
     */
    private final /*@ non_null @*/ String my_name;

    /**
     * The formatted name of the method (includes signature details).
     */
    private final /*@ non_null @*/ String my_formatted_name;

    /**
     * The abbreviated formatted name of the method (includes signature details,
     * but without fully qualified names).
     */
    private final /*@ non_null @*/ String my_abbreviated_formatted_name;

    /**
     * The protection level of the method.
     */
    private final /*@ non_null @*/ ProtectionLevel my_protection_level;

    /**
     * The parameter types of the method in order.
     */
    private final /*@ non_null @*/ List<ParameterInfo> my_parameters;

    /**
     * The name of the return type of the method.
     */
    private final /*@ non_null @*/ TypeInfo my_return_type;

    /**
     * The exception types that this method has signals clauses for.
     */
    private final /*@ non_null @*/ List<ClassInfo> my_signals;

    /**
     * The map from classes to literals declared in this class.
     */
    private final /*@ non_null @*/ Map<String, SortedSet<String>> my_literals;

    /**
     * The map from classes to literals declared in the specs for this class.
     */
    private final /*@ non_null @*/ Map<String, SortedSet<String>> my_spec_literals;

    /**
     * The ClassInfo for the class this method belongs to.
     */
    private final /*@ non_null @*/ ClassInfo my_enclosing_class;

    /**
     * The ClassInfo for the class this method is declared in.
     */
    private final /*@ non_null @*/ ClassInfo my_declaring_class;

    /**
     * Is the method static?
     */
    private final boolean my_is_static;

    /**
     * Is the method deprecated?
     */
    private final boolean my_is_deprecated;

    /**
     * Is the method a constructor?
     */
    private final boolean my_is_constructor;

    /*@ invariant my_is_inherited == !my_declaring_class.equals(my_parent_class); */
    /**
     * Is the method inherited?
     */
    private final boolean my_is_inherited;

    /**
     * Is the method a factory?
     */
    private final boolean my_is_factory;

    /**
     * Is the method a model method?
     */
    private final boolean my_is_model;

    /**
     * Is the method testable?
     */
    private final boolean my_is_testable;

    /**
     * Creates a MethodInfo object representing a method with the given
     * parameters.
     *
     * @param the_name             The name of the method.
     * @param the_enclosing_class  The ClassInfo for the class this method belongs to.
     * @param the_declaring_class  The ClassInfo for the class this method is
     *                             declared in.
     * @param the_protection_level The protection level of the method.
     * @param the_parameter_types  The parameter types of the method in order.
     * @param the_return_type      The name of the return type of the method.
     * @param the_signals          The exceptions listed in this method's signals clause.
     * @param the_literals         The literals found in this method.
     * @param the_spec_literals    The literals found in this method's specs.
     * @param the_is_constructor   Is the method a constructor?
     * @param the_is_static        Is the method static?
     * @param the_is_deprecated    Is the method deprecated?
     * @param the_is_model         Is the method a JML model method?
     */
    //@ requires !the_is_constructor || !the_is_static;
    public MethodInfo(final /*@ non_null @*/ String the_name,
                      final /*@ non_null @*/ ClassInfo the_enclosing_class,
                      final /*@ non_null @*/ ClassInfo the_declaring_class,
                      final /*@ non_null @*/ ProtectionLevel the_protection_level,
                      final /*@ non_null @*/ List<ParameterInfo> the_parameter_types,
                      final /*@ non_null @*/ TypeInfo the_return_type,
                      final /*@ non_null @*/ List<ClassInfo> the_signals,
                      final /*@ non_null @*/ Map<String, SortedSet<String>> the_literals,
                      final /*@ non_null @*/ Map<String, SortedSet<String>>
                              the_spec_literals,
                      final boolean the_is_constructor, final boolean the_is_static,
                      final boolean the_is_deprecated, final boolean the_is_model) {
        my_name = the_name;
        my_enclosing_class = the_enclosing_class;
        my_declaring_class = the_declaring_class;
        my_protection_level = the_protection_level;
        my_parameters = new ArrayList<ParameterInfo>(the_parameter_types);
        my_signals = new ArrayList<ClassInfo>(the_signals);
        my_literals = copyLiteralsMap(the_literals);
        my_spec_literals = copyLiteralsMap(the_spec_literals);
        my_return_type = the_return_type;
        my_is_static = the_is_static;
        my_is_constructor = the_is_constructor;
        my_is_deprecated = the_is_deprecated;
        my_is_model = the_is_model;

        my_is_inherited = !the_enclosing_class.equals(the_declaring_class);
        my_is_factory = determineIsFactory();
        my_is_testable =
                !(my_is_constructor && my_declaring_class.isAbstract()) &&
                        !my_protection_level.equals(ProtectionLevel.PRIVATE) &&
                        !my_is_model && !UNTESTABLE_METHOD_NAMES.contains(my_name);
        my_formatted_name = generateFormattedName();
        my_abbreviated_formatted_name = generateAbbreviatedFormattedName();
    }

    /**
     * Copies the specified literals map to a new map.
     *
     * @param the_map The literals map to copy.
     * @return The new map.
     */
    private static Map<String, SortedSet<String>>
    copyLiteralsMap(final Map<String, SortedSet<String>> the_map) {
        final Map<String, SortedSet<String>> result =
                new HashMap<String, SortedSet<String>>();
        for (Map.Entry<String, SortedSet<String>> e : the_map.entrySet()) {
            final SortedSet<String> new_set = new TreeSet<String>(e.getValue());
            result.put(e.getKey(), Collections.unmodifiableSortedSet(new_set));
        }
        return result;
    }

    /**
     * Generates the formatted name of the method, for use in filenames.
     * This is the full-length formatted name, including fully qualified
     * parameter type names.
     *
     * @return The formatted name.
     */
    private /*@ pure non_null @*/ String generateFormattedName() {
        final StringBuffer sb = new StringBuffer(my_name);
        for (ParameterInfo p : my_parameters) {
            sb.append("__");
            sb.append(p.getType().getFormattedName());
            sb.append("_");
            sb.append(p.getName());
        }
        return sb.toString();
    }

    /**
     * Generates the abbreviated formatted name of the method, for use
     * in filenames. This formatted name includes short parameter type
     * names, and a number (the difference in length between this name
     * and the full formatted name) for disambiguation.
     *
     * @return The abbreviated formatted name.
     */
    private /*@ pure non_null @*/ String generateAbbreviatedFormattedName() {
        final StringBuffer sb = new StringBuffer(my_name);
        for (ParameterInfo p : my_parameters) {
            sb.append("__");
            sb.append(p.getType().getFormattedShortName());
            sb.append('_');
            sb.append(p.getName());
        }
        final int diff = generateFormattedName().length() - sb.length();
        sb.append("__");
        sb.append(diff);
        return sb.toString();
    }

    /**
     * Generates the abbreviated formatted name of the method, for use
     * in filenames when length is a consideration. This includes short
     * parameter type names.
     *
     * @return The formatted name.
     */

    /**
     * @return The name of the method
     */
    public /*@ pure non_null @*/ String getName() {
        return my_name;
    }

    /**
     * @return The "formatted" name of the method, for use in generated
     * code; this name includes full details about the method signature.
     */
    public /*@ pure non_null @*/ String getFormattedName() {
        return my_formatted_name;
    }

    /**
     * @return The abbreviated "formatted" name of the method, for use in
     * generated code; this name does not include fully qualified names
     * for the parameters of the method.
     */
    public /*@ pure non_null @*/ String getAbbreviatedFormattedName() {
        return my_abbreviated_formatted_name;
    }

    /**
     * @return The ClassInfo object for the class that owns this method.
     */
    public /*@ pure non_null @*/ ClassInfo getEnclosingClass() {
        return my_enclosing_class;
    }

    /**
     * @return The ClassInfo object for the class that declared this method.
     */
    public /*@ pure non_null @*/ ClassInfo getDeclaringClass() {
        return my_declaring_class;
    }

    /**
     * @return The protection level of the method.
     */
    public /*@ pure non_null @*/ ProtectionLevel getProtectionLevel() {
        return my_protection_level;
    }

    /**
     * @return an unmodifiable list of the parameters of the method,
     * in the order they are declared in the parameter list.
     */
    public /*@ pure non_null @*/ List<ParameterInfo> getParameters() {
        return Collections.unmodifiableList(my_parameters);
    }

    /**
     * @return The return type of the method.
     */
    public /*@ pure @*/ TypeInfo getReturnType() {
        return my_return_type;
    }

    /**
     * @return an unmodifiable list of the signals of this method.
     */
    public /*@ pure non_null @*/ List<ClassInfo> getSignals() {
        return Collections.unmodifiableList(my_signals);
    }

    /**
     * Retrieve the literals of the specified class declared in
     * this method.
     *
     * @param the_class The class for which to get the literals.
     * @return A set of literals for the specified class, or
     * the empty set if no literals exist for the class.
     */
    //@ requires areLiteralsInitialized();
    public /*@ pure @*/ SortedSet<String>
    getLiterals(final String the_class) {
        final SortedSet<String> result = new TreeSet<String>();
        if (my_literals.get(the_class) != null) {
            result.addAll(my_literals.get(the_class));
        }
        return result;
    }

    /**
     * Retrieve the literals of the specified class declared in
     * the specifications of this method.
     *
     * @param the_class The class for which to get the literals.
     * @return A set of literals for the specified class, or
     * the empty set if no literals exist for the class.
     */
    //@ requires areLiteralsInitialized();
    public /*@ pure @*/ SortedSet<String>
    getSpecLiterals(final String the_class) {
        final SortedSet<String> result = new TreeSet<String>();
        if (my_spec_literals.get(the_class) != null) {
            result.addAll(my_spec_literals.get(the_class));
        }
        return result;
    }

    /**
     * Retrieve the entire map of literals declared in this method.
     *
     * @return An unmodifiable view of the map of literals.
     */
    public /*@ pure @*/ Map<String, SortedSet<String>> getLiterals() {
        return Collections.unmodifiableMap(my_literals);
    }

    /**
     * Retrieve the entire map of literals declared in this method's
     * specification.
     *
     * @return An unmodifiable view of the map of literals.
     */
    public /*@ pure @*/ Map<String, SortedSet<String>> getSpecLiterals() {
        return Collections.unmodifiableMap(my_spec_literals);
    }

    /**
     * @return True if this method is a constructor. False if not.
     */
    public /*@ pure @*/ boolean isConstructor() {
        return my_is_constructor;
    }

    /**
     * Returns true if this method is a factory method. A factory method is
     * defined as a static method whose return type is the same as the class it
     * belongs to or an abstract parent class thereof.
     *
     * @return True if this method is a factory. False otherwise.
     */
    public /*@ pure @*/ boolean isFactory() {
        return my_is_factory;
    }

    /**
     * Returns true if this method is a static method. False if not.
     *
     * @return True if this method is static. False if not.
     */
    public /*@ pure @*/ boolean isStatic() {
        return my_is_static;
    }

    /**
     * @return Is this method deprecated?
     */
    public /*@ pure @*/ boolean isDeprecated() {
        return my_is_deprecated;
    }

    /**
     * @return Is this method a JML model method?
     */
    public /*@ pure @*/ boolean isModel() {
        return my_is_model;
    }

    /**
     * Returns whether or not this method is testable. A method is testable if and
     * only if it a) is not a constructor of an abstract class,
     * b)has a non-private protection level, and c) is not (and does not
     * override) one of the following methods from java.lang.Object: finalize,
     * getClass, notify, notifyAll, wait.
     *
     * @return True if this method is testable. False otherwise.
     */
    public /*@ pure @*/ boolean isTestable() {
        return my_is_testable;
    }

    /**
     * @return True if this method was inherited. False otherwise.
     */
    public /*@ pure @*/ boolean isInherited() {
        return my_is_inherited;
    }

    /**
     * Determines whether or not this method is a factory method.
     *
     * @return True if this method is a factory. False otherwise.
     */
    private /*@ pure @*/ boolean determineIsFactory() {
        //decide if factory
        ClassInfo cur = my_declaring_class;
        while (cur != null && my_name.equals(cur.getShortName())) {
            cur = cur.getParent();
        }
        return my_is_static && cur != null;
    }

    /**
     * @return The method signature as a String.
     */
    public /*@ pure non_null @*/ String toString() {
        final StringBuilder sb = new StringBuilder();
        if (my_return_type != null && !my_is_constructor) {
            sb.append(my_return_type.getFullyQualifiedName());
            sb.append(" ");
        }
        sb.append(my_name);
        sb.append("(");
        final Iterator<ParameterInfo> paramIter = my_parameters.iterator();
        while (paramIter.hasNext()) {
            final ParameterInfo param = paramIter.next();
            sb.append(param.getType().getShortName());
            if (paramIter.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public /*@ pure @*/ boolean equals(final /*@ nullable @*/ Object the_other) {
        boolean result = false;

        if (the_other != this && the_other != null && the_other.getClass() == getClass()) {
            final MethodInfo method = (MethodInfo) the_other;
            result = equalsExceptSignals(method);
            result &= my_signals.equals(method.my_signals);
        } else if (the_other == this) {
            result = true;
        }

        return result;
    }

    /**
     * Compare two methods for equivalence modulo signals.
     *
     * @param the_other The other method.
     * @return true if these methods are identical aside from the signals
     * they generate.
     */
    public /*@ pure @*/ boolean equalsExceptSignals(final MethodInfo the_other) {
        boolean result = my_name.equals(the_other.my_name);
        result &= my_protection_level.equals(the_other.my_protection_level);
        result &= my_return_type.equals(the_other.my_return_type);
        result &= my_parameters.equals(the_other.my_parameters);
        result &= my_enclosing_class.equals(the_other.my_enclosing_class);
        result &= my_declaring_class.equals(the_other.my_declaring_class);
        result &= my_is_static == the_other.my_is_static;
        result &= my_is_deprecated == the_other.my_is_deprecated;
        result &= my_is_constructor == the_other.my_is_constructor;
        result &= my_is_inherited == the_other.my_is_inherited;
        result &= my_is_factory == the_other.my_is_factory;
        result &= my_is_testable == the_other.my_is_testable;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public /*@ pure @*/ int hashCode() {
        return toString().hashCode();
    }

    /**
     * Compares this MethodInfo to the_other; MethodInfos are compared based on their
     * String representations and their enclosing classes.
     *
     * @param the_other The other MethodInfo.
     * @return -1, 0 or 1 as this MethodInfo is less than, equivalent to, or greater
     * than the_other respectively.
     */
    public int compareTo(final MethodInfo the_other) {
        final String s =
                getDeclaringClass().toString() + getEnclosingClass().toString() + this;
        final String other_s =
                the_other.getDeclaringClass().toString() +
                        the_other.getEnclosingClass().toString() +
                        the_other;
        return s.compareTo(other_s);
    }
}
