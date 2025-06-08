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

package cn.ponfee.disjob.id.snowflake.zk;

import lombok.Getter;
import lombok.Setter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Zookeeper config
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ZkConfig implements java.io.Serializable {
    private static final long serialVersionUID = -2679723559759163644L;

    private String connectString = "localhost:2181";
    private String username;
    private String password;

    private int connectionTimeoutMs = 5 * 1000;
    private int sessionTimeoutMs = 5 * 60 * 1000;

    private int baseSleepTimeMs = 50;
    private int maxRetries = 10;
    private int maxSleepMs = 500;
    private int maxWaitTimeMs = 5000;

    public String authorization() {
        if (isEmpty(username)) {
            return isEmpty(password) ? null : ":" + password;
        }
        return username + ":" + (isEmpty(password) ? "" : password);
    }

    public CuratorFramework createCuratorFramework() throws InterruptedException {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .connectionTimeoutMs(connectionTimeoutMs)
            .sessionTimeoutMs(sessionTimeoutMs)
            .retryPolicy(new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs));

        String authorization = authorization();
        if (authorization != null) {
            builder.authorization("digest", authorization.getBytes());
        }

        CuratorFramework curatorFramework = builder.build();

        curatorFramework.start();
        boolean isStarted = curatorFramework.getState() == CuratorFrameworkState.STARTED;
        Assert.state(isStarted, () -> "Snowflake curator framework not started: " + curatorFramework.getState());
        boolean isConnected = curatorFramework.blockUntilConnected(5000, TimeUnit.MILLISECONDS);
        Assert.state(isConnected, () -> "Snowflake curator framework not connected: " + curatorFramework.getState());
        return curatorFramework;
    }

}
