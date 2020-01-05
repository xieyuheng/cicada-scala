package xieyuheng.cicada_backup

import collection.immutable.ListMap

object readback {

  def readback(value: Value): Exp = {
    value match {
      case ValueType() =>
        Type()

      case ValueStrType() =>
        StrType()

      case ValueStr(str: String) =>
        Str(str: String)

      case ValuePi(telescope: Telescope, return_type: Exp) =>
        val name_list = telescope.name_list
        val (type_value_map, return_type_value) =
          util.telescope_force_with_return(telescope, name_list, return_type)
        Pi(
          type_value_map.map { case (name, v) => (name, readback(v)) },
          readback(return_type_value))

      case ValueFn(telescope: Telescope, body: Exp) =>
        val name_list = telescope.name_list
        val (type_value_map, body_value) =
          util.telescope_force_with_return(telescope, name_list, body)
        Fn(
          type_value_map.map { case (name, v) => (name, readback(v)) },
          readback(body_value))

      case ValueFnCase(cases) =>
        FnCase(cases.map {
          case (telescope, body) =>
            val name_list = telescope.name_list
            val (type_value_map, body_value) =
              util.telescope_force_with_return(telescope, name_list, body)
            val type_map = type_value_map.map { case (name, v) => (name, readback(v)) }
            (type_map, readback(body_value))
        })

      case ValueCl(defined, telescope: Telescope) =>
        val name_list = telescope.name_list
        Cl(
          defined.map { case (name, (t, v)) => (name, (readback(t), readback(v))) },
          util.telescope_force(telescope, name_list)
            .map { case (name, v) => (name, readback(v)) })

      case ValueClInferedFromObj(type_map: ListMap[String, Value]) =>
        Cl(
          ListMap(),
          type_map.map { case (name, v) => (name, readback(v)) })

      case ValueObj(value_map: ListMap[String, Value]) =>
        Obj(value_map.map { case (name, v) => (name, readback(v)) })

      case ValueUnion(type_list: List[Value]) =>
        Union(type_list.map { readback })

      case ValueThe(t: Value, value: Value) =>
        The(readback(t), readback(value))

      case NeutralVar(name: String) =>
        Var(name)

      case NeutralAp(target: Neutral, arg_list: List[Value]) =>
        Ap(readback(target), arg_list.map(readback))

      case NeutralDot(target: Neutral, field: String) =>
        Dot(readback(target), field)

      case NeutralThe(t: Value, value: Neutral) =>
        The(readback(t), readback(value))
    }
  }

}