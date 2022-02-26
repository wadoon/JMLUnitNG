/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.generator;

import org.jmlspecs.jmlunitng.JMLUnitNG;
import org.jmlspecs.jmlunitng.JMLUnitNGConfiguration;
import org.jmlspecs.jmlunitng.util.Logger;
import org.jmlspecs.jmlunitng.util.ProtectionLevel;
import org.jmlspecs.jmlunitng.util.StringTemplateUtil;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generator for classes that contain unit tests.
 *
 * @author Jonathan Hogins
 * @author Daniel M. Zimmerman
 * @version July 2011
 */
public class TestClassGenerator {
    /**
     * Valid RAC versions.
     */
    public static final List<String> VALID_RAC_VERSIONS;
    /**
     * The default max protection level.
     */
    public static final ProtectionLevel DEF_PROTECTION_LEVEL = ProtectionLevel.PUBLIC;
    /**
     * Are inherited methods tested by default?
     */
    public static final boolean DEF_TEST_INHERITED_METHODS = false;
    /**
     * Are deprecated methods tested by default?
     */
    public static final boolean DEF_TEST_DEPRECATED_METHODS = false;
    /**
     * Do test data classes use reflection by default?
     */
    public static final boolean DEF_USE_REFLECTION = true;
    /**
     * Do test data classes use children by default?
     */
    public static final boolean DEF_USE_CHILDREN = true;
    /**
     * The default RAC version to generate tests for.
     */
    public static final String DEF_RAC_VERSION = "openjml";
    /**
     * The line max line width of generated code.
     */
    public static final int LINE_WIDTH = 120;

    static {
        final List<String> temp = new ArrayList<>();
        temp.add("jml4");
        temp.add("jml2");
        temp.add("openjml");
        VALID_RAC_VERSIONS = Collections.unmodifiableList(temp);
    }

    /**
     * The configuration to use for generating classes.
     */
    private final JMLUnitNGConfiguration my_config;

    /**
     * The logger to use for printing output.
     */
    private final Logger my_logger;

    /**
     * The set of files we have created.
     */
    private final Set<String> my_created_files = new HashSet<>();

    /**
     * Create a new TestClassGenerator with the default options.
     */
    public TestClassGenerator() {
        this(new JMLUnitNGConfiguration(), new Logger(false));
    }

    /**
     * Create a new TestClassGenerator with the given configuration
     * and logger.
     *
     * @param the_config The JMLUnitNGConfiguration to use.
     * @param the_logger The logger to use to generate output.
     */
    public TestClassGenerator(final JMLUnitNGConfiguration the_config,
                              final Logger the_logger) {
        my_config = the_config;
        my_logger = the_logger;
    }

    /**
     * Generates a local strategy class for the specified method parameter.
     *
     * @param the_class  The class to generate a strategy class for.
     * @param the_method The method to generate a strategy class for.
     * @param the_param  The parameter to generate a strategy class for.
     * @param the_writer The writer to write the strategy class to.
     * @throws IOException if an IOException occurs while writing the class.
     */
    //@ requires the_class.getMethods().contains(the_method);
    //@ requires the_method.getParameters().contains(the_param);
    public void generateLocalStrategyClass(final /*@ non_null @*/ ClassInfo the_class,
                                           final /*@ non_null @*/ MethodInfo the_method,
                                           final /*@ non_null @*/ ParameterInfo the_param,
                                           final /*@ non_null @*/ Writer the_writer)
            throws IOException {
        final STGroup group = StringTemplateUtil.load("strategy_local");
        final ST t = group.getInstanceOf("main");
        final SortedSet<String> children = new TreeSet<>();
        final SortedSet<String> literals = new TreeSet<>();
        final String fq_name = the_param.getType().getFullyQualifiedName();
        final ClassInfo type_class_info = InfoFactory.getClassInfo(fq_name);

        // if "--children" was set, we use all child classes we are currently analyzing
        if (my_config.isChildrenSet() && type_class_info != null) {
            children.addAll(getChildrenFromClassInfo(type_class_info));
        }

        // if "--literals" or "--spec-literals" was set, we use all child classes
        // identified as literals for the method under test

        if (my_config.isLiteralsSet() && type_class_info != null) {
            children.addAll(checkChildLiterals(type_class_info,
                    the_method.getLiterals(Class.class.getName())));
        }
        if (my_config.isSpecLiteralsSet() && type_class_info != null) {
            children.addAll(checkChildLiterals(type_class_info,
                    the_method.getSpecLiterals(Class.class.getName())));
        }

        // add literals for this type

        if (my_config.isLiteralsSet()) {
            literals.addAll(the_method.getLiterals(fq_name));
        }
        if (my_config.isSpecLiteralsSet()) {
            literals.addAll(the_method.getSpecLiterals(fq_name));
        }

        t.add("class", the_class);
        t.add("date", getFormattedDate());
        t.add("method", the_method);
        t.add("param", the_param);
        t.add("literals", literals);
        t.add("jmlunitng_version", JMLUnitNG.version());
        t.add("use_reflection", my_config.isReflectionSet());
        t.add("children", children);

        if (!my_config.isNoGenSet()) {
            my_logger.println("Generating local strategy for parameter " + the_param.getName() +
                    " of " + the_method);
        }

        the_writer.write(t.render(LINE_WIDTH));
    }

    /**
     * Generates a class-scope strategy class for the specified type.
     *
     * @param the_class  The class to generate a strategy class for.
     * @param the_type   The type to generate a strategy class for.
     * @param the_writer The writer to write the strategy class to.
     * @throws IOException if an IOException occurs while writing the class.
     */
    //@ requires the_class.getMethods().contains(the_method);
    //@ requires the_method.getParameters().contains(the_param);
    public void generateClassStrategyClass(final /*@ non_null @*/ ClassInfo the_class,
                                           final /*@ non_null @*/ TypeInfo the_type,
                                           final /*@ non_null @*/ Writer the_writer)
            throws IOException {
        final STGroup group = StringTemplateUtil.load("strategy_class");
        final ST t = group.getInstanceOf("main");
        final SortedSet<String> children = new TreeSet<>();
        final SortedSet<String> literals = new TreeSet<>();
        final String fq_name = the_type.getFullyQualifiedName();
        final ClassInfo type_class_info = InfoFactory.getClassInfo(fq_name);

        // if "--children" was set, we use all child classes we are currently analyzing
        if (my_config.isChildrenSet() && type_class_info != null) {
            children.addAll(getChildrenFromClassInfo(type_class_info));
        }

        // if "--literals" or "--spec-literals" was set, we use all child classes
        // identified as literals for the method under test

        if (my_config.isLiteralsSet() && type_class_info != null) {
            children.addAll(checkChildLiterals(type_class_info,
                    the_class.getLiterals(Class.class.getName())));
        }
        if (my_config.isSpecLiteralsSet() && type_class_info != null) {
            children.addAll(checkChildLiterals(type_class_info,
                    the_class.getSpecLiterals(Class.class.getName())));
        }

        // add literals for this type

        if (my_config.isLiteralsSet()) {
            literals.addAll(the_class.getLiterals(fq_name));
        }
        if (my_config.isSpecLiteralsSet()) {
            literals.addAll(the_class.getSpecLiterals(fq_name));
        }

        t.add("class", the_class);
        t.add("date", getFormattedDate());
        t.add("type", the_type);
        t.add("literals", literals);
        t.add("jmlunitng_version", JMLUnitNG.version());
        t.add("use_reflection", my_config.isReflectionSet());
        t.add("children", children);

        if (!my_config.isNoGenSet()) {
            my_logger.println("Generating class strategy for type " +
                    the_type.getFullyQualifiedName());
        }

        the_writer.write(t.render(LINE_WIDTH));
    }

    /**
     * Generates a package-scope strategy class for the specified type.
     *
     * @param the_class  The class to generate a strategy class for.
     * @param the_type   The type to generate a strategy class for.
     * @param the_writer The writer to write the strategy class to.
     * @throws IOException if an IOException occurs while writing the class.
     */
    //@ requires the_class.getMethods().contains(the_method);
    //@ requires the_method.getParameters().contains(the_param);
    public void generatePackageStrategyClass(final /*@ non_null @*/ ClassInfo the_class,
                                             final /*@ non_null @*/ TypeInfo the_type,
                                             final /*@ non_null @*/ Writer the_writer)
            throws IOException {
        final STGroup group = StringTemplateUtil.load("strategy_package");
        final ST t = group.getInstanceOf("main");
        final SortedSet<String> children = new TreeSet<>();

        final ClassInfo type_class_info =
                InfoFactory.getClassInfo(the_type.getFullyQualifiedName());

        // if "--children" was set, we use all child classes we are currently analyzing
        if (my_config.isChildrenSet() && type_class_info != null) {
            children.addAll(getChildrenFromClassInfo(type_class_info));
        }


        String pkg = null;
        if (the_class.isPackaged()) {
            pkg = the_class.getPackageName();
        }
        t.add("package", pkg);
        t.add("date", getFormattedDate());
        t.add("type", the_type);
        t.add("jmlunitng_version", JMLUnitNG.version());
        t.add("use_reflection", my_config.isReflectionSet());
        t.add("children", children);

        if (!my_config.isNoGenSet()) {
            my_logger.println("Generating package strategy for type " +
                    the_type.getFullyQualifiedName());
        }

        the_writer.write(t.render(LINE_WIDTH));
    }

    /**
     * Generates the instance strategy class for the specified class.
     *
     * @param the_class  The class to generate a strategy class for.
     * @param the_writer The writer to write the strategy class to.
     * @throws IOException if an IOException occurs while writing the class.
     */
    //@ requires the_class.getMethods().contains(the_method);
    //@ requires the_method.getParameters().contains(the_param);
    public void generateInstanceStrategyClass(final /*@ non_null @*/ ClassInfo the_class,
                                              final /*@ non_null @*/ Writer the_writer)
            throws IOException {
        final STGroup group = StringTemplateUtil.load("strategy_instance");
        final ST t = group.getInstanceOf("main");

        t.add("class", the_class);
        t.add("date", getFormattedDate());
        t.add("jmlunitng_version", JMLUnitNG.version());
        t.add("use_reflection", my_config.isReflectionSet());

        if (!my_config.isNoGenSet()) {
            my_logger.println("Generating instance strategy for class " +
                    the_class.getFullyQualifiedName());
        }

        the_writer.write(t.render(LINE_WIDTH));
    }
  
  /*@ requires (\forall MethodInfo m; the_methods.contains(m); 
    @           the_class.getMethods().contains(m));
    @*/

    /**
     * Generates a test class for the_class and writes it to the_writer.
     *
     * @param the_class   The class to generate a test class for.
     * @param the_methods The methods to generate tests for.
     * @param the_writer  The writer to write the test class to.
     * @throws IOException if an IOException occurs while writing the class.
     */
    public void generateTestClass(final /*@ non_null @*/ ClassInfo the_class,
                                  final /*@ non_null @*/ Set<MethodInfo> the_methods,
                                  final /*@ non_null @*/ Writer the_writer)
            throws IOException {
        final STGroup group =
                StringTemplateUtil.load("test_class_" + my_config.getRACVersion());
        final ST t = group.getInstanceOf("main");
        t.add("class", the_class);
        t.add("date", getFormattedDate());
        t.add("methods", the_methods);

        // if there are no methods with parameters to generate tests for,
        // we don't need a data package
        boolean params = false;
        for (MethodInfo m : the_methods) {
            params = params || !m.getParameters().isEmpty();
        }
        t.add("params", params);
        t.add("package_name", the_class.getPackageName());
        t.add("packaged", !"".equals(the_class.getPackageName()));
        t.add("parallel", my_config.isParallelSet());
        t.add("jmlunitng_version", JMLUnitNG.version());

        if (!my_config.isNoGenSet()) {
            my_logger.println("Generating test class for class " +
                    the_class.getFullyQualifiedName());
        }

        the_writer.write(t.render(LINE_WIDTH));
    }

    /**
     * Generates both test and test data classes and writes them to the given
     * directory.
     *
     * @param the_class        The class for which to generate test classes.
     * @param the_test_dir     The directory in which to generate test classes, as well
     *                         as package and instance strategies.
     * @param the_strategy_dir The directory in which to generate parameter and class
     *                         strategies.
     * @throws IOException Thrown if an IOException occurs while generating the classes.
     */
    //@ requires VALID_RAC_VERSIONS.contains(the_rac);
    //@ requires (new File(the_dir)).isDirectory();
    public void generateClasses(final /*@ non_null @*/ ClassInfo the_class,
                                final /*@ non_null @*/ String the_test_dir,
                                final /*@ non_null @*/ String the_strategy_dir)
            throws IOException {
        final STGroup shared = StringTemplateUtil.load("shared_java");

        final Set<MethodInfo> methods_to_test = getMethodsToTest(the_class);
        final Set<ClassInfo> classes_to_test = getClassesToTest(the_class);

        // we don't test nested classes yet but we can say something
        for (ClassInfo c : classes_to_test) {
            my_logger.println("No test generation yet for nested class " +
                    c.getFullyQualifiedName());
        }

        if (methods_to_test.isEmpty()) {
            my_logger.println("No testable methods in class " + the_class.getFullyQualifiedName());
            return;
        }

        // initialize name templates
        final ST tc_name = shared.getInstanceOf("testClassName");
        tc_name.add("classInfo", the_class);

        // this stream is for writing to memory, in the case of a dry run

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos));

        File f;

        // generate the (single) test class, if necessary

        f = new File(the_test_dir + tc_name.render() + JMLUnitNG.JAVA_SUFFIX);
        if (my_config.isDryRunSet() || my_config.isNoGenSet()) {
            generateTestClass(the_class, methods_to_test, bw);
            baos.reset();
        } else {
            final FileWriter fw = new FileWriter(f);
            generateTestClass(the_class, methods_to_test, fw);
            fw.close();
        }
        my_created_files.add(f.getCanonicalPath());

        // generate the strategy classes - there are three stages here
        // first: local-scope method parameter strategy classes, only if concrete

        for (MethodInfo m : methods_to_test) {
            for (ParameterInfo p : m.getParameters()) {
                final ST ls_name = shared.getInstanceOf("localStrategyName");
                ls_name.add("classInfo", the_class);
                ls_name.add("methodInfo", m);
                ls_name.add("paramInfo", p);
                f = new File(the_strategy_dir + ls_name.render() +
                        JMLUnitNG.JAVA_SUFFIX);
                if (my_config.isDryRunSet() || my_config.isNoGenSet()) {
                    generateLocalStrategyClass(the_class, m, p, bw);
                    baos.reset();
                } else if (f.exists()) {
                    my_logger.println("Not overwriting existing strategy for parameter " +
                            p.getName() + " of " + m);
                } else {
                    final FileWriter fw = new FileWriter(f);
                    generateLocalStrategyClass(the_class, m, p, fw);
                    fw.close();
                }
                my_created_files.add(f.getCanonicalPath());
            }
        }

        // second: class-scope strategy classes for all data types, only if concrete

        final Set<TypeInfo> parameterTypes = getUniqueParameterTypes(methods_to_test);

        for (TypeInfo t : parameterTypes) {
            final ST parameters = shared.getInstanceOf("classStrategyName");
            parameters.add("classInfo", the_class);
            parameters.add("typeInfo", t);
            f = new File(the_strategy_dir + parameters.render() + JMLUnitNG.JAVA_SUFFIX);
            if (my_config.isDryRunSet() || my_config.isNoGenSet()) {
                generateClassStrategyClass(the_class, t, bw);
                baos.reset();
            } else if (f.exists()) {
                my_logger.println("Not overwriting existing global strategy " +
                        "for type " + t.getFullyQualifiedName());
            } else {
                final FileWriter fw = new FileWriter(f);
                generateClassStrategyClass(the_class, t, fw);
                fw.close();
            }
            my_created_files.add(f.getCanonicalPath());
        }

        // third: package strategy classes for all types for which strategies
        // were generated above (note that these may duplicate when we generate
        // multiple sets of tests in the same package, but that's OK, as
        // we won't overwrite them after the first one)

        for (TypeInfo t : parameterTypes) {
            final ST ps_name = shared.getInstanceOf("packageStrategyName");
            ps_name.add("typeInfo", t);

            f = new File(the_test_dir + ps_name.render() +
                    JMLUnitNG.JAVA_SUFFIX);
            if (my_config.isDryRunSet() || my_config.isNoGenSet()) {
                generatePackageStrategyClass(the_class, t, bw);
                baos.reset();
            } else if (f.exists()) {
                String pn = "<default>";
                if (the_class.isPackaged()) {
                    pn = the_class.getPackageName();
                }
                my_logger.println("Not overwriting existing package strategy " +
                        "for type " + t.getFullyQualifiedName() +
                        " in package " + pn);
            } else {
                final FileWriter fw = new FileWriter(f);
                generatePackageStrategyClass(the_class, t, fw);
                fw.close();
            }
            my_created_files.add(f.getCanonicalPath());
        }

        // fourth: instance strategy class for this class

        final ST is_name = shared.getInstanceOf("instanceStrategyName");
        is_name.add("classInfo", the_class);
        f = new File(the_test_dir + is_name.render() +
                JMLUnitNG.JAVA_SUFFIX);
        if (my_config.isDryRunSet() || my_config.isNoGenSet()) {
            generateInstanceStrategyClass(the_class, bw);
            baos.reset();
        } else if (f.exists()) {
            my_logger.println("Not overwriting existing instance strategy " +
                    "for class " + the_class.getFullyQualifiedName());
        } else {
            final FileWriter fw = new FileWriter(f);
            generateInstanceStrategyClass(the_class, fw);
            fw.close();
        }
        my_created_files.add(f.getCanonicalPath());

    }

    /**
     * @return an unmodifiable view of the set of files created by this
     * generator.
     */
    public /*@ pure @*/ Set<String> getCreatedFiles() {
        return Collections.unmodifiableSet(my_created_files);
    }

    /**
     * @return a formatted version of the current date and time.
     */
    private String getFormattedDate() {
        final SimpleDateFormat df =
                new SimpleDateFormat("yyyy-MM-dd HH:mm Z", Locale.US);
        return df.format(new Date());
    }
  
  /*@ ensures (\forall MethodInfo m; \result.contains(m); 
    @   m.isTestable() && 
    @   ((m.isInherited() && my_test_inherited_methods) || !m.isInherited()) &&
    @   ((m.isDeprecated() && my_test_deprecated_methods) || !m.isDeprecated()));
   */

    /**
     * Returns the methods from the given class to test based on generator
     * settings.
     *
     * @param classInfo The class for which to find testable methods.
     * @return A list of methods in classInfo to test.
     */
    private /*@ pure non_null @*/ Set<MethodInfo> getMethodsToTest
    (final /*@ non_null @*/ ClassInfo classInfo) {
        final Set<MethodInfo> methods = new HashSet<>();
        for (MethodInfo m : classInfo.getTestableMethods()) {
            if (m.getProtectionLevel().weakerThanOrEqualTo(my_config.getProtectionLevel()) &&
                    (my_config.isInheritedSet() || !m.isInherited()) &&
                    (my_config.isDeprecationSet() || !m.isDeprecated())) {
                methods.add(m);
            }
        }
        return methods;
    }

    /**
     * Returns the nested classes from the given class to test based on
     * generator settings.
     *
     * @param classInfo The class for which to find testable nested classes.
     * @return A list of nested classes in classInfo to test.
     */
    private /*@ pure non_null @*/ Set<ClassInfo> getClassesToTest
    (final /*@ non_null @*/ ClassInfo classInfo) {
        final Set<ClassInfo> classes = new HashSet<>();
        for (ClassInfo c : classInfo.getNestedClasses()) {
            if (!c.isInner() &&
                    c.getProtectionLevel().weakerThanOrEqualTo(my_config.getProtectionLevel())) {
                classes.add(c);
            }
        }
        return classes;
    }

    /**
     * Returns the basic types present in the parameters of the given methods.
     *
     * @param methodInfos The methods for which to find parameter basic types.
     * @return A list of basic types.
     */
    private /*@ pure non_null @*/ Set<TypeInfo> getUniqueParameterTypes
    (final /*@ non_null @*/ Set<MethodInfo> methodInfos) {
        final SortedSet<TypeInfo> classes = new TreeSet<>();
        for (MethodInfo m : methodInfos) {
            for (ParameterInfo p : m.getParameters()) {
                classes.add(p.getType());
                // make sure we add component types of arrays too
                TypeInfo t = p.getType();
                while (t.getArrayComponent() != null) {
                    t = new TypeInfo(t.getArrayComponent());
                    classes.add(t);
                }
            }
        }
        return classes;
    }

    /**
     * Generates a list of the publicly-visible child classes of the class
     * represented by the specified ClassInfo.
     *
     * @param classInfo The ClassInfo for which to find the children.
     * @return The child classes, in the form "fully.qualified.Name.class".
     */
    private SortedSet<String> getChildrenFromClassInfo(final ClassInfo classInfo) {
        final SortedSet<String> result = new TreeSet<>();
        final SortedSet<ClassInfo> children =
                InfoFactory.getConcreteChildren(classInfo);

        // remove non-public children so we don't try to generate them
        children.removeIf(classInfo1 -> classInfo1.getProtectionLevel() != ProtectionLevel.PUBLIC);

        for (ClassInfo c : children) {
            result.add(c.getFullyQualifiedName() + ".class");
        }

        return result;
    }

    /**
     * Checks the specified set of class literal names to see if they are
     * child classes of the specified class, and returns only the ones that are.
     *
     * @param classInfo The class.
     * @param strings   The literals.
     * @return the subset of strings that are child classes of classInfo.
     */
    private SortedSet<String> checkChildLiterals(final ClassInfo classInfo,
                                                 final SortedSet<String> strings) {
        final SortedSet<String> result = new TreeSet<>();
        Class<?> parent = null;
        try {
            parent = Class.forName(classInfo.getFullyQualifiedName());
        } catch (final ClassNotFoundException e) {
            // this should never happen if the class compiled... if it does,
            // we simply cannot proceed so we just ignore the problem
        }
        if (parent != null) {
            for (Object o : strings) {
                try {
                    // class literals end in ".class" so we need to peel that off
                    final String child_fq_name =
                            o.toString().substring(0, o.toString().lastIndexOf('.'));
                    final Class<?> child = Class.forName(child_fq_name);
                    if (parent.isAssignableFrom(child)) {
                        result.add(o.toString());
                    }
                } catch (final ClassNotFoundException e) {
                    // this also should never happen if the class compiled... if
                    // it does, we just ignore it
                }
            }
        }
        return result;
    }
}
