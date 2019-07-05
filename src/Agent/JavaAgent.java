package Agent;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class JavaAgent  {

    public static void premain(String agentArgs, Instrumentation inst) throws IOException, NoSuchFieldException {
        System.out.println("Executing taint premain...........");
        //System.out.println(String.class.getField("hiddenValue").getName());
        System.out.println();
        ClassTransformer transformer = new ClassTransformer();
        inst.addTransformer(transformer);
    }
}

