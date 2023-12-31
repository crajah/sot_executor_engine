package parallelai.sot.engine.config.gcp;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.extensions.gcp.auth.NullCredentialInitializer;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.util.*;
import org.joda.time.Duration;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.Bigquery.Datasets;
import com.google.api.services.bigquery.Bigquery.Tables;
import com.google.api.services.bigquery.model.*;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.Subscription;
import com.google.api.services.pubsub.model.Topic;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.cloud.hadoop.util.ChainingHttpRequestInitializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * The utility class that sets up and tears down external resources,
 * and cancels the streaming pipelines once the program terminates.
 */
public class SOTUtils {

    private static final int SC_NOT_FOUND = 404;

    private final PipelineOptions options;
    private Bigquery bigQueryClient = null;
    private Pubsub pubsubClient = null;
    private Set<PipelineResult> pipelinesToCancel = Sets.newHashSet();
    private List<String> pendingMessages = Lists.newArrayList();

    /**
     * Do resources and runner options setup.
     */
    public SOTUtils(PipelineOptions options) {
        this.options = options;
    }

    /**
     * Sets up external resources that are required by the example,
     * such as Pub/Sub topics and BigQuery tables.
     *
     * @throws IOException if there is a problem setting up the resources
     */
    public void setup() throws IOException {
        Sleeper sleeper = Sleeper.DEFAULT;
        BackOff backOff =
                FluentBackoff.DEFAULT
                        .withMaxRetries(3).withInitialBackoff(Duration.millis(200)).backoff();
        Throwable lastException = null;
        try {
            do {
                try {
                    setupPubsubWithSubscription();
                    setupBigQueryTable();
                    return;
                } catch (GoogleJsonResponseException e) {
                    lastException = e;
                }
            } while (BackOffUtils.next(sleeper, backOff));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Ignore InterruptedException
        }
        throw new RuntimeException(lastException);
    }

    /**
     * Sets up the Google Cloud Pub/Sub topic only.
     * <p>
     * <p>If the topic doesn't exist, a new topic with the given name will be created.
     *
     * @throws IOException if there is a problem setting up the Pub/Sub topic
     */
    public void setupPubsubTopic() throws IOException {
        SOTPubsubTopicAndSubscriptionOptions pubsubOptions = options.as(SOTPubsubTopicAndSubscriptionOptions.class);

        if (!pubsubOptions.getPubsubTopic().isEmpty()) {
            pendingMessages.add("**********************Set Up Pubsub************************");
            setupPubsubTopic(pubsubOptions.getPubsubTopic());
            pendingMessages.add("The Pub/Sub topic has been set up: " + pubsubOptions.getPubsubTopic());
        }
    }

    /**
     * Sets up the Google Pub/Sub subscription only.
     */
    public void setupPubsubSubscription() throws IOException {
        SOTPubsubTopicAndSubscriptionOptions pubsubOptions = options.as(SOTPubsubTopicAndSubscriptionOptions.class);
        if (!pubsubOptions.getPubsubTopic().isEmpty()) {
            pendingMessages.add("**********************Set Up Pubsub************************");
            if (!pubsubOptions.getPubsubSubscription().isEmpty()) {
                setupPubsubSubscription(pubsubOptions.getPubsubTopic(), pubsubOptions.getPubsubSubscription());
                pendingMessages.add("The Pub/Sub subscription has been set up: " + pubsubOptions.getPubsubSubscription());
            }
        }
    }

    /**
     * Sets up the Google Cloud Pub/Sub topic and subscription.
     * <p>
     * <p>If the topic doesn't exist, a new topic with the given name will be created.
     *
     * @throws IOException if there is a problem setting up the Pub/Sub topic
     */
    public void setupPubsubWithSubscription() throws IOException {
        SOTPubsubTopicAndSubscriptionOptions pubsubOptions = options.as(SOTPubsubTopicAndSubscriptionOptions.class);

        if (!pubsubOptions.getPubsubTopic().isEmpty()) {
            pendingMessages.add("**********************Set Up Pubsub************************");
            setupPubsubTopic(pubsubOptions.getPubsubTopic());
            pendingMessages.add("The Pub/Sub topic has been set up: " + pubsubOptions.getPubsubTopic());

            if (!pubsubOptions.getPubsubSubscription().isEmpty()) {
                setupPubsubSubscription(pubsubOptions.getPubsubTopic(), pubsubOptions.getPubsubSubscription());
                pendingMessages.add("The Pub/Sub subscription has been set up: " + pubsubOptions.getPubsubSubscription());
            }
        }
    }

    public String getProject() {
        return options.as(GcpOptions.class).getProject();
    }

    public String getJobName() {
        return options.as(GcpOptions.class).getJobName();
    }

    public String getPubsubTopic() {
        return options.as(SOTPubsubTopicAndSubscriptionOptions.class).getPubsubTopic();
    }

    public String getPubsubSubscription() {
        return options.as(SOTPubsubTopicAndSubscriptionOptions.class).getPubsubSubscription();
    }

    public void setPubsubTopic(String topic) {
        options.as(SOTPubsubTopicAndSubscriptionOptions.class).setPubsubTopic(topic);
    }

    public void setPubsubSubscription(String subscription) {
        options.as(SOTPubsubTopicAndSubscriptionOptions.class).setPubsubSubscription(subscription);
    }

    /**
     * Sets up the BigQuery table with the given schema.
     * <p>
     * <p>If the table already exists, the schema has to match the given one. Otherwise, the example
     * will throw a RuntimeException. If the table doesn't exist, a new table with the given schema
     * will be created.
     *
     * @throws IOException if there is a problem setting up the BigQuery table
     */
    public void setupBigQueryTable() throws IOException {
        SOTBigQueryTableOptions bigQueryTableOptions =
                options.as(SOTBigQueryTableOptions.class);
        if (bigQueryTableOptions.getBigQueryDataset() != null
                && bigQueryTableOptions.getBigQueryTable() != null
                && bigQueryTableOptions.getBigQuerySchema() != null) {
            pendingMessages.add("******************Set Up Big Query Table*******************");
            setupBigQueryTable(bigQueryTableOptions.getProject(),
                    bigQueryTableOptions.getBigQueryDataset(),
                    bigQueryTableOptions.getBigQueryTable(),
                    bigQueryTableOptions.getBigQuerySchema());
            pendingMessages.add("The BigQuery table has been set up: "
                    + bigQueryTableOptions.getProject()
                    + ":" + bigQueryTableOptions.getBigQueryDataset()
                    + "." + bigQueryTableOptions.getBigQueryTable());
        }
    }

    /**
     * Java is a joke!
     * @return Pubsub
     */
    private Pubsub pubsub() {
        if (pubsubClient == null) {
            pubsubClient = newPubsubClient(options.as(PubsubOptions.class)).build();
        }

        return pubsubClient;
    }

    /**
     * Tears down external resources that can be deleted upon completion.
     */
    private void tearDown() {
        pendingMessages.add("*************************Tear Down*************************");
        SOTPubsubTopicAndSubscriptionOptions pubsubOptions =
                options.as(SOTPubsubTopicAndSubscriptionOptions.class);
        if (!pubsubOptions.getPubsubTopic().isEmpty()) {
            try {
                deletePubsubTopic(pubsubOptions.getPubsubTopic());
                pendingMessages.add("The Pub/Sub topic has been deleted: "
                        + pubsubOptions.getPubsubTopic());
            } catch (IOException e) {
                pendingMessages.add("Failed to delete the Pub/Sub topic : "
                        + pubsubOptions.getPubsubTopic());
            }
            if (!pubsubOptions.getPubsubSubscription().isEmpty()) {
                try {
                    deletePubsubSubscription(pubsubOptions.getPubsubSubscription());
                    pendingMessages.add("The Pub/Sub subscription has been deleted: "
                            + pubsubOptions.getPubsubSubscription());
                } catch (IOException e) {
                    pendingMessages.add("Failed to delete the Pub/Sub subscription : "
                            + pubsubOptions.getPubsubSubscription());
                }
            }
        }

        SOTBigQueryTableOptions bigQueryTableOptions =
                options.as(SOTBigQueryTableOptions.class);
        if (bigQueryTableOptions.getBigQueryDataset() != null
                && bigQueryTableOptions.getBigQueryTable() != null
                && bigQueryTableOptions.getBigQuerySchema() != null) {
            pendingMessages.add("The BigQuery table might contain the example's output, "
                    + "and it is not deleted automatically: "
                    + bigQueryTableOptions.getProject()
                    + ":" + bigQueryTableOptions.getBigQueryDataset()
                    + "." + bigQueryTableOptions.getBigQueryTable());
            pendingMessages.add("Please go to the Developers Console to delete it manually."
                    + " Otherwise, you may be charged for its usage.");
        }
    }

    /**
     * Returns a BigQuery client builder using the specified {@link BigQueryOptions}.
     */
    private static Bigquery.Builder newBigQueryClient(BigQueryOptions options) {
        return new Bigquery.Builder(Transport.getTransport(), Transport.getJsonFactory(),
                chainHttpRequestInitializer(
                        options.getGcpCredential(),
                        // Do not log 404. It clutters the output and is possibly even required by the caller.
                        new RetryHttpRequestInitializer(ImmutableList.of(404))))
                .setApplicationName(options.getAppName())
                .setGoogleClientRequestInitializer(options.getGoogleApiTrace());
    }

    /**
     * Returns a Pubsub client builder using the specified {@link PubsubOptions}.
     */
    private static Pubsub.Builder newPubsubClient(PubsubOptions options) {
        return new Pubsub.Builder(Transport.getTransport(),
                Transport.getJsonFactory(),
                chainHttpRequestInitializer(options.getGcpCredential(), new RetryHttpRequestInitializer(ImmutableList.of(404)))) // Do not log 404. It clutters the output and is possibly even required by the caller.
                .setRootUrl(options.getPubsubRootUrl())
                .setApplicationName(options.getAppName())
                .setGoogleClientRequestInitializer(options.getGoogleApiTrace());
    }

    private static HttpRequestInitializer chainHttpRequestInitializer(
            Credentials credential, HttpRequestInitializer httpRequestInitializer) {
        if (credential == null) {
            return new ChainingHttpRequestInitializer(
                    new NullCredentialInitializer(), httpRequestInitializer);
        } else {
            return new ChainingHttpRequestInitializer(
                    new HttpCredentialsAdapter(credential),
                    httpRequestInitializer);
        }
    }

    private void setupBigQueryTable(String projectId, String datasetId, String tableId,
                                    TableSchema schema) throws IOException {
        if (bigQueryClient == null) {
            bigQueryClient = newBigQueryClient(options.as(BigQueryOptions.class)).build();
        }

        Datasets datasetService = bigQueryClient.datasets();
        if (executeNullIfNotFound(datasetService.get(projectId, datasetId)) == null) {
            Dataset newDataset = new Dataset().setDatasetReference(
                    new DatasetReference().setProjectId(projectId).setDatasetId(datasetId));
            datasetService.insert(projectId, newDataset).execute();
        }

        Tables tableService = bigQueryClient.tables();
        Table table = executeNullIfNotFound(tableService.get(projectId, datasetId, tableId));
        if (table == null) {
            Table newTable = new Table().setSchema(schema).setTableReference(
                    new TableReference().setProjectId(projectId).setDatasetId(datasetId).setTableId(tableId));
            tableService.insert(projectId, datasetId, newTable).execute();
        } else if (!table.getSchema().equals(schema)) {
            throw new RuntimeException(
                    "Table exists and schemas do not match, expecting: " + schema.toPrettyString()
                            + ", actual: " + table.getSchema().toPrettyString());
        }
    }

    private void setupPubsubTopic(String topic) throws IOException {
        if (executeNullIfNotFound(pubsub().projects().topics().get(topic)) == null) {
            pubsub().projects().topics().create(topic, new Topic().setName(topic)).execute();
        }

        if (executeNullIfNotFound(pubsub().projects().topics().get(topic)) == null) {
            pubsub().projects().topics().create(topic, new Topic()).execute();
        }
    }

    private void setupPubsubSubscription(String topic, String subscription) throws IOException {
        if (executeNullIfNotFound(pubsub().projects().subscriptions().get(subscription)) == null) {
            Subscription subInfo = new Subscription()
                    .setAckDeadlineSeconds(60)
                    .setTopic(topic);

            pubsub().projects().subscriptions().create(subscription, subInfo).execute();
        }
    }

    /**
     * Deletes the Google Cloud Pub/Sub topic.
     *
     * @throws IOException if there is a problem deleting the Pub/Sub topic
     */
    private void deletePubsubTopic(String topic) throws IOException {
        if (executeNullIfNotFound(pubsub().projects().topics().get(topic)) != null) {
            pubsub().projects().topics().delete(topic).execute();
        }
    }

    /**
     * Deletes the Google Cloud Pub/Sub subscription.
     *
     * @throws IOException if there is a problem deleting the Pub/Sub subscription
     */
    private void deletePubsubSubscription(String subscription) throws IOException {
        if (executeNullIfNotFound(pubsub().projects().subscriptions().get(subscription)) != null) {
            pubsub().projects().subscriptions().delete(subscription).execute();
        }
    }

    /**
     * Waits for the pipeline to finish and cancels it before the program exists.
     */
    public void waitToFinish(PipelineResult result) {
        pipelinesToCancel.add(result);
        if (!options.as(SOTOptions.class).getKeepJobsRunning()) {
            addShutdownHook(pipelinesToCancel);
        }
        try {
            result.waitUntilFinish();
        } catch (UnsupportedOperationException e) {
            // Do nothing if the given PipelineResult doesn't support waitUntilFinish(),
            // such as EvaluationResults returned by DirectRunner.
            tearDown();
            printPendingMessages();
        } catch (Exception e) {
            throw new RuntimeException("Failed to wait the pipeline until finish: " + result);
        }
    }

    private void addShutdownHook(final Collection<PipelineResult> pipelineResults) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                tearDown();
                printPendingMessages();

                for (PipelineResult pipelineResult : pipelineResults) {
                    try {
                        pipelineResult.cancel();
                    } catch (IOException e) {
                        System.out.println("Failed to cancel the job.");
                        System.out.println(e.getMessage());
                    }
                }

                for (PipelineResult pipelineResult : pipelineResults) {
                    boolean cancellationVerified = false;

                    for (int retryAttempts = 6; retryAttempts > 0; retryAttempts--) {
                        if (pipelineResult.getState().isTerminal()) {
                            cancellationVerified = true;
                            break;
                        } else {
                            System.out.println("The pipeline is still running. Verifying the cancellation.");
                        }
                        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
                    }

                    if (!cancellationVerified) {
                        System.out.println("Failed to verify the cancellation for job: " + pipelineResult);
                    }
                }
            }
        });
    }

    private void printPendingMessages() {
        System.out.println();
        System.out.println("***********************************************************");
        System.out.println("***********************************************************");
        for (String message : pendingMessages) {
            System.out.println(message);
        }
        System.out.println("***********************************************************");
        System.out.println("***********************************************************");
    }

    private static <T> T executeNullIfNotFound(
            AbstractGoogleClientRequest<T> request) throws IOException {
        try {
            return request.execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == SC_NOT_FOUND) {
                return null;
            } else {
                throw e;
            }
        }
    }
}