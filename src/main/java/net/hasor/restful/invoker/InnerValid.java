/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.restful.invoker;
import net.hasor.restful.ValidErrors;
import net.hasor.restful.Validation;
import net.hasor.restful.api.Valid;
import net.hasor.restful.api.ValidBy;
import net.hasor.web.WebAppContext;
import org.more.bizcommon.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * 线程安全
 * @version : 2016-08-03
 * @author 赵永春 (zyc@hasor.net)
 */
class InnerValid {
    private Map<String, Valid>    validMap     = null;
    private Map<String, Class<?>> paramTypeMap = null;
    InnerValid(Map<String, Valid> validMap, Map<String, Class<?>> paramTypeMap) {
        this.validMap = validMap;
        this.paramTypeMap = paramTypeMap;
    }
    //
    Map<String, ValidData> doValid(WebAppContext appContext, Object[] resolveParams) {
        final ConcurrentMap<String, ValidData> validDateMap = new ConcurrentHashMap<String, ValidData>();
        for (String paramIndex : this.validMap.keySet()) {
            //
            Valid valid = this.validMap.get(paramIndex);
            if (valid == null) {
                continue;
            }
            final String validName = valid.value();
            Class<?> paramType = this.paramTypeMap.get(paramIndex);
            ValidBy validBy = paramType.getAnnotation(ValidBy.class);
            if (validBy == null) {
                onErrors(validDateMap, validName, new ValidData(validName, "@ValidBy is Undefined."));
                continue;
            }
            //
            Validation validation = appContext.getInstance(validBy.value());
            if (validation == null) {
                onErrors(validDateMap, validName, new ValidData(validName, "program is not exist."));
                continue;
            }
            //
            Object paramObj = resolveParams[Integer.valueOf(paramIndex)];
            validation.doValidation(validName, paramObj, new ValidErrors() {
                @Override
                public void addError(String validString) {
                    onErrors(validDateMap, validName, new ValidData(validName, validString));
                }
                @Override
                public void addError(Message validMessage) {
                    onErrors(validDateMap, validName, new ValidData(validName, validMessage));
                }
                @Override
                public void addErrors(List<Message> validMessage) {
                    ValidData newDate = new ValidData(validName);
                    newDate.addAll(validMessage);
                    onErrors(validDateMap, validName, newDate);
                }
            });
        }
        return validDateMap;
    }
    private static void onErrors(ConcurrentMap<String, ValidData> validDateMap, String validName, ValidData newDate) {
        if (newDate == null) {
            return;
        }
        validDateMap.putIfAbsent(validName, newDate);
        ValidData data = validDateMap.get(validName);
        if (data != newDate) {
            data.addAll(newDate);
        }
    }
}