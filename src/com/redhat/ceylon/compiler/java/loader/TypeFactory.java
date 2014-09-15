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

package com.redhat.ceylon.compiler.java.loader;

import java.util.Collections;

import com.redhat.ceylon.compiler.java.tools.LanguageCompiler;
import com.redhat.ceylon.compiler.typechecker.context.Context;
import com.redhat.ceylon.compiler.typechecker.model.Class;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.IntersectionType;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.UnionType;
import com.redhat.ceylon.compiler.typechecker.model.Unit;
import com.redhat.ceylon.compiler.typechecker.model.UnknownType;
import com.redhat.ceylon.compiler.typechecker.model.Util;
import com.sun.tools.javac.util.List;

public class TypeFactory extends Unit {
    private Context context;

    public static TypeFactory instance(com.sun.tools.javac.util.Context context) {
        TypeFactory instance = context.get(TypeFactory.class);
        if (instance == null) {
            instance = new TypeFactory(LanguageCompiler.getCeylonContextInstance(context));
            context.put(TypeFactory.class, instance);
        }
        return instance;
    }
    
    public TypeFactory(Context context) {
        this.context = context;
    }
    
    public Context getContext() {
        return context;
    }
    
    /**
     * Determines whether the given ProducedType is a union
     * @param pt 
     * @return whether the type is a union type
     */
    public boolean isUnion(ProducedType pt) {
        if (pt==null) return false;
        TypeDeclaration tdecl = pt.getDeclaration();
        return (tdecl instanceof UnionType && tdecl.getCaseTypes().size() > 1);
    }

    /**
     * Determines whether the given ProducedType is an intersection
     * @param pt 
     * @return whether the type is an intersection type
     */
    public boolean isIntersection(ProducedType pt) {
        if (pt==null) return false;
        TypeDeclaration tdecl = pt.getDeclaration();
        return (tdecl instanceof IntersectionType && tdecl.getSatisfiedTypes().size() > 1);
    }

    public TypeDeclaration getArraySequenceDeclaration() {
        return (Class) getLanguageModuleDeclaration("ArraySequence");
    }
    
    public ProducedType getArraySequenceType(ProducedType et) {
        return Util.producedType(getArraySequenceDeclaration(), et);
    }
    
    /**
     * Returns a ProducedType corresponding to {@code Array<T>}
     * @param et The ProducedType corresponding to {@code T}
     * @return The ProducedType corresponding to {@code Array<T>}
     */
    public ProducedType getArrayType(ProducedType et) {
        return Util.producedType(getArrayDeclaration(), et);
    }

    public ProducedType getArrayElementType(ProducedType type) {
        ProducedType st = type.getSupertype(getArrayDeclaration());
        if (st!=null && st.getTypeArguments().size()==1) {
            return st.getTypeArgumentList().get(0);
        }
        else {
            return null;
        }
    }
    
    public ProducedType getCallableType(java.util.List<ProducedType> typeArgs) {
        if (typeArgs.size()!=2) {
            throw new IllegalArgumentException("Callable type always has two arguments: " + typeArgs);
        }
        if (!typeArgs.get(1).isSubtypeOf(
                getSequentialDeclaration().getProducedType(
                        null, Collections.singletonList(getAnythingDeclaration().getType())))) {
            throw new IllegalArgumentException("Callable's second argument should be sequential " + typeArgs.get(1));
        }
        return getCallableDeclaration().getProducedType(null, typeArgs);
    }
    
    public ProducedType getCallableType(ProducedType resultType) {
        return getCallableType(List.<ProducedType>of(resultType,getEmptyDeclaration().getType()));
    }

    public ProducedType getUnknownType() {
        return new UnknownType(this).getType();
    }
    
    public ProducedType getIteratedAbsentType(ProducedType type) {
        ProducedType st = type.getSupertype(getIterableDeclaration());
        if (st!=null && st.getTypeArguments().size()>1) {
            return st.getTypeArgumentList().get(1);
        }
        else {
            return null;
        }
    }

    public Declaration getBooleanTrueDeclaration() {
        return getLanguageModuleDeclaration("true");
    }

    public Declaration getBooleanFalseDeclaration() {
        return getLanguageModuleDeclaration("false");
    }

    public TypeDeclaration getBooleanTrueClassDeclaration() {
        Declaration trueDecl = getBooleanTrueDeclaration();
        return trueDecl instanceof TypedDeclaration ? ((TypedDeclaration)trueDecl).getTypeDeclaration() : null;
    }

    public TypeDeclaration getBooleanFalseClassDeclaration() {
        Declaration trueDecl = getBooleanFalseDeclaration();
        return trueDecl instanceof TypedDeclaration ? ((TypedDeclaration)trueDecl).getTypeDeclaration() : null;
    }

    public TypeDeclaration getMetamodelTypeDeclaration() {
        return (TypeDeclaration) getLanguageModuleModelDeclaration("Type");
    }

    public TypedDeclaration getMetamodelNothingTypeDeclaration() {
        return (TypedDeclaration) getLanguageModuleModelDeclaration("nothingType");
    }

    public TypeDeclaration getMetamodelDeclarationDeclaration() {
        return (TypeDeclaration) getLanguageModuleDeclarationDeclaration("Declaration");
    }
    
    public TypeDeclaration getAssertionErrorDeclaration() {
        return (TypeDeclaration)getLanguageModuleDeclaration("AssertionError");
    }
    
    public ProducedType getReferenceType(ProducedType value) {
        final ProducedType serializedValueType;
        TypeDeclaration referenceTypeDecl = (TypeDeclaration)getLanguageModuleSerializationDeclaration("Reference");
        serializedValueType = referenceTypeDecl.getProducedType(null, Collections.singletonList(value));
        return serializedValueType;
    }
    
    public ProducedType getDeconstructorType() {
        return ((TypeDeclaration)getLanguageModuleSerializationDeclaration("Deconstructor")).getType();
    }
}
