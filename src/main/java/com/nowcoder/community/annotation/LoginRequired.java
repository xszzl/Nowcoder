package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// 自定义注解，方便在拦截器中进行处理
@Target(ElementType.METHOD)             //表示注解是对方法起作用的
@Retention(RetentionPolicy.RUNTIME)     //表示注解起作用的时长
public @interface LoginRequired {

}
