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

package org.totalgrid.reef.loader.helpers

import org.totalgrid.reef.proto.Alarms.EventConfig
import org.totalgrid.reef.proto.Model.{ ConfigFile, Point, Command, EntityAttributes, EntityEdge, Entity }
import org.totalgrid.reef.proto.FEP.{ CommChannel, CommEndpointConfig }
import collection.mutable.{ ArrayBuffer }
import com.google.protobuf.GeneratedMessage
import collection.Iterator
import collection.mutable
import java.lang.IllegalStateException
import org.totalgrid.reef.proto.Model
import org.totalgrid.reef.proto.Processing.TriggerSet
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.loader.LoadingException

class ModelContainer extends Logging {
  private val models = new ArrayBuffer[GeneratedMessage]
  private val triggerSets = new ArrayBuffer[TriggerSet]
  // these must be unique
  private val entities = mutable.Map[String, Entity]()
  private val configFiles = mutable.Map[String, ConfigFile]()

  def getModels: Iterator[GeneratedMessage] =
    {
      models.iterator
    }

  def add(entity: Entity): Entity =
    {
      logger.debug("adding entity: " + entity)

      if (!entities.contains(entity.getName)) {
        entities.put(entity.getName, entity)
        addModel(entity)
        return entity
      }

      throw new IllegalStateException("duplicate entity name found: " + entity.getName)
    }

  def add(edge: EntityEdge): EntityEdge =
    {
      val parent: Entity = edge.getParent
      failIfEntityNotFound(parent)
      val child: Entity = edge.getChild
      // TODO apparently child may not exist yet, if the child added implicitly??
      // failIfEntityNotFound(child)
      addModel(edge)
      edge
    }

  def add(entityAttributes: EntityAttributes): EntityAttributes =
    {
      addModel(entityAttributes)
      entityAttributes
    }

  def add(command: Command): Command =
    {
      addModel(command)
      command
    }

  def add(point: Point): Point =
    {
      addModel(point)
      point
    }

  def add(eventConfig: EventConfig) {
    addModel(eventConfig)
    eventConfig
  }

  def add(endpointConfig: CommEndpointConfig): CommEndpointConfig =
    {
      addModel(endpointConfig)
      endpointConfig
    }

  def add(configFile: ConfigFile): ConfigFile =
    {
      logger.debug("adding config file to container: " + configFile.getName)
      val currentConfigFile: Option[ConfigFile] = configFiles.get(configFile.getName)
      if (!currentConfigFile.isEmpty) {
        throw new LoadingException("duplicate configFile name found: " + configFile.getName)
      } else {
        configFiles.put(configFile.getName, configFile)
        addModel(configFile)
        logger.debug("new config file: " + configFile.getName)
        configFile
      }
    }

  def add(commChannel: CommChannel): CommChannel =
    {
      addModel(commChannel)
      commChannel
    }

  def add(triggerSet: TriggerSet): TriggerSet =
    {
      addTriggerSet(triggerSet);
      triggerSet
    }

  def getEntities() =
    {
      entities.clone()
    }

  def getEntity(name: String): Option[Entity] =
    {
      entities.get(name)
    }

  def getTriggerSets() =
    {
      triggerSets.clone()
    }

  def getConfigFiles() =
    {
      configFiles.clone()
    }

  def getConfigFile(name: String): Option[ConfigFile] =
    {
      configFiles.get(name)
    }

  def reset() {
    models.clear()
    triggerSets.clear()
    entities.clear()
    configFiles.clear()
  }

  private def addModel(proto: GeneratedMessage): GeneratedMessage =
    {
      models.append(proto)
      proto
    }

  private def addTriggerSet(proto: TriggerSet): GeneratedMessage =
    {
      triggerSets.append(proto)
      proto
    }

  private def failIfEntityNotFound(entity: Model.Entity) {
    if (!entities.contains(entity.getName)) {
      throw new LoadingException("entity doesn't exist: " + entity)
    }
  }

}