/*
 * Copyright 2014–2017 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar

import slamdata.Predef._
import quasar.fp.ski.κ
import quasar.fs.mount.MountConfig.ModuleConfig
import quasar.sql._

import pathy.Path._
import scalaz._, Scalaz._

class ResolveImportSpec extends quasar.Qspec {
  "Import resolution" >> {
    "simple case" >> {
      val blob = sqlB"import `/mymodule/`; TRIVIAL(`/foo`)"
      val trivial = FunctionDecl(CIName("Trivial"), List(CIName("from")), sqlE"select * FROM :from")
      val module = ModuleConfig(List(trivial))
      resolveImportsImpl[Id](blob, rootDir, κ(module)).run must_=== Block(sqlE"select * from `/foo`", Nil).right
    }
  }
}