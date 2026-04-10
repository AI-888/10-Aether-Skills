/*
 * Copyright 2015 The Netty Project
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

package com.aliyun.openservices.shade.io.netty.example.http2.tiles;

import static com.aliyun.openservices.shade.io.netty.handler.codec.http2.Http2SecurityUtil.CIPHERS;
import com.aliyun.openservices.shade.io.netty.bootstrap.ServerBootstrap;
import com.aliyun.openservices.shade.io.netty.channel.Channel;
import com.aliyun.openservices.shade.io.netty.channel.ChannelFuture;
import com.aliyun.openservices.shade.io.netty.channel.ChannelInitializer;
import com.aliyun.openservices.shade.io.netty.channel.ChannelOption;
import com.aliyun.openservices.shade.io.netty.channel.EventLoopGroup;
import com.aliyun.openservices.shade.io.netty.channel.socket.SocketChannel;
import com.aliyun.openservices.shade.io.netty.channel.socket.nio.NioServerSocketChannel;
import com.aliyun.openservices.shade.io.netty.handler.ssl.ApplicationProtocolConfig;
import com.aliyun.openservices.shade.io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import com.aliyun.openservices.shade.io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import com.aliyun.openservices.shade.io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import com.aliyun.openservices.shade.io.netty.handler.ssl.ApplicationProtocolNames;
import com.aliyun.openservices.shade.io.netty.handler.ssl.SslContext;
import com.aliyun.openservices.shade.io.netty.handler.ssl.SslContextBuilder;
import com.aliyun.openservices.shade.io.netty.handler.ssl.SupportedCipherSuiteFilter;
import com.aliyun.openservices.shade.io.netty.handler.ssl.util.SelfSignedCertificate;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

/**
 * Demonstrates an Http2 server using Netty to display a bunch of images and
 * simulate latency. It is a Netty version of the <a href="https://http2.golang.org/gophertiles?latency=0">
 * Go lang HTTP2 tiles demo</a>.
 */
public class Http2Server {

    public static final int PORT = Integer.parseInt(System.getProperty("http2-port", "8443"));

    private final EventLoopGroup group;

    public Http2Server(EventLoopGroup eventLoopGroup) {
        group = eventLoopGroup;
    }

    public ChannelFuture start() throws Exception {
        final SslContext sslCtx = configureTLS();
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(group).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler());
            }
        });

        Channel ch = b.bind(PORT).sync().channel();
        return ch.closeFuture();
    }

    private static SslContext configureTLS() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        ApplicationProtocolConfig apn = new ApplicationProtocolConfig(
                Protocol.ALPN,
                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                SelectorFailureBehavior.NO_ADVERTISE,
                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_2,
                ApplicationProtocolNames.HTTP_1_1);

        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey(), null)
                                .ciphers(CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                .applicationProtocolConfig(apn).build();
    }
}
