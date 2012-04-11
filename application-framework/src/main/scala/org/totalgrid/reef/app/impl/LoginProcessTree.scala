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
package org.totalgrid.reef.app.impl

import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.app.process._
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.app.ConnectedApplication
import net.agileautomata.executor4s._

/**
 * Handles the multi stage application login process.
 * Is automatically running as soon as it is constructed, can only be stopped.
 *
 * If heartbeats or application registration fail we go back to trying to login
 * again.
 */
class LoginProcessTree(connection: Connection,
  connectedApp: ConnectedApplication,
  managerSettings: ApplicationManagerSettings,
  executor: Executor)
    extends ErrorHandler {

  private val appSettings = connectedApp.getApplicationSettings
  private val instanceName = managerSettings.nodeSettings.getDefaultNodeName + "-" + appSettings.instanceName
  private val parentProcess: Process = new LoginTask()
  private val processManager = new SimpleProcessManager(executor)

  processManager.addProcess(parentProcess)
  processManager.addErrorHandler(this)
  processManager.start()

  def stop() = processManager.stop()

  // proxy the login errors to the ConnectedApplication
  def onError(msg: String, ex: Option[Exception]) = {
    connectedApp.onConnectionError(msg + ex.map { " : " + _.getMessage }.getOrElse(""))
  }

  class LoginTask extends RetryableProcess("Attempting to login user: " + managerSettings.userSettings.getUserName) {

    override def setupRetryDelay = managerSettings.retryLoginInitialDelayMs
    override def setupRetryDelayMax = managerSettings.retryLoginMaxDelayMs

    private var client = Option.empty[Client]
    private var childTask = Option.empty[Process]

    def setup(p: ProcessManager) {

      client = Some(connection.login(managerSettings.userSettings).await)

      val services = client.get.getRpcInterface(classOf[AllScadaService])
      childTask = Some(new AppRegistrationTask(client.get, services))
      p.addChildProcess(this, childTask.get)
    }

    def cleanup(p: ProcessManager) {
      childTask.foreach { p.removeProcess(_) }
      client.foreach { _.logout().await }
    }
  }

  class AppRegistrationTask(client: Client, services: AllScadaService)
      extends OneShotProcess("Registering application: " + instanceName) {

    var appConfig = Option.empty[ApplicationConfig]

    def setup(p: ProcessManager) {

      appConfig = Some(services.registerApplication(managerSettings.nodeSettings, instanceName, appSettings.capabilites.toList).await)

      // send a single heartbeat just to verify we are correctly registered
      services.sendHeartbeat(appConfig.get).await

      // we need to give the application a new client
      val appClient = client.spawn()
      connectedApp.onApplicationStartup(appConfig.get, connection, appClient)

      p.addChildProcess(this, new HeartbeatTask(services, appConfig.get))
    }

    def cleanup(p: ProcessManager) {

      connectedApp.onApplicationShutdown()

      appConfig.foreach { services.sendApplicationOffline(_).await }

      appConfig = None
    }
  }

  class HeartbeatTask(services: AllScadaService, appConfig: ApplicationConfig)
      extends OneShotProcess("Starting beartbeats for: " + instanceName) {

    var timer = Option.empty[Timer]

    def setup(p: ProcessManager) {

      val period: Long = managerSettings.overrideHeartbeatPeriodMs.getOrElse(appConfig.getHeartbeatCfg.getPeriodMs)
      timer = Some(executor.scheduleWithFixedOffset(period.milliseconds, period.milliseconds) {
        try {
          services.sendHeartbeat(appConfig).await
        } catch {
          case rse: Exception =>
            // need to shunt notification out of timer call, otherwise timer.cancel during cleanup will deadlock
            executor.execute {
              p.reportError(this, "Error hearbeating " + rse.getMessage, Some(rse))
              p.failProcess(this)
            }
        }
      })
    }

    def cleanup(p: ProcessManager) {
      timer.foreach { _.cancel() }
    }
  }
}