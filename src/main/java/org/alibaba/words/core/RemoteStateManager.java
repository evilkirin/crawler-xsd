package org.alibaba.words.core;


public interface RemoteStateManager<T> {

	public void syncState(T state) throws InterruptedException;

	public T initialState() throws InterruptedException;

}