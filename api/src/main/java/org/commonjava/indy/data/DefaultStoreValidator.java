package org.commonjava.indy.data;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.model.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
                // If Remote Repo is disabled return data object with info that repo is disabled and valid true.
                if(remoteRepository.isDisabled()) {
                    return disabledRemoteRepositoryData(remoteRepository);
                }
                //Validate URL from remote Repository URL , throw Mailformed URL Exception if URL is not valid
                remoteUrl = Optional.of(new URL(remoteRepository.getUrl()));
                // Check if remote.ssl.required is set to true and that remote repository protocol is https = throw IndyArtifactStoreException
                if(configuration.isSSLRequired()
                    && !remoteUrl.get().getProtocol().equalsIgnoreCase(StoreValidationConstants.HTTPS)) {
                    ArtifactStoreValidateData allowedByRule = compareRemoteHostToAllowedHostnames(remoteUrl,remoteRepository);
                    // If this Non-SSL remote repository is not allowed by provided rules from configuration
                    // then return valid=false data object
                    if(!allowedByRule.isValid()) {
                        return allowedByRule;
                    }
                }
                return availableSslRemoteRepository(httpRequestsLatch,remoteUrl,remoteRepository);
            }
        }
        catch (MalformedURLException mue) {
            LOGGER.error("=> Mailformed URL: ", mue);
            errors.put(StoreValidationConstants.MAILFORMED_URL,mue.getMessage());
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl( remoteUrl.get().toExternalForm() )
                .setErrors(errors)
                .build();
        }
        catch (Exception e) {
            LOGGER.error(" => Not Valid Remote Repository, \n => Exception: " + e);
            errors.put(StoreValidationConstants.GENERAL, e.getMessage());
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl( remoteUrl.get().toExternalForm() )
                .setErrors(errors)
                .build();
        }
        if (artifactStore instanceof RemoteRepository) {
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setRepositoryUrl(((RemoteRepository) artifactStore).getUrl())
                .setErrors(errors)
                .build();
        }
        else {
            return new ArtifactStoreValidateData
                .Builder(artifactStore.getKey())
                .setValid(true)
                .setErrors(errors)
                .build();
        }
    }

    private boolean allowedNonSSLHostname(String remoteHost, String host) {
        // Get Remote Host name like string parts separated on "." from repo hostname.
        String[] remoteHostPartsTrimed =
                                        Arrays.asList(remoteHost.split("\\."))
                                            .stream()
                                            .map(val -> val.trim())
                                            .collect(Collectors.toList())
                                            .toArray(new String[0]);
        int remoteHostPartsLength = remoteHostPartsTrimed.length;
        String[] hostParts = host.split("\\.");
        int hostPartsLength = hostParts.length;
        int i = 1;
        int iter = remoteHostPartsLength;
        // Iterate through separated allowed hostnames and remote repo hostname and check their equality
        while (iter > 0) {
            String partRemoteHost = remoteHostPartsTrimed[remoteHostPartsLength - i];

            if(i > (hostPartsLength-1)) {
                return true;
            }
            String partHost = hostParts[hostPartsLength - i];
            // Case if there is "*" character for allowed repos then all variants are accepted from remote repo.
            if(partRemoteHost.equalsIgnoreCase(partHost)
                || (partRemoteHost.equalsIgnoreCase("*") || partRemoteHost.equals("") )) {
                iter--;
            } else {
                return false;
            }
            i++;
        }
        return true;
    }

    private Future<Integer> executeGetHttp(HttpGet httpGetTask , CountDownLatch countDownLatch) {
        countDownLatch.countDown();
        return executorService.submit(() -> {
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
            try (CloseableHttpResponse response = closeableHttpClient.execute(httpGetTask)) {
                LOGGER.warn("=> Check HTTP GET Response code: " + response.getStatusLine().getStatusCode());
                return response.getStatusLine().getStatusCode();
            } catch (IOException ioe) {
                LOGGER.error(" => Not Successfull HTTP GET request from StoreValidatorRemote, \n => Exception: " + ioe);
                throw new InvalidArtifactStoreException("Not valid remote Repository", ioe);
            } catch (Exception e) {
                LOGGER.error(" => Not Successfull HTTP GET request from StoreValidatorRemote, \n => Exception: " + e);
                throw new Exception("=> Not valid remote Repository", e);
            }
        });
    }

    private Future<Integer> executeHeadHttp(HttpHead httpHeadTask , CountDownLatch countDownLatch) {
        countDownLatch.countDown();
        return executorService.submit(() -> {
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
            try (CloseableHttpResponse response = closeableHttpClient.execute(httpHeadTask)) {
                LOGGER.warn("=> Check HTTP HEAD Response code: " + response.getStatusLine().getStatusCode());
                return response.getStatusLine().getStatusCode() ;
            } catch (IOException ioe) {
                LOGGER.error(" => Not Successfull HTTP HEAD request from StoreValidatorRemote, \n => Exception: " + ioe);
                throw new InvalidArtifactStoreException("Not valid remote Repository", ioe);
            } catch (Exception e) {
                LOGGER.error(" => Not Successfull HTTP HEAD request from StoreValidatorRemote, \n => Exception: " + e);
                throw new Exception("=> Not valid remote Repository", e);
            }
        });
    }

    private ArtifactStoreValidateData disabledRemoteRepositoryData(RemoteRepository remoteRepository) {
        HashMap<String, String> errors = new HashMap<>();
        errors.put(StoreValidationConstants.DISABLED_REMOTE_REPO, "Remote Repository is disabled");
        return new ArtifactStoreValidateData
            .Builder(remoteRepository.getKey())
            .setValid(true)
            .setErrors(errors)
            .build();
    }

    private ArtifactStoreValidateData compareRemoteHostToAllowedHostnames(Optional<URL> remoteUrl,RemoteRepository remoteRepository) {
        //Check First if this remote repository is in allowed repositories from remote.nossl.hosts Config Variable
        List<String> remoteNoSSLHosts = configuration.getRemoteNoSSLHosts();
        String host = remoteUrl.get().getHost();
        HashMap<String, String> errors = new HashMap<>();

        for(String remoteHost : remoteNoSSLHosts) {
            LOGGER.warn("=> Validating Allowed Remote Hostname: "+remoteHost+" For Host: "+host+"\n");
            // .apache.org , 10.192. , .maven.redhat.com
            if(allowedNonSSLHostname(remoteHost,host)) {
                errors.put(StoreValidationConstants.ALLOWED_SSL,remoteUrl.get().toString());
                LOGGER.warn(
                    "NON-SSL RemoteRepository with URL: "+ host +" is ALLOWED under RULE: " + remoteHost
                );
                return new ArtifactStoreValidateData
                    .Builder(remoteRepository.getKey())
                    .setErrors(errors)
                    .setValid(true)
                    .build();
            } else {
                errors.put(StoreValidationConstants.NON_SSL,remoteUrl.get().toString());
            }
        }
        return new ArtifactStoreValidateData
            .Builder(remoteRepository.getKey())
            .setErrors(errors)
            .setValid(false)
            .build();
    }

    private ArtifactStoreValidateData availableSslRemoteRepository(CountDownLatch httpRequestsLatch,
                                                                   Optional<URL> remoteUrl,RemoteRepository remoteRepository) throws InterruptedException, ExecutionException, URISyntaxException {
        HashMap<String, String> errors = new HashMap<>();
        // Execute HTTP GET & HEAD requests in separate thread pool from executor service
        Future<Integer> httpGetStatus = executeGetHttp(new HttpGet(remoteUrl.get().toURI()), httpRequestsLatch);
        Future<Integer> httpHeadStatus = executeHeadHttp(new HttpHead(remoteUrl.get().toURI()), httpRequestsLatch);
        // Waiting for Http GET & HEAD Request Executor tasks to finish
        httpRequestsLatch.await();
        errors.put(StoreValidationConstants.HTTP_GET_STATUS, httpGetStatus.get().toString());
        errors.put(StoreValidationConstants.HTTP_HEAD_STATUS, httpHeadStatus.get().toString());
        if(!remoteUrl.get().getProtocol().equalsIgnoreCase(StoreValidationConstants.HTTPS)) {
            errors.put(StoreValidationConstants.HTTP_PROTOCOL, remoteUrl.get().getProtocol());
        }
        // Check for Sucessfull Validation
        if (httpGetStatus.get() < 400 && httpHeadStatus.get() < 400) {
            LOGGER.warn("=> Success HTTP GET and HEAD Response from Remote Repository: " + remoteUrl.get());
            return new ArtifactStoreValidateData
                .Builder(remoteRepository.getKey())
                .setRepositoryUrl(remoteUrl.get().toExternalForm())
                .setValid(true)
                .setErrors(errors)
                .build();
        } else {
            LOGGER.warn("=> Failure @ HTTP GET and HEAD Response from Remote Repository: " + remoteUrl.get());
            return new ArtifactStoreValidateData
                .Builder(remoteRepository.getKey())
                .setRepositoryUrl(remoteUrl.get().toExternalForm())
                .setValid(false)
                .setErrors(errors)
                .build();
        }
    }
}
