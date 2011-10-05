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
package org.totalgrid.reef.japi.request.impl

/**
 * "Super" implementation of all of the service interfaces
 */
trait AllScadaServiceImpl
  extends AuthTokenServiceImpl
  with EntityServiceImpl
  with ConfigFileServiceImpl
  with MeasurementServiceImpl
  with MeasurementOverrideServiceImpl
  with EventServiceImpl
  with EventCreationServiceImpl
  with EventConfigServiceImpl
  with CommandServiceImpl
  with PointServiceImpl
  with AlarmServiceImpl
  with AgentServiceImpl
  with EndpointManagementServiceImpl
  with ApplicationServiceImpl

import org.totalgrid.reef.sapi.request.impl._

trait AllScadaServiceJavaShim
    extends AuthTokenServiceJavaShim
    with EntityServiceJavaShim
    with ConfigFileServiceJavaShim
    with MeasurementServiceJavaShim
    with MeasurementOverrideServiceJavaShim
    with EventServiceJavaShim
    with EventCreationServiceJavaShim
    with EventConfigServiceJavaShim
    with CommandServiceJavaShim
    with PointServiceJavaShim
    with AlarmServiceJavaShim
    with AgentServiceJavaShim
    with EndpointManagementServiceJavaShim
    with ApplicationServiceJavaShim {
  def service: AllScadaServiceImpl
}

