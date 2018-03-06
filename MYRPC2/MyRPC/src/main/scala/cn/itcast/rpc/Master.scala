//=============================================f1

package cn.itcast.rpc

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration._



//============================1
/*
* 创建一个Master类，这个Master类继承akka的Actor类
* */
/**
  * Created by root on 2016/5/13.
  */
class Master(val host: String, val port: Int) extends Actor {

  //==============================================3.3
  /*
  * Master新建一个HashMap，用于存储所有的Worker发送过来的注册的信息
  * WorkerINfo是一个类，这个类中是为了保存Worker的信息用的
  * */
  // workerId -> WorkerInfo
  val idToWorker = new mutable.HashMap[String, WorkerInfo]()

  // WorkerInfo
  val workers = new mutable.HashSet[WorkerInfo]() //使用set删除快, 也可用linkList
  //超时检查的间隔
  val CHECK_INTERVAL = 15000


  //==============================================4
  /*
  * preStart()方法是在Actor类的构造方法调用之后，Receive方法调用之前，在整个Actor类的生命周期中只运行一次的方法
  * */
  override def preStart(): Unit = {
    println("preStart invoked")
    //导入隐式转换
    import context.dispatcher //使用timer太low了, 可以使用akka的, 使用定时器, 要导入这个包
    context.system.scheduler.schedule(0 millis, CHECK_INTERVAL millis, self, CheckTimeOutWorker)
  }


  //======================================2
  /*
  *重写receive方法，这个方法是用来接受发送给Actor类的信息的
  * */
  // 用于接收消息
  override def receive: Receive = {

    //============================================3
    /*
    * receive函数会根据接受到的消息的不同地类型来做出不同的相应。
    * 因此这里要设置case类
    * */

    //===============================================3.1
    //你比方说，现在RegisterWorker 接收到了Worker发送过来的这个RegisterWorker的注册信息

    case RegisterWorker(id, memory, cores) => {

      //==============================================3.2
      /*
      * Master在接收到Worker的注册信息之后，要将这些Worker的注册信息保存起来才可以
      * */

      /*
      * Master要进行判断一下，判断这个Worker是不是是不是已经注册过了。如果这个Worker没有进行注册过
      * 如果这个Worker没有进行注册过，那么Master就会把Worker的信息保存起来
      * */
      if(!idToWorker.contains(id)){
        //=============================================3.5
        /*
        * 如果这个Master在检测过程中发现这个Worker的注册信息还没有被接收到，
        * 此时Master就会把这个Worker的注册信息放到workInfo的数组当中
        * */
        //把Worker的信息封装起来保存到内存当中，Spark源码会把Worker不仅会把这些Master的注册信息保存在内存中，
        // 还会把这些注册信息保存在ZooKeeper和本地的文件系统当中，来进行一次持久化的操作
        val workerInfo = new WorkerInfo(id, memory, cores)
        idToWorker(id) = workerInfo
        workers += workerInfo

        //Master的act()在收到信息之后就会利用sender关键字，给Worker的act()函数进行通信
        //=============================================3.6
        //##转,f3,3.6
        sender ! RegisteredWorker(s"akka.tcp://MasterSystem@$host:$port/user/Master")//通知worker注册
      }
    }
      //=========================================================3.12
      /*
      * ##转自,f2,3.12
      * 当Master接收到Worker发送的Heartbeat消息之后
      * */
    case Heartbeat(id) => {
      if(idToWorker.contains(id)){
        val workerInfo = idToWorker(id)

        //Master会为这个Worker进行报活
        /*
        * 报活的方法就是获得当前系统的时间
        * 然后把这个Worker的上一次的心跳的时间设置成系统当前的时间
        * */
        val currentTime = System.currentTimeMillis()
        workerInfo.lastHeartbeatTime = currentTime
      }
    }

      /*
      * 检查当前的已经死掉了的Worker
      * */
    case CheckTimeOutWorker => {
      val currentTime = System.currentTimeMillis() //Master先获得当前系统的时间

      //如果这个Worker上一次报活的时间与当前的时间间隔差出来了CHECK_INTERVAL，
      // 即当前的Worker已经有CHECK_INTERVAL这么长时间没有和Master发送心跳了。Master就认为当前的这个Worker已经死掉了
      val toRemove = workers.filter(x => currentTime - x.lastHeartbeatTime > CHECK_INTERVAL)
      //然后这个Master就会把死掉的Worker从Worker的列表中把死掉的Worker删除掉
      for(w <- toRemove) {
        workers -= w
        idToWorker -= w.id
      }
      println(workers.size)
    }
  }
}


//===========================================5
/*
* 搞一个伴生对象Master，用来创造Master类的对象，进而开始运行Master的进程，用来接受消息
* */
object Master {
  def main(args: Array[String]) {

    val host = args(0)
    val port = args(1).toInt

    //=========================================8
    /*
    * 准备string类型的配置信息
    * */
    // 准备配置
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin  //这个stripMargin 是java当中的一个方法，会将字符串按照 | 来进行切割

    //============================================7
    /*
    * 创建一个config对象。这个config对象是Akka的config类型
    * 这些配置信息可以是从一个string类型的字符串中读取参数，也可以是从一个配置文件当中读取参数。
    * 在这里我们让从string类型的数组中去读取数据
    * */
    val config = ConfigFactory.parseString(configStr)

    //================================================6
    /*
    *创建一个ActorSystem对象。这个对象用于创建和监听所有的创建的Actor进程。并且它是单例的。
    * 所以你在创建ActorSystem对象的时候就不能使用new关键字来进行创建了。因为ActorSystem是一个Object对象
    * param：
    * MasterSystem  为这个ActorSystem起一个名字
    * config     为这个ActorSystem对象引入配置参数
    * */
    //ActorSystem老大，辅助创建和监控下面的Actor，他是单例的
    val actorSystem = ActorSystem("MasterSystem", config)

    //==========================================================9
    /*
    * 利用actorSystem对象创建Master的Actor对象
    * 第一个形参需要传进去一个Props对象。这个Props是一个case类，将Master类进行隐形转换为Props类
    * 这样actorSystem 就会创造一个Master对象的Actor
    * */
    //创建Actor
    val master = actorSystem.actorOf(Props(new Master(host, port)), "Master")

    //=============================================================10
    /*
    * 让这个Master进程等待运行结束
    * 这种结束是友好的结束。当Actor处理完了所有的消息之后，act()函数才退出程序的执行
    * */
    actorSystem.awaitTermination()
  }
}
