/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geekbang.flink.dynamicrules.accumulators;

import java.math.BigDecimal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.accumulators.SimpleAccumulator;

/** An accumulator that sums up {@code double} values. */
@PublicEvolving
public class BigDecimalCounter implements SimpleAccumulator<BigDecimal> {

  private static final long serialVersionUID = 1L;

  private BigDecimal localValue = BigDecimal.ZERO;

  public BigDecimalCounter() {}

  public BigDecimalCounter(BigDecimal value) {
    this.localValue = value;
  }

  // ------------------------------------------------------------------------
  //  Accumulator
  // ------------------------------------------------------------------------

  @Override
  public void add(BigDecimal value) {
    localValue = localValue.add(value);
  }

  @Override
  public BigDecimal getLocalValue() {
    return localValue;
  }

  @Override
  public void merge(Accumulator<BigDecimal, BigDecimal> other) {
    localValue = localValue.add(other.getLocalValue());
  }

  @Override
  public void resetLocal() {
    this.localValue = BigDecimal.ZERO;
  }

  @Override
  public BigDecimalCounter clone() {
    BigDecimalCounter result = new BigDecimalCounter();
    result.localValue = localValue;
    return result;
  }

  // ------------------------------------------------------------------------
  //  Utilities
  // ------------------------------------------------------------------------

  @Override
  public String toString() {
    return "BigDecimalCounter " + this.localValue;
  }
}
