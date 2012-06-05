package org.apache.archiva.rest.services.provider;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
@Provider
@Consumes(MediaType.TEXT_PLAIN)
@Service("charSequenceReader")
public class CharSequenceReader implements MessageBodyReader<CharSequence>{


    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public CharSequence readFrom(Class<CharSequence> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(entityStream, stringWriter);
        return stringWriter.toString();
    }
}
