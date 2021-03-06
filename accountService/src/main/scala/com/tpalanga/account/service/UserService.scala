package com.tpalanga.account.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.tpalanga.account.model.{NewUser, User, UserId}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.immutable.Seq

object UserService {

  sealed trait Request
  case object GetAll extends Request
  case class GetOne(id: UserId) extends Request
  case class AddOne(user: NewUser) extends Request
  case class Update(user: User) extends Request
  case class Delete(id: UserId) extends Request

  sealed trait GetUserResponse
  sealed trait AddUserResponse
  sealed trait UpdateUserResponse
  sealed trait DeleteUserResponse
  sealed trait GetAllUserResponse
  case class Deleted(id: UserId) extends DeleteUserResponse
  case class OneUser(user: User) extends GetUserResponse with AddUserResponse with UpdateUserResponse
  case class NotFound(id: UserId) extends GetUserResponse with UpdateUserResponse with DeleteUserResponse
  case object AlreadyExists extends AddUserResponse
  case class AllUsers(users: Seq[User]) extends GetAllUserResponse

  object DataFormats extends DefaultJsonProtocol {
    import User.DataFormats._
    implicit val newUserFormat: RootJsonFormat[NewUser] = jsonFormat2(NewUser)
    implicit val oneUserFormat: RootJsonFormat[OneUser] = jsonFormat1(OneUser)
    implicit val allUsersFormat: RootJsonFormat[AllUsers] = jsonFormat1(AllUsers)
  }

  def props(newsletterService: ActorRef) = Props(new UserService(newsletterService))

  def newUUID(): String = java.util.UUID.randomUUID.toString
}

class UserService(newsletterService: ActorRef) extends Actor with ActorLogging {
  import UserService._

  private var users: Map[UserId, User] = Map.empty

  override def receive: Receive = {
    case GetAll =>
      sender() ! AllUsers(users.values.to[Seq])

    case GetOne(id) =>
      sender() ! users.get(id).map(OneUser).getOrElse(NotFound(id))

    case AddOne(newUser) =>
      val reply = users.values.find(_.name == newUser.name)
        .map(_ => AlreadyExists)
        .getOrElse {
          val user = User.fromNewUser(newUUID(), newUser)
          users = users + (user.id -> user)
          newsletterService ! NewsletterService.Subscribe(user)
          OneUser(user)
        }
      sender() ! reply

    case Update(user) =>
      val reply = users.get(user.id).map { _ =>
        users = users + (user.id -> user)
        OneUser(user)
      }.getOrElse(NotFound(user.id))
      sender() ! reply

    case Delete(id) =>
      val reply = users.get(id).map { _ =>
        users = users - id
        newsletterService ! NewsletterService.Unsubscribe(id)
        Deleted(id)
      }.getOrElse(NotFound(id))
      sender() ! reply
  }
}
