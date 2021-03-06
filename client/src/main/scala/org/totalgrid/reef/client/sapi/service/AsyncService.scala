/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.service

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import java.util.{ List, Map }
import org.totalgrid.reef.client.registration.{ ServiceResponseCallback, Service }
import org.totalgrid.reef.client.proto.Envelope.ServiceResponse
import org.totalgrid.reef.client.RequestHeaders

/**
 * Defines how to complete a service call with a ServiceResponse
 */
trait AsyncService[A] extends ServiceDescriptor[A] with Service {
  def respond(req: Envelope.ServiceRequest, env: RequestHeaders, callback: ServiceResponseCallback): Unit

  def respond(request: Envelope.ServiceRequest, headers: Map[String, List[String]], callback: ServiceResponseCallback) {
    respond(request, BasicRequestHeaders.from(headers), new ServiceResponseCallback {
      def onResponse(rsp: ServiceResponse) { callback.onResponse(rsp) }
    })
  }
}

