package com.redhat.ceylon.importjar;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.redhat.ceylon.cmr.api.JDKUtils;


public class ApiVerifier implements AutoCloseable{
    
    private static final class VerifierClassLoader extends URLClassLoader {
        private VerifierClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
/*
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }
        */
    }


    private static boolean isPublic(Member method) {
        // TODO Diff between publicly accessible stuff (which would require 
        // a `shared import` in the module desc, iff it's externally loaded)
        // and other missing stuff (which would just require an `import` in the module desc)
        return Modifier.isPublic(method.getModifiers());
    }
    
    private static boolean isProtected(Member method) {
        return Modifier.isProtected(method.getModifiers());
    }
    
    private static boolean isStatic(Member method) {
        return Modifier.isStatic(method.getModifiers());
    }
    
    private static boolean isPublic(Class<?> cls) {
        return Modifier.isPublic(cls.getModifiers());
    }
    
    private static boolean isFinal(Class<?> cls) {
        return Modifier.isFinal(cls.getModifiers());
    }
    
    private static boolean isStatic(Class<?> cls) {
        return Modifier.isStatic(cls.getModifiers());
    }
    
    private static boolean isSubtypeable(Class<?> cls) {
        if (isPublic(cls)) {
            if (cls.isInterface()) {
                return true;
            } else if (!isFinal(cls)) {
                for (Constructor<?> ctor : cls.getConstructors()) {
                    if (isPublic(ctor)
                            || isProtected(ctor)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private static boolean isInstantiable(Class<?> cls) {
        if (cls.isInterface()
                || cls.isEnum()
                || cls.isLocalClass()) {
            return false;
        }
        if (isPublic(cls)) {
            for (Constructor<?> ctor : cls.getConstructors()) {
                if (isPublic(ctor)
                        || isProtected(ctor)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean isVisible(Field f) {
        return isPublic(f);
    }
    
    private static boolean isVisible(Method m) {
        return isPublic(m) && isPublic(m.getDeclaringClass());
    }

    private static URL[] classPath(File jar, List<File> dependentJars){
        URL[] classPath = new URL[1 + dependentJars.size()];
        try {
            classPath[0] = jar.toURI().toURL();
            for (int ii = 0; ii < dependentJars.size(); ii++) {
                classPath[ii+1] = dependentJars.get(ii).toURI().toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return classPath;
    }
    
    private final ApiHandler logger;
    private final Set<String> foundClasses = new HashSet<String>();
    private final Set<String> apiDependentClasses = new HashSet<String>();
    private final Set<String> implDependentClasses = new HashSet<String>();
    private final Set<String> apiDependentJdkModules = new HashSet<String>();
    private final Set<String> implDependentJdkModules = new HashSet<String>();
    private final VerifierClassLoader classloader;
    private final File jar;
    
    public ApiVerifier(ApiHandler logger, File jar) {
        this(logger, jar, Collections.<File>emptyList());
    }
    
    public ApiVerifier(ApiHandler logger, File jar, List<File> dependentJars) {
        this.logger = logger;
        this.jar = jar;
        // Pass explict null parent loader, because we don't want to check 
        // the exact set of jar we're given, not polluted with our 
        // classloaders jars
        this.classloader = new VerifierClassLoader(classPath(jar, dependentJars), null);
    }
    
    @Override
    public void close() {
        try {
            classloader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void depends(Class<?> cls, Class<?> dependsOn, boolean visibly) {
        while (dependsOn.isArray()) {
            dependsOn = dependsOn.getComponentType();
        }
        if (dependsOn.getName().startsWith("java.")
                || dependsOn.isPrimitive()) {
            return;
        }
        String packageName = getPackageName(dependsOn);
        String jdkModule = packageName != null ? JDKUtils.getJDKModuleNameForPackage(packageName) : null;
        if (jdkModule != null) {
            if (visibly) {
                apiDependentJdkModules.add(jdkModule);
            } else {
                implDependentJdkModules.add(jdkModule);
            }
        } else if (visibly) {
            apiDependentClasses.add(dependsOn.getName());
        } else {
            implDependentClasses.add(dependsOn.getName());
        }
    }

    private static String getPackageName(Class<?> cls) {
        Class<?> encl = cls;
        while (encl.getEnclosingClass() != null) {
            encl = encl.getEnclosingClass();
        }
        if (encl.getName().lastIndexOf('.') == -1) {
            return null;
        }
        return encl.getName().substring(0, encl.getName().lastIndexOf('.'));
    }
    /*
     * Find class
     * 
     *   is public interface:
     *       all the types of accessible from all the methods should be public
     *       all the types of public fields shoudl be public
     *   
     *   is public class:
     *     has public constructor: (can be instantiated)
     *       check all public members
     *     non final and has public/protected constructor: (can be subclassed)
     *       check all public and protected members
     *     has public static method:
     *     has public static field:
     * 
     *   
     * 
     *//*
    private void verifyType(Class<?> cls) {
        if (isSubtypeable(cls)) {
            verifyForSubtype(cls);
        } else if (isInstantiable(cls)) {
            verifyForInstantiation(cls);
        }
        verifyStatics(cls);
    }
    
    private void verifyForSubtype(Class<?> cls) {
        for (Field field : cls.getDeclaredFields()) {
            if (!isStatic(field)) {
                verifyField(field);
            }
        }
        for (Method method : cls.getDeclaredMethods()) {
            if (!isStatic(method)) {
                verifyMethod(method);
            }
        }
        for (Class<?>method : cls.getDeclaredClasses()) {
            if (!isStatic(method)) {
                verifyType(method);
            }
        }
    }

    private void verifyStatics(Class<?> cls) {
        for (Field field : cls.getDeclaredFields()) {
            if (isStatic(field)) {
                verifyField(field);
            }
        }
        for (Method method : cls.getDeclaredMethods()) {
            if (isStatic(method)) {
                verifyMethod(method);
            }
        }
        for (Class<?>method : cls.getDeclaredClasses()) {
            if (isStatic(method)) {
                verifyType(method);
            }
        }
    }*/

    public void verify() {
        // TODO Hints about how to deal with javax classes
        // and about things like org.xml.* or org.w3c.dom
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length()-6).replace('/', '.');
                    Class<?> cls;
                    try {
                        try {
                            cls = Class.forName(name, false, classloader);
                        } catch (ClassNotFoundException e) {
                            logger.error("Unable to read .class file from jar: " + name, e);
                            continue;
                        } 
                        verifyClass(cls);
                    } catch (NoClassDefFoundError e) {
                        logger.linkError(name, e.getMessage().replace('/', '.'));
                        //logger.missingApiDependency("Unable to read .class file from jar: " + name, e);
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("unable to read .jar file: " + jar, e);
        }
        
        //implDependentClasses.removeAll(this.foundClasses);
        //apiDependentClasses.removeAll(this.foundClasses);
        implDependentClasses.removeAll(this.apiDependentClasses);
        System.err.println(Arrays.toString(classloader.getURLs()));
        System.err.println("Internal: " + implDependentClasses);
        System.err.println("External: " + apiDependentClasses);
        for (String moduleName : apiDependentJdkModules) {
            logger.missingApiModule(moduleName);
        }
        for (String className : apiDependentClasses) {
            if (!isLoadable(className)) {
                logger.missingApiClass(className);
            }
        }
        for (String moduleName : implDependentJdkModules) {
            logger.missingImplModule(moduleName);
        }
        for (String className : implDependentClasses) {
            if (!isLoadable(className)) {
                logger.missingImplClass(className);
            }
        }
        
    }

    /** 
     * Can we load the given class from the module itself or from its 
     * dependent jars?
     */
    private boolean isLoadable(String className) {
        boolean ok = false;
        try {
            if (Class.forName(className, false, classloader).getClassLoader() == classloader) {
                ok = true;
            }
        } catch (ClassNotFoundException e) {
            // So it's missing from the dependent jars too
        }
        return ok;
    }
    
    private void verifyClass(Class<?> cls) {
        foundClasses.add(cls.getName());
        int mask = Modifier.PUBLIC;
        if (isSubtypeable(cls)) {
            mask |= Modifier.PROTECTED;
        }
        logger.debug(cls);
        if (cls.getSuperclass() != null) {
            logger.debug("  extends " + cls.getSuperclass());
            depends(cls, cls.getSuperclass(), true);
        }
        for (Class<?> iface : cls.getInterfaces()) {
            logger.debug("  implements " + iface);
            depends(cls, iface, true);
        }
        for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
            boolean ctorVis = (ctor.getModifiers() & mask) != 0;
            for(Annotation a : ctor.getAnnotations()) {
                verifyAnnotation(cls, ctor, a, ctorVis);
            }
            for (Class<?> pt : ctor.getParameterTypes()) {
                verifyParameter(cls, ctor, pt, ctorVis);
            }
            for (Annotation[] annoForParam : ctor.getParameterAnnotations()) {
                for (Annotation anno : annoForParam) {
                    verifyAnnotation(cls, ctor, anno, ctorVis);
                }
            }
            for (Class<?> et : ctor.getExceptionTypes()) {
                verifyException(cls, ctor, et, ctorVis);
            }
        }
        for (Field field : cls.getFields()) {
            boolean fieldVis = (field.getModifiers() & mask) != 0;
            for(Annotation a : field.getAnnotations()) {
                verifyAnnotation(cls, field, a, fieldVis);
            }
            depends(cls, field.getType(), fieldVis);
            
        }
        for (Method method : cls.getMethods()) {
            boolean methodVis = (method.getModifiers() & mask) != 0;
            for(Annotation a : method.getAnnotations()) {
                verifyAnnotation(cls, method, a, methodVis);
            }
            depends(cls,  method.getReturnType(), methodVis);
            for (Class<?> pt : method.getParameterTypes()) {
                verifyParameter(cls, method, pt, methodVis);
            }
            for (Annotation[] annoForParam : method.getParameterAnnotations()) {
                for (Annotation anno : annoForParam) {
                    verifyAnnotation(cls, method, anno, methodVis);
                }
            }
            for (Class<?> et : method.getExceptionTypes()) {
                verifyException(cls, method, et, methodVis);
            }
            if (method.getDefaultValue() != null) {
                depends(cls,  method.getDefaultValue().getClass(), methodVis);
            }
        }
    }
    
    private void verifyParameter(Class<?> cls, AccessibleObject of, Class<?> pt, boolean visibly) {
        depends(cls, pt, visibly);
    }
    
    private void verifyException(Class<?> cls, AccessibleObject of, Class<?> pt, boolean visibly) {
        depends(cls, pt, visibly);
    }
    
    private void verifyAnnotation(Class<?> cls, AccessibleObject of, Annotation anno, boolean visibly) {
        depends(cls, anno.annotationType(), visibly);
    }
    
    
    public static void main(String[] args) {
        /*try (ApiVerifier v = new ApiVerifier(
                new StandardApiHandler(), 
                new File("/home/tom/.ceylon/lib/markdownpapers-core-1.2.7.jar"))) {
            v.verify();
        }*/
        /*try (ApiVerifier v = new ApiVerifier(
                new StandardApiHandler(), 
                new File("/home/tom/.ceylon/lib/slf4j-simple-1.6.1.jar"),
                Collections.singletonList(new File("/home/tom/.ceylon/lib/slf4j-api-1.6.1.jar")))) {
            v.verify();
        }*/
        //new ApiVerifier(new StandardLogger()).checkJar(new File("/home/tom/.ceylon/lib/slf4j-simple-1.6.1.jar"),
        //        Collections.singletonList(new File("/home/tom/.ceylon/lib/slf4j-api-1.6.1.jar")));
        
        //new ApiVerifier(new StandardLogger()).checkJar(new File("/home/tom/.ceylon/repo/com/redhat/ceylon/typechecker/1.0.0/com.redhat.ceylon.typechecker-1.0.0.jar"),
                //Collections.<File>emptyList());
        /*try (ApiVerifier v = new ApiVerifier(
                new StandardApiHandler(), 
                new File("/home/tom/.m2/repository/org/apache/ant/ant/1.7.1/ant-1.7.1.jar"),
                Collections.<File>emptyList())) {
            v.verify();
        }*/
        try (ApiVerifier v = new ApiVerifier(
                new StandardApiHandler(), 
                new File("/home/tom/.ceylon/repo/com/redhat/ceylon/compiler/java/1.0.0/com.redhat.ceylon.compiler.java-1.0.0.jar"),
                Arrays.asList(new File[]{
                        /*new File("/home/tom/.ceylon/lib/slf4j-api-1.6.1.jar"),*/
                        //new File("/home/tom/.m2/repository/org/osgi/org.osgi.core/4.1.0/org.osgi.core-4.1.0.jar")
                        new File("/home/tom/.ceylon/repo/com/redhat/ceylon/typechecker/1.0.0/com.redhat.ceylon.typechecker-1.0.0.jar"),
                        new File("/home/tom/.ceylon/repo/com/redhat/ceylon/module-resolver/1.0.0/com.redhat.ceylon.module-resolver-1.0.0.jar"),
                        new File("/home/tom/.ceylon/repo/com/redhat/ceylon/common/1.0.0/com.redhat.ceylon.common-1.0.0.jar"),
                        new File("/home/tom/.ceylon/lib/markdownpapers-core-1.2.7.jar"),
                        new File("/home/tom/.ceylon/lib/txtmark-0.7.jar"),
                        new File("/home/tom/.ceylon/lib/antlr-3.4-complete.jar")
                }))) {
            v.verify();
        }
    }
}

