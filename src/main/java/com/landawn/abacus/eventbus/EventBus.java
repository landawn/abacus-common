/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.eventbus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.AndroidUtil;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MoreExecutors;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.ThreadMode;

// DO NOT try to move me out of this project. Somebody tried and gave up then. I'm small. I'm stay here.

/**
 * It is responsible for managing the registration and un-registration of subscribers,
 * posting events to subscribers, and handling sticky events. It also provides methods to get all registered subscribers,
 * get subscribers for a specific event type, and remove sticky events.
 * <br />
 * The EventBus class is thread-safe and can be used in a multi-thread environment.
 *
 * <pre>
 * <code>
 * final Object strSubscriber_1 = new Subscriber<String>() {
 *     &#64;Override
 *     public void on(String event) {
 *     System.out.println("Subscriber: strSubscriber_1, event: " + event);
 *     }
 * };
 *
 * final Object anySubscriber_2 = new Object() {
 *     &#64;Subscribe(threadMode = ThreadMode.DEFAULT, interval = 1000)
 *     public void anyMethod(Object event) {
 *     System.out.println("Subscriber: anySubscriber_2, event: " + event);
 *     }
 * };
 *
 * final Object anySubscriber_3 = new Object() {
 *     &#64;Subscribe(threadMode = ThreadMode.DEFAULT, sticky = true)
 *     public void anyMethod(Object event) {
 *     System.out.println("Subscriber: anySubscriber_3, event: " + event);
 *     }
 * };
 *
 * final EventBus eventBus = EventBus.getDefault();
 *
 * eventBus.register(strSubscriber_1);
 * eventBus.register(strSubscriber_1);
 * eventBus.register(anySubscriber_2, "eventId_2");
 *
 * eventBus.post("abc");
 * eventBus.postSticky("sticky");
 * eventBus.post("eventId_2", "abc");
 *
 * eventBus.post(123);
 * eventBus.post("eventId_2", 123);
 *
 * eventBus.register(anySubscriber_3);
 * </code>
 * </pre>
 *
 */
public class EventBus {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    private static final Map<Class<?>, List<SubIdentifier>> classMetaSubMap = new ConcurrentHashMap<>();

    private static final Executor DEFAULT_EXECUTOR;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            DEFAULT_EXECUTOR = AndroidUtil.getThreadPoolExecutor();
        } else {
            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(//
                    N.max(8, IOUtil.CPU_CORES), // coreThreadPoolSize
                    N.max(64, IOUtil.CPU_CORES), // maxThreadPoolSize
                    180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

            DEFAULT_EXECUTOR = threadPoolExecutor;

            MoreExecutors.addDelayedShutdownHook(threadPoolExecutor, 120, TimeUnit.SECONDS);
        }
    }

    private final Map<Object, List<SubIdentifier>> registeredSubMap = new LinkedHashMap<>();

    private final Map<String, Set<SubIdentifier>> registeredEventIdSubMap = new HashMap<>();

    private final Map<Object, String> stickyEventMap = new IdentityHashMap<>();

    private final String identifier;

    private final Executor executor;

    private final Map<String, List<SubIdentifier>> listOfEventIdSubMap = new ConcurrentHashMap<>();

    private List<List<SubIdentifier>> listOfSubEventSubs = null;

    private Map<Object, String> mapOfStickyEvent = null;

    private static final EventBus INSTANCE = new EventBus("default");

    @SuppressFBWarnings("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR")
    public EventBus() {
        this(Strings.guid());
    }

    /**
     *
     * @param identifier
     */
    public EventBus(final String identifier) {
        this(identifier, DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param identifier
     * @param executor
     */
    public EventBus(final String identifier, final Executor executor) {
        this.identifier = identifier;
        this.executor = executor == null ? DEFAULT_EXECUTOR : executor;

        if (executor != DEFAULT_EXECUTOR && executor instanceof ExecutorService) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                final ExecutorService executorService = (ExecutorService) executor;

                logger.warn("Starting to shutdown task in EventBus");

                try {
                    executorService.shutdown();

                    //noinspection ResultOfMethodCallIgnored
                    executorService.awaitTermination(60, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    logger.warn("Not all the requests/tasks executed in Eventbus are completed successfully before shutdown.");
                } finally {
                    logger.warn("Completed to shutdown task in Eventbus");
                }
            }));
        }
    }

    /**
     * Gets the default.
     *
     * @return
     */
    @SuppressFBWarnings("MS_EXPOSE_REP")
    public static EventBus getDefault() {
        return INSTANCE;
    }

    public String identifier() {
        return identifier;
    }

    /**
     * Returns the subscriber which is registered with specified {@code eventType}(or its subtypes) and {@code null} event id.
     *
     * @param eventType
     * @return
     */
    public List<Object> getSubscribers(final Class<?> eventType) {
        return getSubscribers(null, eventType);
    }

    /**
     * Returns the subscriber which is registered with specified {@code eventType}(or its subtypes) and {@code eventId}.
     * @param eventId
     * @param eventType
     *
     * @return
     */
    public List<Object> getSubscribers(final String eventId, final Class<?> eventType) {
        final List<Object> eventSubs = new ArrayList<>();

        synchronized (registeredSubMap) {
            for (final Map.Entry<Object, List<SubIdentifier>> entry : registeredSubMap.entrySet()) {
                for (final SubIdentifier sub : entry.getValue()) {
                    if (sub.isMyEvent(eventId, eventType)) {
                        eventSubs.add(entry.getKey());

                        break;
                    }
                }
            }
        }

        return eventSubs;
    }

    /**
     * Returns all registered subscribers.
     *
     * @return
     */
    public List<Object> getAllSubscribers() {
        synchronized (registeredSubMap) {
            return new ArrayList<>(registeredSubMap.keySet());
        }
    }

    /**
     *
     * @param subscriber
     * @return
     */
    public EventBus register(final Object subscriber) {
        return register(subscriber, (ThreadMode) null);
    }

    /**
     *
     * @param subscriber
     * @param eventId
     * @return
     */
    public EventBus register(final Object subscriber, final String eventId) {
        return register(subscriber, eventId, null);
    }

    /**
     *
     * @param subscriber
     * @param threadMode
     * @return
     */
    public EventBus register(final Object subscriber, final ThreadMode threadMode) {
        return register(subscriber, null, threadMode);
    }

    /**
     * Register the subscriber with the specified {@code eventId} and {@code threadMode}.
     * If the same register has been registered before, it can be over-written with the new specified {@code eventId} and {@code threadMode}.
     *
     * @param subscriber
     * @param eventId
     * @param threadMode
     * @return itself
     */
    public EventBus register(final Object subscriber, final String eventId, final ThreadMode threadMode) {
        if (!isSupportedThreadMode(threadMode)) {
            throw new RuntimeException("Unsupported thread mode: " + threadMode);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Registering subscriber: " + subscriber + " with eventId: " + eventId + " and thread mode: " + threadMode);
        }

        final Class<?> cls = subscriber.getClass();
        final List<SubIdentifier> subList = getClassSubList(cls);

        if (N.isEmpty(subList)) {
            throw new RuntimeException("No subscriber method found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final List<SubIdentifier> eventSubList = new ArrayList<>(subList.size());

        for (final SubIdentifier sub : subList) {
            if (sub.isPossibleLambdaSubscriber && Strings.isEmpty(eventId)) {
                throw new RuntimeException(
                        "General subscriber (type is {@code Subscriber} and parameter type is Object, mostly created by lambda) only can be registered with event id");
            }

            eventSubList.add(new SubIdentifier(sub, subscriber, eventId, threadMode));
        }

        synchronized (registeredSubMap) {
            registeredSubMap.put(subscriber, eventSubList);
            listOfSubEventSubs = null;
        }

        if (Strings.isEmpty(eventId)) {
            synchronized (registeredEventIdSubMap) {
                for (final SubIdentifier sub : eventSubList) {
                    if (Strings.isEmpty(sub.eventId)) {
                        continue;
                    }

                    final Set<SubIdentifier> eventSubs = registeredEventIdSubMap.get(sub.eventId);

                    if (eventSubs == null) {
                        registeredEventIdSubMap.put(sub.eventId, N.asLinkedHashSet(sub));
                    } else {
                        eventSubs.add(sub);
                    }

                    listOfEventIdSubMap.remove(sub.eventId);
                }
            }
        } else {
            synchronized (registeredEventIdSubMap) {
                final Set<SubIdentifier> eventSubs = registeredEventIdSubMap.get(eventId);

                if (eventSubs == null) {
                    registeredEventIdSubMap.put(eventId, N.newLinkedHashSet(eventSubList));
                } else {
                    eventSubs.addAll(eventSubList);
                }

                listOfEventIdSubMap.remove(eventId);
            }
        }

        Map<Object, String> localMapOfStickyEvent = mapOfStickyEvent;

        for (final SubIdentifier sub : eventSubList) {
            if (sub.sticky) {
                if (localMapOfStickyEvent == null) {
                    synchronized (stickyEventMap) {
                        localMapOfStickyEvent = new IdentityHashMap<>(stickyEventMap);
                        mapOfStickyEvent = localMapOfStickyEvent;
                    }
                }

                for (final Map.Entry<Object, String> entry : localMapOfStickyEvent.entrySet()) {
                    if (sub.isMyEvent(entry.getValue(), entry.getKey().getClass())) {
                        try {
                            dispatch(sub, entry.getKey());
                        } catch (final Exception e) {
                            logger.error("Failed to post sticky event: " + N.toString(entry.getValue()) + " to subscriber: " + N.toString(sub), e); //NOSONAR
                        }
                    }
                }
            }
        }

        return this;
    }

    /**
     * Gets the class sub list.
     *
     * @param cls
     * @return
     */
    private List<SubIdentifier> getClassSubList(final Class<?> cls) {
        synchronized (classMetaSubMap) {
            List<SubIdentifier> subs = classMetaSubMap.get(cls);

            if (subs == null) {
                subs = new ArrayList<>();
                final Set<Method> added = N.newHashSet();

                final Set<Class<?>> allTypes = ClassUtil.getAllSuperTypes(cls);
                allTypes.add(cls);

                for (final Class<?> supertype : allTypes) {
                    for (final Method method : supertype.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Subscribe.class) && Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                            final Class<?>[] parameterTypes = method.getParameterTypes();

                            if (parameterTypes.length != 1) {
                                throw new RuntimeException(
                                        method.getName() + " has " + parameterTypes.length + " parameters. Subscriber method must have exactly 1 parameter.");
                            }

                            if (added.add(method)) {
                                subs.add(new SubIdentifier(method));
                            }
                        }
                    }
                }

                if (Subscriber.class.isAssignableFrom(cls)) {
                    for (final Method method : cls.getDeclaredMethods()) {
                        if (method.getName().equals("on") && method.getParameterTypes().length == 1) {
                            if (added.add(method)) {
                                subs.add(new SubIdentifier(method));
                            }

                            break;
                        }
                    }
                }

                classMetaSubMap.put(cls, subs);
            }

            return subs;
        }
    }

    //    /**
    //     *
    //     * @param subscriber General subscriber (type is {@code Subscriber} and parameter type is Object, mostly created by lambda) only can be registered with event id
    //     * @param eventId
    //     * @return
    //     */
    //    public <T> EventBus register(final Subscriber<T> subscriber) {
    //        return register(subscriber, (ThreadMode) null);
    //    }

    /**
     *
     * @param <T>
     * @param subscriber General subscriber (type is {@code Subscriber} and parameter type is Object, mostly created by lambda) only can be registered with event id
     * @param eventId
     * @return
     */
    public <T> EventBus register(final Subscriber<T> subscriber, final String eventId) {
        return register(subscriber, eventId, null);
    }

    //    /**
    //     * @param subscriber General subscriber (type is {@code Subscriber} and parameter type is Object, mostly created by lambda) only can be registered with event id
    //     * @param threadMode
    //     * @return
    //     */
    //    public <T> EventBus register(final Subscriber<T> subscriber, ThreadMode threadMode) {
    //        return register(subscriber, (String) null, threadMode);
    //    }

    /**
     *
     * @param <T>
     * @param subscriber General subscriber (type is {@code Subscriber} and parameter type is Object, mostly created by lambda) only can be registered with event id
     * @param eventId
     * @param threadMode
     * @return
     */
    public <T> EventBus register(final Subscriber<T> subscriber, final String eventId, final ThreadMode threadMode) {
        final Object tmp = subscriber;
        return register(tmp, eventId, threadMode);
    }

    /**
     *
     * @param subscriber
     * @return
     */
    public EventBus unregister(final Object subscriber) {
        if (logger.isDebugEnabled()) {
            logger.debug("Unregistering subscriber: " + subscriber);
        }

        Set<SubIdentifier> subEvents = null;

        synchronized (registeredSubMap) {
            subEvents = N.newHashSet(registeredSubMap.remove(subscriber));
            listOfSubEventSubs = null;
        }

        if (N.notEmpty(subEvents)) {
            synchronized (registeredEventIdSubMap) {
                final List<String> keyToRemove = new ArrayList<>();

                for (final Map.Entry<String, Set<SubIdentifier>> entry : registeredEventIdSubMap.entrySet()) {
                    entry.getValue().removeAll(subEvents);

                    if (entry.getValue().isEmpty()) {
                        keyToRemove.add(entry.getKey());
                    }

                    listOfEventIdSubMap.remove(entry.getKey());
                }

                if (N.notEmpty(keyToRemove)) {
                    for (final String key : keyToRemove) {
                        registeredEventIdSubMap.remove(key);
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param event
     * @return
     */
    public EventBus post(final Object event) {
        return post((String) null, event);
    }

    /**
     *
     * @param eventId
     * @param event
     * @return
     */
    public EventBus post(final String eventId, final Object event) {
        final Class<?> cls = event.getClass();

        List<List<SubIdentifier>> listOfSubs = listOfSubEventSubs;

        if (Strings.isEmpty(eventId)) {
            if (listOfSubs == null) {
                synchronized (registeredSubMap) {
                    listOfSubs = new ArrayList<>(registeredSubMap.values()); // in case concurrent register/unregister.
                    listOfSubEventSubs = listOfSubs;
                }
            }
        } else {
            List<SubIdentifier> listOfEventIdSub = listOfEventIdSubMap.get(eventId);

            if (listOfEventIdSub == null) {
                synchronized (registeredEventIdSubMap) {
                    if (registeredEventIdSubMap.containsKey(eventId)) {
                        listOfEventIdSub = new ArrayList<>(registeredEventIdSubMap.get(eventId)); // in case concurrent register/unregister.
                    } else {
                        listOfEventIdSub = N.emptyList();
                    }

                    listOfEventIdSubMap.put(eventId, listOfEventIdSub);
                }

                listOfSubs = Collections.singletonList(listOfEventIdSub);
            }
        }

        for (final List<SubIdentifier> subs : listOfSubs) {
            for (final SubIdentifier sub : subs) {
                if (sub.isMyEvent(eventId, cls)) {
                    try {
                        dispatch(sub, event);
                    } catch (final Exception e) {
                        logger.error("Failed to post event: " + N.toString(event) + " to subscriber: " + N.toString(sub), e);
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param event
     * @return
     */
    public EventBus postSticky(final Object event) {
        return postSticky(null, event);
    }

    /**
     *
     * @param eventId
     * @param event
     * @return
     */
    public EventBus postSticky(final String eventId, final Object event) {
        synchronized (stickyEventMap) {
            stickyEventMap.put(event, eventId);

            mapOfStickyEvent = null;
        }

        post(eventId, event);

        return this;
    }

    /**
     * Remove the sticky event posted with {@code null} event id.
     *
     * @param event
     * @return {@code true}, if successful
     */
    public boolean removeStickyEvent(final Object event) {
        return removeStickyEvent(event, null);
    }

    /**
     * Remove the sticky event posted with the specified {@code eventId}.
     *
     * @param event
     * @param eventId
     * @return {@code true}, if successful
     */
    public boolean removeStickyEvent(final Object event, final String eventId) {
        synchronized (stickyEventMap) {
            final String val = stickyEventMap.get(event);

            if (N.equals(val, eventId) && (val != null || stickyEventMap.containsKey(event))) {
                stickyEventMap.remove(event);

                mapOfStickyEvent = null;
                return true;
            }
        }

        return false;
    }

    /**
     * Remove the sticky events which can be assigned to specified {@code eventType} and posted with {@code null} event id.
     *
     * @param eventType
     * @return {@code true} if one or one more than sticky events are removed, otherwise, {@code false}.
     */
    public boolean removeStickyEvents(final Class<?> eventType) {
        return removeStickyEvents(null, eventType);
    }

    /**
     * Remove the sticky events which can be assigned to specified {@code eventType} and posted with the specified {@code eventId}.
     * @param eventId
     * @param eventType
     *
     * @return {@code true} if one or one more than sticky events are removed, otherwise, {@code false}.
     */
    public boolean removeStickyEvents(final String eventId, final Class<?> eventType) {
        final List<Object> keyToRemove = new ArrayList<>();

        synchronized (stickyEventMap) {
            for (final Map.Entry<Object, String> entry : stickyEventMap.entrySet()) {
                if (N.equals(entry.getValue(), eventId) && eventType.isAssignableFrom(entry.getKey().getClass())) {
                    keyToRemove.add(entry);
                }
            }

            if (N.notEmpty(keyToRemove)) {
                synchronized (stickyEventMap) {
                    for (final Object event : keyToRemove) {
                        stickyEventMap.remove(event);
                    }

                    mapOfStickyEvent = null;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all sticky events.
     */
    public void removeAllStickyEvents() {
        synchronized (stickyEventMap) {
            stickyEventMap.clear();

            mapOfStickyEvent = null;
        }
    }

    /**
     * Returns the sticky events which can be assigned to specified {@code eventType} and posted with {@code null} event id.
     *
     * @param eventType
     * @return
     */
    public List<Object> getStickyEvents(final Class<?> eventType) {
        return getStickyEvents(null, eventType);
    }

    /**
     * Returns the sticky events which can be assigned to specified {@code eventType} and posted with the specified {@code eventId}.
     * @param eventId
     * @param eventType
     *
     * @return
     */
    public List<Object> getStickyEvents(final String eventId, final Class<?> eventType) {
        final List<Object> result = new ArrayList<>();

        synchronized (stickyEventMap) {
            for (final Map.Entry<Object, String> entry : stickyEventMap.entrySet()) {
                if (N.equals(entry.getValue(), eventId) && eventType.isAssignableFrom(entry.getKey().getClass())) {
                    result.add(entry.getKey());
                }
            }
        }

        return result;
    }

    /**
     * Checks if is supported thread mode.
     *
     * @param threadMode
     * @return {@code true}, if is supported thread mode
     */
    protected boolean isSupportedThreadMode(final ThreadMode threadMode) {
        return threadMode == null || threadMode == ThreadMode.DEFAULT || threadMode == ThreadMode.THREAD_POOL_EXECUTOR;
    }

    /**
     *
     * @param identifier
     * @param event
     */
    protected void dispatch(final SubIdentifier identifier, final Object event) {
        switch (identifier.threadMode) {
            case DEFAULT:
                post(identifier, event);

                return;

            case THREAD_POOL_EXECUTOR:
                executor.execute(() -> post(identifier, event));

                return;

            default:
                throw new RuntimeException("Unsupported thread mode");
        }
    }

    /**
     *
     * @param sub
     * @param event
     */
    protected void post(final SubIdentifier sub, final Object event) {
        try {
            if (sub.intervalInMillis > 0 || sub.deduplicate) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (sub) { //NOSONAR
                    if (sub.intervalInMillis > 0 && System.currentTimeMillis() - sub.lastPostTime < sub.intervalInMillis) {
                        // ignore.
                        if (logger.isDebugEnabled()) {
                            logger.debug("Ignoring event: " + N.toString(event) + " to subscriber: " + N.toString(sub) + " because it's in the interval: "
                                    + sub.intervalInMillis);
                        }
                    } else if (sub.deduplicate && (sub.previousEvent != null || sub.lastPostTime > 0) && N.equals(sub.previousEvent, event)) {
                        // ignore.
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                    "Ignoring event: " + N.toString(event) + " to subscriber: " + N.toString(sub) + " because it's same as previous event");
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Posting event: " + N.toString(event) + " to subscriber: " + N.toString(sub));
                        }

                        sub.lastPostTime = System.currentTimeMillis();

                        if (sub.deduplicate) {
                            sub.previousEvent = event;
                        }

                        sub.method.invoke(sub.instance, event);
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Posting event: " + N.toString(event) + " to subscriber: " + N.toString(sub));
                }

                sub.method.invoke(sub.instance, event);
            }
        } catch (final Exception e) {
            logger.error("Failed to post event: " + N.toString(event) + " to subscriber: " + N.toString(sub), e);
        }
    }

    /**
     * The Class SubIdentifier.
     */
    public static final class SubIdentifier {

        /** The cached classes. */
        final Map<Class<?>, Boolean> cachedClasses = new ConcurrentHashMap<>();

        /** The obj. */
        final Object instance;

        /** The method. */
        final Method method;

        /** The parameter type. */
        final Class<?> parameterType;

        /** The event id. */
        final String eventId;

        /** The thread mode. */
        final ThreadMode threadMode;

        /** The strict event type. */
        final boolean strictEventType;

        /** The sticky. */
        final boolean sticky;

        /** The interval. */
        final long intervalInMillis;

        /** The deduplicate. */
        final boolean deduplicate;

        /** The is possible lambda subscriber. */
        final boolean isPossibleLambdaSubscriber;

        /** The last post time. */
        long lastPostTime = 0;

        /** The previous event. */
        Object previousEvent = null;

        /**
         * Instantiates a new sub identifier.
         *
         * @param method
         */
        SubIdentifier(final Method method) {
            final Subscribe subscribe = method.getAnnotation(Subscribe.class);
            instance = null;
            this.method = method;
            parameterType = ClassUtil.isPrimitiveType(method.getParameterTypes()[0]) ? ClassUtil.wrap(method.getParameterTypes()[0])
                    : method.getParameterTypes()[0];
            eventId = subscribe == null || Strings.isEmpty(subscribe.eventId()) ? null : subscribe.eventId();
            threadMode = subscribe == null ? ThreadMode.DEFAULT : subscribe.threadMode();
            strictEventType = subscribe != null && subscribe.strictEventType();
            sticky = subscribe != null && subscribe.sticky();
            intervalInMillis = subscribe == null ? 0 : subscribe.interval();
            deduplicate = subscribe != null && subscribe.deduplicate();

            isPossibleLambdaSubscriber = Subscriber.class.isAssignableFrom(method.getDeclaringClass()) && method.getName().equals("on")
                    && parameterType.equals(Object.class) && subscribe == null;

            ClassUtil.setAccessible(method, true);
        }

        /**
         * Instantiates a new sub identifier.
         *
         * @param sub
         * @param obj
         * @param eventId
         * @param threadMode
         */
        SubIdentifier(final SubIdentifier sub, final Object obj, final String eventId, final ThreadMode threadMode) {
            instance = obj;
            method = sub.method;
            parameterType = sub.parameterType;
            this.eventId = Strings.isEmpty(eventId) ? sub.eventId : eventId;
            this.threadMode = threadMode == null ? sub.threadMode : threadMode;
            strictEventType = sub.strictEventType;
            sticky = sub.sticky;
            intervalInMillis = sub.intervalInMillis;
            deduplicate = sub.deduplicate;
            isPossibleLambdaSubscriber = sub.isPossibleLambdaSubscriber;
        }

        /**
         * Checks if is my event.
         * @param eventId
         * @param eventType
         *
         * @return {@code true}, if is my event
         */
        boolean isMyEvent(final String eventId, final Class<?> eventType) {
            if (!N.equals(this.eventId, eventId)) {
                return false;
            }

            return cachedClasses.computeIfAbsent(eventType, k -> strictEventType ? parameterType.equals(eventType) : parameterType.isAssignableFrom(eventType));
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 31 * h + N.hashCode(instance);
            h = 31 * h + N.hashCode(method);
            h = 31 * h + N.hashCode(parameterType);
            h = 31 * h + N.hashCode(eventId);
            h = 31 * h + N.hashCode(threadMode);
            h = 31 * h + N.hashCode(strictEventType);
            h = 31 * h + N.hashCode(sticky);
            h = 31 * h + N.hashCode(intervalInMillis);
            h = 31 * h + N.hashCode(deduplicate);
            return 31 * h + N.hashCode(isPossibleLambdaSubscriber);
        }

        /**
         *
         * @param obj
         * @return {@code true}, if successful
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof SubIdentifier other) {
                return N.equals(instance, other.instance) && N.equals(method, other.method) && N.equals(parameterType, other.parameterType)
                        && N.equals(eventId, other.eventId) && N.equals(threadMode, other.threadMode) && N.equals(strictEventType, other.strictEventType)
                        && N.equals(sticky, other.sticky) && N.equals(intervalInMillis, other.intervalInMillis) && N.equals(deduplicate, other.deduplicate)
                        && N.equals(isPossibleLambdaSubscriber, other.isPossibleLambdaSubscriber);
            }

            return false;
        }

        @Override
        public String toString() {
            return "{obj=" + N.toString(instance) + ", method=" + N.toString(method) + ", parameterType=" + N.toString(parameterType) + ", eventId="
                    + N.toString(eventId) + ", threadMode=" + N.toString(threadMode) + ", strictEventType=" + N.toString(strictEventType) + ", sticky="
                    + N.toString(sticky) + ", interval=" + N.toString(intervalInMillis) + ", deduplicate=" + N.toString(deduplicate)
                    + ", isPossibleLambdaSubscriber=" + N.toString(isPossibleLambdaSubscriber) + "}";
        }

        //        public static void main(String[] args) {
        //            CodeGenerator.printClassMethod(SubIdentifier.class);
        //        }
    }
}
