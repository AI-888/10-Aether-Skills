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
package com.aliyun.openservices.shade.com.alibaba.rocketmq.common.protocol.body;

import com.aliyun.openservices.shade.com.alibaba.fastjson.JSONReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.protocol.ByteBufferInputStream;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.protocol.RemotingSerializable;

public class TopicList extends RemotingSerializable {
    private Set<String> topicList = new CopyOnWriteArraySet<String>();
    private String brokerAddr;

    public Set<String> getTopicList() {
        return topicList;
    }

    public void setTopicList(Set<String> topicList) {
        this.topicList = topicList;
    }

    public String getBrokerAddr() {
        return brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public static TopicList decode(final ByteBuffer data) {
        TopicList list = new TopicList();
        Set<String> tmpList = new HashSet<String>();
        InputStream inputStream = new ByteBufferInputStream(data);
        JSONReader reader = new JSONReader(new InputStreamReader(inputStream));
        reader.startObject();
        while (reader.hasNext()) {
            String member = reader.readString();
            if ("brokerAddr".equals(member)) {
                list.setBrokerAddr(reader.readString());
                continue;
            }

            if ("topicList".equals(member)) {
                reader.startArray();
                while (reader.hasNext()) {
                    tmpList.add(reader.readString());
                }
                reader.endArray();
                list.getTopicList().addAll(tmpList);
            }
        }
        reader.endObject();
        reader.close();
        return list;
    }
}
