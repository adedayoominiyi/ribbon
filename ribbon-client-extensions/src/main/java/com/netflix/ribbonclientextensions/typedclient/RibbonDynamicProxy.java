/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.ribbonclientextensions.typedclient;

import com.netflix.ribbonclientextensions.http.HttpResourceGroup;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author Tomasz Bak
 */
public class RibbonDynamicProxy<T> implements InvocationHandler {
    private final Map<Method, MethodTemplateExecutor> templateExecutorMap;

    public RibbonDynamicProxy(Class<T> clientInterface, HttpResourceGroup httpResourceGroup) {
        ClassTemplate<T> classTemplate = ClassTemplate.from(clientInterface);
        if (httpResourceGroup == null) {
            httpResourceGroup = new HttpResourceGroupFactory<T>(classTemplate).createResourceGroup();
        }
        templateExecutorMap = MethodTemplateExecutor.from(httpResourceGroup, clientInterface);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodTemplateExecutor template = templateExecutorMap.get(method);
        if (template != null) {
            return template.executeFromTemplate(args);
        }
        // This must be one of the Object methods. Lets run it on the handler itself.
        return Utils.executeOnInstance(this, method, args);
    }

    @Override
    public String toString() {
        return "RibbonDynamicProxy{...}";
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clientInterface, HttpResourceGroup httpResourceGroup) {
        if (!clientInterface.isInterface()) {
            throw new IllegalArgumentException(clientInterface.getName() + " is a class not interface");
        }
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{clientInterface},
                new RibbonDynamicProxy<T>(clientInterface, httpResourceGroup)
        );
    }
}
