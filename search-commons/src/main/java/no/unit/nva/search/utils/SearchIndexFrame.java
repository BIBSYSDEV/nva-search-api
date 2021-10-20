package no.unit.nva.search.utils;

import java.io.InputStream;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;

public final class SearchIndexFrame {

    public static final String FRAME_JSON = "publication_frame.json";
    private static final String frameSrc = IoUtils.stringFromResources(Path.of(FRAME_JSON));

    private SearchIndexFrame() {

    }

    public static InputStream fetchFrame() {
        return IoUtils.stringToStream(frameSrc);
    }
}
