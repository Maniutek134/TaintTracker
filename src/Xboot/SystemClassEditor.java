package Xboot;

import Agent.Taintable;
import Agent.FunctionClassifier.*;
import javassist.*;

import java.io.IOException;

public class SystemClassEditor {
    public static void main(String[] args) {

        System.out.println();
        System.out.println("Starting SystemClassEditor");

        try {
            new SystemClassEditor().edit();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void edit() throws NotFoundException, CannotCompileException, IOException {
        ClassPool cp = ClassPool.getDefault();

        write(addTaintableToClass(cp, "java.lang.String"));
        write(addTaintableToClass(cp, "java.lang.StringBuffer"));
        write(addTaintableToClass(cp, "java.lang.StringBuilder"));


    }

    private CtClass addTaintableToClass(ClassPool cp, String className) throws NotFoundException, CannotCompileException, IOException {
        CtClass cClass = cp.get(className);
        cClass.defrost();

        cClass.addInterface(cp.get(Taintable.class.getName()));

        addTaintField(cClass);
        addTaintMethods(cClass);
//        propagateTaintInMethods(cClass);
//        propagatingDataTypes.put(className, cClass);
//        writeClass(cp, className);

        return cClass;
    }

    private void addTaintField(CtClass cClass) throws CannotCompileException {
        cClass.addField(CtField.make("private boolean tainted;", cClass));//, "TaintUtils.propagateParameterTaint($0, $args)");
        cClass.addField(CtField.make("private String taintSource;", cClass));
    }

    private void addTaintMethods(CtClass cClass) throws CannotCompileException {
        cClass.addMethod(CtMethod.make("public void setTaint(boolean value, String className){ this.tainted = value; if(className != null) this.taintSource = className; }", cClass));
        cClass.addMethod(CtMethod.make("public boolean isTainted(){ return this.tainted; }", cClass));
        cClass.addMethod(CtMethod.make("public String getTaintSource(){ return this.taintSource; }", cClass));
    }

    private void write(CtClass ctClass){
        try {
            ctClass.writeFile(".");
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
