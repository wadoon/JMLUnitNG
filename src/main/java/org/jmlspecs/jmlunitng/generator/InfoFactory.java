/*
 * JMLUnitNG
 * Copyright (C) 2010-14
 */

package org.jmlspecs.jmlunitng.generator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.jml.clauses.JmlSignalsClause;
import com.github.javaparser.ast.jml.clauses.JmlSignalsOnlyClause;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.jmlspecs.jmlunitng.JMLUnitNGError;
import org.jmlspecs.jmlunitng.util.InheritanceComparator;
import org.jmlspecs.jmlunitng.util.ProtectionLevel;

import java.util.*;

import static com.github.javaparser.ast.Modifier.DefaultKeyword.*;

/**
 * Factory class that generates ClassInfo and MethodInfo objects.
 *
 * @author Daniel M. Zimmerman
 * @author Jonathan Hogins
 * @version July 2011
 */
public final class InfoFactory {
    /**
     * The class suffix (for class literals).
     */
    private static final String CLASS_SUFFIX = ".class";

    /**
     * Cache of already created ClassInfo objects.
     */
    private static final Map<String, ClassInfo> CLASS_CACHE =
            new HashMap<>();

    /**
     * Cache of already-created top-level ClassInfo objects by compilation unit.
     */
    private static final Map<CompilationUnit, ClassInfo> COMPILATION_UNIT_CACHE =
            new HashMap<>();

    /**
     * Cache of already-created methods by method symbol.
     */
    private static final SortedMap<ClassInfo, SortedSet<MethodInfo>> METHOD_CACHE =
            new TreeMap<>();

    /**
     * Private constructor to prevent initialization.
     */
    private InfoFactory() {
    }

    /**
     * Generates ClassInfo (and dependent) objects for the given
     * compilation units.
     *
     * @param the_units The compilation units to create ClassInfos from.
     */
    public static void generateInfos(final List<CompilationUnit> the_units) {
        final SortedMap<ClassInfo, SortedSet<MethodInfo>> signals_cache = new TreeMap<>();

        // first, generate ClassInfos and MethodInfos for each tree
        for (CompilationUnit u : the_units) {
            final ClassInfoParser cp = new ClassInfoParser();
            u.accept(cp, null);
            COMPILATION_UNIT_CACHE.put(u, cp.getEnclosingClassInfo());
            final MethodInfoParser mp = new MethodInfoParser(signals_cache);
            u.accept(mp, null);
        }

        // now we should have all the classes and methods, let's match them up;
        // the global method cache has those without signals and literals, so
        // let's replace them with those with signals and literals, where applicable

        final SortedSet<ClassInfo> all_classes = getAllClassInfos();

        for (ClassInfo c : all_classes) {
            final SortedSet<MethodInfo> raw = METHOD_CACHE.get(c);
            final SortedSet<MethodInfo> signals = signals_cache.get(c);
            final SortedSet<MethodInfo> combined = new TreeSet<>();

            if (raw != null) {
                if (signals == null) {
                    combined.addAll(raw);
                } else {
                    final Iterator<MethodInfo> it_raw = raw.iterator();
                    final Iterator<MethodInfo> it_signals = signals.iterator();

                    // iterate over the sets to find the methods to include

                    while (it_signals.hasNext()) {
                        final MethodInfo next_signals = it_signals.next();
                        boolean found = false;
                        while (!found && it_raw.hasNext()) {
                            final MethodInfo next_raw = it_raw.next();
                            if (next_raw.equalsExceptSignals(next_signals)) {
                                found = true;
                                combined.add(next_signals);
                            } else {
                                combined.add(next_raw);
                            }
                        }
                    }
                }
            }

            METHOD_CACHE.put(c, combined);
        }

        processInheritedMethods();
    
    /* // debugging info for literal finding

    for (ClassInfo c : getAllClassInfos()) {
      System.err.println("ClassInfo " + c);
      System.err.println("  Literals");
      for (Map.Entry e : c.getLiterals().entrySet()) {
        System.err.println("    " + e.getKey() + " " + e.getValue());
      }
      System.err.println("  Spec Literals");
      for (Map.Entry e : c.getSpecLiterals().entrySet()) {
        System.err.println("    " + e.getKey() + " " + e.getValue());
      }
      System.err.println("  Methods");
      for (MethodInfo m : c.getMethods()) {
        System.err.println("    " + m);
        System.err.println("      Literals");
        for (Map.Entry e : m.getLiterals().entrySet()) {
          System.err.println("      " + e.getKey() + " " + e.getValue());
        }
        System.err.println("      Spec Literals");
        for (Map.Entry e : m.getSpecLiterals().entrySet()) {
          System.err.println("      " + e.getKey() + " " + e.getValue());
        }
        System.err.println();
      }
      System.err.println();
    }
    */
    }

    /**
     * Returns the cached ClassInfo object for the specified
     * qualified class name.
     *
     * @param the_qualified_name The qualified class name.
     * @return a ClassInfo object representing the class, or null
     * if one has not yet been created.
     */
    public static ClassInfo getClassInfo(final String the_qualified_name) {
        return CLASS_CACHE.get(the_qualified_name);
    }

    /**
     * Returns the cached ClassInfo objects for the specified
     * compilation unit.
     *
     * @param the_unit The compilation unit.
     * @return a list of ClassInfo objects representing the
     * classes in the compilation unit, or null if one
     * has not yet been created.
     */
    public static ClassInfo getClassInfo(final CompilationUnit the_unit) {
        return COMPILATION_UNIT_CACHE.get(the_unit);
    }

    /**
     * @return all the ClassInfos that have been generated.
     */
    public static SortedSet<ClassInfo> getAllClassInfos() {
        final SortedSet<ClassInfo> result = new TreeSet<>();
        result.addAll(CLASS_CACHE.values());
        return result;
    }

    /**
     * Finds all the child classes of the_class for which tests are being
     * generated.
     *
     * @param the_class The class to find the children of.
     * @return all the ClassInfos that describe child classes of the_class.
     */
    public static SortedSet<ClassInfo> getAllChildren(final ClassInfo the_class) {
        final SortedSet<ClassInfo> result = new TreeSet<>();
        for (ClassInfo c : CLASS_CACHE.values()) {
            ClassInfo p = c;
            while (p != null) {
                if (p.equals(the_class)) {
                    result.add(c);
                    break;
                } else {
                    for (ClassInfo i : p.getInterfaces()) {
                        if (i.equals(the_class)) {
                            result.add(c);
                        }
                    }
                    p = p.getParent();
                }
            }
        }
        return result;
    }

    /**
     * Finds all the concrete child classes of the_class for which tests are being
     * generated.
     *
     * @param the_class The class to find the concrete children of.
     * @return all the ClassInfos that describe concrete child classes of the_class.
     */
    public static SortedSet<ClassInfo> getConcreteChildren(final ClassInfo the_class) {
        final SortedSet<ClassInfo> all_children = getAllChildren(the_class);
        final Iterator<ClassInfo> i = all_children.iterator();
        while (i.hasNext()) {
            final ClassInfo c = i.next();
            if (c.isAbstract()) {
                i.remove();
            }
        }
        return all_children;
    }

    /**
     * Processes the inherited methods for all classes.
     */
    private static void processInheritedMethods() {
        final SortedSet<ClassInfo> class_set = getAllClassInfos();
        final Queue<ClassInfo> class_queue = new LinkedList<>();

        // initialize the method sets for all parentless classes

        final Iterator<ClassInfo> it = class_set.iterator();
        while (it.hasNext()) {
            final ClassInfo c = it.next();
            if (c.getParent() == null) {
                it.remove();
                c.initializeMethods(METHOD_CACHE.get(c));
            }
        }

        class_queue.addAll(class_set);

        // initialize the method sets for other classes

        while (!class_queue.isEmpty()) {
            final ClassInfo c = class_queue.poll();
            if (c.getParent().areMethodsInitialized()) {
                final SortedSet<MethodInfo> methods = METHOD_CACHE.get(c);
                // it's safe to add methods from the parent class
                if (c.getParent() != null) {
                    final Set<MethodInfo> parent_methods =
                            new HashSet<>(c.getParent().getMethods());
                    // we do not inherit methods that were already overridden by the parent class
                    parent_methods.removeAll(c.getParent().getOverriddenMethods());
                    for (MethodInfo pm : parent_methods) {
                        if (!pm.isConstructor() && !pm.isStatic() &&
                                !pm.getProtectionLevel().equals(ProtectionLevel.PRIVATE)) {
                            // we do not inherit constructors or static/private methods
                            boolean duplicate = false;
                            for (MethodInfo m : methods) {
                                duplicate = duplicate ||
                                        (m.getName().equals(pm.getName()) &&
                                                m.getParameters().equals(pm.getParameters()));
                            }
                            if (!duplicate) {
                                methods.add(new MethodInfo(pm.getName(), c, pm.getDeclaringClass(),
                                        pm.getProtectionLevel(), pm.getParameters(),
                                        pm.getReturnType(), pm.getSignals(),
                                        pm.getLiterals(), pm.getSpecLiterals(),
                                        pm.isConstructor(), pm.isStatic(),
                                        pm.isDeprecated(), pm.isModel()));
                            }
                        }
                    }
                }
                c.initializeMethods(methods);
            } else {
                class_queue.offer(c);
            }
        }
    }

    /**
     * Creates a ClassInfo object for the given ClassSymbol. Returns a cached
     * version if one exists for the class's qualified name.
     *
     * @param the_class The Class to generate a ClassInfo object for.
     * @return A ClassInfo object representing the class.
     */
    private static synchronized ClassInfo createClassInfo(final ClassOrInterfaceDeclaration the_class) {
        final var fqn = the_class.getFullyQualifiedName().get();
        if (CLASS_CACHE.containsKey(fqn)) {
            return CLASS_CACHE.get(fqn);
        }
        final String name = fqn;
        final boolean is_abstract = the_class.hasModifier(ABSTRACT);
        final boolean is_interface = the_class.isInterface();
        final boolean is_static = the_class.isStatic();
        final boolean is_inner = the_class.isNestedType();

        ClassInfo parent = null;
        //check for instanceof. Returns a NoType instance if no superclass exists
        final var first = the_class.getExtendedTypes().getFirst();
        if (first.isPresent()) {
            var t = first.get().resolve();
            ClassOrInterfaceDeclaration d =
                    (ClassOrInterfaceDeclaration) t.asReferenceType().getTypeDeclaration().get().asClass().toAst().get();
            parent = createClassInfo(d);
        }
        final SortedSet<ClassInfo> interfaces = new TreeSet<>();
        for (var t : the_class.getImplementedTypes()) {
            var it = first.get().resolve();
            ClassOrInterfaceDeclaration d =
                    (ClassOrInterfaceDeclaration) it.asReferenceType().getTypeDeclaration().get().asClass().toAst().get();
            // this should always be the case but it doesn't hurt to be safe
            interfaces.add(createClassInfo(d));
        }
        final boolean is_enumeration =
                parent != null && "java.lang.Enum".equals(parent.getFullyQualifiedName());
        final ClassInfo result =
                new ClassInfo(name, getLevel(the_class), is_abstract, is_interface,
                        is_enumeration, is_static, is_inner, parent, interfaces);
        // ensure this ClassInfo object is cached before creating methods
        CLASS_CACHE.put(name, result);

        // add inner classes after ClassInfo creation.
        final Set<ClassInfo> inner_classes = new HashSet<>();
        final var members = the_class.getMembers();
        for (BodyDeclaration<?> e : members) {
            if (e instanceof ClassOrInterfaceDeclaration td) {
                inner_classes.add(createClassInfo(td));
            }
        }
        result.initializeNestedClasses(inner_classes);

        // add methods after ClassInfo creation.

        SortedSet<MethodInfo> methods = METHOD_CACHE.get(result);
        if (methods == null) {
            methods = new TreeSet<>();
            METHOD_CACHE.put(result, methods);
        }
        for (BodyDeclaration<?> e : members) {
            if (e instanceof MethodDeclaration md) {
                methods.add(createMethodInfo(the_class, md, new ArrayList<>(),
                        new HashMap<>(),
                        new HashMap<>()));

            } else if (e instanceof ConstructorDeclaration cd) {
                methods.add(createMethodInfo(the_class, cd, new ArrayList<>(),
                        new HashMap<>(),
                        new HashMap<>()));
            }
        }
        return result;
    }

  /*@ ensures (\forall String s; \result.getParameterTypes().contains(s);
    @             (\exists VarSymbol v; the_sym.params.contains(v); 
    @                 s.equals(v.getSimpleName().toString()))) &&
    @         the_parent_class != null ==> \result.getParentClass() == the_parent_class &&
    @         the_parent_class == null ==> \result.getParentClass() == \result.getDeclaringClass() &&
    @         \result.getDeclaringClass().getFullyQualifiedName()
    @             .equals(the_sym.getEnclosingElement().getQualifiedName().toString()) &&
    @         \result.getProtectionLevel() == getLevel(the_sym.getModifiers()) &&
    @         \result.isConstructor() == the_sym.isConstructor() &&
    @         \result.isStatic() == the_sym.isStatic();
   */

    /**
     * Creates a MethodInfo object for the given MethodSymbol enclosed in the
     * given ClassInfo.
     *
     * @param the_sym              The MethodSymbol to create a MethodInfo object for.
     * @param the_signals          The ClassInfos for exception types that can be signaled
     *                             by this method.
     * @param the_literal_map      The map of types to literals in the code.
     * @param the_spec_literal_map The map of types to literals in the specs.
     * @return The MethodInfo object.
     */
    private static MethodInfo createMethodInfo(
            final ClassOrInterfaceDeclaration enclDecl,
            final CallableDeclaration<?> the_sym,
            final List<ClassInfo> the_signals,
            final Map<String, SortedSet<String>> the_literal_map,
            final Map<String, SortedSet<String>> the_spec_literal_map) {
        final List<ParameterInfo> params = new ArrayList<>(the_sym.getParameters().size());
        for (var v : the_sym.getParameters()) {
            params.add(createParameterInfo(v));
        }

        final ClassInfo enclosing_class = createClassInfo(enclDecl);
        final ProtectionLevel level = getLevel(the_sym);
        String name = the_sym.getNameAsString();

        // is the method a constructor?
        if ("<init>".equals(name)) {
            name = enclosing_class.getShortName();
        }
        // is the method deprecated? is it a model method? this is crude but functional
        boolean deprecated = false;
        boolean model = false;
        final var annotations = the_sym.getAnnotations();
        deprecated = null != the_sym.getAnnotationByName("java.lang.Deprecated");
        model = null != the_sym.getAnnotationByName("org.jmlspecs.annotation.Model");

        Type returnType;
        if (the_sym.isConstructorDeclaration()) {
            returnType = new ClassOrInterfaceType(null, enclDecl.getNameAsString());
        } else {
            returnType = the_sym.asMethodDeclaration().getType();
        }

        return new MethodInfo(name, enclosing_class, enclosing_class, level, params,
                new TypeInfo(returnType.toString()), the_signals,
                the_literal_map, the_spec_literal_map,
                the_sym.isConstructorDeclaration(), the_sym.isStatic(), deprecated, model);
    }

    /**
     * Returns a ParameterInfo object representing the given VarSymbol.
     *
     * @param the_var_sym The VarSymbol to translate into a ParameterInfo object.
     * @return a ParameterInfo.
     */
    private static ParameterInfo createParameterInfo(final Parameter the_var_sym) {
        var t = the_var_sym.getType();
        int array_dim = 0;

        // check for array dimensions

        if (t instanceof ArrayType a) {
            array_dim = a.getArrayLevel();
            t = a.getElementType();
        }

        // check for generics

        while (t instanceof TypeParameter vt) {
            t = vt.getTypeBound().get(0);//TODO weigl check t.getUpperBound().tsym.asType();
        }

        // create our type name String

        final StringBuilder sb = new StringBuilder(t.toString());
        for (int i = 0; i < array_dim; i++) {
            sb.append("[]");
        }

        return new ParameterInfo(sb.toString(), the_var_sym.getNameAsString());
    }


  /*@ ensures \result.equals(ProtectionLevel.PUBLIC) ==> the_mods.contains(Modifier.PUBLIC) &&
    @         \result.equals(ProtectionLevel.PROTECTED) ==> 
    @           (!the_mods.contains(Modifier.PUBLIC)  && the_mods.contains(Modifier.PROTECTED)) &&  
    @         \result.equals(ProtectionLevel.PRIVATE) ==> (!the_mods.contains(Modifier.PUBLIC) &&
    @            !the_mods.contains(Modifier.PROTECTED) && the_mods.contains(Modifier.PRIVATE)) && 
    @         \result.equals(ProtectionLevel.NO_LEVEL) ==> (!the_mods.contains(Modifier.PUBLIC) &&
    @            !the_mods.contains(Modifier.PROTECTED) && !the_mods.contains(Modifier.PRIVATE));
   */

    /**
     * Returns the protection level present in the given set of Modifiers. Returns
     * null if there are no protection level modifiers (PUBLIC, PROTECTED,
     * PRIVATE) in the given set.
     *
     * @param the_mods The Set<Modifier> from which to extract the protection level
     * @return the protection level.
     */
    private static <T extends Node> ProtectionLevel getLevel(final NodeWithModifiers<T> the_mods) {
        ProtectionLevel level = ProtectionLevel.NO_LEVEL;
        if (the_mods.hasModifier(PUBLIC)) {
            level = ProtectionLevel.PUBLIC;
        } else if (the_mods.hasModifier(PROTECTED)) {
            level = ProtectionLevel.PROTECTED;
        } else if (the_mods.hasModifier(PRIVATE)) {
            level = ProtectionLevel.PRIVATE;
        }
        return level;
    }

    /**
     * JCTree scanner that records relevant information on the classes and methods
     * scanned.
     */
    private static class ClassInfoParser extends VoidVisitorAdapter<Void> {
        /**
         * The parsed enclosing ClassInfo object.
         */
        private ClassInfo my_class_info;

        /**
         * Constructs a new ClassInfoParser with the specified OpenJML API.
         */
        public ClassInfoParser() {
            super();
        }

        /**
         * Extracts information about a class.
         *
         * @param the_tree The class declaration node.
         */
        @Override
        public void visit(ClassOrInterfaceDeclaration the_tree, Void arg) {
            my_class_info = InfoFactory.createClassInfo(the_tree);
            if (!my_class_info.areLiteralsInitialized()) {
                final LiteralsParser lp = new LiteralsParser(false, false);
                the_tree.accept(lp, null);
                //final TypeSpecs specs = my_api.getSpecs(the_tree.sym);
                final LiteralsParser slp = new LiteralsParser(false, true);
                //for (JmlTypeClause c : specs.clauses) {
                the_tree.accept(slp, null);
                //}
                my_class_info.initializeLiterals(lp.getLiteralMap(), slp.getLiteralMap());
            }
        }

        /**
         * Returns the enclosing ClassInfo object parsed by this visitor. Returns null if a
         * tree containing a JmlClassDecl node has not been parsed yet.
         *
         * @return The enclosing ClassInfo for the tree.
         */
        public ClassInfo getEnclosingClassInfo() {
            return my_class_info;
        }
    }

    /**
     * JCTree scanner that records relevant information on the classes and methods
     * scanned.
     */
    private static class MethodInfoParser extends VoidVisitorAdapter<Void> {
        /**
         * A cache of methods associated with classes.
         */
        private final SortedMap<ClassInfo, SortedSet<MethodInfo>> my_cache;

        /**
         * Constructs a MethodInfoParser with the specified cache.
         *
         * @param the_cache The method cache.
         */
        public MethodInfoParser(final SortedMap<ClassInfo, SortedSet<MethodInfo>> the_cache) {
            super();
            my_cache = the_cache;
        }

        /**
         * Extracts information about a method.
         *
         * @param the_tree The method declaration node.
         */
        @Override
        public void visit(MethodDeclaration the_tree, Void arg) {
            var parent = (ClassOrInterfaceDeclaration) the_tree.getParentNode().get();
            // find the signals and add them to the existing method declaration
            final ClassInfo encl_class = createClassInfo(parent);
            final SignalsParser sp = new SignalsParser();
            final var specs = the_tree.getContracts();
            specs.ifPresent(jmlContracts -> jmlContracts.accept(sp, null));
            // find the literals and add them to the existing method declaration
            final LiteralsParser lp = new LiteralsParser(true, false);
            the_tree.accept(lp, null);
            final LiteralsParser slp = new LiteralsParser(false, true);
            specs.ifPresent(jmlContracts -> jmlContracts.accept(slp, null));

            final MethodInfo method =
                    createMethodInfo(parent, the_tree, sp.getExceptionTypes(),
                            lp.getLiteralMap(), slp.getLiteralMap());

            SortedSet<MethodInfo> class_methods = my_cache.get(encl_class);
            if (class_methods == null) {
                class_methods = new TreeSet<>();
                my_cache.put(encl_class, class_methods);
            }
            class_methods.add(method);
            the_tree.accept(this, null);
        }
    }

    /**
     * JCTree scanner that scans for literals to generate a map from primitive types
     * to literals of those types in the tree.
     */
    private static class LiteralsParser extends VoidVisitorAdapter<Void> {
        /**
         * The map of literals.
         */
        private final Map<String, SortedSet<String>> my_literals =
                new HashMap<>();

        /**
         * Do we visit methods?
         */
        private final boolean my_methods;

        /**
         * Do we visit specs?
         */
        private final boolean my_specs;

        /**
         * Constructs a new LiteralsParser.
         *
         * @param the_methods true to visit (and find literals in) methods, false otherwise.
         * @param the_specs   true to visit (and find literals in) specs, false otherwise.
         */
        public LiteralsParser(final boolean the_methods, final boolean the_specs) {
            super();
            my_methods = the_methods;
            my_specs = the_specs;
        }

        /**
         * Scans a tree node. We use this to find instanceof nodes (for class literals),
         * for which there is no specific visitor method.
         */
        @Override
        public void visit(InstanceOfExpr n, Void arg) {
            var checked_type = n.getType();
//                // the type part of an instanceof can be one of three possibilities:
//                // a class/interface, a class/interface with generic parameters,
//                // or an array type (of one of the previous two possibilities)
//                // first we strip off any generics
//                if (checked_type instanceof JCTypeApply) {
//                    checked_type = ((JCTypeApply) checked_type).getType();
//                }
//
//                // then we attempt to determine a class name
//
//                String class_name = null;
//
//                if (checked_type instanceof NameExpr) {
//                    class_name = ((NameExpr) checked_type).sym.getQualifiedName().toString();
//                }
//
//                // currently we don't use array types as class literals, otherwise we'd have
//                // another branch above
            getLiteralSet(Class.class.getName()).add(checked_type.toString() + CLASS_SUFFIX);
            super.visit(n, arg);
        }

        /**
         * Traverses, or not, a method node.
         *
         * @param the_tree The method node.
         */
        @Override
        public void visit(MethodDeclaration the_tree, Void arg) {
            if (my_methods) {
                super.visit(the_tree, arg);
            }
        }


        @Override
        public void visit(BooleanLiteralExpr n, Void arg) {
            addLiteral(n.getValue(), Boolean.class);
        }

        @Override
        public void visit(CharLiteralExpr n, Void arg) {
            addLiteral(n.getValue().charAt(0), Integer.class);
        }

        @Override
        public void visit(DoubleLiteralExpr n, Void arg) {
            addLiteral(Double.valueOf(n.getValue()), Double.class);
        }

        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
            addLiteral(Integer.valueOf(n.getValue()), Integer.class);
        }

        @Override
        public void visit(LongLiteralExpr n, Void arg) {
            addLiteral(Long.valueOf(n.getValue()), Long.class);
        }

        @Override
        public void visit(NullLiteralExpr n, Void arg) {
            addLiteral(null, Object.class);
        }

        @Override
        public void visit(StringLiteralExpr n, Void arg) {
            addLiteral(n.getValue(), String.class);
        }

        /**
         * Extracts information about a literal.
         */
        @Override
        public void visit(TextBlockLiteralExpr n, Void arg) {
            addLiteral(n.getValue(), String.class);
        }

        /**
         * Extracts information about a field access (for class literals).
         *
         * @param the_tree The field access declaration node.
         */
        @Override
        public void visit(final FieldAccessExpr the_tree, Void arg) {
            String class_literal = null;

            // three possible cases; the symbol of this field access is itself a class literal,
            // or the "selected" field of this field access is a class literal, or neither is

            if ("class".equals(the_tree.getNameAsString())) {
                var t = the_tree.calculateResolvedType();
                class_literal = t.describe();
//                if (the_tree.getName() instanceof JCFieldAccess &&
//                        ((FieldAccessExpr) the_tree.selected).sym instanceof ClassSymbol) {
//                    class_literal = ((FieldAccessExpr) the_tree.selected).sym.getQualifiedName().toString() + CLASS_SUFFIX;
//                } else if (the_tree.selected instanceof JCIdent &&
//                        ((JCIdent) the_tree.selected).sym instanceof ClassSymbol) {
//                    class_literal = ((JCIdent) the_tree.selected).sym.getQualifiedName().toString() + CLASS_SUFFIX;
//                }
            }

            if (class_literal != null) {
                getLiteralSet(Class.class.getName()).add(class_literal);
            }

            super.visit(the_tree, arg);
        }

        /**
         * @return the map of literal classes to literals in the tree.
         */
        public Map<String, SortedSet<String>> getLiteralMap() {
            return my_literals;
        }

        /**
         * Adds the specified value to the literals map, adding it for all
         * the integer types it "fits" in if it is an integral literal.
         *
         * @param the_value The value to add.
         * @param the_class The type of the literal in the parse tree.
         */
        private void addLiteral(final Object the_value, final Class<?> the_class) {
            if (isIntegral(the_value)) {
                // get the value as a Long
                final Long integral_value = getIntegralValue(the_value);
                final String integral_string = String.valueOf(integral_value);

                // if the value fits within a byte, add it as a byte
                if (Byte.MIN_VALUE <= integral_value && integral_value <= Byte.MAX_VALUE) {
                    getLiteralSet(byte.class.getName()).add(integral_string);
                }

                // if the value fits within a short, add it as a short
                if (Short.MIN_VALUE <= integral_value && integral_value <= Short.MAX_VALUE) {
                    getLiteralSet(short.class.getName()).add(integral_string);
                }

                // if the value fits within an int, add it as an int
                if (Integer.MIN_VALUE <= integral_value && integral_value <= Integer.MAX_VALUE) {
                    getLiteralSet(int.class.getName()).add(integral_string);
                }

                // always add the value as a long
                getLiteralSet(long.class.getName()).add(integral_string + 'L');

                // if the value fits within a float, and can be exactly translated to a float,
                // add it as a float
                if (-Float.MAX_VALUE <= integral_value && integral_value <= Float.MAX_VALUE) {
                    final float f = integral_value.floatValue();
                    final long l = integral_value.longValue();
                    if ((long) f == l) {
                        getLiteralSet(float.class.getName()).add(integral_string + ".0f");
                    }
                }

                // if the value fits within a double, and can be exactly translated to a double,
                // add it as a double
                if (-Double.MAX_VALUE <= integral_value && integral_value <= Double.MAX_VALUE) {
                    final double d = integral_value.doubleValue();
                    final long l = integral_value.longValue();
                    if ((long) d == l) {
                        getLiteralSet(double.class.getName()).add(integral_string + ".0");
                    }
                }
            } else if (the_value instanceof Float) {
                // floats can also be doubles
                final Float float_value = (Float) the_value;
                getLiteralSet(float.class.getName()).add(String.valueOf(float_value) + 'f');
                getLiteralSet(double.class.getName()).add(String.valueOf(float_value));
            } else if (the_value instanceof Double) {
                // doubles can not always be floats
                final double double_value = ((Double) the_value).doubleValue();
                final float float_value = (float) double_value;
                if ((double) float_value == double_value) {
                    // no loss of precision, let's store it as both
                    getLiteralSet(float.class.getName()).add(String.valueOf(float_value) + 'f');
                }
                getLiteralSet(double.class.getName()).add(String.valueOf(double_value));
            } else {
                getLiteralSet(the_class.getName()).add(the_value.toString());
            }
        }

        /**
         * @param the_class The fully qualified name of the literal class
         *                  for which to get the literal set.
         * @return the literal set.
         */
        private SortedSet<String> getLiteralSet(final String the_class) {
            SortedSet<String> result = my_literals.get(the_class);
            if (result == null) {
                result = new TreeSet<>();
                my_literals.put(the_class, result);
            }
            return result;
        }

        /**
         * @param the_object An object.
         * @return true if the object represents an integral value, false otherwise.
         */
        private boolean isIntegral(final Object the_object) {
            boolean result =
                    the_object instanceof Long || the_object instanceof Integer ||
                            the_object instanceof Short || the_object instanceof Byte;

            if (!result && the_object instanceof Double) {
                final Double d = (Double) the_object;
                result = Math.floor(d) == d;
            } else if (!result && the_object instanceof Float) {
                final Float f = (Float) the_object;
                result = Math.floor(f) == f;
            }

            return result;
        }

        /**
         * @param the_value The value to convert to a Long.
         * @return The converted value.
         */
        private Long getIntegralValue(final Object the_value) {
            Long result = null;
            if (the_value instanceof Number) {
                result = Long.valueOf(((Number) the_value).longValue());
            } else {
                throw new JMLUnitNGError("attempt to convert a " + the_value.getClass() +
                        "to an integral value");
            }
            return result;
        }
    }

    /**
     * JCTree scanner that scans specifically for signals/signals_only clause
     * information to generate a list of exception types.
     */
    private static class SignalsParser extends VoidVisitorAdapter<Void> {
        /**
         * The list of exception types.
         */
        private final List<ClassInfo> my_exception_types = new LinkedList<>();

        /**
         * The comparator used to order the exception types in inheritance order.
         */
        private final InheritanceComparator my_comparator = new InheritanceComparator();

        /**
         * Extracts information about a signals clause for a method.
         */
        @Override
        public void visit(JmlSignalsClause n, Void arg) {
            var t = n.getType().resolve();
            var d = (ClassOrInterfaceDeclaration)
                    t.asReferenceType().getTypeDeclaration().get().asClass().toAst().get();
            addInOrder(createClassInfo(d));
        }

        @Override
        public void visit(JmlSignalsOnlyClause n, Void arg) {
            // for a signals_only clause, we have to add all the exceptions in the list
            for (var et : n.getTypes()) {
                var t = et.resolve();
                var d = (ClassOrInterfaceDeclaration)
                        t.asReferenceType().getTypeDeclaration().get().asClass().toAst().get();
                addInOrder(createClassInfo(d));
            }
        }

        /**
         * @return the exception types found in the methods signals/signals_only clauses.
         */
        public List<ClassInfo> getExceptionTypes() {
            return my_exception_types;
        }

        /**
         * Adds the specified class (which should be an exception type) to the list,
         * in inheritance order.
         *
         * @param the_class The class to add to the list.
         */
        private void addInOrder(final ClassInfo the_class) {
            if (my_exception_types.isEmpty()) {
                my_exception_types.add(the_class);
            } else if (!my_exception_types.contains(the_class)) {
                // we have not previously added this exception type
                boolean added = false;
                for (int i = 0; i < my_exception_types.size(); i++) {
                    final ClassInfo c = my_exception_types.get(i);
                    if (my_comparator.compare(the_class, c) < 0) {
                        my_exception_types.add(i, the_class);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    my_exception_types.add(the_class);
                }
            }
        }
    }
}
