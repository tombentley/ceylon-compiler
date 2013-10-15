package com.redhat.ceylon.importjar;

/**
 * Contract for reporting problems encountered in the {@link ApiVerifier}.
 */
interface ApiHandler {
    
    /** Arbitrary implementation dependent debug information */
    void debug(Object s);
    
    /** An exceptional condition while trying to verify */
    void error(String s, Throwable cause);
    
    

    void linkError(String className, String missingClassName);

    void missingApiModule(String moduleName);
    /** 
     * The given type appears in the accessible API of the module, 
     * and is not present in the jar or any of its 
     * declared dependencies.
     * A suitable {@code shared import} in the module descriptor is 
     * definitely required.
     */
    void missingApiClass(String className);

    void missingImplModule(String moduleName);
    /** 
     * The given type does not appear in the accessible API of the module,
     * but does appear in the internal API of the module, 
     * and is not present in the jar or any of its 
     * declared dependencies. 
     * A suitable {@code import} in the module descriptor is 
     * probably required. 
     */
    void missingImplClass(String className);
    
}