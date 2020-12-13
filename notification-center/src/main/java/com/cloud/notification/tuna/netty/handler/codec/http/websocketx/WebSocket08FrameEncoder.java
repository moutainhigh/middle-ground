/*
 * Copyright 2012 The Netty Project
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
// (BSD License: http://www.opensource.org/licenses/bsd-license)
//
// Copyright (c) 2011, Joe Walnes and contributors
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or
// without modification, are permitted provided that the
// following conditions are met:
//
// * Redistributions of source code must retain the above
// copyright notice, this list of conditions and the
// following disclaimer.
//
// * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the
// following disclaimer in the documentation and/or other
// materials provided with the distribution.
//
// * Neither the name of the Webbit nor the names of
// its contributors may be used to endorse or promote products
// derived from this software without specific prior written
// permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
// CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
// OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.cloud.notification.tuna.netty.handler.codec.http.websocketx;

import com.cloud.notification.tuna.netty.buffer.ByteBuf;
import com.cloud.notification.tuna.netty.buffer.Unpooled;
import com.cloud.notification.tuna.netty.channel.ChannelHandlerContext;
import com.cloud.notification.tuna.netty.handler.codec.MessageToMessageEncoder;
import com.cloud.notification.tuna.netty.handler.codec.TooLongFrameException;
import com.cloud.notification.tuna.util.logging.InternalLogger;
import com.cloud.notification.tuna.util.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * <p>
 * Encodes a web socket frame into wire protocol version 8 format. This code was forked from <a
 * href="https://github.com/joewalnes/webbit">webbit</a> and modified.
 * </p>
 */
public class WebSocket08FrameEncoder extends MessageToMessageEncoder<WebSocketFrame> implements WebSocketFrameEncoder {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocket08FrameEncoder.class);

    private static final byte OPCODE_CONT = 0x0;
    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_BINARY = 0x2;
    private static final byte OPCODE_CLOSE = 0x8;
    private static final byte OPCODE_PING = 0x9;
    private static final byte OPCODE_PONG = 0xA;

    private final boolean maskPayload;

    /**
     * Constructor
     *
     * @param maskPayload
     *            Web socket clients must set this to true to mask payload. Server implementations must set this to
     *            false.
     */
    public WebSocket08FrameEncoder(boolean maskPayload) {
        this.maskPayload = maskPayload;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {

        byte[] mask;

        ByteBuf data = msg.content();
        if (data == null) {
            data = Unpooled.EMPTY_BUFFER;
        }

        byte opcode;
        if (msg instanceof TextWebSocketFrame) {
            opcode = OPCODE_TEXT;
        } else if (msg instanceof PingWebSocketFrame) {
            opcode = OPCODE_PING;
        } else if (msg instanceof PongWebSocketFrame) {
            opcode = OPCODE_PONG;
        } else if (msg instanceof CloseWebSocketFrame) {
            opcode = OPCODE_CLOSE;
        } else if (msg instanceof BinaryWebSocketFrame) {
            opcode = OPCODE_BINARY;
        } else if (msg instanceof ContinuationWebSocketFrame) {
            opcode = OPCODE_CONT;
        } else {
            throw new UnsupportedOperationException("Cannot encode frame of type: " + msg.getClass().getName());
        }

        int length = data.readableBytes();

        if (logger.isDebugEnabled()) {
            logger.debug("Encoding WebSocket Frame opCode=" + opcode + " length=" + length);
        }

        int b0 = 0;
        if (msg.isFinalFragment()) {
            b0 |= 1 << 7;
        }
        b0 |= msg.rsv() % 8 << 4;
        b0 |= opcode % 128;

        if (opcode == OPCODE_PING && length > 125) {
            throw new TooLongFrameException("invalid payload for PING (payload length must be <= 125, was "
                    + length);
        }

        boolean release = true;
        ByteBuf buf = null;
        try {
            int maskLength = maskPayload ? 4 : 0;
            if (length <= 125) {
                int size = 2 + maskLength;
                if (maskPayload) {
                    size += length;
                }
                buf = ctx.alloc().buffer(size);
                buf.writeByte(b0);
                byte b = (byte) (maskPayload ? 0x80 | (byte) length : (byte) length);
                buf.writeByte(b);
            } else if (length <= 0xFFFF) {
                int size = 4 + maskLength;
                if (maskPayload) {
                    size += length;
                }
                buf = ctx.alloc().buffer(size);
                buf.writeByte(b0);
                buf.writeByte(maskPayload ? 0xFE : 126);
                buf.writeByte(length >>> 8 & 0xFF);
                buf.writeByte(length & 0xFF);
            } else {
                int size = 10 + maskLength;
                if (maskPayload) {
                    size += length;
                }
                buf = ctx.alloc().buffer(size);
                buf.writeByte(b0);
                buf.writeByte(maskPayload ? 0xFF : 127);
                buf.writeLong(length);
            }

            // Write payload
            if (maskPayload) {
                int random = (int) (Math.random() * Integer.MAX_VALUE);
                mask = ByteBuffer.allocate(4).putInt(random).array();
                buf.writeBytes(mask);

                int counter = 0;
                for (int i = data.readerIndex(); i < data.writerIndex(); i ++) {
                    byte byteData = data.getByte(i);
                    buf.writeByte(byteData ^ mask[counter++ % 4]);
                }
                out.add(buf);
            } else {
                if (buf.writableBytes() >= data.readableBytes()) {
                    // merge buffers as this is cheaper then a gathering write if the payload is small enough
                    buf.writeBytes(data);
                    out.add(buf);
                } else {
                    out.add(buf);
                    out.add(data.retain());
                }
            }
            release = false;
        } finally {
            if (release && buf != null) {
                buf.release();
            }
        }
    }
}
