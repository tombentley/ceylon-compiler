/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.compiler.java.codegen;

import static com.redhat.ceylon.compiler.java.codegen.Naming.DeclNameFlag.QUALIFIED;
import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.PRIVATE;
import static com.sun.tools.javac.code.Flags.PROTECTED;
import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.STATIC;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.redhat.ceylon.compiler.java.codegen.Naming.Substitution;
import com.redhat.ceylon.compiler.java.codegen.Naming.Suffix;
import com.redhat.ceylon.compiler.java.codegen.Naming.SyntheticName;
import com.redhat.ceylon.compiler.java.codegen.Naming.Unfix;
import com.redhat.ceylon.compiler.java.codegen.StatementTransformer.DeferredSpecification;
import com.redhat.ceylon.compiler.loader.model.LazyInterface;
import com.redhat.ceylon.compiler.typechecker.model.Annotation;
import com.redhat.ceylon.compiler.typechecker.model.Class;
import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.ControlBlock;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Functional;
import com.redhat.ceylon.compiler.typechecker.model.Generic;
import com.redhat.ceylon.compiler.typechecker.model.Interface;
import com.redhat.ceylon.compiler.typechecker.model.Method;
import com.redhat.ceylon.compiler.typechecker.model.MethodOrValue;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
import com.redhat.ceylon.compiler.typechecker.model.ProducedReference;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.ProducedTypedReference;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.Setter;
import com.redhat.ceylon.compiler.typechecker.model.TypeAlias;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.UnknownType;
import com.redhat.ceylon.compiler.typechecker.model.Value;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnnotationList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyClass;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeGetterDefinition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeSetterDefinition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.BaseMemberExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.LazySpecifierExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.MethodDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Primary;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SequencedArgument;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SpecifierExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SpecifierOrInitializerExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SpecifierStatement;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.TypeParameterDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.TypeParameterList;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * This transformer deals with class/interface declarations
 */
public class ClassTransformer extends AbstractTransformer {

    public static ClassTransformer getInstance(Context context) {
        ClassTransformer trans = context.get(ClassTransformer.class);
        if (trans == null) {
            trans = new ClassTransformer(context);
            context.put(ClassTransformer.class, trans);
        }
        return trans;
    }

    private ClassTransformer(Context context) {
        super(context);
    }

    public List<JCTree> transform(final Tree.ClassOrInterface def) {
        final ClassOrInterface model = def.getDeclarationModel();
        
        // we only create types for aliases so they can be imported with the model loader
        // and since we can't import local declarations let's just not create those types
        // in that case
        if(model.isAlias()
                && Decl.isAncestorLocal(def))
            return List.nil();
        
        naming.clearSubstitutions(model);
        final String javaClassName;
        String ceylonClassName = def.getIdentifier().getText();
        if (def instanceof Tree.AnyInterface) {
            javaClassName = naming.makeTypeDeclarationName(model, QUALIFIED).replaceFirst(".*\\.", "");
        } else {
            javaClassName = Naming.quoteClassName(def.getIdentifier().getText());
        }
        ClassDefinitionBuilder instantiatorImplCb;
        ClassDefinitionBuilder instantiatorDeclCb;
        if (Decl.withinInterface(model)) {
            instantiatorImplCb = gen().current().getCompanionBuilder((Interface)model.getContainer());
            instantiatorDeclCb = gen().current();
        } else {
            instantiatorImplCb = gen().current();
            instantiatorDeclCb = null;
        }
        ClassDefinitionBuilder classBuilder = ClassDefinitionBuilder
                .klass(this, javaClassName, ceylonClassName, Decl.isLocal(model))
                .forDefinition(model);
        TypeParameterList typeParameterList = def.getTypeParameterList();

        // Very special case for Anything
        if ("ceylon.language::Anything".equals(model.getQualifiedNameString())) {
            classBuilder.extending(model.getType(), null);
        }
        
        if (def instanceof Tree.AnyClass) {
            Tree.ParameterList paramList = ((Tree.AnyClass)def).getParameterList();
            Class cls = ((Tree.AnyClass)def).getDeclarationModel();
            // Member classes need a instantiator method
            boolean generateInstantiator = Strategy.generateInstantiator(cls);
            if(generateInstantiator){
                generateInstantiators(cls, classBuilder, paramList, cls, instantiatorDeclCb, instantiatorImplCb, typeParameterList);
            }
            classBuilder.annotations(expressionGen().transform(def.getAnnotationList()));
            if(def instanceof Tree.ClassDefinition){
                transformClass((Tree.ClassDefinition)def, (Class)model, classBuilder, paramList, generateInstantiator, cls, instantiatorDeclCb, instantiatorImplCb, typeParameterList);
            }else{
                // class alias
                classBuilder.constructorModifiers(PRIVATE);
                transformClassAlias((Tree.AnyClass)def, classBuilder);
            }
        }
        
        if (def instanceof Tree.AnyInterface) {
            classBuilder.annotations(expressionGen().transform(def.getAnnotationList()));
            if(def instanceof Tree.InterfaceDefinition){
                transformInterface(def, (Interface)model, classBuilder, typeParameterList);
            }else{
                // interface alias
                classBuilder.annotations(makeAtAlias(model.getExtendedType()));
                classBuilder.isAlias(true);
            }
            classBuilder.isDynamic(model.isDynamic());
        }

        // make sure we set the container in case we move it out
        addAtContainer(classBuilder, model);
        
        // Transform the class/interface members
        List<JCStatement> childDefs = visitClassOrInterfaceDefinition(def, classBuilder);

        // If it's a Class without initializer parameters...
        if (Strategy.generateMain(def)) {
            // ... then add a main() method
            classBuilder.method(makeMainForClass(model));
        }
        
        classBuilder
            .modelAnnotations(model.getAnnotations())
            .modifiers(transformClassDeclFlags(def))
            .satisfies(model.getSatisfiedTypes())
            .caseTypes(model.getCaseTypes(), model.getSelfType())
            .init(childDefs);
        
        // aliases don't need a $getType method
        if(!model.isAlias()){
            // only classes get a $getType method
            if(model instanceof Class)
                classBuilder.addGetTypeMethod(model.getType());
            if(supportsReifiedAlias(model))
                classBuilder.reifiedAlias(model.getType());
        }
        
        List<JCTree> result;
        if (Decl.isAnnotationClass(def)) {
            ListBuffer<JCTree> trees = ListBuffer.lb();
            trees.addAll(transformAnnotationClass((Tree.AnyClass)def));
            transformAnnotationClassConstructor((Tree.AnyClass)def, classBuilder);
            trees.addAll(classBuilder.build());
            result = trees.toList();
        } else {
            result = classBuilder.build();
        }
        
        return result;
    }

    private void transformClassAlias(final Tree.AnyClass def,
            ClassDefinitionBuilder classBuilder) {
        Class model = def.getDeclarationModel();
        ProducedType aliasedClass = model.getExtendedType();
        classBuilder.annotations(makeAtAlias(aliasedClass));
        classBuilder.isAlias(true);
        MethodDefinitionBuilder instantiator = transformClassAliasInstantiator(
                def, model, aliasedClass);
        
        ClassDefinitionBuilder cbInstantiator = null;
        switch (Strategy.defaultParameterMethodOwner(model)) {
        case STATIC:
            cbInstantiator = classBuilder;
            break;
        case OUTER:
            cbInstantiator =  classBuilder.getContainingClassBuilder();
            break;
        case OUTER_COMPANION:
            cbInstantiator = classBuilder.getContainingClassBuilder().getCompanionBuilder(Decl.getClassOrInterfaceContainer(model, true));
            break;
        default:
            throw BugException.unhandledEnumCase(Strategy.defaultParameterMethodOwner(model));
        }
        
        cbInstantiator.method(instantiator);
    }

    /**
     * Builds the instantiator method for a class aliases. In 1.0 you can't
     * actually invoke these, they exist just so there's somewhere to put the
     * class alias annotations. In 1.2 (when we fix #1295) the
     * instantiators will actually do something.
     */
    private MethodDefinitionBuilder transformClassAliasInstantiator(
            final Tree.AnyClass def, Class model, ProducedType aliasedClass) {
        MethodDefinitionBuilder instantiator = MethodDefinitionBuilder.systemMethod(this, naming.getAliasInstantiatorMethodName(model));
        int f = 0;
        if (Strategy.defaultParameterMethodStatic(def.getDeclarationModel())) {
            f = STATIC;
        }
        instantiator.modifiers((transformClassDeclFlags(def) & ~FINAL)| f);
        for (TypeParameter tp : typeParametersOfAllContainers(model, true)) {
            instantiator.typeParameter(tp);
        }
        
        instantiator.resultType(null, makeJavaType(aliasedClass));
        instantiator.annotationFlags(Annotations.MODEL_AND_USER | Annotations.IGNORE);
        // We need to reify the parameters, at least so they have reified annotations
        
        
        for (final Tree.Parameter param : def.getParameterList().getParameters()) {
            // Overloaded instantiators
            Parameter paramModel = param.getParameterModel();
            at(param);

            List<JCAnnotation> annotations = expressionGen().transform(Decl.getAnnotations(def, param));
            transformParameter(instantiator, paramModel, annotations);
        }
        instantiator.body(make().Throw(makeNewClass(makeJavaType(typeFact().getExceptionDeclaration().getType(), JT_CLASS_NEW))));
        return instantiator;
    }

    /**
     * Generates a constructor for an annotation class which takes the 
     * annotation type as parameter.
     * @param classBuilder
     */
    private void transformAnnotationClassConstructor(
            Tree.AnyClass def,
            ClassDefinitionBuilder classBuilder) {
        Class klass = def.getDeclarationModel();
        MethodDefinitionBuilder annoCtor = classBuilder.addConstructor();
        annoCtor.ignoreModelAnnotations();
        // constructors are never final
        annoCtor.modifiers(transformClassDeclFlags(klass) & ~FINAL);
        ParameterDefinitionBuilder pdb = ParameterDefinitionBuilder.systemParameter(this, "anno");
        pdb.type(makeJavaType(klass.getType(), JT_ANNOTATION), null);
        annoCtor.parameter(pdb);
        
        // It's up to the caller to invoke value() on the Java annotation for a sequenced
        // annotation
        
        ListBuffer<JCExpression> args = ListBuffer.lb();
        for (Tree.Parameter parameter : def.getParameterList().getParameters()) {
            at(parameter);
            Parameter parameterModel = parameter.getParameterModel();
            JCExpression annoAttr = make().Apply(null, naming.makeQuotedQualIdent(naming.makeUnquotedIdent("anno"),
                    parameter.getParameterModel().getName()),
                    List.<JCExpression>nil());
            ProducedType parameterType = parameterModel.getType();
            JCExpression argExpr;
            if (typeFact().isIterableType(parameterType)
                    && !isCeylonString(parameterType)) {
                // Convert from array to Sequential
                ProducedType iteratedType = typeFact().getIteratedType(parameterType);
                if (isCeylonBasicType(iteratedType)) {
                    argExpr = utilInvocation().sequentialWrapperBoxed(annoAttr);
                } else if (Decl.isAnnotationClass(iteratedType.getDeclaration())) {
                    // Can't use Util.sequentialAnnotation becase we need to 'box'
                    // the Java annotations in their Ceylon annotation class
                    argExpr = make().Apply(null, naming.makeUnquotedIdent(naming.getAnnotationSequenceMethodName()), List.of(annoAttr));
                    ListBuffer<JCStatement> stmts = ListBuffer.lb();
                    SyntheticName array = naming.synthetic(Unfix.$array$);
                    SyntheticName sb = naming.synthetic(Unfix.$sb$);
                    SyntheticName index = naming.synthetic(Unfix.$index$);
                    SyntheticName element = naming.synthetic(Unfix.$element$);
                    stmts.append(makeVar(FINAL, sb, 
                            make().TypeArray(make().Type(syms().objectType)),
                            make().NewArray(make().Type(syms().objectType), List.of(naming.makeQualIdent(array.makeIdent(), "length")), null)));
                    stmts.append(makeVar(index, 
                            make().Type(syms().intType),
                            make().Literal(0)));
                    stmts.append(make().ForeachLoop(
                            makeVar(element, makeJavaType(iteratedType, JT_ANNOTATION), null), 
                            array.makeIdent(), 
                            make().Exec(make().Assign(
                                    make().Indexed(sb.makeIdent(), 
                                            make().Unary(JCTree.POSTINC, index.makeIdent())), 
                                    instantiateAnnotationClass(iteratedType, element.makeIdent())))));
                    stmts.append(make().Return(
                            make().NewClass(null,
                                    null,
                                    make().QualIdent(syms().ceylonTupleType.tsym),
                                    List.of(makeReifiedTypeArgument(iteratedType),
                                            sb.makeIdent(),
                                            makeEmpty(),
                                            make().Literal(false)), 
                                    null)));
                    classBuilder.method(
                            MethodDefinitionBuilder.systemMethod(this, naming.getAnnotationSequenceMethodName())
                                .ignoreModelAnnotations()
                                .modifiers(PRIVATE | STATIC)
                                .resultType(null, makeJavaType(typeFact().getSequentialType(iteratedType)))
                                .parameter(ParameterDefinitionBuilder.systemParameter(this, array.getName())
                                        .type(make().TypeArray(makeJavaType(iteratedType, JT_ANNOTATION)), null))
                                .body(stmts.toList()));
                } else if (isCeylonMetamodelDeclaration(iteratedType)) {
                    argExpr = makeMetamodelInvocation("parseMetamodelReferences", 
                            List.<JCExpression>of(makeReifiedTypeArgument(iteratedType), annoAttr), 
                            List.<JCExpression>of(makeJavaType(iteratedType, JT_TYPE_ARGUMENT)));
                } else if (Decl.isEnumeratedTypeWithAnonCases(iteratedType)) {
                    argExpr = makeMetamodelInvocation("parseEnumerationReferences", 
                            List.<JCExpression>of(makeReifiedTypeArgument(iteratedType), annoAttr), 
                            List.<JCExpression>of(makeJavaType(iteratedType, JT_TYPE_ARGUMENT)));
                } else {
                    argExpr = makeErroneous(parameter, "compiler bug");
                }
            } else if (Decl.isAnnotationClass(parameterType.getDeclaration())) {
                argExpr = instantiateAnnotationClass(parameterType, annoAttr);
            } else if (isCeylonMetamodelDeclaration(parameterType)) {
                argExpr = makeMetamodelInvocation("parseMetamodelReference", 
                            List.<JCExpression>of(annoAttr), 
                            List.<JCExpression>of(makeJavaType(parameterType, JT_TYPE_ARGUMENT)));
            } else if (Decl.isEnumeratedTypeWithAnonCases(parameterType)) {
                argExpr = makeMetamodelInvocation("parseEnumerationReference", 
                        List.<JCExpression>of(annoAttr), 
                        null);
            } else {
                argExpr = annoAttr;
                argExpr = expressionGen().applyErasureAndBoxing(annoAttr, parameterType.withoutUnderlyingType(), false, BoxingStrategy.UNBOXED, parameterType);
            }
            args.add(argExpr);
        }
        annoCtor.body(at(def).Exec(
                make().Apply(null,  naming.makeThis(), args.toList())));
    }

    private JCNewClass instantiateAnnotationClass(
            ProducedType annotationClass,
            JCExpression javaAnnotationInstance) {
        return make().NewClass(null, null, makeJavaType(annotationClass), 
                List.<JCExpression>of(javaAnnotationInstance), null);
    }

    /**
     * Transforms an annotation class into a Java annotation type.
     * <pre>
     * annotation class Foo(String s, Integer i=1) {}
     * </pre>
     * is transformed into
     * <pre>
     * @Retention(RetentionPolicy.RUNTIME)
     * @interface Foo$annotation$ {
     *     String s();
     *     long i() default 1;
     * }
     * </pre>
     * If the annotation class is a subtype of SequencedAnnotation a wrapper
     * annotation is also generated:
     * <pre>
     * @Retention(RetentionPolicy.RUNTIME)
     * @interface Foo$annotations${
     *     Foo$annotation$[] value();
     * }
     * </pre>
     */
    private List<JCTree> transformAnnotationClass(Tree.AnyClass def) {
        Class klass = (Class)def.getDeclarationModel();
        String annotationName = Naming.suffixName(Suffix.$annotation$, klass.getName());
        ClassDefinitionBuilder annoBuilder = ClassDefinitionBuilder.klass(this, annotationName, null, false);
        
        // annotations are never explicitly final in Java
        annoBuilder.modifiers(Flags.ANNOTATION | Flags.INTERFACE | (transformClassDeclFlags(def) & ~FINAL));
        annoBuilder.annotations(makeAtRetention(RetentionPolicy.RUNTIME));
        annoBuilder.annotations(makeAtIgnore());
        
        for (Tree.Parameter p : def.getParameterList().getParameters()) {
            Parameter parameterModel = p.getParameterModel();
            annoBuilder.method(makeAnnotationMethod(p));
        }
        List<JCTree> result;
        if (isSequencedAnnotation(klass)) {
            result = annoBuilder.annotations(makeAtAnnotationTarget()).build();
            String wrapperName = Naming.suffixName(Suffix.$annotations$, klass.getName());
            ClassDefinitionBuilder sequencedBuilder = ClassDefinitionBuilder.klass(this, wrapperName, null, false);
            // annotations are never explicitely final in Java
            sequencedBuilder.modifiers(Flags.ANNOTATION | Flags.INTERFACE | (transformClassDeclFlags(def) & ~FINAL));
            sequencedBuilder.annotations(makeAtRetention(RetentionPolicy.RUNTIME));
            MethodDefinitionBuilder mdb = MethodDefinitionBuilder.systemMethod(this, naming.getSequencedAnnotationMethodName());
            mdb.annotationFlags(Annotations.MODEL_AND_USER);
            mdb.modifiers(PUBLIC | ABSTRACT);
            mdb.resultType(null, make().TypeArray(makeJavaType(klass.getType(), JT_ANNOTATION)));
            mdb.noBody();
            ClassDefinitionBuilder sequencedAnnotation = sequencedBuilder.method(mdb);
            sequencedAnnotation.annotations(transformAnnotationConstraints(klass));
            sequencedAnnotation.annotations(makeAtIgnore());
            result = result.appendList(sequencedAnnotation.build());
            
        } else {
            result = annoBuilder.annotations(transformAnnotationConstraints(klass)).build();
        }
        
        return result;
    }
    
    private List<JCAnnotation> makeAtRetention(RetentionPolicy retentionPolicy) {
        return List.of(
                make().Annotation(
                        make().Type(syms().retentionType), 
                        List.of(naming.makeQuotedQualIdent(make().Type(syms().retentionPolicyType), retentionPolicy.name()))));
    }
    
    /** 
     * Makes {@code @java.lang.annotation.Target(types)} 
     * where types are the given element types.
     */
    private List<JCAnnotation> makeAtAnnotationTarget(ElementType... types) {
        List<JCExpression> typeExprs = List.<JCExpression>nil();
        for (ElementType type : types) {
            typeExprs = typeExprs.prepend(naming.makeQuotedQualIdent(make().Type(syms().elementTypeType), type.name()));
        }
        return List.of(
                make().Annotation(
                        make().Type(syms().targetType), 
                        List.<JCExpression>of(make().NewArray(null, null, typeExprs))));
    }
    
    private List<JCAnnotation> transformAnnotationConstraints(Class klass) {
        TypeDeclaration meta = (TypeDeclaration)typeFact().getLanguageModuleDeclaration("ConstrainedAnnotation");
        ProducedType constrainedType = klass.getType().getSupertype(meta);
        if (constrainedType != null) {
            ProducedType programElement = constrainedType.getTypeArgumentList().get(2);
            if (programElement.isSubtypeOf(((TypeDeclaration)typeFact().getLanguageModuleDeclarationDeclaration("ClassOrInterfaceDeclaration")).getType())
                    || programElement.isSubtypeOf(((TypeDeclaration)typeFact().getLanguageModuleDeclarationDeclaration("Package")).getType())
                    || programElement.isSubtypeOf(((TypeDeclaration)typeFact().getLanguageModuleDeclarationDeclaration("Module")).getType())) {
                return makeAtAnnotationTarget(ElementType.TYPE);
            } else if (programElement.isSubtypeOf(((TypeDeclaration)typeFact().getLanguageModuleDeclarationDeclaration("ValueDeclaration")).getType())
                    || programElement.isSubtypeOf(((TypeDeclaration)typeFact().getLanguageModuleDeclarationDeclaration("FunctionDeclaration")).getType())) {
                return makeAtAnnotationTarget(ElementType.METHOD, ElementType.PARAMETER);
            } else if (programElement.isSubtypeOf(((TypeDeclaration)typeFact().getLanguageModuleDeclarationDeclaration("Import")).getType())) {
                return makeAtAnnotationTarget(ElementType.FIELD);
            }
        }
        return List.<JCAnnotation>nil();
    }

    private JCExpression transformAnnotationParameterDefault(Tree.Parameter p) {
        Tree.SpecifierOrInitializerExpression defaultArgument = Decl.getDefaultArgument(p);
        Tree.Expression defaultExpression = defaultArgument.getExpression();
        Tree.Term term = defaultExpression.getTerm();
        JCExpression defaultLiteral = null;
        if (term instanceof Tree.Literal
                && !(term instanceof Tree.QuotedLiteral)) {
            defaultLiteral = expressionGen().transform((Tree.Literal)term);
        } else if (term instanceof Tree.BaseMemberExpression) {
            Tree.BaseMemberExpression bme = (Tree.BaseMemberExpression)term;
            Declaration decl = bme.getDeclaration();
            if (isBooleanTrue(decl)) {
                defaultLiteral = makeBoolean(true);
            } else if (isBooleanFalse(decl)) {
                defaultLiteral = makeBoolean(false);
            } else if (typeFact().isEmptyType(bme.getTypeModel())) {
                defaultLiteral = make().NewArray(null, null, List.<JCExpression>nil());
            } else if (Decl.isAnonCaseOfEnumeratedType(bme)) {
                defaultLiteral = makeClassLiteral(bme.getTypeModel());
            } else {
                defaultLiteral = make().Literal(bme.getDeclaration().getQualifiedNameString());
            }
        } else if (term instanceof Tree.MemberOrTypeExpression) {
            Tree.MemberOrTypeExpression mte = (Tree.MemberOrTypeExpression)term;
            defaultLiteral = make().Literal(mte.getDeclaration().getQualifiedNameString());
        } else if (term instanceof Tree.SequenceEnumeration) {
            Tree.SequenceEnumeration seq = (Tree.SequenceEnumeration)term;
            SequencedArgument sequencedArgument = seq.getSequencedArgument();
            defaultLiteral = makeArrayInitializer(sequencedArgument);
        } else if (term instanceof Tree.Tuple) {
            Tree.Tuple seq = (Tree.Tuple)term;
            SequencedArgument sequencedArgument = seq.getSequencedArgument();
            defaultLiteral = makeArrayInitializer(sequencedArgument);
        } else if (term instanceof Tree.InvocationExpression) {
            // Allow invocations of annotation constructors, so long as they're
            // themselves being invoked with permitted arguments
            Tree.InvocationExpression invocation = (Tree.InvocationExpression)term;
            try {
                defaultLiteral = AnnotationInvocationVisitor.transform(expressionGen(), invocation);
            } catch (BugException e) {
                defaultLiteral = e.makeErroneous(this, invocation);
            }
        } else if (term instanceof Tree.MemberLiteral) {
            defaultLiteral = expressionGen().makeMetaLiteralStringLiteralForAnnotation((Tree.MemberLiteral) term);
        } else if (term instanceof Tree.TypeLiteral) {
            defaultLiteral = expressionGen().makeMetaLiteralStringLiteralForAnnotation((Tree.TypeLiteral) term);
        }
        if (defaultLiteral == null) {
            defaultLiteral = makeErroneous(p, "compiler bug: " + p.getParameterModel().getName() + " has an unsupported defaulted parameter expression");
        }
        return defaultLiteral;
    }

    private JCExpression transformAnnotationMethodType(Tree.Parameter parameter) {
        ProducedType parameterType = parameter.getParameterModel().getType();
        JCExpression type = null;
        if (isScalarAnnotationParameter(parameterType)) {
            type = makeJavaType(parameterType.withoutUnderlyingType(), JT_ANNOTATION);
        } else if (isMetamodelReference(parameterType)) {
            type = make().Type(syms().stringType);
        } else if (Decl.isEnumeratedTypeWithAnonCases(parameterType)) {
            type = makeJavaClassTypeBounded(parameterType);
        } else if (typeFact().isIterableType(parameterType)) {
            ProducedType iteratedType = typeFact().getIteratedType(parameterType);
            if (isScalarAnnotationParameter(iteratedType)) {
                JCExpression scalarType = makeJavaType(iteratedType, JT_ANNOTATION);
                type = make().TypeArray(scalarType);
            } else if (isMetamodelReference(iteratedType)) {
                JCExpression scalarType = make().Type(syms().stringType);
                type = make().TypeArray(scalarType);
            } else if (Decl.isEnumeratedTypeWithAnonCases(iteratedType)) {
                JCExpression scalarType = makeJavaClassTypeBounded(iteratedType);
                type = make().TypeArray(scalarType);
            }
        }
        if (type == null) {
            type = makeErroneous(parameter, "compiler bug: " + parameter.getParameterModel().getName() + " has an unsupported annotation parameter type");
        }
        return type;
    }

    private boolean isMetamodelReference(ProducedType parameterType) {
        return isCeylonMetamodelDeclaration(parameterType);
    }
    /**
     * Makes a new Array expression suitable for use in initializing a Java array
     * using as elements the positional arguments 
     * of the given {@link Tree.SequencedArgument} (which must 
     * be {@link Tree.ListedArgument}s).
     * 
     * <pre>
     *     Whatever[] w = <strong>{ listedArg1, listedArg2, ... }</strong>
     *     //             ^---------- this bit ---------^
     * </pre>
     * 
     * @param sequencedArgument
     * @return The array initializer expression
     */
    JCExpression makeArrayInitializer(
            Tree.SequencedArgument sequencedArgument) {
        JCExpression defaultLiteral;
        ListBuffer<JCExpression> elements = ListBuffer.<JCTree.JCExpression>lb();
        if (sequencedArgument != null) {
            for (Tree.PositionalArgument arg : sequencedArgument.getPositionalArguments()) {
                if (arg instanceof Tree.ListedArgument) {
                    Tree.ListedArgument la = (Tree.ListedArgument)arg;
                    elements.append(expressionGen().transformExpression(la.getExpression().getTerm(), BoxingStrategy.UNBOXED, la.getExpression().getTypeModel()));
                } else {
                    elements = null;
                    break;
                }
            }
        }
        defaultLiteral = elements == null ? null : 
            make().NewArray(null, List.<JCExpression>nil(), elements.toList());
        return defaultLiteral;
    }

    private boolean isScalarAnnotationParameter(ProducedType parameterType) {
        return isCeylonBasicType(parameterType)
                || Decl.isAnnotationClass(parameterType.getDeclaration());
    }

    private List<JCStatement> visitClassOrInterfaceDefinition(Node def, ClassDefinitionBuilder classBuilder) {
        // Transform the class/interface members
        CeylonVisitor visitor = gen().visitor;
        
        final ListBuffer<JCTree> prevDefs = visitor.defs;
        final boolean prevInInitializer = visitor.inInitializer;
        final ClassDefinitionBuilder prevClassBuilder = visitor.classBuilder;
        try {
            visitor.defs = new ListBuffer<JCTree>();
            visitor.inInitializer = true;
            visitor.classBuilder = classBuilder;
            def.visitChildren(visitor);
            return (List<JCStatement>)visitor.getResult().toList();
        } finally {
            visitor.classBuilder = prevClassBuilder;
            visitor.inInitializer = prevInInitializer;
            visitor.defs = prevDefs;
            naming.closeScopedSubstitutions(def.getScope());
        }
    }

    private void generateInstantiators(Class model, ClassDefinitionBuilder classBuilder, Tree.ParameterList paramList,
            Class cls, ClassDefinitionBuilder instantiatorDeclCb, ClassDefinitionBuilder instantiatorImplCb, TypeParameterList typeParameterList) {
        // TODO Instantiators on companion classes
        classBuilder.constructorModifiers(PROTECTED);
        
        if (Decl.withinInterface(cls)) {
            MethodDefinitionBuilder instBuilder = MethodDefinitionBuilder.systemMethod(this, naming.getInstantiatorMethodName(cls));
            DefaultedArgumentOverload overloaded = new DefaultedArgumentInstantiator(daoAbstract, model);
            overloaded.makeOverload(
                    instBuilder,
                    paramList.getModel(),
                    null,
                    typeParameterListModel(typeParameterList));
            instantiatorDeclCb.method(instBuilder);
        }
        if (!Decl.withinInterface(cls)
                || !model.isFormal()) {
            MethodDefinitionBuilder instBuilder = MethodDefinitionBuilder.systemMethod(this, naming.getInstantiatorMethodName(cls));
            DefaultedArgumentOverload overloaded = new DefaultedArgumentInstantiator(!cls.isFormal() ? daoThis : daoAbstract, model);
            overloaded.makeOverload(
                    instBuilder,
                    paramList.getModel(),
                    null,
                    typeParameterListModel(typeParameterList));
            instantiatorImplCb.method(instBuilder);
        }
    }

    private void makeAttributeForValueParameter(ClassDefinitionBuilder classBuilder, Tree.Parameter parameterTree, List<JCAnnotation> annotations) {
        Parameter decl = parameterTree.getParameterModel();
        if (!(decl.getModel() instanceof Value)) {
            return;
        }        
        final Value value = (Value)decl.getModel();
        if (parameterTree instanceof Tree.ValueParameterDeclaration
                && (value.isShared() || value.isCaptured())) {
            makeFieldForParameter(classBuilder, decl);
            AttributeDefinitionBuilder adb = AttributeDefinitionBuilder.getter(this, decl.getName(), decl.getModel());
            adb.modifiers(classGen().transformAttributeGetSetDeclFlags(decl.getModel(), false));
            adb.userAnnotations(annotations);
            classBuilder.attribute(adb);
            if (value.isVariable()) {
                AttributeDefinitionBuilder setter = AttributeDefinitionBuilder.setter(this, decl.getName(), decl.getModel());
                setter.modifiers(classGen().transformAttributeGetSetDeclFlags(decl.getModel(), false));
                setter.userAnnotations(annotations);
                classBuilder.attribute(setter);
            }
        } else if (decl.isHidden()
                        // TODO Isn't this always true here? We know this is a parameter to a Class
                        && (decl.getDeclaration() instanceof TypeDeclaration)) {
            Declaration member = CodegenUtil.findMethodOrValueForParam(decl);
            if (Strategy.createField(decl, (Value)member)) {
                // The field itself is created by when we transform the AttributeDeclaration 
                // but it has to be initialized here so all the fields are initialized in parameter order
                classBuilder.init(make().Exec(
                        make().Assign(naming.makeQualifiedName(naming.makeThis(), value, Naming.NA_IDENT), 
                                makeUnquotedIdent(Naming.getAliasedParameterName(decl)))));
            }
        }
    }

    private int transformClassParameterDeclFlags(Parameter param) {
        return param.getModel().isVariable() ? 0 : FINAL;
    }
    
    private void makeFieldForParameter(ClassDefinitionBuilder classBuilder,
            Parameter decl) {
        MethodOrValue model = decl.getModel();
        classBuilder.defs(make().VarDef(make().Modifiers(transformClassParameterDeclFlags(decl) | PRIVATE, makeAtIgnore()),
                names().fromString(Naming.quoteFieldName(decl.getName())), 
                classGen().transformClassParameterType(decl), null));
        
        classBuilder.init(make().Exec(make().Assign(
                naming.makeQualifiedName(naming.makeThis(), model, Naming.NA_IDENT), 
                naming.makeName(model, Naming.NA_IDENT_PARAMETER_ALIASED))));
    }
    

    private void transformParameter(ParameterizedBuilder classBuilder, Parameter param, List<JCAnnotation> annotations) {

        JCExpression type = classGen().transformClassParameterType(param);
        ParameterDefinitionBuilder pdb = ParameterDefinitionBuilder.explicitParameter(this, param);
        pdb.aliasName(Naming.getAliasedParameterName(param));
        pdb.sequenced(param.isSequenced());
        pdb.defaulted(param.isDefaulted());
        pdb.type(type, makeJavaTypeAnnotations(param.getModel()));
        pdb.modifiers(transformClassParameterDeclFlags(param));
        if (!(param.getModel().isShared() || param.getModel().isCaptured())) {
            // We load the model for shared parameters from the corresponding member
            pdb.modelAnnotations(param.getModel().getAnnotations());
            pdb.userAnnotations(annotations);
        }

        if (classBuilder instanceof ClassDefinitionBuilder
                && pdb.requiresBoxedVariableDecl()) {
            ((ClassDefinitionBuilder)classBuilder).init(pdb.buildBoxedVariableDecl());
        }
        classBuilder.parameter(pdb);
    }
    
    private void transformClass(com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyClass def, Class model, ClassDefinitionBuilder classBuilder, 
            com.redhat.ceylon.compiler.typechecker.tree.Tree.ParameterList paramList, boolean generateInstantiator, 
            Class cls, ClassDefinitionBuilder instantiatorDeclCb, ClassDefinitionBuilder instantiatorImplCb, TypeParameterList typeParameterList) {
        // do reified type params first
        if(typeParameterList != null)
            classBuilder.reifiedTypeParameters(typeParameterList);
        
        for (final Tree.Parameter param : paramList.getParameters()) {
            // Overloaded instantiators
            
            Parameter paramModel = param.getParameterModel();
            Parameter refinedParam = CodegenUtil.findParamForDecl(
                    (TypedDeclaration)CodegenUtil.getTopmostRefinedDeclaration(param.getParameterModel().getModel()));
            at(param);

            List<JCAnnotation> annotations = expressionGen().transform(Decl.getAnnotations(def, param));
            transformParameter(classBuilder, paramModel, annotations);
            makeAttributeForValueParameter(classBuilder, param, annotations);
            makeMethodForFunctionalParameter(classBuilder, def, param, annotations);
            
            if (Strategy.hasDefaultParameterValueMethod(paramModel)
                    || Strategy.hasDefaultParameterOverload(paramModel)
                    || (generateInstantiator
                            && refinedParam != null
                            && (Strategy.hasDefaultParameterValueMethod(refinedParam)
                                    || Strategy.hasDefaultParameterOverload(refinedParam)))) {
                ClassDefinitionBuilder cbForDevaultValues;
                ClassDefinitionBuilder cbForDevaultValuesDecls = null;
                switch (Strategy.defaultParameterMethodOwner(model)) {
                case STATIC:
                    cbForDevaultValues = classBuilder;
                    break;
                case OUTER:
                    cbForDevaultValues = classBuilder.getContainingClassBuilder();
                    break;
                case OUTER_COMPANION:
                    cbForDevaultValues = classBuilder.getContainingClassBuilder().getCompanionBuilder(Decl.getClassOrInterfaceContainer(model, true));
                    cbForDevaultValuesDecls = classBuilder.getContainingClassBuilder();
                    break;
                default:
                    cbForDevaultValues = classBuilder.getCompanionBuilder(model);
                }
                if ((Strategy.hasDefaultParameterValueMethod(paramModel) 
                            || (refinedParam != null && Strategy.hasDefaultParameterValueMethod(refinedParam)))) { 
                    if (!generateInstantiator || refinedParam == paramModel) {
                        cbForDevaultValues.method(makeParamDefaultValueMethod(false, def.getDeclarationModel(), paramList, param, typeParameterList));
                        if (cbForDevaultValuesDecls != null) {
                            cbForDevaultValuesDecls.method(makeParamDefaultValueMethod(true, def.getDeclarationModel(), paramList, param, typeParameterList));
                        }
                    } else if (Strategy.hasDelegatedDpm(cls)) {
                        java.util.List<Parameter> parameters = paramList.getModel().getParameters();
                        MethodDefinitionBuilder mdb = 
                        makeDelegateToCompanion((Interface)cls.getRefinedDeclaration().getContainer(),
                                paramModel.getModel().getProducedTypedReference(cls.getType(), null),
                                ((TypeDeclaration)cls.getContainer()).getType(),
                                FINAL | transformClassDeclFlags(cls), 
                                List.<TypeParameter>nil(), Collections.<java.util.List<ProducedType>>emptyList(),
                                paramModel.getType(), 
                                Naming.getDefaultedParamMethodName(cls, paramModel),
                                parameters.subList(0, parameters.indexOf(paramModel)), 
                                false, 
                                Naming.getDefaultedParamMethodName(cls, paramModel));
                        cbForDevaultValues.method(mdb);
                    }
                }
                boolean addOverloadedConstructor = false;
                if (generateInstantiator) {
                    if (Decl.withinInterface(cls)) {
                        MethodDefinitionBuilder instBuilder = MethodDefinitionBuilder.systemMethod(this, naming.getInstantiatorMethodName(cls));
                        new DefaultedArgumentInstantiator(daoAbstract, model).makeOverload(instBuilder,
                                paramList.getModel(),
                                param.getParameterModel(),
                                typeParameterListModel(typeParameterList));
                        instantiatorDeclCb.method(instBuilder);
                    }
                    if (!Decl.withinInterface(cls) || !cls.isFormal()) {
                        MethodDefinitionBuilder instBuilder = MethodDefinitionBuilder.systemMethod(this, naming.getInstantiatorMethodName(cls));
                        new DefaultedArgumentInstantiator(daoThis, model).makeOverload(instBuilder,
                                paramList.getModel(),
                                param.getParameterModel(),
                                typeParameterListModel(typeParameterList));
                        instantiatorImplCb.method(instBuilder);
                    } else {
                        addOverloadedConstructor  = true;
                    }
                } else {
                    addOverloadedConstructor = true;
                }
                if (addOverloadedConstructor) {
                    // Add overloaded constructors for defaulted parameter
                    MethodDefinitionBuilder overloadBuilder = classBuilder.addConstructor();
                    new DefaultedArgumentConstructor(daoThis, model).makeOverload(
                            overloadBuilder,
                            paramList.getModel(),
                            param.getParameterModel(),
                            typeParameterListModel(typeParameterList));
                }
            }
        }
        satisfaction(def.getSatisfiedTypes(), model, classBuilder);
        serialization(model, classBuilder);
        at(def);
        
        // Generate the inner members list for model loading
        addAtMembers(classBuilder, model, def);
        addAtLocalDeclarations(classBuilder, def);
        
        // Make sure top types satisfy reified type
        addReifiedTypeInterface(classBuilder, model);
    }

    /**
     * Add extra constructor and methods required for serialization
     */
    private void serialization(Class model, ClassDefinitionBuilder classBuilder) {
        if (!model.isSerializable()) {
            return;
        }
        at(null);
        classBuilder.serializable();
        serializationConstructor(model, classBuilder);
        serializationSerialize(model, classBuilder);
        serializationDeserialize(model, classBuilder);
    }
    
    private boolean hasField(Declaration member) {
        if (member instanceof Value) {
            Value value = (Value)member;
            if (!value.isTransient()
                    && !value.isFormal()
                    && (value.isShared() || value.isCaptured())) {
                return true;
            }
        }
        return false;
    }
    
    /** 
     * <p>Generates the serialization constructor 
     * with signature {@code ($Serialization$)} which:</p>
     * <ul>
     * <li>invokes {@code super()}, if the super class is also 
     *     serializable,</li>
     * <li>initializes all companion instance fields to a 
     *     newly instantiated companion instance,</li>
     * <li>initializes all reified type argument fields to null,</li>
     * <li>initializes all reference attribute fields to null,</li>
     * <li>initializesall primitive attribute fields to a default 
     *     value (basically some kind of 0)</li>
     * </ul>
     */
    private void serializationConstructor(Class model, ClassDefinitionBuilder classBuilder) {
        MethodDefinitionBuilder ctor = classBuilder.addConstructor();
        ctor.ignoreModelAnnotations();
        ctor.modifiers(PUBLIC);
        ParameterDefinitionBuilder serializationPdb = ParameterDefinitionBuilder.systemParameter(this, "ignored");
        serializationPdb.modifiers(FINAL);
        serializationPdb.type(make().Type(syms().ceylonSerializationType), null);
        ctor.parameter(serializationPdb);
        
        for (TypeParameter tp : model.getTypeParameters()) {
            ctor.reifiedTypeParameter(tp);
        }
        
        final ListBuffer<JCStatement> stmts = ListBuffer.lb();

        if (extendsSerializable(model)) {
            // invoke super
            ListBuffer<JCExpression> superArgs = ListBuffer.<JCExpression>lb();
            superArgs.add(naming.makeUnquotedIdent("ignored"));
            for (JCExpression ta : makeReifiedTypeArguments(model.getExtendedType())) {
                superArgs.add(ta);
            }
            stmts.add(make().Exec(make().Apply(null,
                    naming.makeSuper(),
                    superArgs.toList())));
        }

        // initialize reified type arguments to according to parameters
        for (TypeParameter tp : model.getTypeParameters()) {
            stmts.add(makeReifiedTypeParameterAssignment(tp));
        }
        
        // initialize companion instances to a new companion instance
        if (!model.getSatisfiedTypeDeclarations().isEmpty()) {
            SatisfactionVisitor visitor = new SatisfactionVisitor() {
                @Override
                public void visit(Class model, Interface iface) {
                    if (hasImpl(iface)) {
                        stmts.add(makeCompanionInstanceAssignment(model, iface, model.getType().getSupertype(iface)));
                    }
                    
                }
            };
            HashSet<Interface> satisfiedInterfaces = new HashSet<Interface>();
            for (ProducedType satisfiedType : model.getSatisfiedTypes()) {
                TypeDeclaration decl = satisfiedType.getDeclaration();
                if (!(decl instanceof Interface)) {
                    continue;
                }
                // make sure we get the right instantiation of the interface
                satisfiedType = model.getType().getSupertype(decl);
                walkSatisfiedInterfaces(model, visitor, satisfiedType, satisfiedInterfaces);
            }
        }
        
        // initialize attribute fields to null or a zero
        for (Declaration member : model.getMembers()) {
            if (hasField(member)) {
                Value value = (Value)member;
                if (value.isLate()) {
                    continue;
                }
                // initialize all reference fields to null and all primitive 
                // fields to a default value.
                JCExpression nullOrZero;
                if (CodegenUtil.isUnBoxed(value)) {
                    Object literal;
                    ProducedType type = value.getType();
                    if (isCeylonBoolean(type)) {
                        literal = false;
                    } else if (isCeylonByte(type)) {
                        literal = (byte)0;
                    } else if (isCeylonInteger(type)) {
                        literal = 0L;
                    } else if (isCeylonCharacter(type)) {
                        literal = 0;
                    } else if (isCeylonFloat(type)) {
                        literal = 0.0;
                    } else if (isCeylonString(type)) {
                        literal = "";
                    } else {
                        throw BugException.unhandledCase(type);
                    }
                    nullOrZero = make().Literal(literal);
                } else {
                    nullOrZero = makeNull();
                }
                stmts.add(make().Exec(make().Assign(
                        naming.makeQualifiedName(naming.makeThis(), value, Naming.NA_IDENT), 
                        nullOrZero)));
            }
        }
        ctor.body(stmts.toList());
    }
    
    static interface SatisfactionVisitor {
        void visit(Class model, Interface iface);
    }
    
    private void walkSatisfiedInterfaces(final Class model,
            SatisfactionVisitor visitor, 
            ProducedType satisfiedType, Set<Interface> satisfiedInterfaces) {
        satisfiedType = satisfiedType.resolveAliases();
        Interface iface = (Interface)satisfiedType.getDeclaration();
        
        if (satisfiedInterfaces.contains(iface)
                || iface.getType().isExactly(typeFact().getIdentifiableDeclaration().getType())) {
            return;
        }
        
        visitor.visit(model, iface);
        
        satisfiedInterfaces.add(iface);
        // recurse up the hierarchy
        for (ProducedType sat : iface.getSatisfiedTypes()) {
            sat = model.getType().getSupertype(sat.getDeclaration());
            walkSatisfiedInterfaces(model, visitor, sat, satisfiedInterfaces);
        }
    }

    private boolean extendsSerializable(Class model) {
        return !typeFact().getObjectDeclaration().getType().isExactly(model.getExtendedType())
                && !typeFact().getBasicDeclaration().getType().isExactly(model.getExtendedType());
    }
    
    /**
     * <p>Generates the {@code $serialize$()} method to serialize the classes state
     * which:</p>
     * <ul>
     * <li>invokes {@code super.$serialize$()}, if the super class is also 
     *     serializable,</li>
     * <li>invokes {@code dtor.putTypeArgument()} for each type argument in the 
     *     class,</li>
     * <li>invokes {@code dtor.putValue()} for each attribute in the class 
     *     whose state is held in a field.</li>
     * </ul>
     */
    private void serializationSerialize(Class model,
            ClassDefinitionBuilder classBuilder) {
        MethodDefinitionBuilder mdb = MethodDefinitionBuilder.systemMethod(this, Unfix.$serialize$.toString());
        mdb.isOverride(true);
        mdb.ignoreModelAnnotations();
        mdb.modifiers(PUBLIC);
        ParameterDefinitionBuilder pdb = ParameterDefinitionBuilder.systemParameter(this, "dtorFactory");
        pdb.modifiers(FINAL);
        pdb.type(makeJavaType(
                
                typeFact().getCallableType(
                        
                    typeFact().getDeconstructorType())), null);
        mdb.parameter(pdb);
        
        ListBuffer<JCStatement> stmts = ListBuffer.lb();
        if (extendsSerializable(model)) {
            // invoke super.$serialize$()
            stmts.add(make().Exec(make().Apply(null,
                    naming.makeQualIdent(naming.makeSuper(), Unfix.$serialize$.toString()),
                    List.<JCExpression>of(naming.makeUnquotedIdent("dtorFactory")))));
        }
        
        stmts.add(makeVar(FINAL, Unfix.deconstructor.toString(), 
                makeJavaType(typeFact().getDeconstructorType()),
                make().Apply(null,
                        naming.makeQualIdent(naming.makeUnquotedIdent("dtorFactory"), 
                                naming.getCallableMethodName()),
                        List.<JCExpression>of(expressionGen().makeTypeLiteralCall(model.getType(), false, model.getType())))));
        
        
        
        // get reified type arguments
        for (TypeParameter tp : model.getTypeParameters()) {
            JCExpression reifiedType = makeMetamodelInvocation("getAppliedMetamodel", 
                    List.<JCExpression>of(naming.makeUnquotedIdent(naming.getTypeArgumentDescriptorName(tp))), 
                    null);
            stmts.add(make().Exec(make().Apply(
                    null, 
                    naming.makeQualIdent(naming.makeUnquotedIdent(Unfix.deconstructor.toString()), "putTypeArgument"),
                    List.<JCExpression>of(
                            expressionGen().makeTypeParameterDeclaration(null, tp),
                            reifiedType))));
        }
        
        // Get the outer instance, if any
        if (model.getContainer() instanceof ClassOrInterface) {
            ClassOrInterface outerInstanceModel = (ClassOrInterface)model.getContainer();
            ProducedType outerInstanceType = outerInstanceModel.getType();
            stmts.add(make().Exec(make().Apply(
                    List.of(makeJavaType(outerInstanceType, JT_TYPE_ARGUMENT)), 
                    naming.makeQualIdent(naming.makeUnquotedIdent(Unfix.deconstructor.toString()), "putOuterInstance"),
                    List.of(makeReifiedTypeArgument(outerInstanceType),
                            expressionGen().makeOuterExpr(outerInstanceType)))));
        }
        
        // get state from fields
        for (Declaration member : model.getMembers()) {
            if (hasField(member)) {
                Value value = (Value)member;
                final ProducedType serializedValueType;
                JCExpression serializedValue;
                //if (Decl.isValueTypeDecl(simplifyType(value.getType()))) {
                serializedValueType = value.getType();
                if (value.isToplevel() || value.isLate()) {
                    serializedValue = make().Apply(null, naming.makeQualifiedName(naming.makeThis(), value, Naming.NA_MEMBER), List.<JCExpression>nil());
                } else {
                    serializedValue = naming.makeQualifiedName(naming.makeThis(), value, Naming.NA_IDENT);
                }
                serializedValue = expressionGen().applyErasureAndBoxing(serializedValue, 
                        value.getType(), 
                        !CodegenUtil.isUnBoxed(value), 
                        BoxingStrategy.BOXED, 
                        value.getType());
                stmts.add(make().Exec(make().Apply(
                        List.of(makeJavaType(serializedValueType, JT_TYPE_ARGUMENT)), 
                        naming.makeQualIdent(naming.makeUnquotedIdent(Unfix.deconstructor.toString()), "putValue"),
                        List.of(makeReifiedTypeArgument(serializedValueType),
                                makeValueDeclaration(value),
                                serializedValue))));
            }
        }
        
        mdb.body(stmts.toList());
        classBuilder.method(mdb);
    }
    /**
     * <p>Generates the {@code $deserialize$()} method to deserialize 
     * the classes state, which:</p>
     * <ul>
     * <li>invokes {@code super.$deserialize$()}, if the super class is also 
     *     serializable,</li>
     * <li>assigns each reified type argument in the 
     *     class by invoking {@code dted.getTypeArgument()},</li>
     * <li>assigns each field in the 
     *     class by invoking {@code dted.getValue()}.</li>
     * </ul>
     */
    private void serializationDeserialize(Class model,
            ClassDefinitionBuilder classBuilder) {
        MethodDefinitionBuilder mdb = MethodDefinitionBuilder.systemMethod(this, Unfix.$deserialize$.toString());
        mdb.isOverride(true);
        mdb.ignoreModelAnnotations();
        mdb.modifiers(PUBLIC);
        ParameterDefinitionBuilder pdb = ParameterDefinitionBuilder.systemParameter(this, Unfix.deconstructed.toString());
        pdb.modifiers(FINAL);
        pdb.type(make().Type(syms().ceylonDeconstructedType), null);
        mdb.parameter(pdb);
        
        ListBuffer<JCStatement> stmts = ListBuffer.lb();
        boolean requiredLookup = false;
        
        // assign reified type arguments
        /*for (TypeParameter tp : model.getTypeParameters()) {
            requiredLookup = true;
            String descriptorName = naming.getTypeArgumentDescriptorName(tp);
            // Get the c.l.m.m.Type
            JCExpression reifiedType = make().Apply(null, 
                    naming.makeQualIdent(naming.makeUnquotedIdent(Unfix.deconstructed.toString()), "getTypeArgument"),
                    List.of(expressionGen().makeTypeParameterDeclaration(null, tp)));
            // Now convert it to a TypeDescriptor, which is what we actually need
            JCExpression reifiedTypeDescriptor = makeMetamodelInvocation("getTypeDescriptor", List.of(reifiedType), null); 
            stmts.add(makeReassignFinalField(makeTypeDescriptorType(), descriptorName, reifiedTypeDescriptor));
        }*/
        
        // assign fields
        for (Declaration member : model.getMembers()) {
            if (hasField(member)) {
                requiredLookup |= makeDeserializationAssignment(stmts, (Value)member);
            }
        }
        if (requiredLookup) {
            // if we needed to use a lookup object to reset final fields, 
            // prepend that variable
            stmts.prepend(makeVar(FINAL, 
                    "lookup", 
                    naming.makeQualIdent(make().Type(syms().methodHandlesType), "Lookup"), 
                    make().Apply(null, naming.makeQuotedFQIdent("java.lang.invoke.MethodHandles.lookup"), List.<JCExpression>nil())));
        }
        
        if (extendsSerializable(model)) {
            // prepend the invocation of super.$serialize$()
            stmts.prepend(make().Exec(make().Apply(null,
                    naming.makeQualIdent(naming.makeSuper(), Unfix.$deserialize$.toString()),
                    List.<JCExpression>of(naming.makeUnquotedIdent(Unfix.deconstructed.toString())))));
        }
        mdb.body(stmts.toList());
        classBuilder.method(mdb);
    }
    
    private boolean makeDeserializationAssignment(ListBuffer<JCStatement> stmts, Value value) {
        ProducedType typeOrReferenceType;
        boolean isValueType = Decl.isValueTypeDecl(simplifyType(value.getType()));
        //if (isValueType) {
        typeOrReferenceType = value.getType();
        //} else {
          //  typeOrReferenceType = typeFact().getReferenceType(value.getType());
        //}
            
        Naming.SyntheticName n = naming.alias("valueOrRef");
        JCExpression newValue = make().Apply(List.of(makeJavaType(typeOrReferenceType, JT_TYPE_ARGUMENT)),
                naming.makeQualIdent(naming.makeUnquotedIdent(Unfix.deconstructed.toString()), "getValue"),
                List.<JCExpression>of(
                        makeReifiedTypeArgument(typeOrReferenceType),
                        makeValueDeclaration(value)));
        // let ( 
        // Object valueOrRef = ^^;
        // valueOrRef instanceof Reference ? (($InstanceLeaker$)valueOrRef).$leakInstance$() : (Type)valueOrRef;
        // )
        newValue = make().LetExpr(makeVar(0, n, make().Type(syms().objectType), newValue), 
                make().Conditional(
                        make().TypeTest(n.makeIdent(),
                                makeJavaType(typeFact().getReferenceType(value.getType()), JT_RAW)), 
                        make().Apply(null,
                                naming.makeQualIdent(make().TypeCast(
                                        make().TypeApply(make().QualIdent(syms().ceylonInstanceLeakerType.tsym), List.of(makeJavaType(value.getType(), JT_TYPE_ARGUMENT))), n.makeIdent()), "$leakInstance$"),
                                List.<JCExpression>nil()), 
                        make().TypeCast(makeJavaType(typeOrReferenceType, JT_NO_PRIMITIVES), n.makeIdent())));

        if (isValueType) {
            newValue = expressionGen().applyErasureAndBoxing(newValue, 
                    value.getType(), true, CodegenUtil.getBoxingStrategy(value), 
                    value.getType());
        } else {
            // We need to obtain the instance from the reference
            // but we don't need the instance to be fully deserialized
        }
        boolean requiredLookup = false;
        final JCStatement assignment;
        if (value.isToplevel() || value.isLate()) {// XXX duplicates logic in AttributeDefinitionBuilder
            // We use the setter for late values, since that will allocate 
            // the array if needed.
            assignment = make().Exec(make().Apply(null,
                    naming.makeQualifiedName(naming.makeThis(), value, Naming.NA_MEMBER | Naming.NA_SETTER),
                    List.of(newValue)));
        } else {
            // We bypass the setter
            if (value.isVariable()) {
                assignment = make().Exec(make().Assign(
                        naming.makeQualifiedName(naming.makeThis(), value, Naming.NA_IDENT), 
                        newValue));
            } else {
                // The field will have final modifier, so we need some 
                // jiggery pokery to reset it.
                requiredLookup = true;
                String fieldName = value.getName();
                JCExpression fieldType = makeJavaType(value.getType());//TODO probably wrong
                assignment = makeReassignFinalField(fieldType, fieldName, newValue);
            }
        }
        stmts.add(assignment);
        return requiredLookup;
    }

    private JCStatement makeReassignFinalField(JCExpression fieldType,
            String fieldName, JCExpression newValue) {
        final JCStatement assignment;
        JCExpression mhExpr = utilInvocation().setter(
                naming.makeUnquotedIdent("lookup"),
                //naming.makeQualIdent(makeJavaType(((Class)value.getContainer()).getType(), JT_NO_PRIMITIVES), "class"),
                make().Literal(fieldName)// TODO field name should encapsulated
                );
        
        JCExpression expr = make().Apply(null, 
                naming.makeQualIdent(mhExpr, "invokeExact"), 
                List.of(naming.makeThis(), 
                        make().TypeCast(fieldType, newValue)));// We always typecast here, due to method handle
        assignment = make().Exec(expr);
        return assignment;
    }
    
    private JCExpression makeValueDeclaration(Value value) {
        return expressionGen().makeMemberValueOrFunctionDeclarationLiteral(null, value, false);
    }
    
    /**
     * Generate a method for a shared FunctionalParameter which delegates to the Callable 
     * @param klass 
     * @param annotations */
    private void makeMethodForFunctionalParameter(
            ClassDefinitionBuilder classBuilder, AnyClass klass, Tree.Parameter paramTree, List<JCAnnotation> annotations) {
        Parameter paramModel = paramTree.getParameterModel();

        if (Strategy.createMethod(paramModel)) {
            Tree.MethodDeclaration methodDecl = (Tree.MethodDeclaration)Decl.getMemberDeclaration(klass, paramTree);
            makeFieldForParameter(classBuilder, paramModel);
            Method method = (Method)paramModel.getModel();

            java.util.List<Parameter> parameters = method.getParameterLists().get(0).getParameters();
            CallBuilder callBuilder = CallBuilder.instance(this).invoke(
                    naming.makeQualIdent(naming.makeName(method, Naming.NA_IDENT), 
                            Naming.getCallableMethodName(method)));
            for (Parameter parameter : parameters) {
                JCExpression parameterExpr = naming.makeName(parameter.getModel(), Naming.NA_IDENT);
                parameterExpr = expressionGen().applyErasureAndBoxing(parameterExpr, parameter.getType(), 
                        !CodegenUtil.isUnBoxed(parameter.getModel()), BoxingStrategy.BOXED, 
                        parameter.getType());
                callBuilder.argument(parameterExpr);
            }
            JCExpression expr = callBuilder.build();
            JCStatement body;
            if (isVoid(methodDecl) && Decl.isUnboxedVoid(method) && !Strategy.useBoxedVoid(method)) {
                body = make().Exec(expr);
            } else {
                expr = expressionGen().applyErasureAndBoxing(expr, paramModel.getType(), true, CodegenUtil.getBoxingStrategy(method), paramModel.getType());
                body = make().Return(expr);
            }
            classBuilder.methods(transformMethod(method, null, methodDecl.getParameterLists(),
                    methodDecl.getAnnotationList(),
                    true, method.isActual(), true, 
                    List.of(body), daoThis, false));
        }
    }
    
    private void addReifiedTypeInterface(ClassDefinitionBuilder classBuilder, ClassOrInterface model) {
        if(model.getExtendedType() == null || willEraseToObject(model.getExtendedType()) || !Decl.isCeylon(model.getExtendedTypeDeclaration()))
            classBuilder.reifiedType();
    }

    /**
     * Transforms the type of the given class parameter
     * @param decl
     * @return
     */
    JCExpression transformClassParameterType(Parameter parameter) {
        MethodOrValue decl = parameter.getModel();
        if (!(decl.getContainer() instanceof Class)) {
            throw new BugException("expected parameter of Class");
        }
        JCExpression type;
        MethodOrValue attr = decl;
        if (!Decl.isTransient(attr)) {
            ProducedTypedReference typedRef = getTypedReference(attr);
            ProducedTypedReference nonWideningTypedRef = nonWideningTypeDecl(typedRef);
            ProducedType paramType = nonWideningType(typedRef, nonWideningTypedRef);
            type = makeJavaType(nonWideningTypedRef.getDeclaration(), paramType, 0);
        } else {
            ProducedType paramType = decl.getType();
            type = makeJavaType(decl, paramType, 0);
        }
        return type;
    }

    private void transformInterface(com.redhat.ceylon.compiler.typechecker.tree.Tree.ClassOrInterface def, Interface model, ClassDefinitionBuilder classBuilder, TypeParameterList typeParameterList) {
        //  Copy all the qualifying type's type parameters into the interface
        java.util.List<TypeParameter> typeParameters = typeParametersOfAllContainers(model, false);
        for(TypeParameter tp : typeParameters){
            classBuilder.typeParameter(tp, false);
        }
        
        if(model.isCompanionClassNeeded()){
            classBuilder.method(makeCompanionAccessor(model, model.getType(), null, false));
            // Build the companion class
            buildCompanion(def, (Interface)model, classBuilder, typeParameterList);
        }
        
        // Generate the inner members list for model loading
        addAtMembers(classBuilder, model, def);
        addAtLocalDeclarations(classBuilder, def);
    }

    private void addAtMembers(ClassDefinitionBuilder classBuilder, ClassOrInterface model, com.redhat.ceylon.compiler.typechecker.tree.Tree.ClassOrInterface def) {
        List<JCExpression> members = List.nil();
        for(Declaration member : model.getMembers()){
            if(member instanceof ClassOrInterface == false
                    && member instanceof TypeAlias == false){
                continue;
            }
            TypeDeclaration innerType = (TypeDeclaration) member;
            Tree.Declaration innerTypeTree = findInnerType(def, innerType.getName());
            if(innerTypeTree != null && errors().hasDeclarationError(innerTypeTree))
                continue;
            JCAnnotation atMember = makeAtMember(innerType.getType());
            members = members.prepend(atMember);
        }
        classBuilder.annotations(makeAtMembers(members));
    }

    private Tree.Declaration findInnerType(Tree.ClassOrInterface def, String name) {
        Tree.Body body;
        if(def instanceof Tree.ClassDefinition)
            body = ((Tree.ClassDefinition) def).getClassBody();
        else if(def instanceof Tree.InterfaceDefinition)
            body = ((Tree.InterfaceDefinition) def).getInterfaceBody();
        else
            return null;
        for(Node node : body.getStatements()){
            if(node instanceof Tree.Declaration
                    && ((Tree.Declaration) node).getIdentifier() != null
                    && ((Tree.Declaration) node).getIdentifier().getText().equals(name))
                return (Tree.Declaration) node;
        }
        return null;
    }

    private void addAtLocalDeclarations(ClassDefinitionBuilder classBuilder, Tree.ClassOrInterface tree) {
        classBuilder.annotations(makeAtLocalDeclarations(tree));
    }

    private void addAtContainer(ClassDefinitionBuilder classBuilder, TypeDeclaration model) {
        Scope scope = model.getContainer();
        if(scope == null || scope instanceof Package)
            return;
        if(scope instanceof ClassOrInterface){
            ClassOrInterface container = (ClassOrInterface) scope;
            List<JCAnnotation> atContainer = makeAtContainer(container.getType());
            classBuilder.annotations(atContainer);
        }else{
            if(model instanceof Interface)
                classBuilder.annotations(makeLocalContainerPath((Interface) model));
            classBuilder.annotations(makeAtLocalDeclaration(model.getQualifier()));
        }
        
    }

    private void satisfaction(Tree.SatisfiedTypes satisfied, final Class model, ClassDefinitionBuilder classBuilder) {
        final java.util.List<ProducedType> satisfiedTypes = model.getSatisfiedTypes();
        Set<Interface> satisfiedInterfaces = new HashSet<Interface>();
        // start by saying that we already satisfied each interface from superclasses
        Class superClass = model.getExtendedTypeDeclaration();
        while(superClass != null){
            for(TypeDeclaration interfaceDecl : superClass.getSatisfiedTypeDeclarations()){
                collectInterfaces((Interface) interfaceDecl, satisfiedInterfaces);
            }
            superClass = superClass.getExtendedTypeDeclaration();
        }
        // now satisfy each new interface
        if (satisfied != null) {
            for (Tree.StaticType type : satisfied.getTypes()) {
                try {
                    ProducedType satisfiedType = type.getTypeModel();
                    TypeDeclaration decl = satisfiedType.getDeclaration();
                    if (!(decl instanceof Interface)) {
                        continue;
                    }
                    // make sure we get the right instantiation of the interface
                    satisfiedType = model.getType().getSupertype(decl);
                    concreteMembersFromSuperinterfaces(model, classBuilder, satisfiedType, satisfiedInterfaces);
                } catch (BugException e) {
                    e.addError(type);
                }
            }
        }
        // now find the set of interfaces we implemented twice with more refined type parameters
        if(model.getExtendedTypeDeclaration() != null){
            // reuse that Set
            satisfiedInterfaces.clear();
            for(TypeDeclaration interfaceDecl : model.getSatisfiedTypeDeclarations()){
                collectInterfaces((Interface) interfaceDecl, satisfiedInterfaces);
            }
            // now see if we refined them
            for(Interface iface : satisfiedInterfaces){
                // skip those we can't do anything about
                if(!supportsReified(iface) || !CodegenUtil.isCompanionClassNeeded(iface))
                    continue;
                ProducedType thisType = model.getType().getSupertype(iface);
                ProducedType superClassType = model.getExtendedType().getSupertype(iface);
                if(thisType != null
                        && superClassType != null
                        && !thisType.isExactly(superClassType)
                        && thisType.isSubtypeOf(superClassType)){
                    // we're refining it
                    classBuilder.refineReifiedType(thisType);
                }
            }
        }
    }

    private void collectInterfaces(Interface interfaceDecl, Set<Interface> satisfiedInterfaces) {
        if(satisfiedInterfaces.add(interfaceDecl)){
            for(TypeDeclaration newInterfaceDecl : interfaceDecl.getSatisfiedTypeDeclarations()){
                if (!(newInterfaceDecl instanceof UnknownType)) {
                    collectInterfaces((Interface) newInterfaceDecl, satisfiedInterfaces);
                }
            }
        }
    }

    /**
     * Generates companion fields ($Foo$impl) and methods
     */
    private void concreteMembersFromSuperinterfaces(final Class model,
            ClassDefinitionBuilder classBuilder, 
            ProducedType satisfiedType, Set<Interface> satisfiedInterfaces) {
        satisfiedType = satisfiedType.resolveAliases();
        Interface iface = (Interface)satisfiedType.getDeclaration();
        if (satisfiedInterfaces.contains(iface)
                || iface.getType().isExactly(typeFact().getIdentifiableDeclaration().getType())) {
            return;
        }
     
        // If there is no $impl (e.g. implementing a Java interface) 
        // then don't instantiate it...
        if (hasImpl(iface)) {
            // ... otherwise for each satisfied interface, 
            // instantiate an instance of the 
            // companion class in the constructor and assign it to a
            // $Interface$impl field
            transformInstantiateCompanions(classBuilder,
                    model, iface, satisfiedType);
        }
        
        if(!Decl.isCeylon(iface)){
            // let's not try to implement CMI for Java interfaces
            return;
        }
        
        // For each super interface
        for (Declaration member : iface.getMembers()) {
            
            if (member instanceof Class
                    && Strategy.generateInstantiator(member)
                    && model.getDirectMember(member.getName(), null, false) == null) {
                // instantiator method implementation
                Class klass = (Class)member;
                generateInstantiatorDelegate(classBuilder, satisfiedType,
                        iface, klass, model.getType());
            } 
            
            // type aliases are on the $impl class
            if(member instanceof TypeAlias)
                continue;
            
            if (Strategy.onlyOnCompanion(member)) {
                // non-shared interface methods don't need implementing
                // (they're just private methods on the $impl)
                continue;
            }
            if (member instanceof Method) {
                Method method = (Method)member;
                final ProducedTypedReference typedMember = satisfiedType.getTypedMember(method, Collections.<ProducedType>emptyList());
                Declaration sub = (Declaration)model.getMember(method.getName(), null, false);
                if (sub instanceof Method) {
                    Method subMethod = (Method)sub;
                    java.util.List<java.util.List<ProducedType>> producedTypeParameterBounds = producedTypeParameterBounds(
                            typedMember, subMethod);
                    final ProducedTypedReference refinedTypedMember = model.getType().getTypedMember(subMethod, Collections.<ProducedType>emptyList());
                    final java.util.List<TypeParameter> typeParameters = subMethod.getTypeParameters();
                    final java.util.List<Parameter> parameters = subMethod.getParameterLists().get(0).getParameters();
                    boolean hasOverloads = false;
                    if (!satisfiedInterfaces.contains((Interface)method.getContainer())) {

                        for (Parameter param : parameters) {
                            if (Strategy.hasDefaultParameterValueMethod(param)
                                    && CodegenUtil.getTopmostRefinedDeclaration(param.getModel()).getContainer().equals(member)) {
                                final ProducedTypedReference typedParameter = refinedTypedMember.getTypedParameter(param);
                                // If that method has a defaulted parameter, 
                                // we need to generate a default value method
                                // which also delegates to the $impl
                                final MethodDefinitionBuilder defaultValueDelegate = makeDelegateToCompanion(iface,
                                        typedParameter,
                                        model.getType(),
                                        PUBLIC | FINAL, 
                                        typeParameters, producedTypeParameterBounds,
                                        typedParameter.getType(), 
                                        Naming.getDefaultedParamMethodName(method, param), 
                                        parameters.subList(0, parameters.indexOf(param)),
                                        param.getModel().getTypeErased(),
                                        null);
                                classBuilder.method(defaultValueDelegate);
                            }

                            if (Strategy.hasDefaultParameterOverload(param)) {
                                if ((method.isDefault() || method.isShared() && !method.isFormal())
                                        && (method == subMethod)) {
                                    MethodDefinitionBuilder overloadBuilder = MethodDefinitionBuilder.method(this, subMethod);
                                    MethodDefinitionBuilder overload = new DefaultedArgumentMethodTyped(daoThis, typedMember)
                                        .makeOverload(
                                            overloadBuilder, 
                                            subMethod.getParameterLists().get(0),
                                            param,
                                            typeParameters);
                                    classBuilder.method(overload);
                                }

                                hasOverloads = true;
                            }
                        }
                    }
                    // if it has the *most refined* default concrete member, 
                    // then generate a method on the class
                    // delegating to the $impl instance
                    if (needsCompanionDelegate(model, member)) {

                        final MethodDefinitionBuilder concreteMemberDelegate = makeDelegateToCompanion(iface,
                                typedMember,
                                model.getType(),
                                PUBLIC | (method.isDefault() ? 0 : FINAL),
                                typeParameters, 
                                producedTypeParameterBounds, 
                                typedMember.getType(), 
                                naming.selector(method), 
                                method.getParameterLists().get(0).getParameters(),
                                ((Method) member).getTypeErased(),
                                null);
                        classBuilder.method(concreteMemberDelegate);
                    }

                    if (hasOverloads
                            && (method.isDefault() || method.isShared() && !method.isFormal())
                            && (method == subMethod)) {
                        final MethodDefinitionBuilder canonicalMethod = makeDelegateToCompanion(iface,
                                typedMember,
                                model.getType(),
                                PRIVATE,
                                subMethod.getTypeParameters(), 
                                producedTypeParameterBounds,
                                typedMember.getType(), 
                                Naming.selector(method, Naming.NA_CANONICAL_METHOD), 
                                method.getParameterLists().get(0).getParameters(),
                                ((Method) member).getTypeErased(),
                                naming.selector(method));
                        classBuilder.method(canonicalMethod);
                    }
                }
            } else if (member instanceof Value
                    || member instanceof Setter) {
                TypedDeclaration attr = (TypedDeclaration)member;
                final ProducedTypedReference typedMember = satisfiedType.getTypedMember(attr, null);
                if (needsCompanionDelegate(model, member)) {
                    if (member instanceof Value) {
                        final MethodDefinitionBuilder getterDelegate = makeDelegateToCompanion(iface, 
                                typedMember,
                                model.getType(),
                                PUBLIC | (attr.isDefault() ? 0 : FINAL), 
                                Collections.<TypeParameter>emptyList(), 
                                Collections.<java.util.List<ProducedType>>emptyList(),
                                typedMember.getType(), 
                                Naming.getGetterName(attr), 
                                Collections.<Parameter>emptyList(),
                                attr.getTypeErased(),
                                null);
                        classBuilder.method(getterDelegate);
                    }
                    if (member instanceof Setter) { 
                        final MethodDefinitionBuilder setterDelegate = makeDelegateToCompanion(iface, 
                                typedMember,
                                model.getType(),
                                PUBLIC | (((Setter)member).getGetter().isDefault() ? 0 : FINAL), 
                                Collections.<TypeParameter>emptyList(), 
                                Collections.<java.util.List<ProducedType>>emptyList(),
                                typeFact().getAnythingDeclaration().getType(), 
                                Naming.getSetterName(attr), 
                                Collections.<Parameter>singletonList(((Setter)member).getParameter()),
                                ((Setter) member).getTypeErased(),
                                null);
                        classBuilder.method(setterDelegate);
                    }
                    if (Decl.isValue(member) 
                            && ((Value)attr).isVariable()) {
                        // I don't *think* this can happen because although a 
                        // variable Value can be declared on an interface it 
                        // will need to we refined as a Getter+Setter on a 
                        // subinterface in order for there to be a method in a 
                        // $impl to delegate to
                        throw new BugException("assertion failed: " + member.getQualifiedNameString() + " was unexpectedly a variable value");
                    }
                }
            } else if (needsCompanionDelegate(model, member)) {
                throw new BugException("unhandled concrete interface member " + member.getQualifiedNameString() + " " + member.getClass());
            }
        }
        
        // Add $impl instances for the whole interface hierarchy
        satisfiedInterfaces.add(iface);
        for (ProducedType sat : iface.getSatisfiedTypes()) {
            sat = model.getType().getSupertype(sat.getDeclaration());
            concreteMembersFromSuperinterfaces(model, classBuilder, sat, satisfiedInterfaces);
        }
        
    }

    private java.util.List<java.util.List<ProducedType>> producedTypeParameterBounds(
            final ProducedReference typedMember, Generic subMethod) {
        java.util.List<java.util.List<ProducedType>> producedTypeParameterBounds = new ArrayList<java.util.List<ProducedType>>(subMethod.getTypeParameters().size());
        for (TypeParameter tp : subMethod.getTypeParameters()) {
            java.util.List<ProducedType> satisfiedTypes = tp.getType().getSatisfiedTypes();
            ArrayList<ProducedType> bounds = new ArrayList<>(satisfiedTypes.size());
            for (ProducedType bound : satisfiedTypes) {
                bounds.add(bound.getType().substitute(typedMember.getTypeArguments()));
            }
            producedTypeParameterBounds.add(bounds);
        }
        return producedTypeParameterBounds;
    }

    private void generateInstantiatorDelegate(
            ClassDefinitionBuilder classBuilder, ProducedType satisfiedType,
            Interface iface, Class klass, ProducedType currentType) {
        ProducedType typeMember = satisfiedType.getTypeMember(klass, Collections.<ProducedType>emptyList());
        java.util.List<TypeParameter> typeParameters = klass.getTypeParameters();
        java.util.List<Parameter> parameters = klass.getParameterLists().get(0).getParameters();
        
        String instantiatorMethodName = naming.getInstantiatorMethodName(klass);
        for (Parameter param : parameters) {
            if (Strategy.hasDefaultParameterValueMethod(param)) {
                final ProducedTypedReference typedParameter = typeMember.getTypedParameter(param);
                // If that method has a defaulted parameter, 
                // we need to generate a default value method
                // which also delegates to the $impl
                final MethodDefinitionBuilder defaultValueDelegate = makeDelegateToCompanion(iface,
                        typedParameter,
                        currentType,
                        PUBLIC | FINAL, 
                        typeParameters, 
                        producedTypeParameterBounds(typeMember, klass),
                        typedParameter.getType(),
                        Naming.getDefaultedParamMethodName(klass, param), 
                        parameters.subList(0, parameters.indexOf(param)),
                        param.getModel().getTypeErased(),
                        null);
                classBuilder.method(defaultValueDelegate);
            }
            if (Strategy.hasDefaultParameterOverload(param)) {
                final MethodDefinitionBuilder overload = makeDelegateToCompanion(iface,
                        typeMember,
                        currentType,
                        PUBLIC | FINAL, 
                        typeParameters, 
                        producedTypeParameterBounds(typeMember, klass),
                        typeMember.getType(), 
                        instantiatorMethodName, 
                        parameters.subList(0, parameters.indexOf(param)),
                        false,
                        null);
                classBuilder.method(overload);
            }
        }
        final MethodDefinitionBuilder overload = makeDelegateToCompanion(iface,
                typeMember,
                currentType,
                PUBLIC | FINAL, 
                typeParameters, 
                producedTypeParameterBounds(typeMember, klass),
                typeMember.getType(), 
                instantiatorMethodName, 
                parameters,
                false,
                null);
        classBuilder.method(overload);
    }

    private boolean needsCompanionDelegate(final Class model, Declaration member) {
        final boolean mostRefined;
        Declaration m = model.getMember(member.getName(), null, false);
        if (member instanceof Setter && Decl.isGetter(m)) {
            mostRefined = member.equals(((Value)m).getSetter());
        } else {
            mostRefined = member.equals(m);
        }
        return mostRefined
                && (member.isDefault() || !member.isFormal());
    }

    /**
     * Generates a method which delegates to the companion instance $Foo$impl
     */
    private MethodDefinitionBuilder makeDelegateToCompanion(Interface iface,
            ProducedReference typedMember,
            ProducedType currentType,
            final long mods,
            final java.util.List<TypeParameter> typeParameters,
            final java.util.List<java.util.List<ProducedType>> producedTypeParameterBounds,
            final ProducedType methodType,
            final String methodName,
            final java.util.List<Parameter> parameters, 
            boolean typeErased,
            final String targetMethodName) {
        final MethodDefinitionBuilder concreteWrapper = MethodDefinitionBuilder.systemMethod(gen(), methodName);
        concreteWrapper.modifiers(mods);
        concreteWrapper.ignoreModelAnnotations();
        if ((mods & PRIVATE) == 0) {
            concreteWrapper.isOverride(true);
        }
        if(typeParameters != null) {
            concreteWrapper.reifiedTypeParametersFromModel(typeParameters);
        }
        Iterator<java.util.List<ProducedType>> iterator = producedTypeParameterBounds.iterator();
        if(typeParameters != null) {
            for (TypeParameter tp : typeParameters) {
                concreteWrapper.typeParameter(tp, iterator.next());
            }
        }
        boolean explicitReturn = false;
        Declaration member = typedMember.getDeclaration();
        ProducedType returnType = null;
        if (!isAnything(methodType) 
                || ((member instanceof Method || member instanceof Value) && !Decl.isUnboxedVoid(member)) 
                || (member instanceof Method && Strategy.useBoxedVoid((Method)member))) {
            explicitReturn = true;
            if(CodegenUtil.isHashAttribute(member)){
                // delegates for hash attributes are int
                concreteWrapper.resultType(null, make().Type(syms().intType));
                returnType = typedMember.getType();
            }else if (typedMember instanceof ProducedTypedReference) {
                ProducedTypedReference typedRef = (ProducedTypedReference) typedMember;
                // This is very much like for method refinement: if the supertype is erased -> go raw.
                // Except for some reason we only need to do it with multiple inheritance with different type
                // arguments, so let's not go overboard
                int flags = 0;
                if(CodegenUtil.hasTypeErased((TypedDeclaration)member.getRefinedDeclaration()) ||
                        CodegenUtil.hasTypeErased((TypedDeclaration)member)
                        && isInheritedTwiceWithDifferentTypeArguments(currentType, iface)){
                    flags |= AbstractTransformer.JT_RAW;
                }
                concreteWrapper.resultTypeNonWidening(currentType, typedRef, typedMember.getType(), flags);
                // FIXME: this is redundant with what we computed in the previous line in concreteWrapper.resultTypeNonWidening
                ProducedTypedReference nonWideningTypedRef = gen().nonWideningTypeDecl(typedRef, currentType);
                returnType = gen().nonWideningType(typedRef, nonWideningTypedRef);
            } else {
                concreteWrapper.resultType(null, makeJavaType((ProducedType)typedMember));
                returnType = (ProducedType) typedMember;
            }
        }
        
        ListBuffer<JCExpression> arguments = ListBuffer.<JCExpression>lb();
        if(typeParameters != null){
            for(TypeParameter tp : typeParameters){
                arguments.add(naming.makeUnquotedIdent(naming.getTypeArgumentDescriptorName(tp)));
            }
        }
        for (Parameter param : parameters) {
            final ProducedTypedReference typedParameter = typedMember.getTypedParameter(param);
            ProducedType type;
            // if the supertype method itself got erased to Object, we can't do better than this
            if(gen().willEraseToObject(param.getType()) && !gen().willEraseToBestBounds(param))
                type = typeFact().getObjectDeclaration().getType();
            else
                type = typedParameter.getType();
            concreteWrapper.parameter(param, type, FINAL, 0, true);
            arguments.add(naming.makeName(param.getModel(), Naming.NA_MEMBER));
        }
        JCExpression qualifierThis = makeUnquotedIdent(getCompanionFieldName(iface));
        // if the best satisfied type is not the one we think we implement, we may need to cast
        // our impl accessor to get the expected bounds of the qualifying type
        if(explicitReturn){
            ProducedType javaType = getBestSatisfiedType(currentType, iface);
            ProducedType ceylonType = typedMember.getQualifyingType();
            // don't even bother if the impl accessor is turned to raw because casting it to raw doesn't help
            if(!isTurnedToRaw(ceylonType)
                    // if it's exactly the same we don't need any cast
                    && !javaType.isExactly(ceylonType))
                // this will add the proper cast to the impl accessor
                qualifierThis = expressionGen().applyErasureAndBoxing(qualifierThis, currentType, 
                        false, true, BoxingStrategy.BOXED, ceylonType,
                        ExpressionTransformer.EXPR_WANTS_COMPANION);
        }
        JCExpression expr = make().Apply(
                null,  // TODO Type args
                makeSelect(qualifierThis, (targetMethodName != null) ? targetMethodName : methodName),
                arguments.toList());
        
        if (!explicitReturn) {
            concreteWrapper.body(gen().make().Exec(expr));
        } else {
            // deal with erasure and stuff
            BoxingStrategy boxingStrategy;
            boolean exprBoxed;
            if(member instanceof TypedDeclaration){
                TypedDeclaration typedDecl = (TypedDeclaration) member;
                exprBoxed = !CodegenUtil.isUnBoxed(typedDecl);
                boxingStrategy = CodegenUtil.getBoxingStrategy(typedDecl);
            }else{
                // must be a class or interface
                exprBoxed = true;
                boxingStrategy = BoxingStrategy.UNBOXED;
            }
            // if our interface impl is turned to raw, the whole call will be seen as raw by javac, so we may need
            // to force an additional cast
            if(isTurnedToRaw(typedMember.getQualifyingType())
                    // see note in BoxingVisitor.visit(QualifiedMemberExpression) about mixin super calls and variant type args
                    // in invariant locations
                    || needsRawCastForMixinSuperCall(iface, methodType))
                typeErased = true;
            expr = gen().expressionGen().applyErasureAndBoxing(expr, methodType, typeErased, 
                                                               exprBoxed, boxingStrategy,
                                                               returnType, 0);
            concreteWrapper.body(gen().make().Return(expr));
        }
        return concreteWrapper;
    }

    private boolean isInheritedTwiceWithDifferentTypeArguments(ProducedType currentType, Interface iface) {
        ProducedType firstSatisfiedType = getFirstSatisfiedType(currentType, iface);
        ProducedType supertype = currentType.getSupertype(iface);
        return !supertype.isExactly(firstSatisfiedType);
    }

    private Boolean hasImpl(Interface iface) {
        if (gen().willEraseToObject(iface.getType())) {
            return false;
        }
        // Java interfaces never have companion classes
        if (iface instanceof LazyInterface
            && !((LazyInterface)iface).isCeylon()){
            return false;
        }
        return CodegenUtil.isCompanionClassNeeded(iface);
    }

    private void transformInstantiateCompanions(
            ClassDefinitionBuilder classBuilder, 
            Class model, 
            Interface iface,
            ProducedType satisfiedType) {
        at(null);
        
        // make sure we get the first type that java will find when it looks up
        final ProducedType bestSatisfiedType = getBestSatisfiedType(model.getType(), iface);
        
        classBuilder.init(makeCompanionInstanceAssignment(model, iface, satisfiedType));
        
        classBuilder.field(PROTECTED | FINAL, getCompanionFieldName(iface), 
                makeJavaType(bestSatisfiedType, AbstractTransformer.JT_COMPANION | JT_SATISFIES), null, false,
                makeAtIgnore());

        classBuilder.method(makeCompanionAccessor(iface, bestSatisfiedType, model, true));
    }

    /**
     * Returns the companion instances assignment expression used in the constructor,
     * e.g.
     * <pre>
     * this.$ceylon$language$Enumerable$this$ = new .ceylon.language.Enumerable$impl<.com.redhat.ceylon.compiler.java.test.structure.klass.SerializableEnumerable>(.com.redhat.ceylon.compiler.java.test.structure.klass.SerializableEnumerable.$TypeDescriptor$, this);
     * </pre>
     */
    private JCExpressionStatement makeCompanionInstanceAssignment(Class model,
            Interface iface, ProducedType satisfiedType) {
        
        final ProducedType bestSatisfiedType = getBestSatisfiedType(model.getType(), iface);
        
        JCExpression containerInstance = null;
        if(!Decl.isToplevel(iface) && !Decl.isLocal(iface)){
            // if it's a member type we need to qualify the new instance with its $impl container
            ClassOrInterface interfaceContainer = Decl.getClassOrInterfaceContainer(iface, false);
            if(interfaceContainer instanceof Interface){
                ClassOrInterface modelContainer = model;
                while((modelContainer = Decl.getClassOrInterfaceContainer(modelContainer, false)) != null
                        && modelContainer.getType().getSupertype(interfaceContainer) == null){
                    // keep searching
                }
                if (modelContainer == null) {
                    throw new BugException("Could not find container that satisfies interface "
                        + iface.getQualifiedNameString() + " to find qualifying instance for companion instance for "
                        + model.getQualifiedNameString());
                }
                // if it's an interface we just qualify it properly
                if(modelContainer instanceof Interface){
                    JCExpression containerType = makeJavaType(modelContainer.getType(), JT_COMPANION | JT_SATISFIES | JT_RAW);
                    containerInstance = makeSelect(containerType, "this");
                }else{
                    // it's a class: find the right field used for the interface container impl
                    String containerFieldName = getCompanionFieldName((Interface)interfaceContainer);
                    JCExpression containerType = makeJavaType(modelContainer.getType(), JT_SATISFIES);
                    containerInstance = makeSelect(makeSelect(containerType, "this"), containerFieldName);
                }
            }
        }
        
        List<JCExpression> state = List.nil();
        
        // pass all reified type info to the constructor
        for(JCExpression t : makeReifiedTypeArguments(satisfiedType)){
            state = state.append(t);
        }
        // pass the instance of this
        state = state.append( expressionGen().applyErasureAndBoxing(naming.makeThis(), 
                model.getType(), false, true, BoxingStrategy.BOXED, 
                bestSatisfiedType, ExpressionTransformer.EXPR_FOR_COMPANION));
        
        final JCExpression ifaceImplType;
        if(!Decl.isToplevel(iface) && !Decl.isLocal(iface)
                && Decl.getClassOrInterfaceContainer(iface, false) instanceof Interface){
            ifaceImplType = makeJavaType(bestSatisfiedType, JT_COMPANION | JT_SATISFIES | JT_NON_QUALIFIED);
        } else {
            ifaceImplType = makeJavaType(bestSatisfiedType, JT_COMPANION | JT_SATISFIES);
        }
        
        JCExpression newInstance = make().NewClass(containerInstance, 
                null,
                ifaceImplType,
                state,
                null);
        
        JCExpressionStatement companionInstanceAssign = make().Exec(make().Assign(
                makeSelect("this", getCompanionFieldName(iface)),// TODO Use qualified name for quoting? 
                newInstance));
        return companionInstanceAssign;
    }
    
    private MethodDefinitionBuilder makeCompanionAccessor(Interface iface, ProducedType satisfiedType, 
            Class currentType, boolean forImplementor) {
        MethodDefinitionBuilder thisMethod = MethodDefinitionBuilder.systemMethod(
                this, naming.getCompanionAccessorName(iface));
        thisMethod.noModelAnnotations();
        if (!forImplementor && Decl.isAncestorLocal(iface)) {
            // For a local interface the return type cannot be a local
            // companion class, because that won't be visible at the 
            // top level, so use Object instead
            thisMethod.resultType(null, make().Type(syms().objectType));
        } else {
            thisMethod.resultType(null, makeJavaType(satisfiedType, JT_COMPANION));
        }
        if (forImplementor) {
            thisMethod.isOverride(true);
        } else {
            thisMethod.ignoreModelAnnotations();
        }
        thisMethod.modifiers(PUBLIC);
        if (forImplementor) {
            thisMethod.body(make().Return(naming.makeCompanionFieldName(iface)));
        } else {
            thisMethod.noBody();
        }
        return thisMethod;
    }

    private ProducedType getBestSatisfiedType(ProducedType currentType, Interface iface) {
        ProducedType refinedSuperType = currentType.getSupertype(iface);
        ProducedType firstSatisfiedType = getFirstSatisfiedType(currentType, iface);
        // in the very special case of the first satisfied type having type arguments erased to Object and
        // the most refined one having free type parameters, we prefer the one with free type parameters
        // because Java prefers it and it's in range with what nonWideningType does
        Map<TypeParameter, ProducedType> refinedTAs = refinedSuperType.getTypeArguments();
        Map<TypeParameter, ProducedType> firstTAs = firstSatisfiedType.getTypeArguments();
        for(TypeParameter tp : iface.getTypeParameters()){
            ProducedType refinedTA = refinedTAs.get(tp);
            ProducedType firstTA = firstTAs.get(tp);
            if(willEraseToObject(firstTA) && isTypeParameter(refinedTA))
                return refinedSuperType;
        }
        return firstSatisfiedType;
    }

    private ProducedType getFirstSatisfiedType(ProducedType currentType, Interface iface) {
        ProducedType found = null;
        TypeDeclaration currentDecl = currentType.getDeclaration();
        if(currentDecl == iface)
            return currentType;
        if(currentDecl.getExtendedType() != null){
            ProducedType supertype = currentType.getSupertype(currentDecl.getExtendedTypeDeclaration());
            found = getFirstSatisfiedType(supertype, iface);
            if(found != null)
                return found;
        }
        for(ProducedType superInterfaceType : currentType.getSatisfiedTypes()){
            found = getFirstSatisfiedType(superInterfaceType, iface);
            if(found != null)
                return found;
        }
        return null;
    }

    private void buildCompanion(final Tree.ClassOrInterface def,
            final Interface model, ClassDefinitionBuilder classBuilder,
            TypeParameterList typeParameterList) {
        at(def);
        // Give the $impl companion a $this field...
        classBuilder.getCompanionBuilder(model, typeParameterList);
    }

    private List<JCAnnotation> makeLocalContainerPath(Interface model) {
        List<String> path = List.nil();
        Scope container = model.getContainer();
        while(container != null
                && container instanceof Package == false){
            if(container instanceof Declaration)
                path = path.prepend(((Declaration) container).getPrefixedName());
            container = container.getContainer();
        }
        return makeAtLocalContainer(path, model.isCompanionClassNeeded() ? model.getJavaCompanionClassName() : null);
    }

    public List<JCStatement> transformRefinementSpecifierStatement(SpecifierStatement op, ClassDefinitionBuilder classBuilder) {
        List<JCStatement> result = List.<JCStatement>nil();
        // Check if this is a shortcut form of formal attribute refinement
        if (op.getRefinement()) {
            Tree.Term baseMemberTerm = op.getBaseMemberExpression();
            if(baseMemberTerm instanceof Tree.ParameterizedExpression)
                baseMemberTerm = ((Tree.ParameterizedExpression)baseMemberTerm).getPrimary();
            
            Tree.BaseMemberExpression expr = (BaseMemberExpression) baseMemberTerm;
            Declaration decl = expr.getDeclaration();
            
            if (Decl.isValue(decl) || Decl.isGetter(decl)) {
                // Now build a "fake" declaration for the attribute
                Tree.AttributeDeclaration attrDecl = new Tree.AttributeDeclaration(null);
                attrDecl.setDeclarationModel((Value)decl);
                attrDecl.setIdentifier(expr.getIdentifier());
                attrDecl.setScope(op.getScope());
                attrDecl.setSpecifierOrInitializerExpression(op.getSpecifierExpression());
                attrDecl.setAnnotationList(makeShortcutRefinementAnnotationTrees());
                
                // Make sure the boxing information is set correctly
                BoxingDeclarationVisitor v = new CompilerBoxingDeclarationVisitor(this);
                v.visit(attrDecl);
                
                // Generate the attribute
                transform(attrDecl, classBuilder);
            } else if (decl instanceof Method) {
                // Now build a "fake" declaration for the method
                Tree.MethodDeclaration methDecl = new Tree.MethodDeclaration(null);
                Method m = (Method)decl;
                methDecl.setDeclarationModel(m);
                methDecl.setIdentifier(expr.getIdentifier());
                methDecl.setScope(op.getScope());
                methDecl.setAnnotationList(makeShortcutRefinementAnnotationTrees());
                
                Tree.SpecifierExpression specifierExpression = op.getSpecifierExpression();
                methDecl.setSpecifierExpression(specifierExpression);
                
                if(specifierExpression instanceof Tree.LazySpecifierExpression == false){
                    Tree.Expression expression = specifierExpression.getExpression();
                    Tree.Term expressionTerm = Decl.unwrapExpressionsUntilTerm(expression);
                    // we can optimise lambdas and static method calls
                    if(!CodegenUtil.canOptimiseMethodSpecifier(expressionTerm, m)){
                        // we need a field to save the callable value
                        String name = naming.getMethodSpecifierAttributeName(m);
                        JCExpression specifierType = makeJavaType(expression.getTypeModel());
                        JCExpression specifier = expressionGen().transformExpression(expression);
                        classBuilder.field(PRIVATE | FINAL, name, specifierType, specifier, false);
                    }
                }

                // copy from formal declaration
                for (ParameterList pl : m.getParameterLists()) {
                    Tree.ParameterList tpl = new Tree.ParameterList(null);
                    tpl.setModel(pl);
                    for (Parameter p : pl.getParameters()) {
                        Tree.Parameter tp = null;
                        if (p.getModel() instanceof Value) {
                            Tree.ValueParameterDeclaration tvpd = new Tree.ValueParameterDeclaration(null);
                            tvpd.setParameterModel(p);
                            tp = tvpd;
                        } else if (p.getModel() instanceof Method) {
                            Tree.FunctionalParameterDeclaration tfpd = new Tree.FunctionalParameterDeclaration(null);
                            tfpd.setParameterModel(p);
                            tp = tfpd;
                        } else {
                            throw BugException.unhandledDeclarationCase(p.getModel());
                        }
                        tp.setScope(p.getDeclaration().getContainer());
                        //tp.setIdentifier(makeIdentifier(p.getName()));
                        tpl.addParameter(tp);
                    }
                    methDecl.addParameterList(tpl);
                }
                
                // Make sure the boxing information is set correctly
                BoxingDeclarationVisitor v = new CompilerBoxingDeclarationVisitor(this);
                v.visit(methDecl);
                
                // Generate the method
                classBuilder.method(methDecl);
            }
        } else {
            // Normal case, just generate the specifier statement
            result = result.append(expressionGen().transform(op));
        }
        
        Tree.Term term = op.getBaseMemberExpression();
        if (term instanceof Tree.BaseMemberExpression) {
            Tree.BaseMemberExpression bme = (Tree.BaseMemberExpression)term;
            DeferredSpecification ds = statementGen().getDeferredSpecification(bme.getDeclaration());
            if (ds != null && needsInnerSubstitution(term.getScope(), bme.getDeclaration())){
                result = result.append(ds.openInnerSubstitution());
            }
        }
        return result;
    }

    private AnnotationList makeShortcutRefinementAnnotationTrees() {
        AnnotationList annotationList = new AnnotationList(null);
        
        Tree.Annotation shared = new Tree.Annotation(null);
        Tree.BaseMemberExpression sharedPrimary = new Tree.BaseMemberExpression(null);
        sharedPrimary.setDeclaration(typeFact().getLanguageModuleDeclaration("shared"));
        shared.setPrimary(sharedPrimary);
        annotationList.addAnnotation(shared);
        
        Tree.Annotation actual = new Tree.Annotation(null);
        Tree.BaseMemberExpression actualPrimary = new Tree.BaseMemberExpression(null);
        actualPrimary.setDeclaration(typeFact().getLanguageModuleDeclaration("actual"));
        actual.setPrimary(actualPrimary);
        annotationList.addAnnotation(actual);
        
        return annotationList;
    }

    /**
     * We only need an inner substitution if we're within that substitution's scope
     */
    private boolean needsInnerSubstitution(Scope scope, Declaration declaration) {
        while(scope != null && scope instanceof Package == false){
            if(scope instanceof ControlBlock){
                Set<Value> specifiedValues = ((ControlBlock) scope).getSpecifiedValues();
                if(specifiedValues != null && specifiedValues.contains(declaration))
                    return true;
            }
            scope = scope.getScope();
        }
        return false;
    }

    public void transform(AttributeDeclaration decl, ClassDefinitionBuilder classBuilder) {
        final Value model = decl.getDeclarationModel();
        boolean lazy = decl.getSpecifierOrInitializerExpression() instanceof LazySpecifierExpression;
        boolean useField = Strategy.useField(model) && !lazy;
        String attrName = decl.getIdentifier().getText();

        // Only a non-formal or a concrete-non-lazy attribute has a corresponding field
        // and if a captured class parameter exists with the same name we skip this part as well
        Parameter parameter = CodegenUtil.findParamForDecl(decl);
        boolean createField = Strategy.createField(parameter, model) && !lazy;
        boolean concrete = Decl.withinInterface(decl)
                && decl.getSpecifierOrInitializerExpression() != null;
        if (!lazy && 
                (concrete || 
                        (!Decl.isFormal(decl) 
                                && createField))) {
            ProducedTypedReference typedRef = getTypedReference(model);
            ProducedTypedReference nonWideningTypedRef = nonWideningTypeDecl(typedRef);
            ProducedType nonWideningType = nonWideningType(typedRef, nonWideningTypedRef);
            
            if (Decl.isIndirect(decl)) {
                attrName = Naming.getAttrClassName(model, 0);
                nonWideningType = getGetterInterfaceType(model);
            }
            
            JCExpression initialValue = null;
            if (decl.getSpecifierOrInitializerExpression() != null) {
                Value declarationModel = model;
                initialValue = expressionGen().transformExpression(decl.getSpecifierOrInitializerExpression().getExpression(), 
                        CodegenUtil.getBoxingStrategy(declarationModel), 
                        nonWideningType);
            }

            int flags = 0;
            
            if (!CodegenUtil.isUnBoxed(nonWideningTypedRef.getDeclaration())) {
                flags |= JT_NO_PRIMITIVES;
            }
            JCExpression type = makeJavaType(nonWideningType, flags);
            if (Decl.isLate(decl)) {
                type = make().TypeArray(type);
            }

            int modifiers = (useField) ? transformAttributeFieldDeclFlags(decl) : transformLocalDeclFlags(decl);
            
            // If the attribute is really from a parameter then don't generate a field
            // (makeAttributeForValueParameter() or makeMethodForFunctionalParameter() 
            //  does it in those cases)
            if (parameter == null
                    || parameter.isHidden()) {
                if (concrete) {
                    classBuilder.getCompanionBuilder((TypeDeclaration)model.getContainer()).field(modifiers, attrName, type, initialValue, !useField);
                } else {
                    // fields should be ignored, they are accessed by the getters
                    classBuilder.field(modifiers, attrName, type, initialValue, !useField, makeAtIgnore());
                }        
            }
        }

        boolean withinInterface = Decl.withinInterface(decl);
        if (useField || withinInterface || lazy) {
            if (!withinInterface || model.isShared()) {
                // Generate getter in main class or interface (when shared)
                classBuilder.attribute(makeGetter(decl, false, lazy));
            }
            if (withinInterface && lazy) {
                // Generate getter in companion class
                classBuilder.getCompanionBuilder((Interface)decl.getDeclarationModel().getContainer()).attribute(makeGetter(decl, true, lazy));
            }
            if (Decl.isVariable(decl) || Decl.isLate(decl)) {
                if (!withinInterface || model.isShared()) {
                    // Generate setter in main class or interface (when shared)
                    classBuilder.attribute(makeSetter(decl, false, lazy));
                }
                if (withinInterface && lazy) {
                    // Generate setter in companion class
                    classBuilder.getCompanionBuilder((Interface)decl.getDeclarationModel().getContainer()).attribute(makeSetter(decl, true, lazy));
                }
            }
        }
    }

	public AttributeDefinitionBuilder transform(AttributeSetterDefinition decl, boolean forCompanion) {
	    if (Strategy.onlyOnCompanion(decl.getDeclarationModel()) && !forCompanion) {
	        return null;
	    }
        String name = decl.getIdentifier().getText();
        final AttributeDefinitionBuilder builder = AttributeDefinitionBuilder
                /* 
                 * We use the getter as TypedDeclaration here because this is the same type but has a refined
                 * declaration we can use to make sure we're not widening the attribute type.
                 */
            .setter(this, name, decl.getDeclarationModel().getGetter())
            .modifiers(transformAttributeGetSetDeclFlags(decl.getDeclarationModel(), forCompanion));
        
        // companion class members are never actual no matter what the Declaration says
        if(forCompanion)
            builder.notActual();
        
        if (Decl.withinClass(decl) || forCompanion) {
            JCBlock setterBlock = makeSetterBlock(decl.getDeclarationModel(), decl.getBlock(), decl.getSpecifierExpression());
            builder.setterBlock(setterBlock);
        } else {
            builder.isFormal(true);
        }
        builder.userAnnotationsSetter(expressionGen().transform(decl.getAnnotationList()));
        return builder;
    }

    public AttributeDefinitionBuilder transform(AttributeGetterDefinition decl, boolean forCompanion) {
        if (Strategy.onlyOnCompanion(decl.getDeclarationModel()) && !forCompanion) {
            return null;
        }
        String name = decl.getIdentifier().getText();
        //expressionGen().transform(decl.getAnnotationList());
        final AttributeDefinitionBuilder builder = AttributeDefinitionBuilder
            .getter(this, name, decl.getDeclarationModel())
            .modifiers(transformAttributeGetSetDeclFlags(decl.getDeclarationModel(), forCompanion));
        
        // companion class members are never actual no matter what the Declaration says
        if(forCompanion)
            builder.notActual();
        
        if (Decl.withinClass(decl) || forCompanion) {
            JCBlock body = statementGen().transform(decl.getBlock());
            builder.getterBlock(body);
        } else {
            builder.isFormal(true);
        }
        builder.userAnnotations(expressionGen().transform(decl.getAnnotationList()));
        return builder;    
    }

    private int transformDeclarationSharedFlags(Declaration decl){
        return Decl.isShared(decl) && !Decl.isAncestorLocal(decl) ? PUBLIC : 0;
    }
    
    private int transformClassDeclFlags(ClassOrInterface cdecl) {
        int result = 0;

        result |= transformDeclarationSharedFlags(cdecl);
        // aliases cannot be abstract, especially since they're just placeholders
        result |= (cdecl instanceof Class) && (cdecl.isAbstract() || cdecl.isFormal()) && !cdecl.isAlias() ? ABSTRACT : 0;
        result |= (cdecl instanceof Interface) ? INTERFACE : 0;
        // aliases are always final placeholders, final classes are also final
        result |= (cdecl instanceof Class) && (cdecl.isAlias() || cdecl.isFinal())  ? FINAL : 0;

        return result;
    }

    private int transformTypeAliasDeclFlags(TypeAlias decl) {
        int result = 0;

        result |= transformDeclarationSharedFlags(decl);
        result |= FINAL;

        return result;
    }

    private int transformClassDeclFlags(Tree.ClassOrInterface cdecl) {
        return transformClassDeclFlags(cdecl.getDeclarationModel());
    }
    
    private int transformMethodDeclFlags(Method def) {
        int result = 0;

        if (def.isToplevel()) {
            result |= def.isShared() ? PUBLIC : 0;
            result |= STATIC;
        } else if (Decl.isLocalNotInitializer(def)) {
            result |= def.isShared() ? PUBLIC : 0;
        } else {
            result |= def.isShared() ? PUBLIC : PRIVATE;
            result |= def.isFormal() && !def.isDefault() ? ABSTRACT : 0;
            result |= !(def.isFormal() || def.isDefault() || def.getContainer() instanceof Interface) ? FINAL : 0;
        }

        return result;
    }
    
    private int transformAttributeFieldDeclFlags(Tree.AttributeDeclaration cdecl) {
        int result = 0;

        result |= Decl.isVariable(cdecl) || Decl.isLate(cdecl) ? 0 : FINAL;
        result |= PRIVATE;

        return result;
    }

    private int transformLocalDeclFlags(Tree.AttributeDeclaration cdecl) {
        int result = 0;

        result |= Decl.isVariable(cdecl) ? 0 : FINAL;

        return result;
    }

    /**
     * Returns the modifier flags to be used for the getter & setter for the 
     * given attribute-like declaration.  
     * @param tdecl attribute-like declaration (Value, Getter, Parameter etc)
     * @param forCompanion Whether the getter/setter is on a companion type
     * @return The modifier flags.
     */
    int transformAttributeGetSetDeclFlags(TypedDeclaration tdecl, boolean forCompanion) {
        if (tdecl instanceof Setter) {
            // Spec says: A setter may not be annotated shared, default or 
            // actual. The visibility and refinement modifiers of an attribute 
            // with a setter are specified by annotating the matching getter.
            tdecl = ((Setter)tdecl).getGetter();
        }
        
        int result = 0;

        result |= tdecl.isShared() ? PUBLIC : PRIVATE;
        result |= ((tdecl.isFormal() && !tdecl.isDefault()) && !forCompanion) ? ABSTRACT : 0;
        result |= !(tdecl.isFormal() || tdecl.isDefault() || Decl.withinInterface(tdecl)) || forCompanion ? FINAL : 0;

        return result;
    }

    private int transformObjectDeclFlags(Value cdecl) {
        int result = 0;

        result |= FINAL;
        result |= !Decl.isAncestorLocal(cdecl) && Decl.isShared(cdecl) ? PUBLIC : 0;

        return result;
    }

    private AttributeDefinitionBuilder makeGetterOrSetter(Tree.AttributeDeclaration decl, boolean forCompanion, boolean lazy, 
                                                          AttributeDefinitionBuilder builder, boolean isGetter) {
        at(decl);
        if (forCompanion || lazy) {
            SpecifierOrInitializerExpression specOrInit = decl.getSpecifierOrInitializerExpression();
            if (specOrInit != null) {
                HasErrorException error = errors().getFirstExpressionErrorAndMarkBrokenness(specOrInit.getExpression());
                if (error != null) {
                    builder.getterBlock(make().Block(0, List.<JCStatement>of(error.makeThrow(this))));
                } else {
                    Value declarationModel = decl.getDeclarationModel();
                    ProducedTypedReference typedRef = getTypedReference(declarationModel);
                    ProducedTypedReference nonWideningTypedRef = nonWideningTypeDecl(typedRef);
                    ProducedType nonWideningType = nonWideningType(typedRef, nonWideningTypedRef);
                    
                    JCExpression expr = expressionGen().transformExpression(specOrInit.getExpression(), 
                            CodegenUtil.getBoxingStrategy(declarationModel), 
                            nonWideningType);
                    expr = convertToIntIfHashAttribute(declarationModel, expr);
                    builder.getterBlock(make().Block(0, List.<JCStatement>of(make().Return(expr))));
                }
            } else {
                JCExpression accessor = naming.makeQualifiedName(
                        naming.makeQuotedThis(), 
                        decl.getDeclarationModel(), 
                        Naming.NA_MEMBER | (isGetter ? Naming.NA_GETTER : Naming.NA_SETTER));
                
                if (isGetter) {
                    builder.getterBlock(make().Block(0, List.<JCStatement>of(make().Return(
                            make().Apply(
                                    null, 
                                    accessor, 
                                    List.<JCExpression>nil())))));
                } else {
                    List<JCExpression> args = List.<JCExpression>of(naming.makeName(decl.getDeclarationModel(), Naming.NA_MEMBER | Naming.NA_IDENT));
                    builder.setterBlock(make().Block(0, List.<JCStatement>of(make().Exec(
                            make().Apply(
                                    null, 
                                    accessor, 
                                    args)))));
                }
                
            }
        }
        if(forCompanion)
            builder.notActual();
        return builder
            .modifiers(transformAttributeGetSetDeclFlags(decl.getDeclarationModel(), forCompanion))
            .isFormal((Decl.isFormal(decl) || Decl.withinInterface(decl)) && !forCompanion);
    }
    
    private AttributeDefinitionBuilder makeGetter(Tree.AttributeDeclaration decl, boolean forCompanion, boolean lazy) {
        at(decl);
        String attrName = decl.getIdentifier().getText();
        AttributeDefinitionBuilder getter = AttributeDefinitionBuilder
            .getter(this, attrName, decl.getDeclarationModel());
        if(!forCompanion)
            getter.userAnnotations(expressionGen().transform(decl.getAnnotationList()));
        else
            getter.ignoreAnnotations();
        
        if (Decl.isIndirect(decl)) {
            getter.getterBlock(generateIndirectGetterBlock(decl.getDeclarationModel()));
        }
        
        return makeGetterOrSetter(decl, forCompanion, lazy, getter, true);
    }

    private JCTree.JCBlock generateIndirectGetterBlock(Value v) {
        JCTree.JCExpression returnExpr;
        returnExpr = naming.makeQualIdent(naming.makeName(v, Naming.NA_WRAPPER), "get_");
        returnExpr = make().Apply(null, returnExpr, List.<JCExpression>nil());
        JCReturn returnValue = make().Return(returnExpr);
        List<JCStatement> stmts = List.<JCTree.JCStatement>of(returnValue);   
        JCTree.JCBlock block = make().Block(0L, stmts);
        return block;
    }

    private AttributeDefinitionBuilder makeSetter(Tree.AttributeDeclaration decl, boolean forCompanion, boolean lazy) {
        at(decl);
        String attrName = decl.getIdentifier().getText();
        AttributeDefinitionBuilder setter = AttributeDefinitionBuilder.setter(this, attrName, decl.getDeclarationModel());
        return makeGetterOrSetter(decl, forCompanion, lazy, setter, false);
    }

    public List<JCTree> transformWrappedMethod(Tree.AnyMethod def) {
        final Method model = def.getDeclarationModel();
        if (model.isParameter()) {
            return List.nil();
        }
        naming.clearSubstitutions(model);
        // Generate a wrapper class for the method
        String name = def.getIdentifier().getText();
        ClassDefinitionBuilder builder = ClassDefinitionBuilder.methodWrapper(this, name, Decl.isShared(def));
        
        if (Decl.isAnnotationConstructor(def)) {
            AnnotationInvocation ai = ((AnnotationInvocation)def.getDeclarationModel().getAnnotationConstructor());
            if (ai != null) {
                builder.annotations(List.of(makeAtAnnotationInstantiation(ai)));
                builder.annotations(makeExprAnnotations(def, ai));
            }
        }
        
        builder.methods(classGen().transform(def, builder));
        
        // Toplevel method
        if (Strategy.generateMain(def)) {
            // Add a main() method
            builder.method(makeMainForFunction(model));
        }
        
        if(Decl.isLocal(model) || Decl.isToplevel(model)){
            builder.annotations(makeAtLocalDeclarations(def));
        }
        if(Decl.isLocal(model)){
            builder.annotations(makeAtLocalDeclaration(model.getQualifier()));
        }
        
        List<JCTree> result = builder.build();
        
        if (Decl.isLocal(def)) {
            // Inner method
            JCVariableDecl call = at(def).VarDef(
                    make().Modifiers(FINAL),
                    naming.getSyntheticInstanceName(model),
                    naming.makeSyntheticClassname(model),
                    makeSyntheticInstance(model));
            result = result.append(call);
        }
        
        //if (Decl.isAnnotationConstructor(def)) {
            //result = result.prependList(transformAnnotationConstructorType(def));
        //}
        return result;
    }

    /**
     * Make the {@code @*Exprs} annotations to hold the literal arguments 
     * to the invocation.
     */
    private List<JCAnnotation> makeExprAnnotations(Tree.AnyMethod def,
            AnnotationInvocation ai) {
        AnnotationInvocation ctor = (AnnotationInvocation)def.getDeclarationModel().getAnnotationConstructor();
        return ai.makeExprAnnotations(expressionGen(), ctor, List.<AnnotationFieldName>nil());
    }
    
    public JCAnnotation makeAtAnnotationInstantiation(AnnotationInvocation invocation) {
        return invocation.encode(this, ListBuffer.<JCExpression>lb());
    }

    private MethodDefinitionBuilder makeAnnotationMethod(Tree.Parameter parameter) {
        Parameter parameterModel = parameter.getParameterModel();
        JCExpression type = transformAnnotationMethodType(parameter);
        JCExpression defaultValue = parameterModel.isDefaulted() ? transformAnnotationParameterDefault(parameter) : null;
        MethodDefinitionBuilder mdb = MethodDefinitionBuilder.method(this, parameterModel.getModel(), Naming.NA_ANNOTATION_MEMBER);
        if (isMetamodelReference(parameterModel.getType())
                || 
                (typeFact().isIterableType(parameterModel.getType())
                && isMetamodelReference(typeFact().getIteratedType(parameterModel.getType())))) {
            mdb.modelAnnotations(List.of(make().Annotation(make().Type(syms().ceylonAtDeclarationReferenceType), 
                    List.<JCExpression>nil())));
        } else if (Decl.isEnumeratedTypeWithAnonCases(parameterModel.getType())
                || 
                (typeFact().isIterableType(parameterModel.getType())
                        && Decl.isEnumeratedTypeWithAnonCases(typeFact().getIteratedType(parameterModel.getType())))) {
            mdb.modelAnnotations(List.of(make().Annotation(make().Type(syms().ceylonAtEnumerationReferenceType), 
                    List.<JCExpression>nil())));
        }
        mdb.modifiers(PUBLIC | ABSTRACT);
        mdb.resultType(null, type);
        mdb.defaultValue(defaultValue);
        mdb.noBody();
        return mdb;
    }

    public List<MethodDefinitionBuilder> transform(Tree.AnyMethod def, ClassDefinitionBuilder classBuilder) {
        if (def.getDeclarationModel().isParameter()) {
            return List.nil();
        }
        // Transform the method body of the 'inner-most method'
        boolean prevSyntheticClassBody = expressionGen().withinSyntheticClassBody(Decl.isMpl(def.getDeclarationModel())
                || Decl.isLocalNotInitializer(def)
                || expressionGen().isWithinSyntheticClassBody());
        List<JCStatement> body = transformMethodBody(def);
        expressionGen().withinSyntheticClassBody(prevSyntheticClassBody);
        return transform(def, classBuilder, body);
    }

    List<MethodDefinitionBuilder> transform(Tree.AnyMethod def,
            ClassDefinitionBuilder classBuilder, List<JCStatement> body) {
        final Method model = def.getDeclarationModel();
        
        List<MethodDefinitionBuilder> result = List.<MethodDefinitionBuilder>nil();
        if (!Decl.withinInterface(model)) {
            // Transform to the class
            boolean refinedResultType = !model.getType().isExactly(
                    ((TypedDeclaration)model.getRefinedDeclaration()).getType());
            result = transformMethod(def, 
                    true,
                    true,
                    true,
                    transformMplBodyUnlessSpecifier(def, model, body),
                    refinedResultType 
                    && !Decl.withinInterface(model.getRefinedDeclaration())? daoSuper : daoThis,
                    !Strategy.defaultParameterMethodOnSelf(model));
        } else {// Is within interface
            // Transform the definition to the companion class, how depends
            // on what kind of method it is
            List<MethodDefinitionBuilder> companionDefs;
            if (def instanceof Tree.MethodDeclaration) {
                final SpecifierExpression specifier = ((Tree.MethodDeclaration) def).getSpecifierExpression();
                if (specifier == null) {
                    // formal or abstract 
                    // (still need overloads and DPMs on the companion)
                    companionDefs = transformMethod(def,  
                            false,
                            true,
                            true,
                            null,
                            daoCompanion,
                            false);   
                } else {
                    companionDefs = transformMethod(def,
                            true,
                            false,
                            !model.isShared(),
                            transformMplBodyUnlessSpecifier(def, model, body),
                            daoCompanion,
                            false);
                }
            } else if (def instanceof Tree.MethodDefinition) {
                companionDefs = transformMethod(def,  
                        true,
                        false,
                        !model.isShared(),
                        transformMplBodyUnlessSpecifier(def, model, body),
                        daoCompanion,
                        false);
            } else {
                throw BugException.unhandledNodeCase(def);
            }
            if(!companionDefs.isEmpty())
                classBuilder.getCompanionBuilder((TypeDeclaration)model.getContainer())
                    .methods(companionDefs);
            
            // Transform the declaration to the target interface
            // but only if it's shared
            if (Decl.isShared(model)) {
                result = transformMethod(def, 
                        true,
                        true,
                        true,
                        null,
                        daoAbstract,
                        !Strategy.defaultParameterMethodOnSelf(model));
            }
        }
        return result;
    }

    
    /**
     * Transforms a method, generating default argument overloads and 
     * default value methods
     * @param def The method
     * @param model The method model
     * @param methodName The method name
     * @param transformMethod Whether the method itself should be transformed.
     * @param actualAndAnnotations Whether the method itself is actual and has 
     * model annotations
     * @param body The body of the method (or null for an abstract method)
     * @param daoTransformation The default argument overload transformation
     * @param transformDefaultValues Whether to generate default value methods
     * @param defaultValuesBody Whether the default value methods should have a body
     */
    private List<MethodDefinitionBuilder> transformMethod(Tree.AnyMethod def,
            boolean transformMethod, boolean actual, boolean includeAnnotations, List<JCStatement> body, 
            DaoBody daoTransformation, 
            boolean defaultValuesBody) {
        return transformMethod(def.getDeclarationModel(), 
                def.getTypeParameterList(),
                def.getParameterLists(),
                def.getAnnotationList(),
                transformMethod, actual, includeAnnotations, body,
                daoTransformation,
                defaultValuesBody);
    }
    
    private List<MethodDefinitionBuilder> transformMethod(
            final Method methodModel,
            Tree.TypeParameterList typeParameterList, 
            java.util.List<Tree.ParameterList> parameterLists,
            Tree.AnnotationList annotationList,
            boolean transformMethod, boolean actual, boolean includeAnnotations, List<JCStatement> body, 
            DaoBody daoTransformation, 
            boolean defaultValuesBody) {
        
        ListBuffer<MethodDefinitionBuilder> lb = ListBuffer.<MethodDefinitionBuilder>lb();
        Declaration refinedDeclaration = methodModel.getRefinedDeclaration();
        
        final MethodDefinitionBuilder methodBuilder = MethodDefinitionBuilder.method(this, methodModel);
        
        // do the reified type param arguments
        if (typeParameterList != null && gen().supportsReified(methodModel)) {
            methodBuilder.reifiedTypeParameters(typeParameterListModel(typeParameterList));
        }
        
        if (methodModel.getParameterLists().size() > 1) {
            methodBuilder.mpl(methodModel.getParameterLists());
        }
        
        boolean hasOverloads = false;
        Tree.ParameterList parameterList = parameterLists.get(0);
        for (final Tree.Parameter parameter : parameterList.getParameters()) {
            Parameter parameterModel = parameter.getParameterModel();
            List<JCAnnotation> annotations = null;
            if (includeAnnotations
                    && parameter instanceof Tree.ParameterDeclaration
                    && ((Tree.ParameterDeclaration)parameter).getTypedDeclaration() != null) {
                annotations = expressionGen().transform(((Tree.ParameterDeclaration)parameter).getTypedDeclaration().getAnnotationList());
            }
            
            methodBuilder.parameter(parameterModel, annotations, 0, true);

            if (Strategy.hasDefaultParameterValueMethod(parameterModel)
                    || Strategy.hasDefaultParameterOverload(parameterModel)) {
                if (refinedDeclaration == methodModel
                        || (!Decl.withinInterface(methodModel) && body != null)) {
                    
                    if (daoTransformation != null && (daoTransformation instanceof DaoCompanion == false || body != null)) {
                        DaoBody daoTrans = (body == null) ? daoAbstract : daoThis;
                        MethodDefinitionBuilder overloadBuilder = MethodDefinitionBuilder.method(this, methodModel);
                        overloadBuilder.location(null);
                        MethodDefinitionBuilder overloadedMethod = new DefaultedArgumentMethod(daoTrans, methodModel)
                            .makeOverload(
                                overloadBuilder, 
                                parameterList.getModel(),
                                parameter.getParameterModel(),
                                typeParameterListModel(typeParameterList));
                        lb.append(overloadedMethod);
                    }
                    
                    if (refinedDeclaration == methodModel
                            && Strategy.hasDefaultParameterValueMethod(parameterModel)) {
                        lb.append(makeParamDefaultValueMethod(defaultValuesBody, methodModel, parameterList, parameter, typeParameterList));
                    }
                }
                
                hasOverloads = true;
            }
        }

        // Determine if we need to generate a "canonical" method
        boolean createCanonical = hasOverloads
                && Decl.withinClassOrInterface(methodModel)
                && body != null;
        
        if (createCanonical) {
            // Creates the private "canonical" method containing the actual body
            MethodDefinitionBuilder canonicalBuilder = MethodDefinitionBuilder.method(this, methodModel, Naming.NA_CANONICAL_METHOD);
            MethodDefinitionBuilder canonicalMethod = new CanonicalMethod(daoTransformation, methodModel, body)
                .makeOverload(
                    canonicalBuilder, 
                    parameterList.getModel(),
                    null,
                    typeParameterListModel(typeParameterList));
            lb.append(canonicalMethod);
        }
        
        if (transformMethod) {
            methodBuilder.modifiers(transformMethodDeclFlags(methodModel));
            if (actual) {
                methodBuilder.isOverride(methodModel.isActual());
            }
            if (includeAnnotations) {
                methodBuilder.userAnnotations(expressionGen().transform(annotationList));
                methodBuilder.modelAnnotations(methodModel.getAnnotations());
            } else {
                methodBuilder.ignoreModelAnnotations();
            }
            methodBuilder.resultType(methodModel, 0);
            copyTypeParameters(methodModel, methodBuilder);
            
            if (createCanonical) {
                // Creates method that redirects to the "canonical" method containing the actual body
                MethodDefinitionBuilder overloadedMethod = new CanonicalMethod(daoThis, methodModel)
                    .makeOverload(
                        methodBuilder, 
                        parameterList.getModel(),
                        null,
                        typeParameterListModel(typeParameterList));
                lb.append(overloadedMethod);
            } else {
                if (body != null) {
                    // Construct the outermost method using the body we've built so far
                    methodBuilder.body(body);
                } else {
                    methodBuilder.noBody();
                }
                lb.append(methodBuilder);
            }
        }
        return lb.toList();
    }

    List<JCStatement> transformMplBodyUnlessSpecifier(Tree.AnyMethod def,
            Method model,
            List<JCStatement> body) {
        if (def instanceof Tree.MethodDeclaration) {
            Tree.SpecifierExpression specifier = ((Tree.MethodDeclaration)def).getSpecifierExpression();
            if (specifier != null 
                    && !(specifier instanceof Tree.LazySpecifierExpression)) {
                if (!CodegenUtil.canOptimiseMethodSpecifier(specifier.getExpression().getTerm(), model)) {
                    return body;
                }
            }
        }
        return transformMplBody(def.getParameterLists(), model, body);
    }
    
    /**
     * Constructs all but the outer-most method of a {@code Method} with 
     * multiple parameter lists 
     * @param model The {@code Method} model
     * @param body The inner-most body
     */
    List<JCStatement> transformMplBody(java.util.List<Tree.ParameterList> parameterListsTree,
            Method model,
            List<JCStatement> body) {
        ProducedType resultType = model.getType();
        for (int index = model.getParameterLists().size() - 1; index >  0; index--) {
            ParameterList pl = model.getParameterLists().get(index);
            resultType = gen().typeFact().getCallableType(List.of(resultType, typeFact().getParameterTypesAsTupleType(pl.getParameters(), model.getReference())));
            CallableBuilder cb = CallableBuilder.mpl(gen(), resultType, pl, parameterListsTree.get(index), body);
            body = List.<JCStatement>of(make().Return(cb.build()));
        }
        return body;
    }

    private List<JCStatement> transformMethodBody(Tree.AnyMethod def) {
        List<JCStatement> body = null;
        final Method model = def.getDeclarationModel();
        
        if (model.isDeferred()) {
            // Uninitialized or deferred initialized method => Make a Callable field
            String fieldName = naming.selector(model);
            final Parameter initializingParameter = CodegenUtil.findParamForDecl(def);
            int mods = PRIVATE;
            JCExpression initialValue;
            if (initializingParameter != null) {
                mods |= FINAL;
                initialValue = makeUnquotedIdent(Naming.getAliasedParameterName(initializingParameter));
            } else {
                // The field isn't initialized by a parameter, but later in the block
                initialValue = makeNull();
            }
            ProducedType callableType = typeFact().getCallableType(model.getType());
            current().field(mods, fieldName, makeJavaType(callableType), initialValue, false);
            Invocation invocation = new CallableSpecifierInvocation(
                    this,
                    model,
                    makeUnquotedIdent(fieldName),
                    // we don't have to give a Term here because it's used for casting the Callable in case of callable erasure, 
                    // but with deferred methods we can't define them so that they are erased so we're good
                    null,
                    def);
            invocation.handleBoxing(true);
            JCExpression call = expressionGen().transformInvocation(invocation);
            JCStatement stmt;
            if (!isVoid(def) || !Decl.isUnboxedVoid(model) || Strategy.useBoxedVoid((Method)model)) {
                stmt = make().Return(call);
            } else {
                stmt = make().Exec(call);
            }
            
            JCStatement result;
            if (initializingParameter == null) {
                // If the field isn't initialized by a parameter we have to 
                // cope with the possibility that it's never initialized
                final JCBinary cond = make().Binary(JCTree.EQ, makeUnquotedIdent(fieldName), makeNull());
                final JCStatement throw_ = make().Throw(make().NewClass(null, null, 
                        makeIdent(syms().ceylonUninitializedMethodErrorType), 
                        List.<JCExpression>nil(), 
                        null));
                result = make().If(cond, throw_, stmt);
            } else {
                result = stmt;
            }
            return List.<JCStatement>of(result);
        } else if (def instanceof Tree.MethodDefinition) {
            body = transformMethodBlock((Tree.MethodDefinition)def);
        } else if (def instanceof MethodDeclaration
                && ((MethodDeclaration) def).getSpecifierExpression() != null) {
            body = transformSpecifiedMethodBody((MethodDeclaration)def, ((MethodDeclaration) def).getSpecifierExpression());
        }
        return body;
    }

    private List<JCStatement> transformMethodBlock(
            final Tree.MethodDefinition def) {
        final Method model = def.getDeclarationModel();
        final Tree.Block block = def.getBlock();
        List<JCStatement> body;
        boolean prevNoExpressionlessReturn = statementGen().noExpressionlessReturn;
        try {
            statementGen().noExpressionlessReturn = Decl.isMpl(model) || Strategy.useBoxedVoid(model);
            body = statementGen().transformBlock(block);
        } finally {
            statementGen().noExpressionlessReturn = prevNoExpressionlessReturn;
        }
        // We void methods need to have their Callables return null
        // so adjust here.
        if ((Decl.isMpl(model) || Strategy.useBoxedVoid(model))
                && !block.getDefinitelyReturns()) {
            if (Decl.isUnboxedVoid(model)) {
                body = body.append(make().Return(makeNull()));
            } else {
                body = body.append(make().Return(makeErroneous(block, "compiler bug: non-void method doesn't definitely return")));
            }
        }
        return body;
    }

    List<JCStatement> transformSpecifiedMethodBody(Tree.MethodDeclaration  def, SpecifierExpression specifierExpression) {
        final Method model = def.getDeclarationModel();
        List<JCStatement> body;
        Tree.MethodDeclaration methodDecl = def;
        boolean isLazy = specifierExpression instanceof Tree.LazySpecifierExpression;
        boolean returnNull = false;
        JCExpression bodyExpr;
        Tree.Term term = null;
        if (specifierExpression != null
                && specifierExpression.getExpression() != null) {
            term = Decl.unwrapExpressionsUntilTerm(specifierExpression.getExpression());
            HasErrorException error = errors().getFirstExpressionErrorAndMarkBrokenness(term);
            if (error != null) {
                return List.<JCStatement>of(error.makeThrow(this));
            }
        }
        if (!isLazy && term instanceof Tree.FunctionArgument) {
            // Method specified with lambda: Don't bother generating a 
            // Callable, just transform the expr to use as the method body.
            Tree.FunctionArgument fa = (Tree.FunctionArgument)term;
            ProducedType resultType = model.getType();
            returnNull = isAnything(resultType) && fa.getExpression().getUnboxed();
            final java.util.List<com.redhat.ceylon.compiler.typechecker.tree.Tree.Parameter> lambdaParams = fa.getParameterLists().get(0).getParameters();
            final java.util.List<com.redhat.ceylon.compiler.typechecker.tree.Tree.Parameter> defParams = def.getParameterLists().get(0).getParameters();
            List<Substitution> substitutions = List.nil();
            for (int ii = 0; ii < lambdaParams.size(); ii++) {
                substitutions = substitutions.append(naming.addVariableSubst(
                        (TypedDeclaration)lambdaParams.get(ii).getParameterModel().getModel(), 
                        defParams.get(ii).getParameterModel().getName()));
            }
            bodyExpr = gen().expressionGen().transformExpression(fa.getExpression(), 
                            returnNull ? BoxingStrategy.INDIFFERENT : CodegenUtil.getBoxingStrategy(model), 
                            resultType);
            for (Substitution subs : substitutions) {
                subs.close();
            }
        } else if (!isLazy && typeFact().isCallableType(term.getTypeModel())) {
            returnNull = isAnything(term.getTypeModel()) && term.getUnboxed();
            Method method = methodDecl.getDeclarationModel();
            boolean lazy = specifierExpression instanceof Tree.LazySpecifierExpression;
            boolean inlined = CodegenUtil.canOptimiseMethodSpecifier(term, method);
            Invocation invocation;
            if ((lazy || inlined)
                    && term instanceof Tree.MemberOrTypeExpression
                    && ((Tree.MemberOrTypeExpression)term).getDeclaration() instanceof Functional) {
                Declaration primaryDeclaration = ((Tree.MemberOrTypeExpression)term).getDeclaration();
                ProducedReference producedReference = ((Tree.MemberOrTypeExpression)term).getTarget();
                invocation = new MethodReferenceSpecifierInvocation(
                        this, 
                        (Tree.MemberOrTypeExpression)term, 
                        primaryDeclaration,
                        producedReference,
                        method,
                        specifierExpression);
            } else if (!lazy && !inlined) {
                // must be a callable we stored
                String name = naming.getMethodSpecifierAttributeName(method);
                invocation = new CallableSpecifierInvocation(
                        this, 
                        method, 
                        naming.makeUnquotedIdent(name),
                        term,
                        term);
            } else if (isCeylonCallableSubtype(term.getTypeModel())) {
                invocation = new CallableSpecifierInvocation(
                        this, 
                        method, 
                        expressionGen().transformExpression(term),
                        term,
                        term);
            } else {
                throw new BugException(term, "unhandled primary: " + term == null ? "null" : term.getNodeType());
            }
            invocation.handleBoxing(true);
            bodyExpr = expressionGen().transformInvocation(invocation);
        } else {
            bodyExpr = expressionGen().transformExpression(model, term);
            // The innermost of an MPL method declared void needs to return null
            returnNull = Decl.isUnboxedVoid(model) && Decl.isMpl(model);
        }
        if (!Decl.isUnboxedVoid(model)
                || Decl.isMpl(model)
                || Strategy.useBoxedVoid(model)) {
            if (returnNull) {
                body = List.<JCStatement>of(make().Exec(bodyExpr), make().Return(makeNull()));
            } else {
                body = List.<JCStatement>of(make().Return(bodyExpr));
            }
        } else {
            body = List.<JCStatement>of(make().Exec(bodyExpr));
        }
        return body;
    }

    private boolean isVoid(Tree.Declaration def) {
        if (def instanceof Tree.AnyMethod) {
            return gen().isAnything(((Tree.AnyMethod)def).getType().getTypeModel());
        } else if (def instanceof Tree.AnyClass) {
            // Consider classes void since ctors don't require a return statement
            return true;
        }
        throw BugException.unhandledNodeCase(def);
    }
    
    private boolean isVoid(Declaration def) {
        if (def instanceof Method) {
            return gen().isAnything(((Method)def).getType());
        } else if (def instanceof Class) {
            // Consider classes void since ctors don't require a return statement
            return true;
        }
        throw BugException.unhandledDeclarationCase(def);
    }

    /** 
     * Generate a body for the overload method which delegates to the 
     * canonical method using a let to spubstitute defaulted parameters
     */
    private static int OL_BODY_DELEGATE_CANONICAL = 1<<0;
    /** 
     * Modifies OL_BODY_DELEGATE_CANONICAL so that the method is suitable 
     * for a companion class.
     */
    private static int OL_COMPANION = 1<<1;
    /** 
     * Modifies OL_BODY_DELEGATE_CANONICAL so that the canonical method on the 
     * interface instantce, {@code $this}, is called, rather than the 
     * canonical method on {@code this}.
     */ 
    private static int OL_DELEGATE_INTERFACE_INSTANCE = 1<<2;
    
    /** 
     * Abstraction over possible transformations for the body of an overloaded
     * declaration which supplies a defaulted argument.
     * @see DefaultedArgumentOverload 
     */
    abstract class DaoBody {
        
        protected List<JCExpression> makeTypeArguments(DefaultedArgumentOverload ol) {
            if (ol.defaultParameterMethodOnSelf() 
                    || ol.defaultParameterMethodOnOuter()) {
                return List.<JCExpression>nil();
            } else if (ol.defaultParameterMethodStatic()){
                return typeArguments((Functional)ol.getModel());
            } else {
                return List.<JCExpression>nil();
            }
        }

        abstract void makeBody(DefaultedArgumentOverload overloaded,
                MethodDefinitionBuilder overloadBuilder,
                ParameterList parameterList,
                Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList);

        JCExpression makeMethodNameQualifier() {
            return null;
        }
        
    }
    
    /** 
     * a body-less (i.e. abstract) transformation.
     */
    private class DaoAbstract extends DaoBody {
        @Override
        public void makeBody(DefaultedArgumentOverload overloaded,
                MethodDefinitionBuilder overloadBuilder,
                ParameterList parameterList,
                Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList) {
            overloadBuilder.noBody();
        }
    };
    final DaoAbstract daoAbstract = new DaoAbstract();
    /**
     * a transformation for an overloaded 
     * default parameter body which delegates to the "canonical"
     * method/constructor using a {@code let} expresssion
     * to substitute defaulted arguments
     */
    private class DaoThis extends DaoBody {
        public void makeBody(DefaultedArgumentOverload overloaded,
                MethodDefinitionBuilder overloadBuilder,
                ParameterList parameterList,
                Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList) {
            ListBuffer<JCExpression> args = ListBuffer.<JCExpression>lb();
            overloaded.appendImplicitArguments(typeParameterList, overloadBuilder, args);
            
            ListBuffer<JCStatement> vars = ListBuffer.<JCStatement>lb();
            
            boolean initedVars = false;
            boolean useDefault = false;
            for (Parameter parameterModel : parameterList.getParameters()) {
                if (currentParameter != null && parameterModel == currentParameter) {
                    useDefault = true;
                }
                if (useDefault) {
                    JCExpression defaultArgument;
                    if (Strategy.hasDefaultParameterValueMethod(parameterModel)) {
                        if (!initedVars) {
                            // Only call init vars if we actually invoke a defaulted param method
                            overloaded.initVars(currentParameter, vars);
                        }
                        initedVars = true;
                        JCExpression defaultValueMethodName = naming.makeDefaultedParamMethod(overloaded.makeDefaultArgumentValueMethodQualifier(), parameterModel);
                        at(null);
                        defaultArgument = make().Apply(makeTypeArguments(overloaded), 
                                defaultValueMethodName, 
                                ListBuffer.<JCExpression>lb().appendList(args).toList());
                    } else if (Strategy.hasEmptyDefaultArgument(parameterModel)) {
                        defaultArgument = makeEmptyAsSequential(true);
                    } else {
                        defaultArgument = makeErroneous(null, "compiler bug: parameter " + parameterModel.getName() + " has an unsupported default value");
                    }
                    Naming.SyntheticName varName = naming.temp(parameterModel.getName());
                    ProducedType paramType = overloaded.parameterType(parameterModel);
                    vars.append(makeVar(varName, 
                            makeJavaType(paramType, CodegenUtil.isUnBoxed(parameterModel.getModel()) ? 0 : JT_NO_PRIMITIVES), 
                            defaultArgument));
                    args.add(varName.makeIdent());
                } else {
                    args.add(naming.makeName(parameterModel.getModel(), Naming.NA_MEMBER | Naming.NA_ALIASED));
                }
            }
            makeBody(overloaded, overloadBuilder, args, vars);
        }
        
        protected final void makeBody(DefaultedArgumentOverload overloaded, MethodDefinitionBuilder overloadBuilder, ListBuffer<JCExpression> args, ListBuffer<JCStatement> vars) {
            JCExpression invocation = overloaded.makeInvocation(args);
            Declaration model = overloaded.getModel();// TODO Yuk
            if (!isVoid(model)
                    // MPL overloads always return a Callable
                    || (model instanceof Functional && Decl.isMpl((Functional) model))
                    || (model instanceof Method && !(Decl.isUnboxedVoid(model)))
                    || (model instanceof Method && Strategy.useBoxedVoid((Method)model)) 
                    || Strategy.generateInstantiator(model) && overloaded instanceof DefaultedArgumentInstantiator) {
                if (!vars.isEmpty()) {
                    invocation = make().LetExpr(vars.toList(), invocation);
                }
                overloadBuilder.body(make().Return(invocation));
            } else {
                vars.append(make().Exec(invocation));
                invocation = make().LetExpr(vars.toList(), makeNull());
                overloadBuilder.body(make().Exec(invocation));
            }
        }
    }
    final DaoThis daoThis = new DaoThis();
    
    /**
     * specialises {@link DaoThis} for transforming declarations for companion classes
     */
    private class DaoCompanion extends DaoThis {
        @Override
        protected final List<JCExpression> makeTypeArguments(DefaultedArgumentOverload ol) {
            return List.<JCExpression>nil();
        }
    }
    final DaoCompanion daoCompanion = new DaoCompanion();
    
    /**
     * a transformation for an overloaded 
     * default parameter method body which delegates
     * to the super class. This is used when we need to refine the return 
     * type of a DPM. 
     */
    private class DaoSuper extends DaoBody {

        JCExpression makeMethodNameQualifier() {
            return naming.makeSuper();
        }
        
        @Override
        void makeBody(
                DefaultedArgumentOverload overloaded,
                MethodDefinitionBuilder overloadBuilder,
                ParameterList parameterList,
                Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList) {
            
            ListBuffer<JCExpression> args = ListBuffer.<JCExpression>lb();
            for (Parameter parameter : parameterList.getParameters()) {
                if (currentParameter != null && parameter == currentParameter) {
                    break;
                }
                args.add(naming.makeUnquotedIdent(parameter.getName()));
            }
            JCExpression superCall = overloaded.makeInvocation(args);
            /*JCMethodInvocation superCall = make().Apply(null,
                    naming.makeQualIdent(naming.makeSuper(), ((Method)overloaded.getModel()).getName()),
                    args.toList());*/
            JCExpression refinedType = makeJavaType(((Method)overloaded.getModel()).getType(), JT_NO_PRIMITIVES);
            overloadBuilder.body(make().Return(make().TypeCast(refinedType, superCall)));
        }
    }
    final DaoSuper daoSuper = new DaoSuper();
    
    java.util.List<TypeParameter> typeParameterListModel(Tree.TypeParameterList typeParameterList) {
        java.util.List<TypeParameter> tpList = null;
        if (typeParameterList != null) {
            tpList = new ArrayList<TypeParameter>();
            for (TypeParameterDeclaration tpd : typeParameterList.getTypeParameterDeclarations()) {
                tpList.add(tpd.getDeclarationModel());
            }
        }
        return tpList;
    }
    
    /**
     * A base class for transformations used for Ceylon declarations
     * which have defaulted parameters. We generate an overloaded 
     * method/constructor whose implementation which supplies the default
     * argument and delegates to the "canonical" method.
     * 
     * Subclasses specialise for the different kinds of declaration, and 
     * a separate set of classes handle the various transformations for the 
     * body of an overloaded declaration (see {@link DaoBody})
     */
    abstract class DefaultedArgumentOverload {
        protected final DaoBody daoBody;
        
        protected DefaultedArgumentOverload(DaoBody daoBody){
            this.daoBody = daoBody;
        }
        
        protected abstract long getModifiers();

        protected abstract JCExpression makeMethodName();

        protected abstract void resultType(MethodDefinitionBuilder overloadBuilder);

        protected abstract void typeParameters(MethodDefinitionBuilder overloadBuilder);

        protected void parameters(MethodDefinitionBuilder overloadBuilder, ParameterList parameterList, Parameter currentParameter) {
            for (Parameter parameter : parameterList.getParameters()) {
                if (currentParameter != null && parameter == currentParameter) {
                    break;
                }
                overloadBuilder.parameter(parameter, null, 0, false);
            }
        }
        
        protected final void appendImplicitParameters(java.util.List<TypeParameter> typeParameterList,
                MethodDefinitionBuilder overloadBuilder) {
            if(typeParameterList != null){
                overloadBuilder.reifiedTypeParameters(typeParameterList);
            }
        }
        
        protected abstract void appendImplicitArguments(java.util.List<TypeParameter> typeParameterList,
                MethodDefinitionBuilder overloadBuilder, ListBuffer<JCExpression> args);
        
        protected ProducedType parameterType(Parameter parameterModel) {
            ProducedType paramType = null;
            if (parameterModel.getModel() instanceof Method) {
                paramType = typeFact().getCallableType(parameterModel.getType());
            } else {
                paramType = parameterModel.getType();
            }
            return paramType;
        }

        protected abstract void initVars(Parameter currentParameter, ListBuffer<JCStatement> vars);

        protected final boolean defaultParameterMethodOnSelf() {
            return Strategy.defaultParameterMethodOnSelf(getModel());
        }

        protected final boolean defaultParameterMethodOnOuter() {
            return Strategy.defaultParameterMethodOnOuter(getModel());
        }

        protected final boolean defaultParameterMethodStatic() {
            return Strategy.defaultParameterMethodStatic(getModel());
        }

        protected abstract Declaration getModel();

        protected JCExpression makeInvocation(ListBuffer<JCExpression> args) {
            final JCExpression methName = makeMethodName();
            return make().Apply(List.<JCExpression>nil(),
                    methName, args.toList());            
        }

        /** Returns the qualiifier to use when invoking the default parameter value method */
        protected abstract JCIdent makeDefaultArgumentValueMethodQualifier();
        
        
        /**
         * Generates an overloaded method or constructor.
         */
        public MethodDefinitionBuilder makeOverload (
                MethodDefinitionBuilder overloadBuilder,
                ParameterList parameterList,
                Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList) {

            // Make the declaration
            // need annotations for BC, but the method isn't really there
            overloadBuilder.ignoreModelAnnotations();
            overloadBuilder.modifiers(getModifiers());
            resultType(overloadBuilder);
            typeParameters(overloadBuilder);

            appendImplicitParameters(typeParameterList, overloadBuilder);
            parameters(overloadBuilder, parameterList, currentParameter);
            
            // Make the body
            // TODO MPL
            // TODO Type args on method call
            
            daoBody.makeBody(this, overloadBuilder,
                    parameterList,
                    currentParameter,
                    typeParameterList);
            
            return overloadBuilder;
        }
    }
    
    /**
     * A transformation for generating overloaded <em>methods</em> for 
     * defaulted arguments. 
     */
    class DefaultedArgumentMethod extends DefaultedArgumentOverload {
        private final Method method;

        DefaultedArgumentMethod(DaoBody daoBody, Method method) {
            super(daoBody);
            this.method = method;
        }

        @Override
        protected Method getModel() {
            return method;
        }
        
        @Override
        protected long getModifiers() {
            long mods = transformMethodDeclFlags(method);
            if (daoBody instanceof DaoAbstract == false) {
                mods &= ~ABSTRACT;
            }
            if (daoBody instanceof DaoCompanion) {
                mods |= FINAL;
            }
            return mods;
        }

        @Override
        protected final JCExpression makeMethodName() {
            int flags = Naming.NA_MEMBER;
            if (Decl.withinClassOrInterface(method)) {
                flags |= Naming.NA_CANONICAL_METHOD;
            }
            return naming.makeQualifiedName(daoBody.makeMethodNameQualifier(), method, flags);
        }

        @Override
        protected void resultType(MethodDefinitionBuilder overloadBuilder) {
            overloadBuilder.resultType(method, 0);
        }

        @Override
        protected void typeParameters(MethodDefinitionBuilder overloadBuilder) {
            copyTypeParameters(method, overloadBuilder);
        }

        @Override
        protected void appendImplicitArguments(java.util.List<TypeParameter> typeParameterList,
                MethodDefinitionBuilder overloadBuilder, ListBuffer<JCExpression> args) {
            if(typeParameterList != null){
                // we pass the reified type parameters along
                for(TypeParameter tp : typeParameterList){
                    args.append(makeUnquotedIdent(naming.getTypeArgumentDescriptorName(tp)));
                }
            }
        }

        @Override
        protected void initVars(Parameter currentParameter, ListBuffer<JCStatement> vars) {
        }

        @Override
        protected JCIdent makeDefaultArgumentValueMethodQualifier() {
            return null;
        }
    }
    
    /**
     * A transformation for generating overloaded <em>methods</em> for 
     * defaulted arguments. 
     */
    class DefaultedArgumentMethodTyped extends DefaultedArgumentMethod {
        private ProducedTypedReference typedMember;

        DefaultedArgumentMethodTyped(DaoBody daoBody, ProducedTypedReference typedMember) {
            super(daoBody, (Method)typedMember.getDeclaration());
            this.typedMember = typedMember;
        }

        @Override
        protected void resultType(MethodDefinitionBuilder overloadBuilder) {
            if (!isAnything(getModel().getType())
                    || !Decl.isUnboxedVoid(getModel())
                    || Strategy.useBoxedVoid((Method)getModel())) {
                ProducedTypedReference typedRef = (ProducedTypedReference) typedMember;
                overloadBuilder.resultTypeNonWidening(typedMember.getQualifyingType(), typedRef, typedMember.getType(), 0);
            } else {
                super.resultType(overloadBuilder);
            }
        }
        
        @Override
        protected void parameters(MethodDefinitionBuilder overloadBuilder, ParameterList parameterList, Parameter currentParameter) {
            for (Parameter param : parameterList.getParameters()) {
                if (currentParameter != null && param == currentParameter) {
                    break;
                }
                ProducedType type = paramType(param);
                overloadBuilder.parameter(param, type, FINAL, 0, true);
            }
        }
        
        @Override
        protected ProducedType parameterType(Parameter parameter) {
            ProducedType paramType = paramType(parameter);
            if (parameter.getModel() instanceof Method) {
                paramType = typeFact().getCallableType(parameter.getType());
            }
            return paramType;
        }

        private ProducedType paramType(Parameter parameter) {
            final ProducedTypedReference typedParameter = typedMember.getTypedParameter(parameter);
            ProducedType paramType;
            // if the supertype method itself got erased to Object, we can't do better than this
            if (gen().willEraseToObject(parameter.getType()) && !gen().willEraseToBestBounds(parameter)) {
                paramType = typeFact().getObjectDeclaration().getType();
            } else {
                paramType = typedParameter.getType();
            }
            return paramType;
        }

        @Override
        public MethodDefinitionBuilder makeOverload(
                MethodDefinitionBuilder overloadBuilder,
                ParameterList parameterList, Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList) {
            overloadBuilder.isOverride(true);
            return super.makeOverload(overloadBuilder, parameterList, currentParameter,
                    typeParameterList);
        }
        
        
    }
    
    /**
     * A transformation for generating the canonical <em>method</em> used by the
     * defaulted argument overload methods
     */
    class CanonicalMethod extends DefaultedArgumentMethod {
        private List<JCStatement> body;
        private boolean useBody;
        
        CanonicalMethod(DaoBody daoBody, Method method) {
            super(daoBody, method);
            useBody = false;
        }
        
        CanonicalMethod(DaoBody daoBody, Method method, List<JCStatement> body) {
            super(daoBody, method);
            this.body = body;
            useBody = true;
        }
        
        @Override
        protected long getModifiers() {
            long mods = super.getModifiers();
            if (useBody) {
                mods = mods & ~PUBLIC & ~FINAL | PRIVATE;
            }
            return mods;
        }
        
        /**
         * Generates a canonical method
         */
        public MethodDefinitionBuilder makeOverload (
                MethodDefinitionBuilder canonicalBuilder,
                ParameterList parameterList,
                Parameter currentParameter,
                java.util.List<TypeParameter> typeParameterList) {

            if (!useBody) {
                daoBody.makeBody(this, canonicalBuilder,
                        parameterList,
                        currentParameter,
                        typeParameterList);
            } else {
                // Make the declaration
                // need annotations for BC, but the method isn't really there
                canonicalBuilder.ignoreModelAnnotations();
                canonicalBuilder.modifiers(getModifiers());
                resultType(canonicalBuilder);
                typeParameters(canonicalBuilder);

                appendImplicitParameters(typeParameterList, canonicalBuilder);
                parameters(canonicalBuilder, parameterList, currentParameter);
                
                if (body != null) {
                    // Construct the outermost method using the body we've built so far
                    canonicalBuilder.body(body);
                } else {
                    canonicalBuilder.noBody();
                }
            }
            
            return canonicalBuilder;
        }

    }
    
    /**
     * A base class for transformations that generate overloaded declarations for 
     * defaulted arguments. 
     */
    abstract class DefaultedArgumentClass extends DefaultedArgumentOverload {
        
        protected final Class klass;
        
        protected Naming.SyntheticName companionInstanceName = null;

        DefaultedArgumentClass(DaoBody daoBody, Class klass) {
            super(daoBody);
            this.klass = klass;
        }
        
        @Override
        protected final Class getModel() {
            return klass;
        }
        
        @Override
        protected void initVars(Parameter currentParameter, ListBuffer<JCStatement> vars) {
            if (!Strategy.defaultParameterMethodStatic(klass)
                    && !Strategy.defaultParameterMethodOnOuter(klass)
                    && currentParameter != null) {
                companionInstanceName = naming.temp("impl");
                vars.append(makeVar(companionInstanceName, 
                        makeJavaType(klass.getType(), AbstractTransformer.JT_COMPANION),
                        make().NewClass(null, 
                                null,
                                makeJavaType(klass.getType(), AbstractTransformer.JT_CLASS_NEW | AbstractTransformer.JT_COMPANION),
                                List.<JCExpression>nil(), null)));
            }
        }
        
        @Override
        protected JCIdent makeDefaultArgumentValueMethodQualifier() {
            if (defaultParameterMethodOnSelf() 
                    || defaultParameterMethodOnOuter()
                    || daoBody instanceof DaoCompanion) {
                return null;
            } else if (defaultParameterMethodStatic()){
                return null;
            } else {
                return companionInstanceName.makeIdent();
            }
        }
    }
    
    /**
     * A transformation for generating overloaded <em>constructors</em> for 
     * defaulted arguments. 
     */
    class DefaultedArgumentConstructor extends DefaultedArgumentClass {

        DefaultedArgumentConstructor(DaoBody daoBody, Class klass) {
            super(daoBody, klass);
        }

        @Override
        protected long getModifiers() {
            return transformClassDeclFlags(klass) & (PUBLIC | PRIVATE | PROTECTED);
        }

        @Override
        protected JCExpression makeMethodName() {
            return naming.makeQualifiedThis(daoBody.makeMethodNameQualifier());
        }

        @Override
        protected void resultType(MethodDefinitionBuilder overloadBuilder) {
            // Constructor has no result type
        }

        @Override
        protected void typeParameters(MethodDefinitionBuilder overloadBuilder) {
            // Constructor has type parameters
        }
        
        @Override
        protected void appendImplicitArguments(java.util.List<TypeParameter> typeParameterList,
                MethodDefinitionBuilder overloadBuilder, ListBuffer<JCExpression> args) {
            if(typeParameterList != null){
                // we pass the reified type parameters along
                for(TypeParameter tp : typeParameterList){
                    args.append(makeUnquotedIdent(naming.getTypeArgumentDescriptorName(tp)));
                }
            }
        }
    }
    
    /**
     * A transformation for generating overloaded <em>instantiator methods</em> for 
     * defaulted arguments. 
     */
    class DefaultedArgumentInstantiator extends DefaultedArgumentClass {

        DefaultedArgumentInstantiator(DaoBody daoBody, Class klass) {
            super(daoBody, klass);
        }

        @Override
        protected long getModifiers() {
            // remove the FINAL bit in case it gets set, because that is valid for a class decl, but
            // not for a method if in an interface
            long modifiers = transformClassDeclFlags(klass) & ~FINAL;
            // alias classes cannot be abstract since they're placeholders, but it's possible to have formal class aliases
            // and the instantiator method needs the abstract bit
            if(klass.isFormal() && klass.isAlias())
                modifiers |= ABSTRACT;
            return modifiers;
            
        }

        @Override
        protected JCExpression makeMethodName() {
            return naming.makeInstantiatorMethodName(daoBody.makeMethodNameQualifier(), klass);
        }

        @Override
        protected void resultType(MethodDefinitionBuilder overloadBuilder) {
            /* Not actually part of the return type */
            overloadBuilder.ignoreModelAnnotations();
            if (!klass.isAlias() 
                    && Strategy.generateInstantiator(klass.getExtendedTypeDeclaration())
                    && klass.isActual()
                    && klass.getExtendedTypeDeclaration().getContainer() instanceof Class) {
                overloadBuilder.isOverride(true);
            }
            /**/
            
            JCExpression resultType;
            ProducedType type = klass.isAlias() ? klass.getExtendedType() : klass.getType();
            if (Strategy.isInstantiatorUntyped(klass)) {
                // We can't expose a local type name to a place it's not visible
                resultType = make().Type(syms().objectType);
            } else {
                resultType = makeJavaType(type);
            }
            overloadBuilder.resultType(null, resultType);
        }

        @Override
        protected void typeParameters(MethodDefinitionBuilder overloadBuilder) {
            for (TypeParameter tp : typeParametersForInstantiator(klass)) {
                overloadBuilder.typeParameter(tp);
            }
        }
        
        @Override
        protected void appendImplicitArguments(java.util.List<TypeParameter> typeParameterList,
                MethodDefinitionBuilder overloadBuilder, ListBuffer<JCExpression> args) {
            ProducedType type = klass.isAlias() ? klass.getExtendedType() : klass.getType();
            type = type.resolveAliases();
            // fetch the type parameters from the klass we're instantiating itself if any
            for(ProducedType pt : type.getTypeArgumentList()){
                args.append(makeReifiedTypeArgument(pt));
            }
        }

        @Override
        protected JCExpression makeInvocation(ListBuffer<JCExpression> args) {
            ProducedType type = klass.isAlias() ? klass.getExtendedType() : klass.getType();
            return make().NewClass(null, 
                    null, 
                    makeJavaType(type, JT_CLASS_NEW | JT_NON_QUALIFIED),
                    args.toList(),
                    null);
        }
    }

    /**
     * When generating an instantiator method if the inner class has a type 
     * parameter with the same name as a type parameter of an outer type, then the 
     * instantiator method shouldn't declare its own type parameter of that 
     * name -- it should use the captured one. This method filters out the
     * type parameters of the inner class which are the same as type parameters 
     * of the outer class so that they can be captured.
     */
    private java.util.List<TypeParameter> typeParametersForInstantiator(final Class model) {
        if (!Strategy.generateInstantiator(model)) {
            throw new BugException();
        }
        java.util.List<TypeParameter> filtered = new ArrayList<TypeParameter>();
        java.util.List<TypeParameter> tps = model.getTypeParameters();
        if (tps != null) {
            for (TypeParameter tp : tps) {
                boolean omit = false;
                Scope s = model.getContainer();
                while (!(s instanceof Package)) {
                    if (s instanceof Generic) {
                        for (TypeParameter outerTp : ((Generic)s).getTypeParameters()) {
                            if (tp.getName().equals(outerTp.getName())) {
                                omit = true;
                            }
                        }
                    }
                    s = s.getContainer();
                }
                if (!omit) {
                    filtered.add(tp);
                }
            }
        }
        return filtered;
    }

    private java.util.List<TypeParameter> typeParametersOfAllContainers(final ClassOrInterface model, boolean includeModelTypeParameters) {
        java.util.List<java.util.List<TypeParameter>> r = new ArrayList<java.util.List<TypeParameter>>(1);
        Scope s = model.getContainer();
        while (!(s instanceof Package)) {
            if (s instanceof Generic) {
                r.add(0, ((Generic)s).getTypeParameters());
            }
            s = s.getContainer();
        }
        Set<String> names = new HashSet<String>();
        for(TypeParameter tp : model.getTypeParameters()){
            names.add(tp.getName());
        }
        java.util.List<TypeParameter> result = new ArrayList<TypeParameter>(1);
        for (java.util.List<TypeParameter> tps : r) {
            for(TypeParameter tp : tps){
                if(names.add(tp.getName())){
                    result.add(tp);
                }
            }
        }
        if(includeModelTypeParameters)
            result.addAll(model.getTypeParameters());
        return result;
    }
    
    
    /**
     * Creates a (possibly abstract) method for retrieving the value for a 
     * defaulted parameter
     * @param typeParameterList 
     */
    MethodDefinitionBuilder makeParamDefaultValueMethod(boolean noBody, Declaration container, 
            Tree.ParameterList params, Tree.Parameter currentParam, TypeParameterList typeParameterList) {
        at(currentParam);
        Parameter parameter = currentParam.getParameterModel();
        if (!Strategy.hasDefaultParameterValueMethod(parameter)) {
            throw new BugException();
        }
        MethodDefinitionBuilder methodBuilder = MethodDefinitionBuilder.systemMethod(this, Naming.getDefaultedParamMethodName(container, parameter));
        methodBuilder.ignoreModelAnnotations();
        if (container != null && Decl.isAnnotationConstructor(container)) {
            AnnotationInvocation ac = (AnnotationInvocation)((Method)container).getAnnotationConstructor();
            for (AnnotationConstructorParameter acp : ac.getConstructorParameters()) {
                if (acp.getParameter().equals(parameter)
                        && acp.getDefaultArgument() != null) {
                    methodBuilder.userAnnotations(acp.getDefaultArgument().makeDpmAnnotations(expressionGen()));
                }
            }
        }
        int modifiers = 0;
        if (noBody) {
            modifiers |= PUBLIC | ABSTRACT;
        } else if (container == null
                || !(container instanceof Class 
                        && Strategy.defaultParameterMethodStatic(container))) {
            // initializers can override parameter defaults
            modifiers |= FINAL;
        }
        if (container != null && container.isShared()) {
            modifiers |= PUBLIC;
        } else if (container == null || (!container.isToplevel()
                && !noBody)){
            modifiers |= PRIVATE;
        }
        if (Strategy.defaultParameterMethodStatic(container)) {
            // static default parameter methods should be consistently public so that if non-shared class Top and
            // shared class Bottom which extends Top both have the same default param name, we don't get an error
            // if the Bottom class tries to "hide" a static public method with a private one
            modifiers |= STATIC | PUBLIC;
        }
        methodBuilder.modifiers(modifiers);
        
        if (container instanceof Functional) {
            copyTypeParameters((Functional)container, methodBuilder);
        }
        
        // make sure reified type parameters are accepted
        if(typeParameterList != null)
            methodBuilder.reifiedTypeParameters(typeParameterListModel(typeParameterList));
        
        // Add any of the preceding parameters as parameters to the method
        for (Tree.Parameter p : params.getParameters()) {
            if (p == currentParam) {
                break;
            }
            at(p);
            methodBuilder.parameter(p.getParameterModel(), null, 0, container instanceof Class);
        }

        // The method's return type is the same as the parameter's type
        methodBuilder.resultType(parameter.getModel(), parameter.getType(), 0);

        // The implementation of the method
        if (noBody) {
            methodBuilder.noBody();
        } else {
            HasErrorException error = errors().getFirstExpressionErrorAndMarkBrokenness(Decl.getDefaultArgument(currentParam).getExpression());
            if (error != null) {
                methodBuilder.body(error.makeThrow(this));
            } else {
                JCExpression expr = expressionGen().transform(currentParam);
                JCBlock body = at(currentParam).Block(0, List.<JCStatement> of(at(currentParam).Return(expr)));
                methodBuilder.block(body);
            }
        }

        return methodBuilder;
    }

    public List<JCTree> transformObjectDefinition(Tree.ObjectDefinition def, ClassDefinitionBuilder containingClassBuilder) {
        return transformObject(def, def.getSatisfiedTypes(), def.getDeclarationModel(), 
                def.getAnonymousClass(), containingClassBuilder, Decl.isLocalNotInitializer(def));
    }
    
    public List<JCTree> transformObjectArgument(Tree.ObjectArgument def) {
        return transformObject(def, def.getSatisfiedTypes(), def.getDeclarationModel(), 
                def.getAnonymousClass(), null, false);
    }
    
    private List<JCTree> transformObject(Tree.StatementOrArgument def,
            Tree.SatisfiedTypes satisfiesTypes,
            Value model, 
            Class klass,
            ClassDefinitionBuilder containingClassBuilder,
            boolean makeLocalInstance) {
        naming.clearSubstitutions(model);
        
        String name = model.getName();
        ClassDefinitionBuilder objectClassBuilder = ClassDefinitionBuilder.object(
                this, name, Decl.isLocal(model)).forDefinition(klass);
        
        CeylonVisitor visitor = gen().visitor;
        final ListBuffer<JCTree> prevDefs = visitor.defs;
        final boolean prevInInitializer = visitor.inInitializer;
        final ClassDefinitionBuilder prevClassBuilder = visitor.classBuilder;
        List<JCStatement> childDefs;
        try {
            visitor.defs = new ListBuffer<JCTree>();
            visitor.inInitializer = true;
            visitor.classBuilder = objectClassBuilder;
            
            def.visitChildren(visitor);
            childDefs = (List<JCStatement>)visitor.getResult().toList();
        } finally {
            visitor.classBuilder = prevClassBuilder;
            visitor.inInitializer = prevInInitializer;
            visitor.defs = prevDefs;
        }

        satisfaction(satisfiesTypes, klass, objectClassBuilder);
        serialization(klass, objectClassBuilder);
        TypeDeclaration decl = model.getType().getDeclaration();

        if (Decl.isToplevel(model)
                && def instanceof Tree.ObjectDefinition) {
            // generate a field and getter
            AttributeDefinitionBuilder builder = AttributeDefinitionBuilder
                    // TODO attr build take a JCExpression className
                    .wrapped(this, null, model.getName(), model, true)
                    .userAnnotations(makeAtIgnore())
                    .userAnnotationsSetter(makeAtIgnore())
                    .immutable()
                    .initialValue(makeNewClass(naming.makeName(model, Naming.NA_FQ | Naming.NA_WRAPPER)))
                    .is(PUBLIC, Decl.isShared(decl))
                    .is(STATIC, true);
            if (def instanceof Tree.ObjectDefinition) {
                builder.userAnnotations(expressionGen().transform(((Tree.ObjectDefinition) def).getAnnotationList()));
            }            
            objectClassBuilder.defs(builder.build());
        }

        // Make sure top types satisfy reified type
        addReifiedTypeInterface(objectClassBuilder, klass);
        
        // make sure we set the container in case we move it out
        addAtContainer(objectClassBuilder, klass);

        List<JCTree> result = objectClassBuilder
            .annotations(makeAtObject())
            .modelAnnotations(model.getAnnotations())
            .modifiers(transformObjectDeclFlags(model))
            .constructorModifiers(PRIVATE)
            .satisfies(decl.getSatisfiedTypes())
            .init(childDefs)
            .addGetTypeMethod(model.getType())
            .build();
        
        if (makeLocalInstance) {
            if(model.isSelfCaptured()){
                // if it's captured we need to box it and define the var before the class, so it can access it
                JCNewClass newInstance = makeNewClass(objectClassBuilder.getClassName(), false, null);
                JCFieldAccess setter = naming.makeSelect(Naming.getLocalValueName(model), Naming.getSetterName(model));
                JCStatement assign = make().Exec(make().Assign(setter, newInstance));
                result = result.prepend(assign);

                JCVariableDecl localDecl = makeVariableBoxDecl(null, model);
                result = result.prepend(localDecl);
            }else{
                // not captured, we can define the var after the class
                JCVariableDecl localDecl = makeLocalIdentityInstance(name, objectClassBuilder.getClassName(), false);
                result = result.append(localDecl);
            }
            
        } else if (Decl.withinClassOrInterface(model)) {
            boolean visible = Decl.isCaptured(model);
            int modifiers = FINAL | ((visible) ? PRIVATE : 0);
            JCExpression type = makeJavaType(klass.getType());
            JCExpression initialValue = makeNewClass(makeJavaType(klass.getType()), null);
            containingClassBuilder.field(modifiers, name, type, initialValue, !visible);
            
            if (visible) {
                AttributeDefinitionBuilder getter = AttributeDefinitionBuilder
                .getter(this, name, model)
                .modifiers(transformAttributeGetSetDeclFlags(model, false));
                if (def instanceof Tree.ObjectDefinition) {
                    getter.userAnnotations(expressionGen().transform(((Tree.ObjectDefinition)def).getAnnotationList()));
                }
                
                result = result.appendList(getter.build());
            }
        }
        
        return result;
    }
    
    /**
     * Makes a {@code main()} method which calls the given top-level method
     * @param def
     */
    private MethodDefinitionBuilder makeMainForClass(ClassOrInterface model) {
        at(null);
        if(model.isAlias())
            model = model.getExtendedTypeDeclaration();
        JCExpression nameId = makeJavaType(model.getType(), JT_RAW);
        List<JCExpression> arguments = makeBottomReifiedTypeParameters(model.getTypeParameters());
        JCNewClass expr = make().NewClass(null, null, nameId, arguments, null);
        return makeMainMethod(model, expr);
    }
    
    /**
     * Makes a {@code main()} method which calls the given top-level method
     * @param method
     */
    private MethodDefinitionBuilder makeMainForFunction(Method method) {
        at(null);
        JCExpression qualifiedName = naming.makeName(method, Naming.NA_FQ | Naming.NA_WRAPPER | Naming.NA_MEMBER);
        List<JCExpression> arguments = makeBottomReifiedTypeParameters(method.getTypeParameters());
        MethodDefinitionBuilder mainMethod = makeMainMethod(method, make().Apply(null, qualifiedName, arguments));
        return mainMethod;
    }
    
    private List<JCExpression> makeBottomReifiedTypeParameters(
            java.util.List<TypeParameter> typeParameters) {
        List<JCExpression> arguments = List.nil();
        for(int i=typeParameters.size()-1;i>=0;i--){
            arguments = arguments.prepend(gen().makeNothingTypeDescriptor());
        }
        return arguments;
    }

    /** 
     * Makes a {@code main()} method which calls the given callee 
     * (a no-args method or class)
     * @param decl
     * @param callee
     */
    private MethodDefinitionBuilder makeMainMethod(Declaration decl, JCExpression callee) {
        // Add a main() method
        MethodDefinitionBuilder methbuilder = MethodDefinitionBuilder
                .main(this)
                .ignoreModelAnnotations();
        // Add call to process.setupArguments
        JCExpression argsId = makeUnquotedIdent("args");
        JCMethodInvocation processExpr = make().Apply(null, naming.makeLanguageValue("process"), List.<JCTree.JCExpression>nil());
        methbuilder.body(make().Exec(make().Apply(null, makeSelect(processExpr, "setupArguments"), List.<JCTree.JCExpression>of(argsId))));
        // Add call to toplevel method
        methbuilder.body(make().Exec(callee));
        return methbuilder;
    }
    
    void copyTypeParameters(Functional def, MethodDefinitionBuilder methodBuilder) {
        if (def.getTypeParameters() != null) {
            for (TypeParameter t : def.getTypeParameters()) {
                methodBuilder.typeParameter(t);
            }
        }
    }

    public List<JCTree> transform(final Tree.TypeAliasDeclaration def) {
        final TypeAlias model = def.getDeclarationModel();
        
        // we only create types for aliases so they can be imported with the model loader
        // and since we can't import local declarations let's just not create those types
        // in that case
        if(Decl.isAncestorLocal(def))
            return List.nil();
        
        naming.clearSubstitutions(model);
        String ceylonClassName = def.getIdentifier().getText();
        final String javaClassName = Naming.quoteClassName(def.getIdentifier().getText());

        ClassDefinitionBuilder classBuilder = ClassDefinitionBuilder
                .klass(this, javaClassName, ceylonClassName, Decl.isLocal(model));

        // class alias
        classBuilder.constructorModifiers(PRIVATE);
        classBuilder.annotations(makeAtTypeAlias(model.getExtendedType()));
        classBuilder.annotations(expressionGen().transform(def.getAnnotationList()));
        classBuilder.isAlias(true);

        // make sure we set the container in case we move it out
        addAtContainer(classBuilder, model);

        visitClassOrInterfaceDefinition(def, classBuilder);

        return classBuilder
            .modelAnnotations(model.getAnnotations())
            .modifiers(transformTypeAliasDeclFlags(model))
            .satisfies(model.getSatisfiedTypes())
            .build();
    }
}
