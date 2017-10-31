# 简介 #
tinker一款热修复的开源框架,相对于市面上其他热修复框架更具有竞争力.
[官方介绍](https://github.com/Tencent/tinker/wiki)

# 基本配置 #

## 插件配置 ##
tinker需要gradle插件进行配合编译.`2017.10.30 09:24:45 `时版本为`classpath "com.tinkerpatch.sdk:tinkerpatch-gradle-plugin:1.1.8"`
gradle插件已经发布到3.0.0版本,但是不兼容,源码未出,暂用2.3.3的gradle插件.
与之对象的gradle发布到4.1了同因为兼容问题,占用在暂用3.3版本.

## 参数配置 ##

通常处理方式是将配置写到单独的tinkerpatch.gradle文件中,然后通过```apply from: 'tinkerpatch.gradle'```进行引入
tinkerpatch文件配置如下.
```
apply plugin: 'tinkerpatch-support'

/**
 * 设置基准apk位置,从自动生成的 app-1.0.0-1028-11-19-41 文件里抓取,需要apk和R.txt文件
 */
def bakPath = file("${buildDir}/bakApk/")// 编译自动生成的路径
def basePath = file("base/bakApk/")// 基准包的路径
def baseInfo = "app"
def variantName = "debug"

/**
 * 对于插件各参数的详细解析请参考
 * http://tinkerpatch.com/Docs/SDK
 */
tinkerpatchSupport {
    /** 可以在debug的时候关闭 tinkerPatch **/
    /** 当disable tinker的时候需要添加multiDexKeepProguard和proguardFiles,
     这些配置文件本身由tinkerPatch的插件自动添加，当你disable后需要手动添加
     你可以copy本示例中的proguardRules.pro和tinkerMultidexKeep.pro,
     需要你手动修改'tinker.sample.android.app'本示例的包名为你自己的包名, com.xxx前缀的包名不用修改
     **/
    tinkerEnable = true
    reflectApplication = false // true 在application中配置, false在 DefaultApplicationLike 中配置

    autoBackupApkPath = "${bakPath}" // 自动生成的APK和R文件存放路径

    appKey = "5ccb4887a9230886"

    /** 注意: 若发布新的全量包, appVersion一定要更新 **/
    appVersion = "1.0.3"

    def pathPrefix = "${basePath}/${baseInfo}/${variantName}/" // 基准包全路径
    def name = "${project.name}-${variantName}"

    baseApkFile = "${pathPrefix}/${name}.apk"
    baseProguardMappingFile = "${pathPrefix}/${name}-mapping.txt"
    baseResourceRFile = "${pathPrefix}/${name}-R.txt"

    /**
     *  若有编译多flavors需求, 可以参照： https://github.com/TinkerPatch/tinkerpatch-flavors-sample
     *  注意: 除非你不同的flavor代码是不一样的,不然建议采用zip comment或者文件方式生成渠道信息（相关工具：walle 或者 packer-ng）
     **/
}

/**
 * 用于用户在代码中判断tinkerPatch是否被使能
 */
android {
    defaultConfig {
        buildConfigField "boolean", "TINKER_ENABLE", "${tinkerpatchSupport.tinkerEnable}"
    }
}

/**
 * 一般来说,我们无需对下面的参数做任何的修改
 * 对于各参数的详细介绍请参考:
 * https://github.com/Tencent/tinker/wiki/Tinker-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97
 */
tinkerPatch {
    ignoreWarning = false
    useSign = true
    dex {
        dexMode = "jar"
        pattern = ["classes*.dex"]
        loader = []
    }
    lib {
        pattern = ["lib/*/*.so"]
    }

    res {
        pattern = ["res/*", "r/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]
        ignoreChange = []
        largeModSize = 100
    }

    packageConfig {
    }
    sevenZip {
        zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
//        path = "/usr/local/bin/7za"
    }
    buildConfig {
        keepDexApply = false
    }
}
```
bakPath 编译生成包的位置.
basePath 基准包的位置.

增量更新是通过`appVersion`来控制的,建议每次发补丁都更新对应版本号,平台会自动控制不同版本之间的差异更新.