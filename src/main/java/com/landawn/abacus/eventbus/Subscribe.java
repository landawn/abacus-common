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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.landawn.abacus.util.ThreadMode;

/**
 * Annotation to mark methods as event subscribers in the EventBus system.
 * Methods annotated with @Subscribe will receive events when registered with an {@link EventBus}.
 * 
 * <p>The annotated method must meet the following requirements:</p>
 * <ul>
 *   <li>Must be public</li>
 *   <li>Must have exactly one parameter (the event type)</li>
 *   <li>Must not be static</li>
 *   <li>Should not throw checked exceptions</li>
 * </ul>
 * 
 * <p>Basic example:</p>
 * <pre>
 * {@code
 * public class EventHandler {
 *     @Subscribe
 *     public void onStringEvent(String event) {
 *         System.out.println("Received: " + event);
 *     }
 *     
 *     @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
 *     public void onUserEvent(UserEvent event) {
 *         // Handle user event on background thread
 *         processUser(event.getUser());
 *     }
 * }
 * }
 * </pre>
 * 
 * <p>Example with all parameters:</p>
 * <pre>
 * {@code
 * public class AdvancedHandler {
 *     @Subscribe(
 *         threadMode = ThreadMode.THREAD_POOL_EXECUTOR,
 *         eventId = "criticalEvents",
 *         sticky = true,
 *         strictEventType = true,
 *         interval = 1000,
 *         deduplicate = true
 *     )
 *     public void onCriticalEvent(CriticalEvent event) {
 *         // This method will:
 *         // - Run on a background thread
 *         // - Only receive events posted with "criticalEvents" ID
 *         // - Receive the most recent sticky event on registration
 *         // - Only accept exact CriticalEvent type (not subtypes)
 *         // - Ignore events within 1 second of the last one
 *         // - Ignore duplicate consecutive events
 *     }
 * }
 * }
 * </pre>
 * 
 * @see EventBus
 * @see Subscriber
 * @see ThreadMode
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * Specifies the thread mode for event delivery.
     * This determines on which thread the subscriber method will be called.
     * 
     * <p>Available options:</p>
     * <ul>
     *   <li>{@link ThreadMode#DEFAULT} - Events are delivered on the same thread that posts them (default)</li>
     *   <li>{@link ThreadMode#THREAD_POOL_EXECUTOR} - Events are delivered on a background thread from a thread pool</li>
     * </ul>
     * 
     * <p>Example:</p>
     * <pre>
     * {@code
     * @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
     * public void onHeavyEvent(DataEvent event) {
     *     // This will run on a background thread
     *     performExpensiveOperation(event.getData());
     * }
     * }
     * </pre>
     * 
     * @return the thread mode for event delivery
     */
    ThreadMode threadMode() default ThreadMode.DEFAULT;

    /**
     * Controls whether the subscriber accepts only exact event type matches or includes subtypes.
     * 
     * <p>When set to true, only events of the exact parameter type will be delivered.
     * When false (default), events of the parameter type and all its subtypes will be delivered.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * {@code
     * // With strictEventType = false (default)
     * @Subscribe
     * public void onAnimal(Animal event) {
     *     // Receives Animal, Dog, Cat, etc.
     * }
     * 
     * // With strictEventType = true
     * @Subscribe(strictEventType = true)
     * public void onAnimalOnly(Animal event) {
     *     // Receives only Animal, not Dog or Cat
     * }
     * }
     * </pre>
     * 
     * @return {@code true} to accept only exact type matches, {@code false} to accept subtypes
     */
    boolean strictEventType() default false;

    /**
     * Indicates whether this subscriber should receive the most recent sticky event upon registration.
     * 
     * <p>When true, if a sticky event matching this subscriber's type and event ID was previously posted,
     * it will be immediately delivered to this subscriber upon registration.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * {@code
     * // Post a sticky configuration
     * eventBus.postSticky(new AppConfig("production"));
     * 
     * // Later, register a subscriber
     * public class ConfigHandler {
     *     @Subscribe(sticky = true)
     *     public void onConfig(AppConfig config) {
     *         // This will be called immediately with the sticky config
     *         applyConfiguration(config);
     *     }
     * }
     * }
     * </pre>
     * 
     * @return {@code true} to receive sticky events upon registration
     */
    boolean sticky() default false;

    /**
     * Specifies an event ID to filter incoming events.
     * Only events posted with the matching event ID will be delivered to this subscriber.
     * 
     * <p>If empty (default), the subscriber will receive events based on type matching alone,
     * unless it was registered with a specific event ID.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * {@code
     * @Subscribe(eventId = "userUpdates")
     * public void onUserUpdate(User user) {
     *     // Only receives User events posted with "userUpdates" ID
     * }
     * 
     * @Subscribe(eventId = "adminUpdates")
     * public void onAdminUpdate(User user) {
     *     // Only receives User events posted with "adminUpdates" ID
     * }
     * }
     * </pre>
     * 
     * @return the event ID to filter on, or empty string for no filtering
     */
    String eventId() default "";

    /**
     * Specifies the minimum time interval (in milliseconds) between event deliveries.
     * Events posted within this interval will be ignored.
     * 
     * <p>This is useful for throttling high-frequency events to prevent overwhelming the subscriber.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * {@code
     * @Subscribe(interval = 1000)  // Maximum one event per second
     * public void onSensorData(SensorEvent event) {
     *     updateDisplay(event.getValue());
     * }
     * 
     * @Subscribe(interval = 5000)  // Maximum one event per 5 seconds
     * public void onLocationUpdate(Location location) {
     *     saveLocationToServer(location);
     * }
     * }
     * </pre>
     * 
     * @return the minimum interval between events in milliseconds, 0 for no throttling
     */
    long interval() default 0; // Unit is milliseconds.

    /**
     * Controls whether duplicate consecutive events should be ignored.
     * 
     * <p>When true, if an event equal to the previous event is posted, it will be ignored.
     * Events are compared using their equals() method.</p>
     * 
     * <p>This is useful for preventing redundant processing of unchanged data.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * {@code
     * @Subscribe(deduplicate = true)
     * public void onTemperature(Temperature temp) {
     *     // If temperature hasn't changed, event is ignored
     *     updateThermostat(temp.getValue());
     * }
     * 
     * @Subscribe(deduplicate = true)
     * public void onConfigChange(Config config) {
     *     // Only processes when configuration actually changes
     *     applyNewConfiguration(config);
     * }
     * }
     * </pre>
     * 
     * @return {@code true} to ignore duplicate consecutive events
     */
    boolean deduplicate() default false;
}