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

// DO NOT try to move me out of this project. Somebody tried and gave up then. I'm small. I'm stay here.

/**
 * EventBus is a publish-subscribe event bus that simplifies communication between components.
 * It allows components to communicate with each other without requiring them to have explicit references to one another,
 * thus promoting loose coupling.
 * 
 * <p>The EventBus supports the following features:</p>
 * <ul>
 *   <li>Event posting to subscribers based on event type hierarchy</li>
 *   <li>Sticky events that persist and are delivered to new subscribers</li>
 *   <li>Thread mode control for event delivery</li>
 *   <li>Event filtering by event ID</li>
 *   <li>Interval-based and deduplication filtering</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a subscriber using the Subscriber interface
 * final Subscriber<String> strSubscriber = new Subscriber<String>() {
 *     @Override
 *     public void on(String event) {
 *         System.out.println("Received: " + event);
 *     }
 * };
 *
 * // Create a subscriber using @Subscribe annotation
 * final Object annotatedSubscriber = new Object() {
 *     @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
 *     public void handleEvent(String event) {
 *         System.out.println("Handled: " + event);
 *     }
 * };
 *
 * // Register subscribers
 * EventBus eventBus = EventBus.getDefault();
 * eventBus.register(strSubscriber, "myEventId");
 * eventBus.register(annotatedSubscriber);
 *
 * // Post events
 * eventBus.post("Hello World");
 * eventBus.post("myEventId", "Targeted Event");
 *
 * // Post sticky event
 * eventBus.postSticky("Sticky Message");
 *
 * // Unregister when done
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

    private final Map<Object, List<SubIdentifier>> registeredSubMap = new LinkedHashMap<>();

    private final Map<String, Set<SubIdentifier>> registeredEventIdSubMap = new HashMap<>();

    private final Map<Object, String> stickyEventMap = new IdentityHashMap<>();

    private final String identifier;

    private final Executor executor;

    private final Map<String, List<SubIdentifier>> listOfEventIdSubMap = new ConcurrentHashMap<>();

    private List<List<SubIdentifier>> listOfSubEventSubs = null;

    private Map<Object, String> mapOfStickyEvent = null;

    private static final EventBus INSTANCE = new EventBus("default");

    /**
     * Creates a new EventBus instance with a randomly generated identifier.
     * This EventBus will use the default executor for asynchronous event delivery.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = new EventBus();
     * }</pre>
     */
    @SuppressFBWarnings("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR")
    public EventBus() {
        this(Strings.guid());
    }

    /**
     * Creates a new EventBus instance with the specified identifier.
     * This EventBus will use the default executor for asynchronous event delivery.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = new EventBus("myEventBus");
     * }</pre>
     *
     * @param identifier the unique identifier for this EventBus instance
     */
    public EventBus(final String identifier) {
        this(identifier, DEFAULT_EXECUTOR);
    }

    /**
     * Creates a new EventBus instance with the specified identifier and executor.
     * The executor is used for asynchronous event delivery when ThreadMode.THREAD_POOL_EXECUTOR is specified.
     * If the executor is an ExecutorService, a shutdown hook will be registered to properly shutdown the executor.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * EventBus eventBus = new EventBus("myEventBus", executor);
     * }</pre>
     *
     * @param identifier the unique identifier for this EventBus instance
     * @param executor the executor to use for asynchronous event delivery, or {@code null} to use the default executor
     */
    public EventBus(final String identifier, final Executor executor) {
        this.identifier = identifier;
        this.executor = executor == null ? DEFAULT_EXECUTOR : executor;

        if (executor != DEFAULT_EXECUTOR && executor instanceof ExecutorService executorService) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

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
     * Returns the default EventBus instance.
     * This is a singleton instance with identifier "default" that can be used throughout the application.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = EventBus.getDefault();
     * eventBus.post("Hello World");
     * }</pre>
     *
     * @return the default EventBus instance
     */
    @SuppressFBWarnings("MS_EXPOSE_REP")
    public static EventBus getDefault() {
        return INSTANCE;
    }

    /**
     * Returns the unique identifier of this EventBus instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EventBus eventBus = new EventBus("myBus");
     * String id = eventBus.identifier();  // Returns "myBus"
     * }</pre>
     *
     * @return the identifier of this EventBus
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Returns all subscribers that are registered to receive events of the specified type or its subtypes.
     * This method searches for subscribers registered with {@code null} event ID.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> stringSubscribers = eventBus.getSubscribers(String.class);
     * }</pre>
     *
     * @param eventType the event type to search for
     * @return a list of subscribers registered for the specified event type
     */
    public List<Object> getSubscribers(final Class<?> eventType) {
        return getSubscribers(null, eventType);
    }

    /**
     * Returns all subscribers that are registered to receive events of the specified type and event ID.
     * The method checks both the event type hierarchy and the event ID when searching for subscribers.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> subscribers = eventBus.getSubscribers("userEvents", User.class);
     * }</pre>
     *
     * @param eventId the event ID to match, or {@code null} for subscribers without event ID
     * @param eventType the event type to search for
     * @return a list of subscribers registered for the specified event type and ID
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
     * Returns all currently registered subscribers in this EventBus.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> allSubscribers = eventBus.getAllSubscribers();
     * System.out.println("Total subscribers: " + allSubscribers.size());
     * }</pre>
     *
     * @return a list of all registered subscribers
     */
    public List<Object> getAllSubscribers() {
        synchronized (registeredSubMap) {
            return new ArrayList<>(registeredSubMap.keySet());
        }
    }

    /**
     * Registers a subscriber to receive events.
     * The subscriber must either implement the {@link Subscriber} interface or have methods annotated with {@link Subscribe}.
     * Events will be delivered on the default thread mode.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(new Subscriber<String>() {
     *     public void on(String event) {
     *         System.out.println("Received: " + event);
     *     }
     * });
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @return this EventBus instance for method chaining
     * @throws RuntimeException if no subscriber methods are found in the subscriber class
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
     * @param eventId the event ID to filter events
     * @return this EventBus instance for method chaining
     * @throws RuntimeException if registering a lambda subscriber without event ID
     */
    public EventBus register(final Object subscriber, final String eventId) {
        return register(subscriber, eventId, null);
    }

    /**
     * Registers a subscriber with a specific thread mode.
     * The thread mode determines on which thread the event will be delivered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(mySubscriber, ThreadMode.THREAD_POOL_EXECUTOR);
     * // Events will be delivered on a background thread
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @param threadMode the thread mode for event delivery
     * @return this EventBus instance for method chaining
     * @throws RuntimeException if the thread mode is not supported
     */
    public EventBus register(final Object subscriber, final ThreadMode threadMode) {
        return register(subscriber, null, threadMode);
    }

    /**
     * Registers a subscriber with both event ID and thread mode specifications.
     * This is the most comprehensive registration method that allows full control over event filtering and delivery.
     * If the subscriber has sticky event handling enabled, it will immediately receive any matching sticky events.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register(mySubscriber, "userEvents", ThreadMode.THREAD_POOL_EXECUTOR);
     * }</pre>
     *
     * @param subscriber the subscriber to register
     * @param eventId the event ID to filter events, or {@code null} for no filtering
     * @param threadMode the thread mode for event delivery, or {@code null} for default
     * @return this EventBus instance for method chaining
     * @throws RuntimeException if the thread mode is not supported or no subscriber methods are found
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

    /**
     * Registers a Subscriber interface implementation with a specific event ID.
     * This method is specifically designed for lambda expressions and anonymous inner classes
     * that implement the Subscriber interface.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.register((Subscriber<String>) event -> {
     *     System.out.println("Lambda received: " + event);
     * }, "stringEvents");
     * }</pre>
     *
     * @param <T> the type of events the subscriber will receive
     * @param subscriber the Subscriber implementation to register
     * @param eventId the event ID to filter events (required for lambda subscribers)
     * @return this EventBus instance for method chaining
     * @throws RuntimeException if registering a lambda subscriber without event ID or no subscriber methods are found
     */
    public <T> EventBus register(final Subscriber<T> subscriber, final String eventId) {
        return register(subscriber, eventId, null);
    }

    /**
     * Registers a Subscriber interface implementation with both event ID and thread mode.
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
     * @param <T> the type of events the subscriber will receive
     * @param subscriber the Subscriber implementation to register
     * @param eventId the event ID to filter events (required for lambda subscribers)
     * @param threadMode the thread mode for event delivery
     * @return this EventBus instance for method chaining
     * @throws RuntimeException if the thread mode is not supported, registering a lambda subscriber without event ID, or no subscriber methods are found
     */
    public <T> EventBus register(final Subscriber<T> subscriber, final String eventId, final ThreadMode threadMode) {
        final Object tmp = subscriber;
        return register(tmp, eventId, threadMode);
    }

    /**
     * Unregisters a previously registered subscriber.
     * All event subscriptions for this subscriber will be removed.
     * This method should be called when a subscriber is no longer needed to prevent memory leaks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.unregister(mySubscriber);
     * // mySubscriber will no longer receive any events
     * }</pre>
     *
     * @param subscriber the subscriber to unregister
     * @return this EventBus instance for method chaining
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
     * Posts an event to all registered subscribers.
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
     * @return this EventBus instance for method chaining
     */
    public EventBus post(final Object event) {
        return post((String) null, event);
    }

    /**
     * Posts an event with a specific event ID.
     * The event will only be delivered to subscribers registered with the same event ID,
     * or to subscribers registered without an event ID whose methods are annotated with the matching event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.post("userEvents", new UserLoginEvent(userId));
     * // Only subscribers registered with "userEvents" will receive this
     * }</pre>
     *
     * @param eventId the event ID for filtering subscribers, or {@code null} for no filtering
     * @param event the event to post
     * @return this EventBus instance for method chaining
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
            }

            listOfSubs = Collections.singletonList(listOfEventIdSub);
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
     * Posts a sticky event that will be retained and delivered to future subscribers.
     * The sticky event will be immediately delivered to current subscribers and also
     * to any subscribers that register later with sticky=true in their @Subscribe annotation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.postSticky(new AppConfiguration());
     * // Later registered subscribers with sticky=true will receive this event
     * }</pre>
     *
     * @param event the sticky event to post
     * @return this EventBus instance for method chaining
     */
    public EventBus postSticky(final Object event) {
        return postSticky(null, event);
    }

    /**
     * Posts a sticky event with a specific event ID.
     * The sticky event will be retained and delivered to matching future subscribers.
     * Only one sticky event per event object is retained - posting the same object again will update its event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Configuration config = new Configuration();
     * eventBus.postSticky("appConfig", config);
     * // Future subscribers for "appConfig" with sticky=true will receive this
     * }</pre>
     *
     * @param eventId the event ID for filtering subscribers
     * @param event the sticky event to post
     * @return this EventBus instance for method chaining
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
     * Removes a sticky event that was posted without an event ID.
     * The event will no longer be delivered to future subscribers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Configuration config = getConfiguration();
     * eventBus.removeStickyEvent(config);
     * }</pre>
     *
     * @param event the sticky event to remove
     * @return {@code true} if the event was found and removed, {@code false} otherwise
     */
    public boolean removeStickyEvent(final Object event) {
        return removeStickyEvent(event, null);
    }

    /**
     * Removes a sticky event that was posted with a specific event ID.
     * The event will only be removed if both the event object and event ID match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Configuration config = getConfiguration();
     * eventBus.removeStickyEvent(config, "appConfig");
     * }</pre>
     *
     * @param event the sticky event to remove
     * @param eventId the event ID the sticky event was posted with
     * @return {@code true} if the event was found with matching event ID and removed, {@code false} otherwise
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
     * Removes all sticky events of a specific type that were posted without an event ID.
     * This is useful for clearing all sticky events of a particular class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.removeStickyEvents(UserSession.class);
     * // All UserSession sticky events with null event ID are removed
     * }</pre>
     *
     * @param eventType the class type of sticky events to remove
     * @return {@code true} if one or more sticky events were removed, {@code false} otherwise
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
     * eventBus.removeStickyEvents("userSessions", UserSession.class);
     * // All UserSession sticky events with "userSessions" ID are removed
     * }</pre>
     *
     * @param eventId the event ID to match, or {@code null} for events without ID
     * @param eventType the class type of sticky events to remove
     * @return {@code true} if one or more sticky events were removed, {@code false} otherwise
     */
    public boolean removeStickyEvents(final String eventId, final Class<?> eventType) {
        final List<Object> keyToRemove = new ArrayList<>();

        synchronized (stickyEventMap) {
            for (final Map.Entry<Object, String> entry : stickyEventMap.entrySet()) {
                if (N.equals(entry.getValue(), eventId) && eventType.isAssignableFrom(entry.getKey().getClass())) {
                    keyToRemove.add(entry.getKey());
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
     * Removes all sticky events from this EventBus.
     * After calling this method, no sticky events will be delivered to future subscribers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventBus.removeAllStickyEvents();
     * // All sticky events are cleared
     * }</pre>
     */
    public void removeAllStickyEvents() {
        synchronized (stickyEventMap) {
            stickyEventMap.clear();

            mapOfStickyEvent = null;
        }
    }

    /**
     * Returns all sticky events of a specific type that were posted without an event ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> configs = eventBus.getStickyEvents(Configuration.class);
     * for (Object config : configs) {
     *     processConfig((Configuration) config);
     * }
     * }</pre>
     *
     * @param eventType the class type to search for
     * @return a list of sticky events that can be assigned to the specified type
     */
    public List<Object> getStickyEvents(final Class<?> eventType) {
        return getStickyEvents(null, eventType);
    }

    /**
     * Returns all sticky events of a specific type that were posted with the specified event ID.
     * This allows retrieval of sticky events filtered by both type and ID.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> userConfigs = eventBus.getStickyEvents("userConfig", Configuration.class);
     * }</pre>
     *
     * @param eventId the event ID to match, or {@code null} for events without ID
     * @param eventType the class type to search for
     * @return a list of sticky events matching both the type and event ID
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
     * Checks if the specified thread mode is supported by this EventBus.
     * Currently supports DEFAULT and THREAD_POOL_EXECUTOR modes.
     *
     * <p>This method is designed to be overridden by subclasses that want to support additional
     * thread modes. When extending EventBus to add custom thread modes, override this method to
     * return {@code true} for your custom modes, and override {@link #dispatch(SubIdentifier, Object)}
     * to handle the custom thread mode dispatch logic.</p>
     *
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called concurrently from multiple threads.</p>
     *
     * <p><b>Supported Thread Modes:</b></p>
     * <ul>
     *   <li>{@code null} or {@link ThreadMode#DEFAULT} - Events are delivered on the posting thread</li>
     *   <li>{@link ThreadMode#THREAD_POOL_EXECUTOR} - Events are delivered asynchronously on a background thread pool</li>
     * </ul>
     *
     * <p><b>Extension Example:</b></p>
     * <pre>{@code
     * public class CustomEventBus extends EventBus {
     *     @Override
     *     protected boolean isSupportedThreadMode(ThreadMode threadMode) {
     *         if (threadMode == ThreadMode.MAIN_THREAD) {
     *             return true;  // Add support for main thread delivery
     *         }
     *         return super.isSupportedThreadMode(threadMode);
     *     }
     *
     *     @Override
     *     protected void dispatch(SubIdentifier identifier, Object event) {
     *         if (identifier.threadMode == ThreadMode.MAIN_THREAD) {
     *             mainThreadExecutor.execute(() -> post(identifier, event));
     *             return;
     *         }
     *         super.dispatch(identifier, event);
     *     }
     * }
     * }</pre>
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
     * <p>This method is designed to be overridden by subclasses that want to support additional
     * thread modes. When extending EventBus, override this method to add custom dispatch logic
     * for your custom thread modes, and remember to also override {@link #isSupportedThreadMode(ThreadMode)}
     * to indicate support for those modes.</p>
     *
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called concurrently from multiple threads.
     * However, the actual thread on which the event is delivered depends on the thread mode configuration.</p>
     *
     * <p><b>Default Dispatch Behavior:</b></p>
     * <ul>
     *   <li>{@link ThreadMode#DEFAULT} - Event is delivered synchronously on the calling thread</li>
     *   <li>{@link ThreadMode#THREAD_POOL_EXECUTOR} - Event is delivered asynchronously on a background thread from the executor pool</li>
     * </ul>
     *
     * <p><b>Extension Example:</b></p>
     * <pre>{@code
     * public class CustomEventBus extends EventBus {
     *     private final Handler mainHandler = new Handler(Looper.getMainLooper());
     *
     *     @Override
     *     protected void dispatch(SubIdentifier identifier, Object event) {
     *         if (identifier.threadMode == ThreadMode.MAIN_THREAD) {
     *             mainHandler.post(() -> post(identifier, event));
     *             return;
     *         }
     *         super.dispatch(identifier, event);
     *     }
     * }
     * }</pre>
     *
     * @param identifier the subscriber identifier containing delivery configuration including thread mode,
     *                   subscriber instance, and method to invoke
     * @param event the event object to dispatch to the subscriber
     * @throws RuntimeException if the thread mode is not supported (should not happen if isSupportedThreadMode is properly implemented)
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
     * Delivers an event to a specific subscriber method.
     * This method handles interval filtering, deduplication, and actual method invocation
     * by calling the subscriber's method via reflection.
     *
     * <p>This method is designed to be overridden by subclasses that want to customize
     * event delivery behavior, add pre/post-processing hooks, or modify filtering logic.
     * When overriding, you can add custom validation, logging, or transformation of events
     * before they reach the subscriber.</p>
     *
     * <p><b>Thread Safety:</b> This method is thread-safe. When interval filtering or deduplication
     * is enabled, synchronization is performed on the SubIdentifier instance to ensure thread-safe
     * access to the last post time and previous event tracking.</p>
     *
     * <p><b>Event Filtering:</b></p>
     * <ul>
     *   <li><b>Interval Filtering:</b> If {@code interval} is set on the subscriber, events posted
     *       within the specified interval (in milliseconds) will be ignored. This is useful for
     *       throttling high-frequency events.</li>
     *   <li><b>Deduplication:</b> If {@code deduplicate} is enabled, consecutive duplicate events
     *       (determined by {@code equals()}) will be ignored. This prevents redundant processing
     *       of unchanged data.</li>
     * </ul>
     *
     * <p><b>Extension Example:</b></p>
     * <pre>{@code
     * public class LoggingEventBus extends EventBus {
     *     @Override
     *     protected void post(SubIdentifier sub, Object event) {
     *         logger.info("Delivering event {} to {}", event, sub.method.getName());
     *         long startTime = System.currentTimeMillis();
     *         try {
     *             super.post(sub, event);
     *         } finally {
     *             long duration = System.currentTimeMillis() - startTime;
     *             logger.info("Event delivery took {}ms", duration);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param sub the subscriber identifier containing the method to invoke, subscriber instance,
     *            and filtering configuration (interval, deduplicate)
     * @param event the event object to deliver to the subscriber
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
     * Internal class representing a subscriber method and its configuration.
     * This class holds all the metadata needed to deliver events to a subscriber.
     */
    protected static final class SubIdentifier {

        final Map<Class<?>, Boolean> cachedClasses = new ConcurrentHashMap<>();

        final Object instance;

        final Method method;

        final Class<?> parameterType;

        final String eventId;

        final ThreadMode threadMode;

        final boolean strictEventType;

        final boolean sticky;

        final long intervalInMillis;

        final boolean deduplicate;

        final boolean isPossibleLambdaSubscriber;

        long lastPostTime = 0;

        Object previousEvent = null;

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

    }
}
