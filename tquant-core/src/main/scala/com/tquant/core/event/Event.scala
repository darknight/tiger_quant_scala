package com.tquant.core.event

trait EventData

case class Event(eventType: EventType, eventData: Option[EventData])
