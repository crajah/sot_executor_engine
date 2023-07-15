package com.spotify.scio.sot.accumulator;

import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.ValueState;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.MoreObjects.firstNonNull;

public class StatefulDoFn<K, V, Out> extends DoFn<KV<K, V>, Out> {

    private Function<V, Integer> getValue;
    private BiFunction<V, Integer, Out> toOut;

    public StatefulDoFn(Function<V, Integer> getValue, BiFunction<V, Integer, Out> toOut) {
        this.getValue = getValue;
        this.toOut = toOut;
    }

    @StateId("index")
    private final StateSpec<ValueState<Integer>> indexSpec = StateSpecs.value(VarIntCoder.of());

    @ProcessElement
    public void processElement(ProcessContext context, @StateId("index") ValueState<Integer> index) {
        Integer current = firstNonNull(index.read(), 0);
        K key = context.element().getKey();
        Integer value = getValue.apply(context.element().getValue());
        context.output(toOut.apply(context.element().getValue(), current));
        index.write(current + value);
    }

}