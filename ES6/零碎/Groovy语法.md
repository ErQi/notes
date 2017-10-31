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
