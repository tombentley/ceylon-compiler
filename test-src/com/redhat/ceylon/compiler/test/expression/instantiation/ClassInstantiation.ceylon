@nomodel
class ClassInstantiation(){
    shared ClassInstantiation m() {
        return ClassInstantiation();
    }
}

@nomodel
class ClassInstantiationWithParam(Natural i){
    shared ClassInstantiationWithParam m() {
        return ClassInstantiationWithParam(2);
    }
}

@nomodel
class ClassInstantiationWithParams(Natural i, String j){
    shared ClassInstantiationWithParams m() {
        return ClassInstantiationWithParams(2, "");
    }
}

@nomodel
class InnerClassInstantiation() {
    class InnerClass() {
    }
    void m () {
        value x = InnerClass();
    }
}

@nomodel
class LocalClassInstantiation() {
    void m () {
        class LocalClass() {
        }
        value x = LocalClass();
    }
}