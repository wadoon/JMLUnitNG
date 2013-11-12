/*
 * JMLUnitNG 
 * Copyright (C) 2010-13
 */

package org.jmlspecs.jmlunitng;

import ie.ucd.clops.runtime.automaton.AutomatonException;
import ie.ucd.clops.runtime.options.InvalidOptionPropertyValueException;
import ie.ucd.clops.runtime.options.InvalidOptionValueException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jmlspecs.jmlunitng.clops.JMLUnitNGOptionStore;
import org.jmlspecs.jmlunitng.clops.JMLUnitNGParser;
import org.jmlspecs.jmlunitng.generator.ClassInfo;
import org.jmlspecs.jmlunitng.generator.InfoFactory;
import org.jmlspecs.jmlunitng.generator.MethodInfo;
import org.jmlspecs.jmlunitng.generator.TestClassGenerator;
import org.jmlspecs.jmlunitng.util.JavaSuffixFilter;
import org.jmlspecs.jmlunitng.util.Logger;
import org.jmlspecs.jmlunitng.util.ProtectionLevel;
import org.jmlspecs.jmlunitng.util.StringTemplateUtil;
import org.jmlspecs.openjml.Factory;
import org.jmlspecs.openjml.IAPI;
import org.jmlspecs.openjml.JmlTree.JmlCompilationUnit;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * The main executable.
 * 
 * @author Jonathan Hogins
 * @author Daniel M. Zimmerman
 * @version November 2013
 */
public final class JMLUnitNG implements Runnable {
  /**
   * The extension for Java source files.
   */
  public static final String JAVA_SUFFIX = ".java";

  /**
   * The extension for Java class files.
   */
  public static final String CLASS_SUFFIX = ".class";
  
  /**
   * The string to be prepended to the reported version.
   */
  private static final String VERSION_STRING = "1.4rc1";
  
  /**
   * The raw SVN revision string.
   */
  private static final String RAW_SVN_REV = "$Rev$"; 
  
  /**
   * The default output directory.
   */
  private static final String DEF_OUTPUT_DIR = "";
  
  /**
   * The "generated by JMLUnitNG" string, used to detect 
   * JMLUnitNG-generated files.
   */
  private static final String GEN_STRING;
  
  /**
   * The "do not change this comment" string, used to detect 
   * JMLUnitNG-generated files.
   */
  private static final String DNM_STRING;
  
  /**
   * The suffix used for data packages, used to detect 
   * JMLUnitNG-generated directories.
   */
  private static final String SP_SUFFIX;
  
  /**
   * The suffix used for test classes, used to detect
   * JMLUnitNG-generated files.
   */
  private static final String TC_SUFFIX;  
  
  /**
   * The number of milliseconds in a second.
   */
  private static final int MILLIS_IN_SECOND = 1000;
  
  /**
   * The number of seconds in a minute.
   */
  private static final int SECONDS_IN_MINUTE = 60;
  
  /**
   * The command line options store to be used.
   */
  private final JMLUnitNGConfiguration my_config;
  
  /**
   * The set of files/directories we have cleaned on this run.
   */
  private final Set<String> my_cleaned_files = new HashSet<String>();
  
  /**
   * The set of files/directories we have created on this run.
   */
  private final Set<String> my_created_files = new HashSet<String>();
  
  /**
   * The logger to use when generating output.
   */
  private final Logger my_logger;
  
  /**
   * The time at which this instance was created.
   */
  private final long my_start_time = System.currentTimeMillis();
  
  // static initializer
  
  static {
    final STGroup group = StringTemplateUtil.load("shared_java");
    GEN_STRING = group.getInstanceOf("generatedString").render();
    DNM_STRING = group.getInstanceOf("doNotModifyString").render();
    SP_SUFFIX = group.getInstanceOf("strategyPackageSuffix").render();
    TC_SUFFIX = group.getInstanceOf("testClassSuffix").render();
  }
  
  /**
   * Constructs a JMLUnitNG instance with the specified configuration.
   * 
   * @param the_config The configuration to be used.
   */
  public JMLUnitNG(final JMLUnitNGConfiguration the_config) {
    my_config = the_config;
    my_logger = new Logger(the_config.isVerboseSet() || the_config.isDryRunSet());
  }

  /**
   * @return the version string, generated from SVN properties.
   */
  public static String version() {
    final String svn_rev = RAW_SVN_REV.substring(6, RAW_SVN_REV.length() - 2);
    String jml_version = "OpenJML Version Unknown";
    try {
      jml_version = Factory.makeAPI(null).version();
    } catch (final Exception e) {
      // don't do anything
    }
    return VERSION_STRING + " (" + svn_rev + "/" + 
           jml_version + ")";
  }
  
  /**
   * The main method. Parses the command line arguments and runs
   * the tool.
   * 
   * @param the_args Arguments from the command line.
   */
  public static void main(final String[] the_args) {
    try {
      final JMLUnitNGParser clops = new JMLUnitNGParser();
      clops.parse(the_args);
      (new JMLUnitNG(getConfiguration(clops.getOptionStore()))).run();
    } catch (final InvalidOptionPropertyValueException e) {
      System.err.println("Invalid CLOPS option file.");
      e.printStackTrace();
    } catch (final AutomatonException e) {
      System.err.println("Automaton Exception: " + e.getLocalizedMessage());
      e.printStackTrace();
    } catch (final InvalidOptionValueException e) {
      System.err.println(e.getLocalizedMessage());
    } catch (final JMLUnitNGError e) {
      System.err.println("JMLUnitNG exited because of an irrecoverable error: ");
      e.printStackTrace();
    }
  }
  
  /**
   * Converts a CLOPS option store to a JMLUnitNGConfiguration, potentially showing
   * help information (and exiting) or erroring out along the way.
   * 
   * @param the_options The option store.
   * @return a JMLUnitNGConfiguration generated from the option store.
   */
  private static JMLUnitNGConfiguration getConfiguration
  (final JMLUnitNGOptionStore the_options) {
    final JMLUnitNGConfiguration result = new JMLUnitNGConfiguration();
    
    // fill in the configuration from the options

    if (the_options.isDestinationSet()) {
      result.setDestination(the_options.getDestination());
    }
        
    // files come from two places
    
    final List<File> file_list = new ArrayList<File>();
    if (the_options.isFilesSet()) {
      file_list.addAll(the_options.getFiles());
    }
    if (the_options.isDashFilesSet()) {
      file_list.addAll(the_options.getDashFiles());
    }
    result.setFiles(file_list);
    
    // back to normal options
    
    if (the_options.isClasspathSet()) {
      result.setClasspath(the_options.getClasspath());
    }
    
    if (the_options.isSpecspathSet()) {
      result.setSpecspath(the_options.getSpecspath());
    }
    
    if (the_options.isRACVersionSet()) {
      result.setRACVersion(the_options.getRACVersion());
    }
    
    result.setDeprecation(the_options.isDeprecationSet());
    result.setInherited(the_options.isInheritedSet());
    result.setParallel(the_options.isParallelSet());
    
    // protection level requires special processing
    
    ProtectionLevel level = ProtectionLevel.PUBLIC;
    if (the_options.isProtectedSet()) {
      level = ProtectionLevel.PROTECTED;
    }
    if (the_options.isPackageSet()) {
      level = ProtectionLevel.NO_LEVEL;
    }
    result.setProtectionLevel(level);
    
    // back to normal options
    
    result.setReflection(the_options.isReflectionSet());
    result.setChildren(the_options.isChildrenSet());
    result.setLiterals(the_options.isLiteralsSet());
    result.setSpecLiterals(the_options.isSpecLiteralsSet());
    result.setClean(the_options.isCleanSet());
    result.setPrune(the_options.isPruneSet());
    result.setNoGen(the_options.isNoGenSet());
    result.setDryRun(the_options.isDryRunSet());
    result.setVerbose(the_options.isVerboseSet());
    
    // check for help request
    
    if (the_options.isHelpSet() || 
        (result.getFiles().size() == 0 && !result.isNoGenSet())) {
      printHelp();
      Runtime.getRuntime().exit(0);
    }    
    
    // check for invalid RAC
    
    if (result.isRACVersionSet() && 
        !TestClassGenerator.VALID_RAC_VERSIONS.contains
        (result.getRACVersion())) {
      System.err.println(invalidRACError());
      Runtime.getRuntime().exit(1);
    }
    
    return result;
  }

  /**
   * Print usage to standard out.
   */
  private static void printHelp() {
    final STGroup group = StringTemplateUtil.load("help");
    final ST t = group.getInstanceOf("help_msg");
    t.add("version", version());
    System.out.println(t.render());
  }
  
  /**
   * @return a String describing an invalid RAC setting error.
   */
  private static String invalidRACError() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Invalid RAC version specified. Valid versions are: ");
    for (String s : TestClassGenerator.VALID_RAC_VERSIONS) {
      sb.append(s);
      sb.append(' ');
    }
    return sb.toString();
  }
  
  /**
   * Returns a list of Java files in all subdirectories of the given folder,
   * ignoring files generated by JMLUnitNG.
   * 
   * @param the_directory A File object representing the directory to parse.
   * @return A List of Java files.
   */
  //@ requires the_directory.isDirectory();
  private static List<File> findJavaFiles(final File the_directory) {
    final List<File> result = new LinkedList<File>();
    final File[] all_packed_files = the_directory.listFiles(JavaSuffixFilter.instance());
    for (int k = 0; k < all_packed_files.length; k++) {
      if (all_packed_files[k].isDirectory()) {
        result.addAll(findJavaFiles(all_packed_files[k]));
      } else if (isJavaSourcePath(all_packed_files[k].getPath()) &&
                 !isJMLUnitNGGenerated(all_packed_files[k])) {
        try {
          result.add(all_packed_files[k].getCanonicalFile());
        } catch (final IOException e) {
          // this should never happen
          throw new JMLUnitNGError("I/O exception while finding files.", e);
        }
      }
    }
    return result;
  }
  
  /**
   * @param the_path A pathname.
   * @return true if the specified pathname represents a Java file
   * that was not generated by JMLUnitNG.
   */
  private static boolean isJavaSourcePath(final String the_path) {
    return the_path.endsWith(JAVA_SUFFIX) &&
           !the_path.endsWith(TC_SUFFIX + JAVA_SUFFIX) &&
           !the_path.contains(SP_SUFFIX + File.separator);
  }

  /**
   * @param the_file The file to check.
   * @return true if the file/directory was generated by JMLUnitNG, 
   * false otherwise.
   */
  private static /*@ helper @*/ boolean isJMLUnitNGGenerated(final File the_file) {
    boolean result = false;
    try {
      if (the_file.exists() && !the_file.isDirectory()) {
        // it's a file so it's generated by us if it has our comment
        final BufferedReader br = new BufferedReader(new FileReader(the_file));
        boolean gen_string_found = false;
        boolean dnm_string_found = false;
        String line = br.readLine();
        while (line != null && (!gen_string_found || !dnm_string_found)) {
          gen_string_found |= line.contains(GEN_STRING);
          dnm_string_found |= line.contains(DNM_STRING);
          line = br.readLine();
        } 
        br.close();
        result = gen_string_found && dnm_string_found;
      } else if (the_file.exists()) {
        // it's a directory so it's generated by us if it has the right suffix
        result = the_file.getCanonicalPath().endsWith(SP_SUFFIX);
      }
    } catch (final IOException e) {
      // if we got an exception, we couldn't read the file, so let's
      // assume we didn't generate it
      result = false;
    }
    return result;
  }
  
  /**
   * Generates a human-readable string representing an elapsed time.
   * 
   * @param the_time The elapsed time, in milliseconds.
   * @return The human-readable string.
   */
  private static String getHumanReadableDurationFromMillis(final long the_time) {
    final StringBuilder sb = new StringBuilder();
    final long seconds = the_time / MILLIS_IN_SECOND;
    
    if (the_time / SECONDS_IN_MINUTE > 0) {
      sb.append(seconds / SECONDS_IN_MINUTE);
      sb.append(" min ");
    }
    sb.append(seconds % SECONDS_IN_MINUTE);
    sb.append(" sec");
    
    return sb.toString();
  }
  
  /**
   * The run method. Handles the entire execution of JMLUnitNG, once
   * command line arguments have been parsed; JMLUnitNG can be run
   * programmatically by creating a suitable JMLUnitNGConfiguration and
   * passing it to the constructor.
   */
  public void run() {
    if (my_config.isRACVersionSet() && 
        !TestClassGenerator.VALID_RAC_VERSIONS.contains
        (my_config.getRACVersion())) {
      throw new JMLUnitNGError(invalidRACError());
    }
    if (my_config.isDryRunSet()) {
      my_logger.println(">>> DRY RUN <<<");
    }
    if (my_config.isCleanSet()) {
      cleanAllFiles();
    }
    try {
      processAllCompilationUnits();
    } catch (final IOException e) {
      throw new JMLUnitNGError(e);
    }
    if (my_config.isPruneSet()) {
      pruneAllFiles();
    }
   
    my_logger.print("Elapsed time ");
    final long elapsed_time = System.currentTimeMillis() - my_start_time;
    my_logger.println(getHumanReadableDurationFromMillis(elapsed_time));
    my_logger.println();
  }
  
  /**
   * @return A list of files to be processed.
   */
  private List<File> filesToProcess() {
    final Set<File> file_set = new HashSet<File>();
    
    for (File f : my_config.getFiles()) {
      if (f.isDirectory()) {
        file_set.addAll(findJavaFiles(f));
      } else if (f.getPath().endsWith(JAVA_SUFFIX)) {
        try {
          file_set.add(f.getCanonicalFile());
        } catch (final IOException e) {
          // this should never happen
          throw new JMLUnitNGError("I/O exception while finding files.", e);
        }
      } // don't add non-java files to the list
    }
    
    return new ArrayList<File>(file_set);
  }
  
  /**
   * Extracts the classpath from the command line options.
   *
   * @return The final classpath.
   */
  private String generateClasspath() {
    String classpath;
    if (my_config.getClasspath().size() > 0) {
      final List<File> path_list = my_config.getClasspath();
      final StringBuffer sb = new StringBuffer();
      for (File f : path_list) {
        sb.append(f.getAbsolutePath());
        sb.append(File.pathSeparator);
      }
      classpath = sb.toString();
    } else {
      classpath = System.getenv("CLASSPATH");
      if (classpath == null) {
        classpath = "";
      }
    }
    return classpath;
  }
  
  /**
   * Extracts the specspath from the command line options.
   *
   * @return The final specspath.
   */
  private String generateSpecspath() {
    String specspath;
    if (my_config.getSpecspath().size() > 0) {
      final List<File> path_list = my_config.getSpecspath();
      final StringBuffer sb = new StringBuffer();
      for (File f : path_list) {
        sb.append(f.getAbsolutePath());
        sb.append(File.pathSeparator);
      }
      specspath = sb.toString();
    } else {
      specspath = System.getenv("SPECSPATH");
      if (specspath == null) {
        specspath = "";
      }
    }
    return specspath;
  }
  
  /**
   * Processes all necessary compilation units.
   * 
   * @exception IOException if there is a problem processing compilation units.
   */
  private void processAllCompilationUnits() throws IOException {
    if (!my_config.isNoGenSet()) {
      my_logger.println("Starting test generation");
      my_logger.println();
    }
    
    // the only reason to go to this effort if --no-gen is set is if
    // --prune is also set, so we need to find out what files to prune
    
    if (!my_config.isNoGenSet() || my_config.isPruneSet()) {
      final List<File> file_list = filesToProcess();
      final String classpath = generateClasspath();
      final String specspath = generateSpecspath();
      final String[] openjml_args =
          new String[] {"-noPurityCheck", "-noInternalSpecs",
                        "-cp", classpath, "-specspath", specspath};
      final StringWriter openjml_results = new StringWriter();
      try {
        final IAPI api = 
          Factory.makeAPI(new PrintWriter(openjml_results), null, null, openjml_args);
        final List<JmlCompilationUnit> units = 
          api.parseFiles(file_list.toArray(new File[file_list.size()]));
        final int numOfErrors = api.typecheck(units);
        if (numOfErrors > 0) {
          throw new JMLUnitNGError
          ("Encountered " + numOfErrors + " compilation errors: \n" + 
            openjml_results.toString());
        }
      
        // get class info for all classes before generating tests for any,
        // to enable reflective generation of child classes
      
        InfoFactory.generateInfos(units, api);
        for (JmlCompilationUnit u : units) {
          processCompilationUnit(u, InfoFactory.getClassInfo(u));
          if (!my_config.isNoGenSet()) {  
            my_logger.println();
          }
        }
      } catch (final Exception e) {
        throw new JMLUnitNGError("Could not construct OpenJML API", e);
      }
    }
  }
  
  /**
   * Performs all source processing of the given compilation unit.
   *
   * @param the_unit The compilation unit to process.
   * @param the_info The class information generated from the 
   *  compilation unit.
   * @throws IOException Thrown if source output fails.
   */
  private void processCompilationUnit(final JmlCompilationUnit the_unit, 
                                      final ClassInfo the_info) 
    throws IOException {
    if (!my_config.isNoGenSet()) {
      my_logger.print("Processing ");

      if (the_info.isInterface()) {
        my_logger.println("interface " + the_info.getFullyQualifiedName());
      } else if (the_info.isAbstract()) {
        my_logger.println("abstract class " + the_info.getFullyQualifiedName());
      } else if (the_info.isEnumeration()) {
        my_logger.println("enumeration " + the_info.getFullyQualifiedName());
      } else { // normal class
        my_logger.println("class " + the_info.getFullyQualifiedName());
      }
    }
    
    boolean generate = true;
    if (the_info.isAbstract() && the_info.getNestedClasses().isEmpty() &&
        the_info.getTestableMethods().isEmpty()) {
      if (!my_config.isNoGenSet()) {
        my_logger.println("Not generating tests for abstract class with " + 
                          "no concrete static methods");
      }
      generate = false;
    }
    if (the_info.isEnumeration()) {
      if (!my_config.isNoGenSet()) {
        my_logger.println("Not generating tests for enumeration");
      }
      generate = false;
    }
    if (the_info.getProtectionLevel().strongerThan(my_config.getProtectionLevel())) {
      if (!my_config.isNoGenSet()) {
        my_logger.println("Not generating tests for " + the_info.getProtectionLevel() +
                          " " + the_info + ", configured for " + 
                          my_config.getProtectionLevel());
      }
      generate = false;
    }
    if (generate) {
      boolean usable_constructor = false;
      for (MethodInfo m : the_info.getConstructors()) {
        usable_constructor |= 
            m.isConstructor() && 
            m.getProtectionLevel().weakerThanOrEqualTo(my_config.getProtectionLevel());
      }
      if (!usable_constructor && !my_config.isNoGenSet()) {
        my_logger.println("Not generating tests for " + the_info + " with no " +
                          my_config.getProtectionLevel() + 
                          " (or weaker) constructors");
      }
      generate &= usable_constructor;
    }
    if (generate) {
      generateTests(the_unit, the_info);
    }
  }
  
  /**
   * Generates tests for the specified compilation unit.
   * 
   * @param the_unit The compilation unit.
   * @param the_info The class information for the compilation unit.
   * @exception IOException if there is a problem generating tests.
   */
  private void generateTests(final JmlCompilationUnit the_unit, 
                             final ClassInfo the_info) 
    throws IOException {
    final TestClassGenerator generator = 
        new TestClassGenerator(my_config, my_logger);
    final String[] dirs = getDirectories(the_unit, the_info);
    String strategy_dir = dirs[0];
    for (String s : dirs) {
      final File f = new File(s);
      if (!my_config.isNoGenSet() && !the_info.getTestableMethods().isEmpty()) { 
        // don't create dirs for classes with no testable methods
        my_logger.println("Creating directory " + f);
        if (!my_config.isDryRunSet() && !f.mkdirs() && !f.isDirectory()) {
          throw new JMLUnitNGError("Could not create directory " + f + 
                                   " for generated tests.");
        }
      }
      my_created_files.add(f.getCanonicalPath());
      strategy_dir = s;
    }
      
    generator.generateClasses(the_info, dirs[0], strategy_dir);
    my_created_files.addAll(generator.getCreatedFiles());
      
    // if either of our directories ended up empty, delete it
    for (String s : dirs) {
      final File f = new File(s);
      if (!my_config.isNoGenSet() && f.isDirectory() && f.listFiles().length == 0) {
        my_logger.println("Removing empty directory " + f);
        if (!my_config.isDryRunSet() && !f.delete()) {
          my_logger.println("Could not remove empty directory " + f);
          // no Error because what's the harm in leaving a directory around?
        }
      }
    }
  }
  
  /**
   * @param the_unit The unit being processed.
   * @param the_info The class information for the unit being processed.
   * @return An array of directory names to create/use for the specified unit.
   */
  private String[] getDirectories(final JmlCompilationUnit the_unit,
                                  final ClassInfo the_info) {
    String[] result;
    final STGroup group = StringTemplateUtil.load("shared_java");
    final ST sp_template = group.getInstanceOf("strategyPackageShortName");
    sp_template.add("classInfo", the_info);

    final String output_dir = generateDestinationDirectory(the_unit);
    if (the_info.isPackaged()) {
      final String strategy_dir =  
        output_dir + sp_template.render() + File.separator;
      result = new String[] { output_dir, strategy_dir };
    } else {
      result = new String[] { output_dir };
    }
    
    return result;
  }
  
  /**
   * Prunes all necessary files, based on the set of files we've created.
   */
  private void pruneAllFiles() {
    my_logger.println("Pruning obsolete JMLUnitNG-generated files");
    my_cleaned_files.clear();
    final Set<File> files_to_prune = new HashSet<File>();
    if (my_config.isDestinationSet()) {
      files_to_prune.add(new File(my_config.getDestination()));
    } else {
      files_to_prune.addAll(my_config.getFiles());
    }
    for (File f : files_to_prune) {
      try {
        cleanOrPruneFile(f, true);
      } catch (final IOException e) {
        throw new JMLUnitNGError("Error occurred while pruning files.", e);
      }
    }
    my_logger.println("Pruning complete");
    my_logger.println();
  }
  
  /**
   * Cleans all necessary files.
   */
  private void cleanAllFiles() {
    my_logger.println("Cleaning old JMLUnitNG-generated files");
    final Set<File> files_to_clean = new HashSet<File>();
    if (my_config.isDestinationSet()) {
      files_to_clean.add(new File(my_config.getDestination()));
    } else {
      files_to_clean.addAll(my_config.getFiles());
    }
    for (File f : files_to_clean) {
      try {
        cleanOrPruneFile(f, false);
      } catch (final IOException e) {
        throw new JMLUnitNGError("Error occurred while cleaning files.", e);
      }
    }
    my_logger.println("Cleaning complete");
    my_logger.println();
  }
  
  /**
   * Clean or prune the specified file/directory (recursively), by removing any 
   * JMLUnitNG-generated files and directories.
   * 
   * @param the_file The file/directory to clean.
   * @param the_prune true if we're pruning (that is, we only delete those
   * files and directories that we did not create this run) and false otherwise.
   * @exception IOException if there is a problem with the cleaning/pruning.
   */
  private void cleanOrPruneFile
  (final File the_file, final boolean the_prune) throws IOException {
    if (my_cleaned_files.add(the_file.getCanonicalPath()) && the_file.exists()) {
      final boolean one_of_ours = 
        isJMLUnitNGGenerated(the_file) && 
        (!the_prune || !my_created_files.contains(the_file.getCanonicalPath()));
      if (the_file.isDirectory()) {
        for (File f : the_file.listFiles(JavaSuffixFilter.instance())) {
          cleanOrPruneFile(f, the_prune);
        }
      } 
      if (one_of_ours) {
        my_logger.println("Deleting " + the_file);
        if (!my_config.isDryRunSet() && !the_file.delete()) {
          my_logger.println("Unable to delete " + the_file + ", check permissions.");
        }
        // if there's a corresponding .class file, delete that too
        if (the_file.getAbsolutePath().contains(JAVA_SUFFIX)) {
          final File class_file = 
            new File(the_file.getAbsolutePath().replace(JAVA_SUFFIX, CLASS_SUFFIX));
          if (class_file.exists()) {
            my_logger.println("Deleting " + class_file);
            if (!my_config.isDryRunSet() && !class_file.delete()) {
              my_logger.println("Unable to delete " + the_file + ", check permissions.");
            }
          }
        }
      }
    }
  }
  
  /**
   * Generates the destination filename of the given JmlCompilationUnit for the given options.
   *   
   * @param the_unit The JmlCompilationUnit for which to generate a filename.
   * @return the generated filename.
   */
  private String generateDestinationDirectory(final JmlCompilationUnit the_unit) {
    String output_dir = DEF_OUTPUT_DIR;
    if (my_config.isDestinationSet()) {
      final StringBuilder sb = new StringBuilder(my_config.getDestination());
      if (!(output_dir.endsWith("\\") || output_dir.endsWith("/"))) {
        sb.append(File.separator);
      }
      if (the_unit.getPackageName() != null) {
        sb.append(the_unit.getPackageName().toString().replace('.', File.separatorChar));
      }
      if (!(output_dir.endsWith("\\") || output_dir.endsWith("/"))) {
        sb.append(File.separator);
      }
      
      output_dir = sb.toString().replace('\\', File.separatorChar);
      output_dir = output_dir.replace('/', File.separatorChar);
    } else {
      output_dir = 
        new File(the_unit.getSourceFile().toUri().getPath()).getParent() +
        File.separator;
    }
    return output_dir;
  }
}
