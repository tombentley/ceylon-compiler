import ceylon.language.metamodel{Annotated, SequencedAnnotation}

shared annotation class Seq(shared String seq) 
    satisfies SequencedAnnotation<Seq, Annotated> {}
shared annotation Seq seq(String s) => Seq(s); 


"aToplevelValue"
seq("aToplevelValue 1")
seq("aToplevelValue 2")
shared String aToplevelValue = "";

"aToplevelGetter"
seq("aToplevelGetter 1")
shared String aToplevelGetterSetter {
    return "";
}
"aToplevelSetter"
assign aToplevelGetterSetter {
}
"aToplevelFunction"
seq("aToplevelFunction 1")
shared void aToplevelFunction(
    "aToplevelFunction.parameter"
    seq("aToplevelFunction.parameter 1")
    String parameter) {
    
}
"aToplevelObject"
seq("aToplevelObject 1")
object aToplevelObject {
}

"AInterface"
seq("AInterface 1")
seq("AInterface 2")
shared interface AInterface {
    
    "AInterface.FormalInnerClass"
    shared formal class FormalInnerClass(
            "AInterface.FormalInnerClass.parameter"
            String parameter) {
        "AInterface.FormalInnerClass.method"
        shared void method(
                "AInterface.FormalInnerClass.method.parameter"
                String parameter) {}
    }
    
    "AInterface.DefaultInnerClass"
    shared default class DefaultInnerClass(
            "AInterface.DefaultInnerClass.parameter"
            String parameter) {
        
        "AInterface.DefaultInnerClass.method"
        shared void method(
                "Interface.DefaultInnerClass.method.parameter"
                String parameter) {}
    }
    
    "AInterface.formalMethod"
    shared formal void formalMethod(
        "AInterface.formalMethod.parameter"
        String parameter);
    
    "AInterface.defaultMethod"
    shared default void defaultMethod(
        "AInterface.defaultMethod.parameter"
        String parameter){}
    
    "AInterface.method"
    shared void method(
        "AInterface.method.parameter"
        String parameter){}
    
    "AInterface.nonsharedMethod"
    void nonsharedMethod(
        "AInterface.nonsharedMethod.parameter"
        String parameter){}
}

"AAbstractClass"
seq("AAbstractClass 1")
seq("AAbstractClass 2")
shared abstract class AAbstractClass (
    "AAbstractClass.parameter"
    String parameter) satisfies AInterface {

    "AAbstractClass.FormalInnerClass"
    shared actual class FormalInnerClass (
            "AAbstractClass.FormalInnerClass.parameter"
            String parameter)
            extends AInterface::FormalInnerClass(parameter) {
        
    }
    
    "AAbstractClass.DefaultInnerClass"
    shared actual class DefaultInnerClass (
            "AAbstractClass.DefaultInnerClass.parameter"
            String parameter)
            extends AInterface::DefaultInnerClass(parameter)  {
        
        
    }
    
    "AAbstractClass.Inner"
    shared class Inner(
            "AAbstractClass.Inner.parameter"
            String parameter) {
        
        "AAbstractClass.Inner.method"
        shared void method(
            "AAbstractClass.Inner.method.parameter"
            String parameter) {}
    }
    
    "AAbstractClass.formalMethod"
    shared actual default void formalMethod(
        "AAbstractClass.formalMethod.parameter"
        String parameter) {
        
    }
}

"AClass"
seq("AClass 1")
seq("AClass 2")
shared class AClass(
        "AClass.parameter"
        seq("AClass.parameter 1")
        seq("AClass.parameter 2")
        String parameter) 
    extends AAbstractClass(parameter) {
 
}