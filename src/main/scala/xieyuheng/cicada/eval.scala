package xieyuheng.cicada

import collection.immutable.ListMap

import pretty._

object eval {

  def eval(env: Env, exp: Exp): Value = {
    exp match {
      case Var(name: String) =>
        env.lookup_val(name) match {
          case Some(value) => value
          case None => NeutralVar(name)
        }

      case Type() =>
        ValueType()

      case Pi(type_map: ListMap[String, Exp], return_type: Exp) =>
        ValuePi(Telescope(type_map: ListMap[String, Exp], env: Env), return_type: Exp)

      case Fn(type_map: ListMap[String, Exp], body: Exp) =>
        ValueFn(Telescope(type_map: ListMap[String, Exp], env: Env), body: Exp)

      case Ap(target: Exp, arg_list: List[Exp]) =>
        value_apply(eval(env, target), arg_list.map { eval(env, _) })

      case Cl(type_map: ListMap[String, Exp]) =>
        ValueCl(ListMap(), Telescope(type_map: ListMap[String, Exp], env: Env))

      case Obj(value_map: ListMap[String, Exp]) =>
        ValueObj(value_map.map {
          case (name, exp) => (name, eval(env, exp))
        })

      case Dot(target: Exp, field: String) =>
        value_dot(eval(env, target), field)

      case Block(block_entry_map: ListMap[String, BlockEntry], body: Exp) =>
        var local_env = env
        block_entry_map.foreach {
          case (name, BlockEntryLet(exp)) =>
            val value = eval(local_env, exp)
            local_env = local_env.ext(name, value)
          case (name, BlockEntryDefine(_t, exp)) =>
            val value = eval(local_env, exp)
            local_env = local_env.ext(name, value)
        }
        eval(local_env, body)
    }
  }

  def value_apply(value: Value, arg_list: List[Value]): Value = {
    value match {
      case neutral: Neutral =>
        NeutralAp(neutral, arg_list)

      case ValueFn(Telescope(type_map: ListMap[String, Exp], env: Env), body: Exp) =>
        val name_list = type_map.keys.toList
        if (name_list.length != type_map.size) {
          throw Report(List(
            "value_apply fail, ValueFn arity mismatch\n"
          ))
        } else {
          val map = Map(name_list.zip(arg_list): _*)
          eval(env.ext_map(map), body)
        }

      case ValueCl(_defined, Telescope(type_map: ListMap[String, Exp], env: Env)) =>
        val name_list = type_map.keys.toList
        if (name_list.length != type_map.size) {
          throw Report(List(
            "value_apply fail, ValueCl arity mismatch\n"
          ))
        } else {
          val value_map = ListMap(name_list.zip(arg_list): _*)
          ValueObj(value_map)
        }

      case _ =>
        throw Report(List(
          "value_apply fail, expecting ValueFn or ValueCl\n" +
            s"value: ${pretty_value(value)}\n"
        ))
    }
  }

  def value_dot(value: Value, field: String): Value = {
    value match {
      case neutral: Neutral =>
        NeutralDot(neutral, field)

      case ValueObj(value_map: ListMap[String, Value]) =>
        value_map.get(field) match {
          case Some(value) => value
          case None =>
            throw Report(List(
              s"missing field: ${field}\n"
            ))
        }

      case _ =>
        throw Report(List(
          "value_dot fail, expecting ValueObj\n"
        ))
    }
  }

}
