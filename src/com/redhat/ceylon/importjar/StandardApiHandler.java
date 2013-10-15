package com.redhat.ceylon.importjar;

class StandardApiHandler implements ApiHandler {

    @Override
    public void debug(Object s) {
        //System.out.println(s);
    }
    
    @Override
    public void error(String s, Throwable cause) {
        System.err.println("ERROR: "+s);
        cause.printStackTrace();
    }

    @Override
    public void linkError(String className, String missingClassName) {
        System.err.println("Missing dependency on "+missingClassName);
    }

    @Override
    public void missingApiModule(String moduleName) {
        System.err.println("Missing API module "+moduleName);
    }

    @Override
    public void missingApiClass(String className) {
        System.err.println("Missing API class "+className);
    }

    @Override
    public void missingImplModule(String moduleName) {
        System.err.println("Missing implementation module "+moduleName);
    }

    @Override
    public void missingImplClass(String className) {
        System.err.println("Missing implementation class "+className);
    }
    
}