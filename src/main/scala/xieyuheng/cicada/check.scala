package xieyuheng.cicada

import collection.immutable.ListMap
import scala.util.{ Try, Success, Failure }

import eval._
import infer._
import subtype._
import readback._
import pretty._
import equivalent._

object check {

  def check(env: Env, ctx: Ctx, exp: Exp, t: Value): Unit = {
    try {

      t match {
        case ValueUnion(type_list: List[Value]) =>
          type_list.find {
            case (t) =>
              Try {
                check(env, ctx, exp, t)
              } match {
                case Success(()) => true
                case Failure(_error) => false
              }
          } match {
            case Some(_t) => ()
            case None =>
              throw Report(List(
                s"fail on union type\n"
              ))
          }

        case t =>
          exp match {
            case Var(name: String) =>
              env.lookup_value(name) match {
                case Some(value) =>
                  check(env, ctx, readback(value), t)
                case None =>
                  subtype(ctx, infer(env, ctx, exp), t)
              }

            case Obj(value_map: ListMap[String, Exp]) =>
              t match {
                case cl: ValueCl =>
                  val v_map = value_map.map {
                    case (name, exp) => (name, eval(env, exp))
                  }
                  defined_check(env, ctx, v_map, cl.defined)
                  telescope_check(ctx, v_map, cl.telescope)

                case _ =>
                  throw Report(List(
                    s"expecting class type\n" +
                      s"but found: ${pretty_value(t)}\n"
                  ))
              }

            case Fn(type_map: ListMap[String, Exp], body: Exp) =>
              t match {
                case pi: ValuePi =>
                  val (t_map, return_type_value) =
                    util.telescope_force_with_return(
                      pi.telescope,
                      pi.telescope.name_list,
                      pi.return_type)
                  var local_ctx = ctx
                  type_map.zipWithIndex.foreach {
                    case ((name, exp), index) =>
                      check(env, local_ctx, exp, ValueType())
                      val s = eval(env, exp)
                      val (_name, t) = t_map.toList(index)
                      subtype(local_ctx, s, t)
                      local_ctx = local_ctx.ext(name, s)
                  }
                  check(env, local_ctx, body, return_type_value)

                case _ =>
                  throw Report(List(
                    s"expecting pi type\n" +
                      s"but found: ${pretty_value(t)}\n"
                  ))
              }

            case Switch(name: String, cases: List[(Exp, Exp)]) =>
              ctx.lookup_type(name) match {
                case Some(r) =>
                  cases.foreach {
                    case (s, v) =>
                      val s_value = eval(env, s)
                      Try {
                        subtype(ctx, s_value, r)
                      } match {
                        case Success(()) =>
                          check(env, ctx.ext(name, s_value), v, t)
                        case Failure(error) =>
                          throw Report(List(
                            s"at compile time, we know type of ${name} is ${pretty_value(r)}\n" +
                              s"in a case of switch\n" +
                              s"the type ${pretty_value(s_value)} is not a subtype of the abvoe type\n" +
                              s"it is meaningless to write this case\n" +
                              s"because we know it will never be matched\n"
                          ))
                      }
                  }

                case None =>
                  cases.foreach {
                    case (s, v) =>
                      check(env, ctx.ext(name, eval(env, s)), v, t)
                  }
              }

            case _ =>
              subtype(ctx, infer(env, ctx, exp), t)
          }
      }
    } catch {
      case report: Report =>
        report.throw_prepend(
          s"check fail\n" +
            s"exp: ${pretty_exp(exp)}\n" +
            s"t: ${pretty_value(t)}\n"
        )
    }
  }

  def telescope_check(
    ctx: Ctx,
    value_map: ListMap[String, Value],
    telescope: Telescope,
  ): Unit = {
    var local_env = telescope.env
    var local_ctx = ctx
    telescope.type_map.foreach {
      case (name, t_exp) =>
        val t_value = eval(local_env, t_exp)
        val v_value = value_map.get(name) match {
          case Some(v_value) => v_value
          case None =>
            throw Report(List(
              s"telescope_check fail\n" +
                s"can not find a field of class in object\n" +
                s"field: ${name}\n"
            ))
        }
        val v_exp = readback(v_value)
        check(local_env, local_ctx, v_exp, t_value)
        local_env = local_env.ext(name, v_value)
        local_ctx = local_ctx.ext(name, t_value)
    }
  }

  def defined_check(
    env: Env,
    ctx: Ctx,
    value_map: ListMap[String, Value],
    defined: ListMap[String, (Value, Value)],
  ): Unit = {
    defined.foreach {
      case (name, (t_value, defined_value)) =>
        val v_value = value_map.get(name) match {
          case Some(v_value) => v_value
          case None =>
            throw Report(List(
              s"define_check fail\n" +
                s"can not find a field of class in object\n" +
                s"field: ${name}\n"
            ))
        }
        val v_exp = readback(v_value)
        check(env, ctx, v_exp, t_value)
        equivalent(ctx, v_value, defined_value)
    }
  }

}
