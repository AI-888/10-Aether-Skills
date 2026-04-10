/*
 * Copyright 2013 The Netty Project
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
package com.aliyun.openservices.shade.io.netty.example.http.helloworld;

import com.aliyun.openservices.shade.io.netty.buffer.Unpooled;
import com.aliyun.openservices.shade.io.netty.channel.ChannelFuture;
import com.aliyun.openservices.shade.io.netty.channel.ChannelFutureListener;
import com.aliyun.openservices.shade.io.netty.channel.ChannelHandlerContext;
import com.aliyun.openservices.shade.io.netty.channel.SimpleChannelInboundHandler;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.DefaultFullHttpResponse;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.FullHttpResponse;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpObject;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpRequest;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpUtil;

import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static com.aliyun.openservices.shade.io.netty.handler.codec.http.HttpResponseStatus.OK;

public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            boolean keepAlive = HttpUtil.isKeepAlive(req);
            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK,
                                                                    Unpooled.wrappedBuffer(CONTENT));
            response.headers()
                    .set(CONTENT_TYPE, TEXT_PLAIN)
                    .setInt(CONTENT_LENGTH, response.content().readableBytes());

            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                // Tell the client we're going to close the connection.
                response.headers().set(CONNECTION, CLOSE);
            }

            ChannelFuture f = ctx.write(response);

            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
