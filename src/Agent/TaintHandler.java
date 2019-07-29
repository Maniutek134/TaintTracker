package Agent;

import static java.lang.System.out;

public class TaintHandler {

    private static void setTaint(Object s, boolean value, String className) {
        if (s instanceof Taintable) {
            ((Taintable) s).setTaint(value, className);
        }
    }

    public static void taint(Object s, String className) {
        setTaint(s, true, className);
    }

    public static void detaint(Object s) {
        setTaint(s, false, null);
    }

    public static boolean isTainted(Object s) {
        return s instanceof Taintable && ((Taintable) s).isTainted();
    }

    public static void checkTaint(Object s, String className, String methodName){
        if(isTainted(s)){
            ((Taintable) s).setTaint(false, null);

            out.println("Tainted value sent without sanitarization!!!");
            out.println("Source: " + ((Taintable) s).getTaintSource() + "\tSink: " + className);
            out.println("in method: "+ methodName+"\n");
        }
    }
}
