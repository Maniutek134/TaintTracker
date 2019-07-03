package Xboot;

import Agent.FunctionClassifier.*;
import javassist.*;

import java.io.IOException;

public class SystemClassEditor {
    public static void main(String[] args) {

        System.out.println();
        System.out.println("Starting TaintFieldAdder");

        try {
            new SystemClassEditor().run();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void run() throws NotFoundException, CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("java.lang.String");
        CtField f = new CtField(CtClass.intType, "hiddenValue", cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f);
        cc.writeFile(".");
    }
}
