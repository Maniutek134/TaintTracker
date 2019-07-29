package Agent;

import javassist.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;



class ClassTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {


        className = className.replaceAll("/", ".");

        byte[] byteCode = classfileBuffer;
        ClassPool cp;

        cp = ClassPool.getDefault();
        cp.appendClassPath(new LoaderClassPath(loader));
        //System.out.println(classPathLoader.toString());
        //cp.insertClassPath(new ClassClassPath(this.getClass()));

        FunctionClassifier fc = new FunctionClassifier(cp);

        CtClass returnedClass;
        CtClass.debugDump = "./dump";

        try {
            returnedClass = fc.clasify(className);
            if(returnedClass != null){
                return returnedClass.toBytecode();
            }

        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return byteCode;
    }
}
