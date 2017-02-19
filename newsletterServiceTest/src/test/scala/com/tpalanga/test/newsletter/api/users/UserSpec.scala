package com.tpalanga.test.newsletter.api.users

import akka.http.scaladsl.model.StatusCodes
import com.tpalanga.test.newsletter.api.users.model.NewUser
import com.tpalanga.test.spec.RestSpec
import org.scalatest.{AsyncFlatSpec, Matchers}

object UserSpec {

  def newUUID(): String = java.util.UUID.randomUUID.toString
  def newUsername() = s"user-${newUUID()}"

  def createNewUser() = NewUser(newUsername())
}

class UserSpec extends AsyncFlatSpec with Matchers with RestSpec with DataserviceRestServiceClient {
  import UserSpec._

  "Dataservice" should "return 404 if a user does not exist" in {
    userRetrieve("unknown").map { reply =>
      reply.status shouldBe StatusCodes.NotFound
    }
  }

  it should "create user" in {
    val newUser = createNewUser()

    for {
      replyCreate <- userCreate(newUser)
      user <- replyCreate.entity
    } yield {
      replyCreate.status shouldBe StatusCodes.Created
      user.name shouldBe newUser.name
    }
  }

  it should "retrieve user" in {
    val newUser = createNewUser()

    for {
      replyCreate <- userCreate(newUser)
      user <- replyCreate.entity
      replyRetrieve <- userRetrieve(user.id)
      retrievedUser <- replyRetrieve.entity
    } yield {
      replyCreate.status shouldBe StatusCodes.Created
      user.name shouldBe newUser.name
      retrievedUser shouldBe user
    }
  }

  it should "update user" in {
    val newUser = createNewUser()

    for {
      replyCreate <- userCreate(newUser)
      user <- replyCreate.entity
      _ = replyCreate.status shouldBe StatusCodes.Created
      _ = user.name shouldBe newUser.name

      updatingUser = user.copy(name = "new name")
      replyUpdate <- userUpdate(updatingUser)
      _ = replyUpdate.status shouldBe StatusCodes.OK
      userUpdated <- replyUpdate.entity
      _ = userUpdated shouldBe updatingUser

      replyRetrieve <- userRetrieve(user.id)
      retrievedUser <- replyRetrieve.entity
    } yield {
      replyRetrieve.status shouldBe StatusCodes.OK
      retrievedUser shouldBe updatingUser
    }
  }

  it should "delete user" in {
    val newUser = createNewUser()

    for {
      replyCreate <- userCreate(newUser)
      user <- replyCreate.entity
      _ = replyCreate.status shouldBe StatusCodes.Created

      replyDelete <- userDelete(user.id)
      _ = replyDelete.status shouldBe StatusCodes.OK

      replyRetrieve <- userRetrieve(user.id)
    } yield {
      replyRetrieve.status shouldBe StatusCodes.NotFound
    }
  }

  it should "list users" in {
    val newUser = createNewUser()

    for {
      replyCreate <- userCreate(newUser)
      user <- replyCreate.entity
      _ = replyCreate.status shouldBe StatusCodes.Created
      _ = user.name shouldBe newUser.name
      replyList <- userList()
      userList <- replyList.entity
    } yield {
      user.name shouldBe newUser.name
      userList.users should contain(user)
    }
  }

}