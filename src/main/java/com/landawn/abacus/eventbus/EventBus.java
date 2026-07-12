/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.eventbus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.landawn.abacus.util.cs;

// DO NOT try to move me out of this project. Somebody tried and gave up then. I'm small. I stay here.

/**
 * A publish-subscribe event bus that simplifies communication between components.
 * It allows components to communicate without requiring explicit references to one another,
 * thus promoting loose coupling.
 *
 * <p>Supported features:</p>
 * <ul>
 *   <li>Event delivery to subscribers matched by event type hierarchy.</li>
 *   <li>Sticky events that persist and are delivered to newly registered subscribers.</li>
 *   <li>Thread mode control for synchronous or asynchronous event delivery.</li>
 *   <li>Event filtering by event ID.</li>
 *   <li>Interval-based throttling and deduplication of consecutive identical events.</li>
 * </ul>
 *
 * <p>Subscribers can be registered in two ways:</p>
 * <ol>
 *   <li>By implementing the {@link Subscriber} interface (suitable for lambda expressions).</li>
 *   <li>By annotating public instance methods with {@link Subscribe}.</li>
 * </ol>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Subscriber using the Subscriber interface (lambda-friendly)
 * final Subscriber<String> strSubscriber = event -> System.out.println("Received: " + event);
 *
 * // Subscriber using the @Subscribe annotation
 * final Object annotatedSubscriber = new Object() {
 *     @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
 *     public void handleEvent(String event) {
 *         System.out.println("Handled: " + event);
 *     }
 * };
 *
 * EventBus eventBus = EventBus.getDefault();
 * eventBus.register(strSubscriber, "myEventId");
 * eventBus.register(annotatedSubscriber);
 *
 * eventBus.post("Hello World");
 * eventBus.post("myEventId", "Targeted Event");
 * eventBus.postSticky("Sticky Message");
 *
 * eventBus.unregister(strSubscriber);
 * eventBus.unregister(annotatedSubscriber);
 * }</pre>
 *
 * <p>This class is thread-safe and can be used in multi-threaded environments.</p>
 *
 * @see Subscriber
 * @see Subscribe
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

    private final Map<Object, List<SubIdentifier>> registeredSubMap = new IdentityHashMap<>();

    private final Map<String, Set<SubIdentifier>> registeredEventIdSubMap = new HashMap<>();

    private final Map<Object, String> stickyEventMap = new IdentityHashMap<>();

    private final String identifier;

    private final Executor executor;

    private final Map<String, List<SubIdentifier>> listOfEventIdSubMap = new ConcurrentHashMap<>();

    private volatile List<List<SubIdentifier>> listOfSubEventSubs = null;

    private volatile Map<Object, String> mapOfStickyEvent = null;

    /**
     * Serializes the decision points of {@link #register} (make a subscriber visible + snapshot the
     * sticky events to replay) against {@link #postSticky} (record the sticky event + resolve its
     * delivery list). Holding this single lock across those two short critical sections (never across
     * the actual {@code dispatch(...)}, which may run subscriber code) guarantees that a sticky event
     * is delivered to a concurrently-registering subscriber exactly once — either by {@code postSticky}'s
     * own delivery (if the subscriber was already visible) or by {@code register}'s replay (if the event
     * was already recorded), but never both. It is always the outermost lock, so it cannot deadlock with
     * {@code registeredSubMap} / {@code registeredEventIdSubMap} / {@code stickyEventMap}.
     */
    private final Object stickyDeliveryLock = new Object();

    @SuppressWarnings("unused")
    private transient Thread shutdownHook;

    private static final EventBus INSTANCE = new EventBus("default");

    /**
     * Creates a new {@code EventBus} instance with a randomly generated identifier.
     * This {@code EventBus} will use the default executor for asynchronous event delivery.
     */
    private EventBus() {
        this(Strings.uuidWithoutHyphens());
    }

    /**
     * Creates a new {@code EventBus} instance with the specified identifier.
     * This {@code EventBus} will use the default executor for asynchronous event delivery.
     *
     * @param identifier the unique identifier for this {@code EventBus} instance
     */
    private EventBus(final String identifier) {
        this(identifier, DEFAULT_EXECUTOR);
    }

    /**
     * Creates a new {@code EventBus} instance with the specified identifier and executor.
     * The executor is used for asynchronous event delivery when {@link ThreadMode#THREAD_POOL_EXECUTOR} is specified.
     * If the executor is an {@link ExecutorService}, a shutdown hook will be registered to properly shutdown the executor.
     *
     * @param identifier the unique identifier for this {@code EventBus} instance
     * @param executor the executor to use for asynchronous event delivery, or {@code null} to use the default executor
     */
    private EventBus(final String identifier, final Executor executor) {
        this.identifier = identifier;
        this.executor = executor == null ? DEFAULT_EXECUTOR : executor;

        if (executor != DEFAULT_EXECUTOR && executor instanceof ExecutorService executorService) {
            shutdownHook = new Thread(() -> {

                logger.warn("Starting EventBus shutdown");

                try {
                    executorService.shutdown();

                    //noinspection ResultOfMethodCallIgnored
                    executorService.awaitTermination(60, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Not all EventBus tasks completed successfully before shutdown");
                } finally {
                    logger.warn("EventBus shutdown completed");
                }
            });

            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    /**
     * Returns the default {@code EventBus} instance.
     * This is a singleton instance with identifier "default" that can be used throughout the application.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = EventBus.getDefault();
     * eventBus.post("Hello World");
     * }</pre>
     *
     * @return the default {@code EventBus} instance
     */
    @SuppressFBWarnings("MS_EXPOSE_REP")
    public static EventBus getDefault() {
        return INSTANCE;
    }

    /**
     * Creates a new {@code EventBus} instance with a randomly generated identifier.
     * This {@code EventBus} will use the default executor for asynchronous event delivery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = EventBus.create();
     * }</pre>
     *
     * @return a new {@code EventBus} instance
     */
    public static EventBus create() {
        return new EventBus();
    }

    /**
     * Creates a new {@code EventBus} instance with the specified identifier.
     * This {@code EventBus} will use the default executor for asynchronous event delivery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = EventBus.create("myEventBus");
     * }</pre>
     *
     * @param identifier the unique identifier for this {@code EventBus} instance
     * @return a new {@code EventBus} instance with the given identifier
     */
    public static EventBus create(final String identifier) {
        return new EventBus(identifier);
    }

    /**
     * Creates a new {@code EventBus} instance with the specified identifier and executor.
     * The executor is used for asynchronous event delivery when {@link ThreadMode#THREAD_POOL_EXECUTOR} is specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * EventBus eventBus = EventBus.create("myEventBus", executor);
     * }</pre>
     *
     * @param identifier the unique identifier for this {@code EventBus} instance
     * @param executor the executor to use for asynchronous event delivery, or {@code null} to use the default executor
     * @return a new {@code EventBus} instance with the given identifier and executor
     */
    public static EventBus create(final String identifier, final Executor executor) {
        return new EventBus(identifier, executor);
    }

    /**
     * Returns the unique identifier of this {@code EventBus} instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = EventBus.create("myBus");
     * String id = eventBus.identifier();   // returns "myBus"
     * }</pre>
     *
     * @return the identifier of this {@code EventBus}
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Returns all subscribers that are registered to receive events of the specified type or its subtypes.
     * This method searches for subscribers registered without a specific event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> subscribers = eventBus.subscribers(String.class);
     * }</pre>
     *
     * @param eventType the event type to search for
     * @return a snapshot list of subscribers registered for the specified event type
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     */
    public List<Object> subscribers(final Class<?> eventType) {
        return subscribers(null, eventType);
    }

    /**
     * Returns all subscribers that are registered to receive events of the specified type and event ID.
     * The method checks both the event type hierarchy and the event ID when searching for subscribers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> subscribers = eventBus.subscribers("textEvents", String.class);
     * }</pre>
     *
     * @param eventId the event ID to match, or {@code null} for subscribers without event ID
     * @param eventType the event type to search for
     * @return a snapshot list of subscribers registered for the specified event type and ID
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     */
    public List<Object> subscribers(final String eventId, final Class<?> eventType) {
        N.checkArgNotNull(eventType, "eventType");

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
     * Returns all currently registered subscribers in this {@code EventBus}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> allSubscribers = eventBus.allSubscribers();
     * }</pre>
     *
     * @return a snapshot list of all currently registered subscribers
     */
    public List<Object> allSubscribers() {
        synchronized (registeredSubMap) {
            return new ArrayList<>(registeredSubMap.keySet());
        }
    }

    /**
     * Returns the number of subscribers currently registered with this {@code EventBus}.
     *
     * <p>Counts distinct subscriber instances (matching {@code allSubscribers().size()}), not the
     * number of individual {@link Subscribe @Subscribe} handler methods they expose.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (eventBus.countOfSubscribers() == 0) {
     *     // no subscribers registered
     * }
     * }</pre>
     *
     * @return the count of currently registered subscribers
     * @see #allSubscribers()
     * @see #hasSubscribers(Class)
     */
    public int countOfSubscribers() {
        synchronized (registeredSubMap) {
            return registeredSubMap.size();
        }
    }

    /**
     * Checks whether any registered subscriber would receive an event of the given type.
     *
     * <p>A subscriber matches when its {@link Subscribe @Subscribe} handler parameter type is
     * compatible with {@code eventType}: for a normal subscriber the parameter type must be
     * assignable from {@code eventType}; for a {@code strictEventType} subscriber the two types must
     * be exactly equal. The event-ID filter is intentionally ignored by this query — it reports
     * purely on event-type compatibility, so a {@code true} result does not guarantee delivery for a
     * specific event ID.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (eventBus.hasSubscribers(OrderPlacedEvent.class)) {
     *     eventBus.post(new OrderPlacedEvent(...));
     * }
     * }</pre>
     *
     * @param eventType the event class to test for (must not be {@code null})
     * @return {@code true} if at least one registered subscriber accepts {@code eventType}
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     * @see #countOfSubscribers()
     */
    public boolean hasSubscribers(final Class<?> eventType) {
        N.checkArgNotNull(eventType, "eventType");

        synchronized (registeredSubMap) {
            for (final List<SubIdentifier> subs : registeredSubMap.values()) {
                for (final SubIdentifier sub : subs) {
                    if (sub.strictEventType ? sub.parameterType.equals(eventType) : sub.parameterType.isAssignableFrom(eventType)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Registers a subscriber to receive events.
     * The subscriber must either implement the {@link Subscriber} interface or have methods annotated with {@link Subscribe}.
     * No registration-level thread mode override is applied, so each subscriber method is delivered events using the thread mode declared in its own {@link Subscribe} annotation (defaulting to {@link ThreadMode#DEFAULT}).
     *
     * <p>This {@code Object} overload requires a subscriber whose event handling is exposed through a
     * {@code public}, non-{@code static}, single-argument method annotated with {@link Subscribe}.
     * A <b>bare lambda</b> {@link Subscriber} (whose erased {@code on(...)} parameter type is
     * {@code Object}) cannot be registered here: it has no event ID, so this method throws
     * {@link IllegalStateException}. Register lambda / {@code Subscriber} instances through
     * {@link #register(Subscriber, String)} or {@link #register(Subscriber, String, ThreadMode)},
     * which require an event ID, instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(new Subscriber<String>() {
     *     @Override
     *     public void on(String event) {
     *         System.out.println("Received: " + event);
     *     }
     * });
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if no subscriber methods are found in the subscriber class
     * @throws RuntimeException if a {@code @Subscribe} method is {@code static} or does not declare exactly one parameter
     * @throws IllegalStateException if the subscriber is identified as a lambda subscriber (an event ID is required for lambda subscribers)
     */
    public EventBus register(final Object subscriber) {
        return register(subscriber, (ThreadMode) null);
    }

    /**
     * Registers a subscriber with a specific event ID.
     * The subscriber will only receive events posted with the same event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(mySubscriber, "userEvents");
     * // This subscriber will only receive events posted with "userEvents" ID
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @param eventId the event ID to filter events; must be non-empty for lambda-based subscribers
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if no subscriber methods are found in the subscriber class
     * @throws RuntimeException if a {@code @Subscribe} method is {@code static} or does not declare exactly one parameter
     * @throws IllegalStateException if the subscriber is identified as a lambda subscriber and {@code eventId} is empty or {@code null}
     */
    public EventBus register(final Object subscriber, final String eventId) {
        return register(subscriber, eventId, null);
    }

    /**
     * Registers a subscriber with a specific thread mode.
     * The thread mode determines on which thread the event will be delivered.
     * Lambda-based subscribers cannot be registered with this method; use
     * {@link #register(Subscriber, String, ThreadMode)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(mySubscriber, ThreadMode.THREAD_POOL_EXECUTOR);
     * // Events will be delivered on a background thread
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @param threadMode the thread mode override for event delivery, or {@code null} to use the thread mode declared in each subscriber method's {@link Subscribe} annotation
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if the thread mode is not supported or no subscriber methods are found in the subscriber class
     * @throws RuntimeException if a {@code @Subscribe} method is {@code static} or does not declare exactly one parameter
     * @throws IllegalStateException if the subscriber is identified as a lambda subscriber (an event ID is required for lambda subscribers)
     */
    public EventBus register(final Object subscriber, final ThreadMode threadMode) {
        return register(subscriber, null, threadMode);
    }

    /**
     * Registers a subscriber with both event ID and thread mode specifications.
     * This is the most comprehensive registration method that allows full control over event filtering and delivery.
     * If the subscriber has sticky event handling enabled, it will immediately receive any matching sticky events;
     * delivery of those sticky events respects each method's {@link ThreadMode}, so a {@code THREAD_POOL_EXECUTOR}
     * sticky delivery happens on a background thread rather than synchronously inside {@code register}.
     *
     * <p>Subscriber identity is tracked by reference (an {@link IdentityHashMap}). Re-registering the
     * <em>same</em> subscriber instance <em>replaces</em> its previous registration: the old set of
     * subscriber methods is removed from the event-id index and the new {@code eventId}/{@code threadMode}
     * take effect.
     *
     * <p><b>Registration overrides cover only {@code eventId} and {@code threadMode}.</b> The four
     * advanced delivery knobs &mdash; {@code sticky}, {@code strictEventType}, {@code intervalMillis}
     * (throttling) and {@code deduplicate} &mdash; are <b>annotation-only</b>: they can be configured
     * solely through a {@link Subscribe @Subscribe}-annotated method and there is intentionally no
     * registration-time override for them. Consequently a lambda / {@link Subscriber} registration
     * (which has no annotated method) can never be sticky, strict, throttled, or deduplicated; to use
     * any of those features, register an object whose method is annotated with {@code @Subscribe}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(mySubscriber, "userEvents", ThreadMode.THREAD_POOL_EXECUTOR);
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @param eventId the event ID to filter events; must be non-empty for lambda-based subscribers; {@code null} for no filtering otherwise
     * @param threadMode the thread mode override for event delivery, or {@code null} to use the thread mode declared in each subscriber method's {@link Subscribe} annotation
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if the thread mode is not supported or no subscriber methods are found in the subscriber class
     * @throws RuntimeException if a {@code @Subscribe} method is {@code static} or does not declare exactly one parameter
     * @throws IllegalStateException if the subscriber is identified as a lambda subscriber and {@code eventId} is empty or {@code null}
     */
    public EventBus register(final Object subscriber, final String eventId, final ThreadMode threadMode) {
        N.checkArgNotNull(subscriber, cs.subscriber);

        if (!isSupportedThreadMode(threadMode)) {
            throw new IllegalArgumentException("Unsupported thread mode: " + threadMode);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Registering subscriber: {} with eventId: {} and thread mode: {}", subscriber, eventId, threadMode);
        }

        final Class<?> subscriberClass = subscriber.getClass();
        final List<SubIdentifier> subIdentifiers = getClassSubList(subscriberClass);

        if (N.isEmpty(subIdentifiers)) {
            throw new IllegalArgumentException("No subscriber method found in class: " + ClassUtil.getCanonicalClassName(subscriberClass));
        }

        final List<SubIdentifier> eventSubList = new ArrayList<>(subIdentifiers.size());

        for (final SubIdentifier sub : subIdentifiers) {
            if (sub.isPossibleLambdaSubscriber && Strings.isEmpty(eventId)) {
                throw new IllegalStateException("Lambda subscribers must be registered with an event ID");
            }

            eventSubList.add(new SubIdentifier(sub, subscriber, eventId, threadMode));
        }

        Set<SubIdentifier> oldSubEvents = null;
        Map<Object, String> stickySnapshot = null;
        boolean hasStickySub = false;

        // Make the subscriber visible (registeredSubMap / registeredEventIdSubMap) AND snapshot the sticky
        // events to replay, atomically under stickyDeliveryLock — but do NOT dispatch while holding it.
        // This is what guarantees a sticky event reaches this subscriber exactly once relative to a
        // concurrent postSticky(...) (see the stickyDeliveryLock field doc).
        synchronized (stickyDeliveryLock) {
            synchronized (registeredSubMap) {
                final List<SubIdentifier> removed = registeredSubMap.put(subscriber, eventSubList);
                oldSubEvents = removed == null ? null : N.newHashSet(removed);
                listOfSubEventSubs = null;
            }

            if (N.notEmpty(oldSubEvents)) {
                removeFromEventIdSubMap(oldSubEvents);
            }

            if (Strings.isEmpty(eventId)) {
                synchronized (registeredEventIdSubMap) {
                    for (final SubIdentifier sub : eventSubList) {
                        if (Strings.isEmpty(sub.eventId)) {
                            continue;
                        }

                        final Set<SubIdentifier> eventSubs = registeredEventIdSubMap.get(sub.eventId);

                        if (eventSubs == null) {
                            registeredEventIdSubMap.put(sub.eventId, N.toLinkedHashSet(sub));
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

            for (final SubIdentifier sub : eventSubList) {
                if (sub.sticky) {
                    hasStickySub = true;
                    break;
                }
            }

            if (hasStickySub) {
                stickySnapshot = mapOfStickyEvent;

                if (stickySnapshot == null) {
                    synchronized (stickyEventMap) {
                        stickySnapshot = new IdentityHashMap<>(stickyEventMap);
                        mapOfStickyEvent = stickySnapshot;
                    }
                }
            }
        }

        // Replay matching sticky events to the newly-registered sticky subscribers OUTSIDE the lock
        // (dispatch may run subscriber code synchronously on this thread).
        if (hasStickySub) {
            for (final SubIdentifier sub : eventSubList) {
                if (sub.sticky) {
                    for (final Map.Entry<Object, String> entry : stickySnapshot.entrySet()) {
                        if (sub.isMyEvent(entry.getValue(), entry.getKey().getClass())) {
                            try {
                                dispatch(sub, entry.getKey());
                            } catch (final Exception e) {
                                logger.error("Failed to post sticky event: " + N.toString(entry.getKey()) + " with eventId: " + N.toString(entry.getValue())
                                        + " to subscriber: " + N.toString(sub), e); //NOSONAR
                            }
                        }
                    }
                }
            }
        }

        return this;
    }

    private List<SubIdentifier> getClassSubList(final Class<?> subscriberClass) {
        synchronized (classMetaSubMap) {
            List<SubIdentifier> subscriberMethods = classMetaSubMap.get(subscriberClass);

            if (subscriberMethods == null) {
                final Map<String, SubIdentifier> subscriberMethodMap = new LinkedHashMap<>();

                final Set<Class<?>> allTypes = ClassUtil.getAllSuperTypes(subscriberClass);
                allTypes.add(subscriberClass);

                for (final Class<?> supertype : allTypes) {
                    for (final Method method : supertype.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic()) {
                            if (Modifier.isStatic(method.getModifiers())) {
                                throw new RuntimeException(
                                        "Subscriber method must not be static: " + method + " in class: " + ClassUtil.getCanonicalClassName(supertype));
                            }

                            if (!Modifier.isPublic(method.getModifiers())) {
                                continue;
                            }

                            final Class<?>[] parameterTypes = method.getParameterTypes();

                            if (parameterTypes.length != 1) {
                                throw new RuntimeException(
                                        method.getName() + " has " + parameterTypes.length + " parameters. Subscriber method must have exactly 1 parameter.");
                            }

                            final String methodSignature = method.getName() + "#" + parameterTypes[0].getName();
                            final SubIdentifier existing = subscriberMethodMap.get(methodSignature);

                            if (existing == null || existing.method.getDeclaringClass().isAssignableFrom(method.getDeclaringClass())) {
                                subscriberMethodMap.put(methodSignature, new SubIdentifier(method));
                            }
                        }
                    }
                }

                if (Subscriber.class.isAssignableFrom(subscriberClass)) {
                    Method subscriberMethod = null;

                    for (final Method method : subscriberClass.getMethods()) {
                        if (method.getName().equals("on") && method.getParameterTypes().length == 1 && !Modifier.isStatic(method.getModifiers())) {
                            if (!method.isBridge() && !method.isSynthetic()) {
                                subscriberMethod = method;
                                break;
                            }

                            if (subscriberMethod == null) {
                                subscriberMethod = method;
                            }
                        }
                    }

                    if (subscriberMethod != null) {
                        subscriberMethodMap.putIfAbsent(subscriberMethod.getName() + "#" + subscriberMethod.getParameterTypes()[0].getName(),
                                new SubIdentifier(subscriberMethod));
                    }
                }

                subscriberMethods = new ArrayList<>(subscriberMethodMap.values());
                classMetaSubMap.put(subscriberClass, subscriberMethods);
            }

            return subscriberMethods;
        }
    }

    /**
     * Registers a {@link Subscriber} interface implementation with a specific event ID.
     * This method is specifically designed for lambda expressions and anonymous inner classes
     * that implement the {@link Subscriber} interface.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register((Subscriber<String>) event -> {
     *     System.out.println("Lambda received: " + event);
     * }, "stringEvents");
     * }</pre>
     *
     * @param subscriber the {@code Subscriber} implementation to register
     * @param eventId the event ID to filter events; must be non-empty when the subscriber is identified as a lambda subscriber
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if no subscriber methods are found
     * @throws IllegalStateException if the subscriber is identified as a lambda subscriber and {@code eventId} is empty or {@code null}
     */
    public EventBus register(final Subscriber<?> subscriber, final String eventId) {
        return register(subscriber, eventId, null);
    }

    /**
     * Registers a {@link Subscriber} interface implementation with both event ID and thread mode.
     * This method provides full control over how lambda-based subscribers receive events.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register((Subscriber<User>) user -> {
     *     // Process user on background thread
     *     processUser(user);
     * }, "userUpdates", ThreadMode.THREAD_POOL_EXECUTOR);
     * }</pre>
     *
     * @param subscriber the {@code Subscriber} implementation to register
     * @param eventId the event ID to filter events; must be non-empty when the subscriber is identified as a lambda subscriber
     * @param threadMode the thread mode override for event delivery, or {@code null} to use the thread mode declared in each subscriber method's {@link Subscribe} annotation
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if the thread mode is not supported or no subscriber methods are found
     * @throws IllegalStateException if the subscriber is identified as a lambda subscriber and {@code eventId} is empty or {@code null}
     */
    public EventBus register(final Subscriber<?> subscriber, final String eventId, final ThreadMode threadMode) {
        final Object tmp = subscriber;
        return register(tmp, eventId, threadMode);
    }

    /**
     * Unregisters a previously registered subscriber.
     * All event subscriptions for this subscriber will be removed.
     * This method should be called when a subscriber is no longer needed to prevent memory leaks.
     * If the subscriber was not previously registered, this method does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.unregister(mySubscriber);
     * // mySubscriber will no longer receive any events
     * }</pre>
     *
     * @param subscriber the subscriber to unregister
     * @return this {@code EventBus} instance for method chaining
     */
    public EventBus unregister(final Object subscriber) {
        if (logger.isDebugEnabled()) {
            logger.debug("Unregistering subscriber: {}", subscriber);
        }

        Set<SubIdentifier> subEvents = null;

        synchronized (registeredSubMap) {
            final List<SubIdentifier> removed = registeredSubMap.remove(subscriber);
            subEvents = removed == null ? null : N.newHashSet(removed);
            listOfSubEventSubs = null;
        }

        if (N.notEmpty(subEvents)) {
            removeFromEventIdSubMap(subEvents);
        }

        return this;
    }

    /**
     * Removes the given subscriber identifiers from the event-id index ({@code registeredEventIdSubMap})
     * and invalidates the corresponding cached snapshots in {@code listOfEventIdSubMap}. Any event ID
     * whose subscriber set becomes empty as a result is removed from the index entirely.
     *
     * @param subEventsToRemove the subscriber identifiers to remove; must be non-empty
     */
    private void removeFromEventIdSubMap(final Set<SubIdentifier> subEventsToRemove) {
        synchronized (registeredEventIdSubMap) {
            final List<String> keyToRemove = new ArrayList<>();

            for (final Map.Entry<String, Set<SubIdentifier>> entry : registeredEventIdSubMap.entrySet()) {
                entry.getValue().removeAll(subEventsToRemove);

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

    /**
     * Posts an event to subscribers that have no event ID filter.
     * The event will be delivered to all subscribers whose parameter type is assignable from the event's type
     * and who are registered without a specific event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.post("Hello World");
     * eventBus.post(new UserLoginEvent(userId));
     * }</pre>
     *
     * @param event the event to post
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    public EventBus post(final Object event) {
        return post((String) null, event);
    }

    /**
     * Posts an event with an optional event ID.
     * When {@code eventId} is non-null and non-empty, the event will only be delivered to subscribers
     * associated with that event ID, including those registered with that ID directly and those whose
     * {@link Subscribe} annotation specifies that event ID.
     * When {@code eventId} is {@code null} or empty, the event is delivered to all subscribers that
     * have no event ID filter (equivalent to calling {@link #post(Object)}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.post("userEvents", new UserLoginEvent(userId));
     * // Only subscribers associated with "userEvents" will receive this
     * }</pre>
     *
     * @param eventId the event ID for filtering subscribers, or {@code null} (or empty) to deliver to subscribers without a specific event ID
     * @param event the event to post
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    public EventBus post(final String eventId, final Object event) {
        N.checkArgNotNull(event, cs.event);

        final String normalizedEventId = Strings.isEmpty(eventId) ? null : eventId;

        dispatchToSubscribers(resolveSubscriberLists(normalizedEventId), normalizedEventId, event);

        return this;
    }

    /**
     * Resolves (and lazily caches) the list of candidate subscriber lists for the given normalized
     * event ID. This is the snapshot/decision step of a post; it acquires the relevant map lock briefly
     * but performs no dispatch. {@link #postSticky} calls this while holding {@link #stickyDeliveryLock}
     * so that its delivery decision is ordered against a concurrent {@link #register}.
     */
    private List<List<SubIdentifier>> resolveSubscriberLists(final String normalizedEventId) {
        List<List<SubIdentifier>> subscriberLists = listOfSubEventSubs;

        if (normalizedEventId == null) {
            if (subscriberLists == null) {
                synchronized (registeredSubMap) {
                    subscriberLists = listOfSubEventSubs; // re-read inside lock to avoid double initialisation
                    if (subscriberLists == null) {
                        subscriberLists = new ArrayList<>(registeredSubMap.values()); // in case concurrent register/unregister.
                        listOfSubEventSubs = subscriberLists;
                    }
                }
            }
        } else {
            List<SubIdentifier> eventIdSubscribers = listOfEventIdSubMap.get(normalizedEventId);

            if (eventIdSubscribers == null) {
                synchronized (registeredEventIdSubMap) {
                    eventIdSubscribers = listOfEventIdSubMap.get(normalizedEventId); // re-read inside lock to avoid double initialisation
                    if (eventIdSubscribers == null) {
                        if (registeredEventIdSubMap.containsKey(normalizedEventId)) {
                            eventIdSubscribers = new ArrayList<>(registeredEventIdSubMap.get(normalizedEventId)); // in case concurrent register/unregister.
                        } else {
                            eventIdSubscribers = N.emptyList();
                        }

                        listOfEventIdSubMap.put(normalizedEventId, eventIdSubscribers);
                    }
                }
            }

            subscriberLists = Collections.singletonList(eventIdSubscribers);
        }

        return subscriberLists;
    }

    /**
     * Dispatches {@code event} to every matching subscriber in the previously-resolved lists. This is
     * always called OUTSIDE {@link #stickyDeliveryLock} because {@code dispatch(...)} may execute
     * subscriber code synchronously on the calling thread.
     */
    private void dispatchToSubscribers(final List<List<SubIdentifier>> subscriberLists, final String normalizedEventId, final Object event) {
        final Class<?> eventClass = event.getClass();

        for (final List<SubIdentifier> subscribers : subscriberLists) {
            for (final SubIdentifier subscriber : subscribers) {
                if (subscriber.isMyEvent(normalizedEventId, eventClass)) {
                    try {
                        dispatch(subscriber, event);
                    } catch (final Exception e) {
                        logger.error("Failed to post event: " + N.toString(event) + " to subscriber: " + N.toString(subscriber), e);
                    }
                }
            }
        }
    }

    /**
     * Posts a sticky event that will be retained and delivered to future subscribers.
     * The sticky event will be immediately delivered to current subscribers and also
     * to any subscribers that register later with {@code sticky=true} in their {@link Subscribe} annotation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.postSticky(new AppConfiguration());
     * // Later registered subscribers with sticky=true will receive this event
     * }</pre>
     *
     * @param event the sticky event to post
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    public EventBus postSticky(final Object event) {
        return postSticky(null, event);
    }

    /**
     * Posts a sticky event with a specific event ID.
     * The sticky event will be immediately delivered to current matching subscribers and retained for delivery to
     * matching subscribers that register later with {@code sticky=true} in their {@link Subscribe} annotation.
     * Only one sticky event per event object is retained - posting the same object again will update its event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Configuration config = new Configuration();
     * eventBus.postSticky("appConfig", config);
     * // Future subscribers for "appConfig" with sticky=true will receive this
     * }</pre>
     *
     * @param eventId the event ID to associate with the sticky event, or {@code null} (or empty) to retain it without a specific event ID
     * @param event the sticky event to post
     * @return this {@code EventBus} instance for method chaining
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    public EventBus postSticky(final String eventId, final Object event) {
        N.checkArgNotNull(event, cs.event);

        final String normalizedEventId = Strings.isEmpty(eventId) ? null : eventId;

        final List<List<SubIdentifier>> subscriberLists;

        // Record the sticky event AND resolve its delivery list atomically under stickyDeliveryLock so
        // that, relative to a concurrent register(...), this event is delivered to a newly-registering
        // sticky subscriber exactly once: either here (subscriber already visible) or by register's
        // replay (event already recorded), never both. dispatch happens after the lock is released.
        synchronized (stickyDeliveryLock) {
            synchronized (stickyEventMap) {
                stickyEventMap.put(event, normalizedEventId);

                mapOfStickyEvent = null;
            }

            subscriberLists = resolveSubscriberLists(normalizedEventId);
        }

        dispatchToSubscribers(subscriberLists, normalizedEventId, event);

        return this;
    }

    /**
     * Removes a sticky event that was posted without an event ID.
     * The event will no longer be delivered to future subscribers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Configuration config = getConfiguration();
     * boolean removed = eventBus.removeStickyEvent(config);
     * }</pre>
     *
     * @param event the sticky event to remove
     * @return {@code true} if the event was removed, {@code false} otherwise
     */
    public boolean removeStickyEvent(final Object event) {
        return removeStickyEvent(null, event);
    }

    /**
     * Removes a sticky event that was posted with a specific event ID.
     * The event will only be removed if both the event object and event ID match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Configuration config = getConfiguration();
     * boolean removed = eventBus.removeStickyEvent("appConfig", config);
     * }</pre>
     *
     * @param eventId the event ID the sticky event was posted with
     * @param event the sticky event to remove
     * @return {@code true} if the event was removed, {@code false} otherwise
     */
    public boolean removeStickyEvent(final String eventId, final Object event) {
        final String normalizedEventId = Strings.isEmpty(eventId) ? null : eventId;

        synchronized (stickyEventMap) {
            final String val = stickyEventMap.get(event);
            final String normalizedStoredEventId = Strings.isEmpty(val) ? null : val;

            if (N.equals(normalizedStoredEventId, normalizedEventId) && (val != null || stickyEventMap.containsKey(event))) {
                stickyEventMap.remove(event);

                mapOfStickyEvent = null;
                return true;
            }
        }

        return false;
    }

    /**
     * Removes all sticky events of a specific type that were posted without an event ID.
     * This is useful for clearing all sticky events of a particular class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean removed = eventBus.removeStickyEvents(UserSession.class);
     * // All UserSession sticky events with null event ID are removed
     * }</pre>
     *
     * @param eventType the class type of sticky events to remove
     * @return {@code true} if one or more events were removed, {@code false} otherwise
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     */
    public boolean removeStickyEvents(final Class<?> eventType) {
        return removeStickyEvents(null, eventType);
    }

    /**
     * Removes all sticky events of a specific type that were posted with the specified event ID.
     * This allows targeted removal of sticky events by both type and ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean removed = eventBus.removeStickyEvents("userSessions", UserSession.class);
     * // All UserSession sticky events with "userSessions" ID are removed
     * }</pre>
     *
     * @param eventId the event ID to match, or {@code null} for events without ID
     * @param eventType the class type of sticky events to remove
     * @return {@code true} if one or more events were removed, {@code false} otherwise
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     */
    public boolean removeStickyEvents(final String eventId, final Class<?> eventType) {
        N.checkArgNotNull(eventType, "eventType");

        final String normalizedEventId = Strings.isEmpty(eventId) ? null : eventId;
        final List<Object> keyToRemove = new ArrayList<>();

        synchronized (stickyEventMap) {
            for (final Map.Entry<Object, String> entry : stickyEventMap.entrySet()) {
                if (N.equals(Strings.isEmpty(entry.getValue()) ? null : entry.getValue(), normalizedEventId)
                        && eventType.isAssignableFrom(entry.getKey().getClass())) {
                    keyToRemove.add(entry.getKey());
                }
            }

            if (N.notEmpty(keyToRemove)) {
                for (final Object event : keyToRemove) {
                    stickyEventMap.remove(event);
                }

                mapOfStickyEvent = null;

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all sticky events from this {@code EventBus}.
     * After calling this method, no sticky events will be delivered to future subscribers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean removed = eventBus.removeAllStickyEvents();
     * // All sticky events are cleared
     * }</pre>
     *
     * @return {@code true} if one or more sticky events were removed, {@code false} if there were
     *         no sticky events to remove. The return type mirrors {@link #removeStickyEvent(Object)}
     *         and {@link #removeStickyEvents(Class)} for family consistency.
     */
    public boolean removeAllStickyEvents() {
        synchronized (stickyEventMap) {
            if (stickyEventMap.isEmpty()) {
                return false;
            }

            stickyEventMap.clear();

            mapOfStickyEvent = null;

            return true;
        }
    }

    /**
     * Returns all sticky events of a specific type that were posted without an event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Configuration> configs = eventBus.stickyEvents(Configuration.class);
     * for (Configuration config : configs) {
     *     processConfig(config);
     * }
     * }</pre>
     *
     * @param <T> the event type
     * @param eventType the class type to search for
     * @return a list of sticky events that can be assigned to the specified type
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     */
    public <T> List<T> stickyEvents(final Class<T> eventType) {
        return stickyEvents(null, eventType);
    }

    /**
     * Returns all sticky events of a specific type that were posted with the specified event ID.
     * This allows retrieval of sticky events filtered by both type and ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Configuration> userConfigs = eventBus.stickyEvents("userConfig", Configuration.class);
     * }</pre>
     *
     * @param <T> the event type
     * @param eventId the event ID to match, or {@code null} for events without ID
     * @param eventType the class type to search for
     * @return a list of sticky events matching both the type and event ID
     * @throws IllegalArgumentException if {@code eventType} is {@code null}
     */
    public <T> List<T> stickyEvents(final String eventId, final Class<T> eventType) {
        N.checkArgNotNull(eventType, "eventType");

        final String normalizedEventId = Strings.isEmpty(eventId) ? null : eventId;
        final List<T> result = new ArrayList<>();

        synchronized (stickyEventMap) {
            for (final Map.Entry<Object, String> entry : stickyEventMap.entrySet()) {
                if (N.equals(Strings.isEmpty(entry.getValue()) ? null : entry.getValue(), normalizedEventId)
                        && eventType.isAssignableFrom(entry.getKey().getClass())) {
                    result.add(eventType.cast(entry.getKey()));
                }
            }
        }

        return result;
    }

    /**
     * Checks if the specified thread mode is supported by this {@code EventBus}.
     * Currently supports {@code DEFAULT} and {@code THREAD_POOL_EXECUTOR} modes.
     *
     * <p><b>Supported Thread Modes:</b></p>
     * <ul>
     *   <li>{@code null} or {@link ThreadMode#DEFAULT} - Events are delivered on the posting thread</li>
     *   <li>{@link ThreadMode#THREAD_POOL_EXECUTOR} - Events are delivered asynchronously on a background thread pool</li>
     * </ul>
     *
     * @param threadMode the thread mode to check, may be {@code null}
     * @return {@code true} if the thread mode is supported, {@code false} otherwise
     */
    protected boolean isSupportedThreadMode(final ThreadMode threadMode) {
        return threadMode == null || threadMode == ThreadMode.DEFAULT || threadMode == ThreadMode.THREAD_POOL_EXECUTOR;
    }

    /**
     * Dispatches an event to a subscriber according to its thread mode configuration.
     * This method determines whether to deliver the event synchronously or asynchronously
     * based on the thread mode specified in the subscriber identifier.
     *
     * <p><b>Dispatch Behavior:</b></p>
     * <ul>
     *   <li>{@link ThreadMode#DEFAULT} - Event is delivered synchronously on the calling thread</li>
     *   <li>{@link ThreadMode#THREAD_POOL_EXECUTOR} - Event is delivered asynchronously on a background thread from the executor pool</li>
     * </ul>
     *
     * @param identifier the subscriber identifier containing delivery configuration including thread mode,
     *                   subscriber instance, and method to invoke
     * @param event the event object to dispatch to the subscriber
     * @throws IllegalArgumentException if the thread mode is not one of the supported values
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
                throw new IllegalArgumentException("Unsupported thread mode");
        }
    }

    /**
     * Delivers an event to a specific subscriber method.
     * This method handles interval filtering, deduplication, and actual method invocation
     * by calling the subscriber's method via reflection.
     *
     * <p><b>Thread Safety:</b> This method is thread-safe. When interval filtering or deduplication
     * is enabled, synchronization is performed on the {@code SubIdentifier} instance to ensure thread-safe
     * access to the last post time and previous event tracking.</p>
     *
     * <p><b>Event Filtering:</b></p>
     * <ul>
     *   <li><b>Interval Filtering:</b> If {@code intervalMillis} is set on the subscriber, events posted
     *       within the specified interval (in milliseconds) will be ignored. This is useful for
     *       throttling high-frequency events.</li>
     *   <li><b>Deduplication:</b> If {@code deduplicate} is enabled, consecutive duplicate events
     *       (determined by {@code equals()}) will be ignored. This prevents redundant processing
     *       of unchanged data.</li>
     * </ul>
     *
     * @param sub the subscriber identifier containing the method to invoke, subscriber instance,
     *            and filtering configuration (interval, deduplicate)
     * @param event the event object to deliver to the subscriber
     */
    protected void post(final SubIdentifier sub, final Object event) {
        try {
            if (sub.intervalMillis > 0 || sub.deduplicate) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (sub) { //NOSONAR
                    if (sub.intervalMillis > 0 && System.currentTimeMillis() - sub.lastPostTime < sub.intervalMillis) {
                        // ignore.
                        if (logger.isDebugEnabled()) {
                            logger.debug("Ignoring event: {} to subscriber: {} because it is within the interval: {}", N.toString(event), N.toString(sub),
                                    sub.intervalMillis);
                        }
                    } else if (sub.deduplicate && sub.previousEvent != null && N.equals(sub.previousEvent, event)) {
                        // ignore.
                        if (logger.isDebugEnabled()) {
                            logger.debug("Ignoring event: {} to subscriber: {} (duplicate of previous event)", N.toString(event), N.toString(sub));
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Posting event: {} to subscriber: {}", N.toString(event), N.toString(sub));
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
                    logger.debug("Posting event: {} to subscriber: {}", N.toString(event), N.toString(sub));
                }

                sub.method.invoke(sub.instance, event);
            }
        } catch (final Exception e) {
            logger.error("Failed to post event: " + N.toString(event) + " to subscriber: " + N.toString(sub), e);
        }
    }

    /**
     * Internal class representing a subscriber method and its configuration.
     * This class holds all the metadata needed to deliver events to a subscriber,
     * including the method reference, parameter type, thread mode, filtering options,
     * and runtime state such as the last post time and previous event for deduplication.
     */
    protected static final class SubIdentifier {

        /** Cache for type-compatibility checks to avoid repeated {@code isAssignableFrom} calls. */
        final Map<Class<?>, Boolean> cachedClasses = new ConcurrentHashMap<>();

        /** The subscriber object instance on which the method will be invoked. {@code null} for prototype entries. */
        final Object instance;

        /** The subscriber method to invoke when a matching event is posted. */
        final Method method;

        /** The event parameter type accepted by the subscriber method (primitives are auto-boxed). */
        final Class<?> parameterType;

        /** The event ID filter; {@code null} means the subscriber accepts events posted without an ID. */
        final String eventId;

        /** The thread mode that controls on which thread the event is delivered. */
        final ThreadMode threadMode;

        /** If {@code true}, only events of the exact {@link #parameterType} are accepted; subtypes are excluded. */
        final boolean strictEventType;

        /**
         * If {@code true}, the subscriber should receive every retained matching sticky event
         * immediately upon registration.
         */
        final boolean sticky;

        /** Minimum interval in milliseconds between consecutive event deliveries; {@code 0} disables throttling. */
        final long intervalMillis;

        /**
         * If {@code true}, consecutive duplicate events (as determined by {@code equals()}) are ignored
         * and not delivered to the subscriber.
         */
        final boolean deduplicate;

        /**
         * {@code true} if this subscriber is likely a lambda or anonymous implementation of
         * {@link Subscriber} whose erased parameter type is {@code Object}, which requires an
         * explicit event ID to distinguish events.
         */
        final boolean isPossibleLambdaSubscriber;

        /** The system time (in milliseconds) when the last event was delivered to this subscriber. */
        long lastPostTime = 0;

        /** The most recently delivered event, used for deduplication when {@link #deduplicate} is {@code true}. */
        Object previousEvent = null;

        /**
         * Constructs a prototype {@code SubIdentifier} from a subscriber method.
         * The resulting instance has no subscriber object ({@link #instance} is {@code null}) and
         * is used as a template to create bound instances via
         * {@link #SubIdentifier(SubIdentifier, Object, String, ThreadMode)}.
         *
         * @param method the subscriber method annotated with {@link Subscribe}, or the {@code on} method
         *               of a {@link Subscriber} implementation
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
            intervalMillis = subscribe == null ? 0 : subscribe.intervalMillis();
            deduplicate = subscribe != null && subscribe.deduplicate();

            isPossibleLambdaSubscriber = Subscriber.class.isAssignableFrom(method.getDeclaringClass()) && method.getName().equals("on")
                    && parameterType.equals(Object.class) && subscribe == null;

            ClassUtil.setAccessible(method, true);
        }

        /**
         * Constructs a bound {@code SubIdentifier} by combining the metadata from a prototype entry
         * with a concrete subscriber instance and registration-time overrides for the event ID and thread mode.
         *
         * @param sub       the prototype {@code SubIdentifier} carrying method-level defaults
         * @param obj       the subscriber instance that will receive events
         * @param eventId   the registration-level event ID override; if empty or {@code null}, the method-level ID is used
         * @param threadMode the registration-level thread mode override; if {@code null}, the method-level mode is used
         */
        SubIdentifier(final SubIdentifier sub, final Object obj, final String eventId, final ThreadMode threadMode) {
            instance = obj;
            method = sub.method;
            parameterType = sub.parameterType;
            this.eventId = Strings.isEmpty(eventId) ? sub.eventId : eventId;
            this.threadMode = threadMode == null ? sub.threadMode : threadMode;
            strictEventType = sub.strictEventType;
            sticky = sub.sticky;
            intervalMillis = sub.intervalMillis;
            deduplicate = sub.deduplicate;
            isPossibleLambdaSubscriber = sub.isPossibleLambdaSubscriber;
        }

        /**
         * Determines whether this subscriber should receive an event of the specified type and event ID.
         * The event ID must match exactly (both {@code null} or both equal), and the event type must be
         * compatible with this subscriber's {@link #parameterType}, respecting the {@link #strictEventType} flag.
         *
         * @param eventId   the event ID of the posted event, or {@code null} if posted without an ID
         * @param eventType the runtime type of the posted event
         * @return {@code true} if this subscriber is eligible to receive the event
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
            h = 31 * h + System.identityHashCode(instance);
            h = 31 * h + N.hashCode(method);
            h = 31 * h + N.hashCode(parameterType);
            h = 31 * h + N.hashCode(eventId);
            h = 31 * h + N.hashCode(threadMode);
            h = 31 * h + N.hashCode(strictEventType);
            h = 31 * h + N.hashCode(sticky);
            h = 31 * h + N.hashCode(intervalMillis);
            h = 31 * h + N.hashCode(deduplicate);
            return 31 * h + N.hashCode(isPossibleLambdaSubscriber);
        }

        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof SubIdentifier other) {
                return instance == other.instance && N.equals(method, other.method) && N.equals(parameterType, other.parameterType)
                        && N.equals(eventId, other.eventId) && N.equals(threadMode, other.threadMode) && N.equals(strictEventType, other.strictEventType)
                        && N.equals(sticky, other.sticky) && N.equals(intervalMillis, other.intervalMillis) && N.equals(deduplicate, other.deduplicate)
                        && N.equals(isPossibleLambdaSubscriber, other.isPossibleLambdaSubscriber);
            }

            return false;
        }

        @Override
        public String toString() {
            return "{obj=" + N.toString(instance) + ", method=" + N.toString(method) + ", parameterType=" + N.toString(parameterType) + ", eventId="
                    + N.toString(eventId) + ", threadMode=" + N.toString(threadMode) + ", strictEventType=" + N.toString(strictEventType) + ", sticky="
                    + N.toString(sticky) + ", interval=" + N.toString(intervalMillis) + ", deduplicate=" + N.toString(deduplicate)
                    + ", isPossibleLambdaSubscriber=" + N.toString(isPossibleLambdaSubscriber) + "}";
        }

    }
}
