package com.gewuwo.logging.collect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/9 12:52
 */
public class LogThread extends Thread{

	private static final Logger LOGGER = LogManager.getLogger(LogThread.class);

	public static LogThread daemon(final String name, Runnable runnable) {
		return new LogThread(name, runnable, true);
	}

	public static LogThread nonDaemon(final String name, Runnable runnable) {
		return new LogThread(name, runnable, false);
	}

	public LogThread(final String name, boolean daemon) {
		super(name);
		configureThread(name, daemon);
	}

	public LogThread(final String name, Runnable runnable, boolean daemon) {
		super(runnable, name);
		configureThread(name, daemon);
	}

	private void configureThread(final String name, boolean daemon) {
		setDaemon(daemon);
		setUncaughtExceptionHandler(
			new Thread.UncaughtExceptionHandler() {
				public void uncaughtException(Thread t, Throwable e) {
					LOGGER.error("Uncaught error in thread, name={}, e=", name, e);
				}
			});
	}
}
