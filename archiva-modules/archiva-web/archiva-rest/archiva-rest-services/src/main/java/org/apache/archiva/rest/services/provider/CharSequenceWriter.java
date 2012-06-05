package org.apache.archiva.rest.services.provider;

import org.springframework.stereotype.Service;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
@Provider
@Produces(MediaType.TEXT_PLAIN)
@Service("charSequenceWriter")
public class CharSequenceWriter implements MessageBodyWriter<CharSequence>{

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public long getSize(CharSequence charSequence, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return charSequence.length();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTo(CharSequence charSequence, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(entityStream);
        writer.write(charSequence.toString());
    }
}
