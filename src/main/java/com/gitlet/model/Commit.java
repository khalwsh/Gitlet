package com.gitlet.model;

import com.gitlet.util.FileUtils;

import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.TreeMap;

public final class Commit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int SHORT_HASH_LENGTH = 7;
    private static final SimpleDateFormat DISPLAY_FORMAT;

    static {
        DISPLAY_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        DISPLAY_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final Date timeStamp;
    private final String message;
    private final String parentCommitHash;
    private final String secondParentHash;
    private final TreeMap<String, String> trackedFiles;
    private final String commitHash;

    public Commit(Date timeStamp, String message) {
        this(timeStamp, message, null, null, new TreeMap<>());
    }

    public Commit(Date timeStamp, String message, String secondParentHash,
                  String parentCommitHash, Map<String, String> trackedFiles) {
        this.timeStamp = timeStamp;
        this.message = message;
        this.parentCommitHash = parentCommitHash;
        this.secondParentHash = secondParentHash;
        this.trackedFiles = new TreeMap<>(trackedFiles);
        this.commitHash = generateHash();
    }

    private String generateHash() {
        List<Object> items = new ArrayList<>();
        items.add(message);
        items.add(String.valueOf(timeStamp.getTime()));
        items.add(secondParentHash == null ? "" : secondParentHash);
        items.add(parentCommitHash == null ? "" : parentCommitHash);
        for (Map.Entry<String, String> entry : trackedFiles.entrySet()) {
            items.add(entry.getKey() + "=" + entry.getValue());
        }
        return FileUtils.sha1(items);
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getParentCommitHash() {
        return parentCommitHash;
    }

    public String getSecondParentHash() {
        return secondParentHash;
    }

    public Map<String, String> trackedFiles() {
        return Collections.unmodifiableMap(trackedFiles);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("commit ").append(commitHash).append('\n');
        if (secondParentHash != null && parentCommitHash != null) {
            sb.append("Merge: ")
                    .append(shortHash(parentCommitHash)).append(' ')
                    .append(shortHash(secondParentHash)).append('\n');
        }
        synchronized (DISPLAY_FORMAT) {
            sb.append("Date: ").append(DISPLAY_FORMAT.format(timeStamp)).append('\n');
        }
        sb.append(message).append('\n');
        return sb.toString();
    }

    private static String shortHash(String hash) {
        return hash.substring(0, Math.min(SHORT_HASH_LENGTH, hash.length()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Commit)) return false;
        return Objects.equals(commitHash, ((Commit) obj).commitHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commitHash);
    }
}
