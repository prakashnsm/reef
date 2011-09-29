package org.totalgrid.reef.messaging.synchronous

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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.broker.api.BrokerConnection
import org.mockito.Mockito._
import org.jmock.lib.concurrent.DeterministicScheduler
import net.agileautomata.executor4s._
import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class BrokerConnectionManagerTest extends FunSuite with ShouldMatchers {

  def fixture(initial: Int, max: Long)(test: (BrokerConnection, DeterministicScheduler, BrokerConnectionManager) => Unit): Unit = {
    val broker = mock(classOf[BrokerConnection])
    val scheduler = new DeterministicScheduler()
    val strand = Strand(Executors.newCustomExecutor(scheduler))
    val manager = new BrokerConnectionManager(broker, initial, max, strand)
    verify(broker).addListener(manager) // mananger adds itself as a listener on construction
    test(broker, scheduler, manager)
  }

  def fixture(test: (BrokerConnection, DeterministicScheduler, BrokerConnectionManager) => Unit): Unit = fixture(1000, 60000)(test)

  test("No actions until started") {
    fixture { (broker, exe, manager) =>
      exe.isIdle should equal(true)
      verifyNoMoreInteractions(broker)
    }
  }

  test("Starts causes connection execution") {
    fixture { (broker, exe, manager) =>
      manager.start()
      when(broker.connect()).thenReturn(true)
      exe.runNextPendingCommand()
      verify(broker).connect()
      exe.isIdle should be(true)
    }
  }

  test("Connection failure causes exponential backoff up to maximum") {
    fixture(2000, 5000) { (broker, exe, manager) =>
      manager.start()
      when(broker.connect()).thenReturn(false)
      exe.runNextPendingCommand()
      verify(broker).connect()
      reset(broker)
      exe.tick(1000, TimeUnit.MILLISECONDS)
      verifyNoMoreInteractions(broker)
      exe.tick(1000, TimeUnit.MILLISECONDS)
      verify(broker).connect()
      reset(broker)
      exe.tick(4000, TimeUnit.MILLISECONDS)
      verify(broker).connect()
      reset(broker)
      exe.tick(5000, TimeUnit.MILLISECONDS)
      verify(broker).connect()
    }
  }

  test("Connection failure causes reconnection after delay") {
    fixture(2000, 5000) { (broker, exe, manager) =>
      manager.start()
      when(broker.connect()).thenReturn(true)
      exe.runNextPendingCommand()
      reset(broker)
      manager.onConnectionClosed(false)
      verifyNoMoreInteractions(broker)
      exe.isIdle should be(true)
      exe.tick(2000, TimeUnit.MILLISECONDS)
      verify(broker).connect()
    }
  }

}