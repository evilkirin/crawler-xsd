package org.alibaba.words.core;


public interface RemoteStateManager<T> {

	public void update(T state) throws InterruptedException;

	public T query() throws InterruptedException;

}