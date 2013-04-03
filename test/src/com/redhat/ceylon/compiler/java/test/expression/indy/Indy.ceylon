@nomodel
class C() {
    shared void m(String staticArg) {
        print("Greeting: " + staticArg);
    }
    for (i in 0..1) {
        m("");
    }
    
}
