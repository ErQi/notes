# Looper #
Looper是Handler和Thread相关联的桥梁,也是APP开发中线程间通信的用的最多的一个.
然而使用率却是异常的低下,因为在向主线程交互的时Looper该做的工作已经做好,使用上只需要在主线程创建Handler对象,然后再需要的地方发送消息即可.

[参考博客  Android 异步消息处理机制 让你深入理解 Looper、Handler、Message三者关系 洪洋](http://blog.csdn.net/lmj623565791/article/details/38377229)
## Looper的初始化创建 ##
Looper和线程的关系是一一对应的,每一个线程有且只有一个Looper,可以从Looper的使用中看出.
```
    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
```
在使用子线程中使用Hander时,需要有Looper对象,而Looper的构造函数是private修饰的,想要获得创建一个Looper对象就只能调用静态方法prepare.
在方法中我们可以看到调用了`sThreadLocal.get()`方法,返回的若非空就会抛出异常.这里就限定了一个线程只能有一个Looper.

## Looper的构造函数 ##
```
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
```
在构造函数中创建了一个MessageQueue(消息队列).

## Looper的使用 ##
```
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;

        // Make sure the identity of this thread is that of the local process,
        // and keep track of what that identity token actually is.
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            // This must be in a local variable, in case a UI event sets the logger
            Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            msg.target.dispatchMessage(msg);

            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }

            msg.recycleUnchecked();
        }
    }

```
当操作完成就可以调用Looper.loop();从代码中可以看出在通过myLooper中拿到了当前线程的Looper对象,获取了对应的MessageQueue之后,该线程会执行一个死循环,在死循环中不停的读取MessageQueue队列中的Message并调用 msg.target.dispatchMessage(msg);去处理该消息.

# Handler #
在使用Handler时,我们通常都是实例化一个对象去发送消息,然后在handleMessage方法中去处理对应的消息,并作出对应的处理.

## Hanlder的初始化 ##
```
    public Handler() {
        this(null, false);
    }

    public Handler(Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }

        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```
在初始化中我们看到了Handler将自己和Looper进行了关联,如若在子线程中未创建Looper就创建Hanlder那么便会抛出异常.并且在下面继续获取了Looper的消息队列.

## Handler的使用 ##
```
        Message message = Message.obtain();
        message.what = TEXT;
        message.obj = (int) (Math.random() * 1000);
        mHandler.sendMessage(message);
```
Handler使用方式十分之简单,在需要使用的地方发送一个Message消息即可,在创建Handler时重写handleMessage方法中处理对应的Message消息即可.
多数人走到这一步就停下了,因为已经知道如何使用Hanlder和Looper了,能满足跨线程交互需求了,也就没有继续探索的愿望了,然后在面试的一个一问Handler为什么能切换到主线程去执行对应的方法就懵逼了(面试问懵逼好几个了....)

## Handler跨线程的原因 ##
既然发送消息是在子线程,在handleMessage中处理的时候已经到了主线程,那么线程切换的奥秘一定就在这两个方法调用过程中,我们就从sendMessage方法中一步一步往下看.
```
    public final boolean sendMessage(Message msg)
    {
        return sendMessageDelayed(msg, 0);
    }

    public final boolean sendMessageDelayed(Message msg, long delayMillis)
    {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
    }

    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }
```
到这里就可以看出一点端倪了,mQueue出现了,也就是Handler在创建时获取的Looper的MessageQueue对象.
我们继续向下看
```
    private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
```
在这里我们又看到了一点意思的东西,`msg.target = this`在Looper.loop中我们看到`msg.target.dispatchMessage(msg);`这么一行代码,现在稍微一推敲就知道最终发生了什么,怎么切换线程的,但是我们还是继续往下看.
```
    boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }

        synchronized (this) {
            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w(TAG, e.getMessage(), e);
                msg.recycle();
                return false;
            }

            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // Inserted within the middle of the queue.  Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }
```
恩,到这里就是将Message加入到消息队列中去中去,hanlder中消息发送流程已经走完了.
在这里handler将Message加入到了消息队列中,而Looper一直线主线程中阻塞着,所以一收到消息就拿到了Message对象,然后在主线程中调用了Hanlder的处理方法.

# Looper对消息的处理 #
从上面的分析已经了解到了子线程和主线程是如何切换的,我们继续看在主线程中Looper是如何处理发送过来的消息的.
```
    // Looper.loop();中代码
    Message msg = queue.next(); // might block
    
    // MessageQueue中next();代码
    Message next() {
        // Return here if the message loop has already quit and been disposed.
        // This can happen if the application tries to restart a looper after quit
        // which is not supported.
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }

        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }

            nativePollOnce(ptr, nextPollTimeoutMillis);

            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
                        // Next message is not ready.  Set a timeout to wake up when it is ready.
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // Got a message.
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }

                // Process the quit message now that all pending messages have been handled.
                if (mQuitting) {
                    dispose();
                    return null;
                }

                // If first time idle, then get the number of idlers to run.
                // Idle handles only run if the queue is empty or if the first message
                // in the queue (possibly a barrier) is due to be handled in the future.
                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
                if (pendingIdleHandlerCount <= 0) {
                    // No idle handlers to run.  Loop and wait some more.
                    mBlocked = true;
                    continue;
                }

                if (mPendingIdleHandlers == null) {
                    mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                }
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }

            // Run the idle handlers.
            // We only ever reach this code block during the first iteration.
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler

                boolean keep = false;
                try {
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }

                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }

            // Reset the idle handler count to 0 so we do not run them again.
            pendingIdleHandlerCount = 0;

            // While calling an idle handler, a new message could have been delivered
            // so go back and look again for a pending message without waiting.
            nextPollTimeoutMillis = 0;
        }
    }
```
瞧,我们发现了什么,又是一个死循环阻塞,原来主线程不是阻塞在Looper中,而是阻塞在MessageQueue对象中.
可以看到当消息队列有消息的时候立马返回消息,没有消息就阻塞(我就偷了个懒,不在继续深入MessageQueue的原理了...)
Looper收到返回的消息会调用msg.target.dispatchMessage(msg)去处理,也就是调用Handler去处理.
我们继续来到Hanlder的处理方法中.
```
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
```
msg.callback我们先放一放,就我们普通发送的Message通常是没有callback对象的,下面的mCallback我们来看一看,该对象出现在构造函数之中,通过构造函数来赋值,我们来看看这个对象的定义
```
    public interface Callback {
        public boolean handleMessage(Message msg);
    }
```
可以看出就是一个接口,和自己重写handleMessage一样,只不过提供另一种方式来处理.(说实话,在成员对象创建上面还重写其方法,我感觉是看着挺难受的,这样抽取成对象,然后通过构造函数传递进去,看着舒服多了.)
回到正题中来,我们还剩下一个Messaged的callBack对象没整明白.
首先,我们来看一看这究竟是一个什么对象.
```
    /*package*/ Runnable callback;
```
可以看到就是一个任务而已,而handleCallback(msg)方法点进去再来看一看
```
    private static void handleCallback(Message message) {
        message.callback.run();
    }
```
只是单纯的是运行这个任务而已,但是这个任务是哪里来的呢?
答案在Handler,Handler不仅可以发消息给自己在主线程处理,也可以直接发送一个任务去主线程运行.
```
    public final boolean post(Runnable r)
    {
       return  sendMessageDelayed(getPostMessage(r), 0);
    }

    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }
```
到这里也就看明白了,剩下就和普通消息一样的处理方式了.

文章到此结束,顺便给自己打个小广告,深圳求职,目前在职招人顶缸中(ps:找个人顶缸真不好招...全是假简历)
[简历戳我](http://erqi.github.io/images/%E9%BD%90%E6%B6%9B.pdf)