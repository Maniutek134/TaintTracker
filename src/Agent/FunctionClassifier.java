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
        //out.println("dupa1");
        setSanitizers(readSourcesSinkSanitizer("resources/sanitizers.json"));
        setSources(readSourcesSinkSanitizer("resources/sources.json"));
        setSinks(readSourcesSinkSanitizer("resources/sinks.json"));
        //todo do something with the mess connected to those jsons!!!
        //out.println("dupa2");
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


        CtClass returnType = ctMethod.getReturnType();

        if (sources.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sources.isMethodInInterface(alterClass.getName(), ctMethod.getName())){

            if (returnType.subtypeOf(cp.get(Taintable.class.getName()))){
                cp.importPackage(TaintHandler.class.getName());
                cp.importPackage(TaintPropagationHandler.class.getName());

                out.println();
                out.println("method name: "+ctMethod.getName());
                out.println("returned type: "+returnType.getName());


                if(!isMethodStatic(ctMethod)){
                    ctMethod.insertAfter("{ Agent.TaintPropagationHandler.addTaintToMethod($0, $_, \"" + ctClass.getName() + "\"); }");
                    //ctMethod.insertAfter(("{ System.out.println(\""+TaintPropagationHandler.class.getName()+"\"); }"));
                    out.println("not static");
                }
                else {
                    ctMethod.insertAfter("{ Agent.TaintPropagationHandler.addTaintToMethod(null, $_, \"" + ctClass.getName() + "\"); }");
                    //ctMethod.insertAfter(("{ System.out.println(\""+TaintPropagationHandler.addTaintToMethod().;+"\"); }"));
                    out.println("static");
                }
                out.println("Source Defined");
            }
            else {
                out.println("\nUntaintable return type: " + returnType.getName());
            }

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
        //out.println(fileUrl);
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
