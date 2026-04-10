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
package com.aliyun.openservices.shade.io.netty.example.stomp.websocket;

import com.aliyun.openservices.shade.io.netty.buffer.ByteBuf;
import com.aliyun.openservices.shade.io.netty.channel.ChannelHandler.Sharable;
import com.aliyun.openservices.shade.io.netty.channel.ChannelHandlerContext;
import com.aliyun.openservices.shade.io.netty.handler.codec.MessageToMessageCodec;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.websocketx.WebSocketFrame;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import com.aliyun.openservices.shade.io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import com.aliyun.openservices.shade.io.netty.handler.codec.stomp.StompSubframeAggregator;
import com.aliyun.openservices.shade.io.netty.handler.codec.stomp.StompSubframeDecoder;
import com.aliyun.openservices.shade.io.netty.handler.codec.stomp.StompSubframeEncoder;

import java.util.List;

@Sharable
public class StompWebSocketProtocolCodec extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {

    private final StompChatHandler stompChatHandler = new StompChatHandler();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            StompVersion stompVersion = StompVersion.findBySubProtocol(((HandshakeComplete) evt).selectedSubprotocol());
            ctx.channel().attr(StompVersion.CHANNEL_ATTRIBUTE_KEY).set(stompVersion);
            ctx.pipeline()
               .addLast(new StompSubframeDecoder())
               .addLast(new StompSubframeEncoder())
               .addLast(new StompSubframeAggregator(65536))
               .addLast(stompChatHandler)
               .remove(StompWebSocketClientPageHandler.INSTANCE);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf stompFrame, List<Object> out) {
        out.add(new TextWebSocketFrame(stompFrame.retain()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame, List<Object> out) {
        if (webSocketFrame instanceof TextWebSocketFrame) {
            out.add(webSocketFrame.content().retain());
        } else {
            ctx.close();
        }
    }
}
