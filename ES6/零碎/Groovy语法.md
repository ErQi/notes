# 简介 #
Groovy脚本基于Java并且拓展了Java.
Groovy是一种动态语言,它和Java类似但是又具备脚本语言的特点,都在Java虚拟机中运行,当运行Groovy脚本时,它会先被编译成Java类字节码,然后通过JVM虚拟机执行这个Java字节码类.
[文章引用](http://blog.csdn.net/yanbober/article/details/49047515)

# 语法 #
## 注释 ##
Groovy的单行,多行,文档注释都和Java一样,只有一种特殊的单行注释不一样如下:
```
#!/usr/bin/env groovy
println "Hello from the shebang line"
```
这是UNIX系统支持的一种特殊单行注释,叫做Shebang line,用于指明脚本运行环境,这样就可以直接在终端中使用./xxx.groovy运行(当然文件运行权限是基础).

## 关键字 ##
Groovy关键字如下:
```
as、assert、break、case、catch、class、const、continue、def、default、do、else、enum、extends、false、finally、for、goto、if、implements、import、in、instanceof、interface、new、null、package、return、super、switch、this、throw、throws、trait、true、try、while
```
其中trait是2.3新增的关键字,被trait修饰的类,可以被其他类使用implements进行继承,从而调用其中的方法.

## 标识符 ##
Groovy的标识符和Java有些共同点和区别,主要是引用标识符的区别.

### 普通标识符 ###
普通标识符定义和C语言类似,只能以字母,美元符,下划线开始,不能以数字开始.
```
//正确
def name
def $name
def name_type
def foo.assert
//错误
def 5type
def a+b
```
### 引用标识符 ###
引用标识符出现在点后的表达式中,如下.
```
def map = [:]
//引用标示符中出现空格也是对的
map."an identifier with a space and double quotes" = "ALLOWED"
//引用标示符中出现横线也是对的
map.'with-dash-signs-and-single-quotes' = "ALLOWED"

assert map."an identifier with a space and double quotes" == "ALLOWED"
assert map.'with-dash-signs-and-single-quotes' == "ALLOWED"
```
Groovy的所有字符串都可以当做引用标识符定义,如下
```
//如下类型字符串作为引用标识符都是对的
map.'single quote'
map."double quote"
map.'''triple single quote'''
map."""triple double quote"""
map./slashy string/
map.$/dollar slashy string/$

//稍微特殊的GString，也是对的
def firstname = "Homer"
map."Simson-${firstname}" = "Homer Simson"

assert map.'Simson-Homer' == "Homer Simson"
```

## 字符及字符串 ##
Groovy有java.lang.String和groovy.lang.GString两种字符串对象.

### 单引号字符串 ###
单引号字符串时java.lang.String类型,不支持占位符插值操作.
```
def name = 'Test Groovy!'
def body = 'Test $name'

println(name) // Test Groovy!
println(body) // Test $name
```
body中的$name并不会替换.

### 三重单引号字符串 ###
三种单引号依旧是java.lang.String类型,相对于普通的单引号字符串它支持表示多行字符串,如下:
```
def aMultilineString = '''line one
line two
line three'''
```
三重单引号允许字符串的内容在多行出现,新的行被转换为"\n",其他所有的空白字符都被完整的按照文本原样保留.

### 双引号字符串 ###
双引号字符串可以是java.lang.String类型也可以是groovy.lang.GString类型.他们之间用插值占位符进行区分

插值占位符用${}或者$表示,${}用于一般代替字串或者表达式,$主要用于A.B的行驶中,具体如下.
```
def name = 'Guillaume' // a plain string
def greeting = "Hello ${name}"
assert greeting.toString() == 'Hello Guillaume'

def sum = "The sum of 2 and 3 equals ${2 + 3}"
assert sum.toString() == 'The sum of 2 and 3 equals 5'

def person = [name: 'Guillaume', age: 36]
assert "$person.name is $person.age years old" == 'Guillaume is 36 years old'
```
需要注意,$只对属性和基本运算有效,如果是方法调用,大括号,闭包等符号是无效的.如下
```
def number = 3.14
shouldFail(MissingPropertyException) {
    println "$number.toString()"
}

//该代码运行抛出groovy.lang.MissingPropertyException异常，因为Groovy认为去寻找number的名为toString的属性，所以异常
```
在表达式访问属性钱必须保证属性已经定义好(值为空也行),如果使用了未定义的属性会抛出`groovy.lang.MissingPropertyException`异常,GString还支持延迟运算,比如在GSting中使用闭包,闭包在调用GString的toString()方法时被延迟执行,闭包中可以有或一个参数,若指定一个参数,则参数会被传入Writer对象,我们可以利用这个Writer对象来写入字符,若没有参数,闭包返回值toString()方法被调用,如下.
```
//无参数闭包
  def number = 1;
  def eagerGString = "value == ${number}"
  def lazyGString = "value == ${ -> number }"

  println(eagerGString == "value == 1") // true
  println(lazyGString ==  "value == 1") // true
//一个参数闭包
  number = 2
  println(eagerGString == "value == 1") // true
  println(lazyGString ==  "value == 2") // true
  println("1 + 2 == ${-> 3}" )
```
从上面可以看出,普通的插值站位替换都是一样的,但是在使用有参闭包之后就不相同乐,这是因为普通插值表达式的值再被编译的那一刻就直接写死了,而闭包表达式是取得引用,由于延迟运算的原因会获得最新的值.

Gstring和String即使字符串一样,他们的HashCode也不会一样,如下:
```
assert "one: ${1}".hashCode() != "one: 1".hashCode()
```
由于想同字符串的String和GString的HashCode不同,所以一定要避免使用Gstring作为Map的Key,比如:
```
def key = "a"
def m = ["${key}": "letter ${key}"]     

assert m["a"] == null   //由于key的HashCode不同，所以取不到
```
### 多重双引号字符串 ###
多重双引号字符串也支持展位插值操作,我们要特别注意在多重双引号字符串中单引号和双引号转换问题,如下:
```
def name = 'Groovy'
def template = """
    Dear Mr ${name},

    You're the winner of the lottery!

    Yours sincerly,

    Dave
"""

assert template.toString().contains('Groovy')
```
### 斜线字符串 ###
斜线字符串和双引号字符串很类似,通常用在正则表达式中,如下.
```
//普通使用
def fooPattern = /.*foo.*/
assert fooPattern == '.*foo.*'
//含转义字符使用
def escapeSlash = /The character \/ is a forward slash/
assert escapeSlash == 'The character / is a forward slash'
//多行支持
def multilineSlashy = /one
    two
    three/

assert multilineSlashy.contains('\n')
//含站位符使用支持
def color = 'blue'
def interpolatedSlashy = /a ${color} car/

assert interpolatedSlashy == 'a blue car'
```
特别注意，一个空的斜线字符串会被Groovy解析器解析为一注释。

### 字符Characters ###
```
char c1 = 'A' 
assert c1 instanceof Character

def c2 = 'B' as char 
assert c2 instanceof Character

def c3 = (char)'C' 
assert c3 instanceof Character
```

## 数字Numbers ##
Groovy支持各种类型的整形和数值类型,通常支持Java支持的那些.

### 整形 ###
Groovy像Java一样支持如下一些整型，byte、char、short、int、long、java.lang.BigInteger。我们在使用中可以像下面例子一样：
```
// primitive types
byte  b = 1
char  c = 2
short s = 3
int   i = 4
long  l = 5

// infinite precision
BigInteger bi =  6


int xInt = 077 // 八进制
assert xInt == 63

int xInt = 0x77 // 十六进制
assert xInt == 119

int xInt = 0b10101111 // 二进制
assert xInt == 175
```

### 浮点型 ###
Groovy像Java一样支持如下一些浮点型，float、double、java.lang.BigDecimal。我们在使用中可以像下面例子一样：
```
// primitive types
float  f = 1.234
double d = 2.345

// infinite precision
BigDecimal bd =  3.456


assert 1e3  ==  1_000.0
assert 2E4  == 20_000.0
assert 3e+1 ==     30.0
assert 4E-2 ==      0.04
```

### Boolean类型 ###
和Java一样.
```
def myBooleanVariable = true
boolean untypedBooleanVar = false
booleanField = true
```

### List类型 ###
Groovy同样支持java.util.List类型,在Groovy中同样也允许向列表中增加或者移除对象,允许在运行时改变列表大小,保存在列表中的对象不受类型的限制,此外还可以通过超出列表范围的数来索引列表,如下:
```
//使用动态List
def numbers = [1, 2, 3]         
assert numbers instanceof List  
assert numbers.size() == 3

//List中存储任意类型
def heterogeneous = [1, "a", true]

//判断List默认类型
def arrayList = [1, 2, 3]
assert arrayList instanceof java.util.ArrayList

//使用as强转类型
def linkedList = [2, 3, 4] as LinkedList    
assert linkedList instanceof java.util.LinkedList

//定义指定类型List
LinkedList otherLinked = [3, 4, 5]          
assert otherLinked instanceof java.util.LinkedList

//定义List使用
def letters = ['a', 'b', 'c', 'd']
//判断item值
assert letters[0] == 'a'     
assert letters[1] == 'b'
//负数下标则从右向左index
assert letters[-1] == 'd'    
assert letters[-2] == 'c'
//指定item赋值判断
letters[2] = 'C'             
assert letters[2] == 'C'
//给List追加item
letters << 'e'               
assert letters[ 4] == 'e'
assert letters[-1] == 'e'
//获取一段List子集
assert letters[1, 3] == ['b', 'd']         
assert letters[2..4] == ['C', 'd', 'e'] 

//多维List支持
def multi = [[0, 1], [2, 3]]     
assert multi[1][0] == 2 
```

### Arrays类型 ###
Groovy中的数组和Java类似,如下:
```
//定义初始化String数组
String[] arrStr = ['Ananas', 'Banana', 'Kiwi']  
assert arrStr instanceof String[]    
assert !(arrStr instanceof List)

//使用def定义初始化int数组
def numArr = [1, 2, 3] as int[]      
assert numArr instanceof int[]       
assert numArr.size() == 3

//声明定义多维数组指明宽度
def matrix3 = new Integer[3][3]         
assert matrix3.size() == 3

//声明多维数组不指定宽度
Integer[][] matrix2                     
matrix2 = [[1, 2], [3, 4]]
assert matrix2 instanceof Integer[][]

//数组的元素使用及赋值操作
String[] names = ['Cédric', 'Guillaume', 'Jochen', 'Paul']
assert names[0] == 'Cédric'     
names[2] = 'Blackdrag'          
assert names[2] == 'Blackdrag'
```

### Map类型 ###
和Java中的Map对象基本相同.
```
//定义一个Map
def colors = [red: '#FF0000', green: '#00FF00', blue: '#0000FF']   
//获取一些指定key的value进行判断操作
assert colors['red'] == '#FF0000'    
assert colors.green  == '#00FF00'
//给指定key的对赋值value操作与判断    
colors['pink'] = '#FF00FF'           
colors.yellow  = '#FFFF00'           
assert colors.pink == '#FF00FF'
assert colors['yellow'] == '#FFFF00'
//判断Map的类型
assert colors instanceof java.util.LinkedHashMap
//访问Map中不存在的key为null
assert colors.unknown == null

//定义key类型为数字的Map
def numbers = [1: 'one', 2: 'two']
assert numbers[1] == 'one'
```
但是作为key的值需要注意
```
//把一个定义的变量作为Map的key，访问Map的该key是失败的
def key = 'name'
def person = [key: 'Guillaume']      
assert !person.containsKey('name')   
assert person.containsKey('key') 

//把一个定义的变量作为Map的key的正确写法---添加括弧，访问Map的该key是成功的
person = [(key): 'Guillaume']        
assert person.containsKey('name')    
assert !person.containsKey('key') 
```
第一次是将key对象作为字符串"key"来储存,第二次是将key的对象的值"name"作为key来保存.需要注意.

## 运算符 ##
Groovy支持次方运算
```
assert  2 ** 3 == 8

def f = 3
f **= 2
assert f == 9
```
非运算符
```
assert (!true)    == false                      
assert (!'foo')   == false                      
assert (!'')      == true 
```
安全占位符**?.**,用于避免空指针.只有当安全占位符前面的对象不为null时才会调用后面的方法
```
def person = Person.find { it.id == 123 }    
def name = person?.name                      
assert name == null  
```
Groovy支持.@直接访问操作符,因为Groovy自动支持属性getter方法,也就是说对象.属性会调用该属性的get方法,当不相调用get方法时,可以直接用域访问操作符,如下.
```
class User {
    public final String name                 
    User(String name) { this.name = name}
    String getName() { "Name: $name" }       
}
def user = new User('Bob')

assert user.name == 'Name: Bob'
assert user.@name == 'Bob'
```
Groovy支持.&方法指针操作符,因为闭包可以被作为一个方法的参数,如果想让一个方法作为另一个方法的参数则可以将一个方法当做成一个闭包作为另一个方法的参数.
```
    def list = ['a','b','c']  
    //常规写法 
    list.each{  
        println it  
    }  

    String printName(name){  
        println name  
    }  

    //方法指针操作符写法
    list.each(this.&printName)
```
Groovy支持** *. **展开运算符,一个集合使用展开运算符可以得到一个为原集合执行所指定方法后的集合,如下.
```
cars = [
   new Car(make: 'Peugeot', model: '508'),
   null,                                              
   new Car(make: 'Renault', model: 'Clio')]
assert cars*.make == ['Peugeot', null, 'Renault']     
assert null*.make == null 
```
上面根据mak属性展开,就得到了一个只有makr属性的集合.

## 程序结构 ##

### 包名 ###
包名的定义和作用及含义和Java完全一样.

### Imports引入 ###
导入方式及操作和Java一样.