package Agent;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.net.URL;

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

    public String isSourceSinkOrSanitizer(CtClass ctClass, String methodName) {


        if (!ctClass.isInterface()) {
            if (sources.isMethodInClasses(ctClass.getName(), methodName)) {
                return "\t" + methodName + ": source\n";
            }

            if (sinks.isMethodInClasses(ctClass.getName(), methodName)) {
                return "\t" + methodName + ": sink\n";
            }

            if (sanitizers.isMethodInClasses(ctClass.getName(), methodName)) {
                return "\t" + methodName + ": sanitizer\n";
            }
        } else {
            if (sources.isMethodInInterface(ctClass.getName(), methodName)) {
                return "\t" + methodName + ": source\n";
            }

            if (sinks.isMethodInInterface(ctClass.getName(), methodName)) {
                return "\t" + methodName + ": sink\n";
            }

            if (sanitizers.isMethodInInterface(ctClass.getName(), methodName)) {
                return "\t" + methodName + ": sanitizer\n";
            }
        }

        return "";
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

    public Boolean transform(CtClass ctClass, CtClass alterClass) throws NotFoundException {


        CtMethod[] methods = ctClass.getDeclaredMethods();
        for (CtMethod method : methods) {
            //out.println(method.getName());
            out.print(isSourceSinkOrSanitizer(alterClass, method.getName()));
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

    public boolean clasify(String className) throws NotFoundException {

        CtClass ctClass = cp.getOrNull(className);
        if(ctClass == null){
            out.println("Cant load class: " + className);
            return false;
        }


        CtClass temp;

        if(null != (temp = classSourceSinkOrSanitizer(ctClass))) {
            transform(ctClass, temp);
        }else if(null != (temp = classImplementsSourceSinkOrSanitizer(ctClass))){
                transform(ctClass, temp);
        }else if(null != (temp = classExtendsSourceSinkOrSanitizer(ctClass))){
                //out.println(ctClass.getName());
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
