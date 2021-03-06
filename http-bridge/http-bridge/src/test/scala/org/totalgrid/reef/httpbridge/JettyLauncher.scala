/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.httpbridge

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ ServletHolder, ServletContextHandler }
import org.totalgrid.reef.standalone.InMemoryNode
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.httpbridge.servlets._
import org.totalgrid.reef.httpbridge.servlets.apiproviders.AllScadaServiceApiCallLibrary
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.httpbridge.servlets.helpers._
import org.totalgrid.reef.client.settings.UserSettings

object JettyLauncher {
  def main(args: Array[String]) {

    // setup an in-memory (except for the database) reef node for easier testing
    val modelFile = Some("./assemblies/assembly-common/filtered-resources/samples/integration/config.xml")

    InMemoryNode.initialize("./standalone-node.cfg", false, modelFile)

    val properties = PropertyReader.readFromFile("./http-bridge/http-bridge/src/test/resources/org.totalgrid.reef.httpbridge.cfg")
    val defaultUser = DefaultUserConfiguration.getDefaultUser(properties)

    val defaultAuthToken = defaultUser.map { user =>
      InMemoryNode.connection.login(new UserSettings(user.getUserName, user.getUserPassword)).getHeaders.getAuthToken()
    }

    val managedConnection = new ManagedConnection {
      def getAuthenticatedClient(authToken: String) = InMemoryNode.connection.createClient(authToken)
      def getNewAuthToken(userName: String, userPassword: String) =
        InMemoryNode.connection.login(new UserSettings(userName, userPassword)).getHeaders.getAuthToken()

      def getSharedBridgeAuthToken() = defaultAuthToken
    }

    val builderLocator = new BuilderLocator(new ReefServices)

    val server = new Server(8886)

    val subscriptionHolder = new SimpleSubscriptionManager

    val context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY)
    context.setContextPath("/")
    context.addServlet(new ServletHolder(new RestLevelServlet(managedConnection, builderLocator)), "/rest/*")
    context.addServlet(new ServletHolder(new ConverterServlet(builderLocator)), "/convert/*")
    context.addServlet(new ServletHolder(new LoginServlet(managedConnection)), "/login/*")
    context.addServlet(new ServletHolder(new ApiServlet(managedConnection, new AllScadaServiceApiCallLibrary, subscriptionHolder)), "/api/*")
    context.addServlet(new ServletHolder(new SubscriptionServlet(subscriptionHolder)), "/subscribe/*")

    server.setHandler(context)

    server.start()
    server.join()
  }
}