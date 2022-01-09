package com.gewuwo.logging.collect;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/9 13:22
 */
public class ExpiredBatches {

	private final List<ProducerBatch> batches = new ArrayList<ProducerBatch>();

	private long remainingMs;

	public List<ProducerBatch> getBatches() {
		return batches;
	}

	public void add(ProducerBatch producerBatch) {
		if (!batches.add(producerBatch)) {
			throw new IllegalStateException("failed to add producer batch to expired batches");
		}
	}

	public long getRemainingMs() {
		return remainingMs;
	}

	public void setRemainingMs(long remainingMs) {
		this.remainingMs = remainingMs;
	}
}
