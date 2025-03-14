package com.geektime.tdd.rest;// SimpleMockTest.java

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SimpleMockTest {

    @Test
    public void testMockMethod() throws Exception {
        // 创建Mock对象
        UserRepository userRepository = SimpleMock.createMock(UserRepository.class);
        UserService userService = new UserService(userRepository);
        // 获取UserRepository的findByName方法
        Method findByNameMethod = UserRepository.class.getMethod("findByName", String.class);
        // 设置存根：当调用findByName("Alice")时返回new User("Alice")
        User alice = new User("Alice");
        SimpleMock.Stubber.when(alice, findByNameMethod, "Alice");
        // 调用被测试方法
        User result = userService.getUserByName("Alice");
        // 验证返回值
        assertNotNull(result);
        assertEquals("Alice", result.getName());        // 验证方法调用次数
        int invocationCount = SimpleMock.Stubber.getInvocationCount(findByNameMethod, "Alice");
        assertEquals(1, invocationCount);
    }

    @Test
    public void testVoidMethod() throws Exception {
        UserRepository userRepository = SimpleMock.createMock(UserRepository.class);
        UserService userService = new UserService(userRepository);        // 获取save方法
        Method saveMethod = UserRepository.class.getMethod("save", User.class);
        User user = new User("Bob");        // 调用void方法
        userService.saveUser(user);        // 验证方法调用次数
        int invocationCount = SimpleMock.Stubber.getInvocationCount(saveMethod, user);
        assertEquals(1, invocationCount);
    }
}
