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
                boolean disabled = remoteRepository.isDisabled();
                if(disabled) {
                    errors.put(StoreValidationConstants.DISABLED_REMOTE_REPO, "Remote Repository is disabled");
                    return new ArtifactStoreValidateData
                        .Builder(remoteRepository.getKey())
                        .setValid(true)
                        .setErrors(errors)
                        .build();
                    //throw new InvalidArtifactStoreException("Disabled Store", null, null);
                }
                //Validate URL from remote Repository URL
                remoteUrl = Optional.of(new URL(remoteRepository.getUrl()));
                // Check if remote.ssl.required is set to true and that remote repository protocol is https = throw IndyArtifactStoreException
                if(configuration.isSSLRequired()
                    && !remoteUrl.get().getProtocol().equalsIgnoreCase(StoreValidationConstants.HTTPS)) {

                    //Check First if this remote repository is in allowed repositories from remote.nossl.hosts Config Variable
                    List<String> remoteNoSSLHosts = configuration.getRemoteNoSSLHosts();
                    String host = remoteUrl.get().getHost();
                    boolean allowedByRule = false;

                    for(String remoteHost : remoteNoSSLHosts) {
                        LOGGER.warn("\n=> Validating RemoteHost: " + remoteHost + " For Host: " + host + "\n");
                        // .apache.org , 10.192. , .maven.redhat.com
                        if(allowedNonSSLHostname(remoteHost,host)) {
                            errors.put(StoreValidationConstants.ALLOWED_SSL,remoteUrl.get().toString());
                            LOGGER.warn(
                                "NON-SSL RemoteRepository with URL: "+ host +" is ALLOWED under RULE: " + remoteHost
                            );
                            allowedByRule = true;
                            break;
                        } else {
                            errors.put(StoreValidationConstants.NON_SSL,remoteUrl.get().toString());
                            LOGGER.warn(
                                "NON-SSL RemoteRepository with URL: "+ host +" is NOT ALLOWED under RULE: " + remoteHost
                            );

                        }
                    }
                    // If this Non-SSL remote repository is not allowed by provided rules from configuration
                    // then return valid=false data object
                    if(!allowedByRule) {
                        return new ArtifactStoreValidateData
                            .Builder(remoteRepository.getKey())
                            .setErrors(errors)
                            .setValid(false)
                            .build();
                    }
                }
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
                }
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
        while (iter > 0) {
            String partRemoteHost = remoteHostPartsTrimed[remoteHostPartsLength - i];

            LOGGER.warn("=> Checking Remote host:[" + partRemoteHost + "]");

            if(i > (hostPartsLength-1)) {
                return true;
            }
            String partHost = hostParts[hostPartsLength - i];

            LOGGER.warn("=> Checking Host:[" + partHost + "]");

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
}
