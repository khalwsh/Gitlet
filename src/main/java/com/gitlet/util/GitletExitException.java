package com.gitlet.util;

import java.io.Serial;

public final class GitletExitException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GitletExitException(String message) {
        super(message);
    }
}
