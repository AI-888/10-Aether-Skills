/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.aliyun.openservices.shade.io.netty.channel.kqueue;

import com.aliyun.openservices.shade.io.netty.channel.ChannelOption;
import com.aliyun.openservices.shade.io.netty.channel.RecvByteBufAllocator;
import com.aliyun.openservices.shade.io.netty.channel.unix.UnixChannelOption;
import com.aliyun.openservices.shade.io.netty.util.internal.UnstableApi;

@UnstableApi
public final class KQueueChannelOption<T> extends UnixChannelOption<T> {
    public static final ChannelOption<Integer> SO_SNDLOWAT = valueOf(KQueueChannelOption.class, "SO_SNDLOWAT");
    public static final ChannelOption<Boolean> TCP_NOPUSH = valueOf(KQueueChannelOption.class, "TCP_NOPUSH");
    public static final ChannelOption<AcceptFilter> SO_ACCEPTFILTER =
            valueOf(KQueueChannelOption.class, "SO_ACCEPTFILTER");
    /**
     * If this is {@code true} then the {@link RecvByteBufAllocator.Handle#guess()} will be overridden to always attempt
     * to read as many bytes as kqueue says are available.
     */
    public static final ChannelOption<Boolean> RCV_ALLOC_TRANSPORT_PROVIDES_GUESS =
            valueOf(KQueueChannelOption.class, "RCV_ALLOC_TRANSPORT_PROVIDES_GUESS");

    @SuppressWarnings({ "unused", "deprecation" })
    private KQueueChannelOption() {
    }
}
