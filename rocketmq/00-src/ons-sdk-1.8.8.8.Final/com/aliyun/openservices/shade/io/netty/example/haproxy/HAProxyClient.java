/*
 * Copyright 2020 The Netty Project
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

package com.aliyun.openservices.shade.io.netty.example.haproxy;

import com.aliyun.openservices.shade.io.netty.bootstrap.Bootstrap;
import com.aliyun.openservices.shade.io.netty.buffer.Unpooled;
import com.aliyun.openservices.shade.io.netty.channel.Channel;
import com.aliyun.openservices.shade.io.netty.channel.EventLoopGroup;
import com.aliyun.openservices.shade.io.netty.channel.nio.NioEventLoopGroup;
import com.aliyun.openservices.shade.io.netty.channel.socket.nio.NioSocketChannel;
import com.aliyun.openservices.shade.io.netty.handler.codec.haproxy.HAProxyCommand;
import com.aliyun.openservices.shade.io.netty.handler.codec.haproxy.HAProxyMessage;
import com.aliyun.openservices.shade.io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import com.aliyun.openservices.shade.io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import com.aliyun.openservices.shade.io.netty.util.CharsetUtil;

import static com.aliyun.openservices.shade.io.netty.example.haproxy.HAProxyServer.*;

public final class HAProxyClient {

    private static final String HOST = System.getProperty("host", "127.0.0.1");

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new HAProxyHandler());

            // Start the connection attempt.
            Channel ch = b.connect(HOST, PORT).sync().channel();

            HAProxyMessage message = new HAProxyMessage(
                    HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4,
                    "127.0.0.1", "127.0.0.2", 8000, 9000);

            ch.writeAndFlush(message).sync();
            ch.writeAndFlush(Unpooled.copiedBuffer("Hello World!", CharsetUtil.US_ASCII)).sync();
            ch.writeAndFlush(Unpooled.copiedBuffer("Bye now!", CharsetUtil.US_ASCII)).sync();
            ch.close().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
