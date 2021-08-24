package no.unit.nva.search;

import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class FramedJsonTest {

    @Test
    void getFramedJsonReturnsFramedJsonWhenInputIsValid() throws IOException {
        InputStream one = IoUtils.inputStreamFromResources("framed-json/object_one.json");
        InputStream two = IoUtils.inputStreamFromResources("framed-json/object_two.json");
        assert one != null;
        assert two != null;
        List<InputStream> objects = List.of(one, two);
        InputStream frame = IoUtils.inputStreamFromResources("framed-json/test_frame.json");
        var framedJson = new FramedJson(objects, frame);
        assertThat(framedJson.getFramedJson(), is(notNullValue()));
    }

}