package org.commonjava.indy.data;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.kafka.common.protocol.types.Field;
import org.bouncycastle.util.Store;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

import java.util.HashMap;
import java.util.Map;

public class ArtifactStoreValidateData {

    private boolean valid;
    private String repositoryUrl;

    private StoreKey storeKey;
    private Map<String,String> errors;


    private ArtifactStoreValidateData() {
    }


    public static class Builder {

        private boolean valid;
        private String repositoryUrl;

        private StoreKey storeKey;
        private Map<String,String> errors;

        public Builder(StoreKey storeKey) {

            this.storeKey = storeKey;
            this.errors = new HashMap<>();
            this.valid = false;
        }


        public Builder setRepositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        public Builder setValid(Boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder setErrors(Map<String,String> errors) {
            this.errors = errors;
            return this;
        }

        public ArtifactStoreValidateData build() {
            ArtifactStoreValidateData artifactStoreValidateData = new ArtifactStoreValidateData();
            artifactStoreValidateData.valid = this.valid;
            artifactStoreValidateData.repositoryUrl = this.repositoryUrl;
            artifactStoreValidateData.errors = this.errors;
            artifactStoreValidateData.storeKey = this.storeKey;
            return artifactStoreValidateData;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public StoreKey getStoreKey() {
        return storeKey;
    }

    public void setStoreKey(StoreKey storeKey) {
        this.storeKey = storeKey;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "{ valid:" + this.valid +
            ", repositoryUrl:" + this.repositoryUrl +
            ", errors: " + this.errors +
            ", storeKey: " + this.storeKey +
            "}";
    }
}
