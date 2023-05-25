package org.apache.beam.sdk.io.gcp.datastore;

import com.google.datastore.v1.Entity;
import com.google.datastore.v1.Mutation;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

import com.google.common.base.MoreObjects;
import static com.google.common.base.Preconditions.checkNotNull;
import org.apache.beam.sdk.transforms.display.DisplayData;

import javax.annotation.Nullable;

public class DatastoreV1SOT {

    /**
     * Returns an empty {@link DatastoreV1.Write} builder. Configure the destination
     * {@code projectId} using {@link DatastoreV1.Write#withProjectId}.
     */
    public Write write() {
        return new Write(null, null);
    }

    private abstract static class Mutate<T> extends PTransform<PCollection<T>, PDone> {
        protected ValueProvider<String> projectId;
        @Nullable
        protected String localhost;
        /** A function that transforms each {@code T} into a mutation. */
        private final SimpleFunction<T, Mutation> mutationFn;

        /**
         * Note that {@code projectId} is only {@code @Nullable} as a matter of build order, but if
         * it is {@code null} at instantiation time, an error will be thrown.
         */
        Mutate(@Nullable ValueProvider<String> projectId, @Nullable String localhost,
               SimpleFunction<T, Mutation> mutationFn) {
            this.projectId = projectId;
            this.localhost = localhost;
            this.mutationFn = checkNotNull(mutationFn);
        }

        @Override
        public PDone expand(PCollection<T> input) {
            input.apply("Convert to Mutation", MapElements.via(mutationFn))
                    .apply("Write Mutation to Datastore", ParDo.of(
                            new DatastoreV1.DatastoreWriterFn(projectId, localhost)));

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


    public static class Write extends Mutate<Entity> {
        /**
         * Note that {@code projectId} is only {@code @Nullable} as a matter of build order, but if
         * it is {@code null} at instantiation time, an error will be thrown.
         */
        Write(@Nullable ValueProvider<String> projectId, @Nullable String localhost) {
            super(projectId, localhost, new DatastoreV1.UpsertFn());
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
            return new Write(projectId, localhost);
        }

        /**
         * Returns a new {@link Write} that writes to the Cloud Datastore Emulator running locally on
         * the specified host port.
         */
        public Write withLocalhost(String localhost) {
            checkNotNull(localhost, "localhost");
            return new Write(projectId, localhost);
        }
    }

}


