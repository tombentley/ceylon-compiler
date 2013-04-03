package com.redhat.ceylon.compiler.java.test.expression;

import org.junit.Test;

import com.redhat.ceylon.compiler.java.test.CompilerTest;

public class IndyTest extends CompilerTest {

    @Test
    public void testIndy(){
        compileAndRun("com.redhat.ceylon.compiler.java.test.expression.indy.C", "indy/Indy.ceylon");
        System.out.flush();
    }
    
}
