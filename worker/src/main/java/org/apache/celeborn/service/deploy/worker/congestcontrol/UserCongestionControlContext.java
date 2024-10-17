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

package org.apache.celeborn.service.deploy.worker.congestcontrol;

import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.metrics.source.AbstractSource;
import org.apache.celeborn.service.deploy.worker.WorkerSource;

public class UserCongestionControlContext {

  private volatile boolean congestionControlFlag;

  private final UserBufferInfo userBufferInfo;

  private final BufferStatusHub workerBufferStatusHub;

  private final UserIdentifier userIdentifier;

  public UserCongestionControlContext(
      BufferStatusHub workerBufferStatusHub,
      UserBufferInfo userBufferInfo,
      AbstractSource workerSource,
      UserIdentifier userIdentifier) {
    this.congestionControlFlag = false;
    this.userBufferInfo = userBufferInfo;
    this.userIdentifier = userIdentifier;
    this.workerBufferStatusHub = workerBufferStatusHub;
    workerSource.addGauge(
        WorkerSource.USER_PRODUCE_SPEED(),
        userIdentifier.toJMap(),
        () -> userBufferInfo.getBufferStatusHub().avgBytesPerSec());
  }

  public void onCongestionControl() {
    congestionControlFlag = true;
  }

  public void offCongestionControl() {
    congestionControlFlag = false;
  }

  public boolean inCongestionControl() {
    return congestionControlFlag;
  }

  public void updateProduceBytes(long numBytes) {
    long timeNow = System.currentTimeMillis();
    BufferStatusHub.BufferStatusNode node = new BufferStatusHub.BufferStatusNode(numBytes);
    userBufferInfo.updateInfo(timeNow, node);
    workerBufferStatusHub.add(timeNow, node);
  }

  public UserBufferInfo getUserBufferInfo() {
    return userBufferInfo;
  }

  public UserIdentifier getUserIdentifier() {
    return userIdentifier;
  }
}