package com.geektime.tdd.rest;

// SimpleMock.java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SimpleMock {
    private static final InvocationHandler MOCK_HANDLER = new MockInvocationHandler();

    public static <T> T createMock(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                MOCK_HANDLER
        );
    }

    public static class Stubber {
        private static final Map<MethodKey, Object> stubs = new HashMap<>();
        private static final Map<MethodKey, Integer> invocationCounts = new HashMap<>();

        public static void when(Object returnValue, Method method, Object... args) {
            MethodKey key = new MethodKey(method, args);
            stubs.put(key, returnValue);
        }

        public static Object getStubbedValue(Method method, Object[] args) {
            MethodKey key = new MethodKey(method, args);
            invocationCounts.put(key, invocationCounts.getOrDefault(key, 0) + 1);
            return stubs.get(key);
        }

        public static int getInvocationCount(Method method, Object... args) {
            MethodKey key = new MethodKey(method, args);
            return invocationCounts.getOrDefault(key, 0);
        }

        static class MethodKey {
            private final Method method;
            private final Object[] args;

            MethodKey(Method method, Object[] args) {
                this.method = method;
                this.args = args;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                MethodKey methodKey = (MethodKey) o;
                return method.equals(methodKey.method) && Arrays.equals(args, methodKey.args);
            }

            @Override
            public int hashCode() {
                int result = method.hashCode();
                result = 31 * result + Arrays.hashCode(args);
                return result;
            }
        }
    }

    private static class MockInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object stubbedValue = Stubber.getStubbedValue(method, args);
            if (stubbedValue != null) {
                return stubbedValue;
            }
            // 返回方法类型的默认值
            return getDefaultValue(method.getReturnType());
        }

        private Object getDefaultValue(Class<?> type) {
            if (type.isPrimitive()) {
                if (type == boolean.class) return false;
                else if (type == byte.class) return (byte) 0;
                else if (type == short.class) return (short) 0;
                else if (type == int.class) return 0;
                else if (type == long.class) return 0L;
                else if (type == float.class) return 0.0f;
                else if (type == double.class) return 0.0d;
                else if (type == char.class) return '\0';
            }
            return null;
        }
    }
}
