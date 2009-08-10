
package org.jmlspecs.jmlunitng;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
  protected final transient String my_class_name;
  /**
   * Represents the name of the class for which the test will be generated.
   */
  protected final transient String my_class_nm;

  /** Writer class object to print the Test Class. */
  protected final transient Writer my_writer;

  /** String representing the file name and location for Test Class. */
  protected final transient String my_file;

  /**
   * JTypeDeclarationType object which holds information about the class for
   * which the test is to be conducted.
   */
  protected final transient JTypeDeclarationType my_decl_type;

  /**
   * This array represents the list of imported packages.
   */
  protected final transient JPackageImportType[] my_pkgs;
 
  /**
   * This is the name of iterator.
   */
  private final transient String my_itname = "my_iter";
 
  
  /**
   * Constructs JMLUNITNGTestDataClassGenerator Object.
   * 
   * @param the_file_name String file name.
   * @param the_decl JTypeDeclarationType object.
   * @param the_cunit_type JCompilationUnitType object.
   * @throws FileNotFoundException Exception if unable to find specified file.
   */
  public TestDataClassGenerator(final String the_file_name,
         final JTypeDeclarationType the_decl, final JCompilationUnitType the_cunit_type)
    throws FileNotFoundException
  {
    my_class_nm = the_decl.ident();
    this.my_decl_type = the_decl;
    this.my_class_name = the_decl.ident() + TEST_DATA_POSTFIX;
    my_pkgs = the_cunit_type.importedPackages();
    this.my_file = the_file_name;
    my_writer = new Writer(this.my_file);
  }

  /**
   * Generate the Test Data methods.
   * @param the_decl JTypeDeclarationType object.
   * @param the_cunit_type JCompilationUnit object.
   * @param the_iter Iterator object.
   */
  public void createTestDataClass(final JTypeDeclarationType the_decl,
                        final JCompilationUnit the_cunit_type, final Iterator the_iter)
  {
    
    
    printHeaderImportandJavadoc(the_decl);
    printConstructor();
    printCombinedIteratorClass(the_iter, the_decl);
    printClassEnd();
  }

  /**
   * This method prints the header import and javadoc for generated class.
   * 
   * @param the_decl JTypeDeclarationType object.
   */

  private void printHeaderImportandJavadoc(final JTypeDeclarationType the_decl)
  {
    
    my_writer.print("//This class is generated by JMLUNITNG on " + new Date());
    my_writer.newLine(LEVEL1);
    for (int j = 0; j < my_pkgs.length; j++)
    {
      my_writer.print("import " + my_pkgs[j].getName().replace('/', '.') + ".*" + SM_COLN);
    }
    my_writer.print("import org.multijava.*" + SM_COLN);
    my_writer.print("import org.jmlspecs.jmlunit.strategies.*" + SM_COLN);
    my_writer.print("import org.testng.*;");
    my_writer.print("import java.util.Iterator;");
    my_writer.print("import org.testng.annotations.*;");
    my_writer.print("import java.util.*;");
    my_writer.newLine(LEVEL1);
    my_writer.print(JDOC_ST);
    my_writer.print(" * This class is the data provider class generated by JMLUNITNG");
    my_writer.print(" * testing framework");
    my_writer.print(" * for class " + the_decl.ident() + PERIOD);
    my_writer.print(" * @author JMLUNITNG");
    my_writer.print(" * @version 1.0");
    my_writer.print(JDOC_END);
    my_writer.print("public class " + my_class_name);
    my_writer.print(BLK_ST);

  }

  /** Prints the constructor of the Test Data class to be generated. */
  private void printConstructor()
  {
    my_writer.indent(LEVEL1);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL1);
    my_writer.print(" * Constructs the class object.");
    my_writer.indent(LEVEL1);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL1);
    my_writer.print("public " + my_class_name + BKTS);
    my_writer.indent(LEVEL1);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL1);
    my_writer.print(BLK_END);
    my_writer.newLine(ONE);
  }

  /** Prints the data provider methods.
   * @param the_method Object of type method.
   * @param the_decl  JTypeDeclarationType object.
   */
  private void printClassDataProvider(final Object the_method,
                                      final JTypeDeclarationType the_decl)
  {
    
    final Object obj = the_method;
    JFormalParameter[] parameters;
    String name;
    if (obj instanceof JConstructorDeclaration)
    {
      final JConstructorDeclaration construct = (JConstructorDeclaration) obj;
      parameters = construct.parameters();
      name = construct.ident() + getCombinedName(parameters);

      for (int i = 0; i < parameters.length; i++)
      {
        printDataTypeMethod(parameters[i], name);
      }
     // printCombinedIteratorMethod(parameters, name);
      printObjectIterator(the_decl);
      //printObjectCombinedIterator(name);
    }
    else if (obj instanceof JMethodDeclaration)
    {
      final JMethodDeclaration method = (JMethodDeclaration) obj;
      parameters = method.parameters();
      name = method.ident() + getCombinedName(parameters);

      for (int i = 0; i < parameters.length; i++)
      {
        printDataTypeMethod(parameters[i], name);
      }
    //  printCombinedIteratorMethod(parameters, name);
      printObjectIterator(the_decl);
      //printObjectCombinedIterator(name);
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
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * This method returns the Iterator for individual data type.");
    my_writer.indent(LEVEL2);
    my_writer.print(" * @return" + " Iterator");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    String parameter = the_parameter.typeToString();
    final char new_char = Character.toUpperCase(parameter.charAt(0));
    parameter = parameter.replace(parameter.charAt(0), new_char);
    if (the_parameter.typeToString().equals(STRARR))
    {

      my_writer.print("private " + "org.jmlspecs.jmlunit.strategies.IndefiniteIterator" +
                      " StringArray" + UND + the_name + UND +
                   the_parameter.ident() + BKTS);
    }
    else if (the_parameter.typeToString().equals(STR))
    {
      my_writer.print("private" + " org.jmlspecs.jmlunit.strategies.IndefiniteIterator " + 
                      STR + UND + the_name + UND +
                   the_parameter.ident() + BKTS);
    }
    else
    {
      my_writer.print("private org.jmlspecs.jmlunit.strategies.IndefiniteIterator " +
                   the_parameter.typeToString() + UND + the_name + UND +
                   the_parameter.ident() + BKTS);
    }
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL3);
    if (the_parameter.typeToString().equals(STRARR) ||
        the_parameter.typeToString().equals(STR))
    {

      if (the_parameter.typeToString().equals(STRARR))
      {
        my_writer.print("final " + "org.jmlspecs.jmlunit.strategies.StringStrategy " + 
                        the_parameter.ident() + UND +
                     "string_Strategy = new org.jmlspecs.jmlunit.strategies.StringStrategy()");
      }
      else if (the_parameter.typeToString().equals(STR))
      {
        my_writer.print("final org.jmlspecs.jmlunit.strategies.StringStrategy " + 
                        the_parameter.ident() + UND + "string_Strategy " + 
                     "=  new org.jmlspecs.jmlunit.strategies.StringStrategy()");
      }
      my_writer.indent(LEVEL4);
      my_writer.print(BLK_ST);
      my_writer.indent(LEVEL5);
      my_writer.print("protected String[] addData()");
      my_writer.indent(LEVEL5);
      my_writer.print(BLK_ST);
      my_writer.indent(LEVEL5 + 2);
      my_writer.print("return new String[] " +
        "{/*Add strings separated by coma here for testing.*/};");
      my_writer.indent(LEVEL5);
      my_writer.print(BLK_END + SM_COLN);
      my_writer.indent(LEVEL4);
      my_writer.print(BLK_END + SM_COLN);
      
      my_writer.indent(LEVEL3);
      my_writer.print("return " + the_parameter.ident() + UND + "string" +
                   "_Strategy.iterator()" + SM_COLN);
    }
    else
    {
      my_writer.print("final org.jmlspecs.jmlunit.strategies." + parameter + "StrategyType " +
                   the_parameter.ident() + UND + the_parameter.typeToString() + "_Strategy =");
      my_writer.indent(LEVEL4);
      my_writer.print("new org.jmlspecs.jmlunit.strategies." + parameter +
                   "Strategy()");
      my_writer.indent(LEVEL4);
      my_writer.print(BLK_ST);
      my_writer.indent(LEVEL5);
      my_writer.print("protected " + the_parameter.typeToString() + "[]" + " addData()");
      my_writer.indent(LEVEL5);
      my_writer.print(BLK_ST);
      my_writer.indent(LEVEL4);
      my_writer.print("return new " + the_parameter.typeToString() + "[] " +
                   "{/*You can add data elements here.*/};");
      my_writer.indent(LEVEL5);
      my_writer.print(BLK_END);
      my_writer.indent(LEVEL4);
      my_writer.print(BLK_END + SM_COLN);
      my_writer.indent(LEVEL3);
      my_writer.print("return  " + the_parameter.ident() + UND + the_parameter.typeToString() +
                   "_Strategy.iterator();");
    }
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_END);
    my_writer.newLine(ONE);
  }

//  /**
//   * This method prints the combined iterator for the all data types.
//   * 
//   * @param the_parameters Array of JFormalParameter objects. 
//   * @param the_name Combined name of parameters.
//   */
//  private void printCombinedIteratorMethod(final JFormalParameter[] the_parameters,
//                                           final String the_name)
//  {
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_ST);
//    my_writer.indent(FOUR);
//    my_writer.print(" * This method returns the combined Iterator of all data types.");
//    my_writer.indent(FOUR);
//    my_writer.print(" * @return CombinedParameterIterator");
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_END);
//    my_writer.indent(FOUR);
//    my_writer.print("public CombinedParameterIterator params_" + the_name + BKTS);
//    my_writer.indent(FOUR);
//    my_writer.print(BLK_ST);
//    my_writer.indent(SIX);
//    my_writer.print("allParamIterator = new ArrayList<IndefiniteIterator>();");
//    for (int i = 0; i < the_parameters.length; i++)
//    {
//      my_writer.indent(SIX);
//      if (the_parameters[i].typeToString().equals(STRARR))
//      {
//        my_writer.print("allParamIterator.add(StringArray" + UND +
//                     the_name + UND + the_parameters[i].ident() + "());");
//      }
//      else
//      {
//        my_writer.print("allParamIterator.add(" +
//                     the_parameters[i].typeToString() + UND + the_name + UND +
//                     the_parameters[i].ident() + "());");
//      }
//    }
//    my_writer.indent(SIX);
//    my_writer.print("combinedIt = new CombinedParameterIterator(allParamIterator);");
//    my_writer.indent(SIX);
//    my_writer.print("return combinedIt;");
//    my_writer.indent(FOUR);
//    my_writer.print(BLK_END);
//    my_writer.newLine(TWO);
//  }

  /**
   * This method generates the name for all parameters together.
   * 
   * @param the_parameters Array of JFormalParameter objects.
   * @return String
   */
  private String getCombinedName(final JFormalParameter[] the_parameters)
  {
    final StringBuilder name = new StringBuilder();
    for (int i = 0; i < the_parameters.length; i++)
    {
      if (the_parameters[i].typeToString().equals(STRARR))
      {
        name.append(UND + "StringArray");
      }
      else
      {
        name.append(UND + the_parameters[i].typeToString());
      }
    }
    return name.toString();
  }

  /**
   * This method prints the method to return the iterator of objects for given
   * class.
   * @param the_decl JTypeDeclarationType object.
   */
  private void printObjectIterator(final JTypeDeclarationType the_decl)
  {
    final List<JTypeDeclarationType> allMethods = the_decl.methods();
    
    final List<JFormalParameter> parameters = new ArrayList<JFormalParameter>();
    for (int cnt = 0; cnt < allMethods.size(); cnt++)
    {
      if (allMethods.get(cnt) instanceof JConstructorDeclaration)
      {
        final JConstructorDeclaration a_construct = 
          (JConstructorDeclaration) allMethods.get(cnt);
        final JFormalParameter[] params = a_construct.parameters();
        for (int i = 0; i < params.length; i++)
        {
          parameters.add(params[i]);
        }
      }
    }
      
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * This method returns the iterator of objects for test.");
    my_writer.indent(LEVEL2);
    my_writer.print(" * @return Iterator.");
    my_writer.indent(LEVEL2);
    my_writer.print("*/");
    my_writer.indent(LEVEL2);
    my_writer.print("private Iterator<Object> objects()");
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL3);
    my_writer.print("my_objs = new ArrayList<Object>();");
    my_writer.indent(LEVEL3);
    my_writer.print("int object_count = 0;");
    my_writer.indent(LEVEL3);
    my_writer.print("final int numberOfObjects = 2; //change this number for more objects.");
    my_writer.indent(LEVEL3);
    my_writer.print("while (object_count < numberOfObjects)");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL4);
    my_writer.printOnLine("my_objs.add(new " + my_class_nm + "(");
    if (!parameters.isEmpty())
    {
      for (int count = 0; count < parameters.size(); count++)
      {
        if (parameters.get(count).typeToString().equals(STR) ||
            parameters.get(count).typeToString().equals(STRARR))
        {
          my_writer.printOnLine("null");
        }
        else
        {
          my_writer.printOnLine("0");
        }
        if (count < (parameters.size() - 1))
        {
          my_writer.printOnLine(",");
        }
          
      }
    }
    my_writer.printOnLine("));");
    my_writer.printOnLine(" \n");
    my_writer.indent(LEVEL4);
    my_writer.print("object_count++;");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_END);
  

    my_writer.indent(LEVEL3);
    my_writer.print("return my_objs.iterator();");
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_END);
    my_writer.newLine(ONE);
  }

//  /**
//   * This method prints the actual data provider method which returns the array
//   * Object[][].
//   * 
//   * @param the_name This is the String of class name.
//   */
//  private void printObjectCombinedIterator(final String the_name)
//  {
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_ST);
//    my_writer.indent(FOUR);
//    my_writer.print(" * This method returns the Data Provider Iterator.");
//    my_writer.indent(FOUR);
//    my_writer.print(" * @return Iterator");
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_END);
//    my_writer.indent(FOUR);
//    my_writer.print("public Iterator<Object[]> getIter_" + the_name + BKTS);
//    my_writer.indent(FOUR);
//    my_writer.print(BLK_ST);
//    my_writer.indent(SIX);
//    my_writer.print("Iterator<Object> objectIt =  objects();");
//    my_writer.indent(SIX);
//    my_writer.print("CombinedParameterIterator combIt = params_" + the_name + "();");
//    my_writer.indent(SIX);
//    my_writer.print("CombinedObjectParameterIterator combObjParaIt =");
//    my_writer.indent(EIGHT);
//    my_writer.print("new CombinedObjectParameterIterator(combIt, objectIt);");
//    my_writer.indent(SIX);
//    my_writer.print("return (Iterator<Object[]>)combObjParaIt;");
//    my_writer.indent(FOUR);
//    my_writer.print(BLK_END);
//    my_writer.newLine(TWO);
//  }

  /**
   * This method prints the CombinedIterator class
   * for each method to be tested.
   * @param the_iter Iterator object.
   * @param the_decl JTypeDeclarationType object.
   */
  private void printCombinedIteratorClass(final Iterator the_iter, 
                                          final JTypeDeclarationType the_decl)
  {
  
    while (the_iter.hasNext())
    {
      final Object obj = the_iter.next();
  
      if (obj instanceof JMethodDeclaration)
      {
        final JMethodDeclaration method = (JMethodDeclaration) obj;
        final JFormalParameter[] parameters = method.parameters();
        final String name = method.ident() + getCombinedName(parameters);
        
        printDataProvider(method);
        my_writer.newLine(LEVEL1);
        my_writer.indent(LEVEL1);
        my_writer.print(JDOC_ST);
        my_writer.indent(LEVEL1);
        my_writer.print(" * This class is the CombinedIterator for method " + method.ident() +
                     PERIOD);
        my_writer.indent(LEVEL1);
        my_writer.print(JDOC_END);
        my_writer.indent(LEVEL1);
        my_writer.print("private static class CombinedIteratorFor" + method.ident() +
                        " implements Iterator<Object[]>");
        my_writer.indent(LEVEL1);
        my_writer.print(BLK_ST);
        my_writer.newLine(ONE);
        printDataMembers(parameters.length);
        my_writer.indent(LEVEL2);
        my_writer.print(JDOC_ST + " This is the constructor for CombinedIteratorFor" + 
                     method.ident() + ".*/");
        my_writer.indent(LEVEL2);
        my_writer.print("public CombinedIteratorFor" + method.ident() + BKTS);
        my_writer.indent(LEVEL2);
        my_writer.print(BLK_ST);
        
        
        my_writer.indent(LEVEL3);
        my_writer.print("my_currentObjs = new Object" + SQ_BCK_ST +
                        (method.parameters().length + 1) + SQ_BCK_END + ";");
        my_writer.indent(LEVEL3);
        my_writer.print("my_newObjs = objects();");
        
        for (int i = 0; i < parameters.length; i++)
        {
          my_writer.indent(LEVEL3);
          if (parameters[i].typeToString().equals(STRARR))
          {

            my_writer.print(my_itname + SQ_BCK_ST + i + SQ_BCK_END + EQUAL +
                            "StringArray" + UND + name + UND +
                         parameters[i].ident() + BKTS + SM_COLN);
          }
          else if (parameters[i].typeToString().equals(STR))
          {
            my_writer.print(my_itname + SQ_BCK_ST + i + SQ_BCK_END + EQUAL + 
                            STR + UND + name + UND +
                         parameters[i].ident() + BKTS + SM_COLN);
          }
          else
          {
            my_writer.print(my_itname + SQ_BCK_ST + i + SQ_BCK_END + EQUAL +
                         parameters[i].typeToString() + UND + name + UND +
                         parameters[i].ident() + BKTS + SM_COLN);
          }
        }
        
        my_writer.indent(LEVEL2);
        my_writer.print(BLK_END);
        my_writer.newLine(LEVEL1);
        printClassDataProvider(method, the_decl);
        printHasNext();
        printNext(method);
        printRemove();
        my_writer.indent(LEVEL1);
        my_writer.print(BLK_END);
        
      }
    }
  }
  
  /**
   * This method prints the data members of the class.
   * @param the_param_num The number of parameters for the method to be tested.
   */
  private void printDataMembers(final int the_param_num)
  {
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_ST);
//    my_writer.indent(FOUR);
//    my_writer.print(" * This is the Iterator array of Iterators for all parameters.");
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_END);
//    my_writer.indent(FOUR);
//    my_writer.print("protected ArrayList<IndefiniteIterator> allParamIterator;");
//    my_writer.newLine(ONE);
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_ST);
//    my_writer.indent(FOUR);
//    my_writer.print(" * This is the CombinedParameterIterator i.e. array" + 
//                    " of Iterators for all parameters.");
//    my_writer.indent(FOUR);
//    my_writer.print(JDOC_END);
//    my_writer.indent(FOUR);
//    my_writer.print("protected CombinedParameterIterator combinedIt;");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * This is the array of IndefiniteIterator objects for" +
                      " all parameters.");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    my_writer.print("protected org.jmlspecs.jmlunit.strategies.IndefiniteIterator[] " +
         my_itname + EQUAL);
    my_writer.indent(LEVEL3);
    my_writer.print("new org.jmlspecs.jmlunit.strategies.IndefiniteIterator[" +
                    the_param_num + "];");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * Iterator over the newly created objects");
    my_writer.indent(LEVEL2);
    my_writer.print(" * of the class to be tested.");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    my_writer.print("protected Iterator<Object> my_newObjs;");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * True if it is the first element in iterator.");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    my_writer.print("protected boolean isFirstElement = true;");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * Array of the objects to be returned by next method.");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    my_writer.print("protected Object[] my_currentObjs;");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * Array list of objects for object  iterator.");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    my_writer.print("List<Object> my_objs;");
    my_writer.newLine(ONE);
  }

  /**
   * This method prints the individual data provider. 
   * @param the_method JMethodDeclaration object.
   */
  private void printDataProvider(final JMethodDeclaration the_method)
  {
   
    final JFormalParameter[] parameters = the_method.parameters();
    final String name = the_method.ident() + getCombinedName(parameters);
    my_writer.indent(LEVEL1);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL1);
    my_writer.print(" * This is the actual data provider method used by TestNG.");
    my_writer.indent(LEVEL1);
    my_writer.print(" * @return Iterator");
    my_writer.indent(LEVEL1);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL1);
    my_writer.print("@DataProvider(name = \"test_" + name + "\")");
    my_writer.indent(LEVEL1);
    my_writer.print("public Iterator<Object[]> test_" + name + BKTS);
    my_writer.indent(LEVEL1);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL2);
    my_writer.printOnLine("return (Iterator<Object[]>) new CombinedIteratorFor" + 
                          the_method.ident() + BKTS + SM_COLN);
    //my_writer.printOnLine(".getIter_" + name + "();");
    my_writer.printOnLine("\n");
    my_writer.indent(LEVEL1);
    my_writer.print(BLK_END);
   
  }
  /**
   * This method prints the next method in combined iterator class.
   * @param the_method JMethodDeclaration object.
   */
  private void printNext(final JMethodDeclaration the_method)
  {
    final JFormalParameter[] parameters = the_method.parameters();
    final String name = the_method.ident() + getCombinedName(parameters);
    
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * This method returns the next Object[]" + " in the iterator.");
    my_writer.indent(LEVEL2);
    my_writer.print(" * @return Object[]");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    my_writer.indent(LEVEL2);
    my_writer.print("public Object[] next()");
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_ST);
    
        
    my_writer.indent(LEVEL3);
    my_writer.print("if (isFirstElement)");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_ST);
    
    my_writer.indent(LEVEL4);
    my_writer.print("my_currentObjs[0] = my_newObjs.next()" + SM_COLN);
    for (int i = 0; i < parameters.length; i++)
    { 
      my_writer.indent(LEVEL4);
      my_writer.print("my_currentObjs" + SQ_BCK_ST + (i + 1) + SQ_BCK_END +
                      EQUAL + my_itname + SQ_BCK_ST + i + SQ_BCK_END + GET);
      my_writer.indent(LEVEL4);
      my_writer.print(my_itname + SQ_BCK_ST + i + SQ_BCK_END + ADV);
    }
    my_writer.indent(LEVEL4);
    my_writer.print("isFirstElement = false;");
    my_writer.indent(LEVEL4);
    my_writer.print("return my_currentObjs" + SM_COLN);
    
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_END);
    
    my_writer.indent(LEVEL3);
    my_writer.print("else");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_ST);
    
    my_writer.indent(LEVEL4);
    my_writer.print("if" + "(!" + my_itname + SQ_BCK_ST + (parameters.length - 1) + 
                    SQ_BCK_END + ".atEnd())");
    my_writer.indent(LEVEL4);
    my_writer.print(BLK_ST);
    
    my_writer.indent(LEVEL5);
    my_writer.print("my_currentObjs" + SQ_BCK_ST + parameters.length + 
                    SQ_BCK_END + EQUAL + my_itname + SQ_BCK_ST + (parameters.length - 1) +
                    SQ_BCK_END + GET);
    my_writer.indent(LEVEL5);
    my_writer.print(my_itname + SQ_BCK_ST + (parameters.length - 1) +
                    SQ_BCK_END + ADV);
    my_writer.indent(LEVEL4);
    my_writer.print(BLK_END);
    
    for (int i = parameters.length - 2; i >= 0; i--)
    {
      my_writer.indent(LEVEL4);
      my_writer.print("else if (!" + my_itname + "[" + i + "].atEnd())");
      my_writer.indent(LEVEL4);
      my_writer.print(BLK_ST);
      
      my_writer.indent(LEVEL5);
      my_writer.print("my_currentObjs" + SQ_BCK_ST + (i + 1) + 
                      SQ_BCK_END + EQUAL + my_itname + SQ_BCK_ST + i + SQ_BCK_END + GET);
      my_writer.indent(LEVEL5);
      my_writer.print(my_itname + SQ_BCK_ST + i + SQ_BCK_END + ADV);
      for (int j = parameters.length - 1; j > i; j--)
      {
        my_writer.indent(LEVEL5);
        if (parameters[j].typeToString().equals(STRARR))
        {

          my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + EQUAL +
                          "StringArray" + UND + name + UND +
                       parameters[j].ident() + BKTS + SM_COLN);
        }
        else if (parameters[j].typeToString().equals(STR))
        {
          my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + EQUAL + 
                          STR + UND + name + UND +
                       parameters[j].ident() + BKTS + SM_COLN);
        }
        else
        {
          my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + EQUAL +
                       parameters[j].typeToString() + UND + name + UND +
                       parameters[j].ident() + BKTS + SM_COLN);
        }
        my_writer.indent(LEVEL5);
        my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + GET);
        my_writer.indent(LEVEL5);
        my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + ADV);
      }
      my_writer.indent(LEVEL4);
      my_writer.print(BLK_END);
    }
    
    my_writer.indent(LEVEL4);
    my_writer.print("else");
    my_writer.indent(LEVEL4);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL5);
    my_writer.print("my_currentObjs[0] = my_newObjs.next();");
    
    for (int j = parameters.length - 1; j >= 0; j--)
    {
      my_writer.indent(LEVEL5);
      if (parameters[j].typeToString().equals(STRARR))
      {

        my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + EQUAL +
                        "StringArray" + UND + name + UND +
                     parameters[j].ident() + BKTS + SM_COLN);
      }
      else if (parameters[j].typeToString().equals(STR))
      {
        my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + EQUAL + 
                        STR + UND + name + UND +
                     parameters[j].ident() + BKTS + SM_COLN);
      }
      else
      {
        my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + EQUAL +
                     parameters[j].typeToString() + UND + name + UND +
                     parameters[j].ident() + "();");
      }
      my_writer.indent(LEVEL5);
      my_writer.print("my_currentObjs" + SQ_BCK_ST + (j + 1) + SQ_BCK_END + 
                      EQUAL + my_itname + SQ_BCK_ST + j + SQ_BCK_END + GET);
      my_writer.indent(LEVEL5);
      my_writer.print(my_itname + SQ_BCK_ST + j + SQ_BCK_END + ADV);
    }
    my_writer.indent(LEVEL4);
    my_writer.print(BLK_END);
    
    my_writer.indent(LEVEL4);
    my_writer.print("return my_currentObjs;");
    
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_END);
    
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_END);
  }
  /**
   * This method prints the next method in combined iterator class.
   */
  private void printHasNext()
  {
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * This method returns true if there exists" +
      " next element in the Iterator.");
    my_writer.indent(LEVEL2);
    my_writer.print(" * @return boolean");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    
    my_writer.indent(LEVEL2);
    my_writer.print("public boolean hasNext()");
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL3);
    
    my_writer.print("if (my_newObjs.hasNext())");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL4);
    my_writer.print("return" + " true;");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_END);
    my_writer.indent(LEVEL3);
    
    my_writer.print("else");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL4);
    my_writer.print("for (int i = 0; i < " + my_itname + ".length; i++)");
    my_writer.indent(LEVEL4);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL5);
    my_writer.print("if (!" + my_itname + "[i].atEnd())");
    my_writer.indent(LEVEL5);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL5 + 2);
    my_writer.print("return true;");    
    my_writer.indent(LEVEL5);
    my_writer.print(BLK_END);
    my_writer.indent(LEVEL4);
    my_writer.print(BLK_END);
    my_writer.indent(LEVEL4);
    my_writer.print("return false;");
    my_writer.indent(LEVEL3);
    my_writer.print(BLK_END);

    my_writer.indent(LEVEL2);
    my_writer.print(BLK_END);
  }
  /**
   * This method prints the next method in combined iterator class.
   */
  private void printRemove()
  {
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(" * This method returns the next Object[] in the iterator.");
    my_writer.indent(LEVEL2);
    my_writer.print(JDOC_END);
    
    my_writer.indent(LEVEL2);
    my_writer.print("public void remove()");
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_ST);
    my_writer.indent(LEVEL2);
    my_writer.print(BLK_END);
  }
  /**
   * This method prints the end of class bracket "{".
   */
  private void printClassEnd()
  {
    my_writer.print(BLK_END);
  }
}
