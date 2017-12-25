# 标题是伪命题 #
[参考资料  Android中为什么主线程不会因为Looper.loop()里的死循环卡死？ 知乎](http://www.zhihu.com/question/34652589)
之前对这个概念一直处于比较模糊的状态,也是一直被自己忽略了,认为可能涉及的东西过于复杂,所以不敢对自己问,为什么?
这两天状态不错,生活还是code比较有趣,简单而真实,所以曾经被忽略的问题不经意间又开始出现在脑海.
这个问题在我理解看来可以分为两个问题

# 为什么主线程需要阻塞 #

## 何谓主线程,和其他线程有什么不同之处 ##
- 何谓主线程
主线程,通常称之为UI线程,也就是APP进程被创建的时候所处的线程,和其他的线程一样都是一个普通的线程.
- 不同之处
从app角度来说,不同之处主要集中在UI界面更新上面,普通线程不能更新UI界面,如此设计也是为了程序的健壮性,毕竟不同线程同时对对象操作时为了保证其准确性都要进行加锁,而界面更新不能像对象那样仅仅保证准确性,还要保证其连贯性,一个按钮在同一段时间同步`(假同步)`发生了向左又向右的滑动自然是不可取的.

## 线程的生命周期 ##
一个进程或线程在CPU看来无非就是一段的可执行代码,代码执行完毕,线程的生命也就到头了.

## APP的生命周期 ##
从使用手机的角度来看,从点开APP图标开始,到完全退出APP结束.

## 主线程在哪里进行了阻塞 ##
我们知道APP的入口是在ActivityThread,一个Java类,有着main方法,而且main方法中的代码也不是很多.
```
    public static void main(String[] args) {
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
        SamplingProfilerIntegration.start();

        // CloseGuard defaults to true and can be quite spammy.  We
        // disable it here, but selectively enable it later (via
        // StrictMode) on debug builds, but using DropBox, not logs.
        CloseGuard.setEnabled(false);

        Environment.initForCurrentUser();

        // Set the reporter for event logging in libcore
        EventLogger.setReporter(new EventLoggingReporter());

        AndroidKeyStoreProvider.install();

        // Make sure TrustedCertificateStore looks in the right place for CA certificates
        final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
        TrustedCertificateStore.setDefaultUserDirectory(configDir);

        Process.setArgV0("<pre-initialized>");

        Looper.prepareMainLooper();

        ActivityThread thread = new ActivityThread();
        thread.attach(false);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }

        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }

        // End of event ActivityThreadMain.
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```
这就是main方法的全部代码了,23的源码.在其中Looper进行了初始化,但是并不是常规的prepare(),而是prepareMainLooper(),其实差别不大,只不过是给静态成员对象成员sMainLooper进行了初始化赋值,并不准予同进程中sMainLooper初始化第二次而已.
然后在代码末尾Looper.loop进行阻塞.

## 标题为什么是伪命题 ##
我们都知道主线程是随着APP的启动而启动,随着APP的结束而结束的`(多进程对应多主线程的情况我就将其看做一个统一的主线程)`.
APP要一直运行直到用户退出,那么主线程就必然不能代码运行完毕而终止,所以需要进行阻塞,直到用户退出了APP,才能停止阻塞,让CPU执行完剩下的代码,尔后代码执行完毕,主线程从而寿终正寝.

# 为什么主线程阻塞还能更新UI #
既然线程是在Looper中阻塞了,那么与Looper配合着出现的Handler肯定是少不了的.
至于Handler是如何进行线程切换[不了解的同学请戳这](http://erqi.github.io/2016/08/31/handler%E5%92%8CThread%E7%9A%84%E5%85%B3%E7%B3%BB)

## ActivityThread.H ##
很容易就在Activity中找到了继承自Handler的内部类H,并且重写了handleMessage方法,代码就不列出了.

## ActivityThread.H怎么和Looper交互的 ##
光有Handler是不行的,关键要有调用Handler的地方,然后Handler才能去处理,才会在主线程调用一个又一个方法.
答案在这!
```

        ActivityThread thread = new ActivityThread();
        thread.attach(false);
```
进一步来看attach()方法
```
    private void attach(boolean system) {
        ...
        if (!system) {
            ...
            final IActivityManager mgr = ActivityManagerNative.getDefault();
            try {
                mgr.attachApplication(mAppThread);
            } catch (RemoteException ex) {
                // Ignore
            }
            ...
        } else {
            ...
        }

       ...
    }
```
恩,想必你们都知道我想看什么了,这里传入了对象mAppThread,我只关心mAppThread对象,而他作为参数最终通过IPC传递到哪里去,能力有限就不再继续跟进了.
## ApplicationThread ##
上面我们说到了mAppThread对象,那么这个对象是哪个对象呢?就是ApplicationThread的实例化对象,代码不多,也就500来行,我就截取一点点来示例一下
```
        public final void schedulePauseActivity(IBinder token, boolean finished,
                boolean userLeaving, int configChanges, boolean dontReport) {
            sendMessage(
                    finished ? H.PAUSE_ACTIVITY_FINISHING : H.PAUSE_ACTIVITY,
                    token,
                    (userLeaving ? 1 : 0) | (dontReport ? 2 : 0),
                    configChanges);
        }

        public final void scheduleStopActivity(IBinder token, boolean showWindow,
                int configChanges) {
           sendMessage(
                showWindow ? H.STOP_ACTIVITY_SHOW : H.STOP_ACTIVITY_HIDE,
                token, 0, configChanges);
        }

        public final void scheduleCreateService(IBinder token,
                                                        ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
            updateProcessState(processState, false);
            CreateServiceData s = new CreateServiceData();
            s.token = token;
            s.info = info;
            s.compatInfo = compatInfo;

            sendMessage(H.CREATE_SERVICE, s);
        }

        public final void scheduleBindService(IBinder token, Intent intent,
                                              boolean rebind, int processState) {
            updateProcessState(processState, false);
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            s.rebind = rebind;

            if (DEBUG_SERVICE)
                Slog.v(TAG, "scheduleBindService token=" + token + " intent=" + intent + " uid="
                        + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
            sendMessage(H.BIND_SERVICE, s);
        }
```
聪明的同学已经明了,主线程阻塞之后生命周期等方法是如何启用的.
这一个个形似各种声明周期的方法,最终还调用了sendMessage()方法,让我们再来看看sendMessage方法的是怎么操作的
```
    private void sendMessage(int what, Object obj) {
        sendMessage(what, obj, 0, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1) {
        sendMessage(what, obj, arg1, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2) {
        sendMessage(what, obj, arg1, arg2, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2, boolean async) {
        if (DEBUG_MESSAGES) Slog.v(
            TAG, "SCHEDULE " + what + " " + mH.codeToString(what)
            + ": " + arg1 + " / " + obj);
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        if (async) {
            msg.setAsynchronous(true);
        }
        mH.sendMessage(msg);
    }
```
可以看到最终调用了mH.sendMessage()方法,而mH是谁呢?ActivityThread的成员变量是ActivityThread.H的实例化对象.

# 总结 #
到此想必各位也就明了,主线程确实是阻塞的,不阻塞那APP怎么能一直运行,所以说主线程阻塞是一个伪命题,只不过是没有弄明白既然阻塞了,为什么还能调用各种声明周期而已.
调用生命周期是因为有Looper,有MessageQueue,还有沟通的桥梁Handler,通过IPC机制调用Handler发送各种消息,保存到MessageQueue中,然后在主线程中的Looper提取了消息,并在主线程中调用Handler的方法去处理消息.最终完成各种声明周期.

文章到此结束,顺便给自己打个小广告,深圳求职,目前在职招人顶缸中(ps:找个人顶缸真不好招...全是假简历)
[简历戳我](http://erqi.github.io/images/%E9%BD%90%E6%B6%9B.pdf)

## 扩展的知识点 ##
IPC机制下的[startService流程分析](http://gityuan.com/2016/03/06/start-service/)
为什么选择Binder[为什么 Android 要采用 Binder 作为 IPC 机制？](https://www.zhihu.com/question/39440766/answer/89210950)