///* __________              _____                                                *\
//** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
//**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
//**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
//**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
//**                      \/          \/     \/                                   **
//\*                                                                              */
//
//package cn.ponfee.disjob.supervisor.config;
//
//import cn.ponfee.disjob.core.param.worker.AuthenticationParam;
//import org.apache.commons.lang3.ArrayUtils;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Before;
//import org.springframework.stereotype.Component;
//
///**
// * Aspect test
// *
// * @author Ponfee
// */
//@Aspect
//@Component
//public class AuthenticationCallWorker {
//
//    @Before("execution(* cn.ponfee.disjob..*.*(..,cn.ponfee.disjob.core.param.worker.AuthenticationParam+,..))")
//    public void doBefore(JoinPoint point) {
//        Object[] args = point.getArgs();
//        if (ArrayUtils.isEmpty(args)) {
//            return;
//        }
//        for (Object arg : args) {
//            if (arg instanceof AuthenticationParam) {
//                ((AuthenticationParam) arg).setSupervisorToken("xxxxx");
//            }
//        }
//    }
//
//}
