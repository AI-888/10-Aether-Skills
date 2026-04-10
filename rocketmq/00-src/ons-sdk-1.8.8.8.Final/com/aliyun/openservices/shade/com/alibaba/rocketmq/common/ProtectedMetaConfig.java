/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aliyun.openservices.shade.com.alibaba.rocketmq.common;

import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.annotation.JSONField;
import com.aliyun.openservices.shade.com.alibaba.fastjson.serializer.SerializerFeature;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.protocol.RemotingSerializable;

import java.io.Writer;
import java.util.Map;
import java.util.Properties;

/**
 * Store protected meta config.
 * It is used to make sure these fields cannot be modified after persisting into disk.
 */
public class ProtectedMetaConfig extends ConfigManager {
    private static transient  final int ABSENT = -1;
    private static transient  final String ABSENT_STR = String.valueOf(ABSENT);

    private int timerPrecisionMs = ABSENT;

    private  String thisConfigFilePath;

    public ProtectedMetaConfig(String thisConfigFilePath) {
        this.thisConfigFilePath = thisConfigFilePath;
    }

    public ProtectedMetaConfig() {
    }

    /**
     * Check whether {@code targetProperties} contains same key but different value from {@code protectedProperties}.
     * @param protectedProperties The protected properties.
     * @param targetProperties The properties need to be updated.
     * @return The first key whose values in {@code protectedProperties} and {@code targetProperties} are different.
     */
    public static String diffProperties(Properties protectedProperties, Properties targetProperties) {
        for (Map.Entry<Object, Object> entry : protectedProperties.entrySet()) {
            if (ABSENT_STR.equals(entry.getValue())) {
                continue;
            }

            Object targetValue = targetProperties.getProperty((String) entry.getKey());
            if (targetValue != null && !targetValue.equals(entry.getValue())) {
                return (String) entry.getKey();
            }
        }
        return null;
    }

    public String diffProperties(Properties targetProperties) {
        Properties protectedProperties = MixAll.object2Properties(this);
        return diffProperties(protectedProperties, targetProperties);
    }

    /**
     * Update fields whose value is {@code ABSENT}.
     * @param targetProperties The target properties.
     * @return Whether any fields have been updated.
     */
    public boolean updateIfAbsent(Properties targetProperties) {
        boolean updated = false;
        Properties protectedProperties = MixAll.object2Properties(this);
        for (Map.Entry<Object, Object> entry : protectedProperties.entrySet()) {
            if (ABSENT_STR.equals(entry.getValue())) {
                Object newValue = targetProperties.getProperty((String) entry.getKey());
                if (null != newValue) {
                    protectedProperties.put(entry.getKey(), newValue);
                    updated = true;
                }
            }
        }
        if (updated) {
            MixAll.properties2Object(protectedProperties, this);
        }
        return updated;
    }

    public int getTimerPrecisionMs() {
        return timerPrecisionMs;
    }

    public void setTimerPrecisionMs(int timerPrecisionMs) {
        this.timerPrecisionMs = timerPrecisionMs;
    }

    public String encode() {
        Properties properties = MixAll.object2Properties(this);
        return MixAll.properties2String(properties);
    }

    public String encode(boolean prettyFormat) {
        return encode();
    }

    @Override
    public void decode(String content) {
        if (content != null) {
            ProtectedMetaConfig obj = RemotingSerializable.fromJson(content, ProtectedMetaConfig.class);
            if (obj != null) {
                this.timerPrecisionMs = obj.getTimerPrecisionMs();
            }
        }
    }

    @Override
    protected void write0(Writer writer) {
        JSON.writeJSONString(writer, this, SerializerFeature.BrowserCompatible);
    }

    @Override
    public String configFilePath() {
        return thisConfigFilePath;
    }


    @JSONField(serialize = false)
    public String getThisConfigFilePath() {
        return thisConfigFilePath;
    }

    public void setThisConfigFilePath(String thisConfigFilePath) {
        this.thisConfigFilePath = thisConfigFilePath;
    }
}
