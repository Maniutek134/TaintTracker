package Agent;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import static java.lang.System.out;

public class JavaAgent  {

    public static void premain(String agentArgs, Instrumentation inst) throws IOException, NoSuchFieldException, NoSuchMethodException {
        out.println("Executing taint premain...........");
        out.println(String.class.getDeclaredMethod("isTainted").getName());
        out.println();
        ClassTransformer transformer = new ClassTransformer();
        //out.println("dupa");
        inst.addTransformer(transformer);
    }
}

