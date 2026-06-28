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
 * Marks a method as an event subscriber in the {@link EventBus} system.
 * Methods annotated with {@code @Subscribe} will receive events when their containing object
 * is registered with an {@link EventBus}.
 *
 * <p>The annotated method must meet the following requirements:</p>
 * <ul>
 *   <li>Must be {@code public}.</li>
 *   <li>Must have exactly one parameter representing the event type.</li>
 *   <li>Must not be {@code static}.</li>
 *   <li>Should not throw checked exceptions (any thrown exceptions are caught and logged).</li>
 * </ul>
 *
 * <p><b>Annotation-only advanced features:</b> The {@link #sticky()}, {@link #strictEventType()},
 * {@link #intervalMillis()} (throttling) and {@link #deduplicate()} knobs can be configured
 * <em>only</em> through this annotation. The {@code EventBus.register(...)} methods accept
 * registration-time overrides for the event ID and thread mode only, so a lambda /
 * {@link Subscriber} registration (which has no annotated method) cannot use any of these four
 * features. Annotate a subscriber method with {@code @Subscribe} to opt into them.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class EventHandler {
 *     @Subscribe
 *     public void onStringEvent(String event) {
 *         System.out.println("Received: " + event);
 *     }
 *
 *     @Subscribe(
 *         threadMode = ThreadMode.THREAD_POOL_EXECUTOR,
 *         eventId = "criticalEvents",
 *         sticky = true,
 *         strictEventType = true,
 *         intervalMillis = 1000,
 *         deduplicate = true
 *     )
 *     public void onCriticalEvent(CriticalEvent event) {
 *         // Runs on background thread, receives only exact CriticalEvent type,
 *         // picks up sticky events on registration, throttled to 1 per second,
 *         // and ignores consecutive duplicate events.
 *     }
 * }
 * }</pre>
 *
 * @see EventBus
 * @see Subscriber
 * @see ThreadMode
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
     * public void onHeavyEvent(DataEvent event) {
     *     // This will run on a background thread
     *     performExpensiveOperation(event.getData());
     * }
     * }</pre>
     *
     * @return the thread mode for event delivery
     */
    ThreadMode threadMode() default ThreadMode.DEFAULT;

    /**
     * Controls whether the subscriber accepts only exact event type matches or includes subtypes.
     *
     * <p>When set to {@code true}, only events of the exact parameter type will be delivered.
     * When {@code false} (default), events of the parameter type and all its subtypes will be delivered.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
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
     * }</pre>
     *
     * @return {@code true} to accept only exact type matches, {@code false} to accept subtypes
     */
    boolean strictEventType() default false;

    /**
     * Indicates whether this subscriber should receive previously posted sticky events upon registration.
     *
     * <p>When {@code true}, every retained sticky event matching this subscriber's type and event ID is
     * immediately delivered to this subscriber upon registration.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
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
     * }</pre>
     *
     * @return {@code true} to receive sticky events upon registration
     */
    boolean sticky() default false;

    /**
     * Specifies an event ID to filter incoming events.
     * Only events posted with the matching event ID will be delivered to this subscriber.
     *
     * <p>If empty (default) and no registration-level event ID is supplied, the subscriber receives
     * only events posted without an event ID, matched by type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Subscribe(eventId = "userUpdates")
     * public void onUserUpdate(User user) {
     *     // Only receives User events posted with "userUpdates" ID
     * }
     *
     * @Subscribe(eventId = "adminUpdates")
     * public void onAdminUpdate(User user) {
     *     // Only receives User events posted with "adminUpdates" ID
     * }
     * }</pre>
     *
     * @return the event ID to filter on, or an empty string to match events posted without an event ID
     */
    String eventId() default "";

    /**
     * Specifies the minimum time interval (in milliseconds) between event deliveries.
     * Events posted within this interval will be ignored.
     *
     * <p>This is useful for throttling high-frequency events to prevent overwhelming the subscriber.</p>
     *
     * <p>A value of {@code 0} (the default) disables throttling. Any value <b>{@code <= 0}</b> is
     * likewise treated as "no throttling": the {@code EventBus} delivery guard only throttles when
     * the interval is strictly positive, so a negative interval has the same effect as {@code 0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Subscribe(intervalMillis = 1000)  // keeps at most one event per second
     * public void onSensorData(SensorEvent event) {
     *     updateDisplay(event.getValue());
     * }
     *
     * @Subscribe(intervalMillis = 5000)  // keeps at most one event per 5 seconds
     * public void onLocationUpdate(Location location) {
     *     saveLocationToServer(location);
     * }
     * }</pre>
     *
     * @return the minimum interval between events in milliseconds; any value {@code <= 0} disables throttling
     */
    long intervalMillis() default 0;

    /**
     * Controls whether duplicate consecutive events should be ignored.
     *
     * <p>When {@code true}, if an event equal to the previous event is posted, it will be ignored.
     * Events are compared using their {@code equals()} method.</p>
     *
     * <p>This is useful for preventing redundant processing of unchanged data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
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
     * }</pre>
     *
     * @return {@code true} to ignore duplicate consecutive events
     */
    boolean deduplicate() default false;
}
