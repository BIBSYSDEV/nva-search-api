package no.unit.nva.search.utils;

import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;

public final class SearchIndexFrame {

    public static final String FRAME_JSON = "publication_frame.json";
    public static final String FRAME_SRC = streamToString(inputStreamFromResources(FRAME_JSON));

    private SearchIndexFrame() {

    }

}
