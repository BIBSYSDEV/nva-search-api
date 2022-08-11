package no.unit.nva.search.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import nva.commons.core.ioutils.IoUtils;

public class DemoHandler implements RequestStreamHandler {
    
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
        throws IOException {
        var inputString = IoUtils.streamToString(input);
        try (var writer = new BufferedWriter(new OutputStreamWriter(output))) {
            writer.write(inputString);
            writer.flush();
        }
    }
}
