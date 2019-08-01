package org.commonjava.indy.data;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.model.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;


public class DefaultStoreValidator implements StoreValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStoreValidator.class);

    @Inject
    @WeftManaged
    @ExecutorConfig( named="store-validation", threads=2, priority=6 )
    private ExecutorService executorService;

    @Inject
    IndyConfiguration configuration;

    @Override
    public ArtifactStoreValidateData validate(ArtifactStore artifactStore) throws InvalidArtifactStoreException, MalformedURLException {
        final CountDownLatch httpRequestsLatch = new CountDownLatch(2);
        final HashMap<String, String> errors = new HashMap<>();
        Optional<URL> remoteUrl = Optional.empty();


        try {
            if(StoreType.remote == artifactStore.getType()) {

                // Cast to Remote Repository
                RemoteRepository remoteRepository = (RemoteRepository) artifactStore;

                //Validate URL from remote Repository URL
                remoteUrl = Optional.of(new URL(remoteRepository.getUrl()));
                // Check if remote.ssl.required is set to true and that remote repository protocol is https = throw IndyArtifactStoreException
                if(configuration.isSSLRequired() && !remoteUrl.get().getProtocol().equalsIgnoreCase("https")) {

                    //Check First if this remote repository is in allowed repositories from remote.nossl.hosts Config Variable
                    List<String> remoteNoSSLHosts = configuration.getRemoteNoSSLHosts();
                    String host = remoteUrl.get().getHost();
                    for(String remoteHost : remoteNoSSLHosts) {
                        if(!remoteHost.contains(host)) {
                            errors.put(NON_SSL, "Not Valid SSL Remote Repository");
                            LOGGER.warn("NON-SSL RemoteRepository with URL: " + remoteUrl);
                            // throw new InvalidArtifactStoreException("Not Valid SSL Remote Repository", null, null);
                        }
                    }

                }
                // Execute HTTP GET & HEAD requests in separate thread pool from executor service
                Future<Integer> httpGetStatus = executeGetHttp(new HttpGetTask(new HttpGet(remoteUrl.get().toURI())),httpRequestsLatch);
                Future<Integer> httpHeadStatus = executeHeadHttp(new HttpHeadTask(new HttpHead(remoteUrl.get().toURI())),httpRequestsLatch);
                // Waiting for Http GET & HEAD Request Executor tasks to finish
                httpRequestsLatch.await();
                errors.put(HTTP_GET_STATUS, httpGetStatus.get().toString());
                errors.put(HTTP_HEAD_STATUS, httpHeadStatus.get().toString());
                errors.put(HTTP_PROTOCOL, remoteUrl.get().getProtocol());
                // Check for Sucessfull Validation
                if (httpGetStatus.get() < 400 && httpHeadStatus.get() < 400) {
                    LOGGER.info("=> Succesfull Validation for Remote Repository: " + remoteUrl.get() + "\\" +
                        "=> Remote URl HTTP_PROTOCOL: " + remoteUrl.get().getProtocol()
                    );
                    return new ArtifactStoreValidateData
                        .Builder(remoteRepository.getKey())
                        .setRepositoryUrl(remoteUrl.get().toExternalForm())
                        .setStoreType(remoteRepository.getType())
                        .setErrors(errors)
                        .build();
                }
            }
        }
        catch (MalformedURLException mue) {
            LOGGER.error("=> Mailformed URL: ", mue);
            errors.put(StoreValidator.MAILFORMED_URL,mue.getMessage());
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl( remoteUrl.get().toExternalForm() )
                .setStoreType(artifactStore.getType())
                .setErrors(errors)
                .build();
//            throw new MalformedURLException("=> Mailformed URL: " + ((RemoteRepository) artifactStore).getUrl());
        }
        catch (Exception e) {
            LOGGER.error(" => Not Valid Remote Repository, \n => Exception: " + e);
            errors.put(StoreValidator.GENERAL, e.getMessage());
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl( remoteUrl.get().toExternalForm() )
                .setStoreType(artifactStore.getType())
                .setErrors(errors)
                .build();
//            throw new InvalidArtifactStoreException("=> Not valid remote Repository: " + artifactStore.getType().singularEndpointName(), e);
        }

        if (artifactStore instanceof RemoteRepository) {
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl(((RemoteRepository) artifactStore).getUrl())
                .setStoreType(artifactStore.getType())
                .setErrors(errors)
                .build();
        }
        else {
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl("")
                .setStoreType(artifactStore.getType())
                .setErrors(errors)
                .build();
        }

    }

    private Future<Integer> executeGetHttp(HttpGetTask httpGetTask , CountDownLatch countDownLatch) {
        countDownLatch.countDown();
        return executorService.submit(httpGetTask);
    }


    private Future<Integer> executeHeadHttp(HttpHeadTask httpHeadTask , CountDownLatch countDownLatch) {
        countDownLatch.countDown();
        return executorService.submit(httpHeadTask);
    }

    class HttpGetTask implements Callable<Integer> {

        private HttpGet httpGetCall;

        public HttpGetTask(HttpGet httpGetCall) {
            this.httpGetCall = httpGetCall;
        }

        @Override
        public Integer call() throws Exception {

            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();

            try (CloseableHttpResponse response = closeableHttpClient.execute(httpGetCall)) {
                LOGGER.warn("=> HTTP GET Response code: " + response.getStatusLine().getStatusCode());
                return response.getStatusLine().getStatusCode();
            } catch (IOException ioe) {
                LOGGER.error(" => Not Successfull HTTP GET request from StoreValidatorRemote, \n => Exception: " + ioe);
                throw new InvalidArtifactStoreException("Not valid remote Repository", ioe);
            } catch (Exception e) {
                LOGGER.error(" => Not Successfull HTTP GET request from StoreValidatorRemote, \n => Exception: " + e);
                throw new Exception("=> Not valid remote Repository", e);
            }
        }
    }

    class HttpHeadTask implements Callable<Integer> {

        private HttpHead httpHeadCall;

        public HttpHeadTask(HttpHead httpHeadCall) {
            this.httpHeadCall = httpHeadCall;
        }

        @Override
        public Integer call() throws Exception {

            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();

            try (CloseableHttpResponse response = closeableHttpClient.execute(httpHeadCall)) {

                LOGGER.warn("=> HTTP HEAD Response code: " + response.getStatusLine().getStatusCode());
                return response.getStatusLine().getStatusCode() ;
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
