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
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 *   When/Then:
 *     1）when(yourMock.yourMethod()).thenReturn(yourReturnValue);
 *     2）when(yourMock.yourMethod()).thenThrow();
 *   Do/When:
 *     1）doReturn(yourReturnValue).when(yourMock).yourMethod();
 *     2）doNothing().when(yourMock).yourMethod();
 *     3）doThrow(Throwable).when(yourMock).yourMethod();
 *   Given/Will:
 *     1）given(yourMethod()).willThrow(OutOfMemoryException.class);
 *   Will/Given/Do:
 *     1）willReturn(any()).given(yourMethod()).doNothing();
 *   Verify/Do:
 *     1）verify(yourMethod()).doThrow(SomeException.class);
 *
 * 6、MockedStatic<A> mockedStatic = Mockito.mockStatic(A.class)
 *    1）when...thenReturn后调用静态方法返回mock结果
 *    2）reset()后调用静态方法都返回null，会重置verify上下文
 *    3）close()后调用静态方法返回正常且不能再使用`mockedStatic`对象
 * </pre>
 *
 * @author Ponfee
 */
// 测试类(方法)要使用“@org.junit.jupiter.api.Test”来注解
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public abstract class SpringBootTestMockito {

    static {
        // Mock CoreUtils.getLocalHost返回`127.0.0.1`，支持在断网时能跑测试用例
        MockedStatic<CoreUtils> mockedStaticCoreUtils = mockStatic(CoreUtils.class);
        String localhostIp = "127.0.0.1";

        // Mock CoreUtils#getLocalHost(String)
        mockedStaticCoreUtils.when(() -> CoreUtils.getLocalHost(any())).thenReturn(localhostIp);
        assertEquals(localhostIp, CoreUtils.getLocalHost(null));
        mockedStaticCoreUtils.verify(() -> CoreUtils.getLocalHost(any()));

        // Mock CoreUtils#getLocalHost()
        mockedStaticCoreUtils.when(CoreUtils::getLocalHost).thenReturn(localhostIp);
        assertEquals(localhostIp, CoreUtils.getLocalHost());
        mockedStaticCoreUtils.verify(CoreUtils::getLocalHost);
    }

    // Only reset mock field which is defined in SpringBootTestMockito
    private static final List<Field> MOCKED_FIELDS = FieldUtils.getAllFieldsList(SpringBootTestMockito.class)
        .stream()
        .filter(e -> !Modifier.isStatic(e.getModifiers()))
        .filter(e -> e.isAnnotationPresent(MockitoBean.class) || MockedStatic.class.isAssignableFrom(e.getType()))
        .peek(e -> assertTrue(Modifier.isProtected(e.getModifiers()), () -> "Mock field must be protected: " + e.toGenericString()))
        .collect(Collectors.toList());

    // --------------------------------------mock bean definition

    @MockitoBean
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
                    throw new RuntimeException("Mocked object cannot be null: " + field.toGenericString());
                } else if (mockedObj instanceof MockedStatic) {
                    ((MockedStatic<?>) mockedObj).close();
                } else {
                    reset(mockedObj);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Mocked object reset error: " + field.toGenericString(), ex);
            }
        }
    }

}
