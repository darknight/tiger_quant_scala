package com.tquant.core.engine

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import cats.effect.unsafe.implicits.global
import com.tquant.core.event.{EventEngine, EventType}
import org.specs2.mutable.Specification
import com.tquant.core.event.Event
import com.tquant.core.model.data.Tick

class OrderEngineSpec  extends Specification with CatsEffect {
  "OrderEngine" should {
    "get tick by symbol correctly" in IO {
      val eventEngine = EventEngine(10).unsafeRunSync()
      val orderEngine = OrderEngine(eventEngine).unsafeRunSync()

      val res = orderEngine.getTick("TEST").value.unsafeRunSync()

      res should beNone
    }

    "get tick after process tick event" in IO {
      val eventEngine = EventEngine(10).unsafeRunSync()
      val orderEngine = OrderEngine(eventEngine).unsafeRunSync()
      orderEngine.start().unsafeRunSync()

      val tick = Tick.empty.copy(symbol = "TEST")
      eventEngine.process(Event(EventType.EVENT_TICK, Some(tick))).unsafeRunSync()
      val res = orderEngine.getTick("TEST").value.unsafeRunSync()

      res should beSome(tick)
    }
  }
}
