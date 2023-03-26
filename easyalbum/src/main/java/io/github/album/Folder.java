package io.github.album;

import java.util.List;

public final class Folder {
    public final String name;
    public final long updatedTime;
    public final List<MediaData> mediaList;

    public Folder(String name, List<MediaData> mediaList) {
        this.name = name;
        this.mediaList = mediaList;
        this.updatedTime = mediaList.isEmpty() ? 0 : mediaList.get(0).modifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder other = (Folder) o;
        return updatedTime == other.updatedTime && name.equals(other.name) && mediaList.equals(other.mediaList);
    }
}
