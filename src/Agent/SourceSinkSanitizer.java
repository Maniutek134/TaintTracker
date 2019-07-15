package Agent;

import java.util.List;


public class SourceSinkSanitizer {

    private String type;
    private List<ClassGroup> classes;
    private List<ClassGroup> interfaces;

    public List<ClassGroup> getClasses() {
        return classes;
    }

    public List<ClassGroup> getInterfaces() {
        return interfaces;
    }

    public Boolean isMethodInClasses(String className, String methodName) {
        return getaBoolean(className, methodName,  classes);
    }

    public Boolean isMethodInInterface(String className, String methodName) {
        return getaBoolean(className, methodName, interfaces);
    }

    public Boolean isClassInClasses(String className){
        for (ClassGroup cg : classes){
            if(cg.getClazz().equals(className)){
                return true;
            }
        }
        return false;
    }

    public Boolean isClassInInterfaces(String className){
        for (ClassGroup cg : interfaces){
            if(cg.getClazz().equals(className)){
                return true;
            }
        }
        return false;
    }

    private Boolean getaBoolean(String className, String methodName, List<ClassGroup> classGroupList) {
        for (ClassGroup gc : classGroupList) {
            if (gc.getClazz().equals(className)) {
                for (String method : gc.getMethods()) {
                    if (method.equals(methodName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}

class ClassGroup{

    private String clazz;
    private String[] methods;
    private String descriptor;

    public String getDescriptor() {
        return descriptor;
    }

    public String getClazz() {
        return clazz;
    }

    public String[] getMethods() {
        return methods;
    }

    public void setMethods(String[] methods) {
        this.methods = methods;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public boolean isMethodInMethods(String method){
        for (String meth : methods){
            if(meth.equals(method)){
                return true;
            }
        }

        return false;
    }
}
