package com.landawn.abacus.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

class InvocationHandlerTest {

    @Test
    public void test() {
        final MyClassInterface original = new MyClass();

        // Create a dynamic proxy
        final MyClassInterface proxy = (MyClassInterface) Proxy.newProxyInstance(MyClass.class.getClassLoader(), new Class[] { MyClassInterface.class },
                new LoggingInvocationHandler(original));

        // Call methods on the proxy
        proxy.method1();
        proxy.method2();
    }

    class LoggingInvocationHandler implements InvocationHandler {
        private final Object target;

        public LoggingInvocationHandler(final Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            // Log the method call
            System.out.println("Calling method: " + method.getName());

            // Invoke the actual method
            final Object result = method.invoke(target, args);

            // Log after the method call
            System.out.println("Method " + method.getName() + " executed");

            return result;
        }

    }

    public interface MyClassInterface {
        void method1();

        void method2();
    }

    public static class MyClass implements MyClassInterface {
        @Override
        public void method1() {
            System.out.println("Executing method1");
        }

        @Override
        public void method2() {
            System.out.println("Executing method2");
        }
    }
}
