package org.apache.beam.sdk.io.gcp.datastore;

import com.google.datastore.v1.CommitRequest;
import com.google.datastore.v1.Entity;
import com.google.datastore.v1.Key;
import com.google.datastore.v1.Mutation;
import com.google.datastore.v1.client.Datastore;
import com.google.datastore.v1.client.DatastoreException;
import com.google.rpc.Code;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.util.BackOff;
import org.apache.beam.sdk.util.BackOffUtils;
import org.apache.beam.sdk.util.FluentBackoff;
import org.apache.beam.sdk.util.Sleeper;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.beam.sdk.transforms.display.DisplayData;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastoreV1SOT {

    // A package-private constructor to prevent direct instantiation from outside of this package
    DatastoreV1SOT() {
    }

    /**
     * A {@link PTransform} that writes {@link Entity} objects to Cloud Datastore.
     *
     * @see DatastoreIO
     */
    public static class Write extends Mutate<Entity> {
        /**
         * Note that {@code projectId} is only {@code @Nullable} as a matter of build order, but if
         * it is {@code null} at instantiation time, an error will be thrown.
         */
        Write(@Nullable ValueProvider<String> projectId, @Nullable String localhost, Boolean dedupCommits) {
            super(projectId, localhost, new DatastoreV1.UpsertFn(), dedupCommits);
        }

        /**
         * Returns a new {@link Write} that writes to the Cloud Datastore for the specified project.
         */
        public Write withProjectId(String projectId) {
            checkNotNull(projectId, "projectId");
            return withProjectId(ValueProvider.StaticValueProvider.of(projectId));
        }

        /**
         * Same as {@link Write#withProjectId(String)} but with a {@link ValueProvider}.
         */
        public Write withProjectId(ValueProvider<String> projectId) {
            checkNotNull(projectId, "projectId ValueProvider");
            return new Write(projectId, localhost, dedupCommits);
        }

        public Write removeDuplicatesWithinCommits(Boolean dedupCommits) {
            return new Write(projectId, localhost, dedupCommits);
        }

        /**
         * Returns a new {@link Write} that writes to the Cloud Datastore Emulator running locally on
         * the specified host port.
         */
        public Write withLocalhost(String localhost) {
            checkNotNull(localhost, "localhost");
            return new Write(projectId, localhost, dedupCommits);
        }
    }

    /**
     * Returns an empty {@link DatastoreV1.Write} builder. Configure the destination
     * {@code projectId} using {@link DatastoreV1.Write#withProjectId}.
     */
    public Write write() {
        return new Write(null, null, false);
    }


    public abstract static class Mutate<T> extends PTransform<PCollection<T>, PDone> {
        protected ValueProvider<String> projectId;
        @Nullable
        protected String localhost;
        protected Boolean dedupCommits;
        /**
         * A function that transforms each {@code T} into a mutation.
         */
        private final SimpleFunction<T, Mutation> mutationFn;

        /**
         * Note that {@code projectId} is only {@code @Nullable} as a matter of build order, but if
         * it is {@code null} at instantiation time, an error will be thrown.
         */
        Mutate(@Nullable ValueProvider<String> projectId, @Nullable String localhost,
               SimpleFunction<T, Mutation> mutationFn, Boolean dedupCommits) {
            this.projectId = projectId;
            this.localhost = localhost;
            this.mutationFn = checkNotNull(mutationFn);
            this.dedupCommits = dedupCommits;
        }

        @Override
        public PDone expand(PCollection<T> input) {
            input.apply("Convert to Mutation", MapElements.via(mutationFn))
                    .apply("Write Mutation to Datastore", ParDo.of(
                            new DatastoreWriterFn(projectId, localhost, dedupCommits)));

            return PDone.in(input.getPipeline());
        }

        @Override
        public void validate(PipelineOptions options) {
            checkNotNull(projectId, "projectId ValueProvider");
            if (projectId.isAccessible()) {
                checkNotNull(projectId.get(), "projectId");
            }
            checkNotNull(mutationFn, "mutationFn");
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("projectId", projectId)
                    .add("mutationFn", mutationFn.getClass().getName())
                    .toString();
        }

        @Override
        public void populateDisplayData(DisplayData.Builder builder) {
            super.populateDisplayData(builder);
            builder
                    .addIfNotNull(DisplayData.item("projectId", projectId)
                            .withLabel("Output Project"))
                    .include("mutationFn", mutationFn);
        }

        public String getProjectId() {
            return projectId.get();
        }
    }

    /**
     * {@link DoFn} that writes {@link Mutation}s to Cloud Datastore. Mutations are written in
     * batches; see {@link DatastoreV1.WriteBatcherImpl}.
     * <p>
     * <p>See <a
     * href="https://cloud.google.com/datastore/docs/concepts/entities">
     * Datastore: Entities, Properties, and Keys</a> for information about entity keys and mutations.
     * <p>
     * <p>Commits are non-transactional.  If a commit fails because of a conflict over an entity
     * group, the commit will be retried (up to {@link DatastoreV1.DatastoreWriterFn#MAX_RETRIES}
     * times). This means that the mutation operation should be idempotent. Thus, the writer should
     * only be used for {@code upsert} and {@code delete} mutation operations, as these are the only
     * two Cloud Datastore mutations that are idempotent.
     */
    static class DatastoreWriterFn extends DoFn<Mutation, Void> {
        private class MutationTimestamped {
            Mutation mutation;
            Instant instant;

            public MutationTimestamped(Mutation mutation, Instant instant) {
                this.mutation = mutation;
                this.instant = instant;
            }
        }
        private static final Logger LOG = LoggerFactory.getLogger(DatastoreWriterFn.class);
        private final ValueProvider<String> projectId;
        @Nullable
        private final String localhost;
        private transient Datastore datastore;
        private final DatastoreV1.V1DatastoreFactory datastoreFactory;
        // Current batch of mutations to be written.
        private final List<MutationTimestamped> mutationsTimestamped = new ArrayList<>();
        private final List<Mutation> mutations = new ArrayList<>();
        private int mutationsSize = 0;  // Accumulated size of protos in mutations.
        private DatastoreV1.WriteBatcher writeBatcher;

        private static final int MAX_RETRIES = 5;
        private static final FluentBackoff BUNDLE_WRITE_BACKOFF =
                FluentBackoff.DEFAULT
                        .withMaxRetries(MAX_RETRIES).withInitialBackoff(Duration.standardSeconds(5));
        private Boolean dedupCommits;

        DatastoreWriterFn(String projectId, @Nullable String localhost, Boolean dedupCommits) {
            this(ValueProvider.StaticValueProvider.of(projectId), localhost, new DatastoreV1.V1DatastoreFactory(),
                    new DatastoreV1.WriteBatcherImpl(), dedupCommits);
        }

        DatastoreWriterFn(ValueProvider<String> projectId, @Nullable String localhost, Boolean dedupCommits) {
            this(projectId, localhost, new DatastoreV1.V1DatastoreFactory(), new DatastoreV1.WriteBatcherImpl(), dedupCommits);
        }

        DatastoreWriterFn(ValueProvider<String> projectId, @Nullable String localhost,
                          DatastoreV1.V1DatastoreFactory datastoreFactory, DatastoreV1.WriteBatcher writeBatcher, Boolean dedupCommits) {
            this.projectId = checkNotNull(projectId, "projectId");
            this.localhost = localhost;
            this.datastoreFactory = datastoreFactory;
            this.writeBatcher = writeBatcher;
        }

        @StartBundle
        public void startBundle(StartBundleContext c) {
            datastore = datastoreFactory.getDatastore(c.getPipelineOptions(), projectId.get(), localhost);
            writeBatcher.start();
        }

        @ProcessElement
        public void processElement(ProcessContext c) throws Exception {
            Mutation write = c.element();
            int size = write.getSerializedSize();
            if (mutations.size() > 0
                    && mutationsSize + size >= DatastoreV1.DATASTORE_BATCH_UPDATE_BYTES_LIMIT) {
                flushBatch();
            }
            mutationsTimestamped.add(new MutationTimestamped(c.element(), c.timestamp()));
            mutationsSize += size;
            if (mutationsTimestamped.size() >= writeBatcher.nextBatchSize(System.currentTimeMillis())) {
                if (dedupCommits) {
                    dedupeMutations();
                }
                flushBatch();
                mutationsTimestamped.clear();
            }
        }

        @FinishBundle
        public void finishBundle() throws Exception {
            if (!mutations.isEmpty()) {
                flushBatch();
            }
        }

        public void dedupeMutations() {
            Map<Object, MutationTimestamped> seenKeys = new HashMap<>();
            for (MutationTimestamped m : mutationsTimestamped) {
                Object k = getKey(m.mutation);
                if (k == null) k = m.mutation;
                MutationTimestamped prevMutation = seenKeys.get(k);
                if (prevMutation != null) {
                    if (prevMutation.instant.isBefore(m.instant)) {
                        LOG.warn("Dropping duplicate mutation for the key: " + k + " with ts " + prevMutation.instant +
                                ", found newer mutation with ts " + m.instant);
                        seenKeys.put(k, m);
                    }
                } else {
                    seenKeys.put(k, m);
                }
            }
            mutations.addAll(seenKeys.values().stream().map(m-> m.mutation).collect(Collectors.toList()));
        }

        private Key getKey(Mutation m) {
            Entity e = m.getInsert();
            if (!e.equals(Entity.getDefaultInstance())) {
                return e.getKey();
            }
            e = m.getUpdate();
            if (!e.equals(Entity.getDefaultInstance())) {
                return e.getKey();
            }
            e = m.getUpsert();
            if (!e.equals(Entity.getDefaultInstance())) {
                return e.getKey();
            }
            Key k = m.getDelete();
            if (!k.equals(Key.getDefaultInstance())) {
                return k;
            }
            return null;
        }

        /**
         * Writes a batch of mutations to Cloud Datastore.
         * <p>
         * <p>If a commit fails, it will be retried up to {@link #MAX_RETRIES} times. All
         * mutations in the batch will be committed again, even if the commit was partially
         * successful. If the retry limit is exceeded, the last exception from Cloud Datastore will be
         * thrown.
         *
         * @throws DatastoreException if the commit fails or IOException or InterruptedException if
         *                            backing off between retries fails.
         */
        private void flushBatch() throws DatastoreException, IOException, InterruptedException {
            LOG.debug("Writing batch of {} mutations", mutations.size());

            Sleeper sleeper = Sleeper.DEFAULT;
            BackOff backoff = BUNDLE_WRITE_BACKOFF.backoff();

            while (true) {
                // Batch upsert entities.
                CommitRequest.Builder commitRequest = CommitRequest.newBuilder();
                commitRequest.addAllMutations(mutations);
                commitRequest.setMode(CommitRequest.Mode.NON_TRANSACTIONAL);
                long startTime = System.currentTimeMillis(), endTime;

                try {
                    datastore.commit(commitRequest.build());
                    endTime = System.currentTimeMillis();

                    writeBatcher.addRequestLatency(endTime, endTime - startTime, mutations.size());

                    // Break if the commit threw no exception.
                    break;
                } catch (DatastoreException exception) {
                    if (exception.getCode() == Code.DEADLINE_EXCEEDED) {
                        /* Most errors are not related to request size, and should not change our expectation of
                         * the latency of successful requests. DEADLINE_EXCEEDED can be taken into
                         * consideration, though. */
                        endTime = System.currentTimeMillis();
                        writeBatcher.addRequestLatency(endTime, endTime - startTime, mutations.size());
                    }

                    // Only log the code and message for potentially-transient errors. The entire exception
                    // will be propagated upon the last retry.
                    LOG.error("Error writing batch of {} mutations to Datastore ({}): {}", mutations.size(),
                            exception.getCode(), exception.getMessage());
                    if (!BackOffUtils.next(sleeper, backoff)) {
                        LOG.error("Aborting after {} retries.", MAX_RETRIES);
                        throw exception;
                    }
                }
            }
            LOG.debug("Successfully wrote {} mutations", mutations.size());
            mutations.clear();
            mutationsSize = 0;
        }

        @Override
        public void populateDisplayData(DisplayData.Builder builder) {
            super.populateDisplayData(builder);
            builder
                    .addIfNotNull(DisplayData.item("projectId", projectId)
                            .withLabel("Output Project"));
        }
    }


}


