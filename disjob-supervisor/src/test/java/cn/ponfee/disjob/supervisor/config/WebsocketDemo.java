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

//package cn.ponfee.disjob.supervisor.config;
//
//import cn.ponfee.disjob.common.spring.SpringContextHolder;
//import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.server.standard.ServerEndpointExporter;
//
//import javax.annotation.Resource;
//import javax.servlet.http.HttpSession;
//import javax.websocket.*;
//import javax.websocket.server.HandshakeRequest;
//import javax.websocket.server.PathParam;
//import javax.websocket.server.ServerEndpoint;
//import javax.websocket.server.ServerEndpointConfig;
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * <dependency>
// *   <groupId>org.springframework.boot</groupId>
// *   <artifactId>spring-boot-starter-websocket</artifactId>
// * </dependency>
// *
// * <p>Websocket based javax.websocket
// * <p><a href="http://www.easyswoole.com/wstool.html">WebSocket 在线测试工具</a>
// * <p>当OnOpen、OnMessage处理出异常时，会走到OnError做异常处理，最后会走到OnClose做session关闭后的处理
// *
// * @author Ponfee
// */
//@Slf4j
//@Component
//@ServerEndpoint(value = "/test/websocket/{taskId}", configurator = TestWebsocket.WebsocketConfigurator.class)
//public class WebsocketDemo {
//
//    /**
//     * Map<UserId, Session>：缓存Session，当服务端需要主动推送消息给客户端时，使用此缓存获取Session
//     */
//    private static final ConcurrentMap<String, Session> WEBSOCKET_SESSION_CACHE = new ConcurrentHashMap<>();
//
//    @Resource
//    private DistributedJobManager jobManager;
//
//    @Bean
//    public ServerEndpointExporter serverEndpointExporter() {
//        return new ServerEndpointExporter();
//    }
//
//    @OnOpen
//    public void onOpen(Session session, EndpointConfig endpointConfig, @PathParam("taskId") Long taskId) {
//        log.info("OnOpen: " + session.getId() + " -> " + taskId);
//
//        HttpSession httpSession = (HttpSession) endpointConfig.getUserProperties().get(HttpSession.class.getName());
//
//        // user id such as from jwt token
//        WEBSOCKET_SESSION_CACHE.put("userId", session);
//
//        session.getAsyncRemote().sendText("OnOpen AsyncRemote send: " + session.getId() + " -> " + taskId);
//
//        if (taskId == 123) {
//            RuntimeException ex = new RuntimeException();
//            throw ex;
//        }
//    }
//
//    @OnMessage
//    public void onMessage(Session session, String message) throws IOException {
//        if (message.equals("test")) {
//            RuntimeException ex = new RuntimeException();
//            throw ex;
//        }
//        log.info("OnMessage: " + session.getId() + " -> " + message);
//        session.getBasicRemote().sendText("OnMessage BasicRemote send: " + session.getId() + " -> " + message);
//    }
//
//    @OnError
//    public void onError(Session session, Throwable throwable) {
//        log.error("OnError: " + session.getId() + " -> " + throwable.getMessage());
//        try {
//            session.getBasicRemote().sendText("OnError send: " + session.getId());
//
//            session.close();
//        } catch (IOException e) {
//            log.error("OnError failed: " + session.getId(), e);
//        }
//    }
//
//    @OnClose
//    public void onClose(Session session, @PathParam("taskId") Long taskId) {
//        log.info("OnClose: " + session);
//
//        // user id such as from jwt token
//        WEBSOCKET_SESSION_CACHE.remove("userId");
//
//        try {
//            // Message will not be sent because the WebSocket session has been closed
//            //session.getBasicRemote().sendText("OnClose send: " + session.getId()); // session has been closed
//
//            session.close();
//            log.info("OnClose success: " + session.getId());
//        } catch (IOException e) {
//            log.error("OnClose failed: " + session.getId(), e);
//        }
//    }
//
//    public static class WebsocketConfigurator extends ServerEndpointConfig.Configurator {
//        @Override
//        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
//            System.out.println("==============>modifyHandshake");
//        }
//
//        @Override
//        public <T> T getEndpointInstance(Class<T> clazz) {
//            return SpringContextHolder.getBean(clazz);
//        }
//    }
//
//}
