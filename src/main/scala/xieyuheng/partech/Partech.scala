package xieyuheng.partech

trait Partech {

  def parse_tokens_by_rule(
    tokens: List[Token],
    rule: Rule,
    debug_p: Boolean,
  ): Tree

}
