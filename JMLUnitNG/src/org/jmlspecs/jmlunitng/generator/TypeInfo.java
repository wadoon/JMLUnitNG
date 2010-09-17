/*
 * OpenJMLUnit
 * 
 * @author "Jonathan Hogins (jon.hogins@gmail.com)"
 * 
 * @module "OpenJML"
 * 
 * @creation_date "April 2010"
 * 
 * @last_updated_date "May 2010"
 * 
 * @keywords "unit testing", "JML"
 */

package org.jmlspecs.jmlunitng.generator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Name information about a type.
 * 
 * @author Daniel M. Zimmerman
 * @author Jonathan Hogins
 * @version August 2010
 */
public class TypeInfo {
  /**
   * The set of primitive types.
   */
  private static final Set<String> PRIMITIVE_TYPES;

  static {
    final Set<String> prims = new HashSet<String>();
    prims.add("boolean");
    prims.add("int");
    prims.add("long");
    prims.add("float");
    prims.add("double");
    prims.add("byte");
    prims.add("short");
    prims.add("char");
    prims.add("java.lang.String");
    PRIMITIVE_TYPES = Collections.unmodifiableSet(prims);
  }

  /**
   * The fully qualified name of this class.
   */
  protected final String my_name;
  /**
   * The unqualified name of the class.
   */
  protected final String my_short_name;
  /**
   * The generic component of the class.
   */
  protected final String my_generic_comp;

  // @ invariant my_short_name.equals(my_name.substring(my_name.lastIndexOf('.') + 1));
  
  /**
   * Create a new Type with the given fully qualified name. 
   * If the given fully qualified name has a generic portion, it is removed.
   * 
   * @param the_name The fully qualified name of the type.
   */
  // @ ensures my_generic_comp != null <==> the_name.indexOf('<') != the_name.length;
  public TypeInfo(final String the_name) {
    int generic_start = the_name.indexOf('<');
    if (generic_start == -1) {
      generic_start = the_name.length();
      my_generic_comp = "";
    } else {
      my_generic_comp = the_name.substring(generic_start, the_name.length());
    }
    my_name = the_name.substring(0, generic_start);
    my_short_name = my_name.substring(my_name.lastIndexOf('.') + 1);
  }

  /**
   * @return The unqualified name of the class.
   */
  public String getShortName() {
    return my_short_name;
  }

  /**
   * @return The fully qualified name of the class, without generic information.
   */
  public String getFullyQualifiedName() {
    return my_name;
  }
  
  /**
   * @return The generic component of the type.
   */
  public String getGenericComponent() {
    return my_generic_comp;
  }

  /**
   * @return A formatted fully qualified name of the type, with '.' characters
   * replaced by '_' and [] replaced by "Array".
   */
  public String getFormattedName() {
    return my_name.replace('.', '_').replaceAll("\\[\\]", "Array");
  }

  /**
   * @return true if this class is in a package, false otherwise.
   */
  public boolean isPackaged() {
    return my_name.length() > my_short_name.length();
  }
  
  /**
   * @return The package name of the class
   */
  public String getPackageName() {
    String result = "";

    if (my_name.length() > my_short_name.length()) {
      result = my_name.substring(0, my_name.length() - my_short_name.length() - 1);
    }

    return result;
  }

  /**
   * @return true if the type is a primitive, false otherwise.
   */
  // @ensures \result == PRIMITIVE_TYPES.contains(my_name);
  public boolean isPrimitive() {
    return PRIMITIVE_TYPES.contains(my_name);
  }

  /**
   * Compares with object for equality. Two ClassInfo objects are equal if they
   * have the same fully qualified name.
   * 
   * @param the_o The object to compare.
   * @return true if qualified names are equal. false otherwise.
   */
  public boolean equals(final Object the_o) {
    boolean result = false;
    
    if (the_o instanceof TypeInfo) {
      result = ((TypeInfo) the_o).my_name.equals(my_name);
    } 

    return result;
  }

  /**
   * @return A hash code for this object.
   */
  public int hashCode() {
    return my_name.hashCode();
  }

  /**
   * @return The fully qualified name.
   */
  public String toString() {
    return getFullyQualifiedName();
  }
}
