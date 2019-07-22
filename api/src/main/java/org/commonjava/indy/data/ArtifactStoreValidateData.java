package org.commonjava.indy.data;

import org.apache.kafka.common.protocol.types.Field;
import org.bouncycastle.util.Store;
import org.commonjava.indy.model.core.StoreType;

public class ArtifactStoreValidateData {

    private boolean valid;
    private String repositoryUrl;
    private StoreType storeType;

    private ArtifactStoreValidateData() {
    }


    public static class Builder {

        private boolean valid;
        private String repositoryUrl;
        private StoreType storeType;

        public Builder(boolean valid) {
            this.valid = valid;
        }


        public Builder setRepositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        public Builder setStoreType(StoreType storeType) {
            this.storeType = storeType;
            return this;
        }

        public ArtifactStoreValidateData build() {
            ArtifactStoreValidateData artifactStoreValidateData = new ArtifactStoreValidateData();
            artifactStoreValidateData.valid = this.valid;
            artifactStoreValidateData.repositoryUrl = this.repositoryUrl;
            artifactStoreValidateData.storeType = this.storeType;
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

    public StoreType getStoreType() {
        return storeType;
    }

    public void setStoreType(StoreType storeType) {
        this.storeType = storeType;
    }

    @Override
    public String toString() {
        return "ArtifactStoreValidateData={ valid:" + this.valid +
            ", repositoryUrl:" + this.repositoryUrl +
            ", storeType: " + this.storeType +
            "}";
    }
}
