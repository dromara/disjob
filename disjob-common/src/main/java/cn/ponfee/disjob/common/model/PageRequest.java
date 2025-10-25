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

package cn.ponfee.disjob.common.model;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.collect.TypedDictionary;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.beans.Transient;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * <pre>
 * Page request for pageable query
 *
 * 1、SpringMVC form表单嵌套对象传参(即Request Header “Content-Type=application/x-www-form-urlencoded”)：
 *   1）普通对象：<input type="text" name="address.city" />
 *   2）Map对象：<input type="text" name="params['city']" />
 *
 * 2、Mybatis xml中使用方式：#{params.city}
 * </pre>
 *
 * @author Ponfee
 */
@Getter
@Setter
@SuppressWarnings("SuspiciousMethodCalls")
public class PageRequest extends Page implements TypedDictionary<String, Object> {
    private static final long serialVersionUID = 2032344850017264330L;

    /**
     * Is whether page query.
     * <p>如数据导出场景时，设置`paged=false`导出全部数据
     */
    private boolean paged = true;

    /**
     * Parameter of query condition
     */
    private Map<String, Object> params = new HashMap<>();

    @Override
    public Object get(Object key) {
        return params.get(key);
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    @Override
    public Object put(String key, Object value) {
        return params.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return params.remove(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return params.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return params.containsValue(value);
    }

    @Transient
    public long getOffset() {
        Assert.state(paged, "Unpaged unsupported get offset operation.");
        return (long) (pageNumber - 1) * pageSize;
    }

    public <P extends PageRequest, A> PageResponse<A> query(ToLongFunction<P> countQuerier,
                                                            Function<P, List<A>> recordQuerier) {
        return query(countQuerier, recordQuerier, null);
    }

    @SuppressWarnings("unchecked")
    public <P extends PageRequest, A, B> PageResponse<B> query(ToLongFunction<P> countQuerier,
                                                               Function<P, List<A>> recordQuerier,
                                                               Function<A, B> recordMapper) {
        long total;
        List<A> list;
        if (paged) {
            Assert.isTrue(pageNumber > 0, "Paged number must be greater than 0.");
            Assert.isTrue(pageSize > 0, "Paged size must be greater than 0.");
            total = countQuerier.applyAsLong((P) this);
            // Correct exceed page number
            this.pageNumber = Math.min(pageNumber, computeTotalPages(pageSize, total));
            list = (total == 0) ? Collections.emptyList() : recordQuerier.apply((P) this);
        } else {
            list = recordQuerier.apply((P) this);
            total = list.size();
            this.pageNumber = 1;
            this.pageSize = list.size();
        }
        return toPageResponse(total, recordMapper == null ? (List<B>) list : Collects.convert(list, recordMapper));
    }

    private <B> PageResponse<B> toPageResponse(long total, List<B> records) {
        PageResponse<B> response = new PageResponse<>();
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setSort(sort);
        response.setTotal(total);
        response.setRecords(records == null ? Collections.emptyList() : records);
        return response;
    }

}
