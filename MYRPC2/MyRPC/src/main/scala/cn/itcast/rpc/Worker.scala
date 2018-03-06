//========================================f3
package cn.itcast.rpc

import java.util.UUID

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/**
  * Created by root on 2016/5/13.
  */
class Worker(val masterHost: String, val masterPort: Int, val memory: Int, val cores: Int) extends Actor{

  var master : ActorSelection = _
  val workerId = UUID.randomUUID().toString
  val HEART_INTERVAL = 10000

  //=========================================================4
  /*
  * preStart()方法是当 步骤2 创建了Worker类的Actor之后，在Worker的构造函数创建完成之后，preStart()方法会被进行执行，
  * 而且整个Worker过程中只执行一次
  * 这个函数中做的工作是跟Master进行链接，然后向Master发送信息
  * */
  override def preStart(): Unit = {
    //跟Master建立连接
    /*
    * s"akka.tcp://MasterSystem@$masterHost:$masterPort/user/Master") 是一个固定的格式。它是akka的PRC通信所使用的协议
    * 你在s"akka.tcp://MasterSystem@$ 和/user/Master"之间将你要传送的变量放进去，这些变量之间使用“:”进行分开。
    * 比如这个下面的这一行代码中，传送了两个变量。masterHost和masterPort两个变量，这两个变量之间是使用":"来切分开的
    * */
    master = context.actorSelection(s"akka.tcp://MasterSystem@$masterHost:$masterPort/user/Master")
    //向Master发送注册消息
    //这个注册过程是在集群上的Woker节点启动之后，Worker向Master节点进行通信，以让Worker取得与Master的链接的过程
    master ! RegisterWorker(workerId, memory, cores)
  }

  //=============================================3
  /*
  * receive是当Worker接收到Master发来的信息之后，要根据接收到的信息的类型给Master进行回复
  * */
  override def receive: Receive = {

    //==========================================3.6
    /*
    * ##转自,f1,3.6
    * 当Worker在Master上注册成功之后。Master会给Worker发送一个RegisteredWorker 信息。
    * Worker的Receive函数在接收到Master发送的信息之后，使用模式匹配来进行匹配，确定Master发给Worker的信息是RegisteredWorker
    * 这时候就会执行  case RegisteredWorker(masterUrl)  当中的代码了
    * */
    case RegisteredWorker(masterUrl) => {
      println(masterUrl)
      //启动定时器发送心跳
      import context.dispatcher

      //=============================================3.7
      /*
      *
      * Worker在接收到Master发送的RegisteredWorker信息之后，会设立一个定时器，定时地向Master发送心跳，“报活”
      * context.system.scheduler 中的scheduler是一个调度器。这个调度器可以发送信息
      * schedule(0 millis, HEART_INTERVAL millis, self, SendHeartbeat) 参数
      * 0 代表立刻发送心跳。 1 代表间隔一个周期之后再和Master发送心跳
      * millis  是一个时间单位，表示心跳的时间间隔的单位是“毫秒”级别。要使用millis 需要导包import scala.concurrent.duration._
      * HEART_INTERVAL 是我的心跳的间隔时间
      * millis 代表我的HEART_INTERVAL 的时间单位是millis毫秒
      * self 这个形参是设定接受这个由scheduler 发送的消息的类是谁。这里使用self代表Worker发送了这个消息，
      * 同时接受这个消息的类还是这个Worker对象自己
      * SendHeartbeat  这个形参是我的这个scheduler 发送了什么消息
      * */
      //多长时间后执行 单位,多长时间执行一次 单位, 消息的接受者(直接给master发不好, 先给自己发送消息,
      // 以后可以做下判断, 什么情况下再发送消息), 信息
      //=============================================3.8
      /*
      * ##转,f2,3.8
      * */
      context.system.scheduler.schedule(0 millis, HEART_INTERVAL millis, self, SendHeartbeat)
    }

      //=================================================3.9
      /*
      * ##转自,f2,3.9
      * 我的Worker给自己发送了一个消息，在 3.8 过程中，这样我的Receive函数中就必须得有一个case类用来接受传送过来的消息
      * SendHeartbeat
      * */
    case SendHeartbeat => {
      println("send heartbeat to master")

      //================================================3.10
      /*
      * 等Worker给自己发送的消息发送完了之后，就要给Master发送消息了。
      * 发送的消息的类型是 Heartbeat 类型
      * */
      //================================================3.11
      /*
      * ##转,f2,3.11
      * */
      master ! Heartbeat(workerId)
    }
  }
}

object Worker {
  def main(args: Array[String]) {

    //===================================================5
    /*
    * 对我的Worker的config进行配置
    * */
    //这是Worker的IP地址和端口号
    val host = args(0)
    val port = args(1).toInt

    //这是Master的IP地址和端口号
    val masterHost = args(2)
    val masterPort = args(3).toInt

    //我这个Worker当前有多少的内存和内核
    val memory = args(4).toInt
    val cores = args(5).toInt
    // 准备配置
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)

    //===================================================1
    /*
    * 给你的当前的Worker进程取一个名字，叫做WorkerSystem
    * */
    //ActorSystem老大，辅助创建和监控下面的Actor，他是单例的
    val actorSystem = ActorSystem("WorkerSystem", config)

    //==================================================2
    /*
    * 通过这个actorSystem创建Worker的actor
    * param:
    * Props:我需要将我要创建成Actor的Worker类封装进Props当中。
    * “Worker” ： 是指我创建的actor进程的名字叫做Worker
    * */
    actorSystem.actorOf(Props(new Worker(masterHost, masterPort, memory, cores)), "Worker")
    actorSystem.awaitTermination()
  }
}
