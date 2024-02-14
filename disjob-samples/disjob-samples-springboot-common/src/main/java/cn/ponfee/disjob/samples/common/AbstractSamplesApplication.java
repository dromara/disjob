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

package cn.ponfee.disjob.samples.common;

import cn.ponfee.disjob.common.spring.EnableJacksonDateConfigurer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * <pre>
 * Abstract samples application
 *
 * `@ComponentScans(@ComponentScan("xxx"))：会合并SpringBootApplication原本的扫描方式
 * </pre>
 *
 * @author Ponfee
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"cn.ponfee.disjob.samples.common", "cn.ponfee.disjob.test.handler"})
@EnableJacksonDateConfigurer // 解决日期反序列化报错的问题
public abstract class AbstractSamplesApplication {

}
