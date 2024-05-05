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

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.util.DisjobUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;

/**
 * <pre>
 * Mockito test base class
 *
 * 1、@MockitoSettings(strictness = Strictness.STRICT_STUBS)
 *   等同于：@ExtendWith(MockitoExtension.class)
 *
 * 2、mock对象不执行具体逻辑，spy对象执行具体逻辑
 *
 * 3、用spy时会有区别：
 *   when().thenReturn()在返回指定值之前会调用真实方法；
 *   doReturn().when()根本不调用真实方法；
 *
 * 4、Mockito官方建议优先考虑使用when(...).thenReturn(...)，而不是doReturn(...).when(...)
 *
 * 5、Mockito语法：
 *   When/Then: when(yourMethod()).thenReturn(5);
 *   Given/Will: given(yourMethod()).willThrow(OutOfMemoryException.class);
 *   Do/When: doReturn(7).when(yourMock.fizzBuzz());
 *   Will/Given/Do: willReturn(any()).given(yourMethod()).doNothing();
 *   Verify/Do: verify(yourMethod()).doThrow(SomeException.class);
 * </pre>
 *
 * @author Ponfee
 */
// 测试类(方法)要使用“@org.junit.jupiter.api.Test”来注解
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public abstract class MockitoTestBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    static {
        // Mock DisjobUtils.getLocalHost返回`127.0.0.1`，支持在断网时能跑测试用例
        MockedStatic<DisjobUtils> mocked = mockStatic(DisjobUtils.class);
        mocked.when(() -> DisjobUtils.getLocalHost(any())).thenReturn(NetUtils.LOCAL_IP_ADDRESS);
        Assertions.assertEquals(NetUtils.LOCAL_IP_ADDRESS, DisjobUtils.getLocalHost(null));
        mocked.verify(() -> DisjobUtils.getLocalHost(any()));
    }

    // Only reset mock field which is defined in MockitoTestBase
    private static final List<Field> MOCKED_FIELDS = FieldUtils.getAllFieldsList(MockitoTestBase.class)
        .stream()
        .filter(e -> !Modifier.isStatic(e.getModifiers()))
        .filter(e -> e.isAnnotationPresent(MockBean.class) || MockedStatic.class.isAssignableFrom(e.getType()))
        .peek(e -> {
            if (!Modifier.isProtected(e.getModifiers())) {
                throw new AssertionError("Mock field must protected: " + e.toGenericString());
            }
        })
        .collect(Collectors.toList());

    // --------------------------------------mock bean definition

    @MockBean
    protected WorkerRpcService workerRpcService;

    // --------------------------------------mock static method definition

    protected MockedStatic<NetUtils> mockedStaticNetUtils;

    // --------------------------------------methods

    protected final void initMock() {
        mockedStaticNetUtils = mockStatic(NetUtils.class);
    }

    protected final void destroyMock() {
        for (Field field : MOCKED_FIELDS) {
            try {
                Object mockedObj = field.get(this);
                if (mockedObj == null) {
                    log.error("Mock field object is null: " + field.toGenericString());
                } else if (mockedObj instanceof MockedStatic) {
                    ((MockedStatic<?>) mockedObj).close();
                } else {
                    reset(mockedObj);
                }
            } catch (Exception ex) {
                log.error("Mock field object reset error: " + field.toGenericString(), ex);
            }
        }
    }

}
