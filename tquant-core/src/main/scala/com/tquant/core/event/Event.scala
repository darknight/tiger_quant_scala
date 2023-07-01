package com.tquant.core.event

// FIXME: add MonoidK constraint ?
trait EventData

case class Event(eventType: EventType, eventData: Option[EventData])
