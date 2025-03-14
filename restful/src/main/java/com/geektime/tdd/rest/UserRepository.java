package com.geektime.tdd.rest;

public interface UserRepository {
    User findByName(String name);
    void save(User user);
}
