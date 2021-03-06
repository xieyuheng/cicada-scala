package xieyuheng.partech

sealed trait Tree
final case class Leaf(token: Token) extends Tree
final case class Node(rule: Rule, choice_name: String, children: List[Tree]) extends Tree

object Tree {

  def matcher[A](
    name: String,
    choices: Map[String, List[Tree] => A],
  ): Tree => A = {
    case Node(rule, choice_name, children) =>
      if (rule.name == name) {
        choices.get(choice_name) match {
          case Some(f) => f(children)
          case None =>
            println(s"matcher on ${name}, does not have choice: ${choice_name}")
            throw new Exception()
        }
      } else {
        println(s"matcher expecting rule: ${name}, but found: ${rule.name}:${choice_name}")
        throw new Exception()
      }
    case Leaf(str) =>
      println(s"matcher can not match leaf: ${Leaf(str)}")
      throw new Exception()
  }

}
