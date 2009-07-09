
package org.jmlspecs.jmlunitng;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Iterator;

import org.multijava.mjc.JCompilationUnit;
import org.multijava.mjc.JCompilationUnitType;
import org.multijava.mjc.JConstructorDeclaration;
import org.multijava.mjc.JFormalParameter;
import org.multijava.mjc.JMethodDeclaration;
import org.multijava.mjc.JPackageImportType;
import org.multijava.mjc.JTypeDeclarationType;

/**
 * Generates the JMLUNITNG_Test_Data class by JMLUNITNG framework. The generated
 * class provides data to run unit tests for the class to be tested using
 * JMLUNITNG testing framework.
 * 
 * @author Rinkesh Nagmoti
 * @version 1.0
 */
public class TestDataClassGenerator implements Constants
{

  /**
   * Represents the class name for the Test class to be generated.
   */
  protected String className;

  /** Writer class object to print the Test Class. */
  protected Writer writer;

  /** String representing the file name and location for Test Class. */
  protected final String my_file;

  /**
   * JTypeDeclarationType object which holds information about the class for
   * which the test is to be conducted.
   */
  protected JTypeDeclarationType declarationType;

  /**
   * This array represents the list of imported packages.
   */
  protected JPackageImportType[] pkgs;

  /**
   * Constructs JMLUNITNGTestDataClassGenerator Object.
   * 
   * @param the_fileName
   * @throws FileNotFoundException
   */
  public TestDataClassGenerator(final String the_fileName) throws FileNotFoundException
  {
    this.my_file = the_fileName;
    writer = new Writer(this.my_file);
  }

  /**
   * Generate the Test Data methods.
   */
  public void createTestDataClass(final JTypeDeclarationType the_decl,
                                  final JCompilationUnit the_cUnitType, final Iterator the_Iter)
  {
    printHeaderImportandJavadoc(the_decl, the_cUnitType);
    printDataMembers();
    printConstructor();
    printDataProvider(the_Iter);
    printClassEnd();
  }

  /**
   * This method prints the header import and javadoc for generated class.
   * 
   * @param the_decl
   * @param the_cUnitType
   */

  private void printHeaderImportandJavadoc(final JTypeDeclarationType the_decl,
                                           final JCompilationUnitType the_cUnitType)
  {

    this.declarationType = the_decl;
    this.className = the_decl.ident() + TEST_DATA_NAME_POSTFIX;
    pkgs = the_cUnitType.importedPackages();

    writer.print("This class is generated by JMLUNITNG on " + new Date());
    writer.newLine(2);
    for (int j = 0; j < pkgs.length; j++)
    {
      writer.print("import " + pkgs[j].getName().replace('/', '.') + ".*;");
    }

    writer.print("/** This class is the data provider class generated by JMLUNITNG");
    writer.print(" testing framework");
    writer.print(" *  for class " + className);
    writer.print(" */");
    writer.print("public class " + className + " {");

  }

  /** Prints the constructor of the Test Data class to be generated. */
  private void printConstructor()
  {
    writer.print("/** Constructs the class object.*/");
    writer.print("public " + className + " () {");
    writer.print("}");
  }

  /** Prints the data provider methods. */
  private void printDataProvider(Iterator the_method_Iterator)
  {
    while (the_method_Iterator.hasNext())
    {
      Object obj = the_method_Iterator.next();
      JFormalParameter[] parameters;
      String name;
      if (obj instanceof JConstructorDeclaration)
      {
        JConstructorDeclaration construct = (JConstructorDeclaration) obj;
        parameters = construct.parameters();
        name = construct.ident() + "_" + getCombinedName(parameters);

        for (int i = 0; i < parameters.length; i++)
        {
          printDataTypeMethod(parameters[i], name);
        }
        printCombinedIteratorMethod(parameters, name);
      }
      else if (obj instanceof JMethodDeclaration)
      {
        JMethodDeclaration method = (JMethodDeclaration) obj;
        parameters = method.parameters();
        name = method.ident() + "_" + getCombinedName(parameters);

        for (int i = 0; i < parameters.length; i++)
        {
          printDataTypeMethod(parameters[i], name);
        }
        printCombinedIteratorMethod(parameters, name);
      }

    }
  }

  /**
   * This method print the individual method for each data type in the method
   * which returns the Iterator of the data type.
   * 
   * @param the_parameter
   * @param the_name
   */
  private void printDataTypeMethod(final JFormalParameter the_parameter, final String the_name)
  {
    writer.print("/** This method returns the Iterator for individual data type.*/");
    writer.print("public org.jmlspecs.jmlunit.strategies." + the_parameter.typeToString() +
                 "Iterator " + the_parameter.typeToString() + "_" + the_name + "_" +
                 the_parameter.ident() + "()");
    writer.print("{");
    writer.print(" private org.jmlspecs.jmlunit.strategies." + the_parameter.typeToString() +
                 "StrategyType " + the_parameter.ident() + "_" + the_parameter.typeToString() +
                 "_Strategy =" + " new org.jmlspecs.jmlunit.strategies." +
                 the_parameter.typeToString() + "Strategy()");
    writer.print("{");
    writer.print("protected " + the_parameter.typeToString() + "[] " + "addData()");
    writer.print("{");
    writer.print("return new " + the_parameter.typeToString() + "[] " +
                 "{//You can data elements here.};");
    writer.print("}");
    writer.print("};");
    writer.print("return  " + the_parameter.ident() + "_" + the_parameter.typeToString() +
                 "_Strategy." + the_parameter.typeToString() + "Iterator();");
    writer.print("}");
  }

  /**
   * This method prints the combined iterator for the all data types.
   * @param the_parameters
   * @param the_name
   */
  private void printCombinedIteratorMethod
  (final JFormalParameter[] the_parameters, final String the_name)
  {

    writer.print("/** This method returns the combined Iterator of all data types.*/");
    writer.print("public Iterator<Object[]> params_" + the_name + "()");
    writer.print("{");
    writer.print("allParamIterator = new Iterator[" + the_parameters.length + "];");
    for (int i = 0; i < the_parameters.length; i++)
    {
      writer.print(" allParamIterator[" + i + "] = " + the_parameters[i].typeToString() + "_" +
                   the_name + "_" + the_parameters[i].ident());
    }
    writer.print("CombinedIterator combIter = new CombinedIterator(allParamIterator)");
    writer.print("return combIter");
    writer.print("}");
  }

  /**
   * This method generates the name for all parameters together.
   * @param parameters
   */
  private String getCombinedName(JFormalParameter[] parameters)
  {
    StringBuilder name = new StringBuilder();
    for (int i = 0; i < parameters.length; i++)
    {
      name.append("_" + parameters[i].typeToString());
    }
    return name.toString();
  }

  /**
   * This method prints the data members of the class.
   */
  private void printDataMembers()
  {
    writer.print("/**");
    writer.print("* This is the Iterator array of Iterators for all parameters.");
    writer.print("*/");
    writer.print("protected Iterator[] allParamIterator;");
  }

  /**
   * This method prints the end of class bracket "{".
   */
  private void printClassEnd()
  {
    writer.print("}");
  }
}
