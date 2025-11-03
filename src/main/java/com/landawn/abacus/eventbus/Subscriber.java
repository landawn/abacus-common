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

/**
 * A functional interface for implementing event subscribers in the EventBus system.
 * Classes implementing this interface can be registered with an {@link EventBus} to receive events
 * of the specified type.
 * 
 * <p>This interface is particularly useful for creating subscribers using lambda expressions
 * or anonymous inner classes. When using lambda expressions, the subscriber must be registered
 * with an event ID to avoid ambiguity.</p>
 * 
 * <p><b>Usage Examples with lambda expression:</b></p>
 * <pre>{@code
 * EventBus eventBus = EventBus.getDefault();
 *
 * // Lambda subscriber for String events
 * eventBus.register((Subscriber<String>) event -> {
 *     System.out.println("Received: " + event);
 * }, "stringEvents");
 *
 * // Lambda subscriber for custom event types
 * eventBus.register((Subscriber<UserLoginEvent>) event -> {
 *     System.out.println("User logged in: " + event.getUserId());
 * }, "userEvents");
 * }</pre>
 * 
 * <p><b>Usage Examples with anonymous inner class:</b></p>
 * <pre>{@code
 * Subscriber<Integer> numberSubscriber = new Subscriber<Integer>() {
 *     @Override
 *     public void on(Integer event) {
 *         System.out.println("Number received: " + event);
 *     }
 * };
 * eventBus.register(numberSubscriber, "numberEvents");
 * }</pre>
 * 
 * <p><b>Usage Examples with method reference:</b></p>
 * <pre>{@code
 * public class MyHandler {
 *     public void handleString(String message) {
 *         System.out.println("Handling: " + message);
 *     }
 * }
 *
 * MyHandler handler = new MyHandler();
 * eventBus.register((Subscriber<String>) handler::handleString, "messages");
 * }</pre>
 * 
 * @param <E> the type of event this subscriber will receive
 * @see EventBus
 * @see Subscribe
 */
@FunctionalInterface
public interface Subscriber<E> {

    /**
     * Called when an event of type E is posted to the EventBus.
     * This method will be invoked by the EventBus when a matching event is posted.
     * 
     * <p>The method will be called on the thread specified during registration,
     * or on the posting thread if no thread mode was specified.</p>
     * 
     * <p>Implementations should handle the event appropriately and should not throw
     * unchecked exceptions. Any exceptions thrown will be caught and logged by the EventBus.</p>
     * 
     * <p><b>Usage Examples for implementation:</b></p>
     * <pre>{@code
     * public void on(UserEvent event) {
     *     // Process the user event
     *     updateUserInterface(event.getUser());
     *     logUserActivity(event);
     * }
     * }</pre>
     *
     * @param event the event instance posted to the EventBus. Never {@code null}.
     */
    void on(E event);
}
