/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.circuitbreaker

//#imports1
import scala.concurrent.duration._
import akka.pattern.CircuitBreaker
import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.concurrent.Future

//#imports1

class CircuitBreakerDocSpec {}

//#circuit-breaker-initialization
class DangerousActor extends Actor with ActorLogging {
  import context.dispatcher

  val breaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = 5,
      callTimeout = 10.seconds,
      resetTimeout = 1.minute).onOpen(notifyMeOnOpen())

  def notifyMeOnOpen(): Unit =
    log.warning("My CircuitBreaker is now open, and will not close for one minute")
  //#circuit-breaker-initialization

  //#circuit-breaker-usage
  def dangerousCall: String = "This really isn't that dangerous of a call after all"

  def receive = {
    case "is my middle name" =>
      breaker.withCircuitBreaker(Future(dangerousCall)) pipeTo sender()
    case "block for me" =>
      sender() ! breaker.withSyncCircuitBreaker(dangerousCall)
  }
  //#circuit-breaker-usage

}

class TellPatternActor(recipient : ActorRef) extends Actor with ActorLogging {
  import context.dispatcher

  val breaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = 5,
      callTimeout = 10.seconds,
      resetTimeout = 1.minute).onOpen(notifyMeOnOpen())

  def notifyMeOnOpen(): Unit =
    log.warning("My CircuitBreaker is now open, and will not close for one minute")

  //#circuit-breaker-tell-pattern
  import akka.actor.ReceiveTimeout

  def receive = {
    case "call" if breaker.isClosed => {
      recipient ! "message"
    }
    case "response" => {
      breaker.succeed()
    }
    case err: Throwable => {
      breaker.fail()
    }
    case ReceiveTimeout => {
      breaker.fail()
    }
  }
  //#circuit-breaker-tell-pattern
}