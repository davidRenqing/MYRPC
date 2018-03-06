//===========================================f4
package cn.itcast.rpc

/**
  * Created by root on 2016/5/13.
  */
//===========================================3.4
/*
* 这个WorkInfo当中有4个成员变量属性。
* id:记录Worker的id
* memory:记录这个Worker一共有多少内存
* cores:记录这个Worker中一共有多少的CPU的核数
* lastHeartbeatTime ：代表这个Worker上一次向Master进行通信的时间
* */
class WorkerInfo(val id: String, val memory: Int, val cores: Int) {

  //TODO 上一次心跳
  var lastHeartbeatTime : Long = _
}
