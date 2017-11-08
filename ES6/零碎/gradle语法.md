# 简介 #
Gradle核心是基于Groovy的领域特定语言(DSL，具体概念参见[Groovy语法](Groovy.md))，具有非常好的扩展性，所以不管是简单的独立项目还是大型的多项目构建它都能高效的提高构建任务，尤其对多项目支持是非常牛逼的；Gradle还提供了局部构建功能，譬如构建一个单独子项目时它会构建这个子项目依赖的所有子项目；当然了他对远程仓库和本地库的支持也很到位.

既然Gradle核心是Groovy，Groovy本质又是Java，所以很明显可以发现Gradle环境必须依赖JDK与Groovy库，具体如下：
- JDK版本必须是JDK6以上；
- 因为Gradle自带Groovy库, 所以已安装的Groovy会被Gradle忽略；

# 基本语法 #
Gradle的实质是配置脚本,执行一种类型的配置脚本就会创建一个关联的对象,譬如执行Build script脚本就会创建一个Project对象,这个对象其实就是Gradle的代理对象.


脚本类型|关联类型
 --- | --- 
Build script|Project
Init script|Gradle
Settings script|Setting

Gradle的三种主要对象解释如下
- Project对象:每个build.gradle会转换成一个Project对象.
- Gradle对象:构建初始化时创建,整个构建执行过程中只有这么一个对象,一般很少去修改这个默认配置脚本
- Setting对象:每个setting.gradle会转换成为一个Setting对象.

可以看见,当我们编写制定类型Gradle脚本时,我们可以直接使用关联对象的属性和方法,当然了,每个脚本也都实现了Script接口,也就是说明我们也可以直接使用Script接口的属性与方法.

### 构建脚本Build script(Project) ###
在Gradle中每个带编译的工程都是一个Project(每个工程的build.gradle对应一个Project对象),每个Project在构建的时候都包含一系列的Task,这些Task中很多又是Gradle的插件默认支持的.

每个Project对象和build.gradle一一对应,一个项目在构建的时候都具备如下流程.
1. 为当前项目创建一个Settings类型实例
2. 如果当前项目存在settings.gradle文件,则通过该文件配置刚才创建的Settings实例.
3. 通过Settings实例的配置创建项目层级结构的Projec对象实例.
4. 最后通过上面创建的项目层级结构Project对象实例去执行每个Project对应的build.gradle脚本.


### 初始化脚本Init script(Gradle)和设置脚本 Settings script(Settings) ###
**Gradle对象**
初始化脚本Init script(Gradle)类型与Gradle的其他类型脚本,这种脚本在构建开始之前运行,主要的用途是为接下来的Build script做一些准备,我们如果需要编写初始化脚本Init script,则可以把它按规则放在值