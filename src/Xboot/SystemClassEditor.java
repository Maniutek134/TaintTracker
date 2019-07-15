package Xboot;

import Agent.*;
import javassist.*;

import java.io.IOException;

import static Agent.FunctionClassifier.*;
import static Agent.TaintPropagationHandler.*;
import static Agent.TaintHandler.*;


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

        cp.importPackage(TaintPropagationHandler.class.getName());
        cp.importPackage(TaintHandler.class.getName());
        //cp.importPackage(Taintable.class.getName());

        write(addTainableToClass(cp, "java.lang.String"));
        write(addTainableToClass(cp, "java.lang.StringBuffer"));
        write(addTainableToClass(cp, "java.lang.StringBuilder"));


    }

    private CtClass addTainableToClass(ClassPool cp, String className) throws NotFoundException, CannotCompileException, IOException {
        CtClass cClass = cp.get(className);
        cClass.defrost();

        cClass.addInterface(cp.get(Taintable.class.getName()));

        addTaintField(cClass);
        addTaintMethods(cClass);
        propagateTaintInMethods(cClass);
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
        cClass.addMethod(CtMethod.make("public void setTaintSource(String taintSource){ this.taintSource = taintSource; }", cClass));
    }


    private void propagateTaintInMethods(CtClass cClass) throws NotFoundException, CannotCompileException {
        CtMethod[] cMethods = cClass.getDeclaredMethods();
        for (CtMethod cMethod : cMethods) {
            if (!isMethodStatic(cMethod) &&
                    !isMethodNative(cMethod) &&
                    !isMethodAbstract(cMethod) &&
                    !cMethod.getName().equals("setTaint") &&
                    !cMethod.getName().equals("isTainted") &&
                    !cMethod.getName().equals("setTaintSource") &&
                    !cMethod.getName().equals("getTaintSource")) {

                CtClass returnType = cMethod.getReturnType();
                if (returnType.subtypeOf(ClassPool.getDefault().get(Taintable.class.getName()))) {
                    cMethod.insertAfter("{ Object ret = TaintPropagationHandler.propagateParameterTaintObject($0, $args); if(ret != null) $_.setTaint(true, TaintPropagationHandler.getTaintSource(ret)); }");
                }

                if (cMethod.getParameterTypes().length > 0) {
                    cMethod.insertBefore("{ Object ret = TaintPropagationHandler.propagateParameterTaintObject($0, $args); if(ret != null) $0.setTaint(true, TaintPropagationHandler.getTaintSource(ret)); }");
                }
            }
        }
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
