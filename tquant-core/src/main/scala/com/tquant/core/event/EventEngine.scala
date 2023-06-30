package com.tquant.core.event

import cats.effect.{IO, Spawn, Temporal}
import cats.effect.std.{AtomicCell, Queue}
import com.tquant.core.engine.Engine

import scala.concurrent.duration._
//import cats.implicits._
//import cats.effect._

class EventEngine(capacity: Int) extends Engine {

  val engineName = "EventEngine"

  private val queueIO = Queue.bounded[IO, Event](capacity)

  private val engineActiveIO = AtomicCell[IO].of(false)

  private val timerActiveIO = AtomicCell[IO].of(false)

  private val handlerMapIO = AtomicCell[IO].empty[Map[EventType, List[EventHandler]]]

  // TODO: respect `engineActiveIO`
  def start(): IO[Unit] = {
    def poll(): IO[Unit] =
      for {
        queue <- queueIO
        event <- queue.take
        _ <- process(event)
      } yield ()

    poll() >> start()
  }

  def stop(): IO[Unit] = IO.unit

  // TODO: return from recursion
  def timer(): IO[Unit] = {
    def enqueue(): IO[Unit] =
      Temporal[IO].sleep(10.seconds) >> queueIO.flatMap(_.offer(Event(EventType.EVENT_TIMER, None)))

    enqueue() >> timer()
  }

  /**
   * Process event
   */
  def process(event: Event): IO[Unit] = {
    for {
      handlerMap <- handlerMapIO
      map <- handlerMap.get
      handlerList = map.getOrElse(event.eventType, List.empty)
      _ = handlerList.map(_.processEvent(event))
    } yield ()
  }

  /**
   * Add event to event queue
   * @param event
   * @return
   */
  def put(event: Event): IO[Unit] = {
    for {
      queue <- queueIO
      _ <- queue.offer(event)
    } yield ()
  }

  def registerHandler(eventType: EventType, handler: EventHandler): IO[Unit] = {
    for {
      handlerMap <- handlerMapIO
      _ <- handlerMap.update(addHandlerIfAbsent(_, eventType, handler))
    } yield ()
  }

  def unregisterHandler(eventType: EventType, handler: EventHandler): IO[Unit] = {
    for {
      handlerMap <- handlerMapIO
      _ <- handlerMap.update(removeHandlerIfExit(_, eventType, handler))
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
  def apply(capacity: Int): EventEngine = {
    new EventEngine(capacity)
  }
}
