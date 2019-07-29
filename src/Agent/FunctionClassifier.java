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

        setSanitizers(readSourcesSinkSanitizer("resources/sanitizers.json"));
        setSources(readSourcesSinkSanitizer("resources/sources.json"));
        setSinks(readSourcesSinkSanitizer("resources/sinks.json"));

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

    public CtMethod isSourceSinkOrSanitizer(CtClass ctClass, CtClass alterClass, CtMethod ctMethod) throws NotFoundException, CannotCompileException {


        CtClass returnType = ctMethod.getReturnType();

        cp.importPackage(Agent.TaintHandler.class.getName());
        cp.importPackage(Agent.TaintPropagationHandler.class.getName());

//        if(returnType.subtypeOf(cp.get(Taintable.class.getName()))) {
//            ctMethod.insertAfter("{System.out.println(\"" + ctMethod.getName() + "\");}");
//            //ctMethod.insertAfter("{System.out.println($args[0]);}");
//            ctMethod.insertAfter("{System.out.println($_);}");
//        }

        if (sources.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sources.isMethodInInterface(alterClass.getName(), ctMethod.getName())){
            if (returnType.subtypeOf(cp.get(Taintable.class.getName()))){

                out.println();
                out.println("altered name: "+alterClass.getName());
                out.println("class name: "+ctClass.getName());
                out.println("method name: "+ctMethod.getName());
                out.println("returned type: "+returnType.getName());


                if(!isMethodStatic(ctMethod)){
                    ctMethod.insertAfter("{ Agent.TaintPropagationHandler.addTaintToMethod($0, $_, \"" + ctClass.getName() + "\"); }");
                    //ctMethod.insertAfter("{ System.out.println(\"scource activated\");}");
                }
                else {
                    ctMethod.insertAfter("{ Agent.TaintPropagationHandler.addTaintToMethod(null, $_, \"" + ctClass.getName() + "\"); }");
                    //ctMethod.insertAfter("{ System.out.println(\"scource activated\");}");
                }
                out.println("Source Defined\n");
            }
            else {
                out.println("\nUntaintable return type: " + returnType.getName() + "\n");
            }

            return ctMethod;

        }else if (sinks.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sinks.isMethodInInterface(alterClass.getName(), ctMethod.getName())) {

            out.println();
            out.println("altered name: "+alterClass.getName());
            out.println("class name: "+ctClass.getName());
            out.println("method name: " + ctMethod.getName());
            out.println("returned type: " + returnType.getName());

            if (!isMethodStatic(ctMethod)) {
                ctMethod.insertBefore("{ Agent.TaintPropagationHandler.assertNonTaint($0, " +
                        "$args, " +
                        "\"" + ctClass.getName() + "\", " +
                        "\"" + ctMethod.getName()+ "\"); }");

            } else {
                ctMethod.insertBefore("{ Agent.TaintPropagationHandler.assertNonTaint(null, " +
                        "$args, " +
                        "\"" + ctClass.getName() + "\", " +
                        "\"" + ctMethod.getName()+ "\"); }");
            }
            out.println("Sink Defined\n" );

            return ctMethod;

        }else if (sanitizers.isMethodInClasses(alterClass.getName(), ctMethod.getName()) ||
                sanitizers.isMethodInInterface(alterClass.getName(), ctMethod.getName())){

            out.println();
            out.println("altered name: "+alterClass.getName());
            out.println("class name: "+ctClass.getName());
            out.println("method name: " + ctMethod.getName());
            out.println("returned type: " + returnType.getName());

            ctMethod.insertAfter("{ Agent.TaintPropagationHandler.detaintMethodReturn($_);}");

            out.println("Sanitizer Defined\n" );
            return ctMethod;
        }


        return ctMethod;

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

    public CtClass transform(CtClass ctClass, CtClass alterClass) throws NotFoundException, CannotCompileException {

        CtMethod ret;
        //out.println("altered name transform: "+alterClass.getName());
        CtMethod[] methods = ctClass.getDeclaredMethods();
        if(methods.length >0){
            for (CtMethod method : methods) {
                if(!isMethodNative(method) && !isMethodAbstract(method)){
                    isSourceSinkOrSanitizer(ctClass,alterClass, method);
                }
            }
        }
        return ctClass;
    }

    public CtClass classSourceSinkOrSanitizer(CtClass ctClass) {


        if (sources.isClassInClasses(ctClass.getName())) {
            return ctClass;
        } else if (sinks.isClassInClasses(ctClass.getName())) {
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

    public CtClass classImplementsSourceSinkOrSanitizer(CtClass ctClass) {

        CtClass[] interfaces;

        //out.println("class: " + ctClass.getName());

        try {
            interfaces = ctClass.getInterfaces();

            if(interfaces.length > 0){
                for(CtClass interElement: interfaces){
                    if(sources.isClassInInterfaces(interElement.getName())){
                        return interElement;
                    }else if(sinks.isClassInInterfaces(interElement.getName())){
                        return interElement;
                    }else if(sanitizers.isClassInInterfaces(interElement.getName())){
                        return interElement;
                    }
                }
           }

        } catch (NotFoundException e) {
//            //ignore
        }

        return null;

    }

    public CtClass clasify(String className) throws NotFoundException, CannotCompileException {

        CtClass ctClass = cp.getOrNull(className);


        //CtClass temp, ret;

        if(ctClass == null){
            out.println("Cant load class: " + className);
            return ctClass;
        }
        if(ctClass.isInterface()){
            return null;
        }

        ctClass.defrost();
        //out.println("load class: " + className);
////                        CtMethod[] methods = ctClass.getDeclaredMethods();
////                        for(CtMethod method : methods){
////                            out.println("\t"+method.getName()); org.apache.tomcat.util.http.Parameters
////                        }
//        if(ctClass.getName().equals("org.apache.tomcat.util.http.Parameters")) {
//
//            try {
//                CtMethod method = ctClass.getDeclaredMethod("getParameter");
//
//                method.insertAfter("{ System.out.println(\"scource activated\");}");
//                method.insertAfter("{ System.out.println(\"" + method.getName() + "\");}");
//                method.insertAfter("{ System.out.println($_);}");
//                method.insertAfter("{ System.out.println($0.getClass().getName());}");
//                out.println(ctClass.getName());
//                out.println("\t" + method.getName());
//                out.println("\t added!!!!");
//            } catch (NotFoundException e) {
//                //ignore
//            }
//        }

        //}

//





        //}

//        if(ctClass.getName().equals("org.apache.tomcat.util.http.Parameters")) {
//            if(null != (temp = classSourceSinkOrSanitizer(ctClass))) {
//                out.println("load ctclass name: "+temp.getName());
//                ret = transform(ctClass, temp);
//                return ret;
//            }else if(null != (temp = classImplementsSourceSinkOrSanitizer(ctClass))){
//                ret = transform(ctClass, temp);
//                return ret;
//            }else if(null != (temp = classExtendsSourceSinkOrSanitizer(ctClass))){
//                ret  = transform(ctClass, temp);
//                return ret;
//            }
//        }

        ctClass.defrost();
        CtClass temp, ret;

        if(null != (temp = classSourceSinkOrSanitizer(ctClass))) {
            ret = transform(ctClass, temp);
            return ret;
        }else if(null != (temp = classImplementsSourceSinkOrSanitizer(ctClass))){
            ret = transform(ctClass, temp);
            return ret;
        }else if(null != (temp = classExtendsSourceSinkOrSanitizer(ctClass))){
            ret  = transform(ctClass, temp);
            return ret;
        }

        return ctClass;
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
