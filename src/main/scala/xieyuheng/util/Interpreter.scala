package xieyuheng.util

import collection.immutable.ListMap

// NOTE
// comand line option start with `-` (instead of `--`)
//   will be catched by `JAVA_OPTS`
//   thus our `Interpreter` only use `--` for option prefix

trait Interpreter {

  val name: String
  val version: String
  val config_declaration: ListMap[String, Int]
  def run_code(code: String): Unit

  var config: ListMap[String, List[String]] = ListMap()

  def print_help(): Unit = {
    val usage = s"""
        |${name} ${version}
        |
        |usage:
        |  --eval <file_path> [default]
        |  --version
        |  --help
        """.stripMargin
    println(usage)
  }

  def main(args: Array[String]): Unit = {

    config_declaration.foreach {
      case (name, arity) =>
        opt(args, name, arity).foreach {
          case values =>
            config = config + (name -> values)
        }
    }

    opt(args, "--help", 0).foreach {
      case _ =>
        print_help()
        System.exit(0)
    }

    opt(args, "--version", 0).foreach {
      case _ =>
        println(version)
        System.exit(0)
    }

    opt(args, "--eval", 1).foreach {
      case List(file_path) =>
        run_file(file_path)
        System.exit(0)
    }

    if (args.length == 1) {
      val file_path = args(0)
      run_file(file_path)
      System.exit(0)
    }

    print_help()
    System.exit(0)
  }

  def opt(args: Array[String], name: String, arity: Int): Option[List[String]] = {
    val i = args.indexOf(name)
    if (i == -1) {
      None
    } else {
      Some(args.slice(i + 1, i + 1 + arity).toList)
    }
  }

  def run_file(file_path: String): Unit = {
    val pwd = System.getProperty("user.dir")
    val source = io.Source.fromFile(pwd + "/"+ file_path)
    try {
      val code = source.mkString
      run_code(code)
    } finally {
      source.close()
    }
  }

}
