package no.unit.nva.search.utils;

import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;

public final class SearchIndexFrame {

    public static final String FRAME_JSON = "publication_frame.json";
    public static final String FRAME_SRC = IoUtils.stringFromResources(Path.of(FRAME_JSON));

    private SearchIndexFrame() {

    }

}
