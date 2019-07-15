package Agent;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.*;
import javassist.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static java.lang.System.out;

public class FunctionClassifier {

    private SourceSinkSanitizer sources;
    private SourceSinkSanitizer sinks;
    private SourceSinkSanitizer sanitizers;


    private ClassPool cp;

    public FunctionClassifier(ClassPool cp) {

        this.cp = cp;
        out.println("dupa");
        setSanitizers(readSourcesSinkSanitizer("resources/sanitizers.json"));
        setSources(readSourcesSinkSanitizer("resources/sources.json"));
        setSinks(readSourcesSinkSanitizer("resources/sinks.json"));
        //todo do something with the mess connected to those jsons!!!

    }

    public static boolean isMethodNative(CtMethod method) {
        return Modifier.isNative(method.getModifiers());
    }

    public static boolean isMethodStatic(CtMethod method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isMethodAbstract(CtMethod method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    public void isSourceSinkOrSanitizer(CtClass ctClass, CtClass alterClass, CtMethod ctMethod) throws NotFoundException, CannotCompileException {

//        List<ClassGroup> sources;
//        List<ClassGroup> sinks;
//        List<ClassGroup> sanitizers;
//
//        if(!ctClass.isInterface()){
//            sources = this.sources.getInterfaces();
//            sinks = this.sinks.getInterfaces();
//            sanitizers = this.sanitizers.getInterfaces();
//        }
//        else{
//            sources = this.sources.getClasses();
//            sinks = this.sinks.getClasses();
//            sanitizers = this.sanitizers.getClasses();
//        }
//
//        if(sources.)
        CtClass returnType = ctMethod.getReturnType();

        if (sources.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sources.isMethodInInterface(alterClass.getName(), ctMethod.getName())){

            if (returnType.subtypeOf(cp.get(Taintable.class.getName()))){
                cp.importPackage(TaintHandler.class.getName());
                cp.importPackage(TaintPropagationHandler.class.getName());

                if(!isMethodStatic(ctMethod)){
                    ctMethod.insertAfter("{ TaintPropagation.addTaintToMethod($0, $_, \"" + ctClass.getName() + "\"); }");
                }
                else {
                    ctMethod.insertAfter("{ TaintUtils.addTaintToMethod(null, $_, \"" + ctClass.getName() + "\"); }");
                }
                out.println("\t\tSource Defined");
            }
            out.println("\t\t Untaintable return type: " + returnType.getName());

        }

        if (sinks.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sinks.isMethodInInterface(alterClass.getName(), ctMethod.getName())){

        }

        if (sanitizers.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sanitizers.isMethodInInterface(alterClass.getName(), ctMethod.getName())){

        }


        //return "";
    }

    public SourceSinkSanitizer readSourcesSinkSanitizer(String fileName) {
        URL fileUrl = ClassLoader.getSystemClassLoader().getResource(fileName);
        ObjectMapper mapper = new ObjectMapper();
        SourceSinkSanitizer sourceSinkSanitizer = new SourceSinkSanitizer();
        //out.println(fileUrl);
        try {
            sourceSinkSanitizer = mapper.readValue(fileUrl, SourceSinkSanitizer.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sourceSinkSanitizer;
    }

    public Boolean transform(CtClass ctClass, CtClass alterClass) throws NotFoundException, CannotCompileException {


        CtMethod[] methods = ctClass.getDeclaredMethods();
        if(methods.length >0){
            for (CtMethod method : methods) {
                out.println(method.getName());
                if(!isMethodNative(method) && !isMethodAbstract(method)){
                    isSourceSinkOrSanitizer(ctClass,alterClass, method);
                }
            }
        }
        return true;
    }

    public CtClass classSourceSinkOrSanitizer(CtClass ctClass) {

        if (sinks.isClassInClasses(ctClass.getName())) {
            return ctClass;
        } else if (sources.isClassInClasses(ctClass.getName())) {
            return ctClass;
        } else if (sanitizers.isClassInClasses(ctClass.getName())) {
            return ctClass;
        }
        return null;
    }

    public CtClass classExtendsSourceSinkOrSanitizer(CtClass ctClass) {
        try {
            CtClass superClass = ctClass.getSuperclass();

            if (null != superClass) {
                return superClassSourceSinkOrSanitizer(superClass);
            }
        } catch (NotFoundException e) {
            //ignored
        }

        return null;
    }

    public CtClass superClassSourceSinkOrSanitizer(CtClass ctClass) throws NotFoundException {
        CtClass temp;

        if (null != (temp = classSourceSinkOrSanitizer(ctClass))) {
            return temp;
        } else if (null != (temp = classImplementsSourceSinkOrSanitizer(ctClass))) {
            return temp;
        } else if(null != (temp = classExtendsSourceSinkOrSanitizer(ctClass))){
            return temp;
        }
        return null;
    }

    public CtClass classImplementsSourceSinkOrSanitizer(CtClass ctClass){

        CtClass[] interfaces;

        try {
            interfaces = ctClass.getInterfaces();

            if(interfaces.length > 0){
                for(CtClass interElement: interfaces){
                    if(sinks.isClassInInterfaces(interElement.getName())){
                        return interElement;
                    }
                }
            }

        } catch (NotFoundException e) {
            //ignore
        }

        return null;

    }

    public boolean clasify(String className) throws NotFoundException, CannotCompileException {

        CtClass ctClass = getCp().getOrNull(className);
        if(ctClass == null){
            out.println("Cant load class: " + className);
            return false;
        }

//        else{
//            out.println("loaded class: "+ctClass.getName());
//        }

        CtClass temp;

        if(null != (temp = classSourceSinkOrSanitizer(ctClass))) {
            transform(ctClass, temp);
        }else if(null != (temp = classImplementsSourceSinkOrSanitizer(ctClass))){
                transform(ctClass, temp);
        }else if(null != (temp = classExtendsSourceSinkOrSanitizer(ctClass))){
                transform(ctClass, temp);
        }

        return true;
    }

    public void setSanitizers(SourceSinkSanitizer sanitizers) {
        this.sanitizers = sanitizers;
    }

    public void setSinks(SourceSinkSanitizer sinks) {
        this.sinks = sinks;
    }

    public void setSources(SourceSinkSanitizer sources) {
        this.sources = sources;
    }

    public SourceSinkSanitizer getSinks() {
        return sinks;
    }

    public SourceSinkSanitizer getSources() {
        return sources;
    }

    public SourceSinkSanitizer getSanitizer() {
        return sanitizers;
    }

    public ClassPool getCp() {
        return cp;
    }

    public void setCp(ClassPool cp) {
        this.cp = cp;
    }

}
