/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.trace.export;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import io.opentelemetry.context.Context;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;

/**
 * Internal accessor of JCTools package for fast queues.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class JcTools {

  /**
   * Returns a new {@link Queue} appropriate for use with multiple producers and a single consumer.
   */
  public static <T> Queue<T> newFixedSizeQueue(int capacity) {
    return AccessController.doPrivileged((PrivilegedAction<Queue<T>>) () -> {
      try {
        return new MpscArrayQueue<>(capacity);
      } catch (NoClassDefFoundError e) {
        // Happens when modules such as jdk.unsupported are disabled in a custom JRE distribution
        return new ArrayBlockingQueue<>(capacity);
      }
    });
  }

  /**
   * Returns the capacity of the {@link Queue}. We cast to the implementation so callers do not need
   * to use the shaded classes.
   */
  public static long capacity(Queue<?> queue) {
    if (queue instanceof MessagePassingQueue) {
      return ((MessagePassingQueue<?>) queue).capacity();
    } else {
      return (long) ((ArrayBlockingQueue<?>) queue).remainingCapacity() + queue.size();
    }
  }

  /**
   * Remove up to <i>limit</i> elements from the {@link Queue} and hand to consume.
   *
   * @throws IllegalArgumentException consumer is {@code null}
   * @throws IllegalArgumentException if maxExportBatchSize is negative
   */
  @SuppressWarnings("unchecked")
  public static <T> void drain(Queue<T> queue, int limit, Consumer<T> consumer) {
    if (queue instanceof MessagePassingQueue) {
      ((MessagePassingQueue<T>) queue).drain(consumer::accept, limit);
    } else {
      drainNonJcQueue(queue, limit, consumer);
    }
  }

  private static <T> void drainNonJcQueue(
      Queue<T> queue, int maxExportBatchSize, Consumer<T> consumer) {
    int polledCount = 0;
    T item;
    while (polledCount++ < maxExportBatchSize && (item = queue.poll()) != null) {
      consumer.accept(item);
    }
  }

  private JcTools() {}
}
