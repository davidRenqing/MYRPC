//=========================================f2

package cn.itcast.rpc


//============================================1
/*
* 这个Message是Master和Worker进行序列化所需要发送的信息。
*Master和Worker要进行PRC通信的时候就要走网络，一旦走网络就会涉及到序列化和反序列化的操作
* */
/**
  * Created by root on 2016/5/13.
  */
trait RemoteMessage extends Serializable

//==================================================2
/*
* 定义用于注册使用的Worker，即RegisterWorker
* */
//Worker -> Master
case class RegisterWorker(id: String, memory: Int, cores: Int) extends RemoteMessage

//================================================3.11
/*
* 转自,f3,3.11
* 这个样例类是用来让Worker给Master发送信息的。
* Param:
* id:是指给这个Master发送消息的Worker的Ip地址
* */
//===============================================3.12
/*
* ##转,f1,3.12
* */
case class Heartbeat(id: String) extends RemoteMessage



//=====================================================3
/*
* 这是Master在收到Worker的注册信息之后，要给Worker发送的信息，
* Master告诉Worker，Master的IP地址。这个masterurl字段中存储着Master的IP地址
* */

//Master -> Worker
case class RegisteredWorker(masterUrl: String) extends RemoteMessage


//=========================================================3.8
/*
* 转自，f3,3.8
* 我的Worker自己给自己发消息就不用继承RemoteMessage 类了。因为自己给自己发消息就没有经过网络的shuffle过程了
* 也就没有了网络传输的过程了，所以也就不需要使用序列化的操作了
* */
//Worker -> self
case object SendHeartbeat
//======================================================3.9
/*
* ##转,f3,3.9
* */

// Master -> self
case object CheckTimeOutWorker

