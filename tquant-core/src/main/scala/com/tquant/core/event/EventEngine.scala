package com.tquant.core.event

import cats.effect.{Concurrent, IO, Ref, Spawn, Temporal}
import cats.effect.std.{AtomicCell, Queue}
import com.tquant.core.engine.Engine
import com.tquant.core.log.logging
import org.typelevel.log4cats.LoggerFactory

import scala.concurrent.duration._
import cats.implicits._
//import cats.effect.syntax.all._

class EventEngine(capacity: Int, queue: Queue[IO, Event],
                  handlerMapRef: Ref[IO, Map[EventType, List[EventHandler]]]) extends Engine {

  val engineName = "EventEngine"
  val logger = LoggerFactory[IO].getLogger

  // TODO: add & respect `engineActiveIO`
  def start(): IO[Unit] = {
    for {
      _ <- IO.defer(timer(30.seconds).start).void
      _ <- polling()
    } yield ()
  }

  def stop(): IO[Unit] = IO.unit

  def polling(): IO[Unit] = {
    dequeue() >> polling()
  }

  def dequeue(): IO[Unit] = {
    for {
      _ <- Temporal[IO].sleep(100.seconds) // FIXME
      _ <- logger.info("polling...")
      event <- queue.take
      _ <- logger.info(s"fetch event => $event")
      _ <- process(event)
      _ <- logger.info(s"process event, done...")
    } yield ()
  }

  // TODO: return from recursion
  def timer(duration: Duration): IO[Unit] = {
    Temporal[IO].sleep(duration) >> enqueue() >> timer(duration)
  }

  def enqueue(): IO[Unit] = {
    for {
      _ <- queue.offer(Event(EventType.EVENT_TIMER, None))
      size <- queue.size
      _ <- logger.info(s"send timer event, queue size => ($size, $capacity)")
    } yield ()
  }

  /**
   * Process event
   */
  def process(event: Event): IO[Unit] = {
    for {
      map <- handlerMapRef.get
      handlerList = map.getOrElse(event.eventType, List.empty)
      _ <- handlerList.map(_.processEvent(event)).sequence_
    } yield ()
  }

  /**
   * Add event to event queue
   *
   * @param event
   * @return
   */
  def put(event: Event): IO[Unit] = {
    for {
      _ <- queue.offer(event)
    } yield ()
  }

  def registerHandler(eventType: EventType, handler: EventHandler): IO[Unit] = {
    for {
      _ <- handlerMapRef.update(addHandlerIfAbsent(_, eventType, handler))
      map <- handlerMapRef.get
      _ <- logger.debug(s"added ($eventType, $handler) => map size = ${map.size}")
    } yield ()
  }

  def unregisterHandler(eventType: EventType, handler: EventHandler): IO[Unit] = {
    for {
      _ <- handlerMapRef.update(removeHandlerIfExit(_, eventType, handler))
    } yield ()
  }

  private def addHandlerIfAbsent(map: Map[EventType, List[EventHandler]],
                                 eventType: EventType,
                                 handler: EventHandler): Map[EventType, List[EventHandler]] = {
    val handlerList = map.getOrElse(eventType, List.empty)
    if (handlerList.contains(handler)) {
      map
    } else {
      map + (eventType -> (handler :: handlerList))
    }
  }

  private def removeHandlerIfExit(map: Map[EventType, List[EventHandler]],
                                  eventType: EventType,
                                  handler: EventHandler): Map[EventType, List[EventHandler]] = {
    val handlerList = map.getOrElse(eventType, List.empty)
    if (handlerList.contains(handler)) {
      map + (eventType -> handlerList.filterNot(_ == handler))
    } else {
      map
    }
  }
}

object EventEngine {
  def apply(capacity: Int): IO[EventEngine] = {
    for {
      queue <- Queue.bounded[IO, Event](capacity)
      handlerMapRef <- Ref.of[IO, Map[EventType, List[EventHandler]]](Map.empty)
      engine = new EventEngine(capacity, queue, handlerMapRef)
    } yield engine
  }
}
