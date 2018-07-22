package ideal.sylph.runner.spark.etl.sparkstreaming

import ideal.sylph.api.etl.Sink
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges}
import org.slf4j.LoggerFactory

/** *
  * spark 老流 kafka优化
  * */
object DStreamUtil {
  private val logger = LoggerFactory.getLogger(DStreamUtil.getClass)

  def getFristDStream(stream: DStream[_]): DStream[_] = if (stream.dependencies.isEmpty) stream
  else getFristDStream(stream.dependencies.head)

  def getFristRdd(rdd: RDD[_]): RDD[_] = if (rdd.dependencies.isEmpty) rdd
  else getFristRdd(rdd.dependencies.head.rdd)

  def DstreamParser(stream: DStream[Row], sink: Sink[RDD[Row]]): Unit = {
    val fristDStream = getFristDStream(stream.map(x => x))
    logger.info("数据源驱动:{}", fristDStream.getClass.getName)

    if ("DirectKafkaInputDStream".equals(fristDStream.getClass.getSimpleName)) {
      logger.info("发现job 数据源是kafka,将开启空job优化 且 自动上报offect")
      stream.foreachRDD(rdd => {
        val kafkaRdd = getFristRdd(rdd) //rdd.dependencies(0).rdd
        val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges
        if (kafkaRdd.count() > 0) {
          sink.run(rdd) //执行业务操作
        }
        fristDStream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
      })
    } else { //非kafka数据源 暂时无法做任何优化
      stream.foreachRDD(rdd => sink.run(rdd))
    }
  }
}
