package io.mubel.client;

import java.util.List;

public interface Subscription<T> {

    T next() throws InterruptedException;

    List<T> nextBatch(int size) throws InterruptedException;

}
