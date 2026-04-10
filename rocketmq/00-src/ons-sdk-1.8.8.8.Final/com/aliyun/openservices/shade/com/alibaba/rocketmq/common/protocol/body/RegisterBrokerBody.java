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

import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSONReader;
import com.aliyun.openservices.shade.com.alibaba.fastjson.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.DataVersion;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.MixAll;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.QueueGroupConfig;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.TopicConfig;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.constant.LoggerName;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.logging.InternalLogger;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.logging.InternalLoggerFactory;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.protocol.ByteBufferInputStream;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.protocol.RemotingSerializable;

public class RegisterBrokerBody extends RemotingSerializable {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    private TopicConfigSerializeWrapper topicConfigSerializeWrapper = new TopicConfigSerializeWrapper();
    private List<String> filterServerList = new ArrayList<String>();

    public byte[] encode(boolean compress) {
        if (!compress) {
            return super.encode();
        }
        long start = System.currentTimeMillis();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream outputStream = new DeflaterOutputStream(byteArrayOutputStream, new Deflater(Deflater.BEST_COMPRESSION));
        DataVersion dataVersion = topicConfigSerializeWrapper.getDataVersion();
        ConcurrentMap<String, TopicConfig> topicConfigTable = cloneTopicConfigTable(topicConfigSerializeWrapper.getTopicConfigTable());
        ConcurrentMap<String, QueueGroupConfig> queueGroupConfigTable = cloneQueueGroupConfigTable(topicConfigSerializeWrapper.getQueueGroupConfigTable());
        assert topicConfigTable != null;
        try {
            byte[] buffer = dataVersion.encode();

            // write data version
            outputStream.write(convertIntToByteArray(buffer.length));
            outputStream.write(buffer);

            int topicNumber = topicConfigTable.size();

            // write number of topic configs
            outputStream.write(convertIntToByteArray(topicNumber));

            // write topic config entry one by one.
            for (ConcurrentMap.Entry<String, TopicConfig> next : topicConfigTable.entrySet()) {
                buffer = next.getValue().encode().getBytes(MixAll.DEFAULT_CHARSET);
                outputStream.write(convertIntToByteArray(buffer.length));
                outputStream.write(buffer);
            }

            buffer = JSON.toJSONString(filterServerList).getBytes(MixAll.DEFAULT_CHARSET);

            // write filter server list json length
            outputStream.write(convertIntToByteArray(buffer.length));

            // write filter server list json
            outputStream.write(buffer);
            
            buffer = JSON.toJSONString(queueGroupConfigTable).getBytes(MixAll.DEFAULT_CHARSET);
            outputStream.write(convertIntToByteArray(buffer.length));
            outputStream.write(buffer);

            outputStream.finish();
            long interval = System.currentTimeMillis() - start;
            if (interval > 50) {
                LOGGER.info("Compressing takes {}ms", interval);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Failed to compress RegisterBrokerBody object", e);
        }
        return null;
    }

    public static RegisterBrokerBody streamParse(ByteBuffer byteBuffer) {
        JSONReader reader = new JSONReader(new InputStreamReader(new ByteBufferInputStream(byteBuffer)));
        RegisterBrokerBody registerBrokerBody = new RegisterBrokerBody();
        reader.startObject();
        while (reader.hasNext()) {
            String key = reader.readString();
            if ("filterServerList".equals(key)) {
                reader.startArray();
                while (reader.hasNext()) {
                    registerBrokerBody.getFilterServerList().add(reader.readString());
                }
                reader.endArray();
                continue;
            }
            if ("topicConfigSerializeWrapper".equals(key)) {
                reader.startObject();
                while (reader.hasNext()) {
                    String member = reader.readString();
                    if ("dataVersion".equals(member)) {
                        DataVersion dataVersion = reader.readObject(DataVersion.class);
                        registerBrokerBody.getTopicConfigSerializeWrapper().setDataVersion(dataVersion);
                        continue;
                    }

                    if ("topicConfigTable".equals(member)) {
                        reader.startObject();
                        while (reader.hasNext()) {
                            String topic = reader.readString();
                            TopicConfig topicConfig = reader.readObject(TopicConfig.class);
                            registerBrokerBody.getTopicConfigSerializeWrapper().getTopicConfigTable()
                                .put(topic, topicConfig);
                        }
                        reader.endObject();
                        continue;
                    }

                    if ("queueGroupConfigTable".equals(member)) {
                        reader.startObject();
                        while (reader.hasNext()) {
                            String topic = reader.readString();
                            QueueGroupConfig queueGroupConfig = reader.readObject(QueueGroupConfig.class);
                            registerBrokerBody.getTopicConfigSerializeWrapper().getQueueGroupConfigTable()
                                .put(topic, queueGroupConfig);
                        }
                        reader.endObject();
                    }
                }
                reader.endObject();
            }
        }
        reader.endObject();
        reader.close();
        return registerBrokerBody;
    }

    public static RegisterBrokerBody decode(ByteBuffer data, boolean compressed) throws IOException {
        if (!compressed) {
            return streamParse(data);
        }
        long start = System.currentTimeMillis();
        InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteBufferInputStream(data));
        int dataVersionLength = readInt(inflaterInputStream);
        byte[] dataVersionBytes = readBytes(inflaterInputStream, dataVersionLength);
        DataVersion dataVersion = DataVersion.decode(dataVersionBytes, DataVersion.class);

        RegisterBrokerBody registerBrokerBody = new RegisterBrokerBody();
        registerBrokerBody.getTopicConfigSerializeWrapper().setDataVersion(dataVersion);
        ConcurrentMap<String, TopicConfig> topicConfigTable =
            registerBrokerBody.getTopicConfigSerializeWrapper().getTopicConfigTable();

        int topicConfigNumber = readInt(inflaterInputStream);
        LOGGER.debug("{} topic configs to extract", topicConfigNumber);

        for (int i = 0; i < topicConfigNumber; i++) {
            int topicConfigJsonLength = readInt(inflaterInputStream);

            byte[] buffer = readBytes(inflaterInputStream, topicConfigJsonLength);
            TopicConfig topicConfig = new TopicConfig();
            String topicConfigJson = new String(buffer, MixAll.DEFAULT_CHARSET);
            topicConfig.decode(topicConfigJson);
            topicConfigTable.put(topicConfig.getTopicName(), topicConfig);
        }

        int filterServerListJsonLength = readInt(inflaterInputStream);

        byte[] filterServerListBuffer = readBytes(inflaterInputStream, filterServerListJsonLength);
        String filterServerListJson = new String(filterServerListBuffer, MixAll.DEFAULT_CHARSET);
        List<String> filterServerList = new ArrayList<String>();
        try {
            filterServerList = JSON.parseArray(filterServerListJson, String.class);
        } catch (Exception e) {
            LOGGER.error("Decompressing occur Exception {}", filterServerListJson);
        }

        registerBrokerBody.setFilterServerList(filterServerList);

        if (inflaterInputStream.available() != 0) {
            try {
                int queueGroupConfigTableJsonLength = readInt(inflaterInputStream);
                byte[] queueGroupConfigTableBuffer = readBytes(inflaterInputStream, queueGroupConfigTableJsonLength);
                String queueGroupConfigTableJson = new String(queueGroupConfigTableBuffer, MixAll.DEFAULT_CHARSET);
                ConcurrentMap<String, QueueGroupConfig> queueGroupConfigTable =
                    JSON.parseObject(queueGroupConfigTableJson, new TypeReference<ConcurrentMap<String, QueueGroupConfig>>() {
                    });
                registerBrokerBody.getTopicConfigSerializeWrapper().setQueueGroupConfigTable(queueGroupConfigTable);
            } catch (IOException e) {
                if (inflaterInputStream.available() != 0) {
                    throw e;
                }
            }
        }

        long interval = System.currentTimeMillis() - start;
        if (interval > 50) {
            LOGGER.info("Decompressing takes {}ms", interval);
        }
        return registerBrokerBody;
    }

    private static byte[] convertIntToByteArray(int n) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(n);
        return byteBuffer.array();
    }

    private static byte[] readBytes(InflaterInputStream inflaterInputStream, int length) throws IOException {
        byte[] buffer = new byte[length];
        int bytesRead = 0;
        while (bytesRead < length) {
            int len = inflaterInputStream.read(buffer, bytesRead, length - bytesRead);
            if (len == -1) {
                throw new IOException("End of compressed data has reached");
            } else {
                bytesRead += len;
            }
        }
        return buffer;
    }

    private static int readInt(InflaterInputStream inflaterInputStream) throws IOException {
        byte[] buffer = readBytes(inflaterInputStream, 4);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        return byteBuffer.getInt();
    }

    public TopicConfigSerializeWrapper getTopicConfigSerializeWrapper() {
        return topicConfigSerializeWrapper;
    }

    public void setTopicConfigSerializeWrapper(TopicConfigSerializeWrapper topicConfigSerializeWrapper) {
        this.topicConfigSerializeWrapper = topicConfigSerializeWrapper;
    }

    public List<String> getFilterServerList() {
        return filterServerList;
    }

    public void setFilterServerList(List<String> filterServerList) {
        this.filterServerList = filterServerList;
    }

    public static ConcurrentMap<String, TopicConfig> cloneTopicConfigTable(
        ConcurrentMap<String, TopicConfig> topicConfigConcurrentMap) {
        return cloneTable(topicConfigConcurrentMap);
    }

    public static ConcurrentMap<String, QueueGroupConfig> cloneQueueGroupConfigTable(
        ConcurrentMap<String, QueueGroupConfig> queueGroupConfigConcurrentMap) {
        return cloneTable(queueGroupConfigConcurrentMap);
    }

    private static <T> ConcurrentMap<String, T> cloneTable(ConcurrentMap<String, T> table) {
        ConcurrentMap<String, T> result = new ConcurrentHashMap<String, T>();
        if (table != null) {
            for (Map.Entry<String, T> entry : table.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
