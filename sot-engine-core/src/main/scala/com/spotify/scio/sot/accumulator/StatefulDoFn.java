package com.spotify.scio.sot.accumulator;

import com.google.datastore.v1.Entity;
import parallelai.sot.engine.generic.row.Row;
import parallelai.sot.engine.io.datatype.FromMappable;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.ValueState;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.joda.time.Instant;
import parallelai.sot.engine.io.datastore.Datastore;
import shapeless.HList;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulDoFn<K, V extends HList, Value extends HList>
        extends DoFn<KV<K, V>, scala.Tuple3<V, Value, Instant>> {

    private Function<V, Value> getValue;
    private Value defaultValue;
    private BiFunction<Value, Value, Value> aggr;
    private scala.Option<Datastore> persistence;
    private FromMappable<Value, Entity.Builder> fromL;

    public StatefulDoFn(Function<V, Value> getValue, Value defaultValue,
                        BiFunction<Value, Value, Value> aggr,
                        scala.Option<Datastore> persistence,
                        FromMappable<Value, Entity.Builder> fromL
                        ) {
        this.getValue = getValue;
        this.defaultValue = defaultValue;
        this.aggr = aggr;
        this.persistence = persistence;
        this.fromL = fromL;
    }


    public void test(Row r) {

    }

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @StateId("value")
    private final StateSpec<ValueState<Value>> stateSpec = StateSpecs.value();

    @ProcessElement
    public void processElement(ProcessContext context, @StateId("value") ValueState<Value> state) {

        K key = context.element().getKey();

        Value s = state.read();
        LOG.error("reading state " + s);

        scala.Option<Value> current = StatefulDoFnScala.readValue(key, scala.Option.apply(state.read()), persistence, fromL);

        Value value = getValue.apply(context.element().getValue());

        Value newValue;

        if (current.isEmpty()) {
            newValue = aggr.apply(defaultValue, value);
        } else {
            newValue = aggr.apply(current.get(), value);
        }

        context.output(scala.Tuple3.apply(context.element().getValue(), newValue, Instant.now()));

        LOG.error("writing state " + newValue);

        state.write(newValue);
    }


}

