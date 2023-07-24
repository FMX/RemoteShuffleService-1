/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.network.protocol;

import static org.apache.celeborn.common.protocol.MessageType.OPEN_STREAM_VALUE;
import static org.apache.celeborn.common.protocol.MessageType.STREAM_HANDLER_VALUE;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.exception.CelebornIOException;
import org.apache.celeborn.common.protocol.MessageType;
import org.apache.celeborn.common.protocol.PbOpenStream;
import org.apache.celeborn.common.protocol.PbStreamHandler;

public class TransportMessage implements Serializable {
  private static final long serialVersionUID = -3259000920699629773L;
  private static Logger logger = LoggerFactory.getLogger(TransportMessage.class);
  @Deprecated private final MessageType type;
  private final int messageTypeValue;
  private final byte[] payload;

  public TransportMessage(MessageType type, byte[] payload) {
    this.type = type;
    this.messageTypeValue = type.getNumber();
    this.payload = payload;
  }

  public MessageType getType() {
    return type;
  }

  public int getMessageTypeValue() {
    return messageTypeValue;
  }

  public byte[] getPayload() {
    return payload;
  }

  public <T extends GeneratedMessageV3> T getPayLoad() throws InvalidProtocolBufferException {
    switch (messageTypeValue) {
      case OPEN_STREAM_VALUE:
        return (T) PbOpenStream.parseFrom(payload);
      case STREAM_HANDLER_VALUE:
        return (T) PbStreamHandler.parseFrom(payload);
      default:
        logger.error("Unexpected type {}", type);
    }
    return null;
  }

  public ByteBuffer toByteBuffer() {
    int totalBufferSize = payload.length + 4 + 4;
    ByteBuffer buffer = ByteBuffer.allocate(totalBufferSize);
    buffer.putInt(messageTypeValue);
    buffer.putInt(payload.length);
    buffer.put(payload);
    buffer.flip();
    return buffer;
  }

  public static TransportMessage fromByteBuffer(ByteBuffer buffer) throws CelebornIOException {
    int type = buffer.getInt();
    if (MessageType.forNumber(type) == null) {
      throw new CelebornIOException("Decode failed,fallback to legacy messages.");
    }
    int payloadLen = buffer.getInt();
    byte[] payload = new byte[payloadLen];
    buffer.get(payload);
    MessageType msgType = MessageType.forNumber(type);
    return new TransportMessage(msgType, payload);
  }
}
