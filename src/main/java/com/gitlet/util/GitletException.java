package com.gitlet.util;

import java.io.Serial;

public class GitletException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GitletException() {
        super();
    }

    public GitletException(String message) {
        super(message);
    }
}
