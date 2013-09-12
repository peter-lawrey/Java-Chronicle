/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// based from eishay/jvm-serializers
package data.media;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class MediaContent implements Externalizable {
    public Media media;
    public List<Image> images;

    public MediaContent() {
    }

    public MediaContent(Media media, List<Image> images) {
        this.media = media;
        this.images = images;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaContent that = (MediaContent) o;

        if (images != null ? !images.equals(that.images) : that.images != null) return false;
        return !(media != null ? !media.equals(that.media) : that.media != null);

    }

    @Override
    public int hashCode() {
        int result = media != null ? media.hashCode() : 0;
        result = 31 * result + (images != null ? images.hashCode() : 0);
        return result;
    }

    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[MediaContent: ");
        sb.append("media=").append(media);
        sb.append(", images=").append(images);
        sb.append("]");
        return sb.toString();
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public Media getMedia() {
        return media;
    }

    public List<Image> getImages() {
        return images;
    }

    @Override
    public void writeExternal(@NotNull ObjectOutput out) throws IOException {
        media.writeExternal(out);
        out.writeInt(images.size());
        for (int i = 0; i < images.size(); i++)
            images.get(i).writeExternal(out);
    }

    @Override
    public void readExternal(@NotNull ObjectInput in) throws IOException, ClassNotFoundException {
        if (media == null)
            media = new Media();
        media.readExternal(in);
        int size = in.readInt();
        if (images == null) {
            images = new ArrayList<Image>();
            while (images.size() > size)
                images.remove(images.size() - 1);
            while (images.size() < size)
                images.add(new Image());
            for (int i = 0; i < size; i++)
                images.get(i).readExternal(in);
        }
    }
}
