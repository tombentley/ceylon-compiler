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
package com.redhat.ceylon.importjar;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.api.JDKUtils;
import com.redhat.ceylon.cmr.api.Logger;
import com.redhat.ceylon.cmr.api.ModuleInfo;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils.CeylonRepoManagerBuilder;
import com.redhat.ceylon.cmr.impl.CMRException;
import com.redhat.ceylon.cmr.impl.PropertiesDependencyResolver;
import com.redhat.ceylon.cmr.impl.XmlDependencyResolver;
import com.redhat.ceylon.common.tool.Argument;
import com.redhat.ceylon.common.tool.CeylonBaseTool;
import com.redhat.ceylon.common.tool.Description;
import com.redhat.ceylon.common.tool.Option;
import com.redhat.ceylon.common.tool.OptionArgument;
import com.redhat.ceylon.common.tool.Summary;
import com.redhat.ceylon.compiler.java.loader.CeylonModelLoader;
import com.redhat.ceylon.tools.ModuleSpec;
import com.sun.tools.classfile.ClassReader;

@Summary("Imports a jar file into a Ceylon module repository")
@Description("Imports the given `<jar-file>` using the module name and version " +
        "given by `<module>` into the repository named by the " +
        "`--out` option.\n" +
        "\n" +
        "`<module>` is a module name and version separated with a slash, for example " +
        "`com.example.foobar/1.2.0`.\n" +
        "\n" +
        "`<jar-file>` is the name of the Jar file to import.")
public class CeylonImportJarTool extends CeylonBaseTool {

    private Appendable err = System.err;
    private ModuleSpec module;
    private String out;
    private String user;
    private String pass;
    private String jarFile;
    private Logger log = new CMRLogger();
    private String descriptor;
    private CeylonRepoManagerBuilder repoManager;
    private boolean verify = true;

    public CeylonImportJarTool() {
    }
    
    CeylonImportJarTool(String moduleSpec, String out, String user, String pass, String jarFile, String verbose){
        setModuleSpec(moduleSpec);
        this.out = out;
        this.user = user;
        this.pass = pass;
        this.jarFile = jarFile;
        this.verbose = verbose;
        init();
    }
    
    public void setErr(Appendable err) {
        this.err = err;
    }

    @OptionArgument(argumentName="name")
    @Description("Sets the user name for use with an authenticated output repository.")
    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    @OptionArgument(argumentName="secret")
    @Description("Sets the password for use with an authenticated output repository.")
    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getOut() {
        return out;
    }

    @OptionArgument(argumentName="dir-or-url")
    @Description("Specifies the module repository (which must be publishable) " +
            "into which the jar file should be imported. " +
            "(default: `./modules`)")
    public void setOut(String out) {
        this.out = out;
    }
    
    @OptionArgument(argumentName="file")
    @Description("Specify a module.xml or module.properties file to be used "
            + "as the module descriptor")
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
    
    @Option
    @Description("Whether the verification of the dependencies of the jar "
            + "file should be skipped (default: false).")
    public void setNoVerify(boolean noVerify) {
        this.verify = !noVerify;
    }
    
    public File getDescriptorFile() {
        if (this.descriptor == null) {
            return null;
        }
        return new File(this.cwd, this.descriptor);
    }
    
    public boolean isXmlDescriptor() {
        return this.descriptor != null 
                && this.descriptor.toLowerCase().endsWith(".xml");
    }
    
    public boolean isPropertiesDescriptor() {
        return this.descriptor != null 
                && this.descriptor.toLowerCase().endsWith(".properties");
    }
    
    @Argument(argumentName="module", multiplicity="1", order=0)
    public void setModuleSpec(String module) {
        setModuleSpec(ModuleSpec.parse(module, 
                ModuleSpec.Option.VERSION_REQUIRED, 
                ModuleSpec.Option.DEFAULT_MODULE_PROHIBITED));
    }
    
    public void setModuleSpec(ModuleSpec module) {
        this.module = module;
    }

    @Argument(argumentName="jar-file", multiplicity="1", order=1)
    public void setJar(String jarFile) {
        this.jarFile = jarFile;
    }
    
    public File getJarFile() {
        if (this.jarFile == null) {
            return null;
        }
        return new File(this.cwd, this.jarFile);
    }
    
    @PostConstruct
    public void init() {
        
        this.repoManager = CeylonUtils.repoManager()
                .cwd(cwd)
                .outRepo(this.out)
                .logger(log)
                .user(user)
                .password(pass);
    }

    private void checkReadableFile(File f, String keyPrefix) {
        if(!f.exists())
            throw new ImportJarException(keyPrefix + ".doesNotExist", new Object[]{f.toString()}, null);
        if(f.isDirectory())
            throw new ImportJarException(keyPrefix + ".isDirectory", new Object[]{f.toString()}, null);
        if(!f.canRead())
            throw new ImportJarException(keyPrefix + ".notReadable", new Object[]{f.toString()}, null);
    }
    
    void err(String s) {
        try {
            this.err.append(s).append(System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void publish() {
        if(this.jarFile == null || this.jarFile.isEmpty())
            throw new ImportJarException("error.jarFile.empty");
        File f = getJarFile();
        checkReadableFile(f, "error.jarFile");
        if(!f.getName().toLowerCase().endsWith(".jar"))
            throw new ImportJarException("error.jarFile.notJar", new Object[]{f.toString()}, null);
        
        List<File> dependentJars = Collections.emptyList();
        if (getDescriptorFile() != null) {
            File descriptorFile = getDescriptorFile();
            checkReadableFile(descriptorFile, "error.descriptorFile");
            if(!(isXmlDescriptor() ||
                    isPropertiesDescriptor())) {
                throw new ImportJarException("error.descriptorFile.badSuffix", new Object[]{descriptorFile.getPath()}, null);
            }
            
            RepositoryManager repository = repoManager.buildManager();
            if(isXmlDescriptor())
                dependentJars = checkModuleXml(repository, descriptorFile);
            else if(isPropertiesDescriptor())
                dependentJars = checkModuleProperties(repository, descriptorFile);
        }
        
        if (verify) {
            final int errors = verify(f, dependentJars);
            
            if (errors > 0) {
                throw new ImportJarException("verifyErrors", new Object[]{errors}, null);
            }
        }
        
        
        RepositoryManager outputRepository = this.repoManager
                .outRepo(this.out)
                .buildOutputManager();

        ArtifactContext context = new ArtifactContext(module.getName(), module.getVersion(), ArtifactContext.JAR);
        context.setForceOperation(true);
        ArtifactContext descriptorContext = null;
        if (getDescriptorFile() != null) {
            if (isXmlDescriptor()) {
                descriptorContext = new ArtifactContext(module.getName(), module.getVersion(), ArtifactContext.MODULE_XML);
            } else if (isPropertiesDescriptor()) {
                descriptorContext = new ArtifactContext(module.getName(), module.getVersion(), ArtifactContext.MODULE_PROPERTIES);
            }
            descriptorContext.setForceOperation(true);
        }
        
        
        try{
            outputRepository.putArtifact(context, getJarFile());
            String sha1 = ShaSigner.sha1(getJarFile().getPath(), log);
            if(sha1 != null){
                File shaFile = ShaSigner.writeSha1(sha1, log);
                if(shaFile != null){
                    try{
                        ArtifactContext sha1Context = context.getSha1Context();
                        outputRepository.putArtifact(sha1Context, shaFile);
                    }finally{
                        shaFile.delete();
                    }
                }
            }
            
            if (descriptorContext != null) {
                outputRepository.putArtifact(descriptorContext, getDescriptorFile());
            }
        }catch(CMRException x){
            throw new ImportJarException("error.failedWriteArtifact", new Object[]{context, x.getLocalizedMessage()}, x);
        }catch(Exception x){
            // FIXME: remove when the whole CMR is using CMRException
            throw new ImportJarException("error.failedWriteArtifact", new Object[]{context, x.getLocalizedMessage()}, x);
        }
    }

    private int verify(File f, List<File> dependentJars) {
        final int[] errors = new int[]{0};
        new ApiVerifier(new ApiHandler() {
            
            @Override
            public void missingImplModule(String moduleName) {
                err(ImportJarMessages.msg("verify.missing.impl.module", moduleName));
                errors[0]++;
            }
            
            @Override
            public void missingImplClass(String className) {
                err(ImportJarMessages.msg("verify.missing.impl.class", className));
                errors[0]++;
            }
            
            @Override
            public void missingApiModule(String moduleName) {
                err(ImportJarMessages.msg("verify.missing.api.module", moduleName));
                errors[0]++;
            }
            
            @Override
            public void missingApiClass(String className) {
                err(ImportJarMessages.msg("verify.missing.api.class", className));
                errors[0]++;
            }
            
            @Override
            public void linkError(String className, String missingClassName) {
                err(ImportJarMessages.msg("verify.missing.dependent.class", missingClassName));
                errors[0]++;
            }
            
            @Override
            public void error(String s, Throwable cause) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                cause.printStackTrace(pw);
                pw.flush();
                err(ImportJarMessages.msg("verify.internal.error", s, cause.toString(), pw));
                errors[0]++;
            }
            @Override
            public void debug(Object s) {
            }
        }, f, dependentJars);
        return errors[0];
    }
    
    private List<File> checkModuleProperties(RepositoryManager repository, File file) {
        try{
            Set<ModuleInfo> dependencies = PropertiesDependencyResolver.INSTANCE.resolveFromFile(file);
            return checkDependencies(repository, dependencies);
        }catch(ImportJarException x){
            throw x;
        }catch(Exception x){
            throw new ImportJarException("error.descriptorFile.invalid.properties", new Object[]{file.getPath()}, x);
        }
    }

    private List<File> checkModuleXml(RepositoryManager repository, File file) {
        try{
            Set<ModuleInfo> dependencies = XmlDependencyResolver.INSTANCE.resolveFromFile(file);
            return checkDependencies(repository, dependencies);
        }catch(ImportJarException x){
            throw x;
        }catch(Exception x){
            throw new ImportJarException("error.descriptorFile.invalid.xml", new Object[]{file.getPath(), x.getMessage()}, x);
        }
    }

    private List<File> checkDependencies(RepositoryManager repository, Set<ModuleInfo> dependencies) throws IOException {
        ArrayList<File> dependentJars = new ArrayList<File>();
        if(dependencies.isEmpty()){
            err.append("[WARNING] Empty dependencies file").append(System.lineSeparator());
        }else{
            err.append("Checking declared dependencies:").append(System.lineSeparator());
            for(ModuleInfo dep : dependencies){
                String name = dep.getName();
                String version = dep.getVersion();
                // missing dep is OK, it can be fixed later, but invalid module/dependency is not OK
                if(name == null || name.isEmpty())
                    throw new ImportJarException("error.descriptorFile.invalid.module", new Object[]{name}, null);
                if("default".equals(name))
                    throw new ImportJarException("error.descriptorFile.invalid.module.default");
                if(version == null || version.isEmpty())
                    throw new ImportJarException("error.descriptorFile.invalid.module.version", new Object[]{version}, null);
                err.append("- "+dep+" ");
                if(JDKUtils.isJDKModule(name) || JDKUtils.isOracleJDKModule(name))
                    err.append("[OK]").append(System.lineSeparator());
                else{
                    err.append("... [");
                    ArtifactContext context = new ArtifactContext(name, dep.getVersion(), ArtifactContext.CAR, ArtifactContext.JAR);
                    File artifact = repository.getArtifact(context);
                    dependentJars.add(artifact);
                    if(artifact != null && artifact.exists())
                        err.append("OK]").append(System.lineSeparator());
                    else
                        err.append("NOT FOUND]").append(System.lineSeparator());
                }
            }
        }
        return dependentJars;
    }

    @Override
    public void run(){
        publish();
    }

    public class CMRLogger implements Logger {

        @Override
        public void error(String str) {
            throw new ImportJarException("error.cmrError", new Object[]{str}, null);
        }

        @Override
        public void warning(String str) {
            System.err.println(ImportJarMessages.msg("log.warning", str));
        }

        @Override
        public void info(String str) {
            System.err.println(ImportJarMessages.msg("log.info", str));
        }

        @Override
        public void debug(String str) {
            if(verbose != null)
                System.err.println(ImportJarMessages.msg("log.debug", str));
        }
    }
}