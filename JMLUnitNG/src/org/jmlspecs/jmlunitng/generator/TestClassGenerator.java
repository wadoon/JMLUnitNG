/*
 * OpenJMLUnit
 * 
 * @author "Daniel M. Zimmerman (dmz@acm.org)"
 * 
 * @author Jonathan Hogins
 * 
 * @module "OpenJML"
 * 
 * @creation_date "April 2010"
 * 
 * @last_updated_date "April 2010"
 * 
 * @keywords "unit testing", "JML"
 */

package org.jmlspecs.jmlunitng.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.jmlspecs.jmlunitng.JMLUnitNG;
import org.jmlspecs.jmlunitng.util.StringTemplateUtil;

/**
 * Generator for classes that contain unit tests.
 * 
 * @author Jonathan Hogins
 * @author Daniel M. Zimmerman
 * @version September 2010
 */
public class TestClassGenerator {
  /**
   * Valid RAC versions.
   */
  public static final List<String> VALID_RAC_VERSIONS;
  
  static {
    final List<String> temp = new ArrayList<String>();
    temp.add("jml4");
    VALID_RAC_VERSIONS = Collections.unmodifiableList(temp);
  }
    
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
   * The default RAC version to generate tests for.
   */
  public static final String DEF_RAC_VERSION = "jml4";
  
  /**
   * The line max line width of generated code.
   */
  public static final int LINE_WIDTH = 120;
  
  /**
   * The maximum name level for which to generate unique names.
   */
  private static final int MAX_NAME_LEVEL = 3;
  
  /**
   * The max protection level for which to generate tests.
   */
  private final ProtectionLevel my_level;
  
  /**
   * Do I test inherited methods?
   */
  private final boolean my_test_inherited_methods;
  
  /**
   * Do I test deprecated methods?
   */
  private final boolean my_test_deprecated_methods;
  
  /**
   * Do I use reflection?
   */
  private final boolean my_use_reflection;

  /**
   * The RAC version to generate test classes for.
   */
  private final String my_rac_version;
  
  /**
   * Create a new TestClassGenerator with the default options.
   */
  public TestClassGenerator() {
    this(DEF_PROTECTION_LEVEL, DEF_TEST_INHERITED_METHODS, 
         DEF_TEST_DEPRECATED_METHODS, DEF_USE_REFLECTION,
         DEF_RAC_VERSION);
  }

  /**
   * Create a new TestClassGenerator with the given options.
   * 
   * @param the_protection_level The maximum protection level for which to
   *  generate method tests.
   * @param the_test_inherited_methods Do I test inherited methods?
   * @param the_test_deprecated_methods Do I test deprecated methods?
   * @param the_use_reflection Do I use reflection to generate test data?
   * @param the_rac_version The RAC version to generate test classes for.
   */
  public TestClassGenerator(final ProtectionLevel the_protection_level,
                            final boolean the_test_inherited_methods,
                            final boolean the_test_deprecated_methods,
                            final boolean the_use_reflection,
                            final String the_rac_version) {
    my_level = the_protection_level;
    my_test_inherited_methods = the_test_inherited_methods;
    my_test_deprecated_methods = the_test_deprecated_methods;
    my_use_reflection = the_use_reflection;
    my_rac_version = the_rac_version;
  }

  /**
   * Generates a strategy class for the specified method parameter.
   * 
   * @param the_class The class to generate a strategy class for.
   * @param the_method The method to generate a strategy class for.
   * @param the_param The parameter to generate a strategy class for.
   * @param the_writer The writer to write the strategy class to.
   * @throws IOException if an IOException occurs while writing the class.
   */
  //@ requires the_class.getMethods().contains(the_method);
  //@ requires the_method.getParameters().contains(the_param);
  public void generateMethodParamStrategyClass
  (final /*@ non_null @*/ ClassInfo the_class,
   final /*@ non_null @*/ MethodInfo the_method,
   final /*@ non_null @*/ ParameterInfo the_param, 
   final /*@ non_null @*/ Writer the_writer)
  throws IOException {
    final StringTemplateGroup group = StringTemplateGroup.loadGroup("strategy_method_param");
    final StringTemplate t = group.getInstanceOf("main");
    t.setAttribute("class", the_class);
    t.setAttribute("date", getFormattedDate());
    t.setAttribute("method", the_method);
    t.setAttribute("param", the_param);
    t.setAttribute("jmlunitng_version", JMLUnitNG.version());
    the_writer.write(t.toString(LINE_WIDTH));
  }
  
  /**
   * Generates a global strategy class for the specified type.
   * 
   * @param the_class The class to generate a strategy class for.
   * @param the_type The type to generate a strategy class for.
   * @param the_writer The writer to write the strategy class to.
   * @throws IOException if an IOException occurs while writing the class.
   */
  //@ requires the_class.getMethods().contains(the_method);
  //@ requires the_method.getParameters().contains(the_param);
  public void generateGlobalStrategyClass
  (final /*@ non_null @*/ ClassInfo the_class,
   final /*@ non_null @*/ TypeInfo the_type,
   final /*@ non_null @*/ Writer the_writer)
  throws IOException {
    final StringTemplateGroup group = StringTemplateGroup.loadGroup("strategy_global");
    final StringTemplate t = group.getInstanceOf("main");
    t.setAttribute("class", the_class);
    t.setAttribute("date", getFormattedDate());
    t.setAttribute("type", the_type);
    t.setAttribute("jmlunitng_version", JMLUnitNG.version());
    t.setAttribute("use_reflection", my_use_reflection);
    the_writer.write(t.toString(LINE_WIDTH));
  }
  
  /**
   * Generates the instance strategy class for the specified class.
   * 
   * @param the_class The class to generate a strategy class for.
   * @param the_writer The writer to write the strategy class to.
   * @throws IOException if an IOException occurs while writing the class.
   */
  //@ requires the_class.getMethods().contains(the_method);
  //@ requires the_method.getParameters().contains(the_param);
  public void generateInstanceStrategyClass
  (final /*@ non_null @*/ ClassInfo the_class,
   final /*@ non_null @*/ Writer the_writer)
  throws IOException {
    final StringTemplateGroup group = StringTemplateGroup.loadGroup("strategy_instance");
    final StringTemplate t = group.getInstanceOf("main");
    t.setAttribute("class", the_class);
    t.setAttribute("date", getFormattedDate());
    t.setAttribute("jmlunitng_version", JMLUnitNG.version());
    t.setAttribute("use_reflection", my_use_reflection);
    the_writer.write(t.toString(LINE_WIDTH));
  }
  /**
   * @return a formatted version of the current date and time.
   */
  private String getFormattedDate() {
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
    return df.format(new Date());
  }
  
  /**
   * Generates a test class for the_class and writes it to the_writer. 
   * 
   * @param the_class The class to generate a test class for.
   * @param the_methods The methods to generate tests for. 
   * @param the_writer The writer to write the test class to.
   * @throws IOException if an IOException occurs while writing the class.
   */
  /*@ requires (\forall MethodInfo m; the_methods.contains(m); 
    @           the_class.getMethods().contains(m));
    @*/
  public void generateTestClass(final /*@ non_null @*/ ClassInfo the_class,
                                final /*@ non_null @*/ Set<MethodInfo> the_methods,
                                final /*@ non_null @*/ Writer the_writer)
  throws IOException {
    final StringTemplateGroup group = StringTemplateGroup.loadGroup("test_class_" + my_rac_version);
    final StringTemplate t = group.getInstanceOf("main");
    t.setAttribute("class", the_class);
    t.setAttribute("date",
                   DateFormat.getDateInstance().format(Calendar.getInstance().getTime()));
    t.setAttribute("methods", the_methods);
    t.setAttribute("package_name", the_class.getPackageName());
    t.setAttribute("packaged", !the_class.getPackageName().equals(""));
    t.setAttribute("jmlunitng_version", JMLUnitNG.version());
    the_writer.write(t.toString(LINE_WIDTH));
  }

  /**
   * Returns the methods from the given class to test based on generator
   * settings.
   * 
   * @param the_class The class for which to find testable methods.
   * @return A list of methods in the_class to test.
   */
  /*@ ensures (\forall MethodInfo m; \result.contains(m); 
    @   m.isTestable() && 
    @   ((m.isInherited() && my_test_inherited_methods) || !m.isInherited()) &&
    @   ((m.isDeprecated() && my_test_deprecated_methods) || !m.isDeprecated()));
   */
  private /*@ pure non_null @*/ Set<MethodInfo> getMethodsToTest
  (final /*@ non_null @*/ ClassInfo the_class) {
    final Set<MethodInfo> methods = new HashSet<MethodInfo>();
    for (MethodInfo m : the_class.getTestableMethods()) {
      if (m.getProtectionLevel().weakerThanOrEqualTo(my_level) &&
          (my_test_inherited_methods || !m.isInherited()) &&
          (my_test_deprecated_methods || !m.isDeprecated()))
      {
        methods.add(m);
      }
    }
    return methods;
  }

  /**
   * Returns the basic types present in the parameters of the given methods.
   * 
   * @param the_methods The methods for which to find parameter basic types.
   * @return A list of basic types.
   */
  private /*@ pure non_null @*/ Set<TypeInfo> getUniqueParameterTypes
  (final /*@ non_null @*/ Set<MethodInfo> the_methods) {
    final Set<TypeInfo> classes = new HashSet<TypeInfo>();
    for (MethodInfo m : the_methods) {
      for (ParameterInfo p : m.getParameters()) {
        classes.add(p.getType());
      }
    }
    return new HashSet<TypeInfo>(classes);
  }

  /**
   * Generates both test and test data classes and writes them to the given
   * directory. If either writer is null, that class is not generated.
   * 
   * @param the_class The class for which to generate test classes.
   * @param the_dir The directory in which to generate test classes.
   * @throws IOException Thrown if an IOException occurs while generating the classes.
   */
  //@ requires VALID_RAC_VERSIONS.contains(the_rac);
  //@ requires (new File(the_dir)).isDirectory();
  public void generateClasses(final /*@ non_null @*/ ClassInfo the_class, 
                              final /*@ non_null @*/ String the_dir) throws IOException {
    StringTemplateUtil.initialize();
    final StringTemplateGroup group = StringTemplateGroup.loadGroup("shared_java");
    final StringTemplate tcNameTemplate = group.lookupTemplate("testClassName");
    final StringTemplate msNameTemplate = group.lookupTemplate("strategyName");
    final StringTemplate isNameTemplate = group.lookupTemplate("instanceStrategyName");
    final StringTemplate gsNameTemplate = group.lookupTemplate("globalStrategyName");
    final StringTemplate pkgNameTemplate = group.lookupTemplate("strategyPackageShortName");
    final Set<MethodInfo> methods_to_test = getMethodsToTest(the_class);
    
    // initialize name templates
    tcNameTemplate.setAttribute("classInfo", the_class);
    pkgNameTemplate.setAttribute("classInfo", the_class);
    
    // generate the (single) test class
    final FileWriter tcWriter = 
      new FileWriter(new File(the_dir + tcNameTemplate.toString() + JMLUnitNG.JAVA_SUFFIX));
    generateTestClass(the_class, methods_to_test, tcWriter);
    tcWriter.close();
    
    // generate the strategy classes - there are three stages here
    
    // first: individual method parameter strategy classes
    for (MethodInfo m : methods_to_test) {
      for (ParameterInfo p : m.getParameters()) {
        msNameTemplate.reset();
        msNameTemplate.setAttribute("methodInfo", m);
        msNameTemplate.setAttribute("paramInfo", p);
        final FileWriter msWriter = 
          new FileWriter(new File(the_dir + pkgNameTemplate.toString() + File.separator +
                                  msNameTemplate.toString() + JMLUnitNG.JAVA_SUFFIX));
        generateMethodParamStrategyClass(the_class, m, p, msWriter);
        msWriter.close(); 
      }
    }
    
    // second: global strategy classes for all data types
    
    for (TypeInfo t : getUniqueParameterTypes(methods_to_test)) {
      gsNameTemplate.reset();
      gsNameTemplate.setAttribute("typeInfo", t);
      final FileWriter gsWriter = 
        new FileWriter(new File(the_dir + pkgNameTemplate.toString() + File.separator + 
                                gsNameTemplate.toString() + JMLUnitNG.JAVA_SUFFIX));
      generateGlobalStrategyClass(the_class, t, gsWriter);
      gsWriter.close();
    }
    
    // third: instance strategy class for this class
    
    final FileWriter isWriter = 
      new FileWriter(new File(the_dir + pkgNameTemplate.toString() + File.separator + 
                              isNameTemplate.toString() + JMLUnitNG.JAVA_SUFFIX));
    generateInstanceStrategyClass(the_class, isWriter);
    isWriter.close();
  }
}
