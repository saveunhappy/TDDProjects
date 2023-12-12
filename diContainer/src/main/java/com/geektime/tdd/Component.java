package com.geektime.tdd;

import java.lang.annotation.Annotation;

record Component(Class<?> type, Annotation qualifier) {

}
