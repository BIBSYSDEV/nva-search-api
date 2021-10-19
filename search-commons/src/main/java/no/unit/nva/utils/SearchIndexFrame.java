package no.unit.nva.utils;

import nva.commons.core.ioutils.IoUtils;

import java.io.InputStream;
import java.nio.file.Path;

public final class SearchIndexFrame {

    public static final String FRAME_JSON = "publication_frame.json";
    private static final String frameSrc =IoUtils.stringFromResources(Path.of(FRAME_JSON));

    private SearchIndexFrame() {

    }

    public static InputStream asInputStream() {
        return IoUtils.stringToStream(frameSrc);
    }

}
