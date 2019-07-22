package org.commonjava.indy.data;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.indy.model.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;


@RemoteStoreValidator
public class StoreValidatorRemote implements StoreValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreValidatorRemote.class);
    private final CountDownLatch httpRequestsLatch = new CountDownLatch(2);
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static final String REMOTE = "remote";


    @Override
    public ArtifactStoreValidateData validate(ArtifactStore artifactStore)
        throws InvalidArtifactStoreException, MalformedURLException {
        // Parse Remote Repository for checking if it is a valid URL
        try {
            //#region
//            LOGGER.info("=> ArtifactStore Type: " + artifactStore.getType().singularEndpointName());
            if (REMOTE.equalsIgnoreCase(artifactStore.getType().singularEndpointName())
                || artifactStore instanceof RemoteRepository) {
                // Cast to Remote Repository
                RemoteRepository remoteRepository = (RemoteRepository) artifactStore;
                //Validate URL from remote Repository URL
                Optional<URL> remoteUrl = Optional.of(new URL(remoteRepository.getUrl()));
                // Execute HTTP GET & HEAD requests in separate thread pool from executor service
                Future<Boolean> httpGetStatus = executorService.submit(new HttpGetTask(new HttpGet(remoteUrl.get().toURI())));
                Future<Boolean> httpHeadStatus = executorService.submit(new HttpHeadTask(new HttpHead(remoteUrl.get().toURI())));
                // Waiting for Http GET & HEAD Request Executor tasks to finish
                httpRequestsLatch.await();
                // Check for Sucessfull Validation
                if (httpGetStatus.get() && httpHeadStatus.get()) {
                    LOGGER.info("=> Succesfull Validation for Remote Repository: " + remoteUrl.get());
                    return new ArtifactStoreValidateData
                        .Builder(true)
                        .setRepositoryUrl(remoteUrl.get().toExternalForm())
                        .setStoreType(remoteRepository.getType())
                        .build();
                }
            } else {
                LOGGER.warn("=> It's not Remote Artifact Store Repository: " + artifactStore.getType().singularEndpointName());
            }
            //#endregion
        } catch (MalformedURLException mue) {
            LOGGER.error("=> Mailformed URL: ", mue);
            throw new MalformedURLException("=> Mailformed URL: " + ((RemoteRepository) artifactStore).getUrl());
        } catch (Exception e) {
            LOGGER.error(" => Not Valid Remote Repository, \n => Exception: " + e);
            throw new InvalidArtifactStoreException("=> Not valid remote Repository: " + artifactStore.getType().singularEndpointName(), e);
        } finally {
            if (executorService.isShutdown()) {
                try {
                    executorService.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    LOGGER.warn("=> Some of Http Executors Tasks are still in waiting state after 60s timeout");
                    executorService.shutdownNow();
                }
            }
        }

        if (artifactStore instanceof RemoteRepository) {
            return new ArtifactStoreValidateData
                .Builder(false)
                .setRepositoryUrl(((RemoteRepository) artifactStore).getUrl())
                .setStoreType(artifactStore.getType())
                .build();
        } else if (artifactStore instanceof HostedRepository) {
            return new ArtifactStoreValidateData
                .Builder(false)
                .setRepositoryUrl(((HostedRepository) artifactStore).getStorage())
                .setStoreType(artifactStore.getType())
                .build();
        } else {
            return new ArtifactStoreValidateData
                .Builder(false)
                .setRepositoryUrl("/")
                .setStoreType(artifactStore.getType())
                .build();
        }

    }

    private boolean allowedGet(List<String> allowed) {
        return allowed.indexOf("GET") != -1 ? true : false;
    }

    private boolean allowedHead(List<String> allowed) {
        return allowed.indexOf("HEAD") != -1 ? true : false;
    }

    class HttpGetTask implements Callable<Boolean> {

        private HttpGet httpGetCall;

        public HttpGetTask(HttpGet httpGetCall) {
            this.httpGetCall = httpGetCall;
        }

        @Override
        public Boolean call() throws Exception {

            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();

            try (CloseableHttpResponse response = closeableHttpClient.execute(httpGetCall)) {
                httpRequestsLatch.countDown();
                LOGGER.warn("=> HTTP GET Response code: " + response.getStatusLine().getStatusCode());
                return (response.getStatusLine().getStatusCode() < 400) ? true : false;
            } catch (IOException ioe) {
                LOGGER.error(" => Not Successfull HTTP GET request from StoreValidatorRemote, \n => Exception: " + ioe);
                throw new InvalidArtifactStoreException("Not valid remote Repository", ioe);
            } catch (Exception e) {
                LOGGER.error(" => Not Successfull HTTP GET request from StoreValidatorRemote, \n => Exception: " + e);
                throw new Exception("=> Not valid remote Repository", e);
            }
        }
    }

    class HttpHeadTask implements Callable<Boolean> {

        private HttpHead httpHeadCall;

        public HttpHeadTask(HttpHead httpHeadCall) {
            this.httpHeadCall = httpHeadCall;
        }

        @Override
        public Boolean call() throws Exception {

            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();

            try (CloseableHttpResponse response = closeableHttpClient.execute(httpHeadCall)) {
                httpRequestsLatch.countDown();
                LOGGER.warn("=> HTTP HEAD Response code: " + response.getStatusLine().getStatusCode());
                return response.getStatusLine().getStatusCode() < 400 ? true : false;
            } catch (IOException ioe) {
                LOGGER.error(" => Not Successfull HTTP HEAD request from StoreValidatorRemote, \n => Exception: " + ioe);
                throw new InvalidArtifactStoreException("Not valid remote Repository", ioe);
            } catch (Exception e) {
                LOGGER.error(" => Not Successfull HTTP HEAD request from StoreValidatorRemote, \n => Exception: " + e);
                throw new Exception("=> Not valid remote Repository", e);
            }
        }
    }

}
