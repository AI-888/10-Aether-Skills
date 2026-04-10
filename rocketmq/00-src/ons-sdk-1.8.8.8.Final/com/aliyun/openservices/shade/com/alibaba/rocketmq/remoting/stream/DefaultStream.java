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
package com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.stream;

import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.serializer.SerializerFeature;

import com.aliyun.openservices.shade.com.google.common.base.Stopwatch;
import com.aliyun.openservices.shade.io.netty.buffer.ByteBuf;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.aliyun.openservices.shade.io.netty.buffer.CompositeByteBuf;
import com.aliyun.openservices.shade.io.netty.buffer.PooledByteBufAllocator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.logging.InternalLogger;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.logging.InternalLoggerFactory;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.common.RemotingHelper;

public class DefaultStream implements Stream {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);

    public static final int INITIAL_SLICE_SIZE = 1024 * 64;
    public static final int MAX_SLICE_SIZE = 1024 * 1024;

    /**
     * If number of topics on current broker exceeds 65536, compositeByteBuf#addComponent would be very expensive.
     */
    public static final int MAX_COMPONENT_NUMBER = 32;

    private final CompositeByteBuf bufferChain;

    public DefaultStream() {
        bufferChain = PooledByteBufAllocator.DEFAULT.compositeDirectBuffer(MAX_COMPONENT_NUMBER);
    }

    protected void serialize() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        LinkedList<ByteBuf> bufferChain = new LinkedList<ByteBuf>();
        OutputStream outputStream = new BufferChainOutputStream(bufferChain, this.bufferChain.alloc());
        Writer writer = new OutputStreamWriter(outputStream);
        JSON.writeJSONString(writer, this, SerializerFeature.BrowserCompatible,
            SerializerFeature.SkipTransientField, SerializerFeature.QuoteFieldNames);
        for (ByteBuf buffer : bufferChain) {
            this.bufferChain.addComponent(true, buffer);
        }
        LOGGER.debug("Serialization costs {}ms. Component number: {}",
            stopwatch.stop().elapsed(TimeUnit.MILLISECONDS), this.bufferChain.numComponents());
        LOGGER.debug("PooledCompositeBuffer: readerIndex={}, writerIndex={}, capacity={}", this.bufferChain.readerIndex(),
            this.bufferChain.writerIndex(), this.bufferChain.capacity());
    }

    @Override
    public int payloadLength() {
        return bufferChain.readableBytes();
    }

    @Override
    public ByteBuf payload() {
        return bufferChain.slice();
    }
}
