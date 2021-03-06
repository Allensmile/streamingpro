package streaming.dsl

import streaming.dsl.mmlib.SQLAlg
import streaming.dsl.parser.DSLSQLParser._

/**
  * Created by allwefantasy on 12/1/2018.
  */
class TrainAdaptor(scriptSQLExecListener: ScriptSQLExecListener) extends DslAdaptor {
  override def parse(ctx: SqlContext): Unit = {
    var tableName = ""
    var format = ""
    var path = ""
    var options = Map[String, String]()
    (0 to ctx.getChildCount() - 1).foreach { tokenIndex =>
      ctx.getChild(tokenIndex) match {
        case s: TableNameContext =>
          tableName = s.getText
        case s: FormatContext =>
          format = s.getText
        case s: PathContext =>
          path = cleanStr(s.getText)
        case s: ExpressionContext =>
          options += (cleanStr(s.identifier().getText) -> cleanStr(s.STRING().getText))
        case s: BooleanExpressionContext =>
          options += (cleanStr(s.expression().identifier().getText) -> cleanStr(s.expression().STRING().getText))
        case _ =>
      }
    }
    val df = scriptSQLExecListener.sparkSession.table(tableName)
    val sqlAlg = AlgMapping.findAlg(format)
    sqlAlg.train(df, path, options)
  }
}

object AlgMapping {
  val mapping = Map[String, String](
    "Word2vec" -> "streaming.dsl.mmlib.algs.SQLWord2Vec",
    "NaiveBayes" -> "streaming.dsl.mmlib.algs.SQLNaiveBayes",
    "RandomForest" -> "streaming.dsl.mmlib.algs.SQLRandomForest"
  )

  def findAlg(name: String) = {
    mapping.get(name.capitalize) match {
      case Some(clzz) =>
        Class.forName(clzz).newInstance().asInstanceOf[SQLAlg]
      case None =>
        throw new RuntimeException(s"${name} is not found")
    }
  }
}
