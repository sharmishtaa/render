package org.janelia.alignment.match;

import com.google.common.base.Objects;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.janelia.alignment.json.JsonUtils;

/**
 * Key identifiers for a canvas.
 *
 * @author Eric Trautman
 */
public class CanvasId
        implements Comparable<CanvasId>, Serializable {

    /** Group (e.g. section) identifier. */
    private final String groupId;

    /** Canvas (e.g. tile) identifier. */
    private final String id;

    // no-arg constructor needed for JSON deserialization
    @SuppressWarnings("unused")
    private CanvasId() {
        this.groupId = null;
        this.id = null;
    }

    /**
     * Basic constructor.
     *
     * @param  groupId  group (e.g. section) identifier.
     * @param  id       canvas (e.g. tile) identifier.
     */
    public CanvasId(@Nonnull final String groupId,
                    @Nonnull final String id) {
        this.groupId = groupId;
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        final CanvasId canvasId = (CanvasId) that;
        return Objects.equal(id, canvasId.id) &&
               Objects.equal(groupId, canvasId.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, groupId);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int compareTo(@Nonnull final CanvasId that) {
        int result = this.groupId.compareTo(that.groupId);
        if (result == 0) {
            result = this.id.compareTo(that.id);
        }
        return result;
    }

    @Override
    public String toString() {
        return id;
    }

    public String toJson() {
        return JSON_HELPER.toJson(this);
    }

    private static final JsonUtils.Helper<CanvasId> JSON_HELPER =
            new JsonUtils.Helper<>(CanvasId.class);

}
