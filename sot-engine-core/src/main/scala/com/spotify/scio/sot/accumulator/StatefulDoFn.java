package com.spotify.scio.sot.accumulator;

import com.google.datastore.v1.Entity;
import parallelai.sot.engine.generic.row.JavaRow;
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

/**
  * StatefulDoFn keeps track of a state of the marked variables and stores them to datastore if persistence is provided.
  * @tparam K     key
  * @tparam V     value
  * @tparam Value value type
  */
public class StatefulDoFn<K, V extends HList, Value extends HList>
        extends DoFn<KV<K, JavaRow<V>>, scala.Tuple3<JavaRow<V>, JavaRow<Value>, Instant>> {

    private Function<JavaRow<V>, JavaRow<Value>> getValue;
    private JavaRow<Value> defaultValue;
    private BiFunction<JavaRow<Value>, JavaRow<Value>, JavaRow<Value>> aggr;
    private scala.Option<Datastore> persistence;
    private FromMappable<Value, Entity.Builder> fromL;

    /**
     * @param getValue     function to get the value to keep track of from the data object
     * @param defaultValue value to initialise the state
     * @param aggr         aggregate values
     * @param persistence  storage option to persist states
     */
    public StatefulDoFn(Function<JavaRow<V>, JavaRow<Value>> getValue, JavaRow<Value> defaultValue,
                        BiFunction<JavaRow<Value>, JavaRow<Value>, JavaRow<Value>> aggr,
                        scala.Option<Datastore> persistence,
                        FromMappable<Value, Entity.Builder> fromL
                        ) {
        this.getValue = getValue;
        this.defaultValue = defaultValue;
        this.aggr = aggr;
        this.persistence = persistence;
        this.fromL = fromL;
    }


    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @StateId("value")
    private final StateSpec<ValueState<JavaRow<Value>>> stateSpec = StateSpecs.value();

    @ProcessElement
    public void processElement(ProcessContext context, @StateId("value") ValueState<JavaRow<Value>> state) {

        K key = context.element().getKey();

        JavaRow<Value> s = state.read();
        LOG.error("reading state " + s);

        scala.Option<JavaRow<Value>> current = DatastoreValueReader.readValue(key, scala.Option.apply(state.read()), persistence, fromL);

        JavaRow<Value> value = getValue.apply(context.element().getValue());

        JavaRow<Value> newValue;

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

