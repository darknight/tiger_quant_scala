package com.tquant.core.event

import cats.effect.{IO, Ref}
import cats.effect.std.Queue
import cats.effect.testing.specs2.CatsEffect
import cats.effect.unsafe.implicits.global
import org.specs2.mutable.Specification

class EventEngineSpec extends Specification with CatsEffect {
  "EventEngine" should {
    "put event in the queue successfully" in IO {
      val capacity = 10
      val queue = Queue.bounded[IO, Event](capacity).unsafeRunSync()
      val handlerMapRef = Ref.of[IO, Map[EventType, List[EventHandler]]](Map.empty).unsafeRunSync()
      val engine = new EventEngine(capacity, queue, handlerMapRef)
      val event = Event(EventType.EVENT_TIMER, None)

      engine.put(event).unsafeRunSync()

      queue.size.unsafeRunSync() shouldEqual 1
    }

    "put event in the queue successfully" in IO {
      val capacity = 10
      val queue = Queue.bounded[IO, Event](capacity).unsafeRunSync()
      val handlerMapRef = Ref.of[IO, Map[EventType, List[EventHandler]]](Map.empty).unsafeRunSync()
      val engine = new EventEngine(capacity, queue, handlerMapRef)

      engine.registerHandler(EventType.EVENT_TIMER, (_: Event) => IO.unit).unsafeRunSync()
      engine.registerHandler(EventType.EVENT_TIMER, (_: Event) => IO.unit).unsafeRunSync()

      handlerMapRef.get.unsafeRunSync().size shouldEqual 1
    }
  }
}
