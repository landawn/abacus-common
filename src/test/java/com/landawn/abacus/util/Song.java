/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Song {
    private UUID id;
    private String title;
    private String album;
    private String artist;
    private Set<String> tags;
    private java.nio.ByteBuffer data;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public java.nio.ByteBuffer getData() {
        return data;
    }

    public void setData(java.nio.ByteBuffer data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(album, artist, data, id, tags, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Song other = (Song) obj;
        if (!Objects.equals(album, other.album) || !Objects.equals(artist, other.artist) || !Objects.equals(data, other.data) || !Objects.equals(id, other.id)) {
            return false;
        }
        if (!Objects.equals(tags, other.tags)) {
            return false;
        }
        if (!Objects.equals(title, other.title)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "{id=" + id + ", title=" + title + ", album=" + album + ", artist=" + artist + ", tags=" + tags + ", data=" + data + "}";
    }

}
