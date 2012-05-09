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
package com.redhat.ceylon.compiler.java.test.structure;

import org.junit.Ignore;
import org.junit.Test;

import com.redhat.ceylon.compiler.java.test.CompilerTest;

public class StructureTest extends CompilerTest {
    
    //
    // Packages
    
    @Test
    public void testPkgPackage(){
        compareWithJavaSource("pkg/pkg");
    }

    @Test
    public void testPkgPackageMetadata(){
        compareWithJavaSource("pkg/package");
    }

    //
    // Modules
    
    @Test
    public void testMdlModule(){
        compareWithJavaSource("module/single/module");
    }

    //
    // Attributes
    
    @Test
    public void testAtrClassAttribute(){
        compareWithJavaSource("attribute/ClassAttribute");
    }
    @Test
    public void testAtrClassAttributeWithInitializer(){
        compareWithJavaSource("attribute/ClassAttributeWithInitializer");
    }
    @Test
    public void testAtrClassAttributeGetter(){
        compareWithJavaSource("attribute/ClassAttributeGetter");
    }
    @Test
    public void testAtrClassAttributeGetterSetter(){
        compareWithJavaSource("attribute/ClassAttributeGetterSetter");
    }
    @Test
    public void testAtrClassVariable(){
        compareWithJavaSource("attribute/ClassVariable");
    }
    @Test
    public void testAtrClassVariableWithInitializer(){
        compareWithJavaSource("attribute/ClassVariableWithInitializer");
    }
    @Test
    public void testAtrInnerAttributeGetter(){
        compareWithJavaSource("attribute/InnerAttributeGetter");
    }
    @Test
    public void testAtrInnerAttributeGetterSetter(){
        compareWithJavaSource("attribute/InnerAttributeGetterSetter");
    }
    @Test
    public void testAtrInnerAttributeGetterLateInitialisation(){
        compareWithJavaSource("attribute/InnerAttributeGetterLateInitialisation");
    }
    @Test
    public void testAtrClassAttributeWithConflictingMethods(){
        compareWithJavaSource("attribute/ClassAttributeWithConflictingMethods");
    }
    @Test
    public void testAtrInnerAttributeGetterWithConflictingMethods(){
        compareWithJavaSource("attribute/InnerAttributeGetterWithConflictingMethods");
    }
    
    //
    // Classes
    
    @Test
    public void testKlsAbstractFormal(){
        compareWithJavaSource("klass/AbstractFormal");
    }
    @Test
    public void testKlsCaseTypes(){
        compareWithJavaSource("klass/CaseTypes");
    }
    @Test
    public void testKlsDefaultedInitializerParameter(){
        compareWithJavaSource("klass/DefaultedInitializerParameter");
    }
    @Test
    public void testKlsExtends(){
        compareWithJavaSource("klass/Extends");
    }
    @Test
    public void testKlsExtendsGeneric(){
        compareWithJavaSource("klass/ExtendsGeneric");
    }
    @Test
    public void testKlsInitializerParameter(){
        compareWithJavaSource("klass/InitializerParameter");
    }
    @Test
    public void testKlsInitializerVarargs(){
        compareWithJavaSource("klass/InitializerVarargs");
    }
    @Test
    public void testKlsInnerClass(){
        compareWithJavaSource("klass/InnerClass");
    }
    @Test
    public void testKlsInterface(){
        compareWithJavaSource("klass/Interface");
    }
    @Ignore("M3")
    @Test
    public void testKlsInterfaceWithConcreteMembers(){
        compareWithJavaSource("klass/InterfaceWithConcreteMembers");
    }
    @Test
    public void testKlsInterfaceWithMembers(){
        compareWithJavaSource("klass/InterfaceWithMembers");
    }
    @Test
    public void testKlsClass(){
        compareWithJavaSource("klass/Klass");
    }
    @Test
    public void testKlsKlassMethodTypeParams(){
        compareWithJavaSource("klass/KlassMethodTypeParams");
    }
    @Test
    public void testKlsKlassTypeParams(){
        compareWithJavaSource("klass/KlassTypeParams");
    }
    @Test
    public void testKlsKlassTypeParamsSatisfies(){
        compareWithJavaSource("klass/KlassTypeParamsSatisfies");
    }
    @Test
    public void testKlsKlassWithObjectMember(){
        compareWithJavaSource("klass/KlassWithObjectMember");
    }
    @Test
    public void testKlsLocalClass(){
        compareWithJavaSource("klass/LocalClass");
    }
    @Test
    public void testKlsDoublyLocalClass(){
        compareWithJavaSource("klass/DoublyLocalClass");
    }
    @Test
    public void testKlsLocalClassWithLocalObject(){
        compareWithJavaSource("klass/LocalClassWithLocalObject");
    }
    @Test
    public void testKlsPublicClass(){
        compareWithJavaSource("klass/PublicKlass");
    }
    @Test
    public void testKlsSatisfies(){
        compareWithJavaSource("klass/Satisfies");
    }
    @Test
    public void testKlsSatisfiesErasure(){
        compareWithJavaSource("klass/SatisfiesErasure");
    }
    @Test
    public void testKlsSatisfiesGeneric(){
        compareWithJavaSource("klass/SatisfiesGeneric");
    }
    @Test
    public void testKlsSatisfiesWithMembers(){
        compareWithJavaSource("klass/SatisfiesWithMembers");
    }
    @Test
    public void testKlsRefinedVarianceInheritance(){
        // See https://github.com/ceylon/ceylon-compiler/issues/319
        //compareWithJavaSource("klass/RefinedVarianceInheritance");
        compileAndRun("com.redhat.ceylon.compiler.java.test.structure.klass.rvi_run", "klass/RefinedVarianceInheritance.ceylon");
    }
    @Test
    public void testKlsRefinedVarianceInheritance2(){
        // See https://github.com/ceylon/ceylon-compiler/issues/354
        compareWithJavaSource("klass/RefinedVarianceInheritance2");
    }
    @Test
    public void testKlsVariance(){
        compareWithJavaSource("klass/Variance");
    }
    @Test
    public void testKlsObjectInMethod(){
        compareWithJavaSource("klass/ObjectInMethod");
    }
    @Test
    public void testKlsObjectInStatement(){
        compareWithJavaSource("klass/ObjectInStatement");
    }
    @Test
    public void testKlsInitializerObjectInStatement(){
        compareWithJavaSource("klass/InitializerObjectInStatement");
    }
    @Test
    public void testKlsKlassInStatement(){
        compareWithJavaSource("klass/KlassInStatement");
    }
    @Test
    public void testKlsInitializerKlassInStatement(){
        compareWithJavaSource("klass/InitializerKlassInStatement");
    }
    @Test
    public void testKlsObjectInGetter(){
        compareWithJavaSource("klass/ObjectInGetter");
    }
    @Test
    public void testKlsObjectInSetter(){
        compareWithJavaSource("klass/ObjectInSetter");
    }
    @Test
    public void testKlsClassInGetter(){
        compareWithJavaSource("klass/KlassInGetter");
    }
    @Test
    public void testKlsClassInSetter(){
        compareWithJavaSource("klass/KlassInSetter");
    }
    @Test
    public void testKlsInnerClassUsingOutersTypeParam(){
        compareWithJavaSource("klass/InnerClassUsingOutersTypeParam");
    }
    @Test
    public void testKlsInnerClassUsingOutersTypeParam2(){
        compareWithJavaSource("klass/InnerClassUsingOutersTypeParam2");
    }
    
    //
    // Methods
    
    @Test
    public void testMthLocalMethod(){
        compareWithJavaSource("method/LocalMethod");
    }
    @Test
    public void testMthMethod(){
        compareWithJavaSource("method/Method");
    }
    @Test
    public void testMthMethodErasure(){
        compareWithJavaSource("method/MethodErasure");
    }
    @Test
    public void testMthMethodTypeParams(){
        compareWithJavaSource("method/MethodTypeParams");
    }
    @Test
    public void testMthMethodWithDefaultParams(){
        compareWithJavaSource("method/MethodWithDefaultParams");
    }
    @Test
    public void testMthMethodWithLocalObject(){
        compareWithJavaSource("method/MethodWithLocalObject");
    }
    @Test
    public void testMthMethodWithParam(){
        compareWithJavaSource("method/MethodWithParam");
    }
    @Test
    public void testMthMethodWithVarargs(){
        compareWithJavaSource("method/MethodWithVarargs");
    }
    @Test
    public void testMthPublicMethod(){
        compareWithJavaSource("method/PublicMethod");
    }
    @Test
    public void testMthFunctionInStatement(){
        compareWithJavaSource("method/FunctionInStatement");
    }
    @Test
    public void testMthFunctionInGetter(){
        compareWithJavaSource("method/FunctionInGetter");
    }
    @Test
    public void testMthFunctionInSetter(){
        compareWithJavaSource("method/FunctionInSetter");
    }
    @Test
    public void testMthMethodSpecifyingNullaryTopLevel(){
        compareWithJavaSource("method/MethodSpecifyingNullaryTopLevel");
    }
    @Test
    public void testMthMethodSpecifyingUnaryTopLevel(){
        compareWithJavaSource("method/MethodSpecifyingUnaryTopLevel");
    }
    @Test
    public void testMthMethodSpecifyingTopLevelWithResult(){
        compareWithJavaSource("method/MethodSpecifyingTopLevelWithResult");
    }
    @Test
    public void testMthMethodSpecifyingCallable(){
        compareWithJavaSource("method/MethodSpecifyingCallable");
    }
    @Test
    public void testMthMethodSpecifyingInitializer(){
        compareWithJavaSource("method/MethodSpecifyingInitializer");
    }
    
    @Test
    public void testMthMethodSpecifyingTopLevelWithTypeParam(){
        compareWithJavaSource("method/MethodSpecifyingTopLevelWithTypeParam");
    }
    @Test
    public void testMthMethodSpecifyingTopLevelWithTypeParamMixed(){
        compareWithJavaSource("method/MethodSpecifyingTopLevelWithTypeParamMixed");
    }
    @Test
    public void testMthMethodSpecifyingMethod(){
        compareWithJavaSource("method/MethodSpecifyingMethod");
    }
    @Test
    public void testMthMethodSpecifyingGetter(){
        compareWithJavaSource("method/MethodSpecifyingGetter");
    }
    @Test
    @Ignore("Awaiting fix for ceylon-spec#218")
    public void testMthMethodSpecifyingInitParam(){
        compareWithJavaSource("method/MethodSpecifyingInitParam");
    }
    @Test
    public void testMthRefinedMethodSpecifyingTopLevel(){
        compareWithJavaSource("method/RefinedMethodSpecifyingTopLevel");
    }
    @Test
    public void testMthLocalMethodSpecifyingMethod(){
        compareWithJavaSource("method/LocalMethodSpecifyingMethod");
    }
    @Test
    public void testMthLocalMethodSpecifyingParam(){
        compareWithJavaSource("method/LocalMethodSpecifyingParam");
    }
    @Test
    public void testMthVarargsMethodSpecifyingMethodWithSequence(){
        compareWithJavaSource("method/VarargsMethodSpecifyingMethodWithSequence");
    }
    @Test
    public void testMthVarargsMethodSpecifyingMethodWithVarargs(){
        compareWithJavaSource("method/VarargsMethodSpecifyingMethodWithVarargs");
    }
    @Test
    public void testMthSequenceMethodSpecifyingMethodWithVarargs(){
        compareWithJavaSource("method/SequenceMethodSpecifyingMethodWithVarargs");
    }
    @Test
    public void testTwoParamLists(){
        compareWithJavaSource("method/TwoParamLists");
    }
    @Test
    public void testThreeParamLists(){
        compareWithJavaSource("method/ThreeParamLists");
    }
    @Test
    public void testCallableEscaping(){
        compareWithJavaSource("method/CallableEscaping");
    }
    
    //
    // Toplevel
    
    @Test
    public void testTopToplevelAttribute(){
        compareWithJavaSource("toplevel/ToplevelAttribute");
    }
    @Test
    public void testTopToplevelAttributeGenerics(){
        compareWithJavaSource("toplevel/ToplevelAttributeGenerics");
    }
    @Test
    public void testTopToplevelAttributeShared(){
        compareWithJavaSource("toplevel/ToplevelAttributeShared");
    }
    @Test
    public void testTopToplevelGetter(){
        compareWithJavaSource("toplevel/ToplevelGetter");
    }
    @Test
    public void testTopToplevelGetterSetter(){
        compareWithJavaSource("toplevel/ToplevelGetterSetter");
    }
    @Test
    public void testTopToplevelMethods(){
        compareWithJavaSource("toplevel/ToplevelMethods");
    }
    @Test
    public void testTopToplevelMethodWithDefaultedParams(){
        compareWithJavaSource("toplevel/ToplevelMethodWithDefaultedParams");
    }
    @Test
    public void testTopToplevelObject(){
        compareWithJavaSource("toplevel/ToplevelObject");
    }
    @Test
    public void testTopToplevelObjectWithMembers(){
        compareWithJavaSource("toplevel/ToplevelObjectWithMembers");
    }
    @Test
    public void testTopToplevelObjectShared(){
        compareWithJavaSource("toplevel/ToplevelObjectShared");
    }
    @Test
    public void testTopToplevelObjectWithSupertypes(){
        compareWithJavaSource("toplevel/ToplevelObjectWithSupertypes");
    }
    @Test
    public void testTopToplevelVariable(){
        compareWithJavaSource("toplevel/ToplevelVariable");
    }
    @Test
    public void testTopToplevelVariableShared(){
        compareWithJavaSource("toplevel/ToplevelVariableShared");
    }
    @Test
    public void testMthTopLevelSpecifyingTopLevel(){
        compareWithJavaSource("toplevel/TopLevelSpecifyingTopLevel");
    }
    //
    // Type
    
    @Test
    public void testTypBasicTypes(){
        compareWithJavaSource("type/BasicTypes");
    }
    @Test
    public void testTypBottom(){
        compareWithJavaSource("type/Bottom");
    }
    @Test
    public void testTypConversions(){
        compareWithJavaSource("type/Conversions");
    }
    @Test
    public void testTypOptionalType(){
        compareWithJavaSource("type/OptionalType");
    }
    @Test
    public void testTypSequenceType(){
        compareWithJavaSource("type/SequenceType");
    }
    
    //
    // import
    
    @Test
    public void testImpImportAliasAndWildcard(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportAliasAndWildcard");
    }
    
    private void compileImportedPackage() {
        compile("import_/pkg/C1.ceylon", "import_/pkg/C2.ceylon");
    }

    @Test
    public void testImpImportAttrSingle(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportAttrSingle");
    }

    @Test
    public void testImpImportMethodSingle(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportMethodSingle");
    }
    
    @Test
    public void testImpImportTypeSingle(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportTypeSingle");
    }
    
    @Test
    public void testImpImportTypeMultiple(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportTypeMultiple");
    }
    
    @Test
    public void testImpImportTypeAlias(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportTypeAlias");
    }
    
    @Test
    public void testImpImportWildcard(){
        compileImportedPackage();
        compareWithJavaSource("import_/ImportWildcard");
    }
    
    @Test
    public void testImpImportJavaRuntimeTypeSingle(){
        compareWithJavaSource("import_/ImportJavaRuntimeTypeSingle");
    }

    @Test
    public void testImpImportJavaRuntimeTypeWildcard(){
        compareWithJavaSource("import_/ImportJavaRuntimeTypeWildcard");
    }

    @Test
    public void testImpImportCeylonLanguageType(){
        compareWithJavaSource("import_/ImportCeylonLanguageType");
    }
    
    // Tests for nesting of declarations
    /*@Test
    public void testNstNesting(){
        compareWithJavaSource("nesting/Nesting");
    }*/
    @Test
    public void testNstCcc(){
        compareWithJavaSource("nesting/ccc/CCC");
    }
    @Test
    public void testNstCci(){
        compareWithJavaSource("nesting/cci/CCI");
    }
    @Test
    public void testNstCic(){
        compareWithJavaSource("nesting/cic/CIC");
    }
    @Test
    public void testNstCii(){
        compareWithJavaSource("nesting/cii/CII");
    }
    @Test
    public void testNstIcc(){
        compareWithJavaSource("nesting/icc/ICC");
    }
    @Test
    public void testNstIci(){
        compareWithJavaSource("nesting/ici/ICI");
    }
    @Test
    public void testNstIic(){
        compareWithJavaSource("nesting/iic/IIC");
    }
    @Test
    public void testNstIii(){
        compareWithJavaSource("nesting/iii/III");
    }
    @Test
    public void testNstLocals(){
        compareWithJavaSource("nesting/Locals");
    }
    
    @Test
    public void testNstClassMethodDefaultedParameter(){
        compareWithJavaSource("nesting/ClassMethodDefaultedParameter");
    }
    
    @Test
    public void testNstFunctionDefaultedParameter(){
        compareWithJavaSource("nesting/FunctionDefaultedParameter");
    }
    
    @Test
    public void testNstClassInitDefaultedParameter(){
        compareWithJavaSource("nesting/ClassInitDefaultedParameter");
    }
    

    // Tests for concrete members of interfaces
    @Test
    public void testCncThis(){
        compareWithJavaSource("concrete/This");
    }
    
    @Test
    public void testCncConcreteInterface(){
        compareWithJavaSource("concrete/ConcreteInterface");
    }

    @Test
    public void testCncInterfaceMethodDefaultedParameter(){
        compareWithJavaSource("concrete/InterfaceMethodDefaultedParameter");
    }
    
    @Test
    public void testCncSatisfaction(){
        compareWithJavaSource("concrete/Satisfaction");
    }
    
    @Test
    public void testNstObjects(){
        compareWithJavaSource("nesting/Objects");
    }

}
