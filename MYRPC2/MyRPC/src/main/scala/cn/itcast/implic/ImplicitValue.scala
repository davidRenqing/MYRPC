package cn.itcast.implic


/**
  * Created by root on 2016/5/13.
  */
//================================================2
/*
* 创建一个隐式转换的类Context类。这里面的aaaaa 和i 都是隐式转换类型的数据
* */
//所有的隐式值和隐式方法必须放到object
object Context {
  implicit val aaaaa = "laozhao"

  implicit val i = 1
}

object ImplicitValue {


  //===================================================1
  /*
  * 在这个sayHi的函数当中，有一个name属性，这个name属性的类型是一个String类型，但是是隐式转换的
  * */
  def sayHi()(implicit name: String = "laoduan"): Unit = {
    println(s"hi~ $name")
  }

  def main(args: Array[String]) {


//==================================================3
    /*
    * 你会发现，如果我不导入Context这个Object当中的所有的数据类型，这个sayHi()函数会输出"hi,laoduan"
    * 但是如果我导入了Context这个包，sayHi()函数将输出的值是"hi,laozhao"
    * 分析：
    * 为什么会这样呢？这是因为sayHi()函数中的name形参的类型是一个隐式转换类型的。当我的sayHi方法调用的时候，它会去Context类中
    * 找和我的name的数据类型是一样的String的变量，然后把这个变量打印出来。
    * 注意啊：
    * 当我调用sayHi方法的时候，我不能给类型为implicit的变量赋值。这个sayHi方法传入形参，如果给sayHi传入形参，会发现报错。
    * 如果我的Context类中有两个
    * */
    import Context._
    sayHi()
  }

}
