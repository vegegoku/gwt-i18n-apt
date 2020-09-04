package org.gwtproject.i18n.processor;

import java.util.ArrayList;
import java.util.List;

public class InterfaceMethods {

    private final List<ConstantMethod> methods = new ArrayList<>();

    public List<ConstantMethod> getMethods() {
        return methods;
    }

    public void addAll(List<ConstantMethod> methods) {
        methods.forEach(this::add);
    }

    public void addAll(InterfaceMethods interfaceMethods) {
        addAll(interfaceMethods.methods);
    }

    public void add(ConstantMethod constantMethod) {
        if(constantMethod.getMethod().getSimpleName().toString().equals("shared")){
            System.out.println("hello");
        }
        if (!methods.contains(constantMethod)) {
            methods.add(constantMethod);
        }else{
            methods
                    .get(methods.indexOf(constantMethod))
                    .addSuperMethod(constantMethod);
        }
    }
}
