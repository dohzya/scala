import java.io.{ ByteArrayOutputStream, PrintStream }
import scala.reflect.mirror._
import scala.reflect.api._
import scala.reflect.api.Trees
import scala.reflect.internal.Types
import scala.util.matching.Regex

object Test extends App {
  val tree = tree_printf(reify("hello %s").tree, reify("world").tree)

  import scala.reflect.mirror._
  val toolbox = mkToolBox()

  val output = new ByteArrayOutputStream()
  Console.setOut(new PrintStream(output))
  val evaluated = toolbox.runExpr(tree)

  assert(output.toString() == "hello world", output.toString() +" ==     hello world")

  /*
  // upd. Oh, good old times, our very-very first experiments with macros :)
  macro def printf(format: String, params: Any*) : String = tree_printf(format: Tree, (params: Seq[Tree]): _*)
   */

  var i = 0
  def gensym(name: String) = { i += 1; newTermName(name + i) }

  def createTempValDef( value : Tree, tpe : Type ) : (Option[Tree],Tree) = {
    val local = gensym("temp")
    (
      Some(
        ValDef(
          Modifiers()
          , local
          , TypeTree().setType(tpe)
          , value
        )
      )
      , Ident(local)
    )
  }

  def tree_printf(format: Tree, params: Tree*) = {
    val Literal(Constant(s_format: String)) = format
    val paramsStack = scala.collection.mutable.Stack(params: _*)
    val parsed = s_format.split("(?<=%[\\w%])|(?=%[\\w%])") map {
      case "%d" => createTempValDef( paramsStack.pop, classToType(classOf[Int]) )
      case "%s" => createTempValDef( paramsStack.pop, classToType(classOf[String]) )
      case "%%" => {
        (None:Option[Tree], Literal(Constant("%")))
      }
      case part => {
        (None:Option[Tree], Literal(Constant(part)))
      }
    }

    val evals = for ((Some(eval), _) <- parsed if eval != None) yield (eval: Tree)
    val prints = for ((_, ref) <- parsed) yield
      Apply(
        Select(
          Select(
            Ident( newTermName("scala") )
            , newTermName("Predef")
          )
          , newTermName("print")
        )
        , List(ref)
      ): Tree
    Block((evals ++ prints).toList, Literal(Constant(())))
  }
}
