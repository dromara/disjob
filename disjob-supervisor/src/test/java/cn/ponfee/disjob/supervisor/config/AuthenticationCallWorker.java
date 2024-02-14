/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
