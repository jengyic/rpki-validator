/**
 * The BSD License
 *
 * Copyright (c) 2010-2012 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator.authentication

import org.scalatra.ScalatraBase
import org.scalatra.auth.strategy.{BasicAuthSupport, BasicAuthStrategy}
import org.scalatra.auth.{ScentryConfig, ScentrySupport}


object AdminLoginStrategy {
  val ADMIN_PASSWORD_ENVIRONMENT_VAR = "RIPE_NCC_RPKI_VALIDATOR_ADMIN_PASSWORD"
}

/**
 * Basic authentication for "admin" users.
 *
 * Expects username "admin", and password to match the admin env variable
 */
class AdminLoginStrategy (protected override val app: ScalatraBase, realm: String) extends BasicAuthStrategy[User](app, realm) {

  protected def getUserId(user: User): String = user.id

  protected def validate(userName: String, password: String): Option[User] = {
    sys.env.get(AdminLoginStrategy.ADMIN_PASSWORD_ENVIRONMENT_VAR) match {
      case Some(adminPassword) => {
        if (userName == "admin" && password == adminPassword) {
          Some(User(userName))
        } else {
          None
        }
      }
      case None => None
    }
  }
}

/**
 * See: http://www.scalatra.org/guides/http/authentication.html
 */
trait AuthenticationSupport extends ScentrySupport[User] with BasicAuthSupport[User] {
  self: ScalatraBase =>

  val realm = "RPKI Validator KIOSK mode, see README.txt for details"

  protected def fromSession = { case id: String => User(id)  }
  protected def toSession   = { case usr: User => usr.id }

  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

  override protected def registerAuthStrategies = {
    scentry.register("Basic", app => new AdminLoginStrategy(app, realm))
  }

  /**
   * Prompts for basic authentication if the admin password env variable has been set.
   */
  def authenticatedAction(action: => Any) = {
    sys.env.get(AdminLoginStrategy.ADMIN_PASSWORD_ENVIRONMENT_VAR) match {
      case Some(password) => {
        basicAuth()
        action
      }
      case None => action
    }
  }
}

case class User(id: String)