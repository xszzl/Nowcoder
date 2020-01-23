package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component
//@Aspect
public class AlphaAspect {
    // 第一个*代表方法的返回值，可以为任意值
    // com.nowcoder.community.service.*.* 表示service包下的所有类的所有方法
    // (..)表示所有的参数
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){

    }

    // 切点之前
    // 针对pointcut()的切点
    @Before("pointcut()")
    public void before(){
        System.out.println("before");
    }

    // 切点之后
    @After("pointcut()")
    public void after(){
        System.out.println("after");
    }

    // 在有返回值之后
    @AfterReturning("pointcut()")
    public void afterReturning(){
        System.out.println("afterReturning");
    }

    // 在抛异常之后
    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        System.out.println("afterThrowing");
    }

    // 在切点之前和之后
    @Around("pointcut()")
    // joinPoint表示连接点，程序织入的位置
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("around before");
        // 调用目标组件的方法
        Object obj = joinPoint.proceed();
        System.out.println("around after");
        return obj;
    }
}
