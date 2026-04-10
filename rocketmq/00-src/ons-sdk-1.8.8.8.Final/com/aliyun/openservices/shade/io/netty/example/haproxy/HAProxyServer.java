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

import com.aliyun.openservices.shade.io.netty.bootstrap.ServerBootstrap;
import com.aliyun.openservices.shade.io.netty.buffer.ByteBuf;
import com.aliyun.openservices.shade.io.netty.buffer.ByteBufUtil;
import com.aliyun.openservices.shade.io.netty.channel.ChannelHandlerContext;
import com.aliyun.openservices.shade.io.netty.channel.ChannelInitializer;
import com.aliyun.openservices.shade.io.netty.channel.EventLoopGroup;
import com.aliyun.openservices.shade.io.netty.channel.SimpleChannelInboundHandler;
import com.aliyun.openservices.shade.io.netty.channel.nio.NioEventLoopGroup;
import com.aliyun.openservices.shade.io.netty.channel.socket.SocketChannel;
import com.aliyun.openservices.shade.io.netty.channel.socket.nio.NioServerSocketChannel;
import com.aliyun.openservices.shade.io.netty.handler.codec.haproxy.HAProxyMessage;
import com.aliyun.openservices.shade.io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import com.aliyun.openservices.shade.io.netty.handler.logging.LogLevel;
import com.aliyun.openservices.shade.io.netty.handler.logging.LoggingHandler;

public final class HAProxyServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HAProxyServerInitializer());
            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    static class HAProxyServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(
                    new LoggingHandler(LogLevel.DEBUG),
                    new HAProxyMessageDecoder(),
                    new SimpleChannelInboundHandler() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof HAProxyMessage) {
                                System.out.println("proxy message: " + msg);
                            } else if (msg instanceof ByteBuf) {
                                System.out.println("bytebuf message: " + ByteBufUtil.prettyHexDump((ByteBuf) msg));
                            }
                        }
                    });
        }
    }
}
