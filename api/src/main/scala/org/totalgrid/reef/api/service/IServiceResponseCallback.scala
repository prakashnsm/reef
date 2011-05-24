package org.totalgrid.reef.api.service

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.totalgrid.reef.japi.Envelope

/**
 * Defines how to complete a service call with a ServiceResponse
 */
trait IServiceResponseCallback {
  def onResponse(rsp: Envelope.ServiceResponse)
}

class CallbackTimer(callback: IServiceResponseCallback, timerFun: (Long, Envelope.ServiceResponse) => Unit) extends IServiceResponseCallback {

  val start = System.currentTimeMillis

  def onResponse(rsp: Envelope.ServiceResponse) {
    timerFun(System.currentTimeMillis - start, rsp)
    callback.onResponse(rsp)
  }

}
