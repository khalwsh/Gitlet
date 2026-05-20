package com.gitlet.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class Branch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private String referredCommitHash;

    public Branch(String name, String referredCommitHash) {
        this.name = name;
        this.referredCommitHash = referredCommitHash;
    }

    public String getName() {
        return name;
    }

    public String getReferredCommitHash() {
        return referredCommitHash;
    }

    public void setCommit(String commitHash) {
        this.referredCommitHash = commitHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Branch)) return false;
        Branch other = (Branch) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(referredCommitHash, other.referredCommitHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, referredCommitHash);
    }
}
