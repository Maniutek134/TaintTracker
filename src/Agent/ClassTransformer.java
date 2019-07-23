package Agent;

import javassist.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        FunctionClassifier fc = new FunctionClassifier(cp);

        try {
            fc.clasify(className);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }



        return byteCode;
    }
}
